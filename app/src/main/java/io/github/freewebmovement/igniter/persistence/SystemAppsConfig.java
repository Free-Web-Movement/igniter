package io.github.freewebmovement.igniter.persistence;

import io.github.freewebmovement.igniter.IgniterApplication;

public class SystemAppsConfig {
    // System Application records
    String[] records;
    IgniterApplication app;
    public SystemAppsConfig(IgniterApplication app) {
        this.app = app;
        records = Storage.readLines(app.storage.getSystemAppsPath());
    }

    public String[] getRecords() {
        return records;
    }
    public boolean isSystemApps(String packageName) {
        for(String filter : records ) {
            if (packageName.startsWith(filter)) {
                return true;
            }
        }
        return false;
    }
}
