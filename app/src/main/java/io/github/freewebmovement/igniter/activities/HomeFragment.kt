package io.github.freewebmovement.igniter.activities

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import io.github.freewebmovement.igniter.IgniterApplication
import io.github.freewebmovement.igniter.R
import io.github.freewebmovement.igniter.services.ProxyService

/**
 * Connect page: current state, a big start/stop toggle, the active server card
 * and a connection test.
 */
class HomeFragment : Fragment() {

    private var mStatusTitle: TextView? = null
    private var mStatusSub: TextView? = null
    private var mServerValue: TextView? = null
    private var mConnectBtn: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mStatusTitle = view.findViewById(R.id.homeStatusTitle)
        mStatusSub = view.findViewById(R.id.homeStatusSub)
        mServerValue = view.findViewById(R.id.homeServerValue)
        mConnectBtn = view.findViewById(R.id.homeConnectBtn)

        mConnectBtn!!.setOnClickListener {
            val activity = activity as? MainActivity ?: return@setOnClickListener
            if (activity.isProxyRunning()) {
                activity.stopProxy()
            } else {
                activity.startProxy()
            }
        }
        view.findViewById<View>(R.id.homeServerRow).setOnClickListener {
            val activity = activity as? MainActivity
            if (activity != null) {
                activity.openServersTab()
            }
        }
        view.findViewById<View>(R.id.homeTestBtn).setOnClickListener {
            val activity = activity as? MainActivity
            if (activity != null) {
                activity.testConnection()
            }
        }

        refreshServerInfo()
        updateState((requireActivity() as MainActivity).getProxyState())
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_home, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        val activity = activity as? MainActivity
        if (activity == null) {
            return false
        }
        if (id == R.id.action_view_test_connection) {
            activity.testConnection()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        refreshServerInfo()
    }

    fun updateState(state: Int) {
        val statusTitle = mStatusTitle
        val connectBtn = mConnectBtn
        if (statusTitle == null || connectBtn == null) {
            return
        }
        when (state) {
            ProxyService.STARTED -> {
                statusTitle.setText(R.string.home_status_started)
                statusTitle.setTextColor(0xFF2E7D32.toInt())
                connectBtn.setText(R.string.home_btn_stop)
                connectBtn.isEnabled = true
            }
            ProxyService.STARTING, ProxyService.STOPPING -> {
                statusTitle.setText(if (state == ProxyService.STARTING)
                    R.string.home_status_starting else R.string.home_status_stopping)
                statusTitle.setTextColor(0xFFF57F17.toInt())
                connectBtn.isEnabled = false
            }
            else -> {
                statusTitle.setText(R.string.home_status_stopped)
                statusTitle.setTextColor(0xFF757575.toInt())
                connectBtn.setText(R.string.home_btn_start)
                connectBtn.isEnabled = true
            }
        }
    }

    fun refreshServerInfo() {
        val serverValue = mServerValue
        val statusSub = mStatusSub
        if (serverValue == null || statusSub == null) {
            return
        }
        val app = IgniterApplication.getApplication()
        val addr = app.trojanConfig.getRemoteAddr()
        if (addr.isEmpty()) {
            serverValue.setText(R.string.home_server_unknown)
            statusSub.setText(R.string.home_server_unknown)
        } else {
            val text = "$addr:${app.trojanConfig.getRemotePort()}"
            serverValue.text = text
            statusSub.text = text
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(): HomeFragment {
            return HomeFragment()
        }
    }
}
