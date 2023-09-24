package io.github.freewebmovement.igniter.persistence;

import static io.github.freewebmovement.igniter.constants.Trojan.KEY_CA_CERT_PATH;
import static io.github.freewebmovement.igniter.constants.Trojan.KEY_CIPHER_LIST;
import static io.github.freewebmovement.igniter.constants.Trojan.KEY_LOCAL_ADDR;
import static io.github.freewebmovement.igniter.constants.Trojan.KEY_LOCAL_PORT;
import static io.github.freewebmovement.igniter.constants.Trojan.KEY_PASSWORD;
import static io.github.freewebmovement.igniter.constants.Trojan.KEY_REMOTE_ADDR;
import static io.github.freewebmovement.igniter.constants.Trojan.KEY_REMOTE_PORT;
import static io.github.freewebmovement.igniter.constants.Trojan.KEY_SSL;
import static io.github.freewebmovement.igniter.constants.Trojan.KEY_TLS13_CIPHER_LIST;
import static io.github.freewebmovement.igniter.constants.Trojan.KEY_VERIFY_CERT;
import static io.github.freewebmovement.igniter.constants.Trojan.SCHEMA;

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

import io.github.freewebmovement.igniter.R;

public class TrojanConfig implements Parcelable {

    private static TrojanConfig instance = null;
    private static JSONObject defaultJSON = null;

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

    public static TrojanConfig getInstance(Storage storage) {
        if (instance != null) {
            return instance;
        }
        defaultJSON = storage.readRawJSON(R.raw.config);

        String filename = storage.getTrojanConfigPath();
        TrojanConfig trojanConfig = TrojanConfig.read(filename);

        if (trojanConfig == null) {
            trojanConfig = new TrojanConfig().fromJSON(defaultJSON);
            TrojanConfig.write(trojanConfig, filename);
        }

        trojanConfig.setCaCertPath(storage.getCaCertPath());
        instance = trojanConfig;
        return  instance;
    }


    // Local Config

    public TrojanConfig() {
        this.fromJSON(defaultJSON);
    }

    public static JSONObject getDefaultJSON(Storage storage) {
        return storage.readRawJSON(R.raw.config);
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
    public JSONObject toJSON() {
        try {
            JSONObject json = new JSONObject();
            json.put(KEY_LOCAL_ADDR, this.localAddr);
            json.put(KEY_LOCAL_PORT, this.localPort);
            json.put(KEY_REMOTE_ADDR, this.remoteAddr);
            json.put(KEY_REMOTE_PORT, this.remotePort);
            json.put(KEY_PASSWORD, new JSONArray().put(this.password));
            JSONObject ssl = new JSONObject();
            ssl.put(KEY_VERIFY_CERT, this.verifyCert);
            ssl.put(KEY_CA_CERT_PATH, this.caCertPath);
            ssl.put(KEY_CIPHER_LIST, this.cipherList);
            ssl.put(KEY_TLS13_CIPHER_LIST, this.tls13CipherList);
            assert new File(this.caCertPath).exists();
            json.put(KEY_SSL, ssl);
            return json;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String toJSONString() {
        return toJSON().toString();
    }

    public TrojanConfig fromJSON(JSONObject json) {
        try {
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
        if (remoteAddr == null || remoteAddr.length() == 0) {
            return "0.0.0.0";
        }
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
