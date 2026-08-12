package io.github.freewebmovement.igniter.persistence.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ServerDao {
    @Query("SELECT * from servers;")
    List<Server> all();

    @Query("SELECT * from servers limit (:page - 1)*:limit, :limit;")
    List<Server> paginate(int page, int limit);

    @Query("SELECT * FROM servers WHERE hostname like :hostname")
    Server findByHost(String hostname);

    @Query("SELECT * FROM servers WHERE hostname = :hostname AND port = :port LIMIT 1")
    Server findByHostAndPort(String hostname, int port);


    @Query("DELETE FROM servers WHERE hostname = :hostname AND port = :port")
    void deleteByUniquePair(String hostname, int port);

    @Query("DELETE FROM servers")
    void deleteAll();

    @Insert
    void insert(Server... servers);

    @Delete
    void delete(Server server);
}
