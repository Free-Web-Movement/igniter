package io.github.freewebmovement.igniter.x.persistence;

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
}
