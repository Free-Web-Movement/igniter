package io.github.freewebmovement.igniter.exempt.data;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.annotation.NonNull;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.freewebmovement.igniter.IgniterApplication;
import io.github.freewebmovement.igniter.persistence.Storage;

/**
 * Implementation of {@link ExemptAppDataSource}. This class reads and writes exempted app list in a
 * file. The exempted app package names will be written line by line in the file.
 * <br/>
 * Example:
 * <br/>
 * com.google.playstore
 * <br/>
 * io.github.freewebmovement.igniter
 * <br/>
 * com.android.something
 */
public class ExemptAppDataManager implements ExemptAppDataSource {
    private final PackageManager mPackageManager;
    IgniterApplication app;

    public ExemptAppDataManager(IgniterApplication app) {
        super();
        this.app = app;
        mPackageManager = app.getPackageManager();
    }

    @Override
    public void saveExemptAppInfoSet(Set<String> exemptAppPackageNames) {
        if (exemptAppPackageNames == null || exemptAppPackageNames.isEmpty()) {
            return;
        }
        StringBuilder exemptApps = new StringBuilder();
            for (String name : exemptAppPackageNames) {
                exemptApps.append(name).append("\n");
            }
        Storage.write(app.storage.getExemptedAppListPath(), exemptApps.toString().getBytes());
    }

    @NonNull
    private String[] readExemptAppListConfig() {
        String exemptApps = new String(Storage.read(app.storage.getExemptedAppListPath()));
        return exemptApps.split("\\r?\\n");
    }

    @Override
    public Set<String> loadExemptAppPackageNameSet() {
        String[] exemptAppPackageNames = readExemptAppListConfig();
        // filter uninstalled apps
        List<ApplicationInfo> applicationInfoList = queryCurrentInstalledApps();
        Set<String> installedAppPackageNames = new HashSet<>();
        for (ApplicationInfo applicationInfo : applicationInfoList) {
            installedAppPackageNames.add(applicationInfo.packageName);
        }
        Set<String> ret = new HashSet<>();
        for (String packageName : exemptAppPackageNames) {
            if (installedAppPackageNames.contains(packageName)) {
                ret.add(packageName);
            }
        }
        return ret;
    }

    private List<ApplicationInfo> queryCurrentInstalledApps() {
        int flags = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            flags |= PackageManager.MATCH_UNINSTALLED_PACKAGES | PackageManager.MATCH_DISABLED_COMPONENTS;
        } else {
            flags |= PackageManager.GET_UNINSTALLED_PACKAGES | PackageManager.GET_DISABLED_COMPONENTS;
        }
        return mPackageManager.getInstalledApplications(flags);
    }

    @Override
    public List<AppInfo> getAllAppInfoList() {
        List<ApplicationInfo> applicationInfoList = queryCurrentInstalledApps();
        List<AppInfo> appInfoList = new ArrayList<>(applicationInfoList.size());
        for (ApplicationInfo applicationInfo : applicationInfoList) {
            AppInfo appInfo = new AppInfo();
            appInfo.setAppName(mPackageManager.getApplicationLabel(applicationInfo).toString());
            appInfo.setPackageName(applicationInfo.packageName);
            appInfo.setIcon(mPackageManager.getApplicationIcon(applicationInfo));
            appInfoList.add(appInfo);
        }
        return appInfoList;
    }
}
