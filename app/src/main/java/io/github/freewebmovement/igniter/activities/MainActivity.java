package io.github.freewebmovement.igniter.activities;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.VpnService;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.IOException;

import io.github.freewebmovement.igniter.IgniterApplication;
import io.github.freewebmovement.igniter.R;
import io.github.freewebmovement.igniter.common.os.Task;
import io.github.freewebmovement.igniter.common.os.Threads;
import io.github.freewebmovement.igniter.connection.TrojanConnection;
import io.github.freewebmovement.igniter.activities.exempt.fragment.ExemptAppFragment;
import io.github.freewebmovement.igniter.activities.exempt.presenter.ExemptAppPresenter;
import io.github.freewebmovement.igniter.activities.servers.fragment.ServerListFragment;
import io.github.freewebmovement.igniter.activities.servers.data.ServerListDataManager;
import io.github.freewebmovement.igniter.activities.servers.presenter.ServerListPresenter;
import io.github.freewebmovement.igniter.persistence.TrojanConfig;
import io.github.freewebmovement.igniter.proxy.aidl.ITrojanService;
import io.github.freewebmovement.igniter.services.ProxyService;

/**
 * Tab shell hosting the four pages: connect, servers, apps and rules.
 */
public class MainActivity extends AppCompatActivity implements TrojanConnection.Callback {
    private static final String TAG = "MainActivity";
    private static final int READ_WRITE_EXT_STORAGE_PERMISSION_REQUEST = 514;
    private static final String CONNECTION_TEST_URL = "https://www.google.com";

    private static final String TAG_HOME = "tab_home";
    private static final String TAG_SERVERS = "tab_servers";
    private static final String TAG_APPS = "tab_apps";
    private static final String TAG_RULES = "tab_rules";

    public static final int TAB_HOME = 0;
    public static final int TAB_SERVERS = 1;
    public static final int TAB_APPS = 2;
    public static final int TAB_RULES = 3;

    IgniterApplication app;

    // Launchers
    ActivityResultLauncher<Intent> vpnLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() != Activity.RESULT_OK) {
                        app.startProxyService();
                    }
                }
            });

    private HomeFragment mHomeFragment;
    private ServerListFragment mServerFragment;
    private ExemptAppFragment mAppsFragment;
    private RulesFragment mRulesFragment;
    private BottomNavigationView mBottomNav;
    private int mCurrentTab = TAB_HOME;
    private @ProxyService.ProxyState
    int proxyState = ProxyService.STATE_NONE;
    private final TrojanConnection connection = new TrojanConnection(false);
    private ITrojanService trojanService;
    private ServerListDataManager serverListDataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        app = IgniterApplication.getApplication();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setIcon(R.mipmap.ic_launcher);
            actionBar.setTitle(R.string.app_name);
        }

        serverListDataManager = new ServerListDataManager();

        mBottomNav = findViewById(R.id.bottomNav);
        mBottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.tab_servers) {
                switchTab(TAB_SERVERS);
            } else if (id == R.id.tab_apps) {
                switchTab(TAB_APPS);
            } else if (id == R.id.tab_rules) {
                switchTab(TAB_RULES);
            } else {
                switchTab(TAB_HOME);
            }
            return true;
        });

        createFragments();

        connection.connect(this, this);
        if (!app.storage.isExternalWritable() && ActivityCompat
                .shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            requestReadWriteExternalStoragePermission();
        }
    }

    private void createFragments() {
        FragmentManager fm = getSupportFragmentManager();
        mHomeFragment = (HomeFragment) fm.findFragmentByTag(TAG_HOME);
        if (mHomeFragment == null) {
            mHomeFragment = HomeFragment.newInstance();
        }
        mServerFragment = (ServerListFragment) fm.findFragmentByTag(TAG_SERVERS);
        if (mServerFragment == null) {
            mServerFragment = ServerListFragment.newInstance();
        }
        mAppsFragment = (ExemptAppFragment) fm.findFragmentByTag(TAG_APPS);
        if (mAppsFragment == null) {
            mAppsFragment = ExemptAppFragment.newInstance();
        }
        mRulesFragment = (RulesFragment) fm.findFragmentByTag(TAG_RULES);
        if (mRulesFragment == null) {
            mRulesFragment = RulesFragment.newInstance();
        }

        new ServerListPresenter(mServerFragment, new ServerListDataManager());
        new ExemptAppPresenter(mAppsFragment, app.exemptAppDataManager);

        FragmentTransaction ft = fm.beginTransaction();
        if (!mHomeFragment.isAdded()) {
            ft.add(R.id.fragmentContainer, mHomeFragment, TAG_HOME);
        }
        if (!mServerFragment.isAdded()) {
            ft.add(R.id.fragmentContainer, mServerFragment, TAG_SERVERS);
        }
        if (!mAppsFragment.isAdded()) {
            ft.add(R.id.fragmentContainer, mAppsFragment, TAG_APPS);
        }
        if (!mRulesFragment.isAdded()) {
            ft.add(R.id.fragmentContainer, mRulesFragment, TAG_RULES);
        }
        ft.hide(mServerFragment);
        ft.hide(mAppsFragment);
        ft.hide(mRulesFragment);
        ft.show(mHomeFragment);
        ft.commitAllowingStateLoss();
    }

    private void switchTab(int tab) {
        mCurrentTab = tab;
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.hide(mHomeFragment);
        ft.hide(mServerFragment);
        ft.hide(mAppsFragment);
        ft.hide(mRulesFragment);
        Fragment selected;
        int titleRes;
        switch (tab) {
            case TAB_SERVERS: {
                selected = mServerFragment;
                titleRes = R.string.tab_servers;
                break;
            }
            case TAB_APPS: {
                selected = mAppsFragment;
                titleRes = R.string.tab_apps;
                break;
            }
            case TAB_RULES: {
                selected = mRulesFragment;
                titleRes = R.string.tab_rules;
                break;
            }
            default: {
                selected = mHomeFragment;
                titleRes = R.string.app_name;
                break;
            }
        }
        ft.show(selected);
        ft.commitAllowingStateLoss();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(titleRes);
        }
        if (selected == mHomeFragment) {
            mHomeFragment.refreshServerInfo();
            mHomeFragment.updateState(proxyState);
        } else if (selected == mServerFragment) {
            mServerFragment.refresh();
        }
    }

    public void openServersTab() {
        switchTab(TAB_SERVERS);
    }

    public void openHomeTab() {
        switchTab(TAB_HOME);
    }

    /**
     * Applies a server selected from the Servers page: persists it and returns
     * to the connect page.
     */
    public void onServerSelected(TrojanConfig config) {
        if (config == null) {
            return;
        }
        config.setCaCertPath(app.storage.path.caCert);
        app.trojanConfig.fromJSON(config.toJSON());
        Threads.instance().runOnWorkThread(new Task() {
            @Override
            public void onRun() {
                TrojanConfig.write(app.trojanConfig, app.storage.path.trojanConfig);
                try {
                    app.clashConfig.save(app.storage.path.clashConfig);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                serverListDataManager.saveServerConfig(app.trojanConfig);
            }
        });
        openHomeTab();
        Toast.makeText(this, R.string.common_save_success, Toast.LENGTH_SHORT).show();
    }

    public void startVPN() {
        // start ProxyService
        Intent i = VpnService.prepare(getApplicationContext());
        if (i != null) {
            vpnLauncher.launch(i);
        } else {
            app.startProxyService();
        }
    }

    public void startProxy() {
        if (!app.trojanConfig.isValidRunningConfig()) {
            Toast.makeText(MainActivity.this,
                    R.string.invalid_configuration,
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (proxyState == ProxyService.STATE_NONE || proxyState == ProxyService.STOPPED) {
            TrojanConfig.write(
                    app.trojanConfig,
                    app.storage.path.trojanConfig
            );
            startVPN();
        }
    }

    public void stopProxy() {
        if (proxyState == ProxyService.STARTED) {
            app.stopProxyService();
        }
    }

    public boolean isProxyRunning() {
        return proxyState == ProxyService.STARTED;
    }

    public int getProxyState() {
        return proxyState;
    }

    private void requestReadWriteExternalStoragePermission() {
        new AlertDialog.Builder(this).setTitle(R.string.common_alert)
                .setMessage(R.string.main_write_external_storage_permission_requirement)
                .setPositiveButton(R.string.common_confirm, (dialog, which) -> {
                    dialog.dismiss();
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    }, READ_WRITE_EXT_STORAGE_PERMISSION_REQUEST);
                })
                .setNegativeButton(R.string.common_cancel, (dialog, which) -> dialog.dismiss()).show();
    }

    @Override
    public void onServiceConnected(final ITrojanService service) {
        Log.i(TAG, "onServiceConnected");
        trojanService = service;
        Threads.instance().runOnWorkThread(new Task() {
            @Override
            public void onRun() {
                try {
                    final int state = service.getState();
                    runOnUiThread(() -> updateViews(state));
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onServiceDisconnected() {
        Log.i(TAG, "onServiceDisconnected");
        trojanService = null;
    }

    private void updateViews(int state) {
        proxyState = state;
        if (mHomeFragment != null && mHomeFragment.isAdded()) {
            mHomeFragment.updateState(state);
        }
    }

    @Override
    public void onStateChanged(int state, String msg) {
        Log.i(TAG, "onStateChanged# state: " + state + " msg: " + msg);
        updateViews(state);
    }

    @Override
    public void onTestResult(final String testUrl, final boolean connected, final long delay, @NonNull final String error) {
        runOnUiThread(() -> showTestConnectionResult(testUrl, connected, delay, error));
    }

    private void showTestConnectionResult(String testUrl, boolean connected, long delay, @NonNull String error) {
        if (connected) {
            Toast.makeText(getApplicationContext(), getString(R.string.connected_to__in__ms,
                    testUrl, String.valueOf(delay)), Toast.LENGTH_LONG).show();
        } else {
            Log.e(TAG, "TestError: " + error);
            Toast.makeText(getApplicationContext(),
                    getString(R.string.failed_to_connect_to__,
                            testUrl, error),
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onBinderDied() {
        Log.i(TAG, "onBinderDied");
        connection.disconnect(this);
        connection.connect(this, this);
    }

    /**
     * Test connection by invoking {@link ITrojanService#testConnection(String)}.
     */
    public void testConnection() {
        ITrojanService service = trojanService;
        if (service == null) {
            showTestConnectionResult(CONNECTION_TEST_URL, false, 0L, "Trojan service is not available.");
        } else {
            try {
                service.testConnection(CONNECTION_TEST_URL);
            } catch (RemoteException e) {
                showTestConnectionResult(CONNECTION_TEST_URL, false, 0L, "Trojan service throws RemoteException.");
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_shell, menu);
        MenuItem settings = menu.findItem(R.id.action_view_settings);
        if (settings != null && settings.getIcon() != null) {
            Drawable wrapper = DrawableCompat.wrap(settings.getIcon());
            DrawableCompat.setTint(wrapper, Color.WHITE);
            settings.setIcon(wrapper);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_view_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean isAutoStart = app.trojanPreferences.isEnableAutoStart();
        if (isAutoStart) {
            Log.v("PROXY_STATE", "ProxyState = " + proxyState);
            switch (proxyState) {
                case ProxyService.STARTING:
                case ProxyService.STARTED:
                case ProxyService.STOPPING:
                    break;
                case ProxyService.STATE_NONE:
                case ProxyService.STOPPED:
                default:
                    startProxy();
                    break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        connection.disconnect(this);
    }
}
