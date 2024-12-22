package io.github.freewebmovement.igniter.persistence.data;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

public class AppInfo implements Cloneable {
    private String appName;
    private Drawable icon;
    private String packageName;
    private boolean enabled;

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @NonNull
    @Override
    protected Object clone() throws CloneNotSupportedException {
        AppInfo appInfo = (AppInfo) super.clone();
        appInfo.appName = appName;
        appInfo.icon = icon;
        appInfo.packageName = packageName;
        appInfo.enabled = enabled;
        return appInfo;
    }
}
