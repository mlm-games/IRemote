package app.iremote.ir.data

import androidx.room.*
import app.iremote.ir.CodeFormat

@Entity(tableName = "remotes")
data class RemoteProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val brand: String? = null,
    val model: String? = null,
    val favorite: Boolean = false,
    val color: Long = 0xFF3B82F6, // default accent
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "keys",
    foreignKeys = [
        ForeignKey(
            entity = RemoteProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("profileId")]
)
data class RemoteKeyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val name: String,
    val format: CodeFormat,
    // For PRONTO: payload = raw string; For RAW: payload ignored; For NEC/RC5: "addr:cmd"
    val payload: String? = null,
    val carrierHz: Int? = null,       // used when RAW
    val patternMicros: List<Int>? = null, // used when RAW
    val repeatWhileHeld: Boolean = true,
    val holdRepeatDelayMs: Int = 110
)

class Converters {
    @TypeConverter fun fmtToString(f: CodeFormat) = f.name
    @TypeConverter fun stringToFmt(s: String) = CodeFormat.valueOf(s)
    @TypeConverter fun listToString(l: List<Int>?) = l?.joinToString(",")
    @TypeConverter fun stringToList(s: String?): List<Int>? =
        s?.takeIf { it.isNotBlank() }?.split(",")?.mapNotNull { it.trim().toIntOrNull() }
}

data class RemoteWithKeys(
    @Embedded val remote: RemoteProfileEntity,
    @Relation(parentColumn = "id", entityColumn = "profileId")
    val keys: List<RemoteKeyEntity>
)