package io.github.freewebmovement.igniter.persistence.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
public interface UserDao {
    @Query("SELECT * from users;")
    fun all(): List<Server?>?

    @Query("SELECT * from users limit (:page - 1)*:limit, :limit;")
    fun paginate(page: Int, limit: Int): List<Server?>?

    @Query("SELECT * FROM users WHERE server = :server")
    fun findByServer(server: Server): List<Server?>?

    @Query("DELETE FROM users WHERE server = :server AND username = :username")
    fun deleteByUniquePair(server: Server, username: String)

    @Insert
    fun insert(vararg users: Server?)

    @Delete
    fun delete(server: Server?)
}