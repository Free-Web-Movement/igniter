package io.github.freewebmovement.igniter.persistence.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;
@Database(entities = {Server.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
//    public abstract EnabledPackageDao enabledPackageDao ();
    public abstract ServerDao serverDao();
//    public abstract UserDao userDao();
}
