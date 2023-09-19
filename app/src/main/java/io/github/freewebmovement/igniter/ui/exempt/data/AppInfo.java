package io.github.freewebmovement.igniter.ui.exempt.data;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

public class AppInfo implements Cloneable {
    private String appName;
    private Drawable icon;
    private String packageName;
    private boolean exempt;

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

    public boolean isExempt() {
        return exempt;
    }

    public void setExempt(boolean exempt) {
        this.exempt = exempt;
    }

    @NonNull
    @Override
    protected Object clone() throws CloneNotSupportedException {
        AppInfo appInfo = (AppInfo) super.clone();
        appInfo.appName = appName;
        appInfo.icon = icon;
        appInfo.packageName = packageName;
        appInfo.exempt = exempt;
        return appInfo;
    }
}
