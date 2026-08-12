package io.github.freewebmovement.igniter.persistence.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "enabled_packages")
class EnabledPackage {
    @PrimaryKey
    @ColumnInfo(name = "package_name")
    var packageName: String = ""

    constructor()
}
