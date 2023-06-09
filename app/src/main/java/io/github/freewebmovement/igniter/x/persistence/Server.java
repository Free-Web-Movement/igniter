package io.github.freewebmovement.igniter.x.persistence;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "servers", indices = @Index(value = {"hostname", "port"},
        unique = true))
public class Server {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "hostname")
    @NonNull
    public String hostname;

    @ColumnInfo(name = "port")
    @NonNull
    public int port;

    @ColumnInfo(name = "password")
    @NonNull
    public String password;

    @ColumnInfo(name = "localhost", defaultValue = "127.0.0.1")
    public String localhost;

    @ColumnInfo(name = "local_port")
    @NonNull
    public int local_port;
}
