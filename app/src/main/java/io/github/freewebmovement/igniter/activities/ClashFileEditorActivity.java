package io.github.freewebmovement.igniter.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.nio.charset.StandardCharsets;

import io.github.freewebmovement.igniter.IgniterApplication;
import io.github.freewebmovement.igniter.R;
import io.github.freewebmovement.igniter.persistence.Storage;

public class ClashFileEditorActivity extends AppCompatActivity {
    IgniterApplication app;
    EditText clashConfigEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clash_file_editor);
        app = IgniterApplication.getApplication();
        clashConfigEditor = findViewById(R.id.edit_text_clash_file);
        onLoad(null);
    }

    public void onLoad(View view) {
        new Thread(() -> {
            byte[] content = Storage.read(app.storage.path.clashConfig);
            runOnUiThread(() -> clashConfigEditor.setText(content == null
                    ? "" : new String(content, StandardCharsets.UTF_8)));
        }, "clash-config-load").start();
    }

    public void onReset(View view) {
        new Thread(() -> {
            String clashConfigText = app.storage.readRawText(R.raw.clash_config);
            runOnUiThread(() -> clashConfigEditor.setText(clashConfigText));
        }, "clash-config-reset").start();
    }

    public void onSave(View view) {
        final String content = clashConfigEditor.getText().toString();
        new Thread(() -> {
            Storage.write(app.storage.path.clashConfig, content.getBytes(StandardCharsets.UTF_8));
            // Reload so the in-memory config used at connect time matches the file.
            try {
                app.clashConfig.reload();
            } catch (Exception e) {
                e.printStackTrace();
            }
            runOnUiThread(() -> Toast.makeText(this, R.string.main_save_success, Toast.LENGTH_SHORT).show());
        }, "clash-config-save").start();
    }
}
