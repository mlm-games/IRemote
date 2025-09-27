package app.iremote.ir.data

import app.iremote.App
import app.iremote.ir.CodeFormat
import app.iremote.ir.EncodedIr
import app.iremote.ir.InfraredTransmitter
import app.iremote.ir.Pronto
import app.iremote.ir.data.transfer.ExportBundle
import app.iremote.ir.data.transfer.ExportKey
import app.iremote.ir.data.transfer.ExportRemote
import app.iremote.ir.protocol.Nec
import app.iremote.ir.protocol.Rc5
import app.iremote.ir.protocol.Rc6
import app.iremote.ir.protocol.Samsung
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.json.Json

class IrRepository(
    private val db: IrDatabase,
    private val tx: InfraredTransmitter
) {
    val remoteDao = db.remoteDao()
    val keyDao = db.keyDao()

    fun hasEmitter(): Boolean = tx.hasEmitter()
    fun capabilityRanges(): List<IntRange> = tx.capabilities()

    fun remotes() = remoteDao.all()
    fun remoteWithKeys(id: Long) = remoteDao.withKeys(id)
    fun remote(id: Long) = remoteDao.byIdFlow(id)

    suspend fun saveRemote(remote: RemoteProfileEntity): Long {
        return if (remote.id == 0L) remoteDao.insert(remote) else {
            remoteDao.update(remote.copy(updatedAt = System.currentTimeMillis())); remote.id
        }
    }
    suspend fun deleteRemote(remote: RemoteProfileEntity) = remoteDao.delete(remote)

    fun keys(profileId: Long) = keyDao.keysFor(profileId)
    suspend fun saveKey(key: RemoteKeyEntity): Long = if (key.id == 0L) keyDao.insert(key) else { keyDao.update(key); key.id }
    suspend fun deleteKey(key: RemoteKeyEntity) = keyDao.delete(key)

    fun send(key: RemoteKeyEntity): Result<Unit> {
        val encoded = when (key.format) {
            CodeFormat.PRONTO -> {
                val p = requireNotNull(key.payload) { "Pronto payload required" }
                Pronto.decode(p)
            }
            CodeFormat.RAW -> {
                val f = requireNotNull(key.carrierHz) { "RAW carrier required" }
                val pat = requireNotNull(key.patternMicros) { "RAW pattern required" }
                EncodedIr(f, pat.toIntArray())
            }
            CodeFormat.NEC -> {
                val (addr, cmd) = parseAddrCmd(key.payload)
                Nec.encode(addr, cmd)
            }
            CodeFormat.RC5 -> {
                val (addr, cmd) = parseAddrCmd(key.payload)
                Rc5.encode(addr, cmd)
            }
            CodeFormat.SAMSUNG -> {
                // addr: 16-bit, cmd: 8-bit
                val (addr, cmd) = parseAddrCmd(key.payload)
                Samsung.encode(addr, cmd)
            }
            CodeFormat.RC6 -> {
                val (addr, cmd) = parseAddrCmd(key.payload)
                Rc6.encode(addr, cmd)
            }
        }
        val result = tx.transmit(encoded.carrierHz, encoded.patternMicros)
        // Update QS tile last-used
        result.onSuccess { LastUsedStore.markLastKeyId(App.ctx, key.id) }
        return result
    }

    suspend fun sendByKeyId(id: Long): Result<Unit> {
        val key = keyDao.byId(id) ?: return Result.failure(IllegalArgumentException("Key not found"))
        return send(key)
    }

    private fun parseAddrCmd(payload: String?): Pair<Int, Int> {
        val p = requireNotNull(payload) { "payload addr:cmd required" }
        val parts = p.split(":")
        require(parts.size == 2) { "payload must be addr:cmd" }
        val addr = parts[0].removePrefix("0x").toInt(16)
        val cmd = parts[1].removePrefix("0x").toInt(16)
        return addr to cmd
    }

    // ---------- Export / Import ----------

    suspend fun exportAll(): String {
        val remotes = remotes().firstOrNull() ?: emptyList()
        val payload = ExportBundle(
            version = 1,
            remotes = remotes.map { r ->
                val keys = keys(r.id).firstOrNull().orEmpty()
                ExportRemote(
                    name = r.name, brand = r.brand, model = r.model, color = r.color,
                    keys = keys.map { k ->
                        ExportKey(
                            name = k.name,
                            format = k.format.name,
                            payload = k.payload,
                            carrierHz = k.carrierHz,
                            patternMicros = k.patternMicros,
                            repeatWhileHeld = k.repeatWhileHeld,
                            holdRepeatDelayMs = k.holdRepeatDelayMs
                        )
                    }
                )
            }
        )
        return Json { prettyPrint = true }.encodeToString(payload)
    }

    suspend fun exportRemote(id: Long): String {
        val r = remoteWithKeys(id).first() ?: error("Remote not found")
        val payload = ExportBundle(
            version = 1,
            remotes = listOf(
                ExportRemote(
                    name = r.remote.name, brand = r.remote.brand, model = r.remote.model, color = r.remote.color,
                    keys = r.keys.map { k ->
                        ExportKey(
                            name = k.name,
                            format = k.format.name,
                            payload = k.payload,
                            carrierHz = k.carrierHz,
                            patternMicros = k.patternMicros,
                            repeatWhileHeld = k.repeatWhileHeld,
                            holdRepeatDelayMs = k.holdRepeatDelayMs
                        )
                    }
                )
            )
        )
        return Json { prettyPrint = true }.encodeToString(payload)
    }

    suspend fun importBundle(json: String) {
        val bundle = Json.decodeFromString<ExportBundle>(json)
        bundle.remotes.forEach { er ->
            val id = saveRemote(
                RemoteProfileEntity(
                    name = er.name, brand = er.brand, model = er.model, color = er.color
                )
            )
            er.keys.forEach { ek ->
                val fmt = CodeFormat.valueOf(ek.format)
                saveKey(
                    RemoteKeyEntity(
                        profileId = id,
                        name = ek.name,
                        format = fmt,
                        payload = ek.payload,
                        carrierHz = ek.carrierHz,
                        patternMicros = ek.patternMicros,
                        repeatWhileHeld = ek.repeatWhileHeld,
                        holdRepeatDelayMs = ek.holdRepeatDelayMs
                    )
                )
            }
        }
    }
}