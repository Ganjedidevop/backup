package com.example.arkin.util

import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object FileViewerHelper {
    private const val TAG = "FileViewerHelper"
    private const val ATTACHMENTS_DIR = "attachments"

    /**
     * Persistently saves an attached file from a picked Content URI or file URI into the app's internal sandbox.
     * This guarantees that the photo/document will never expire, lose permissions, or become inaccessible.
     *
     * @return Pair of (savedFilePathString, originalFileName)
     */
    fun persistAttachment(context: Context, sourceUri: Uri): Pair<String, String> {
        val attachmentsDir = File(context.filesDir, ATTACHMENTS_DIR).apply { mkdirs() }
        val (originalName, _) = DocumentTextExtractor.getFileMetadata(context, sourceUri)

        // Check if this is already an internal file in attachmentsDir
        if (sourceUri.scheme == "file" || sourceUri.scheme == null) {
            val localPath = sourceUri.path
            if (!localPath.isNullOrBlank()) {
                val localFile = File(localPath)
                if (localFile.exists() && localFile.parentFile?.absolutePath == attachmentsDir.absolutePath) {
                    return Pair(localFile.absolutePath, originalName)
                }
            }
        }

        try {
            // Attempt to take persistable URI permission if supported by the provider
            context.contentResolver.takePersistableUriPermission(
                sourceUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
            // Ignored if provider does not support persistable permissions
        }

        return try {
            val cleanExt = originalName.substringAfterLast('.', "").lowercase().let {
                if (it.isBlank() || it.length > 6) {
                    val mime = context.contentResolver.getType(sourceUri)
                    MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "dat"
                } else it
            }

            val uniqueFileName = "att_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}${if (cleanExt.isNotBlank()) ".$cleanExt" else ""}"
            val destFile = File(attachmentsDir, uniqueFileName)

            val inputStream = if (sourceUri.scheme == "file" || (sourceUri.scheme == null && sourceUri.path?.startsWith("/") == true)) {
                java.io.FileInputStream(File(sourceUri.path!!))
            } else {
                context.contentResolver.openInputStream(sourceUri)
            }

            inputStream?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            if (destFile.exists() && destFile.length() > 0) {
                Log.d(TAG, "Successfully copied attachment to permanent storage: ${destFile.absolutePath}")
                Pair(destFile.absolutePath, originalName)
            } else {
                Pair(sourceUri.toString(), originalName)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to copy file to internal storage, falling back to original URI: ${e.message}", e)
            Pair(sourceUri.toString(), originalName)
        }
    }

    /**
     * Opens a document or photo with an external viewer app using FileProvider.
     * Automatically attempts to recover from expired URI permissions by checking MediaStore or cached files.
     */
    fun openDocument(
        context: Context,
        uriString: String?,
        fileName: String? = null,
        onUriUpdated: ((String) -> Unit)? = null
    ) {
        if (uriString.isNullOrBlank()) {
            Toast.makeText(context, "No attachment available to open", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val attachmentsDir = File(context.filesDir, ATTACHMENTS_DIR).apply { mkdirs() }

            // 1. First, check if the URI directly points to an existing file on disk
            var fileToOpen: File? = when {
                // Direct absolute path (must exist and not be empty)
                uriString.startsWith("/") && File(uriString).exists() && File(uriString).length() > 0 -> File(uriString)
                // file:// URI
                uriString.startsWith("file://") -> {
                    val parsed = Uri.parse(uriString)
                    parsed.path?.let { File(it) }?.takeIf { it.exists() && it.length() > 0 }
                }
                // Relative to attachments directory
                File(attachmentsDir, uriString.substringAfterLast('/')).exists() && File(attachmentsDir, uriString.substringAfterLast('/')).length() > 0 -> {
                    File(attachmentsDir, uriString.substringAfterLast('/'))
                }
                // Direct file name in filesDir
                File(context.filesDir, uriString.substringAfterLast('/')).exists() && File(context.filesDir, uriString.substringAfterLast('/')).length() > 0 -> {
                    File(context.filesDir, uriString.substringAfterLast('/'))
                }
                // Check cacheDir
                File(context.cacheDir, uriString.substringAfterLast('/')).exists() && File(context.cacheDir, uriString.substringAfterLast('/')).length() > 0 -> {
                    File(context.cacheDir, uriString.substringAfterLast('/'))
                }
                else -> null
            }

            // 2. Check if a cached copy already exists in attachmentsDir or filesDir matching fileName
            if ((fileToOpen == null || !fileToOpen.exists() || fileToOpen.length() == 0L) && !fileName.isNullOrBlank()) {
                val candidateAttachments = File(attachmentsDir, fileName)
                val candidateFiles = File(context.filesDir, fileName)
                val candidateCache = File(context.cacheDir, fileName)
                fileToOpen = when {
                    candidateAttachments.exists() && candidateAttachments.length() > 0 -> candidateAttachments
                    candidateFiles.exists() && candidateFiles.length() > 0 -> candidateFiles
                    candidateCache.exists() && candidateCache.length() > 0 -> candidateCache
                    else -> null
                }
            }

            // 3. If file is not yet a local file, try content URI cache
            if (fileToOpen == null || !fileToOpen.exists() || fileToOpen.length() == 0L) {
                if (uriString.startsWith("content://")) {
                    val srcUri = Uri.parse(uriString)
                    val cached = tryCacheContentUri(context, srcUri, fileName)
                    if (cached != null && cached.exists() && cached.length() > 0) {
                        fileToOpen = cached
                        onUriUpdated?.invoke(cached.absolutePath)
                    }
                }
            }

            // 4. Exhaustive MediaStore recovery by display name, original name, or fallback filename
            if (fileToOpen == null || !fileToOpen.exists() || fileToOpen.length() == 0L) {
                val nameToSearch = fileName?.takeIf { it.isNotBlank() }
                    ?: uriString.substringAfterLast('/').takeIf { it.isNotBlank() }

                val recovered = tryFindInMediaStore(context, nameToSearch)
                if (recovered != null && recovered.exists() && recovered.length() > 0) {
                    fileToOpen = recovered
                    onUriUpdated?.invoke(recovered.absolutePath)
                }
            }

            // 5. If still not accessible, notify the user cleanly
            if (fileToOpen == null || !fileToOpen.exists() || fileToOpen.length() == 0L) {
                val displayName = fileName ?: uriString.substringAfterLast('/')
                Log.w(TAG, "File cannot be accessed: $uriString (name: $fileName)")
                Toast.makeText(
                    context,
                    "Attachment '$displayName' is not accessible on device storage.\n\nPlease edit this item to attach the file or photo again.",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            // 6. Open via FileProvider with proper package authority
            val authority = "${context.packageName}.fileprovider"
            val providerUri = FileProvider.getUriForFile(context, authority, fileToOpen)
            val mimeType = resolveMimeType(fileToOpen.name, fileName)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(providerUri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(intent, "Open with").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            try {
                context.startActivity(chooser)
            } catch (_: ActivityNotFoundException) {
                // Fallback: try with generic mime type
                val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(providerUri, "*/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(fallbackIntent, "Open with").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                })
            }
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No app available to open this file", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException opening file: $uriString", e)
            Toast.makeText(
                context,
                "Cannot access file permission. Please re-attach the file to save it permanently.",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Log.e(TAG, "Exception opening file: ${e.message}", e)
            Toast.makeText(context, "Failed to open file: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Safely loads and decodes an image thumbnail bitmap for Notes & Homework preview.
     * Downsamples to avoid memory pressure and returns null if not an accessible image.
     */
    fun getDecodedThumbnail(context: Context, uriString: String?): android.graphics.Bitmap? {
        if (uriString.isNullOrBlank()) return null
        return try {
            val attachmentsDir = File(context.filesDir, ATTACHMENTS_DIR)
            val file = when {
                uriString.startsWith("/") -> File(uriString)
                uriString.startsWith("file://") -> Uri.parse(uriString).path?.let { File(it) }
                File(attachmentsDir, uriString.substringAfterLast('/')).exists() -> File(attachmentsDir, uriString.substringAfterLast('/'))
                File(context.filesDir, uriString.substringAfterLast('/')).exists() -> File(context.filesDir, uriString.substringAfterLast('/'))
                else -> null
            }

            if (file != null && file.exists() && file.length() > 0) {
                val ext = file.extension.lowercase()
                if (ext in listOf("jpg", "jpeg", "png", "webp", "bmp")) {
                    val boundsOpts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    android.graphics.BitmapFactory.decodeFile(file.absolutePath, boundsOpts)
                    val sampleSize = calculateInSampleSize(boundsOpts, 320, 320)
                    val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = sampleSize }
                    return android.graphics.BitmapFactory.decodeFile(file.absolutePath, opts)
                }
            }

            if (uriString.startsWith("content://")) {
                val uri = Uri.parse(uriString)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val boundsOpts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    android.graphics.BitmapFactory.decodeStream(stream, null, boundsOpts)
                }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = 4 }
                    return android.graphics.BitmapFactory.decodeStream(stream, null, opts)
                }
            }
            null
        } catch (_: Throwable) {
            null
        }
    }

    private fun calculateInSampleSize(options: android.graphics.BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun tryCacheContentUri(context: Context, srcUri: Uri, fileName: String?): File? {
        return try {
            try {
                context.contentResolver.takePersistableUriPermission(
                    srcUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}

            val attachmentsDir = File(context.filesDir, ATTACHMENTS_DIR).apply { mkdirs() }
            val name = fileName ?: "cached_file"
            val cleanExt = name.substringAfterLast('.', "").lowercase().let {
                if (it.isBlank() || it.length > 6) {
                    val mime = context.contentResolver.getType(srcUri)
                    MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "dat"
                } else it
            }
            val destFile = File(attachmentsDir, "cached_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.${cleanExt}")
            context.contentResolver.openInputStream(srcUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (destFile.exists() && destFile.length() > 0) destFile else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Smart fallback: searches MediaStore for screenshots or photos by original file name
     * when the previous Content URI grant has expired.
     */
    private fun tryFindInMediaStore(context: Context, fileName: String?): File? {
        if (fileName.isNullOrBlank()) return null
        return try {
            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME
            )
            val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
            val selectionArgs = arrayOf(fileName)

            val urisToQuery = listOf(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                MediaStore.Files.getContentUri("external")
            )

            for (queryUri in urisToQuery) {
                try {
                    context.contentResolver.query(
                        queryUri,
                        projection,
                        selection,
                        selectionArgs,
                        "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val idIndex = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
                            if (idIndex != -1) {
                                val id = cursor.getLong(idIndex)
                                val itemUri = ContentUris.withAppendedId(queryUri, id)
                                val attachmentsDir = File(context.filesDir, ATTACHMENTS_DIR).apply { mkdirs() }
                                val cleanExt = fileName.substringAfterLast('.', "dat").lowercase()
                                val destFile = File(attachmentsDir, "recovered_${System.currentTimeMillis()}_${cleanExt}")
                                context.contentResolver.openInputStream(itemUri)?.use { input ->
                                    FileOutputStream(destFile).use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                if (destFile.exists() && destFile.length() > 0) {
                                    Log.d(TAG, "Successfully recovered file from MediaStore: ${destFile.absolutePath}")
                                    return destFile
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "MediaStore search failed: ${e.message}")
            null
        }
    }

    private fun resolveMimeType(name: String, fallbackName: String?): String {
        val ext = (name.ifBlank { fallbackName.orEmpty() }).substringAfterLast('.', "").lowercase()
        val mimeFromExt = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
        if (!mimeFromExt.isNullOrBlank()) return mimeFromExt

        return when (ext) {
            "pdf" -> "application/pdf"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "txt" -> "text/plain"
            "doc", "docx" -> "application/msword"
            "ppt", "pptx" -> "application/vnd.ms-powerpoint"
            "xls", "xlsx" -> "application/vnd.ms-excel"
            else -> "*/*"
        }
    }
}