package io.github.freewebmovement.igniter.persistence.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface EnabledPackageDao {
    @Query("SELECT * from enabled_packages")
    List<EnabledPackage> getPackages();
    @Query("SELECT * FROM enabled_packages WHERE package_name = :packageName")
    EnabledPackage getPackage(String packageName);
    @Insert
    void insert(EnabledPackage... packages);
    @Delete
    void delete(EnabledPackage aPackage);
}