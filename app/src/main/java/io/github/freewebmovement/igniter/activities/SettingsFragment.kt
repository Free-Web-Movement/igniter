package io.github.freewebmovement.igniter.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import io.github.freewebmovement.igniter.IgniterApplication
import io.github.freewebmovement.igniter.persistence.NetWorkConfig
import io.github.freewebmovement.igniter.theme.IgniterTheme
import io.github.freewebmovement.igniter.ui.settings.SettingsScreen

/**
 * Settings page: IPv6, Clash, LAN, auto start and boot start.
 */
class SettingsFragment : Fragment() {

    private val app: IgniterApplication
        get() = IgniterApplication.getApplication()

    private var mIpv6 by mutableStateOf(false)
    private var mClash by mutableStateOf(true)
    private var mLan by mutableStateOf(false)
    private var mAutoStart by mutableStateOf(false)
    private var mBootStart by mutableStateOf(false)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val preferences = app.trojanPreferences
        mIpv6 = preferences.getEnableIPV6()
        mClash = preferences.getEnableClash()
        mLan = preferences.isEnableLan()
        mAutoStart = preferences.isEnableAutoStart()
        mBootStart = preferences.isEnableBootStart()
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                IgniterTheme {
                    SettingsScreen(
                        ipv6Enabled = mIpv6,
                        clashEnabled = mClash,
                        lanEnabled = mLan,
                        autoStartEnabled = mAutoStart,
                        bootStartEnabled = mBootStart,
                        onIpv6Change = {
                            mIpv6 = it
                            app.trojanPreferences.setEnableIPV6(it)
                        },
                        onClashChange = {
                            mClash = it
                            app.trojanPreferences.setEnableClash(it)
                            val port: Int = if (app.trojanPreferences.getEnableClash()) {
                                app.clashConfig.getPort()
                            } else {
                                app.trojanConfig.getLocalPort()
                            }
                            NetWorkConfig.setPort(app, port)
                        },
                        onLanChange = {
                            mLan = it
                            app.trojanPreferences.setEnableLan(it)
                        },
                        onAutoStartChange = {
                            mAutoStart = it
                            app.trojanPreferences.setEnableAutoStart(it)
                        },
                        onBootStartChange = {
                            mBootStart = it
                            app.trojanPreferences.setEnableBootStart(it)
                        }
                    )
                }
            }
        }
    }
}
