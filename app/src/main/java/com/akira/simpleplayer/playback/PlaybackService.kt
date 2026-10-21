package com.akira.simpleplayer.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.akira.simpleplayer.MainActivity

/**
 * Playback service.
 *
 * Audio path:
 *
 *   network/file
 *       ↓
 *   ExoPlayer decoder
 *       ↓
 *   EqualizerAudioProcessor (parametric EQ, settings from EqStore)
 *       ↓
 *   SpectrumAudioProcessor
 *       ├── PCM copy → SpectrumEngine → FFT → UI
 *       │
 *       └── original PCM → DefaultAudioSink → speaker/headphones
 *
 * No microphone and no android.media.audiofx.Visualizer are used.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private lateinit var player: ExoPlayer
    private lateinit var session: MediaSession

    private lateinit var spectrumEngine: SpectrumEngine
    private lateinit var spectrumProcessor: SpectrumAudioProcessor
    private val equalizerProcessor = EqualizerAudioProcessor()

    override fun onCreate() {
        super.onCreate()

        // The service can start without the activity (media buttons, resumption).
        EqStore.init(this)

        // Same process as the UI: share one engine so the UI sees this PCM.
        spectrumEngine =
            SpectrumEngine.shared

        spectrumProcessor =
            SpectrumAudioProcessor(
                spectrumEngine
            )

        val audioAttributes =
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(
                    C.AUDIO_CONTENT_TYPE_MUSIC
                )
                .build()

        val renderersFactory =
            object : DefaultRenderersFactory(this) {

                override fun buildAudioSink(
                    context: android.content.Context,
                    enableFloatOutput: Boolean,
                    enableAudioTrackPlaybackParams: Boolean
                ): AudioSink {

                    /*
                     * Audio offload stays disabled (ExoPlayer's default; it is
                     * only enabled through TrackSelectionParameters). Offloaded
                     * playback would bypass the PCM AudioProcessor path and the
                     * spectrum processor would never see the samples.
                     */
                    return DefaultAudioSink.Builder(context)
                        .setEnableFloatOutput(
                            false
                        )
                        .setEnableAudioTrackPlaybackParams(
                            enableAudioTrackPlaybackParams
                        )
                        .setAudioProcessors(
                            arrayOf(
                                equalizerProcessor,
                                spectrumProcessor
                            )
                        )
                        .build()
                }
            }

        player =
            ExoPlayer.Builder(
                this,
                renderersFactory
            )
                .setAudioAttributes(
                    audioAttributes,
                    true
                )
                .build()

        val intent =
            Intent(
                this,
                MainActivity::class.java
            )

        val pendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or
                        PendingIntent.FLAG_UPDATE_CURRENT
            )

        session =
            MediaSession.Builder(
                this,
                player
            )
                .setSessionActivity(
                    pendingIntent
                )
                .build()
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession {
        return session
    }

    /**
     * Called when the user swipes the app away from the recent-tasks list.
     *
     * With android:stopWithTask="false" on the <service> tag, the system will
     * NOT stop this service automatically. We decide here:
     *
     *   - still playing (or queued with playWhenReady) → keep running so the
     *     foreground media notification keeps the audio alive.
     *   - idle → stopSelf() so the system can reclaim us.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session.player
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        if (::session.isInitialized) {
            session.release()
        }

        if (::player.isInitialized) {
            player.release()
        }

        if (::spectrumEngine.isInitialized) {
            spectrumEngine.stop()
        }

        super.onDestroy()
    }
}