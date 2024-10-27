package tachiyomi.domain.storage.service

import android.annotation.SuppressLint
import com.hippo.unifile.UniFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.storage.FolderProvider

class StoragePreferences(private val folderProvider: FolderProvider) {
    constructor(folderProvider: FolderProvider, ignored: StoragePreferences): this(folderProvider)

    @SuppressLint("SdCardPath")
    fun baseStorageDirectory(): Preference<String> = ConstantPreference(folderProvider)

    companion object {
        private class ConstantPreference(val folderProvider: FolderProvider): Preference<String> {
            override fun key() = "storage_dir"

            override fun get() = UniFile.fromFile(folderProvider.directory())?.uri.toString()

            override fun isSet() = true

            override fun delete() {}

            override fun defaultValue() = ""

            override fun changes(): Flow<String> = flow { get() }

            override fun stateIn(scope: CoroutineScope) = changes().stateIn(scope, SharingStarted.Eagerly, get())

            override fun set(value: String) {}

        }
    }

}
