package io.github.freewebmovement.igniter.persistence.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Server::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    //    abstract fun enabledPackageDao(): EnabledPackageDao
    abstract fun serverDao(): ServerDao
    //    abstract fun userDao(): UserDao
}
