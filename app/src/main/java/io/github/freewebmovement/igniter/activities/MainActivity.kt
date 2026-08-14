package io.github.freewebmovement.igniter.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.RemoteException
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.ViewModelProvider
import io.github.freewebmovement.igniter.IgniterApplication
import io.github.freewebmovement.igniter.R
import io.github.freewebmovement.igniter.activities.exempt.fragment.ExemptAppFragment
import io.github.freewebmovement.igniter.activities.exempt.presenter.ExemptAppPresenter
import io.github.freewebmovement.igniter.common.dialog.AppSheet
import io.github.freewebmovement.igniter.common.dialog.AppSheetHost
import io.github.freewebmovement.igniter.common.os.Task
import io.github.freewebmovement.igniter.common.os.Threads
import io.github.freewebmovement.igniter.common.util.BatteryOptimization
import io.github.freewebmovement.igniter.connection.TrojanConnection
import io.github.freewebmovement.igniter.persistence.TrojanConfig
import io.github.freewebmovement.igniter.persistence.database.AccessDatabase
import io.github.freewebmovement.igniter.proxy.aidl.ITrojanService
import io.github.freewebmovement.igniter.services.ProxyService
import io.github.freewebmovement.igniter.theme.IgniterTheme
import io.github.freewebmovement.igniter.ui.MainShell
import java.io.IOException

/**
 * Tab shell hosting the four pages: servers, apps, rules and settings.
 */
class MainActivity : AppCompatActivity(), AppSheetHost, TrojanConnection.Callback {
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

        /**
         * Intent extra set by [ProxyService.restart] before the process is torn
         * down, so the relaunched activity automatically reconnects the proxy.
         */
        const val EXTRA_AUTO_START_PROXY = "auto_start_proxy"

        /**
         * Intent extra set by the new-URL prompt notification to open the
         * 分流规则 page when tapped.
         */
        const val EXTRA_OPEN_RULES_TAB = "open_rules_tab"
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
        if (result.resultCode == Activity.RESULT_OK) {
            app.startProxyService()
        }
    }

    private var mHomeFragment: HomeFragment? = null
    private var mAppsFragment: ExemptAppFragment? = null
    private var mRulesFragment: RulesFragment? = null
    private var mSettingsFragment: SettingsFragment? = null
    private var mCurrentTab by mutableStateOf(TAB_HOME)
    private var fragmentsCreated = false
    @ProxyService.ProxyState
    private var proxyState: Int = ProxyService.STATE_NONE
    private val connection = TrojanConnection(false)
    private var trojanService: ITrojanService? = null

    // AppSheet overlay state
    private var sheetContent by mutableStateOf<(@Composable () -> Unit)?>(null)
    private var sheetDismissOnOutsideTap by mutableStateOf(false)
    private var sheetSeq = 0
    private var activeSheetSeq = 0

    override fun presentSheet(
        content: @Composable () -> Unit,
        dismissOnOutsideTap: Boolean
    ): () -> Unit {
        val seq = ++sheetSeq
        activeSheetSeq = seq
        sheetContent = content
        sheetDismissOnOutsideTap = dismissOnOutsideTap
        return {
            if (activeSheetSeq == seq) {
                sheetContent = null
                sheetDismissOnOutsideTap = false
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IgniterTheme {
                MainShell(
                    currentTab = mCurrentTab,
                    onTabSelected = { switchTab(it) },
                    sheetContent = sheetContent,
                    sheetDismissOnOutsideTap = sheetDismissOnOutsideTap,
                    onDismissSheet = { AppSheet.dismissActive() },
                    fragmentContent = {
                        AndroidView(
                            factory = { ctx ->
                                FragmentContainerView(ctx).apply {
                                    id = R.id.fragmentContainer
                                }
                            },
                            update = { container ->
                                if (!fragmentsCreated) {
                                    fragmentsCreated = true
                                    createFragments()
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                )
            }
        }

        if (!app.storage.isExternalWritable() &&
            ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        ) {
            requestReadWriteExternalStoragePermission()
        }

        if (intent?.getBooleanExtra(EXTRA_AUTO_START_PROXY, false) == true) {
            startProxy()
        }
        if (intent?.getBooleanExtra(EXTRA_OPEN_RULES_TAB, false) == true) {
            openRulesTab()
        }

        requestNotificationPermission()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2001)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_AUTO_START_PROXY, false)) {
            startProxy()
        }
        if (intent.getBooleanExtra(EXTRA_OPEN_RULES_TAB, false)) {
            openRulesTab()
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
        when (tab) {
            TAB_APPS -> {
                selected = mAppsFragment!!
            }
            TAB_RULES -> {
                selected = mRulesFragment!!
            }
            TAB_SETTINGS -> {
                selected = mSettingsFragment!!
            }
            else -> {
                selected = mHomeFragment!!
            }
        }
        ft.show(selected)
        ft.commitAllowingStateLoss()
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
                // Note: do NOT re-dump the Clash config here. SnakeYAML would
                // rewrite the hand-written block-style `rules:` section into a
                // flow-style list, which ClashRouteBuilder handles but which also
                // strips every comment in the file.
                AccessDatabase.insertServerIfMissing(app.trojanConfig)
            }
        })
        homeViewModel.refreshServer()
        homeViewModel.refreshRoute()
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
            requestBatteryWhitelistIfNeeded()
            // Re-bind so the newly started service is observed. The connection
            // is dropped when the tunnel stops, so it must be re-established
            // here on every start.
            if (!connection.isConnected()) {
                connection.connect(this, this)
            }
        }
    }

    /**
     * Prompts the user to add Igniter to the battery optimization whitelist so
     * aggressive battery managers don't kill the :proxy process while the
     * tunnel is running in the background. The system dialog only appears when
     * the whitelist has not been granted yet.
     */
    private fun requestBatteryWhitelistIfNeeded() {
        if (!BatteryOptimization.isIgnoringBatteryOptimizations(this)) {
            BatteryOptimization.requestIgnoreOptimizations(this)
        }
    }

    fun stopProxy() {
        if (proxyState == ProxyService.STARTING || proxyState == ProxyService.STARTED) {
            app.stopProxyService()
        }
    }

    /**
     * Restarts a running proxy so saved exempt-app changes take effect on the
     * VPN interface. The native go stack cannot restart in-process, so the
     * whole app process is relaunched; the relaunch auto-connects again.
     */
    fun restartProxy() {
        if (proxyState != ProxyService.STARTING && proxyState != ProxyService.STARTED) {
            return
        }
        Toast.makeText(this, R.string.main_proxy_restarting_tip, Toast.LENGTH_LONG).show()
        // Let the toast show before the process is killed.
        Handler(Looper.getMainLooper()).postDelayed({
            app.restartProxyService()
        }, 1500)
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
        if (state == ProxyService.STOPPED) {
            // Drop the binding when the tunnel stops: the :proxy process is
            // about to be recycled, and keeping the connection would make the
            // system restart it as an idle zombie merely to serve the binding.
            if (connection.isConnected()) {
                connection.disconnect(this)
            }
        }
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
        // Keep the binding alive only while the tunnel should be running.
        // Binding to a stopped service would make the system resurrect the
        // killed :proxy process as an idle zombie merely to serve the binding.
        app.trojanPreferences.reload()
        val vpnActive = app.trojanPreferences.isVpnActive()
        if (vpnActive) {
            if (!connection.isConnected()) {
                connection.connect(this, this)
            }
        } else {
            if (connection.isConnected()) {
                connection.disconnect(this)
            }
            if (proxyState == ProxyService.STARTED || proxyState == ProxyService.STARTING) {
                updateViews(ProxyService.STOPPED)
            }
        }
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
