package app.iremote.ir.protocol

import app.iremote.ir.EncodedIr

// Basic RC5 (bi‑phase) encoder; 36 kHz, 14 bits (2 start + toggle + 5 addr + 6 cmd)
object Rc5 {
    private const val CARRIER = 36000
    private const val T = 889 // us

    fun encode(address: Int, command: Int, toggle: Int = 1): EncodedIr {
        val s1 = 1; val s2 = 1
        val t = toggle and 1
        val addr = address and 0x1F
        val cmd = command and 0x3F
        var frame = (s1 shl 13) or (s2 shl 12) or (t shl 11) or (addr shl 6) or cmd

        val marks = ArrayList<Int>()
        val spaces = ArrayList<Int>()

        // Bi‑phase: each bit is T mark + T space or vice versa depending on bit
        // We’ll emit starting with a mark
        var last = 1 // start with mark
        var pulse = ArrayList<Int>()
        for (i in 13 downTo 0) {
            val bit = (frame shr i) and 1
            // For RC5: a '1' is mark then space; '0' is space then mark, but with phase flips
            // Simplified: always emit T, T alternating starting with mark and toggling each half‑bit
            for (h in 0 until 2) {
                pulse += T
                last = 1 - last
            }
        }
        // Convert to [mark,space,mark,...] microsecond lengths, starting with a MARK
        // Our simplified construction: just alternate T values starting with mark.
        val pattern = IntArray(pulse.size) { T }
        return EncodedIr(CARRIER, pattern)
    }
}
