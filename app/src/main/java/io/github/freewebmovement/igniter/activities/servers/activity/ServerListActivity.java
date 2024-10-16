package io.github.freewebmovement.igniter.activities.servers.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import io.github.freewebmovement.igniter.R;
import io.github.freewebmovement.igniter.activities.servers.data.ServerListDataManager;
import io.github.freewebmovement.igniter.activities.servers.fragment.ServerListFragment;
import io.github.freewebmovement.igniter.activities.servers.presenter.ServerListPresenter;

public class ServerListActivity extends AppCompatActivity {
    public static final String KEY_TROJAN_CONFIG = "trojan_config";

    ServerListFragment fragment;
    ActivityResultLauncher<Intent> mGetAddBack = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            (result) -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent intent = getIntent();
                    finish();
                    startActivity(intent);
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
        new ServerListPresenter(fragment, new ServerListDataManager());
        fm.beginTransaction()
                .replace(R.id.parent_fl, fragment, ServerListFragment.TAG)
                .commitAllowingStateLoss();
    }
}
