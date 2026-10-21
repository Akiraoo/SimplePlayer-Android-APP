package com.akira.simpleplayer.playback

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Parametric equalizer in the ExoPlayer audio path (before the spectrum processor, so
 * the visualizer shows what is heard).
 *
 * Settings come from [EqStore]; a change is picked up on the next buffer. Filtering runs
 * in double precision (transposed direct form II biquads, state kept per channel). When
 * the equalizer is off or the preset is flat, PCM passes through untouched.
 */
class EqualizerAudioProcessor : BaseAudioProcessor() {

    private var seenVersion = -1
    private var bandCount = 0
    private var preampGain = 1.0
    private var bypass = true

    /** b0 b1 b2 a1 a2 per band */
    private var coeffs = DoubleArray(EqLimits.MAX_BANDS * 5)

    /** z1 z2 per band per channel */
    private var state = DoubleArray(0)

    private var ditherSeed = 0x2545F491

    override fun onConfigure(
        inputAudioFormat: AudioProcessor.AudioFormat
    ): AudioProcessor.AudioFormat {
        if (
            inputAudioFormat.encoding != C.ENCODING_PCM_16BIT &&
            inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT
        ) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        return inputAudioFormat
    }

    override fun onFlush() {
        // new format or a seek: recompute for the (maybe new) sample rate, clear the filters
        seenVersion = -1
        state.fill(0.0)
    }

    override fun onReset() {
        seenVersion = -1
        state = DoubleArray(0)
    }

    private fun refresh() {
        val version = EqStore.version
        if (version == seenVersion) return
        seenVersion = version

        val preset = EqStore.active
        val wasBypassed = bypass
        bypass = !EqStore.enabled || preset.isFlat

        val rate = inputAudioFormat.sampleRate
        val channels = inputAudioFormat.channelCount
        val bands = preset.bands.take(EqLimits.MAX_BANDS)
        for ((i, b) in bands.withIndex()) EqMath.coefficients(b, rate, coeffs, i * 5)
        preampGain = 10.0.pow(preset.preamp / 20.0)

        // keep the filter memory while bands are only being tweaked (no click while
        // dragging a slider); start clean when the layout changes or it was off
        val size = channels * bands.size * 2
        if (bands.size != bandCount || state.size != size || wasBypassed) {
            state = DoubleArray(size)
        }
        bandCount = bands.size
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        refresh()
        if (bypass) {
            replaceOutputBuffer(remaining).put(inputBuffer).flip()
            return
        }

        val channels = inputAudioFormat.channelCount
        val isFloat = inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT
        val bytesPerSample = if (isFloat) 4 else 2
        val frames = remaining / (channels * bytesPerSample)

        val input = inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        val out = replaceOutputBuffer(frames * channels * bytesPerSample)
            .order(ByteOrder.LITTLE_ENDIAN)

        val nb = bandCount
        val c = coeffs
        val z = state
        val pre = preampGain

        for (f in 0 until frames) {
            for (ch in 0 until channels) {
                var x = if (isFloat) {
                    input.getFloat().toDouble()
                } else {
                    input.getShort() / 32768.0
                }
                x *= pre
                var zi = ch * nb * 2
                for (b in 0 until nb) {
                    val o = b * 5
                    val y = c[o] * x + z[zi]
                    z[zi] = c[o + 1] * x - c[o + 3] * y + z[zi + 1]
                    z[zi + 1] = c[o + 2] * x - c[o + 4] * y
                    x = y
                    zi += 2
                }
                if (isFloat) {
                    out.putFloat(x.coerceIn(-1.0, 1.0).toFloat())
                } else {
                    // TPDF dither before rounding back to 16 bit
                    val s = (x * 32768.0 + tpdf()).roundToInt().coerceIn(-32768, 32767)
                    out.putShort(s.toShort())
                }
            }
        }
        // skip a trailing partial frame, if any (keeps the input fully consumed)
        inputBuffer.position(inputBuffer.limit())
        out.flip()
    }

    /** Triangular noise of ±1 LSB (two uniform values summed). */
    private fun tpdf(): Double {
        var s = ditherSeed
        s = s xor (s shl 13); s = s xor (s ushr 17); s = s xor (s shl 5)
        val a = (s and 0xFFFF) / 65536.0
        s = s xor (s shl 13); s = s xor (s ushr 17); s = s xor (s shl 5)
        val b = (s and 0xFFFF) / 65536.0
        ditherSeed = s
        return a - b
    }
}
