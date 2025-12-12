package app.iremote.ir

import android.content.Context
import android.hardware.ConsumerIrManager
import kotlin.math.roundToInt


enum class CodeFormat { PRONTO, RAW, NEC, RC5, SAMSUNG, RC6 }

data class EncodedIr(
    val carrierHz: Int,
    val patternMicros: IntArray
)

interface InfraredTransmitter {
    fun hasEmitter(): Boolean
    fun capabilities(): List<IntRange> // frequency ranges in Hz, e.g., 30000..60000
    fun transmit(carrierHz: Int, patternMicros: IntArray): Result<Unit>
}


class AndroidInfraredTransmitter(ctx: Context) : InfraredTransmitter {
    private val mgr = ctx.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    override fun hasEmitter(): Boolean = mgr?.hasIrEmitter() == true

    override fun capabilities(): List<IntRange> {
        val m = mgr ?: return emptyList()
        return try {
            m.carrierFrequencies?.map { it.minFrequency..it.maxFrequency } ?: emptyList()
        } catch (_: Throwable) {
            // Some OEMs hide or break this; fallback to a common window
            listOf(30000..60000)
        }
    }

    override fun transmit(carrierHz: Int, patternMicros: IntArray): Result<Unit> {
        val m = mgr ?: return Result.failure(IllegalStateException("No IR manager"))
        return try {
            // Some chipsets fail if pattern too long; chunking could be added if needed
            m.transmit(carrierHz, patternMicros)
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}

object Pronto {
    // Parses "0000 ..." Pronto hex.
    fun decode(pronto: String): EncodedIr {
        val parts = pronto.trim()
            .split(Regex("\\s+"))
            .mapNotNull { it.toIntOrNull(16) }

        require(parts.isNotEmpty() && parts[0] == 0x0000) { "Only Pronto 0000 supported" }
        require(parts.size >= 4) { "Malformed Pronto" }

        val freqWord = parts[1]

        // Freq in Hz
        val carrierHz = (1_000_000.0 / (freqWord * 0.241246)).roundToInt()

        // single Pronto time unit in ms
        val unitUs = freqWord * 0.241246

        val oncePairs = parts[2]
        val repeatPairs = parts[3]
        val totalPairs = (oncePairs + repeatPairs) * 2

        val bursts = parts.drop(4).take(totalPairs)
        val pattern = bursts
            .map { (it * unitUs).roundToInt().coerceAtLeast(1) }
            .toIntArray()

        return EncodedIr(carrierHz, pattern)
    }
}