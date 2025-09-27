package app.iremote.ir.protocol

import app.iremote.ir.EncodedIr

// Simple NEC 32‑bit encoder (addr, ~addr, cmd, ~cmd); 38 kHz
object Nec {
    private const val CARRIER = 38000
    private const val HDR_MARK = 9000
    private const val HDR_SPACE = 4500
    private const val BIT_MARK = 560
    private const val ONE_SPACE = 1690
    private const val ZERO_SPACE = 560
    private const val TRAIL_MARK = 560

    fun encode(address: Int, command: Int): EncodedIr {
        val addr = address and 0xFF
        val nAddr = addr.inv() and 0xFF
        val cmd = command and 0xFF
        val nCmd = cmd.inv() and 0xFF

        val data = (addr) or (nAddr shl 8) or (cmd shl 16) or (nCmd shl 24)
        val bits = IntArray(32) { i -> (data shr i) and 1 } // LSB first per NEC
        val out = ArrayList<Int>(2 + 32 * 2 + 1)

        out += HDR_MARK; out += HDR_SPACE
        for (b in bits) {
            out += BIT_MARK
            out += if (b == 1) ONE_SPACE else ZERO_SPACE
        }
        out += TRAIL_MARK
        return EncodedIr(CARRIER, out.toIntArray())
    }
}
