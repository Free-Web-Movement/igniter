package io.github.freewebmovement.igniter.persistence;

import static io.github.freewebmovement.igniter.constants.Clash.DEFAULT_TROJAN_PORT;
import static io.github.freewebmovement.igniter.constants.Clash.KEY_NAME;
import static io.github.freewebmovement.igniter.constants.Clash.KEY_PORT;
import static io.github.freewebmovement.igniter.constants.Clash.KEY_PROXIES;
import static io.github.freewebmovement.igniter.constants.Clash.KEY_SOCKS_PORT;
import static io.github.freewebmovement.igniter.constants.Clash.KEY_TROJAN_NAME;

import android.util.Log;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import clash.Clash;
import clash.ClashStartOptions;
import io.github.freewebmovement.igniter.IgniterApplication;
import io.github.freewebmovement.igniter.R;

public class ClashConfig {
    public static String TAG = "ClashConfig";
    private String filename;
    public Map<String, Object> data;
    Yaml yaml;

    public ClashConfig(String filename) {
        this.filename = filename;
        try {
            loadFromFile(filename);
        } catch (Exception e) {
            // A corrupt or partial config must not crash startup; restore the bundled default.
            Log.e(TAG, "Failed to parse clash config, restoring default", e);
            restoreDefault();
        }
    }

    private void loadFromFile(String file) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(file);
        yaml = new Yaml();
        data = (Map<String, Object>) yaml.load(fileInputStream);
        fileInputStream.close();
        if (data == null) {
            throw new IOException("empty clash config");
        }
    }

    /**
     * Re-reads the config from disk into memory. Call after editing the config
     * file so the in-memory data (used at connect time) matches what was saved.
     */
    public void reload() throws IOException {
        loadFromFile(filename);
    }

    private void restoreDefault() {
        try {
            Storage storage = new Storage(IgniterApplication.getApplication());
            storage.reset(storage.path.clashConfig, R.raw.clash_config);
            loadFromFile(storage.path.clashConfig);
        } catch (Exception e) {
            Log.e(TAG, "Failed to load default clash config", e);
            data = new HashMap<>();
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
            if (proxies == null || proxies.isEmpty()) {
                return;
            }
            for (int i = 0; i < proxies.size(); i++) {
                Map<String, Object> map = proxies.get(i);
                if (Objects.equals(map.get(KEY_NAME), KEY_TROJAN_NAME)) {
                    map.put(KEY_PORT, port);
                    proxies.set(i, map);
                    break;
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

    /**
     * Validates that this config is compatible with the embedded Clash library.
     * The library hard-requires that the first proxy entry is a "trojan" socks5
     * proxy, otherwise it calls log.Fatalf (which terminates the whole process).
     *
     * @return an error message if the config is unusable, or {@code null} if it is valid.
     */
    public String validateConfig() {
        if (data == null) {
            return "clash config is empty or invalid";
        }
        Object proxies = data.get(KEY_PROXIES);
        if (!(proxies instanceof List) || ((List<?>) proxies).isEmpty()) {
            return "clash config has no proxies entry";
        }
        Object first = ((List<?>) proxies).get(0);
        if (first instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) first;
            if ("socks5".equals(map.get("type")) && "trojan".equals(map.get("name"))) {
                return null;
            }
        }
        return "the first clash proxy entry must be type=socks5, name=trojan";
    }

    public static boolean startClash(String path, int port, int proxy, boolean enableLan) {
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
        // Clash.start() does not report failures back to Java; the embedded
        // library logs and exits the process on fatal errors. Verify the SOCKS
        // listener is actually up so we do not silently blackhole traffic.
        return NetWorkConfig.waitForPort("127.0.0.1", port, 5000);
    }
}
