package io.github.freewebmovement.igniter.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.freewebmovement.igniter.IgniterApplication
import io.github.freewebmovement.igniter.R
import io.github.freewebmovement.igniter.persistence.Storage
import io.github.freewebmovement.igniter.theme.IgniterTheme
import io.github.freewebmovement.igniter.ui.clash.ClashEditorScreen
import java.nio.charset.StandardCharsets

class ClashFileEditorActivity : AppCompatActivity() {
    private val app: IgniterApplication
        get() = IgniterApplication.getApplication()

    private var text by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IgniterTheme {
                ClashEditorScreen(
                    text = text,
                    onTextChange = { text = it },
                    onLoad = { loadClashConfig() },
                    onReset = { resetClashConfig() },
                    onSave = { saveClashConfig() }
                )
            }
        }
        loadClashConfig()
    }

    private fun loadClashConfig() {
        Thread({
            val content = Storage.read(app.storage.path.clashConfig!!)
            runOnUiThread {
                text = content?.let { String(it, StandardCharsets.UTF_8) } ?: ""
            }
        }, "clash-config-load").start()
    }

    private fun resetClashConfig() {
        Thread({
            val clashConfigText = app.storage.readRawText(R.raw.clash_config)
            runOnUiThread { text = clashConfigText }
        }, "clash-config-reset").start()
    }

    private fun saveClashConfig() {
        val content = text
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
