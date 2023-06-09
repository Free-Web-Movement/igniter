package io.github.freewebmovement.igniter.servers.activity;

import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Objects;

import io.github.freewebmovement.igniter.R;
import io.github.freewebmovement.igniter.x.persistence.AccessDatabase;
import io.github.freewebmovement.igniter.x.persistence.AppDatabase;
import io.github.freewebmovement.igniter.x.persistence.Server;
import io.github.freewebmovement.igniter.x.persistence.ServerDao;

public class AddServerActivity extends AppCompatActivity {

    public final String TAG = "ADD SERVER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_server);
    }


    private boolean checkText(EditText text, int resId) {
        if (text.getText().length() < 1) {
            Toast.makeText(AddServerActivity.this,
                    resId,
                    Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    public void onSave(View view) {

        EditText etRemoteHost = findViewById(R.id.remote_address);
        EditText etRemotePort = findViewById(R.id.remote_port);
        EditText etRemotePassword = findViewById(R.id.remote_password);
        EditText etLocalPort = findViewById(R.id.local_port);
        TextView tvTrojanURI = findViewById(R.id.trojan_uri);

        Object[][] checks = new Object[4][2];
        checks[0] = new Object[]{etRemoteHost, R.string.error_remote_address};
        checks[1] = new Object[]{etRemotePort, R.string.error_remote_port};
        checks[2] = new Object[]{etRemotePassword, R.string.error_remote_password};
        checks[3] = new Object[]{etLocalPort, R.string.error_local_port};

        for (Object[] check : checks) {
            if (!checkText((EditText) check[0], (int) check[1])) {
                return;
            }
        }

        String remoteAddress = etRemoteHost.getText().toString();
        String remotePort = etRemotePort.getText().toString();
        String remotePassword = etRemotePassword.getText().toString();
        String localPort = etLocalPort.getText().toString();

        String trojanURI = "trojan://" + remotePassword + "@" + remoteAddress + ":" + remotePort;

        tvTrojanURI.setText(trojanURI);

        Log.wtf(TAG, "ON Saving");
        Log.wtf(TAG, remoteAddress);
        Log.wtf(TAG, remotePort);
        Log.wtf(TAG, remotePassword);
        Log.wtf(TAG, localPort);
        new Thread(() -> {
            // A potentially time consuming task.
            try {
                AppDatabase db = AccessDatabase.getDatabase(getApplicationContext());
                ServerDao serverDao = db.serverDao();
                Server server = new Server();
                server.hostname = remoteAddress;
                server.port = remotePort;
                server.password = remotePassword;
                server.local_port = localPort;
                serverDao.insert(server);
            } catch (SQLiteConstraintException e) {
                e.printStackTrace();
                if (Objects.requireNonNull(e.getMessage()).startsWith("UNIQUE constraint failed")) {
                    runOnUiThread(() -> Toast.makeText(AddServerActivity.this,
                            R.string.error_dabase_unique_constraint_failed,
                            Toast.LENGTH_LONG).show());
                }
            }
        }).start();
    }
}