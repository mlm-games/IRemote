package app.iremote.ir.protocol

import app.iremote.ir.EncodedIr
import kotlin.math.max

// RC6 Mode 0 (common for MCE remotes).
// Carrier ~36 kHz. Unit T = 444us. Leader: 6T mark + 2T space.
// Start bit (double width): logical '1' with 2T high + 2T low (bi-phase).
// Frame: start, mode(3=000), toggle(1), address(8), command(8). Total 1 + 3 + 1 + 8 + 8 = 21 bits.
// Manchester (bi-phase) coding: each bit is T high + T low for '1', and T low + T high for '0'.
object Rc6 {
    private const val CARRIER = 36000
    private const val T = 444 // microseconds

    fun encode(address8: Int, command8: Int, toggle: Int = 0): EncodedIr {
        val mode = 0 // Mode 0
        val addr = address8 and 0xFF
        val cmd = command8 and 0xFF
        // Build MSB-first bitstream (after start bit)
        val bits = ArrayList<Int>(1 + 3 + 1 + 8 + 8)
        // Start bit is handled specially; append the rest here:
        for (i in 2 downTo 0) bits += (mode shr i) and 1
        bits += (toggle and 1)
        for (i in 7 downTo 0) bits += (addr shr i) and 1
        for (i in 7 downTo 0) bits += (cmd shr i) and 1

        val pattern = ArrayList<Int>()

        // Leader 6T mark + 2T space
        pattern += 6 * T
        pattern += 2 * T

        // Start bit (double width '1'): 2T high + 2T low in bi-phase
        // We model as: T mark, T mark, T space, T space
        pattern += T; pattern += T // 2T mark
        pattern += T; pattern += T // 2T space

        // Data bits, bi-phase: each bit is T mark + T space for '1' else T space + T mark for '0'
        // We need to maintain alternating phases; start with mark (high) half-bit
        var highPhase = true
        fun pushHalf(high: Boolean) {
            // convert half-bit to mark/space durations
            if (pattern.size % 2 == 0) {
                // next is mark
                pattern += T
            } else {
                // next is space
                pattern += T
            }
        }
        // Using simpler alternating: append for each half-bit; ConsumerIrManager expects mark/space pairs.
        // We'll explicitly alternate mark/space durations.
        fun appendHalf(isMark: Boolean) {
            if (pattern.size % 2 == 0) {
                // we are about to add a mark length
                pattern += T
            } else {
                pattern += T
            }
        }
        // Encode bits
        for (b in bits) {
            if (b == 1) {
                // mark then space
                appendHalf(true)
                appendHalf(false)
            } else {
                // space then mark
                appendHalf(false)
                appendHalf(true)
            }
        }
        // Ensure odd length -> end on mark; RC6 does not require trailing mark, but many senders include a final T mark
        if (pattern.size % 2 == 0) {
            pattern += max(T, 200)
        }
        return EncodedIr(CARRIER, pattern.toIntArray())
    }
}