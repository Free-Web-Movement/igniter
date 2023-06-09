package io.github.freewebmovement.igniter.servers.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import io.github.freewebmovement.igniter.IgniterApplication;
import io.github.freewebmovement.igniter.R;
import io.github.freewebmovement.igniter.persistence.ServerList;
import io.github.freewebmovement.igniter.persistence.Storage;
import io.github.freewebmovement.igniter.persistence.TrojanConfig;
import io.github.freewebmovement.igniter.servers.data.ServerListDataManager;
import io.github.freewebmovement.igniter.servers.fragment.ServerListFragment;
import io.github.freewebmovement.igniter.servers.presenter.ServerListPresenter;

public class ServerListActivity extends AppCompatActivity {
    public static final String KEY_TROJAN_CONFIG = "trojan_config";

    ServerListFragment fragment;
    ActivityResultLauncher<Intent> mGetAddBack = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            (result) -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Log.wtf("OK", "RESULT_OK");
                    new Thread(() -> {
                        List<TrojanConfig> trojanConfigList = ServerList.readDatabase(ServerListActivity.this);
                        ServerListActivity.this.runOnUiThread(() -> fragment.mServerListAdapter.replaceData(trojanConfigList));
                    });
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_list);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            Intent intent = new Intent(ServerListActivity.this, AddServerActivity.class);
            mGetAddBack.launch(intent);
        });
        initView();
    }

    public void initView() {
        FragmentManager fm = getSupportFragmentManager();
        fragment = (ServerListFragment) fm.findFragmentByTag(ServerListFragment.TAG);
        if (fragment == null) {
            fragment = ServerListFragment.newInstance();
        }
        Storage storage = IgniterApplication.getApplication().storage;
        new ServerListPresenter(fragment, new ServerListDataManager(storage.getTrojanConfigListPath()));
        fm.beginTransaction()
                .replace(R.id.parent_fl, fragment, ServerListFragment.TAG)
                .commitAllowingStateLoss();
    }
}
