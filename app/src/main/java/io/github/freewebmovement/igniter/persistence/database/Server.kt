package io.github.freewebmovement.igniter.persistence.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "servers", indices = [Index(value = ["hostname", "port"], unique = true)])
class Server(
    @PrimaryKey(autoGenerate = true)
    @JvmField
    var id: Int = 0,

    @ColumnInfo(name = "hostname")
    @JvmField
    var hostname: String = "",

    @ColumnInfo(name = "port")
    @JvmField
    var port: Int = 0,

    @ColumnInfo(name = "password")
    @JvmField
    var password: String = "",

    @ColumnInfo(name = "localhost", defaultValue = "127.0.0.1")
    @JvmField
    var localhost: String? = null,

    @ColumnInfo(name = "local_port")
    @JvmField
    var local_port: Int = 0
)
