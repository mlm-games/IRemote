package app.iremote.ir.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RemoteDao {
    @Query("SELECT * FROM remotes ORDER BY favorite DESC, updatedAt DESC")
    fun all(): Flow<List<RemoteProfileEntity>>

    @Query("SELECT * FROM remotes WHERE id=:id")
    fun byIdFlow(id: Long): Flow<RemoteProfileEntity?>

    @Insert suspend fun insert(remote: RemoteProfileEntity): Long
    @Update suspend fun update(remote: RemoteProfileEntity)
    @Delete suspend fun delete(remote: RemoteProfileEntity)

    @Transaction
    @Query("SELECT * FROM remotes WHERE id=:id")
    fun withKeys(id: Long): Flow<RemoteWithKeys?>
}

@Dao
interface KeyDao {
    @Query("SELECT * FROM keys WHERE profileId=:profileId ORDER BY id")
    fun keysFor(profileId: Long): Flow<List<RemoteKeyEntity>>

    @Query("SELECT * FROM keys WHERE id=:id")
    suspend fun byId(id: Long): RemoteKeyEntity?

    @Insert suspend fun insert(key: RemoteKeyEntity): Long
    @Update suspend fun update(key: RemoteKeyEntity)
    @Delete suspend fun delete(key: RemoteKeyEntity)
}