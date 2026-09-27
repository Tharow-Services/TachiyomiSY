package eu.kanade.tachiyomi.source

import android.content.Context
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.all.EHentai
import eu.kanade.tachiyomi.source.online.all.MergedSource
import exh.source.BlacklistedSources
import exh.source.DelegatedHttpSource
import exh.source.EH_SOURCE_ID
import exh.source.EXH_SOURCE_ID
import exh.source.EnhancedHttpSource
import exh.source.ExhPreferences
import exh.source.MERGED_SOURCE_ID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import tachiyomi.domain.source.model.StubSource
import tachiyomi.domain.source.repository.StubSourceRepository
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.source.local.LocalSource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

class AndroidSourceManager(
    private val context: Context,
    private val extensionManager: ExtensionManager,
    private val sourceRepository: StubSourceRepository,
) : SourceManager {

    private val _isInitialized = MutableStateFlow(false)
    override val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val downloadManager: DownloadManager by injectLazy()

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    private val sourcesMapFlow = MutableStateFlow(ConcurrentHashMap<Long, Source>())

    private val stubSourcesMap = ConcurrentHashMap<Long, StubSource>()

    override val sources: Flow<List<Source>> = sourcesMapFlow.map { it.values.toList() }

    // SY -->
    private val exhPreferences: ExhPreferences by injectLazy()
    private val sourcePreferences: SourcePreferences by injectLazy()
    // SY <--

    init {
        scope.launch {
            extensionManager.installedExtensionsFlow
                // SY -->
                .combine(exhPreferences.enableExhentai.changes()) { extensions, enableExhentai ->
                    extensions to enableExhentai
                }
                // SY <--
                .collectLatest { (extensions, enableExhentai) ->
                    val mutableMap: ConcurrentHashMap<Long, Source> = ConcurrentHashMap<Long, Source>(
                        mapOf(
                            LocalSource.ID to LocalSource(
                                context,
                                Injekt.get(),
                                Injekt.get(),
                                // SY -->
                                sourcePreferences.allowLocalSourceHiddenFolders::get,
                                // SY <--
                            ),
                        ),
                    )

                    mutableMap.apply {
                        // SY -->
                        put(EH_SOURCE_ID, EHentai(EH_SOURCE_ID, false, context))
                        if (enableExhentai) {
                            put(EXH_SOURCE_ID, EHentai(EXH_SOURCE_ID, true, context))
                        }
                        put(MERGED_SOURCE_ID, MergedSource())
                        // SY <--w
                    }

                    extensions.forEach { extension ->
                        extension.sources.map { it }.forEach {
                            mutableMap[it.id] = it
                            registerStubSource(StubSource.from(it))
                        }
                    }
                    sourcesMapFlow.value = mutableMap
                    _isInitialized.value = true
                }
        }

        scope.launch {
            sourceRepository.subscribeAll()
                .collectLatest { sources ->
                    val mutableMap = stubSourcesMap.toMutableMap()
                    sources.forEach {
                        mutableMap[it.id] = it
                    }
                }
        }
    }

    override fun get(sourceKey: Long): Source? {
        return sourcesMapFlow.value[sourceKey]
    }

    override fun getOrStub(sourceKey: Long): Source {
        return sourcesMapFlow.value[sourceKey] ?: stubSourcesMap.getOrPut(sourceKey) {
            runBlocking { createStubSource(sourceKey) }
        }
    }

    override fun getAll() = sourcesMapFlow.value.values.toList()

    override fun getOnlineSources() = sourcesMapFlow.value.values.filterIsInstance<HttpSource>()

    override fun getStubSources(): List<StubSource> {
        val onlineSourceIds = getOnlineSources().map { it.id }
        return stubSourcesMap.values.filterNot { it.id in onlineSourceIds }
    }

    // SY -->
    override fun getVisibleOnlineSources() = sourcesMapFlow.value.values
        .filterIsInstance<HttpSource>()
        .filter {
            it.id !in BlacklistedSources.HIDDEN_SOURCES
        }

    override fun getVisibleSources() = sourcesMapFlow.value.values
        .filter {
            it.id !in BlacklistedSources.HIDDEN_SOURCES
        }

    fun getDelegatedCatalogueSources() = sourcesMapFlow.value.values
        .filterIsInstance<EnhancedHttpSource>()
        .mapNotNull { enhancedHttpSource ->
            enhancedHttpSource.enhancedSource as? DelegatedHttpSource
        }
    // SY <--

    private fun registerStubSource(source: StubSource) {
        scope.launch {
            val dbSource = sourceRepository.getStubSource(source.id)
            if (dbSource == source) return@launch
            sourceRepository.upsertStubSource(source.id, source.lang, source.name)
            if (dbSource != null) {
                downloadManager.renameSource(dbSource, source)
            }
        }
    }

    private suspend fun createStubSource(id: Long): StubSource {
        sourceRepository.getStubSource(id)?.let {
            return it
        }
        extensionManager.getSourceData(id)?.let {
            registerStubSource(it)
            return it
        }
        return StubSource(id = id, lang = "", name = "")
    }

    // SY -->
    companion object {
        private const val fillInSourceId = Long.MAX_VALUE
    }

    private class ListenMutableMap<K, V>(
        private val internalMap: MutableMap<K, V>,
        private val listener: () -> Unit,
    ) : MutableMap<K, V> by internalMap {
        override fun clear() {
            val clearResult = internalMap.clear()
            listener()
            return clearResult
        }

        override fun put(key: K, value: V): V? {
            val putResult = internalMap.put(key, value)
            if (putResult == null) {
                listener()
            }
            return putResult
        }

        override fun putAll(from: Map<out K, V>) {
            internalMap.putAll(from)
            listener()
        }

        override fun remove(key: K): V? {
            val removeResult = internalMap.remove(key)
            if (removeResult != null) {
                listener()
            }
            return removeResult
        }
    }

    // SY <--
}
