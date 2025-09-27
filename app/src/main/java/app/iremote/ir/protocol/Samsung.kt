package app.iremote.ir.protocol

import app.iremote.ir.EncodedIr

// Samsung32 (NECx-like): 38 kHz, header 4500/4500, bit mark 560, '1' space 1690, '0' space 560
// Frame: 16-bit address (LSB first), 16-bit command (8 cmd + 8 ~cmd), LSB first
object Samsung {
    private const val CARRIER = 38000
    private const val HDR_MARK = 4500
    private const val HDR_SPACE = 4500
    private const val BIT_MARK = 560
    private const val ONE_SPACE = 1690
    private const val ZERO_SPACE = 560
    private const val TRAIL_MARK = 560

    // addr: 16-bit (e.g., 0xE0E0), cmd: 8-bit (e.g., 0x10). High byte of command is inverted.
    fun encode(address16: Int, command8: Int): EncodedIr {
        val addr = address16 and 0xFFFF
        val cmd = command8 and 0xFF
        val nCmd = cmd.inv() and 0xFF

        // 32 bits LSB-first: 16 address + 8 cmd + 8 ~cmd
        val words = intArrayOf(addr, (cmd or (nCmd shl 8)))
        val bits = IntArray(32) { i ->
            val w = if (i < 16) words[0] else words[1]
            (w shr (i % 16)) and 1
        }

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