package io.github.freewebmovement.igniter.persistence.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface EnabledPackageDao {
    @Query("SELECT * from enabled_packages")
    fun getPackages(): List<EnabledPackage>

    @Query("SELECT * FROM enabled_packages WHERE package_name = :packageName")
    fun getPackage(packageName: String): EnabledPackage?

    @Insert
    fun insert(vararg packages: EnabledPackage)

    @Delete
    fun delete(aPackage: EnabledPackage)
}
