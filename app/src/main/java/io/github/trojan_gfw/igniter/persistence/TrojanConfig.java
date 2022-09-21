package io.github.trojan_gfw.igniter.persistence;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.trojan_gfw.igniter.R;

public class TrojanConfig implements Parcelable {

    // Class Scoped Static Definitions

    // URI
    public static final String SCHEMA = "trojan";

    // Tags
    public static final String SINGLE_CONFIG_TAG = "TrojanConfig";
    public static final String CONFIG_LIST_TAG = "TrojanConfigList";

    // Top Level Keys
    public static final String KEY_LOCAL_ADDR = "local_addr";
    public static final String KEY_LOCAL_PORT = "local_port";
    public static final String KEY_REMOTE_ADDR = "remote_addr";
    public static final String KEY_REMOTE_PORT = "remote_port";
    public static final String KEY_PASSWORD = "password";

    // SSL Sub Keys
    public static final String KEY_SSL = "ssl";
    public static final String KEY_VERIFY_CERT = "verify";
    public static final String KEY_CA_CERT_PATH = "cert";
    public static final String KEY_CIPHER_LIST = "cipher";
    public static final String KEY_TLS13_CIPHER_LIST = "cipher_tls13";

    private static TrojanConfig instance;
    private static JSONObject defaultJSON;

    // Object Scoped members
    private String localAddr;
    private int localPort;
    private String remoteAddr;
    private int remotePort;
    private String password;
    private boolean verifyCert;
    private String caCertPath;
    private String cipherList;
    private String tls13CipherList;

    private JSONObject json;

    // Global Config
    public static void init(Storage storage) {

        defaultJSON = storage.readRawJSON(R.raw.config);

        String filename = storage.getTrojanConfigPath();
        TrojanConfig trojanConfig = TrojanConfig.read(filename);

        if (trojanConfig == null) {
            trojanConfig = new TrojanConfig().fromJSON(defaultJSON);
            TrojanConfig.write(trojanConfig, filename);
        }

        trojanConfig.setCaCertPath(storage.getCaCertPath());
        setInstance(trojanConfig);
    }

    public static TrojanConfig getInstance() {
        return instance;
    }

    public static void setInstance(TrojanConfig trojanConfig) {
        instance = trojanConfig;
    }


    // Local Config

    public TrojanConfig() {
        this.fromJSON(defaultJSON);
    }

    // Parcel processing

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(localAddr);
        dest.writeInt(localPort);
        dest.writeString(remoteAddr);
        dest.writeInt(remotePort);
        dest.writeString(password);
        dest.writeByte((byte) (verifyCert ? 1 : 0));
        dest.writeString(caCertPath);
        dest.writeString(cipherList);
        dest.writeString(tls13CipherList);
    }

    protected TrojanConfig readFromParcel(Parcel in) {
        localAddr = in.readString();
        localPort = in.readInt();
        remoteAddr = in.readString();
        remotePort = in.readInt();
        password = in.readString();
        verifyCert = in.readByte() != 0;
        caCertPath = in.readString();
        cipherList = in.readString();
        tls13CipherList = in.readString();
        return this;
    }

    public static final Creator<TrojanConfig> CREATOR = new Creator<TrojanConfig>() {
        @Override
        public TrojanConfig createFromParcel(Parcel in) {
            return new TrojanConfig().readFromParcel(in);
        }

        @Override
        public TrojanConfig[] newArray(int size) {
            return new TrojanConfig[size];
        }
    };

    // JSON Processing
    public JSONObject toJSON() throws JSONException {
        json.put(KEY_LOCAL_ADDR, this.localAddr);
        json.put(KEY_LOCAL_PORT, this.localPort);
        json.put(KEY_REMOTE_ADDR, this.remoteAddr);
        json.put(KEY_REMOTE_PORT, this.remotePort);
        json.put(KEY_PASSWORD, new JSONArray().put(this.password));
        JSONObject ssl = json.getJSONObject(KEY_SSL);
        ssl.put(KEY_VERIFY_CERT, this.verifyCert);
        ssl.put(KEY_CA_CERT_PATH, this.caCertPath);
        ssl.put(KEY_CIPHER_LIST, this.cipherList);
        ssl.put(KEY_TLS13_CIPHER_LIST, this.tls13CipherList);
        assert new File(this.caCertPath).exists();
        json.put(KEY_SSL, ssl);
        return json;
    }

    public String toJSONString() throws JSONException {
        return toJSON().toString();
    }

    public TrojanConfig fromJSON(JSONObject from) {
        try {
            json = new JSONObject(from.toString());
            this.localAddr = json.getString(KEY_LOCAL_ADDR);
            this.localPort = json.getInt(KEY_LOCAL_PORT);
            this.remoteAddr = json.getString(KEY_REMOTE_ADDR);
            this.remotePort = json.getInt(KEY_REMOTE_PORT);
            this.password = json.getJSONArray(KEY_PASSWORD).getString(0);
            JSONObject ssl = json.getJSONObject(KEY_SSL);
            this.verifyCert = ssl.getBoolean(KEY_VERIFY_CERT);
            this.caCertPath = ssl.getString(KEY_CA_CERT_PATH);
            this.cipherList = ssl.getString(KEY_CIPHER_LIST);
            this.tls13CipherList = ssl.getString(KEY_TLS13_CIPHER_LIST);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return this;
    }

    public boolean isValidRunningConfig() {
        return !TextUtils.isEmpty(this.caCertPath)
                && !TextUtils.isEmpty(this.remoteAddr)
                && !TextUtils.isEmpty(this.password);
    }

    public int getLocalPort() {
        return localPort;
    }

    public void setLocalPort(int localPort) {
        this.localPort = localPort;
    }

    public String getRemoteAddr() {
        return remoteAddr;
    }

    public void setRemoteAddr(String remoteAddr) {
        this.remoteAddr = remoteAddr;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public void setRemotePort(int remotePort) {
        this.remotePort = remotePort;
    }

    public String getPassword() {
        return password;
    }

    public TrojanConfig setPassword(String password) {
        this.password = password;
        return this;
    }

    public boolean getVerifyCert() {
        return verifyCert;
    }

    public void setVerifyCert(boolean verifyCert) {
        this.verifyCert = verifyCert;
    }


    public void setCaCertPath(String caCertPath) {
        this.caCertPath = caCertPath;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof TrojanConfig)) {
            return false;
        }
        TrojanConfig that = (TrojanConfig) obj;
        return (paramEquals(remoteAddr, that.remoteAddr) && paramEquals(remotePort, that.remotePort)
                && paramEquals(localAddr, that.localAddr) && paramEquals(localPort, that.localPort))
                && paramEquals(password, that.password) && paramEquals(verifyCert, that.verifyCert)
                && paramEquals(caCertPath, that.caCertPath)
                && paramEquals(cipherList, that.cipherList) && paramEquals(tls13CipherList, that.tls13CipherList);
    }

    private static boolean paramEquals(Object a, Object b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.equals(b);
    }

    public static TrojanConfig read(String filename) {

        JSONObject json = Storage.readJSON(filename);
        TrojanConfig trojanConfig = new TrojanConfig();
        return trojanConfig.fromJSON(json);
    }

    public static void write(TrojanConfig trojanConfig, String filename) {
        try {
            String config = trojanConfig.toJSONString();
            File file = new File(filename);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(config.getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static <T> void update(String trojanConfigPath, String key, T v) {
        File file = new File(trojanConfigPath);
        if (file.exists()) {
            try {
                String str;
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] content = new byte[(int) file.length()];
                    fis.read(content);
                    str = new String(content);
                }
                JSONObject json = new JSONObject(str);
                json.put(key, v);
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(json.toString().getBytes());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public static void writeList(List<TrojanConfig> configList, String filename) {
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
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    @NonNull
    public static List<TrojanConfig> readList(String filename) {
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


    public static String toURIString(TrojanConfig trojanConfig) {
        URI trojanUri;
        try {
            trojanUri = new URI(SCHEMA,
                    trojanConfig.getPassword(),
                    trojanConfig.getRemoteAddr(),
                    trojanConfig.getRemotePort(),
                    null, null, null);
        } catch (java.net.URISyntaxException e) {
            e.printStackTrace();
            return null;
        }

        return trojanUri.toString();
    }

    public static TrojanConfig fromURIString(String URIString) {
        URI trojanUri;
        try {
            trojanUri = new URI(URIString);
        } catch (java.net.URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
        String scheme = trojanUri.getScheme();
        if (scheme == null) {
            return null;
        }
        if (!scheme.equals(SCHEMA))
            return null;
        String host = trojanUri.getHost();
        int port = trojanUri.getPort();
        String userInfo = trojanUri.getUserInfo();

        TrojanConfig retConfig = new TrojanConfig();
        retConfig.setRemoteAddr(host);
        retConfig.setRemotePort(port);
        retConfig.setPassword(userInfo);
        return retConfig;
    }

}
