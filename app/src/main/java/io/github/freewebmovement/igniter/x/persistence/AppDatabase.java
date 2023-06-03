package io.github.trojan_gfw.igniter.x.persistence;
import androidx.room.Database;
@Database(entities = {EnabledPackage.class}, version = 1)
public abstract class AppDatabase {
    public abstract EnabledPackageDao enabledPackageDao();
}
