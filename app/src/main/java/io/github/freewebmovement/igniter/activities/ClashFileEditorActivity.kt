package io.github.freewebmovement.igniter.activities

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.github.freewebmovement.igniter.IgniterApplication
import io.github.freewebmovement.igniter.R
import io.github.freewebmovement.igniter.persistence.Storage
import java.nio.charset.StandardCharsets

class ClashFileEditorActivity : AppCompatActivity() {
    lateinit var app: IgniterApplication
    lateinit var clashConfigEditor: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_clash_file_editor)
        app = IgniterApplication.getApplication()
        clashConfigEditor = findViewById(R.id.edit_text_clash_file)
        onLoad(null)
    }

    fun onLoad(view: View?) {
        Thread({
            val content = Storage.read(app.storage.path.clashConfig!!)
            runOnUiThread {
                clashConfigEditor.setText(
                    content?.let { String(it, StandardCharsets.UTF_8) } ?: "")
            }
        }, "clash-config-load").start()
    }

    fun onReset(view: View?) {
        Thread({
            val clashConfigText = app.storage.readRawText(R.raw.clash_config)
            runOnUiThread { clashConfigEditor.setText(clashConfigText) }
        }, "clash-config-reset").start()
    }

    fun onSave(view: View?) {
        val content = clashConfigEditor.text.toString()
        Thread({
            Storage.write(app.storage.path.clashConfig!!,
                content.toByteArray(StandardCharsets.UTF_8))
            // Reload so the in-memory config used at connect time matches the file.
            try {
                app.clashConfig.reload()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            runOnUiThread {
                Toast.makeText(this, R.string.main_save_success, Toast.LENGTH_SHORT).show()
            }
        }, "clash-config-save").start()
    }
}
