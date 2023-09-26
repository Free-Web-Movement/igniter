package io.github.freewebmovement.igniter.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.github.freewebmovement.igniter.R;
import io.github.freewebmovement.igniter.connection.API;
import io.github.freewebmovement.igniter.models.Server;

public class ServersActivity extends AppCompatActivity {

    Server[] servers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_servers);
    }

    protected void fetchJSON() {
        API api = new API();
        servers = api.server();
    }
}