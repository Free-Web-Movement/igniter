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

/**
 * Plain-text editor for the app's hand-editable YAML files. The target file is
 * selected with [EXTRA_TARGET]; it defaults to the Clash config so existing
 * callers keep working unchanged.
 */
class ClashFileEditorActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TARGET = "target"
        const val TARGET_CLASH = "clash"
        const val TARGET_DOMAIN_RULES = "domain_rules"
    }

    private val app: IgniterApplication
        get() = IgniterApplication.getApplication()

    private var text by mutableStateOf("")

    private val target: String
        get() = intent.getStringExtra(EXTRA_TARGET) ?: TARGET_CLASH

    private val isDomainRules: Boolean
        get() = target == TARGET_DOMAIN_RULES

    private val filePath: String
        get() = if (isDomainRules) app.storage.path.domainRules!!
        else app.storage.path.clashConfig!!

    private val rawRes: Int
        get() = if (isDomainRules) R.raw.domain_rules else R.raw.clash_config

    private val titleRes: Int
        get() = if (isDomainRules) R.string.domain_rules_editor_title
        else R.string.main_menu_action_clash_editor_file_editor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IgniterTheme {
                ClashEditorScreen(
                    title = getString(titleRes),
                    text = text,
                    onTextChange = { text = it },
                    onLoad = { load() },
                    onReset = { reset() },
                    onSave = { save() }
                )
            }
        }
        load()
    }

    private fun load() {
        val path = filePath
        Thread({
            val content = Storage.read(path)
            runOnUiThread {
                text = content?.let { String(it, StandardCharsets.UTF_8) } ?: ""
            }
        }, "config-load").start()
    }

    private fun reset() {
        val res = rawRes
        Thread({
            val defaultText = app.storage.readRawText(res)
            runOnUiThread { text = defaultText }
        }, "config-reset").start()
    }

    private fun save() {
        val content = text
        val path = filePath
        val reloadClash = !isDomainRules
        val messageRes = if (isDomainRules) R.string.domain_rules_saved_hint
        else R.string.main_save_success
        Thread({
            Storage.write(path, content.toByteArray(StandardCharsets.UTF_8))
            if (reloadClash) {
                // Reload so the in-memory config used at connect time matches the file.
                try {
                    app.clashConfig.reload()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            runOnUiThread {
                Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show()
            }
        }, "config-save").start()
    }
}
