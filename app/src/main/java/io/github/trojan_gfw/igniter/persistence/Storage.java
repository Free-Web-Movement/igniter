package io.github.trojan_gfw.igniter.persistence;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;

import io.github.trojan_gfw.igniter.LogHelper;
import io.github.trojan_gfw.igniter.R;

public class Storage {
    Context context;
    public static final int CACHE = 0;
    public static final int FILES = 1;
    public static final int EXTERNAL = 2;

    public File[] dirs = new File[3];

    public Storage(Context context) {
        this.context = context;
        dirs[CACHE] = context.getCacheDir();
        dirs[FILES] = context.getFilesDir();
        dirs[EXTERNAL] = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        dirs[EXTERNAL] = new File(dirs[EXTERNAL], context.getString(R.string.app_name));
    }

    public String getPath(int type, String filename) {
        switch (type) {
            case CACHE:
            case FILES:
            case EXTERNAL:
                return new File(dirs[type], filename).getPath();
            default:
                return null;
        }
    }

    public static void print(String filename, String tag) {
        try {
            File file = new File(filename);
            FileInputStream fis = new FileInputStream(file);
            int length = (int)file.length();
            byte[] content = new byte[length];
            int readBytes = fis.read(content);
            assert readBytes == length;
            LogHelper.v(tag, new String(content));
        } catch (Exception e) {
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
        return getPath(EXTERNAL, context.getString(R.string.exempted_app_list_config));
    }

    public String getCaCertPath() {
        return getPath(CACHE, context.getString(R.string.ca_cert_config));
    }
}
