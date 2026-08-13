package io.github.freewebmovement.igniter.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.os.RemoteException
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.github.freewebmovement.igniter.IgniterApplication
import io.github.freewebmovement.igniter.R
import io.github.freewebmovement.igniter.activities.exempt.fragment.ExemptAppFragment
import io.github.freewebmovement.igniter.activities.exempt.presenter.ExemptAppPresenter
import io.github.freewebmovement.igniter.common.dialog.AppSheet
import io.github.freewebmovement.igniter.common.os.Task
import io.github.freewebmovement.igniter.common.os.Threads
import io.github.freewebmovement.igniter.connection.TrojanConnection
import io.github.freewebmovement.igniter.persistence.TrojanConfig
import io.github.freewebmovement.igniter.persistence.database.AccessDatabase
import io.github.freewebmovement.igniter.proxy.aidl.ITrojanService
import io.github.freewebmovement.igniter.services.ProxyService
import java.io.IOException

/**
 * Tab shell hosting the four pages: servers, apps, rules and settings.
 */
class MainActivity : AppCompatActivity(), TrojanConnection.Callback {
    companion object {
        private const val TAG = "MainActivity"
        private const val READ_WRITE_EXT_STORAGE_PERMISSION_REQUEST = 514
        private const val CONNECTION_TEST_URL = "https://www.google.com"

        private const val TAG_HOME = "tab_home"
        private const val TAG_APPS = "tab_apps"
        private const val TAG_RULES = "tab_rules"
        private const val TAG_SETTINGS = "tab_settings"

        const val TAB_HOME = 0
        const val TAB_APPS = 1
        const val TAB_RULES = 2
        const val TAB_SETTINGS = 3
    }

    private val app: IgniterApplication
        get() = IgniterApplication.getApplication()
    private val homeViewModel: HomeViewModel by lazy {
        ViewModelProvider(this)[HomeViewModel::class.java]
    }

    // Launchers
    private val vpnLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            app.startProxyService()
        }
    }

    private var mHomeFragment: HomeFragment? = null
    private var mAppsFragment: ExemptAppFragment? = null
    private var mRulesFragment: RulesFragment? = null
    private var mSettingsFragment: SettingsFragment? = null
    private var mBottomNav: BottomNavigationView? = null
    private var mCurrentTab = TAB_HOME
    @ProxyService.ProxyState
    private var proxyState: Int = ProxyService.STATE_NONE
    private val connection = TrojanConnection(false)
    private var trojanService: ITrojanService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.let {
            it.setDisplayShowHomeEnabled(true)
            it.setIcon(R.mipmap.ic_launcher)
            it.title = getString(R.string.app_name)
        }

        mBottomNav = findViewById(R.id.bottomNav)
        mBottomNav!!.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.tab_apps -> switchTab(TAB_APPS)
                R.id.tab_rules -> switchTab(TAB_RULES)
                R.id.tab_settings -> switchTab(TAB_SETTINGS)
                else -> switchTab(TAB_HOME)
            }
            true
        }

        createFragments()

        connection.connect(this, this)
        if (!app.storage.isExternalWritable() &&
            ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        ) {
            requestReadWriteExternalStoragePermission()
        }
    }

    private fun createFragments() {
        val fm: FragmentManager = supportFragmentManager
        val home = fm.findFragmentByTag(TAG_HOME) as? HomeFragment ?: HomeFragment.newInstance()
        val apps = fm.findFragmentByTag(TAG_APPS) as? ExemptAppFragment
            ?: ExemptAppFragment.newInstance()
        val rules = fm.findFragmentByTag(TAG_RULES) as? RulesFragment
            ?: RulesFragment.newInstance()
        val settings = fm.findFragmentByTag(TAG_SETTINGS) as? SettingsFragment
            ?: SettingsFragment()
        mHomeFragment = home
        mAppsFragment = apps
        mRulesFragment = rules
        mSettingsFragment = settings

        ExemptAppPresenter(apps, app.exemptAppDataManager)

        val ft: FragmentTransaction = fm.beginTransaction()
        if (!home.isAdded) {
            ft.add(R.id.fragmentContainer, home, TAG_HOME)
        }
        if (!apps.isAdded) {
            ft.add(R.id.fragmentContainer, apps, TAG_APPS)
        }
        if (!rules.isAdded) {
            ft.add(R.id.fragmentContainer, rules, TAG_RULES)
        }
        if (!settings.isAdded) {
            ft.add(R.id.fragmentContainer, settings, TAG_SETTINGS)
        }
        ft.hide(apps)
        ft.hide(rules)
        ft.hide(settings)
        ft.show(home)
        ft.commitAllowingStateLoss()
    }

    private fun switchTab(tab: Int) {
        mCurrentTab = tab
        val fm = supportFragmentManager
        val ft = fm.beginTransaction()
        mHomeFragment?.let { ft.hide(it) }
        mAppsFragment?.let { ft.hide(it) }
        mRulesFragment?.let { ft.hide(it) }
        mSettingsFragment?.let { ft.hide(it) }
        val selected: Fragment
        val titleRes: Int
        when (tab) {
            TAB_APPS -> {
                selected = mAppsFragment!!
                titleRes = R.string.tab_apps
            }
            TAB_RULES -> {
                selected = mRulesFragment!!
                titleRes = R.string.tab_rules
            }
            TAB_SETTINGS -> {
                selected = mSettingsFragment!!
                titleRes = R.string.tab_settings
            }
            else -> {
                selected = mHomeFragment!!
                titleRes = R.string.app_name
            }
        }
        ft.show(selected)
        ft.commitAllowingStateLoss()
        supportActionBar?.title = getString(titleRes)
        if (selected === mHomeFragment) {
            homeViewModel.refreshServer()
            homeViewModel.refreshRoute()
        }
    }

    fun openHomeTab() {
        switchTab(TAB_HOME)
    }

    fun openRulesTab() {
        switchTab(TAB_RULES)
    }

    /**
     * Applies a server selected from the servers page: persists it and returns
     * to the connect page.
     */
    fun onServerSelected(config: TrojanConfig?) {
        if (config == null) {
            return
        }
        config.setCaCertPath(app.storage.path.caCert!!)
        app.trojanConfig.fromJSON(config.toJSON())
        Threads.instance().runOnWorkThread(object : Task() {
            override fun onRun() {
                TrojanConfig.write(app.trojanConfig, app.storage.path.trojanConfig!!)
                try {
                    app.clashConfig.save(app.storage.path.clashConfig!!)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                AccessDatabase.insertServerIfMissing(app.trojanConfig)
            }
        })
        homeViewModel.refreshServer()
        homeViewModel.refreshRoute()
        Toast.makeText(this, R.string.common_save_success, Toast.LENGTH_SHORT).show()
    }

    fun startVPN() {
        // start ProxyService
        val i = VpnService.prepare(applicationContext)
        if (i != null) {
            vpnLauncher.launch(i)
        } else {
            app.startProxyService()
        }
    }

    fun startProxy() {
        if (!app.trojanConfig.isValidRunningConfig()) {
            Toast.makeText(this, R.string.invalid_configuration, Toast.LENGTH_LONG).show()
            return
        }
        if (proxyState == ProxyService.STATE_NONE || proxyState == ProxyService.STOPPED) {
            TrojanConfig.write(app.trojanConfig, app.storage.path.trojanConfig!!)
            startVPN()
        }
    }

    fun stopProxy() {
        if (proxyState == ProxyService.STARTED) {
            app.stopProxyService()
        }
    }

    fun isProxyRunning(): Boolean {
        return proxyState == ProxyService.STARTED
    }

    fun getProxyState(): Int {
        return proxyState
    }

    private fun requestReadWriteExternalStoragePermission() {
        AppSheet.builder(this)
            .setTitle(R.string.common_alert)
            .setMessage(R.string.main_write_external_storage_permission_requirement)
            .setPositiveButton(R.string.common_confirm) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    READ_WRITE_EXT_STORAGE_PERMISSION_REQUEST
                )
            }
            .setNegativeButton(R.string.common_cancel, null)
            .show()
    }

    override fun onBackPressed() {
        if (AppSheet.isShowing()) {
            AppSheet.dismissActive()
            return
        }
        super.onBackPressed()
    }

    override fun onServiceConnected(service: ITrojanService) {
        Log.i(TAG, "onServiceConnected")
        trojanService = service
        Threads.instance().runOnWorkThread(object : Task() {
            override fun onRun() {
                try {
                    val state = service.getState()
                    runOnUiThread { updateViews(state) }
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
            }
        })
    }

    override fun onServiceDisconnected() {
        Log.i(TAG, "onServiceDisconnected")
        trojanService = null
    }

    private fun updateViews(state: Int) {
        proxyState = state
        homeViewModel.setProxyState(state)
    }

    override fun onStateChanged(state: Int, msg: String?) {
        Log.i(TAG, "onStateChanged# state: $state msg: $msg")
        updateViews(state)
    }

    override fun onTestResult(testUrl: String?, connected: Boolean, delay: Long, error: String) {
        runOnUiThread { showTestConnectionResult(testUrl, connected, delay, error) }
    }

    private fun showTestConnectionResult(testUrl: String?, connected: Boolean, delay: Long, error: String) {
        if (connected) {
            Toast.makeText(
                applicationContext,
                getString(R.string.connected_to__in__ms, testUrl ?: "", delay.toString()),
                Toast.LENGTH_LONG
            ).show()
        } else {
            Log.e(TAG, "TestError: $error")
            Toast.makeText(
                applicationContext,
                getString(R.string.failed_to_connect_to__, testUrl ?: "", error),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onBinderDied() {
        Log.i(TAG, "onBinderDied")
        connection.disconnect(this)
        connection.connect(this, this)
    }

    /**
     * Test connection by invoking [ITrojanService.testConnection].
     */
    fun testConnection() {
        val service = trojanService
        if (service == null) {
            showTestConnectionResult(CONNECTION_TEST_URL, false, 0L, "Trojan service is not available.")
        } else {
            try {
                service.testConnection(CONNECTION_TEST_URL)
            } catch (e: RemoteException) {
                showTestConnectionResult(CONNECTION_TEST_URL, false, 0L, "Trojan service throws RemoteException.")
                e.printStackTrace()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val isAutoStart = app.trojanPreferences.isEnableAutoStart()
        if (isAutoStart) {
            Log.v("PROXY_STATE", "ProxyState = $proxyState")
            when (proxyState) {
                ProxyService.STARTING, ProxyService.STARTED, ProxyService.STOPPING -> {
                    // already running, do nothing
                }
                else -> startProxy()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        connection.disconnect(this)
    }
}
