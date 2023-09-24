package io.github.freewebmovement.igniter.persistence.database;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "enabled_packages")
public class EnabledPackage {
    @PrimaryKey
    @ColumnInfo(name = "package_name")
    @NonNull
    public String packageName;

    public EnabledPackage() {
        packageName = "";
    }
}
