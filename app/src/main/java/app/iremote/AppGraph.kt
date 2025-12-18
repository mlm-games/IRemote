package app.iremote

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import app.iremote.data.repository.AppSettings
import app.iremote.data.repository.AppSettingsSchema
import app.iremote.ir.data.IrDatabase
import app.iremote.ir.AndroidInfraredTransmitter
import app.iremote.ir.InfraredTransmitter
import app.iremote.ir.data.IrRepository
import io.github.mlmgames.settings.core.SettingsRepository
import io.github.mlmgames.settings.core.datastore.createSettingsDataStore

object AppGraph {
    @Volatile private var instance: Instance? = null
    private val lock = Any()

    private class Instance(ctx: Context) {
        val appContext: Context = ctx.applicationContext

        // DataStore for settings
        private val settingsDataStore: DataStore<Preferences> =
            createSettingsDataStore(appContext, "app_settings")

        // Settings repository using the library
        val settings: SettingsRepository<AppSettings> =
            SettingsRepository(settingsDataStore, AppSettingsSchema)

        val db: IrDatabase = IrDatabase.build(appContext)
        val transmitter: InfraredTransmitter = AndroidInfraredTransmitter(appContext)
        val irRepo: IrRepository = IrRepository(db, transmitter)
    }

    fun init(context: Context) {
        if (instance == null) {
            synchronized(lock) {
                if (instance == null) instance = Instance(context)
            }
        }
    }

    private fun ck() = instance ?: error("AppGraph.init(context) not called")

    val settings: SettingsRepository<AppSettings> get() = ck().settings
    val schema get() = AppSettingsSchema
    val db get() = ck().db
    val transmitter get() = ck().transmitter
    val irRepo get() = ck().irRepo
}