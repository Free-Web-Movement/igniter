package io.github.freewebmovement.igniter;

import android.app.Application;
import android.content.Intent;

import androidx.core.content.ContextCompat;

import io.github.freewebmovement.igniter.persistence.ClashConfig;
import io.github.freewebmovement.igniter.persistence.Storage;
import io.github.freewebmovement.igniter.persistence.SystemAppsConfig;
import io.github.freewebmovement.igniter.persistence.TrojanConfig;
import io.github.freewebmovement.igniter.persistence.TrojanPreferences;
import io.github.freewebmovement.igniter.persistence.data.ExemptAppDataManager;
import io.github.freewebmovement.igniter.services.ProxyService;
import io.github.freewebmovement.igniter.activities.MainActivity;

public class IgniterApplication extends Application {
    public static IgniterApplication instance;

    public static IgniterApplication getApplication() {
        return instance;
    }

    // Sharable Singletons
    public Storage storage;
    public ClashConfig clashConfig;
    public TrojanConfig trojanConfig;
    public TrojanPreferences trojanPreferences;
    public SystemAppsConfig systemAppsConfig;
    public ExemptAppDataManager exemptAppDataManager;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        init();
    }

    public void init() {
        trojanPreferences = new TrojanPreferences(this);

        storage = new Storage(this);
        storage.check();
        // Make sure the CA file exists;
        trojanConfig = TrojanConfig.getInstance(storage);
        clashConfig = new ClashConfig(storage.path.clashConfig);
        systemAppsConfig = new SystemAppsConfig(this);
        exemptAppDataManager = new ExemptAppDataManager(this);
    }

    public void startProxyService() {
        Intent intent = new Intent(this, ProxyService.class);
        ContextCompat.startForegroundService(this, intent);
    }

    public void stopProxyService() {
        Intent intent = new Intent(this.getString(R.string.stop_service));
        intent.setPackage(getPackageName());
        this.sendBroadcast(intent);
    }

    public void startLauncherActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        this.startActivity(intent);
    }
}
