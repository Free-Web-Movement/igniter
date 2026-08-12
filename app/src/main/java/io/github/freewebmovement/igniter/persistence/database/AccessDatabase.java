package io.github.freewebmovement.igniter.persistence.database;
import android.content.Context;

import androidx.room.Room;

import java.util.ArrayList;
import java.util.List;

import io.github.freewebmovement.igniter.persistence.TrojanConfig;

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

    public static void deleteServer(Context context, String remoteAddress, int port) {
        AppDatabase appDatabase = AccessDatabase.getDatabase(context);
        ServerDao serverDao = appDatabase.serverDao();
        serverDao.deleteByUniquePair(remoteAddress, port);
    }

    public static List<TrojanConfig> readServers(Context context) {
        AppDatabase appDatabase = AccessDatabase.getDatabase(context);
        ServerDao serverDao = appDatabase.serverDao();
        List<Server> servers = serverDao.all();
        List<TrojanConfig> configList = new ArrayList<>(servers.size());
        for(Server server: servers) {
            TrojanConfig tc = new TrojanConfig();
            tc.setRemoteAddr(server.hostname);
            tc.setRemotePort(server.port);
            tc.setPassword(server.password);
            tc.setLocalPort(server.local_port);
            configList.add(tc);
        }
        return configList;
    }

    /** Replaces the whole server list with the given configs, atomically. */
    public static void replaceServers(Context context, List<TrojanConfig> configs) {
        AppDatabase appDatabase = AccessDatabase.getDatabase(context);
        ServerDao serverDao = appDatabase.serverDao();
        appDatabase.runInTransaction(() -> {
            serverDao.deleteAll();
            for (TrojanConfig config : configs) {
                serverDao.insert(toServer(config));
            }
        });
    }

    /** Persists a server unless one with the same hostname/port already exists. */
    public static void insertServerIfMissing(Context context, TrojanConfig config) {
        if (config == null || config.getRemoteAddr() == null || config.getRemoteAddr().isEmpty()) {
            return;
        }
        AppDatabase appDatabase = AccessDatabase.getDatabase(context);
        ServerDao serverDao = appDatabase.serverDao();
        if (serverDao.findByHostAndPort(config.getRemoteAddr(), config.getRemotePort()) != null) {
            return;
        }
        serverDao.insert(toServer(config));
    }

    private static Server toServer(TrojanConfig config) {
        Server server = new Server();
        server.hostname = config.getRemoteAddr();
        server.port = config.getRemotePort();
        server.password = config.getPassword();
        server.local_port = config.getLocalPort();
        server.localhost = "127.0.0.1";
        return server;
    }
}
