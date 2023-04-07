package io.github.freewebmovement.igniter.persistence;

import android.util.Log;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import clash.Clash;
import clash.ClashStartOptions;

public class ClashConfig {
    public static String TAG = "ClashConfig";
    // KEYS
    private static final String KEY_SOCKS_PORT = "socks-port";
    private static final String KEY_PROXIES = "proxies";
    private static final String KEY_NAME = "name";
    private static final String KEY_PORT = "port";
    private static final String KEY_TROJAN_NAME = "trojan";

    //    private static final int DEFAULT_PORT = 1080;
    private static final int DEFAULT_TROJAN_PORT = 1081;

    private String filename;
    public Map<String, Object> data;
    Yaml yaml;

    public ClashConfig(String filename) {
        try {
            this.filename = filename;
            FileInputStream fileInputStream = new FileInputStream(filename);
            yaml = new Yaml();
            data = (Map<String, Object>) yaml.load(fileInputStream);
            Log.wtf("CLASH", data.toString());
            fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public <T> void update(String key, T value) {
        update(data, key, value);
    }

    public <T> void update(Map<String, Object> data, String key, T value) {
        data.put(key, value);
    }

    public void save(String filename) throws IOException {
        File file = new File(filename);
        if (!file.exists()) {
            file.createNewFile();
        }
        PrintWriter writer = new PrintWriter(filename);
        yaml.dump(data, writer);
        writer.close();
    }

    public void setPort(int port) {
        try {
            data.put(KEY_SOCKS_PORT, port);
            save(filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setTrojanPort(int port) {
        List<Map<String, Object>> proxies = (List<Map<String, Object>>) data.get(KEY_PROXIES);
        try {
            if (proxies != null) {
                for (int i = 0; i < proxies.size(); i++) {
                    Map<String, Object> map = proxies.get(i);
                    if (Objects.equals(map.get(KEY_NAME), KEY_TROJAN_NAME)) {
                        map.put(KEY_PORT, port);
                        proxies.set(i, map);
                        break;
                    }
                }
            }
            data.put(KEY_PROXIES, proxies);
            save(filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getPort() {
        return (int) data.get(KEY_SOCKS_PORT);
    }

    public int getTrojanPort() {
        List<Map<String, Object>> proxies = (List<Map<String, Object>>) data.get(KEY_PROXIES);
        if (proxies != null) {
            for (int i = 0; i < proxies.size(); i++) {
                Map<String, Object> map = proxies.get(i);
                if (Objects.equals(map.get(KEY_NAME), KEY_TROJAN_NAME)) {
                    return (int) map.get(KEY_PORT);
                }
            }
        }
        return DEFAULT_TROJAN_PORT;
    }

    public static void startClash(String path, int port, int proxy, boolean enableLan) {
        ClashStartOptions clashStartOptions = new ClashStartOptions();
        clashStartOptions.setHomeDir(path);
        clashStartOptions.setTrojanProxyServer("127.0.0.1:" + proxy);
        if (enableLan) {
            clashStartOptions.setSocksListener("*:" + port);
        } else {
            clashStartOptions.setSocksListener("127.0.0.1:" + port);
        }
        clashStartOptions.setTrojanProxyServerUdpEnabled(true);
        Clash.start(clashStartOptions);
    }
}
