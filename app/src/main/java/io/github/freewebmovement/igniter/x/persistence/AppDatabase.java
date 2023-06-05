package io.github.freewebmovement.igniter.x.persistence;
import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {EnabledPackage.class, Server.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract EnabledPackageDao enabledPackageDao();
    public abstract ServerDao serverDao();
}
