package app.iremote.ir

import android.content.Context
import android.hardware.ConsumerIrManager
import kotlin.math.roundToInt


enum class CodeFormat { PRONTO, RAW, NEC, RC5 }

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
    // Parses "0000 ..." Pronto hex. Supports non‑learned form (0000).
    fun decode(pronto: String): EncodedIr {
        val parts = pronto.trim().split(Regex("\\s+")).mapNotNull { it.toIntOrNull(16) }
        require(parts.isNotEmpty() && parts[0] == 0x0000) { "Only Pronto 0000 supported" }
        require(parts.size >= 4) { "Malformed Pronto" }

        val freqWord = parts[1]
        val oneClock = 1_000_000.0 / (freqWord * 0.241246) // microseconds
        val carrierHz = (1_000_000.0 / (oneClock * 0.5)).roundToInt()

        val bursts = parts.drop(4)
        val pattern = bursts.map { (it * oneClock).roundToInt().coerceAtLeast(1) }.toIntArray()
        return EncodedIr(carrierHz, pattern)
    }

    // Utility to encode raw pattern to pronto (optional)
}