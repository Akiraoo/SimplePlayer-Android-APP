@file:OptIn(
    ExperimentalMaterial3Api::class,
    UnstableApi::class
)

package com.akira.simpleplayer

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.annotation.SuppressLint
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.akira.simpleplayer.data.LyricLine
import com.akira.simpleplayer.data.SimplePlayerApi
import com.akira.simpleplayer.update.Updater
import com.akira.simpleplayer.data.Track
import com.akira.simpleplayer.playback.PlaybackService
import com.akira.simpleplayer.playback.SpectrumEngine
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.exp
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import com.akira.simpleplayer.AppExecutors
import com.akira.simpleplayer.R

import kotlin.math.sin

private val DarkBg = Color(0xFF141617)
private val DarkBar = Color(0xFF181A1C)
private val DarkPanel = Color(0xFF17191B)
private val DarkLine = Color(0xFF2B3034)
private val DarkText = Color(0xFFE4E6E8)
private val DarkMuted = Color(0xFF858B91)

private val LightBg = Color(0xFFE8E9EA)
private val LightBar = Color(0xFFEEF0F1)
private val LightPanel = Color(0xFFF4F5F5)
private val LightLine = Color(0xFFCFD2D5)
private val LightText = Color(0xFF232629)
private val LightMuted = Color(0xFF656B70)

private fun formatApiUrl(raw: String): String {
    val trimmed = raw.trim().trimEnd('/')
    if (trimmed.isBlank()) return ""
    return if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
        "http://$trimmed"
    } else {
        trimmed
    }
}

@OptIn(UnstableApi::class)
class PlayerViewModel : ViewModel() {
    private val prefs get() = AppPrefs.instance

    private val scrollPositions = mutableMapOf<String, Pair<Int, Int>>()

    fun getScrollPos(key: String): Pair<Int, Int> {
        return scrollPositions[key] ?: (prefs.scrollIndex(key) to prefs.scrollOffset(key))
    }

    fun saveScrollPos(key: String, index: Int, offset: Int) {
        scrollPositions[key] = index to offset
        prefs.saveScroll(key, index, offset)
    }

    var apiBase by mutableStateOf(formatApiUrl(prefs.apiBase))
        private set

    var library by mutableStateOf<List<Track>>(emptyList())
        private set

    var playlists by mutableStateOf<Map<String, List<String>>>(emptyMap())
        private set

    var selectedView by mutableStateOf(prefs.lastView())
        private set

    var query by mutableStateOf("")
        private set

    var visibleTracks by mutableStateOf<List<Track>>(emptyList())
        private set

    var current by mutableStateOf<Track?>(null)
        private set

    var lyrics by mutableStateOf<List<LyricLine>>(emptyList())
        private set

    var isPlaying by mutableStateOf(false)
        private set

    var position by mutableLongStateOf(0L)
        private set

    var duration by mutableLongStateOf(0L)
        private set

    var showPlaylistSheet by mutableStateOf(false)
    var showNowPlaying by mutableStateOf(false)
    var showSettings by mutableStateOf(false)
    var showCustomPlaylists by mutableStateOf(false)
    var showEqualizer by mutableStateOf(false)

    /** Custom share playlist picking. In select mode a tap on a track row highlights it instead of playing. */
    var selectMode by mutableStateOf(false)
    val customPicked = mutableStateListOf<String>()
    var customStartEditor by mutableStateOf(false)

    fun toggleCustomPick(id: String) {
        if (!customPicked.remove(id) && customPicked.size < CustomPlaylistStore.MAX_TRACKS) {
            customPicked.add(id)
        }
    }
    /** Bumped after every successful resync so cover images re-read their local files. */
    var syncVersion by mutableIntStateOf(0)
        private set

    private var syncJob: Job? = null
    private var syncToken = 0
    private var syncCommits = 0

    var loading by mutableStateOf(false)
        private set

    var loadingProgress by mutableFloatStateOf(0f)
        private set

    var loadingStage by mutableStateOf("準備中")
        private set

    var error by mutableStateOf<String?>(null)
        private set

    private var api: SimplePlayerApi? = null

    private fun getApi(): SimplePlayerApi? {
        if (apiBase.isBlank()) return null

        if (api == null) {
            val cacheDir = File(AppPrefs.context.filesDir, "simple-player-cache")
            api = SimplePlayerApi(apiBase, cacheDir)
        }

        return api
    }

    /** Cover from the local cache (filled by resync); null if not cached. */
    fun coverFile(id: String): File? = getApi()?.cachedCover(id)

    fun streamUrl(id: String): String = getApi()?.streamUrl(id) ?: ""

    private var controllerFuture: ListenableFuture<MediaController>? = null

    var controller: MediaController? = null
        private set

    private var ticker: Job? = null

    internal val spectrumEngine = SpectrumEngine.shared

    fun init() {
        Log.d("PlayerViewModel", "init() called")

        if (apiBase.isBlank()) {
            Log.d("PlayerViewModel", "init() skipped: apiBase is blank")
            return
        }

        if (controllerFuture != null && controller != null) return

        if (controllerFuture != null) {
            Log.d(
                "PlayerViewModel",
                "controllerFuture is not null, but controller is null. Resetting..."
            )
            controllerFuture = null
        }

        val token = SessionToken(
            AppPrefs.context,
            ComponentName(
                AppPrefs.context,
                PlaybackService::class.java
            )
        )

        controllerFuture = MediaController.Builder(
            AppPrefs.context,
            token
        ).buildAsync().also { future ->

            future.addListener({
                viewModelScope.launch {
                    val result = runCatching { future.get() }

                    if (result.isSuccess) {
                        val c = result.getOrNull()
                        controller = c

                        Log.d(
                            "PlayerViewModel",
                            "Controller successfully initialized: $c"
                        )

                        c?.addListener(object : Player.Listener {

                            override fun onMediaItemTransition(
                                mediaItem: MediaItem?,
                                reason: Int
                            ) {
                                mediaItem?.mediaId?.let {
                                    updateCurrentFromId(it)
                                }
                            }
                        })

                        restoreIntoController()
                    } else {
                        Log.e(
                            "PlayerViewModel",
                            "Failed to get controller from future: ${result.exceptionOrNull()}"
                        )

                        controller = null
                        controllerFuture = null
                    }

                    startTicker()
                }
            }, MoreExecutors.directExecutor())
        }

        loadLocal()

        if (ticker == null) {
            startTicker()
        }
    }

    fun updateApiBase(value: String) {
        val formatted = formatApiUrl(value)
        val changed = formatted != apiBase

        apiBase = formatted
        prefs.apiBase = formatted

        api =
            if (formatted.isNotBlank()) {
                SimplePlayerApi(
                    formatted,
                    File(
                        AppPrefs.context.filesDir,
                        "simple-player-cache"
                    )
                )
            } else {
                null
            }

        // Changing the server address is one of the two moments we sync.
        if (changed && formatted.isNotBlank()) {
            init()
            runSync(scanFirst = false)
        }
    }

    /** Read the local cache only. Never touches the network. */
    private fun loadLocal() {
        val activeApi = getApi() ?: return
        val commitsAtStart = syncCommits

        viewModelScope.launch {
            val (cachedLib, cachedPl) =
                withContext(AppExecutors.io) {
                    runCatching {
                        activeApi.cachedLibrary() to activeApi.cachedPlaylists()
                    }.getOrDefault(null to null)
                }

            // A resync finished while we were reading; its data is newer.
            if (commitsAtStart != syncCommits) return@launch

            if (cachedLib != null) {
                library = cachedLib
            }

            if (cachedPl != null) {
                playlists = cachedPl
            }

            rebuildVisible()

            if (current == null && cachedLib != null) {
                val savedId = prefs.lastTrackId()
                val saved = cachedLib.firstOrNull { it.id == savedId }

                if (saved != null) {
                    current = saved
                    position = prefs.lastPosition()
                    duration = prefs.lastDuration()
                    restoreIntoController()

                    val cachedMeta =
                        withContext(AppExecutors.io) {
                            activeApi.cachedMetadata(saved.id)
                        }

                    if (cachedMeta != null) {
                        lyrics = cachedMeta.second
                    }
                }
            }
        }
    }

    // ---------- App update (GitHub Releases) ----------

    var updateRelease by mutableStateOf<Updater.Release?>(null)
        private set

    var updateMessage by mutableStateOf<String?>(null)
        private set

    var updateChecking by mutableStateOf(false)
        private set

    /** -1 = not downloading, 0..1 = download progress. */
    var updateProgress by mutableFloatStateOf(-1f)
        private set

    val installedVersion: String
        get() = Updater.installedVersion(AppPrefs.context)

    fun checkUpdate() {
        if (updateChecking || updateProgress >= 0f) return

        viewModelScope.launch {
            updateChecking = true
            updateMessage = null
            updateRelease = null

            try {
                val r = Updater.latest()

                when {
                    r == null ->
                        updateMessage = "找不到發布版本"

                    Updater.isNewer(r.version, installedVersion) -> {
                        updateRelease = r
                        updateMessage = "發現新版本 ${r.version}"
                    }

                    else ->
                        updateMessage = "已是最新版本"
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                updateMessage =
                    "檢查更新失敗：" +
                            (e.localizedMessage ?: e.javaClass.simpleName)
            } finally {
                updateChecking = false
            }
        }
    }

    fun installUpdate() {
        val release = updateRelease ?: return
        if (updateProgress >= 0f) return

        val ctx = AppPrefs.context

        if (!Updater.canInstall(ctx)) {
            updateMessage = "請先允許本 App 安裝應用程式，返回後再按一次"
            Updater.openInstallPermission(ctx)
            return
        }

        viewModelScope.launch {
            updateProgress = 0f
            updateMessage = "下載中…"

            try {
                val apk =
                    Updater.download(ctx, release) {
                        updateProgress = it
                    }

                updateMessage = "下載完成，請在系統畫面確認安裝"
                Updater.install(ctx, apk)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                updateMessage =
                    "更新失敗：" +
                            (e.localizedMessage ?: e.javaClass.simpleName)
            } finally {
                updateProgress = -1f
            }
        }
    }

    /** Manual resync (the Resync button): ask the server to rescan first, then pull the result. */
    fun resync() {
        runSync(scanFirst = true)
    }

    fun scan() {
        runSync(scanFirst = true)
    }

    private fun runSync(scanFirst: Boolean) {
        val activeApi = getApi() ?: return

        // A newer request (e.g. server address changed) replaces a running one.
        syncJob?.cancel()
        val token = ++syncToken

        syncJob = viewModelScope.launch {
            loading = true
            loadingProgress = 0.03f
            loadingStage = if (scanFirst) "掃描音樂庫" else "取得最新清單"
            error = null

            try {
                if (scanFirst) {
                    try {
                        activeApi.scan()
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        throw Exception(
                            "伺服器掃描失敗：" +
                                    (e.localizedMessage ?: e.javaClass.simpleName)
                        )
                    }
                    loadingStage = "取得最新清單"
                }

                val result = activeApi.sync { done, total ->
                    if (token == syncToken) {
                        loadingProgress =
                            if (total == 0) 0.9f
                            else 0.1f + 0.85f * done / total
                        loadingStage =
                            if (total == 0) "整理本機快取"
                            else "同步歌曲資料 ($done/$total)"
                    }
                }

                if (token != syncToken) return@launch

                library = result.tracks
                playlists = result.playlists
                syncCommits++
                syncVersion++

                rebuildVisible()

                // Pick up refreshed metadata for the track that is currently shown.
                current?.let { c ->
                    result.tracks.firstOrNull { it.id == c.id }?.let { current = it }

                    val meta =
                        withContext(AppExecutors.io) {
                            activeApi.cachedMetadata(c.id)
                        }

                    if (meta != null && current?.id == c.id) {
                        lyrics = meta.second
                    }
                }

                loadingProgress = 1f
                loadingStage = "完成"

                if (result.failed > 0) {
                    error = "${result.failed} 首歌曲的資料同步失敗，可再按一次重新同步"
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                if (token == syncToken) {
                    error =
                        e.localizedMessage
                            ?: e.message
                            ?: "同步失敗，請檢查伺服器位址或網路"
                }
            } finally {
                if (token == syncToken) {
                    loading = false
                }
            }
        }
    }

    val visible: List<Track>
        get() = visibleTracks

    @Volatile
    private var filterGeneration = 0L

    private fun rebuildVisible() {
        val source = library

        val playlistIds =
            if (selectedView == "Library") {
                null
            } else {
                playlists[selectedView]?.toSet() ?: emptySet()
            }

        val q = query.trim()
        val generation = ++filterGeneration

        viewModelScope.launch(AppExecutors.cpu) {
            val result = source.filter { t ->
                val matchesPlaylist =
                    playlistIds == null || t.id in playlistIds

                if (!matchesPlaylist) {
                    return@filter false
                }

                if (q.isEmpty()) {
                    return@filter true
                }

                t.title.contains(q, ignoreCase = true) ||
                        t.name.contains(q, ignoreCase = true) ||
                        t.artist.contains(q, ignoreCase = true) ||
                        t.album.contains(q, ignoreCase = true) ||
                        t.folder.contains(q, ignoreCase = true) ||
                        t.id.contains(q, ignoreCase = true)
            }

            withContext(
                kotlinx.coroutines.Dispatchers.Main.immediate
            ) {
                if (generation == filterGeneration) {
                    visibleTracks = result
                }
            }
        }
    }

    fun updateQuery(value: String) {
        query = value
        rebuildVisible()
    }

    fun selectView(name: String) {
        selectedView = name
        prefs.saveLastView(name)
        rebuildVisible()
    }

    fun play(track: Track) {
        current = track
        lyrics = emptyList()

        position =
            if (prefs.lastTrackId() == track.id) {
                prefs.lastPosition()
            } else {
                0L
            }

        duration = 0L

        val activeApi = getApi()

        controller?.let { c ->
            val (items, index) = buildQueue(track, activeApi)

            c.setMediaItems(
                items,
                index,
                position.coerceAtLeast(0L)
            )

            c.prepare()
            c.play()
        }

        loadLyricsFromCache(track.id)

        startTicker()
    }

    /** Queue for [track]: the list it is shown in, falling back to its playlist / the library. */
    private fun buildQueue(
        track: Track,
        activeApi: SimplePlayerApi?
    ): Pair<List<MediaItem>, Int> {
        val inView =
            if (selectedView == "Library") {
                library
            } else {
                val ids = playlists[selectedView].orEmpty().toHashSet()
                library.filter { it.id in ids }
            }

        val source =
            when {
                visibleTracks.any { it.id == track.id } -> visibleTracks
                inView.any { it.id == track.id } -> inView
                else -> listOf(track)
            }

        val items = source.map { t ->
            val sUrl =
                activeApi?.streamUrl(t.id) ?: ""

            val cFile =
                if (t.hasCover) activeApi?.cachedCover(t.id) else null

            MediaItem.Builder()
                .setMediaId(t.id)
                .setUri(Uri.parse(sUrl))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(t.title)
                        .setArtist(t.artist)
                        .setAlbumTitle(t.album)
                        .apply {
                            if (cFile != null) {
                                setArtworkUri(
                                    Uri.fromFile(cFile)
                                )
                            }
                        }
                        .build()
                )
                .build()
        }

        val index =
            source.indexOfFirst {
                it.id == track.id
            }.coerceAtLeast(0)

        return items to index
    }

    /**
     * After the app is reopened the playback service is usually gone, so the controller is empty
     * while the UI already shows the last track. Put that track back (at the saved position) so the
     * mini player's play button works. Not prepared: nothing is downloaded until the user presses play.
     * Does nothing if the service is still alive with its own queue.
     */
    private fun restoreIntoController() {
        val c = controller ?: return
        val track = current ?: return

        if (c.mediaItemCount > 0) return

        val (items, index) = buildQueue(track, getApi())
        if (items.isEmpty()) return

        val start =
            if (prefs.lastTrackId() == track.id) prefs.lastPosition() else 0L

        c.setMediaItems(items, index, start.coerceAtLeast(0L))
    }

    private fun updateCurrentFromId(id: String) {
        val track =
            library.firstOrNull { it.id == id }
                ?: visibleTracks.firstOrNull { it.id == id }
                ?: return

        if (current?.id == id) {
            return
        }

        current = track
        lyrics = emptyList()
        position = 0L
        duration = 0L

        loadLyricsFromCache(id)
    }

    /**
     * Lyrics come from the local metadata cache (filled by resync).
     * Only if this track has never been cached do we fetch it once, so playback
     * of a track missing from the cache is not left without lyrics.
     */
    private fun loadLyricsFromCache(id: String) {
        val activeApi = getApi() ?: return

        val cached = activeApi.cachedMetadata(id)

        if (cached != null) {
            lyrics = cached.second
            return
        }

        viewModelScope.launch {
            runCatching {
                val loaded = activeApi.metadata(id).second

                if (current?.id == id) {
                    lyrics = loaded
                }
            }
        }
    }

    fun togglePlay() {
        val c = controller ?: return

        if (c.isPlaying) {
            c.pause()
            return
        }

        if (c.mediaItemCount == 0) {
            current?.let { play(it) }
            return
        }

        if (c.playbackState == Player.STATE_IDLE) {
            c.prepare()
        }

        c.play()
    }

    fun seekTo(ms: Long) {
        val target = ms.coerceAtLeast(0L)
        position = target
        controller?.seekTo(target)
    }

    fun next(direction: Int) {
        val list = visible

        if (list.isEmpty()) {
            return
        }

        val index =
            list.indexOfFirst {
                it.id == current?.id
            }

        val nextIndex =
            if (index < 0) {
                0
            } else {
                (index + direction + list.size) % list.size
            }

        play(list[nextIndex])
    }

    private var lastSavedSecond = -1L

    private fun startTicker() {
        if (ticker != null) {
            return
        }

        ticker = viewModelScope.launch {
            while (true) {
                var wait = 300L

                try {
                    controller?.let { c ->
                        val playing = c.isPlaying

                        isPlaying = playing

                        // An empty controller (fresh service after the app was closed) reports
                        // position 0. Never let that overwrite the restored / saved position.
                        val holdsCurrent =
                            c.mediaItemCount > 0 &&
                                    current != null &&
                                    c.currentMediaItem?.mediaId == current?.id

                        if (holdsCurrent) {
                            position = c.currentPosition.coerceAtLeast(0L)
                            if (c.duration > 0L) {
                                duration = c.duration
                            }
                        }

                        val loaded =
                            holdsCurrent &&
                                    c.playbackState != Player.STATE_IDLE

                        val second = position / 1000L
                        if (loaded && second != lastSavedSecond) {
                            lastSavedSecond = second
                            current?.let {
                                prefs.savePlayback(
                                    it.id,
                                    position,
                                    duration
                                )
                            }
                        }

                        wait =
                            when {
                                !playing -> 300L
                                showNowPlaying -> 50L
                                else -> 250L
                            }
                    }
                } catch (e: Exception) {
                    Log.e(
                        "PlayerViewModel",
                        "Ticker error: ${e.message}",
                        e
                    )

                    controller = null
                    controllerFuture = null
                    isPlaying = false
                    init()
                }

                delay(wait)
            }
        }
    }

    fun requestShare(track: Track) {
        share(track)
    }

    /** Opens the Android system share sheet with the track's link. */
    fun share(track: Track) {
        val url =
            getApi()?.webShareUrl(track.id) ?: return

        val send =
            Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, url)
                .putExtra(
                    Intent.EXTRA_SUBJECT,
                    track.title.ifBlank { track.name }
                )
                .putExtra(
                    Intent.EXTRA_TITLE,
                    track.title.ifBlank { track.name }
                )

        AppPrefs.context.startActivity(
            Intent.createChooser(send, null)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun download(track: Track) {
        val url =
            getApi()?.webDirectUrl(track.id) ?: return

        val intent =
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(url)
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        AppPrefs.context.startActivity(intent)
    }

    /** Web link of a custom share playlist (/c/<id>), on the same host as the /p/ links. */
    fun customPlaylistUrl(id: String): String {
        val p = getApi()?.webPlaylistUrl(id)
        val i = p?.lastIndexOf("/p/") ?: -1
        return if (p != null && i >= 0) {
            p.substring(0, i) + "/c/" + p.substring(i + 3)
        } else {
            apiBase.trimEnd('/') + "/c/" + id
        }
    }

    fun sharePlaylist(name: String) {
        val url =
            getApi()?.webPlaylistUrl(name) ?: return

        val intent =
            Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, url)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        AppPrefs.context.startActivity(
            Intent.createChooser(
                intent,
                "分享播放清單"
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    override fun onCleared() {
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }

        ticker?.cancel()
        spectrumEngine.stop()

        super.onCleared()
    }
}

class AppPrefs private constructor() {

    @SuppressLint("StaticFieldLeak")
    companion object {
        lateinit var context: Context
            private set

        lateinit var instance: AppPrefs
            private set

        lateinit var imageLoader: ImageLoader
            private set

        fun init(context: Context) {
            this.context = context.applicationContext
            instance = AppPrefs()

            // Covers now live in simple-player-cache/cover-files (managed by resync).
            // Remove the old Coil network cache that used to take up to 256 MB.
            Thread {
                File(this.context.filesDir, "simple-player-cache/covers")
                    .takeIf { it.exists() }
                    ?.deleteRecursively()
            }.apply { isDaemon = true }.start()

            imageLoader =
                ImageLoader.Builder(this.context)
                    .memoryCache {
                        MemoryCache.Builder(this.context)
                            .maxSizePercent(0.20)
                            .build()
                    }
                    .crossfade(false)
                    .build()
        }
    }

    private val prefs
        get() = context.getSharedPreferences(
            "simple_player",
            0
        )

    var apiBase: String
        get() =
            prefs.getString("apiBase", "") ?: ""
        set(value) {
            prefs.edit {
                putString("apiBase", value)
            }
        }

    var lightTheme: Boolean
        get() =
            prefs.getBoolean("lightTheme", false)
        set(value) {
            prefs.edit {
                putBoolean("lightTheme", value)
            }
        }

    fun scrollIndex(key: String): Int =
        prefs.getInt(
            "scroll_index_$key",
            0
        )

    fun scrollOffset(key: String): Int =
        prefs.getInt(
            "scroll_offset_$key",
            0
        )

    fun saveScroll(
        key: String,
        index: Int,
        offset: Int
    ) {
        prefs.edit {
            putInt(
                "scroll_index_$key",
                index
            )
            putInt(
                "scroll_offset_$key",
                offset
            )
        }
    }

    fun lastView(): String =
        prefs.getString(
            "last_view",
            "Library"
        ) ?: "Library"

    fun saveLastView(value: String) {
        prefs.edit {
            putString(
                "last_view",
                value
            )
        }
    }

    fun lastTrackId(): String =
        prefs.getString(
            "last_track",
            ""
        ) ?: ""

    fun lastPosition(): Long =
        prefs.getLong(
            "last_position",
            0L
        )

    fun lastDuration(): Long =
        prefs.getLong(
            "last_duration",
            0L
        )

    fun savePlayback(
        id: String,
        position: Long,
        duration: Long
    ) {
        prefs.edit {
            putString("last_track", id)
            putLong("last_position", position)
            putLong("last_duration", duration)
        }
    }
}

enum class MobilePage {
    PLAYLISTS,
    SONGS
}

class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        AppPrefs.init(this)
        com.akira.simpleplayer.playback.EqStore.init(this)

        setContent {
            PlayerRoot()
        }
    }
}

@Composable
fun PlayerRoot(
    vm: PlayerViewModel = viewModel()
) {
    val lifecycleOwner =
        LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (
                    event ==
                    Lifecycle.Event.ON_RESUME
                ) {
                    vm.init()
                }
            }

        lifecycleOwner.lifecycle.addObserver(
            observer
        )

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(
                observer
            )
        }
    }

    var light by remember {
        mutableStateOf(
            AppPrefs.instance.lightTheme
        )
    }

    SimplePlayerTheme(light) {
        PlayerApp(
            vm,
            light
        ) {
            light = !light
            AppPrefs.instance.lightTheme = light
        }
    }
}

@Composable
fun SimplePlayerTheme(
    light: Boolean,
    content: @Composable () -> Unit
) {
    val colors =
        if (light) {
            lightColorScheme(
                primary = Color(0xFF202326),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFD9DADC),
                onPrimaryContainer = Color(0xFF17191B),
                secondary = Color(0xFF555B60),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFE0E2E3),
                onSecondaryContainer = Color(0xFF232629),
                tertiary = Color(0xFF44494D),
                onTertiary = Color.White,
                tertiaryContainer = Color(0xFFD8DADC),
                onTertiaryContainer = Color(0xFF202326),
                background = LightBg,
                onBackground = LightText,
                surface = LightBar,
                onSurface = LightText,
                surfaceVariant = LightPanel,
                onSurfaceVariant = LightMuted,
                outline = LightLine,
                outlineVariant = Color(0xFFB9BDC0),
                inverseSurface = Color(0xFF2A2D30),
                inverseOnSurface = Color(0xFFF0F1F2),
                inversePrimary = Color(0xFFD9DADC),
                scrim = Color.Black,
                surfaceTint = Color.Transparent,
                surfaceDim = Color(0xFFD8DADB),
                surfaceBright = Color(0xFFF8F9F9),
                surfaceContainerLowest = Color.White,
                surfaceContainerLow = Color(0xFFF1F2F2),
                surfaceContainer = Color(0xFFECEDEE),
                surfaceContainerHigh = Color(0xFFE5E7E8),
                surfaceContainerHighest = Color(0xFFDEE0E1)
            )
        } else {
            darkColorScheme(
                primary = Color(0xFFE1E3E5),
                onPrimary = Color(0xFF151718),
                primaryContainer = Color(0xFF303336),
                onPrimaryContainer = Color(0xFFF1F2F3),
                secondary = Color(0xFFB6BABE),
                onSecondary = Color(0xFF17191B),
                secondaryContainer = Color(0xFF36393C),
                onSecondaryContainer = Color(0xFFE5E7E8),
                tertiary = Color(0xFF9EA3A7),
                onTertiary = Color(0xFF181A1C),
                tertiaryContainer = Color(0xFF303336),
                onTertiaryContainer = Color(0xFFE1E3E5),
                background = DarkBg,
                onBackground = DarkText,
                surface = DarkBar,
                onSurface = DarkText,
                surfaceVariant = DarkPanel,
                onSurfaceVariant = DarkMuted,
                outline = DarkLine,
                outlineVariant = Color(0xFF42474B),
                inverseSurface = Color(0xFFE7E8E9),
                inverseOnSurface = Color(0xFF1B1D1F),
                inversePrimary = Color(0xFF42464A),
                scrim = Color.Black,
                surfaceTint = Color.Transparent,
                surfaceDim = Color(0xFF101112),
                surfaceBright = Color(0xFF2C2F32),
                surfaceContainerLowest = Color(0xFF101112),
                surfaceContainerLow = Color(0xFF181A1C),
                surfaceContainer = Color(0xFF1D2022),
                surfaceContainerHigh = Color(0xFF232629),
                surfaceContainerHighest = Color(0xFF292C2F)
            )
        }

    MaterialTheme(
        colorScheme = colors
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            LocalContentColor provides
                    if (light) LightText else DarkText,
            content = content
        )
    }
}

@Composable
fun PlayerApp(
    vm: PlayerViewModel,
    light: Boolean,
    onTheme: () -> Unit
) {
    LaunchedEffect(vm.apiBase) {
        if (vm.apiBase.isNotBlank()) {
            vm.init()
        }
    }

    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = false
        }
    }

    val sheetScope = rememberCoroutineScope()
    val sheet = remember(vm, sheetScope) {
        NowSheetController(vm, sheetScope)
    }

    LaunchedEffect(vm.showNowPlaying) {
        sheet.animateTo(vm.showNowPlaying)
    }

    val showMini by remember(vm, sheet) {
        derivedStateOf {
            vm.current != null &&
                    !(vm.showNowPlaying &&
                            sheet.progress.value >= 0.999f)
        }
    }

    val configuration =
        LocalConfiguration.current

    val wide =
        configuration.screenWidthDp >= 800

    var localBase by remember(vm.apiBase) {
        mutableStateOf(vm.apiBase)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.background
            )
            .windowInsetsPadding(
                WindowInsets.safeDrawing
            )
    ) {
        Column(
            Modifier.fillMaxSize()
        ) {
            TopBar(vm, light, onTheme)

            if (
                vm.apiBase.isNotBlank() &&
                !(vm.loading && vm.library.isEmpty())
            ) {
                SyncStatus(vm)
            }

            if (vm.apiBase.isBlank()) {
                SetupCard(localBase) {
                    vm.updateApiBase(it)
                }
            } else if (
                vm.loading &&
                vm.library.isEmpty()
            ) {
                InitialLoadingScreen(vm)
            } else if (wide) {
                DesktopLayout(vm)
            } else {
                MobileLayout(vm)
            }
        }

        if (showMini && !vm.selectMode) {
            Box(
                Modifier.align(
                    Alignment.BottomCenter
                )
            ) {
                MiniPlayer(vm, sheet)
            }
        }

        if (vm.selectMode) {
            Box(
                Modifier.align(
                    Alignment.BottomCenter
                )
            ) {
                CustomSelectBar(vm)
            }
        }

        if (vm.showCustomPlaylists) {
            CustomPlaylistScreen(vm) {
                vm.showCustomPlaylists = false
            }
        }
        if (vm.showPlaylistSheet) {
            PlaylistSheet(vm)
        }

        NowPlayingHost(vm, sheet)

        if (vm.showEqualizer) {
            EqualizerScreen {
                vm.showEqualizer = false
            }
        }

        if (vm.showSettings) {
            AlertDialog(
                onDismissRequest = {
                    vm.showSettings = false
                },
                title = {
                    Text(
                        "伺服器設定",
                        color = appText()
                    )
                },
                text = {
                    Column(
                        verticalArrangement =
                            Arrangement.spacedBy(14.dp)
                    ) {
                    OutlinedTextField(
                        value = localBase,
                        onValueChange = {
                            localBase = it
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text(
                                "API 位址",
                                color = appText()
                            )
                        },
                        placeholder = {
                            Text(
                                "http://伺服器:55555",
                                color = appMuted()
                            )
                        },
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                focusedTextColor = appText(),
                                unfocusedTextColor = appText(),
                                focusedBorderColor = appText(),
                                unfocusedBorderColor =
                                    appMuted().copy(
                                        alpha = .45f
                                    ),
                                cursorColor = appText()
                            )
                    )

                    OutlinedButton(
                        onClick = {
                            vm.showSettings = false
                            vm.showEqualizer = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "等化器",
                            color = appText()
                        )
                    }

                    UpdateSection(vm)
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            vm.updateApiBase(localBase)
                            vm.showSettings = false
                        }
                    ) {
                        Text(
                            "儲存",
                            color = appText()
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            vm.showSettings = false
                        }
                    ) {
                        Text(
                            "取消",
                            color = appMuted()
                        )
                    }
                },
                containerColor =
                    MaterialTheme.colorScheme.surface
            )
        }
    }
}

@Composable
fun TopBar(
    vm: PlayerViewModel,
    light: Boolean,
    onTheme: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(58.dp)
                .padding(horizontal = 10.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.LibraryMusic,
                null,
                Modifier.size(24.dp),
                tint = appText()
            )

            Spacer(
                Modifier.width(10.dp)
            )

            Text(
                "Simple Player",
                color = appText(),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                enabled =
                    vm.apiBase.isNotBlank() &&
                            !vm.loading,
                onClick = {
                    vm.resync()
                }
            ) {
                Icon(
                    Icons.Default.Refresh,
                    "重新同步",
                    tint =
                        if (
                            vm.apiBase.isNotBlank() &&
                            !vm.loading
                        ) {
                            appText()
                        } else {
                            appMuted().copy(alpha = .4f)
                        }
                )
            }

            IconButton(
                onClick = onTheme
            ) {
                Icon(
                    if (light)
                        Icons.Default.DarkMode
                    else
                        Icons.Default.LightMode,
                    "切換主題",
                    tint = appText()
                )
            }

            IconButton(
                onClick = {
                    vm.showSettings = true
                }
            ) {
                Icon(
                    Icons.Default.Settings,
                    "設定",
                    tint = appText()
                )
            }
        }
    }
}

@Composable
fun UpdateSection(
    vm: PlayerViewModel
) {
    val release = vm.updateRelease
    val downloading = vm.updateProgress >= 0f

    Column(
        verticalArrangement =
            Arrangement.spacedBy(6.dp)
    ) {
        Text(
            "應用程式更新",
            color = appText(),
            fontWeight = FontWeight.SemiBold
        )

        Text(
            "目前版本 ${vm.installedVersion}",
            color = appMuted(),
            style =
                MaterialTheme.typography.bodySmall
        )

        vm.updateMessage?.let {
            Text(
                it,
                color = appText(),
                style =
                    MaterialTheme.typography.bodySmall
            )
        }

        if (release != null && release.notes.isNotBlank()) {
            Text(
                release.notes,
                color = appMuted(),
                style =
                    MaterialTheme.typography.bodySmall,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (downloading) {
            LinearProgressIndicator(
                progress = {
                    vm.updateProgress.coerceIn(0f, 1f)
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                color = appText(),
                trackColor =
                    appMuted().copy(alpha = .22f)
            )
        }

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            TextButton(
                enabled =
                    !vm.updateChecking && !downloading,
                onClick = { vm.checkUpdate() }
            ) {
                Text(
                    if (vm.updateChecking) "檢查中…" else "檢查更新",
                    color = appText()
                )
            }

            if (release != null) {
                TextButton(
                    enabled = !downloading,
                    onClick = { vm.installUpdate() }
                ) {
                    Text(
                        "下載並安裝",
                        color = appText(),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SyncStatus(
    vm: PlayerViewModel
) {
    Column(
        Modifier.fillMaxWidth()
    ) {
        AnimatedVisibility(
            vm.loading
        ) {
            Column {
                LinearProgressIndicator(
                    progress = {
                        vm.loadingProgress.coerceIn(
                            0f,
                            1f
                        )
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                    color = appText(),
                    trackColor =
                        appMuted().copy(alpha = .22f)
                )

                Text(
                    vm.loadingStage,
                    color = appMuted(),
                    style =
                        MaterialTheme.typography.labelSmall,
                    modifier =
                        Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 4.dp
                        )
                )
            }
        }

        vm.error?.let {
            Text(
                it,
                color =
                    MaterialTheme.colorScheme.error,
                style =
                    MaterialTheme.typography.bodySmall,
                modifier =
                    Modifier.padding(
                        horizontal = 12.dp,
                        vertical = 6.dp
                    )
            )
        }
    }
}

@Composable
fun SetupCard(
    value: String,
    onSave: (String) -> Unit
) {
    var text by remember(value) {
        mutableStateOf(value)
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(22.dp),
        verticalArrangement =
            Arrangement.Center,
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.LibraryMusic,
            null,
            Modifier.size(56.dp),
            tint = appText()
        )

        Spacer(
            Modifier.height(14.dp)
        )

        Text(
            "連接 Simple Player",
            color = appText(),
            style =
                MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            Modifier.height(8.dp)
        )

        Text(
            "輸入 API 伺服器位址:55555",
            color = appMuted(),
            style =
                MaterialTheme.typography.bodyMedium
        )

        Spacer(
            Modifier.height(18.dp)
        )

        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
            },
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp),
            singleLine = true,
            label = {
                Text(
                    "API 位址",
                    color = appText()
                )
            },
            placeholder = {
                Text(
                    "http://伺服器:55555",
                    color = appMuted()
                )
            },
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedTextColor = appText(),
                    unfocusedTextColor = appText(),
                    focusedBorderColor = appText(),
                    unfocusedBorderColor =
                        appMuted().copy(
                            alpha = .5f
                        ),
                    focusedLabelColor = appText(),
                    unfocusedLabelColor =
                        appMuted(),
                    cursorColor = appText()
                )
        )

        Spacer(
            Modifier.height(14.dp)
        )

        Button(
            onClick = {
                if (
                    text.trim().isNotBlank()
                ) {
                    onSave(text.trim())
                }
            },
            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        MaterialTheme.colorScheme.onSurface,
                    contentColor =
                        MaterialTheme.colorScheme.background
                )
        ) {
            Text("連線")
        }
    }
}

@Composable
fun InitialLoadingScreen(
    vm: PlayerViewModel
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement =
            Arrangement.Center,
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.LibraryMusic,
            null,
            Modifier.size(54.dp),
            tint = appText()
        )

        Spacer(
            Modifier.height(18.dp)
        )

        Text(
            "正在載入音樂",
            color = appText(),
            style =
                MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(
            Modifier.height(8.dp)
        )

        Text(
            vm.loadingStage,
            color = appMuted(),
            style =
                MaterialTheme.typography.bodyMedium
        )

        Spacer(
            Modifier.height(18.dp)
        )

        LinearProgressIndicator(
            progress = {
                vm.loadingProgress.coerceIn(
                    0f,
                    1f
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .height(4.dp),
            color = appText(),
            trackColor =
                appMuted().copy(alpha = .22f)
        )

        Spacer(
            Modifier.height(7.dp)
        )

        Text(
            "${(vm.loadingProgress.coerceIn(0f, 1f) * 100).roundToInt()}%",
            color = appMuted(),
            style =
                MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun DesktopLayout(
    vm: PlayerViewModel
) {
    Row(
        Modifier.fillMaxSize()
    ) {
        Sidebar(vm)

        MainLibrary(
            vm,
            Modifier
                .weight(1f)
                .fillMaxHeight()
        )
    }
}

@Composable
private fun appText(): Color =
    if (AppPrefs.instance.lightTheme) {
        LightText
    } else {
        DarkText
    }

@Composable
private fun appMuted(): Color =
    if (AppPrefs.instance.lightTheme) {
        LightMuted
    } else {
        DarkMuted
    }

@Composable
fun MobileLayout(
    vm: PlayerViewModel
) {
    var page by rememberSaveable {
        mutableStateOf(MobilePage.PLAYLISTS)
    }

    // Horizontal offset (px) of the song list while it is being swiped back.
    // The playlist grid is drawn underneath during the swipe, so when the swipe
    // finishes the page switch needs no second animation.
    val backSwipe = remember { Animatable(0f) }
    var instantBack by remember { mutableStateOf(false) }

    val screenWidthPx =
        with(LocalDensity.current) {
            LocalConfiguration.current.screenWidthDp.dp.toPx()
        }

    BackHandler(enabled = page == MobilePage.SONGS) {
        page = MobilePage.PLAYLISTS
    }

    LaunchedEffect(page) {
        if (page == MobilePage.PLAYLISTS) {
            backSwipe.snapTo(0f)
            instantBack = false
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (page == MobilePage.SONGS && backSwipe.value > 0f) {
            val p = (backSwipe.value / screenWidthPx).coerceIn(0f, 1f)

            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Same parallax as the tap-back transition: starts 1/3 to the left.
                        translationX = -(screenWidthPx / 3f) * (1f - p)
                    }
            ) {
                PlaylistGrid(vm) { name ->
                    vm.selectView(name)
                    page = MobilePage.SONGS
                }

                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = .25f * (1f - p)))
                )
            }
        }

        AnimatedContent(
            targetState = page,
            transitionSpec = {
                if (instantBack) {
                    EnterTransition.None.togetherWith(ExitTransition.None)
                } else run {
                val forward = targetState == MobilePage.SONGS
                val duration = 280
                if (forward) {
                    (
                        slideInHorizontally(
                            animationSpec = tween(duration, easing = SheetEasing),
                            initialOffsetX = { it }
                        ) + fadeIn(tween(duration))
                    ).togetherWith(
                        slideOutHorizontally(
                            animationSpec = tween(duration, easing = SheetEasing),
                            targetOffsetX = { -it / 3 }
                        ) + fadeOut(tween(duration))
                    )
                } else {
                    (
                        slideInHorizontally(
                            animationSpec = tween(duration, easing = SheetEasing),
                            initialOffsetX = { -it / 3 }
                        ) + fadeIn(tween(duration))
                    ).togetherWith(
                        slideOutHorizontally(
                            animationSpec = tween(duration, easing = SheetEasing),
                            targetOffsetX = { it }
                        ) + fadeOut(tween(duration))
                    )
                }
                }
            },
            label = "mobilePage"
        ) { currentPage ->
            when (currentPage) {
                MobilePage.PLAYLISTS ->
                    PlaylistGrid(vm) { name ->
                        vm.selectView(name)
                        page = MobilePage.SONGS
                    }

                MobilePage.SONGS ->
                    SongListPage(
                        vm,
                        offsetX = backSwipe,
                        onSwipedBack = {
                            instantBack = true
                            page = MobilePage.PLAYLISTS
                        }
                    ) {
                        page = MobilePage.PLAYLISTS
                    }
            }
        }
    }
}

@Composable
fun PlaylistGrid(
    vm: PlayerViewModel,
    onOpen: (String) -> Unit
) {
    val entries =
        remember(
            vm.playlists,
            vm.library
        ) {
            val byId =
                vm.library.associateBy {
                    it.id
                }

            buildList {
                add(
                    "Library" to vm.library
                )

                vm.playlists.forEach {
                        (name, ids) ->
                    val first =
                        ids.asSequence()
                            .mapNotNull(
                                byId::get
                            )
                            .firstOrNull()

                    add(
                        name to
                                if (
                                    first == null
                                ) {
                                    emptyList()
                                } else {
                                    listOf(first)
                                }
                    )
                }
            }
        }

    // This page leaves composition while a playlist is open, so keep its scroll
    // position in the ViewModel (and prefs) like SongListPage does.
    val gridKey = "__playlist_grid__"

    val gridState =
        remember {
            val (idx, off) = vm.getScrollPos(gridKey)
            LazyGridState(idx, off)
        }

    LaunchedEffect(gridState) {
        snapshotFlow {
            gridState.firstVisibleItemIndex to
                    gridState.firstVisibleItemScrollOffset
        }.collectLatest { (idx, off) ->
            delay(400L)
            vm.saveScrollPos(gridKey, idx, off)
        }
    }

    DisposableEffect(gridState) {
        onDispose {
            vm.saveScrollPos(
                gridKey,
                gridState.firstVisibleItemIndex,
                gridState.firstVisibleItemScrollOffset
            )
        }
    }

    Column(
        Modifier.fillMaxSize()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Text(
                "Playlists",
                style =
                    MaterialTheme.typography.headlineSmall,
                fontWeight =
                    FontWeight.Bold,
                color = appText(),
                modifier =
                    Modifier.weight(1f)
            )

            IconButton(
                onClick = {
                    vm.showCustomPlaylists = true
                }
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "建立自訂播放清單",
                    tint = appText()
                )
            }
        }
        LazyVerticalGrid(
            columns =
                GridCells.Fixed(2),
            state = gridState,
            modifier =
                Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = 4.dp,
                    // Leave room for the mini player / select bar floating over the bottom.
                    bottom =
                        if (vm.current != null || vm.selectMode) {
                            120.dp
                        } else {
                            22.dp
                        }
                ),
            horizontalArrangement =
                Arrangement.spacedBy(12.dp),
            verticalArrangement =
                Arrangement.spacedBy(18.dp)
        ) {
            items(
                entries,
                key = { it.first }
            ) { (name, firstTracks) ->
                val first =
                    firstTracks.firstOrNull()

                PlaylistCard(
                    name,
                    first,
                    vm
                ) {
                    onOpen(
                        if (
                            name == "Library"
                        ) {
                            "Library"
                        } else {
                            name
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistCard(
    name: String,
    first: Track?,
    vm: PlayerViewModel,
    onClick: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(16.dp)
            )
            .clickable(
                onClick = onClick
            )
            .padding(2.dp)
    ) {
        Cover(
            vm,
            first,
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            RoundedCornerShape(16.dp)
        )

        Spacer(
            Modifier.height(8.dp)
        )

        Text(
            name,
            color = appText(),
            fontWeight =
                FontWeight.Bold,
            fontSize =
                MaterialTheme.typography.titleMedium.fontSize,
            maxLines = 1,
            overflow =
                TextOverflow.Ellipsis
        )

        Text(
            "Music",
            color = appMuted(),
            style =
                MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun SongListPage(
    vm: PlayerViewModel,
    offsetX: Animatable<Float, AnimationVector1D>,
    onSwipedBack: () -> Unit,
    onBack: () -> Unit
) {
    val tracks = vm.visible
    val hero = tracks.firstOrNull()

    val key =
        remember(vm.selectedView) {
            vm.selectedView
        }

    val listState = rememberLazyListState()

    var initialRestoreDone by remember(key) {
        mutableStateOf(false)
    }

    LaunchedEffect(key, tracks.size) {
        if (initialRestoreDone) return@LaunchedEffect
        if (tracks.isEmpty()) return@LaunchedEffect

        val (idx, off) = vm.getScrollPos(key)
        if (idx > 0 || off > 0) {
            withFrameNanos { }
            withFrameNanos { }
            runCatching { listState.scrollToItem(idx, off) }
        }

        initialRestoreDone = true
    }

    val collapsed by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 ||
                    listState.firstVisibleItemScrollOffset > 150
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex to
                    listState.firstVisibleItemScrollOffset
        }.collectLatest { (idx, off) ->
            delay(400L)
            vm.saveScrollPos(key, idx, off)
        }
    }

    DisposableEffect(listState) {
        onDispose {
            vm.saveScrollPos(
                key,
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset
            )
        }
    }

    val configuration = LocalConfiguration.current
    val screenWidthPx =
        with(LocalDensity.current) {
            configuration.screenWidthDp.dp.toPx()
        }

    val dragScope = rememberCoroutineScope()
    val currentOnSwipedBack by rememberUpdatedState(onSwipedBack)

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(screenWidthPx) {
                val tracker = VelocityTracker()
                val distanceThreshold = 110.dp.toPx()
                val velocityThreshold = 800.dp.toPx()

                detectHorizontalDragGestures(
                    onDragStart = {
                        tracker.resetTracking()
                    },
                    onDragCancel = {
                        dragScope.launch {
                            offsetX.animateTo(
                                0f,
                                spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                        }
                    },
                    onDragEnd = {
                        val vx = tracker.calculateVelocity().x
                        val shouldClose =
                            offsetX.value > distanceThreshold ||
                                    vx > velocityThreshold

                        if (shouldClose) {
                            dragScope.launch {
                                offsetX.animateTo(
                                    screenWidthPx,
                                    tween(
                                        220,
                                        easing = SheetEasing
                                    )
                                )
                                // The playlist grid is already fully in place underneath.
                                currentOnSwipedBack()
                            }
                        } else {
                            dragScope.launch {
                                offsetX.animateTo(
                                    0f,
                                    spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        tracker.addPosition(
                            change.uptimeMillis,
                            change.position
                        )
                        dragScope.launch(
                            start = CoroutineStart.UNDISPATCHED
                        ) {
                            offsetX.snapTo(
                                (offsetX.value + dragAmount)
                                    .coerceAtLeast(0f)
                            )
                        }
                    }
                )
            }
            .graphicsLayer {
                translationX = offsetX.value
            }
            .background(
                MaterialTheme.colorScheme.background
            )
    ) {
        LazyColumn(
            state = listState,
            modifier =
                Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    bottom =
                        if (vm.current != null || vm.selectMode) {
                            96.dp
                        } else {
                            24.dp
                        }
                ),
            verticalArrangement =
                Arrangement.spacedBy(2.dp)
        ) {
            item(key = "hero") {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(224.dp)
                ) {
                    Cover(
                        vm,
                        hero,
                        Modifier.fillMaxSize(),
                        RoundedCornerShape(0.dp)
                    )

                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color.Black.copy(
                                            alpha = .9f
                                        )
                                    )
                                )
                            )
                    )

                    Column(
                        Modifier
                            .align(
                                Alignment.BottomStart
                            )
                            .padding(
                                start = 16.dp,
                                end = 16.dp,
                                bottom = 16.dp
                            )
                    ) {
                        Text(
                            if (
                                vm.selectedView ==
                                "Library"
                            ) {
                                "All songs"
                            } else {
                                vm.selectedView
                            },
                            color = Color.White,
                            style =
                                MaterialTheme.typography.headlineSmall,
                            fontWeight =
                                FontWeight.Bold,
                            maxLines = 1,
                            overflow =
                                TextOverflow.Ellipsis
                        )

                        Text(
                            "Music  ·  ${tracks.size}",
                            color =
                                Color.White.copy(
                                    alpha = .82f
                                ),
                            style =
                                MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            items(
                tracks,
                key = { it.id },
                contentType = { "track" }
            ) { track ->
                PowerampTrackRow(
                    track,
                    vm
                )
            }

            if (
                tracks.isEmpty() &&
                !vm.loading
            ) {
                item {
                    Text(
                        "沒有找到歌曲",
                        color = appMuted(),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(50.dp),
                        textAlign =
                            TextAlign.Center
                    )
                }
            }
        }

        Surface(
            color =
                MaterialTheme.colorScheme.surface.copy(
                    alpha =
                        if (collapsed) {
                            .96f
                        } else {
                            .08f
                        }
                ),
            tonalElevation =
                if (collapsed) {
                    2.dp
                } else {
                    0.dp
                },
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(54.dp)
        ) {
            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        "返回",
                        tint =
                            if (collapsed) {
                                appText()
                            } else {
                                Color.White
                            }
                    )
                }

                if (collapsed) {
                    Text(
                        if (
                            vm.selectedView ==
                            "Library"
                        ) {
                            "All songs"
                        } else {
                            vm.selectedView
                        },
                        color = appText(),
                        fontWeight =
                            FontWeight.SemiBold,
                        maxLines = 1,
                        overflow =
                            TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }

                if (vm.selectMode) {
                    val allPicked =
                        tracks.isNotEmpty() &&
                                tracks.all { it.id in vm.customPicked }
                    TextButton(
                        onClick = {
                            if (allPicked) {
                                vm.customPicked.removeAll(tracks.map { it.id }.toSet())
                            } else {
                                tracks.forEach {
                                    if (it.id !in vm.customPicked &&
                                        vm.customPicked.size < CustomPlaylistStore.MAX_TRACKS
                                    ) {
                                        vm.customPicked.add(it.id)
                                    }
                                }
                            }
                        }
                    ) {
                        Text(
                            if (allPicked) "取消全選" else "全選",
                            color =
                                if (collapsed) {
                                    appText()
                                } else {
                                    Color.White
                                }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PowerampTrackRow(
    track: Track,
    vm: PlayerViewModel
) {
    val active =
        vm.current?.id == track.id
    val picked =
        vm.selectMode && track.id in vm.customPicked

    Row(
        Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(12.dp)
            )
            .background(
                if (picked) {
                    appText().copy(alpha = .16f)
                } else if (active && !vm.selectMode) {
                    appText().copy(alpha = .08f)
                } else {
                    Color.Transparent
                }
            )
            .clickable {
                if (vm.selectMode) {
                    vm.toggleCustomPick(track.id)
                } else {
                    vm.play(track)
                    vm.showNowPlaying = true
                }
            }
            .padding(vertical = 8.dp),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Cover(
            vm,
            track,
            Modifier.size(72.dp),
            RoundedCornerShape(12.dp)
        )

        Spacer(
            Modifier.width(12.dp)
        )

        Column(
            Modifier.weight(1f),
            verticalArrangement =
                Arrangement.spacedBy(2.dp)
        ) {
            Text(
                track.title.ifBlank {
                    track.name
                },
                color = appText(),
                fontWeight =
                    if (active) {
                        FontWeight.Bold
                    } else {
                        FontWeight.SemiBold
                    },
                fontSize =
                    MaterialTheme.typography.titleMedium.fontSize,
                maxLines = 1,
                overflow =
                    TextOverflow.Ellipsis
            )

            Text(
                track.artist.ifBlank {
                    track.album
                },
                color = appText(),
                fontWeight =
                    FontWeight.Medium,
                style =
                    MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow =
                    TextOverflow.Ellipsis
            )

            Text(
                track.ext
                    .removePrefix(".")
                    .uppercase(),
                color = appMuted(),
                style =
                    MaterialTheme.typography.labelSmall
            )
        }

        if (vm.selectMode) {
            Icon(
                if (picked) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                if (picked) "已選取" else "未選取",
                tint = if (picked) appText() else appMuted(),
                modifier = Modifier.padding(12.dp)
            )
        } else {
            IconButton(
                onClick = {
                    vm.requestShare(track)
                }
            ) {
                Icon(
                    Icons.Default.Share,
                    "分享",
                    tint = appMuted()
                )
            }
        }
    }
}

@Composable
fun Sidebar(
    vm: PlayerViewModel
) {
    Surface(
        color =
            MaterialTheme.colorScheme.surface,
        modifier =
            Modifier
                .width(230.dp)
                .fillMaxHeight()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            Text(
                "LIBRARY",
                style =
                    MaterialTheme.typography.labelSmall,
                color = appMuted(),
                modifier =
                    Modifier.padding(10.dp)
            )

            NavItem(
                "全部歌曲",
                vm.library.size,
                vm.selectedView == "Library"
            ) {
                vm.selectView("Library")
            }

            HorizontalDivider(
                color =
                    MaterialTheme.colorScheme.outline.copy(
                        alpha = .35f
                    ),
                modifier =
                    Modifier.padding(
                        vertical = 8.dp
                    )
            )

            Text(
                "PLAYLISTS",
                style =
                    MaterialTheme.typography.labelSmall,
                color = appMuted(),
                modifier =
                    Modifier.padding(10.dp)
            )

            vm.playlists.forEach {
                    (name, ids) ->
                NavItem(
                    name,
                    ids.size,
                    vm.selectedView == name
                ) {
                    vm.selectView(name)
                }
            }
        }
    }
}

@Composable
fun NavItem(
    name: String,
    count: Int,
    active: Boolean,
    onClick: () -> Unit
) {
    val bg =
        if (active) {
            MaterialTheme.colorScheme.onSurface
                .copy(alpha = .08f)
        } else {
            Color.Transparent
        }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(8.dp)
            )
            .background(bg)
            .clickable(
                onClick = onClick
            )
            .padding(
                horizontal = 10.dp,
                vertical = 10.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Icon(
            if (name == "全部歌曲") {
                Icons.Default.LibraryMusic
            } else {
                Icons.AutoMirrored.Filled.PlaylistPlay
            },
            null,
            Modifier.size(19.dp),
            tint =
                if (active) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    appMuted()
                }
        )

        Spacer(
            Modifier.width(10.dp)
        )

        Text(
            name,
            Modifier.weight(1f),
            color =
                if (active) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    appMuted()
                },
            maxLines = 1,
            overflow =
                TextOverflow.Ellipsis
        )

        Text(
            count.toString(),
            style =
                MaterialTheme.typography.labelSmall,
            color = appMuted()
        )
    }
}

@Composable
fun MainLibrary(
    vm: PlayerViewModel,
    modifier: Modifier,
    showHeader: Boolean = true
) {
    Column(
        modifier.padding(
            horizontal =
                if (showHeader) {
                    18.dp
                } else {
                    8.dp
                }
        )
    ) {
        if (showHeader) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = 16.dp
                    ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Column(
                    Modifier.weight(1f)
                ) {
                    Text(
                        if (
                            vm.selectedView ==
                            "Library"
                        ) {
                            "全部歌曲"
                        } else {
                            vm.selectedView
                        },
                        style =
                            MaterialTheme.typography.headlineSmall,
                        fontWeight =
                            FontWeight.SemiBold
                    )

                    Text(
                        "${vm.visible.size} tracks",
                        color = appMuted(),
                        style =
                            MaterialTheme.typography.labelSmall
                    )
                }

                SearchBox(
                    vm,
                    Modifier.width(300.dp)
                )
            }
        }

        val listState =
            rememberLazyListState()

        LazyColumn(
            state = listState,
            modifier =
                Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    start = 8.dp,
                    end = 8.dp,
                    bottom = 110.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(5.dp)
        ) {
            itemsIndexed(
                vm.visible,
                key = { _, t -> t.id }
            ) { index, track ->
                TrackRow(
                    index,
                    track,
                    vm
                )
            }

            if (
                vm.visible.isEmpty() &&
                !vm.loading
            ) {
                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 80.dp),
                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {
                        Text(
                            "♪",
                            style =
                                MaterialTheme.typography.displaySmall,
                            color = appMuted()
                        )

                        Text(
                            "沒有找到歌曲",
                            fontWeight =
                                FontWeight.SemiBold
                        )

                        Text(
                            "換個關鍵字，或按右上角「重新同步」",
                            color = appMuted(),
                            style =
                                MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchBox(
    vm: PlayerViewModel,
    modifier: Modifier,
    focusRequester: FocusRequester? = null
) {
    OutlinedTextField(
        value = vm.query,
        onValueChange = vm::updateQuery,
        modifier =
            modifier
                .height(48.dp)
                .focusRequester(
                    focusRequester
                        ?: remember {
                            FocusRequester()
                        }
                ),
        singleLine = true,
        placeholder = {
            Text(
                "搜尋曲名、檔名",
                color = appMuted()
            )
        },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                null,
                Modifier.size(19.dp),
                tint = appMuted()
            )
        },
        shape =
            RoundedCornerShape(9.dp),
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedTextColor = appText(),
                unfocusedTextColor = appText(),
                focusedBorderColor =
                    appText().copy(
                        alpha = .75f
                    ),
                unfocusedBorderColor =
                    appMuted().copy(
                        alpha = .45f
                    ),
                cursorColor = appText(),
                focusedLeadingIconColor =
                    appText(),
                unfocusedLeadingIconColor =
                    appMuted()
            )
    )
}

@Composable
fun TrackRow(
    index: Int,
    track: Track,
    vm: PlayerViewModel
) {
    val active =
        vm.current?.id == track.id

    Row(
        Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(10.dp)
            )
            .background(
                if (active) {
                    appText().copy(alpha = .08f)
                } else {
                    Color.Transparent
                }
            )
            .clickable {
                vm.play(track)
            }
            .padding(
                horizontal = 12.dp,
                vertical = 12.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Text(
            if (
                active &&
                vm.isPlaying
            ) {
                "♪"
            } else {
                (index + 1).toString()
            },
            color =
                if (active) {
                    appText()
                } else {
                    appMuted()
                },
            modifier =
                Modifier.width(38.dp)
        )

        Cover(
            vm,
            track,
            Modifier.size(60.dp),
            RoundedCornerShape(10.dp)
        )

        Spacer(
            Modifier.width(12.dp)
        )

        Column(
            Modifier.weight(1f),
            verticalArrangement =
                Arrangement.spacedBy(3.dp)
        ) {
            Text(
                track.title.ifBlank {
                    track.name
                },
                color = appText(),
                fontWeight =
                    if (active) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Medium
                    },
                maxLines = 1,
                overflow =
                    TextOverflow.Ellipsis
            )

            Text(
                listOf(
                    track.artist.ifBlank {
                        track.album
                    },
                    track.ext
                        .removePrefix(".")
                        .uppercase()
                )
                    .filter {
                        it.isNotBlank()
                    }
                    .joinToString(" · "),
                color = appMuted(),
                style =
                    MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow =
                    TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = {
                vm.requestShare(track)
            }
        ) {
            Icon(
                Icons.Default.Share,
                "分享",
                tint = appMuted()
            )
        }

        IconButton(
            onClick = {
                vm.download(track)
            }
        ) {
            Icon(
                Icons.Default.Download,
                "下載",
                tint = appMuted()
            )
        }
    }
}

@Composable
fun Cover(
    vm: PlayerViewModel,
    track: Track?,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape =
        RoundedCornerShape(8.dp)
) {
    if (
        track?.hasCover == true &&
        vm.apiBase.isNotBlank()
    ) {
        val model =
            remember(
                vm.apiBase,
                track.id,
                vm.syncVersion
            ) {
                vm.coverFile(track.id)
                    ?: R.drawable.default_cover
            }

        AsyncImage(
            model = model,
            imageLoader =
                AppPrefs.imageLoader,
            contentDescription = null,
            contentScale =
                ContentScale.Crop,
            modifier =
                modifier.clip(shape)
        )
    } else {
        Surface(
            modifier =
                modifier.clip(shape),
            color =
                appText().copy(
                    alpha = 0.08f
                )
        ) {
            Box(
                contentAlignment =
                    Alignment.Center
            ) {
                Text(
                    "♪",
                    color = appMuted(),
                    style =
                        MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistSheet(
    vm: PlayerViewModel
) {
    ModalBottomSheet(
        onDismissRequest = {
            vm.showPlaylistSheet = false
        },
        containerColor =
            MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
        ) {
            Text(
                "播放清單",
                style =
                    MaterialTheme.typography.titleLarge,
                fontWeight =
                    FontWeight.SemiBold,
                modifier =
                    Modifier.padding(18.dp)
            )

            LazyColumn(
                contentPadding =
                    PaddingValues(
                        bottom = 22.dp
                    )
            ) {
                item {
                    PlaylistChoice(
                        "全部歌曲",
                        vm.library.size,
                        vm.selectedView == "Library"
                    ) {
                        vm.selectView("Library")
                        vm.showPlaylistSheet = false
                    }
                }

                items(
                    vm.playlists.entries.toList(),
                    key = { it.key }
                ) { entry ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        PlaylistChoice(
                            entry.key,
                            entry.value.size,
                            vm.selectedView ==
                                    entry.key
                        ) {
                            vm.selectView(
                                entry.key
                            )
                            vm.showPlaylistSheet =
                                false
                        }

                        IconButton(
                            onClick = {
                                vm.sharePlaylist(
                                    entry.key
                                )
                            }
                        ) {
                            Icon(
                                Icons.Default.Share,
                                "分享播放清單",
                                tint = appMuted()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistChoice(
    name: String,
    count: Int,
    active: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick
            )
            .padding(
                horizontal = 18.dp,
                vertical = 13.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Icon(
            if (active) {
                Icons.AutoMirrored.Filled.PlaylistPlay
            } else {
                Icons.Default.LibraryMusic
            },
            null,
            tint =
                if (active) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    appMuted()
                }
        )

        Spacer(
            Modifier.width(10.dp)
        )

        Text(
            name,
            Modifier.weight(1f),
            fontWeight =
                if (active) {
                    FontWeight.SemiBold
                } else {
                    FontWeight.Normal
                }
        )

        Text(
            count.toString(),
            color = appMuted()
        )
    }
}

/* ------------------------------------------------------------------------
 * Now-playing sheet: gestures + animation.
 *
 *  Web 版 (public/app.js setupMiniSwipe / setupNowSwipe + .now-sheet CSS):
 *
 *   1. 拖拽期間（mini player 往上滑）
 *        - 手指往上滑時，sheet 的 translateY 由 (1 - progress) * 100% 驅動，
 *          整個 sheet 一起滑上來，overlay opacity = progress。
 *        - 此時還沒有 is-open，所以只看得到略微縮小（scale .94、opacity .7）
 *          的封面；標題、頻譜、進度、控制、歌詞都還是隱藏的。
 *
 *   2. 鬆手
 *        - 判定 progress >= .68 或 fling velocity 夠大 → is-open 打開
 *        - 網頁版在這一步交給 CSS transition：sheet 從當前位置滑到
 *          translateY(0)，同時 CSS 動畫讓每個內容元素依序 fade + rise
 *          入場。
 *
 *   3. 全螢幕往下滑關閉
 *        - sheet 直接跟手 1:1 translateY，超過門檻就 closeNowPlaying()。
 *
 *  對應到 Compose：
 *
 *   - sheet.progress  : sheet 位置 (0 = 底部外, 1 = 全螢幕)
 *   - sheet.contentProgress : 內容錯開入場 (0 = 全部隱藏, 1 = 全部顯示)
 *
 *  mini player 上滑 → 只推進 progress，contentProgress 保持 0
 *                     （= 只有縮小的封面，其他元件隱藏）
 *  鬆手打開 → 跟點擊一樣：sheet 從當前位置滑完，同時播放錯開入場。
 *  全螢幕往下滑 → 內容本來就是顯示狀態，跟著 sheet 一起移動。
 * ------------------------------------------------------------------------ */

private const val SHEET_MS = 380            // 全螢幕面板滑入 / 滑出的時間
private const val CONTENT_MS = 500f         // 內容錯開入場的總時間
private const val STAGE_MS = 150f           // 單一元素的動畫時長

private val SheetEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
private val StageEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f) // CSS "ease"

@Stable
class NowSheetController(
    private val vm: PlayerViewModel,
    private val scope: CoroutineScope
) {
    /** Sheet 位置：0 = 完全關閉（畫面下方），1 = 完全打開（全螢幕）。 */
    val progress = Animatable(0f)

    /** 內容錯開入場：0 = 全部初始隱藏，1 = 全部顯示。 */
    val contentProgress = Animatable(0f)

    /**
     * 拖拽跟手，只移動 sheet，不碰 contentProgress：
     *  - 從 mini player 上滑時內容是隱藏的（contentProgress = 0），
     *    所以只看得到縮小的封面，鬆手後才依序入場（同網頁版）。
     *  - 全螢幕往下滑時內容本來就是顯示的，會跟著 sheet 一起移動。
     */
    fun drag(p: Float) {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            progress.snapTo(p.coerceIn(0f, 1f))
        }
    }

    fun settle(open: Boolean) {
        if (vm.showNowPlaying != open) {
            // PlayerApp 內的 LaunchedEffect 會在旗標變化時呼叫 animateTo。
            vm.showNowPlaying = open
        } else {
            scope.launch { animateTo(open) }
        }
    }

    suspend fun animateTo(open: Boolean) {
        if (open) {
            val wasVisible = contentProgress.value > 0.5f

            if (wasVisible) {
                // 內容已可見（例如往下滑到一半又放回去）：只讓 sheet 完成剩下的距離。
                contentProgress.snapTo(1f)
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(SHEET_MS, easing = SheetEasing)
                )
            } else {
                // 點擊或從 mini player 上滑後鬆手：內容錯開入場，
                // 同時 sheet 從當前位置滑上去。
                contentProgress.snapTo(0f)
                kotlinx.coroutines.coroutineScope {
                    launch {
                        progress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(SHEET_MS, easing = SheetEasing)
                        )
                    }
                    launch {
                        contentProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(CONTENT_MS.toInt(), easing = LinearEasing)
                        )
                    }
                }
            }
        } else {
            // 關閉：sheet 滑出，內容保持當前狀態（讓它在滑出過程仍可見），
            // 等動畫完再重置。
            progress.animateTo(
                targetValue = 0f,
                animationSpec = tween(SHEET_MS, easing = SheetEasing)
            )
            contentProgress.snapTo(0f)
        }
    }
}

/**
 * 全螢幕面板往下滑關閉。會被歌詞 LazyColumn、Slider、按鈕、IconButton 等
 * 內部可拖曳元素優先攔截，所以歌詞區的垂直滾動不受影響。
 * 必須放在 graphicsLayer 外層，手指座標才不會被位移影響。
 */
private fun Modifier.sheetSwipeDown(
    sheet: NowSheetController,
    screenHeightPx: Float
): Modifier =
    pointerInput(sheet, screenHeightPx) {
        val tracker = VelocityTracker()
        val threshold = max(110.dp.toPx(), screenHeightPx * .22f)
        var total = 0f

        detectVerticalDragGestures(
            onDragStart = {
                tracker.resetTracking()
                // 從被打斷的動畫當前位置繼續。
                total = (1f - sheet.progress.value) * screenHeightPx
            },
            onDragEnd = {
                val velocity = tracker.calculateVelocity().y // 向下為正
                val close = total >= threshold || velocity >= 750.dp.toPx()
                sheet.settle(open = !close)
            },
            onDragCancel = { sheet.settle(open = true) },
            onVerticalDrag = { change, dragAmount ->
                change.consume()
                tracker.addPosition(change.uptimeMillis, change.position)
                total = (total + dragAmount).coerceAtLeast(0f)
                sheet.drag(1f - total / screenHeightPx)
            }
        )
    }

/**
 * 內容錯開：fade + 上浮 12dp，由 contentProgress 驅動。
 *
 * 各元素的 delay：
 *   cover 0 / title 60 / visualizer 140 / progress 210 / controls 280 / lyrics 350
 * 最後一個元素在 350 + STAGE_MS(150) = 500 = CONTENT_MS 時完成。
 */
private fun Modifier.staged(
    sheet: NowSheetController,
    delayMs: Float
): Modifier =
    graphicsLayer {
        val t = sheet.contentProgress.value * CONTENT_MS
        val local = StageEasing.transform(
            ((t - delayMs) / STAGE_MS).coerceIn(0f, 1f)
        )
        alpha = local
        translationY = (1f - local) * 12.dp.toPx()
    }

/** Cover 入場：scale 0.94 → 1，opacity 0.7 → 1。 */
private fun Modifier.coverEntrance(sheet: NowSheetController): Modifier =
    graphicsLayer {
        val t = sheet.contentProgress.value * CONTENT_MS
        val local = SheetEasing.transform((t / 380f).coerceIn(0f, 1f))
        val s = .94f + .06f * local
        scaleX = s
        scaleY = s
        alpha = .7f + .3f * local
    }

@Composable
fun MiniPlayer(
    vm: PlayerViewModel,
    sheet: NowSheetController
) {
    val track = vm.current ?: return

    val shape = RoundedCornerShape(20.dp)

    val screenHeightPx =
        with(LocalDensity.current) {
            LocalConfiguration.current.screenHeightDp.dp.toPx()
        }

    // 統一手勢：拖動 = 上滑展開、點一下 = 打開全螢幕。
    // 不再使用 clickable + detectVerticalDragGestures 疊加，因為那會讓
    // clickable 先把 down 事件吃掉，上滑偵測完全收不到手勢。
    Box(
        Modifier
            .fillMaxWidth()
            .padding(
                start = 12.dp,
                end = 12.dp,
                bottom = 12.dp
            )
            .shadow(14.dp, shape, clip = false)
            .clip(shape)
            .background(
                MaterialTheme.colorScheme.surface
                    .copy(alpha = 0.95f)
            )
            .border(
                1.dp,
                appText().copy(alpha = 0.16f),
                shape
            )
            .pointerInput(sheet, screenHeightPx) {
                val touchSlopPx = viewConfiguration.touchSlop

                // 慢拖到 sheet 完全展開所需的距離：螢幕高度 47%。
                val travel = screenHeightPx * 0.47f

                // 判定「快滑」的速度門檻（px/s，向上為負）。
                val flingVelocityPx = 1800.dp.toPx()

                // 松手後判定開啟的滑動距離下限（約螢幕高度的 23.5%）。
                val openDistancePx = travel * 0.5f

                awaitEachGesture {
                    val down =
                        awaitFirstDown(requireUnconsumed = false)

                    var total = 0f
                    var dragging = false
                    var flingTriggered = false
                    val tracker = VelocityTracker()

                    while (true) {
                        val event = awaitPointerEvent()
                        val change =
                            event.changes
                                .firstOrNull { it.id == down.id }
                                ?: break

                        if (!change.pressed) break

                        val dy =
                            change.position.y - change.previousPosition.y
                        total += dy

                        if (!dragging && abs(total) > touchSlopPx) {
                            dragging = true
                            tracker.resetTracking()
                        }

                        if (dragging) {
                            tracker.addPosition(
                                change.uptimeMillis,
                                change.position
                            )

                            val v = tracker.calculateVelocity().y
                            if (v <= -flingVelocityPx) {
                                flingTriggered = true
                                change.consume()
                                break
                            }

                            if (total < 0f) {
                                sheet.drag(-total / travel)
                            }
                            change.consume()
                        }
                    }

                    when {
                        flingTriggered -> {
                            // 快滑：mini player 留在原位，sheet 播放完整的
                            // 錯開入場（因為拖曳中沒有推進 contentProgress）。
                            sheet.settle(open = true)
                        }

                        dragging -> {
                            val v = tracker.calculateVelocity().y
                            val movedUpDistance = -total
                            val shouldOpen =
                                movedUpDistance >= openDistancePx ||
                                        v <= -900.dp.toPx()
                            sheet.settle(open = shouldOpen)
                        }

                        else -> {
                            // 沒有拖動 → 當作點擊，播放完整錯開入場。
                            vm.showNowPlaying = true
                        }
                    }
                }
            }
    ) {
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(
                                alpha =
                                    if (
                                        AppPrefs.instance.lightTheme
                                    ) {
                                        .22f
                                    } else {
                                        .08f
                                    }
                            ),
                            Color.Transparent
                        )
                    )
                )
        )

        Column {
            LinearProgressIndicator(
                progress = {
                    if (vm.duration > 0) {
                        (
                                vm.position.toFloat() /
                                        vm.duration
                                ).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                color =
                    appText().copy(
                        alpha = .86f
                    ),
                trackColor =
                    appText().copy(
                        alpha = .10f
                    )
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .padding(
                        horizontal = 9.dp
                    ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Cover(
                    vm,
                    track,
                    Modifier.size(52.dp),
                    RoundedCornerShape(12.dp)
                )

                Spacer(
                    Modifier.width(10.dp)
                )

                Column(
                    Modifier.weight(1f)
                ) {
                    Text(
                        track.title.ifBlank {
                            track.name
                        },
                        color = appText(),
                        fontWeight =
                            FontWeight.SemiBold,
                        maxLines = 1,
                        overflow =
                            TextOverflow.Ellipsis
                    )

                    Text(
                        track.artist.ifBlank {
                            track.album
                        },
                        color = appMuted(),
                        style =
                            MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow =
                            TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = {
                        vm.next(-1)
                    }
                ) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        "上一首",
                        tint = appText()
                    )
                }

                IconButton(
                    onClick = vm::togglePlay
                ) {
                    AnimatedContent(
                        vm.isPlaying,
                        label = "miniPlay"
                    ) {
                        Icon(
                            if (it) {
                                Icons.Default.Pause
                            } else {
                                Icons.Default.PlayArrow
                            },
                            if (it) {
                                "暫停"
                            } else {
                                "播放"
                            },
                            tint = appText()
                        )
                    }
                }

                IconButton(
                    onClick = {
                        vm.next(1)
                    }
                ) {
                    Icon(
                        Icons.Default.SkipNext,
                        "下一首",
                        tint = appText()
                    )
                }
            }
        }
    }
}

/**
 * 全螢幕面板打開 / 關閉動畫期間保持 composition；完全關閉時就從組合中移除。
 */
@Composable
fun NowPlayingHost(
    vm: PlayerViewModel,
    sheet: NowSheetController
) {
    val visible by remember(vm, sheet) {
        derivedStateOf {
            vm.current != null &&
                    (vm.showNowPlaying || sheet.progress.value > 0.001f)
        }
    }

    if (visible) {
        NowPlaying(vm, sheet)
    }
}

@Composable
fun NowPlaying(
    vm: PlayerViewModel,
    sheet: NowSheetController
) {
    val track =
        vm.current ?: return

    val screenHeightPx =
        with(LocalDensity.current) {
            LocalConfiguration.current.screenHeightDp.dp.toPx()
        }

    BackHandler(enabled = vm.showNowPlaying) {
        vm.showNowPlaying = false
    }

    Box(
        Modifier
            .fillMaxSize()
            // 阻擋下方列表繼續收到觸控。
            .sheetSwipeDown(sheet, screenHeightPx)
            .graphicsLayer {
                // sheet 用 progress 做 Y 位移；alpha 也由 progress 淡入，
                // 對應網頁版 overlay.style.opacity = progress。
                val p = sheet.progress.value
                translationY = (1f - p) * size.height
                alpha = p
                compositingStrategy = CompositingStrategy.ModulateAlpha
            }
            .background(
                MaterialTheme.colorScheme.background
            )
            .windowInsetsPadding(
                WindowInsets.safeDrawing
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 16.dp
                )
        ) {
            Row(
                Modifier.height(50.dp),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        vm.showNowPlaying = false
                    }
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        "縮小",
                        tint = appText()
                    )
                }

                Spacer(
                    Modifier.weight(1f)
                )
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(
                        min = 190.dp,
                        max = 285.dp
                    )
                    .coverEntrance(sheet),
                contentAlignment =
                    Alignment.Center
            ) {
                Cover(
                    vm,
                    track,
                    Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .fillMaxWidth(.70f),
                    RoundedCornerShape(18.dp)
                )
            }

            Spacer(
                Modifier.height(10.dp)
            )

            Column(Modifier.staged(sheet, 60f)) {
                Text(
                    track.title.ifBlank {
                        track.name
                    },
                    color = appText(),
                    style =
                        MaterialTheme.typography.titleLarge,
                    fontWeight =
                        FontWeight.SemiBold,
                    maxLines = 2,
                    overflow =
                        TextOverflow.Ellipsis
                )

                Text(
                    track.artist.ifBlank {
                        track.album
                    },
                    color = appMuted(),
                    maxLines = 1,
                    overflow =
                        TextOverflow.Ellipsis
                )
            }

            Spacer(
                Modifier.height(6.dp)
            )

            AudioVisualizer(
                Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .staged(sheet, 140f)
            )

            ProgressBar(
                vm,
                Modifier.staged(sheet, 210f)
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .staged(sheet, 280f),
                horizontalArrangement =
                    Arrangement.Center,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        vm.next(-1)
                    },
                    modifier =
                        Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        "上一首",
                        tint = appText(),
                        modifier =
                            Modifier.size(28.dp)
                    )
                }

                FilledIconButton(
                    onClick = vm::togglePlay,
                    shape = CircleShape,
                    modifier =
                        Modifier.size(56.dp),
                    colors =
                        IconButtonDefaults.filledIconButtonColors(
                            containerColor =
                                appText(),
                            contentColor =
                                if (
                                    AppPrefs.instance.lightTheme
                                ) {
                                    Color.White
                                } else {
                                    Color(0xFF111213)
                                }
                        )
                ) {
                    AnimatedContent(
                        vm.isPlaying,
                        label = "bigPlay"
                    ) {
                        Icon(
                            if (it) {
                                Icons.Default.Pause
                            } else {
                                Icons.Default.PlayArrow
                            },
                            if (it) {
                                "暫停"
                            } else {
                                "播放"
                            },
                            Modifier.size(27.dp)
                        )
                    }
                }

                IconButton(
                    onClick = {
                        vm.next(1)
                    },
                    modifier =
                        Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.SkipNext,
                        "下一首",
                        tint = appText(),
                        modifier =
                            Modifier.size(28.dp)
                    )
                }
            }

            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .staged(sheet, 350f)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Text(
                        "LYRICS",
                        color = appText(),
                        style =
                            MaterialTheme.typography.labelSmall,
                        fontWeight =
                            FontWeight.SemiBold
                    )

                    Text(
                        if (
                            vm.lyrics.any {
                                it.time != null
                            }
                        ) {
                            "SYNCED"
                        } else {
                            "TEXT"
                        },
                        color = appMuted(),
                        style =
                            MaterialTheme.typography.labelSmall
                    )
                }

                LyricsView(
                    vm,
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }
        }
    }
}

private const val SPECTRUM_ATTACK_TAU = 0.045f
private const val SPECTRUM_RELEASE_TAU = 0.18f

@Composable
fun AudioVisualizer(
    modifier: Modifier = Modifier
) {
    val engine = SpectrumEngine.shared
    val levels = remember { FloatArray(SpectrumEngine.BAR_COUNT) }
    val target = remember { FloatArray(SpectrumEngine.BAR_COUNT) }
    var frame by remember { mutableLongStateOf(0L) }

    val spectrumColor = appText()

    LaunchedEffect(engine) {
        var lastNanos = 0L

        while (true) {
            var settled = false

            withFrameNanos { now ->
                val dt =
                    if (lastNanos == 0L) {
                        1f / 60f
                    } else {
                        ((now - lastNanos) / 1_000_000_000f)
                            .coerceIn(0.001f, 0.1f)
                    }
                lastNanos = now

                engine.readLatest(target)

                val attack = 1f - exp(-dt / SPECTRUM_ATTACK_TAU)
                val release = 1f - exp(-dt / SPECTRUM_RELEASE_TAU)

                var sum = 0f
                for (i in levels.indices) {
                    val cur = levels[i]
                    val goal = target[i]
                    var next =
                        cur + (goal - cur) *
                                (if (goal > cur) attack else release)
                    if (goal == 0f && next < 0.002f) next = 0f
                    levels[i] = next
                    sum += next
                }

                settled = sum == 0f
                frame = now
            }

            if (settled) {
                lastNanos = 0L
                delay(80L)
            }
        }
    }

    Spacer(
        modifier.drawWithCache {
            val w = size.width
            val h = size.height

            val barCount =
                (w / 8.dp.toPx())
                    .toInt()
                    .coerceIn(20, SpectrumEngine.BAR_COUNT)

            val gap = 3.dp.toPx()

            val barWidth =
                ((w - gap * (barCount - 1)) / barCount)
                    .coerceAtLeast(1f)

            val minHeight = 2.dp.toPx()
            val radius = CornerRadius(barWidth / 2f, barWidth / 2f)

            val source = IntArray(barCount) {
                (it * SpectrumEngine.BAR_COUNT / barCount)
                    .coerceIn(0, SpectrumEngine.BAR_COUNT - 1)
            }

            val brush =
                Brush.verticalGradient(
                    colors = listOf(
                        spectrumColor.copy(alpha = 0.95f),
                        spectrumColor.copy(alpha = 0.55f)
                    ),
                    startY = 0f,
                    endY = h
                )

            onDrawBehind {
                if (frame == Long.MIN_VALUE) return@onDrawBehind

                for (i in 0 until barCount) {
                    val v = levels[source[i]].coerceIn(0f, 1f)
                    val bh = max(minHeight, v * h * 0.85f)

                    drawRoundRect(
                        brush = brush,
                        topLeft = Offset(i * (barWidth + gap), h - bh),
                        size = Size(barWidth, bh),
                        cornerRadius = radius
                    )
                }
            }
        }
    )
}

@Composable
fun ProgressBar(
    vm: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }

    val playing =
        if (vm.duration > 0) {
            (
                    vm.position.toFloat() /
                            vm.duration
                    ).coerceIn(0f, 1f)
        } else {
            0f
        }

    val shown = if (dragging) dragValue else playing

    Column(modifier) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {
            Text(
                formatTime((shown * vm.duration).toLong()),
                color = appMuted(),
                style =
                    MaterialTheme.typography.labelSmall
            )

            Text(
                formatTime(vm.duration),
                color = appMuted(),
                style =
                    MaterialTheme.typography.labelSmall
            )
        }

        Slider(
            value = shown,
            onValueChange = {
                dragging = true
                dragValue = it
            },
            onValueChangeFinished = {
                vm.seekTo((dragValue * vm.duration).toLong())
                dragging = false
            },
            enabled = vm.duration > 0
        )
    }
}

@Composable
fun LyricsView(
    vm: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val listState =
        rememberLazyListState()

    val lyrics = vm.lyrics

    val activeIndex by remember(lyrics) {
        derivedStateOf {
            activeLyric(
                lyrics,
                vm.position / 1000.0
            )
        }
    }

    LaunchedEffect(
        activeIndex,
        lyrics
    ) {
        if (activeIndex >= 0 && lyrics.isNotEmpty()) {
            val target =
                activeIndex.coerceIn(
                    0,
                    lyrics.lastIndex
                )

            runCatching {
                val info = listState.layoutInfo
                val item =
                    info.visibleItemsInfo
                        .firstOrNull { it.index == target }

                if (item != null) {
                    val viewportCenter =
                        (info.viewportStartOffset + info.viewportEndOffset) / 2f
                    val delta =
                        item.offset + item.size / 2f - viewportCenter

                    if (abs(delta) > 1f) {
                        listState.animateScrollBy(
                            delta,
                            tween(
                                320,
                                easing = SheetEasing
                            )
                        )
                    }
                } else {
                    val viewport =
                        info.viewportEndOffset - info.viewportStartOffset
                    listState.animateScrollToItem(
                        target,
                        scrollOffset = -(viewport / 2 - 40)
                    )
                }
            }
        }
    }

    if (lyrics.isEmpty()) {
        Box(
            modifier,
            contentAlignment =
                Alignment.Center
        ) {
            Text(
                "這首歌沒有可用的內嵌歌詞",
                color = appMuted(),
                style =
                    MaterialTheme.typography.bodySmall
            )
        }

        return
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding =
            PaddingValues(
                vertical = 14.dp
            ),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {
        itemsIndexed(
            lyrics,
            key = { index, _ -> index },
            contentType = { _, _ -> "lyric" }
        ) { index, line ->

            val active =
                index == activeIndex

            val textColor by animateColorAsState(
                if (active) {
                    appText()
                } else {
                    appMuted().copy(alpha = .72f)
                },
                tween(220),
                label = "lyricText"
            )

            val bgColor by animateColorAsState(
                if (active) {
                    appText().copy(alpha = .07f)
                } else {
                    Color.Transparent
                },
                tween(220),
                label = "lyricBg"
            )

            Text(
                line.text,
                color = textColor,
                fontWeight =
                    if (active) {
                        FontWeight.SemiBold
                    } else {
                        FontWeight.Normal
                    },
                style =
                    MaterialTheme.typography.bodyMedium,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(
                            RoundedCornerShape(8.dp)
                        )
                        .drawBehind {
                            drawRect(bgColor)
                        }
                        .clickable {
                            line.time?.let {
                                vm.seekTo(
                                    (
                                            it * 1000
                                            ).toLong()
                                )
                            }
                        }
                        .padding(
                            horizontal = 10.dp,
                            vertical = 7.dp
                        ),
                textAlign =
                    TextAlign.Center
            )
        }
    }
}

fun activeLyric(
    lines: List<LyricLine>,
    time: Double
): Int {
    if (lines.isEmpty()) {
        return -1
    }

    var best = -1
    var bestTime =
        Double.NEGATIVE_INFINITY

    lines.forEachIndexed { i, line ->
        val t = line.time

        if (
            t != null &&
            t.isFinite() &&
            t <= time &&
            t >= bestTime
        ) {
            best = i
            bestTime = t
        }
    }

    return best
}

fun formatTime(
    milliseconds: Long
): String {
    val seconds =
        (milliseconds / 1000L)
            .coerceAtLeast(0L)

    return "${seconds / 60}:${
        (seconds % 60)
            .toString()
            .padStart(2, '0')
    }"
}