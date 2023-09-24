package io.github.freewebmovement.igniter.persistence.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
class User (
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    @ColumnInfo
    val server: Server,
    @ColumnInfo
    val username: String,
    @ColumnInfo
    val password: String,
)