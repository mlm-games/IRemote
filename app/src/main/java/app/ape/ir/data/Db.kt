package app.ape.ir.data

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import app.ape.ir.CodeFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [RemoteProfileEntity::class, RemoteKeyEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class IrDatabase : RoomDatabase() {
    abstract fun remoteDao(): RemoteDao
    abstract fun keyDao(): KeyDao

    companion object {
        fun build(ctx: Context): IrDatabase {
            return Room.databaseBuilder(ctx, IrDatabase::class.java, "ir.db")
                .fallbackToDestructiveMigration(true)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed with a generic TV remote (NEC)
                        CoroutineScope(Dispatchers.IO).launch {
                            val inst = build(ctx)
                            val rId = inst.remoteDao().insert(
                                RemoteProfileEntity(name = "Generic TV (NEC)", brand = "Generic")
                            )
                            inst.keyDao().insert(RemoteKeyEntity(rId, rId, "Power", CodeFormat.NEC, "0x00:0x10"))
                            inst.keyDao().insert(RemoteKeyEntity(0, rId, "Vol +", CodeFormat.NEC, "0x00:0x18"))
                            inst.keyDao().insert(RemoteKeyEntity(0, rId, "Vol -", CodeFormat.NEC, "0x00:0x19"))
                            inst.keyDao().insert(RemoteKeyEntity(0, rId, "Mute",  CodeFormat.NEC, "0x00:0x0D"))
                        }
                    }
                })
                .build()
        }
    }
}