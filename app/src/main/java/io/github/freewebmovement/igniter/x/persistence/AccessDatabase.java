package io.github.freewebmovement.igniter.x.persistence;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import android.content.Context;

import androidx.room.Room;

public class AccessDatabase {
    public static final String databaseName = "trojan.db";
    public static AppDatabase db;
    public static AppDatabase getDatabase (Context context) {
        if (db == null) {
            db = Room.databaseBuilder(context,
                    AppDatabase.class, databaseName).build();
        }
        return  db;
    }
}
