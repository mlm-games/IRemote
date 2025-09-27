package app.iremote

import android.content.Context
import app.iremote.data.repository.SettingsRepository
import app.iremote.ir.data.IrDatabase
import app.iremote.ir.AndroidInfraredTransmitter
import app.iremote.ir.InfraredTransmitter
import app.iremote.ir.repo.IrRepository

object AppGraph {
    @Volatile private var instance: Instance? = null
    private val lock = Any()

    private class Instance(ctx: Context) {
        val appContext = ctx.applicationContext
        val settings = SettingsRepository(appContext)

        val db: IrDatabase = IrDatabase.build(appContext)
        val transmitter: InfraredTransmitter = AndroidInfraredTransmitter(appContext)
        val irRepo: IrRepository = IrRepository(db, transmitter)
    }

    fun init(context: Context) {
        if (instance == null) {
            synchronized(lock) { if (instance == null) instance = Instance(context) }
        }
    }

    private fun ck() = instance ?: error("AppGraph.init(context) not called")

    val settings get() = ck().settings
    val db get() = ck().db
    val transmitter get() = ck().transmitter
    val irRepo get() = ck().irRepo
}
