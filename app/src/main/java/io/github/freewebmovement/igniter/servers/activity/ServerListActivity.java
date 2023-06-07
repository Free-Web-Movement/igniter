package io.github.freewebmovement.igniter.servers.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import io.github.freewebmovement.igniter.IgniterApplication;
import io.github.freewebmovement.igniter.R;
import io.github.freewebmovement.igniter.persistence.Storage;
import io.github.freewebmovement.igniter.servers.data.ServerListDataManager;
import io.github.freewebmovement.igniter.servers.fragment.ServerListFragment;
import io.github.freewebmovement.igniter.servers.presenter.ServerListPresenter;

public class ServerListActivity extends AppCompatActivity {
    public static final String KEY_TROJAN_CONFIG = "trojan_config";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_list);

        FragmentManager fm = getSupportFragmentManager();
        ServerListFragment fragment = (ServerListFragment) fm.findFragmentByTag(ServerListFragment.TAG);
        if (fragment == null) {
            fragment = ServerListFragment.newInstance();
        }
        Storage storage = IgniterApplication.getApplication().storage;
        new ServerListPresenter(fragment, new ServerListDataManager(storage.getTrojanConfigListPath()));
        fm.beginTransaction()
                .replace(R.id.parent_fl, fragment, ServerListFragment.TAG)
                .commitAllowingStateLoss();

        FloatingActionButton fab = findViewById(R.id.fab);
        Context context = getApplicationContext();
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ServerListActivity.this, AddServerActivity.class);
                ServerListActivity.this.startActivity(intent);
            }
        });
    }
}
