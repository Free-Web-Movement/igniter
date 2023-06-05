package io.github.freewebmovement.igniter.x.persistence;

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

    @Insert
    void insert(Server... servers);

    @Delete
    void delete(Server server);
}
