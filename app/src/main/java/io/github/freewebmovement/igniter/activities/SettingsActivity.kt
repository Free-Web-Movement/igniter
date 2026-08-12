package io.github.freewebmovement.igniter.activities

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import io.github.freewebmovement.igniter.IgniterApplication
import io.github.freewebmovement.igniter.R
import io.github.freewebmovement.igniter.persistence.NetWorkConfig
import io.github.freewebmovement.igniter.persistence.TrojanPreferences

class SettingsActivity : AppCompatActivity() {
    private lateinit var ipv6Switch: SwitchCompat
    private lateinit var clashSwitch: SwitchCompat
    private lateinit var enableLanSwitch: SwitchCompat
    private lateinit var enableAutoStartSwitch: SwitchCompat
    private lateinit var enableBootStartSwitch: SwitchCompat

    lateinit var app: IgniterApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        app = IgniterApplication.getApplication()
        init()
        initListener()
        initFromPreferences(app.trojanPreferences)
    }

    private fun init() {
        ipv6Switch = findViewById(R.id.ipv6Switch)
        clashSwitch = findViewById(R.id.clashSwitch)
        enableLanSwitch = findViewById(R.id.switch_enable_lan)
        enableAutoStartSwitch = findViewById(R.id.switch_enable_auto_start)
        enableBootStartSwitch = findViewById(R.id.switch_enable_boot_start)
        val clashLink = findViewById<TextView>(R.id.clashLink)
        clashLink.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun initFromPreferences(preferences: TrojanPreferences) {
        ipv6Switch.isChecked = preferences.getEnableIPV6()
        clashSwitch.isChecked = preferences.getEnableClash()
        enableLanSwitch.isChecked = preferences.isEnableLan()
        enableAutoStartSwitch.isChecked = preferences.isEnableAutoStart()
        enableBootStartSwitch.isChecked = preferences.isEnableBootStart()
    }

    private fun initListener() {
        ipv6Switch.setOnCheckedChangeListener { _, isChecked ->
            app.trojanPreferences.setEnableIPV6(isChecked)
        }
        clashSwitch.setOnCheckedChangeListener { _, isChecked ->
            app.trojanPreferences.setEnableClash(isChecked)
            val port: Int = if (app.trojanPreferences.getEnableClash()) {
                app.clashConfig.getPort()
            } else {
                app.trojanConfig.getLocalPort()
            }
            NetWorkConfig.setPort(app, port)
            Log.wtf("MAIN", "" + app.clashConfig.getPort())
            Log.wtf("MAIN", "" + app.trojanConfig.getLocalPort())
        }

        enableLanSwitch.setOnCheckedChangeListener { _, isChecked ->
            app.trojanPreferences.setEnableLan(isChecked)
        }

        enableAutoStartSwitch.setOnCheckedChangeListener { _, isChecked ->
            Log.v(TAG, " auto start : $isChecked")
            app.trojanPreferences.setEnableAutoStart(isChecked)
        }

        enableBootStartSwitch.setOnCheckedChangeListener { _, isChecked ->
            Log.v(TAG, " auto start : $isChecked")
            app.trojanPreferences.setEnableBootStart(isChecked)
        }
    }

    companion object {
        private const val TAG = "SettingsActivity"
    }
}
