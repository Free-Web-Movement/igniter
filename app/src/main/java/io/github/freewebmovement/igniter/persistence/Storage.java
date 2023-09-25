package io.github.freewebmovement.igniter.persistence;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.util.Log;

import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import io.github.freewebmovement.igniter.R;

public class Storage {
    public static final String TAG = "STORAGE";
    public Path path;

    public Storage(Context context) {
        this.path = new Path(context);
    }

    public static void print(String filename, String tag) {
        String result = new String(read(filename));
        Log.v(tag, result);
    }

    public static byte[] read(String filename) {
        try {
            File file = new File(filename);
            if (!file.exists()) {
                return null;
            }
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

    public boolean isExternalWritable() {
        String[] permissions = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(path.context, permission) !=
                    PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public void reset() {
        String[] paths = {
                path.caCert,
                path.countryMmdb,
                path.clashConfig
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
            Resources res = path.context.getResources();
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
                path.caCert,
                path.countryMmdb,
                path.clashConfig
        };

        for (String filename : paths) {
            File file = new File(filename);
            file.delete();
        }
    }

    public void check() {

        String[] paths = {
                path.caCert,
                path.countryMmdb,
                path.clashConfig,
                path.trojanConfig,
                path.systemApps
        };
        int[] ids = {
                R.raw.cacert,
                R.raw.country,
                R.raw.clash_config,
                R.raw.config,
                R.raw.system_apps
        };
        for (int i = 0; i < ids.length - 1; i++) {
            check(paths[i], ids[i]);
        }

        // Keep this line before system apps filters can be edited.
        reset(paths[ids.length - 1], ids[ids.length - 1]);
    }

    public void check(String filename, int resId) {
        File file = new File(filename);
        Log.v(TAG, "Checking file: " + filename);
        if (!file.exists()) {
            Log.v(TAG, "File: " + filename + " not found! Resetting...");
            reset(filename, resId);
        }
    }

    public static String[] readLines(String filename) {
        ArrayList<String> records = new ArrayList<String>();
        try {
            FileInputStream is;
            BufferedReader reader;
            final File file = new File(filename);
            if (file.exists()) {
                is = new FileInputStream(file);
                reader = new BufferedReader(new InputStreamReader(is));
                String line = reader.readLine();
                while (line != null) {
                    records.add(line);
                    line = reader.readLine();
                }
                String[] ret = new String[records.size()];
                records.toArray(ret);
                return ret;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
