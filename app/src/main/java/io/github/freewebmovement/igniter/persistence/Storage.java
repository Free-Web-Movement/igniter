package io.github.freewebmovement.igniter.persistence;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.util.Log;

import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

import io.github.freewebmovement.igniter.R;

public class Storage {
    public static final String TAG = "STORAGE";
    Context context;
    public static final int CACHE = 0;
    public static final int FILES = 1;

    public File[] dirs = new File[2];

    public Storage(Context context) {
        this.context = context;
        dirs[CACHE] = context.getCacheDir();
        dirs[FILES] = context.getFilesDir();
    }

    public String getPath(int type, String filename) {
        switch (type) {
            case CACHE:
            case FILES:
                return new File(dirs[type], filename).getPath();
            default:
                return null;
        }
    }

    public static void print(String filename, String tag) {
        String result = new String(read(filename));
        Log.v(tag, result);
    }

    public static byte[] read(String filename) {
        try {
            File file = new File(filename);
            FileInputStream fis = new FileInputStream(file);
            int length = (int) file.length();
            byte[] content = new byte[length];
            int readBytes = fis.read(content);
            assert readBytes == length;
            return content;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void write(String filename, byte[] bytes) {
        try {
            File file = new File(filename);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(bytes);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static JSONObject readJSON(String filename) {
        try {
            String jsonStr = new String(read(filename));
            return new JSONObject(jsonStr);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void reset(String filename, int resId) {
        File file = new File(filename);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            byte[] bytes = readRawBytes(resId);
            write(filename, bytes);
        } catch (Exception e) {
            Log.e(TAG, "Error creating file: " + filename);
            e.printStackTrace();
        }
    }

    public String getCountryMmdbPath() {
        return getPath(FILES, context.getString(R.string.country_mmdb_config));
    }

    public String getClashConfigPath() {
        return getPath(FILES, context.getString(R.string.clash_config));
    }

    public String getTrojanConfigPath() {
        return getPath(FILES, context.getString(R.string.trojan_config));
    }

    public String getTrojanConfigListPath() {
        return getPath(FILES, context.getString(R.string.trojan_list_config));
    }

    public String getExemptedAppListPath() {
        return getPath(FILES, context.getString(R.string.exempted_app_list_config));
    }

    public String getCaCertPath() {
        return getPath(FILES, context.getString(R.string.ca_cert_config));
    }

    public boolean isExternalWritable() {
        String[] permissions = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) !=
                    PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public void reset() {
        String[] paths = {
                getCaCertPath(),
                getCountryMmdbPath(),
                getClashConfigPath()
        };
        int[] ids = {
                R.raw.cacert,
                R.raw.country,
                R.raw.clash_config
        };

        for (int i = 0; i < ids.length; i++) {
            reset(paths[i], ids[i]);
        }
    }

    public byte[] readRawBytes(int id) {
        try {
            Resources res = context.getResources();
            InputStream inputStream = res.openRawResource(id);
            byte[] b = new byte[inputStream.available()];
            inputStream.read(b);
            return b;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String readRawText(int id) {
        return new String(readRawBytes(id));
    }

    public JSONObject readRawJSON(int id) {

        try {
            String rawText = readRawText(id);
            return new JSONObject(rawText);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void deleteConfigs() {
        String[] paths = {
                getCaCertPath(),
                getCountryMmdbPath(),
                getClashConfigPath()
        };

        for (String filename : paths) {
            File file = new File(filename);
            file.delete();
        }
    }

    public void check() {
        String[] paths = {
                getCaCertPath(),
                getCountryMmdbPath(),
                getClashConfigPath(),
                getTrojanConfigPath()
        };
        int[] ids = {
                R.raw.cacert,
                R.raw.country,
                R.raw.clash_config,
                R.raw.config
        };
        for (int i = 0; i < ids.length; i++) {
            check(paths[i], ids[i]);
        }
    }

    public void check(String filename, int resId) {
        File file = new File(filename);
        Log.v(TAG, "Checking file: " + filename);
        if (!file.exists()) {
            Log.v(TAG, "File: " + filename + " not found! Resetting...");
            reset(filename, resId);
        }
    }
}
