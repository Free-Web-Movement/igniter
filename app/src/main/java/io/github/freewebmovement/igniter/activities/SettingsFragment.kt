package io.github.freewebmovement.igniter.activities

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import io.github.freewebmovement.igniter.IgniterApplication
import io.github.freewebmovement.igniter.R
import io.github.freewebmovement.igniter.common.dialog.AppSheet
import io.github.freewebmovement.igniter.constants.Clash as ClashConstants
import io.github.freewebmovement.igniter.databinding.FragmentSettingsBinding
import io.github.freewebmovement.igniter.persistence.NetWorkConfig
import io.github.freewebmovement.igniter.persistence.TrojanPreferences

/**
 * Settings page: IPv6, Clash toggle, Clash mode, LAN, auto start and boot start.
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val app: IgniterApplication
        get() = IgniterApplication.getApplication()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.clashLink.movementMethod = LinkMovementMethod.getInstance()
        initFromPreferences(app.trojanPreferences)
        initListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initFromPreferences(preferences: TrojanPreferences) {
        binding.ipv6Switch.isChecked = preferences.getEnableIPV6()
        binding.clashSwitch.isChecked = preferences.getEnableClash()
        binding.switchEnableLan.isChecked = preferences.isEnableLan()
        binding.switchEnableAutoStart.isChecked = preferences.isEnableAutoStart()
        binding.switchEnableBootStart.isChecked = preferences.isEnableBootStart()
        refreshClashMode()
    }

    private fun refreshClashMode() {
        val mode = app.clashConfig.getMode()
        binding.clashModeValue.text = when (mode) {
            ClashConstants.MODE_GLOBAL ->
                getString(R.string.clash_mode_global)
            ClashConstants.MODE_DIRECT ->
                getString(R.string.clash_mode_direct)
            else -> getString(R.string.clash_mode_rule)
        }
    }

    private fun initListeners() {
        binding.ipv6Switch.setOnCheckedChangeListener { _, isChecked ->
            app.trojanPreferences.setEnableIPV6(isChecked)
        }
        binding.clashSwitch.setOnCheckedChangeListener { _, isChecked ->
            app.trojanPreferences.setEnableClash(isChecked)
            val port: Int = if (app.trojanPreferences.getEnableClash()) {
                app.clashConfig.getPort()
            } else {
                app.trojanConfig.getLocalPort()
            }
            NetWorkConfig.setPort(app, port)
        }
        binding.switchEnableLan.setOnCheckedChangeListener { _, isChecked ->
            app.trojanPreferences.setEnableLan(isChecked)
        }
        binding.switchEnableAutoStart.setOnCheckedChangeListener { _, isChecked ->
            app.trojanPreferences.setEnableAutoStart(isChecked)
        }
        binding.switchEnableBootStart.setOnCheckedChangeListener { _, isChecked ->
            app.trojanPreferences.setEnableBootStart(isChecked)
        }
        binding.clashModeRow.setOnClickListener { showClashModeDialog() }
    }

    private fun showClashModeDialog() {
        val modes = ClashConstants.MODES
        val current = app.clashConfig.getMode()
        val selected = modes.indexOf(current).takeIf { it >= 0 } ?: 0
        val radioGroup = RadioGroup(requireContext())
        modes.forEachIndexed { index, mode ->
            val radio = RadioButton(requireContext())
            radio.text = mode
            radio.isChecked = index == selected
            radio.setOnClickListener {
                app.clashConfig.setMode(mode)
                refreshClashMode()
                AppSheet.dismissActive()
            }
            radioGroup.addView(radio)
        }
        AppSheet.builder(this)
            .setTitle(R.string.clash_mode)
            .setContent(radioGroup)
            .setNegativeButton(R.string.common_cancel, null)
            .show()
    }
}
