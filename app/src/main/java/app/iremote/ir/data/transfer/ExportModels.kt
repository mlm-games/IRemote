package app.iremote.ir.data.transfer

import kotlinx.serialization.Serializable

@Serializable
data class ExportBundle(
    val version: Int = 1,
    val remotes: List<ExportRemote>
)

@Serializable
data class ExportRemote(
    val name: String,
    val brand: String? = null,
    val model: String? = null,
    val color: Long = 0xFF3B82F6,
    val keys: List<ExportKey>
)

@Serializable
data class ExportKey(
    val name: String,
    val format: String,
    val payload: String? = null,
    val carrierHz: Int? = null,
    val patternMicros: List<Int>? = null,
    val repeatWhileHeld: Boolean = true,
    val holdRepeatDelayMs: Int = 110
)