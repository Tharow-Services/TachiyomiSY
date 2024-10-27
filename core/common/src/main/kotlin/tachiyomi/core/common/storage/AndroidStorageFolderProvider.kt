package tachiyomi.core.common.storage

import android.content.Context
import androidx.core.net.toUri


class AndroidStorageFolderProvider(
    private val context: Context,
) : FolderProvider {

    override fun directory() = context.filesDir

    override fun path() = directory().toUri().toString()
}
