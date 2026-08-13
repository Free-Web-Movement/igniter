package io.github.freewebmovement.igniter.persistence.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ServerDao {
    @Query("SELECT * from servers;")
    fun all(): List<Server>

    @Query("SELECT * FROM servers ORDER BY id")
    fun observeAllLive(): LiveData<List<Server>>

    @Query("SELECT * from servers limit (:page - 1)*:limit, :limit;")
    fun paginate(page: Int, limit: Int): List<Server>

    @Query("SELECT * FROM servers WHERE hostname like :hostname")
    fun findByHost(hostname: String): Server?

    @Query("SELECT * FROM servers WHERE hostname = :hostname AND port = :port LIMIT 1")
    fun findByHostAndPort(hostname: String, port: Int): Server?

    @Query("DELETE FROM servers WHERE hostname = :hostname AND port = :port")
    fun deleteByUniquePair(hostname: String, port: Int)

    @Query("DELETE FROM servers")
    fun deleteAll()

    @Query("SELECT * FROM servers WHERE id = :id LIMIT 1")
    fun findById(id: Int): Server?

    @Insert
    fun insert(vararg servers: Server)

    @Update
    fun update(server: Server)

    @Delete
    fun delete(server: Server)
}
