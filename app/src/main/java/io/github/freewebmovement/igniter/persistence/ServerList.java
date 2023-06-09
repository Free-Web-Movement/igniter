package io.github.freewebmovement.igniter.persistence;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.freewebmovement.igniter.IgniterApplication;
import io.github.freewebmovement.igniter.x.persistence.AccessDatabase;
import io.github.freewebmovement.igniter.x.persistence.AppDatabase;
import io.github.freewebmovement.igniter.x.persistence.Server;
import io.github.freewebmovement.igniter.x.persistence.ServerDao;

public class ServerList {
    public static final String CONFIG_LIST_TAG = "TrojanConfigList";
    public String filename;
    IgniterApplication app;
    int currentIndex;

    public ServerList(IgniterApplication app) {
        this.app = app;
        filename = app.storage.getTrojanConfigListPath();
        currentIndex = app.trojanPreferences.getSelectedIndex();
    }

//    public void selectIndex(int index) {
//        app.trojanPreferences.setSelectedIndex(index);
//        currentIndex = index;
//    }

//    public TrojanConfig getDefaultConfig() {
//        List<TrojanConfig> list = ServerList.read(filename);
//        if (list == null || list.size() <= currentIndex) {
//            List<TrojanConfig> newList = new ArrayList<>();
//            newList.add(app.trojanConfig);
//            write(newList, filename);
//            currentIndex = 0;
//        }
//        return list.get(currentIndex);
//    }
    public static void write(List<TrojanConfig> configList, String filename) {
        try {
            JSONArray jsonArray = new JSONArray();
            for (TrojanConfig config : configList) {
                JSONObject jsonObject = config.toJSON();
                jsonArray.put(jsonObject);
            }
            String configStr = jsonArray.toString();
            File file = new File(filename);
            if (file.exists()) {
                file.delete();
            }
            OutputStream fos = new FileOutputStream(file);
            fos.write(configStr.getBytes());
            fos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static List<TrojanConfig> readDatabase(Context context) {
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
}
