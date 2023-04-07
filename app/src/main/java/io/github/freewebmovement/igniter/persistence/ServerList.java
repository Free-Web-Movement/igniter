package io.github.freewebmovement.igniter.persistence;

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

    public void selectIndex(int index) {
        app.trojanPreferences.setSelectedIndex(index);
        currentIndex = index;
    }

    public TrojanConfig getDefaultConfig() {
        List<TrojanConfig> list = ServerList.read(filename);
        if (list == null || list.size() <= currentIndex) {
            List<TrojanConfig> newList = new ArrayList<>();
            newList.add(app.trojanConfig);
            write(newList, filename);
            currentIndex = 0;
        }
        return list.get(currentIndex);
    }
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

    public static List<TrojanConfig> read(String filename) {
        File file = new File(filename);
        if (!file.exists()) {
            return Collections.emptyList();
        }
        try (InputStream fis = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            String json = new String(data);
            JSONArray jsonArr = new JSONArray(json);
            int len = jsonArr.length();
            List<TrojanConfig> list = new ArrayList<>(len);
            for (int i = 0; i < len; i++) {
                TrojanConfig tc = new TrojanConfig().fromJSON(jsonArr.getJSONObject(i));
                list.add(tc);
            }
            return list;
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }
}
