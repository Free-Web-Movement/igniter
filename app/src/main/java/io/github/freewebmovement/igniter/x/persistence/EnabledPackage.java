package io.github.trojan_gfw.igniter.x.persistence;


import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "enabled_packages")
public class EnabledPackage {
    @PrimaryKey
    @ColumnInfo(name = "package_name")
    public String packageName;
}
