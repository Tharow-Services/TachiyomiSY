package exh

import android.annotation.SuppressLint
import android.database.Cursor
import android.database.MatrixCursor
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import android.util.Log
import android.webkit.MimeTypeMap
import eu.kanade.tachiyomi.BuildConfig
import java.io.File
import eu.kanade.tachiyomi.R
import okio.IOException
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream

class StorageProvider: DocumentsProvider() {
    lateinit var baseDir: File

    override fun onCreate(): Boolean {
        baseDir = context!!.filesDir
        return true
    }

    override fun queryRoots(projection: Array<out String>?): Cursor {
        val result = projection.resolveProjection(true)
        val row = result.newRow()
        row.add(Root.COLUMN_ROOT_ID, baseDir.name)
        row.add(Root.COLUMN_ICON, R.drawable.ic_tachi)
        row.add(Root.COLUMN_TITLE, context!!.getString(R.string.app_name))
        row.add(Root.COLUMN_DOCUMENT_ID, baseDir.getId())
        row.add(Root.COLUMN_FLAGS, Root.FLAG_SUPPORTS_CREATE.or(Root.FLAG_SUPPORTS_IS_CHILD))
        return result
    }

    override fun queryDocument(documentId: String?, projection: Array<out String>?): Cursor {
        "queryDocument($documentId, $projection)".toLogV()
        return projection.resolveProjection(false).includeFile(documentId, null)
    }

    override fun queryChildDocuments(
        parentDocumentId: String?,
        projection: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        "queryChildDocuments($parentDocumentId, $projection, $sortOrder)".toLogV()
        val result = projection.resolveProjection(false)
        val parent = parentDocumentId?.getFile()
        parent?.listFiles()?.forEach { result.includeFile(null, it) }
        return result
    }

    override fun openDocument(documentId: String?, mode: String?, signal: CancellationSignal?): ParcelFileDescriptor {
        "openDocument($documentId, $mode, $signal)".toLogV()
        return ParcelFileDescriptor.open(documentId?.getFile(), ParcelFileDescriptor.parseMode(mode))
    }

    override fun isChildDocument(parentDocumentId: String?, documentId: String?): Boolean {
        if (parentDocumentId.isNullOrEmpty() || documentId.isNullOrEmpty()) return false
        return documentId.contains(parentDocumentId)
    }

    override fun createDocument(
        parentDocumentId: String?,
        mimeType: String?,
        displayName: String?,
    ): String? {
        "createDocument($parentDocumentId, $mimeType, $displayName)"
        if (parentDocumentId.isNullOrEmpty() || displayName.isNullOrEmpty()) return null
        val parent = parentDocumentId.getFile()
        val file = File(parent, displayName)
        if (mimeType.isNullOrEmpty()) mimeType.apply {file.getMimeType()}
        if (mimeType.equals(Document.MIME_TYPE_DIR)) {
            if (!file.mkdir()) throw FileNotFoundException("Failed to Create Directory")
            return file.getId()
        }
        try {
            var wasFileChanged = false
            if (file.createNewFile()) wasFileChanged = (file.setWritable(true) && file.setReadable(true))
            if (!wasFileChanged) throw FileNotFoundException("Failed to create doc with name $displayName and docId $parentDocumentId")

        } catch (e: IOException) {
            Log.e(TAG, "Failed to create doc with name $displayName and docId $parentDocumentId", e)
            throw FileNotFoundException(e.message)
        }
        return file.getId()
    }

    override fun renameDocument(documentId: String?, displayName: String?): String? {
        "renameDocument($documentId, $displayName)".toLogV()
        if (documentId.isNullOrEmpty() || displayName.isNullOrEmpty()) return null
        val sourceFile = documentId.getFile()
        val sourceParentFile = sourceFile.parent
        if (sourceParentFile.isNullOrEmpty()) throw FileNotFoundException("Failed to rename $displayName with id $documentId. File has no parent.")
        val destFile = File(sourceParentFile, displayName)
        try {
            if (!sourceFile.renameTo(destFile)) throw okio.FileNotFoundException("Failed to rename $documentId to $displayName. Rename failed")
        } catch (e: Exception) {
            Log.w(TAG, "Rename exception: ${e.localizedMessage}", e.cause)
            throw FileNotFoundException("Failed to rename document $documentId to $displayName: ${e.message}")
        }
        return destFile.getId()
    }

    override fun deleteDocument(documentId: String?) {
        "deleteDocument($documentId)".toLogV()
        if (documentId.isNullOrEmpty()) return
        if (!documentId.getFile().delete()) throw okio.FileNotFoundException("Failed to delete document with id: $documentId")
    }

    override fun removeDocument(documentId: String?, parentDocumentId: String?) {
        "removeDocument($documentId, $parentDocumentId)"
        if (documentId.isNullOrEmpty() || parentDocumentId.isNullOrEmpty()) return
        val parent = parentDocumentId.getFile()
        val file = documentId.getFile()
        val fileParent = file.parentFile
        if (fileParent==null) throw FileNotFoundException("Failed to remove document, no vaild parent for $documentId")

        if (parent == file || fileParent == parent) {
            if (!file.delete()) throw FileNotFoundException("Failed to delete document with id $documentId")
        } else throw FileNotFoundException("Failed to delete document with id $documentId")
    }

    fun copyDocument(sourceDocumentId: String?, sourceParentDocumentId: String?, targetParentDocumentId: String?): String? {
        if (!isChildDocument(sourceParentDocumentId, sourceDocumentId)) throw FileNotFoundException("Failed to copy document with id $sourceDocumentId parent is not $sourceDocumentId")
        return copyDocument(sourceDocumentId, targetParentDocumentId)
    }

    @SuppressLint("NewApi")
    override fun copyDocument(sourceDocumentId: String?, targetParentDocumentId: String?): String? {
        "copyDocument($sourceDocumentId, $targetParentDocumentId)".toLogV()
        if (sourceDocumentId.isNullOrEmpty() || targetParentDocumentId.isNullOrEmpty()) return null
        val parent = targetParentDocumentId.getFile()
        val oldFile = sourceDocumentId.getFile()
        val newFile = File(parent.path, oldFile.name)
        try {
            if (newFile.createNewFile() && newFile.setWritable(true) && newFile.setReadable(true)) throw FileNotFoundException("Failed to copy document $sourceDocumentId. could not create new file $newFile")
            FileInputStream(oldFile).transferTo(FileOutputStream(newFile))
        } catch (e: IOException) {
            Log.e(TAG, "Failed to copy document", e)
            throw FileNotFoundException("Failed to copy document $sourceDocumentId ${e.message}")
        }
        return newFile.getId()
    }

    override fun moveDocument(
        sourceDocumentId: String?,
        sourceParentDocumentId: String?,
        targetParentDocumentId: String?,
    ): String? {
        val newId = copyDocument(sourceDocumentId, sourceParentDocumentId, targetParentDocumentId)
        removeDocument(sourceDocumentId, sourceParentDocumentId)
        return newId
    }

    companion object {
        val TAG = "StorageProvider"

    }
    private fun MatrixCursor.includeFile(documentId: String?, inFile: File?): MatrixCursor {
        var docId = documentId
        var file = inFile
        if (docId == null) {
            docId = file?.getId()
        } else {
                file = docId.getFile()
        }

            var flags = 0x0000

        if (file!!.isDirectory) {
                if (file.isDirectory() && file.canWrite()) {
                    flags = flags or Document.FLAG_DIR_SUPPORTS_CREATE
                }
            } else if (file.canWrite()) {
                // If the file is writable set FLAG_SUPPORTS_WRITE and
                // FLAG_SUPPORTS_DELETE
                flags = flags or Document.FLAG_SUPPORTS_WRITE
                flags = flags or Document.FLAG_SUPPORTS_DELETE

                // Add SDK specific flags if appropriate
                flags = flags or Document.FLAG_SUPPORTS_RENAME
                if (SDK_INT >= Build.VERSION_CODES.N) {
                    flags = flags or Document.FLAG_SUPPORTS_REMOVE
                    flags = flags or Document.FLAG_SUPPORTS_MOVE
                    flags = flags or Document.FLAG_SUPPORTS_COPY
                }
            }
            Log.w(TAG, "current Flag $flags")
            var displayName = file.getName()
            var mimeType = file.getMimeType()

            if (mimeType.startsWith("image/")) {
                // Allow the image to be represented by a thumbnail rather than an icon
                flags = flags or Document.FLAG_SUPPORTS_THUMBNAIL
            }

            val row = this.newRow()
        row.add(Document.COLUMN_DOCUMENT_ID, docId)
        row.add(Document.COLUMN_DISPLAY_NAME, displayName)
        row.add(Document.COLUMN_SIZE, file.length())
        row.add(Document.COLUMN_MIME_TYPE, mimeType)
        row.add(Document.COLUMN_LAST_MODIFIED, file.lastModified())
        row.add(Document.COLUMN_FLAGS, Document.FLAG_DIR_SUPPORTS_CREATE)

        // Add a custom icon
            row.add(Document.COLUMN_ICON, R.drawable.ic_book_24dp)
        return this
    }

    private fun File.getId(): String {
        return this.absolutePath
    }
    private fun String.getFile(): File {
        return File(this)
    }
    private fun File.getMimeType(): String {
        if (isDirectory) return Document.MIME_TYPE_DIR
        val lastDot = name.lastIndexOf('.')
        if (lastDot >= 0) {
            val extension = name.substring(lastDot + 1)
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            if (mime != null) {
                return mime
            }
        }
        return "application/octet-stream"
    }
    private fun String.toLogV() {
        if (BuildConfig.DEBUG) Log.v(TAG, this)
    }
}



private fun Array<out String>?.resolveProjection(root: Boolean): MatrixCursor {
    if (this!=null) {
        return MatrixCursor(this)
    }
    if (root) return MatrixCursor(arrayOf(
        Root.COLUMN_ROOT_ID,
        Root.COLUMN_MIME_TYPES,
        Root.COLUMN_FLAGS,
        Root.COLUMN_ICON,
        Root.COLUMN_TITLE,
        Root.COLUMN_SUMMARY,
        Root.COLUMN_DOCUMENT_ID,
        Root.COLUMN_AVAILABLE_BYTES))
    else return MatrixCursor(arrayOf(
        Document.COLUMN_DOCUMENT_ID,
        Document.COLUMN_MIME_TYPE,
        Document.COLUMN_DISPLAY_NAME,
        Document.COLUMN_LAST_MODIFIED,
        Document.COLUMN_FLAGS,
        Document.COLUMN_SIZE))
}


