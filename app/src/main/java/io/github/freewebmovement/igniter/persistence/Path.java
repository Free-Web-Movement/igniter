package io.github.freewebmovement.igniter.persistence;

import android.content.Context;

import java.io.File;

import io.github.freewebmovement.igniter.R;

public class Path {
    public static final int CACHE = 0;
    public static final int FILES = 1;

    public File[] dirs = new File[2];
    public String countryMmdb;
    public String clashConfig;
    public String trojanConfig;
    public String trojanConfigList;
    public String exemptedAppList;
    public String caCert;
    public String systemApps;
    Context context;

    Path(Context context) {
        this.context = context;
        dirs[CACHE] = context.getCacheDir();
        dirs[FILES] = context.getFilesDir();
        countryMmdb = get(FILES, context.getString(R.string.country_mmdb_config));
        clashConfig = get(FILES, context.getString(R.string.clash_config));
        trojanConfig = get(FILES, context.getString(R.string.trojan_config));
        trojanConfigList = get(FILES, context.getString(R.string.trojan_list_config));
        exemptedAppList = get(FILES, context.getString(R.string.exempted_app_list_config));
        caCert = get(FILES, context.getString(R.string.ca_cert_config));
        systemApps = get(FILES, context.getString(R.string.system_apps_config));
    }

    public String get(int type, String filename) {
        switch (type) {
            case CACHE:
            case FILES:
                return new File(dirs[type], filename).getPath();
            default:
                return null;
        }
    }

}
