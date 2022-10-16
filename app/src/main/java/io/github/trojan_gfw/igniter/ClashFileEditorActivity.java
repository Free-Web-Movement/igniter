package io.github.trojan_gfw.igniter;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import io.github.trojan_gfw.igniter.persistence.Storage;

public class ClashFileEditorActivity extends AppCompatActivity {
    IgniterApplication app;
    EditText clashConfigEditor;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clash_file_editor);
        app = IgniterApplication.getApplication();
        clashConfigEditor = findViewById(R.id.edit_text_clash_file);
    }

    public void onLoad(View view) {
        String clashConfigText = new String(Storage.read(app.storage.getClashConfigPath()));
        clashConfigEditor.setText(clashConfigText);
    }

    public void onReset(View view) {
        String clashConfigText = app.storage.readRawText(R.raw.clash_config);
        clashConfigEditor.setText(clashConfigText);
    }

    public void onSave(View view) {
       Storage.write(app.storage.getClashConfigPath(), clashConfigEditor.getText().toString().getBytes());
    }
}