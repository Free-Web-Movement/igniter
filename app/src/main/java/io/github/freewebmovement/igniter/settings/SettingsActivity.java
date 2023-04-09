package io.github.freewebmovement.igniter.settings;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.widget.TextView;

import io.github.freewebmovement.igniter.IgniterApplication;
import io.github.freewebmovement.igniter.R;
import io.github.freewebmovement.igniter.persistence.NetWorkConfig;
import io.github.freewebmovement.igniter.persistence.TrojanPreferences;

public class SettingsActivity extends AppCompatActivity {
    private SwitchCompat ipv6Switch;
    private SwitchCompat clashSwitch;
    private SwitchCompat enableLanSwitch;

    private SwitchCompat enableAutoStartSwitch;

    private SwitchCompat enableBootStartSwitch;
    private TextView clashLink;

    IgniterApplication app;

    private static final String TAG = "SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        app = IgniterApplication.getApplication();
        init();
        initListener();
        initFromPreferences(app.trojanPreferences);
    }

    private void init() {
        ipv6Switch = findViewById(R.id.ipv6Switch);
        clashSwitch = findViewById(R.id.clashSwitch);
        enableLanSwitch = findViewById(R.id.switch_enable_lan);
        enableAutoStartSwitch = findViewById(R.id.switch_enable_auto_start);
        enableBootStartSwitch = findViewById(R.id.switch_enable_boot_start);
        clashLink = findViewById(R.id.clashLink);
        clashLink.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void initFromPreferences(TrojanPreferences preferences) {
        ipv6Switch.setChecked(preferences.getEnableIPV6());
        clashSwitch.setChecked(preferences.getEnableClash());
        enableLanSwitch.setChecked(preferences.isEnableLan());
        enableAutoStartSwitch.setChecked(preferences.isEnableAutoStart());
        enableBootStartSwitch.setChecked(preferences.isEnableBootStart());

    }

    private void initListener() {
        ipv6Switch.setOnCheckedChangeListener((buttonView, isChecked) -> app.trojanPreferences.setEnableIPV6(isChecked));
        clashSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            app.trojanPreferences.setEnableClash(isChecked);
            int port;
            if (app.trojanPreferences.getEnableClash()) {
                port = app.clashConfig.getPort();
            } else {
                port = app.trojanConfig.getLocalPort();
            }
//            localOrClashPortText.setText(String.valueOf(port));
            NetWorkConfig.setPort(app, port);
            Log.wtf("MAIN", "" + app.clashConfig.getPort());
            Log.wtf("MAIN", "" + app.trojanConfig.getLocalPort());
        });

        enableLanSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> app.trojanPreferences.setEnableLan(isChecked));

        enableAutoStartSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.v(TAG, " auto start : " + isChecked);
            app.trojanPreferences.setEnableAutoStart(isChecked);
        });

        enableBootStartSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.v(TAG, " auto start : " + isChecked);
            app.trojanPreferences.setEnableBootStart(isChecked);
        });


    }
}