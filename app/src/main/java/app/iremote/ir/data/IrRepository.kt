package app.iremote.ir.repo

import app.iremote.ir.CodeFormat
import app.iremote.ir.EncodedIr
import app.iremote.ir.InfraredTransmitter
import app.iremote.ir.Pronto
import app.iremote.ir.protocol.Nec
import app.iremote.ir.data.*
import app.iremote.ir.protocol.*
import kotlinx.coroutines.flow.Flow

class IrRepository(
    private val db: IrDatabase,
    private val tx: InfraredTransmitter
) {
    val remoteDao = db.remoteDao()
    val keyDao = db.keyDao()

    fun hasEmitter(): Boolean = tx.hasEmitter()
    fun capabilityRanges(): List<IntRange> = tx.capabilities()

    fun remotes(): Flow<List<RemoteProfileEntity>> = remoteDao.all()
    fun remoteWithKeys(id: Long): Flow<RemoteWithKeys?> = remoteDao.withKeys(id)
    fun remote(id: Long): Flow<RemoteProfileEntity?> = remoteDao.byIdFlow(id)

    suspend fun saveRemote(remote: RemoteProfileEntity): Long {
        return if (remote.id == 0L) remoteDao.insert(remote) else {
            remoteDao.update(remote.copy(updatedAt = System.currentTimeMillis())); remote.id
        }
    }

    suspend fun deleteRemote(remote: RemoteProfileEntity) = remoteDao.delete(remote)

    fun keys(profileId: Long): Flow<List<RemoteKeyEntity>> = keyDao.keysFor(profileId)
    suspend fun saveKey(key: RemoteKeyEntity): Long {
        return if (key.id == 0L) keyDao.insert(key) else { keyDao.update(key); key.id }
    }
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
        }
        return tx.transmit(encoded.carrierHz, encoded.patternMicros)
    }

    private fun parseAddrCmd(payload: String?): Pair<Int, Int> {
        val p = requireNotNull(payload) { "payload addr:cmd required" }
        val parts = p.split(":")
        require(parts.size == 2) { "payload must be addr:cmd" }
        val addr = parts[0].removePrefix("0x").toInt(16)
        val cmd = parts[1].removePrefix("0x").toInt(16)
        return addr to cmd
    }
}