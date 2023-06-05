package io.github.freewebmovement.igniter.x.persistence;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "servers")
public class Server {

    @PrimaryKey
    public int id;

    @ColumnInfo(name = "hostname")
    @NonNull
    public String hostname;

    @ColumnInfo(name = "port")
    @NonNull
    public String port;

    @ColumnInfo(name = "password")
    @NonNull
    public String password;

    @ColumnInfo(name = "localhost")
    @NonNull
    public String localhost;

    @ColumnInfo(name = "local_port")
    @NonNull
    public String local_port;
}
