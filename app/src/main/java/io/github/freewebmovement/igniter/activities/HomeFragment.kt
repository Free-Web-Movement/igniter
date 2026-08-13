package io.github.freewebmovement.igniter.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import io.github.freewebmovement.igniter.IgniterApplication
import io.github.freewebmovement.igniter.R
import io.github.freewebmovement.igniter.activities.servers.activity.AddServerActivity
import io.github.freewebmovement.igniter.activities.servers.viewmodel.ServerListViewModel
import io.github.freewebmovement.igniter.connection.ServerPingManager
import io.github.freewebmovement.igniter.persistence.TrojanConfig
import io.github.freewebmovement.igniter.persistence.database.Server
import io.github.freewebmovement.igniter.services.ProxyService
import io.github.freewebmovement.igniter.services.ServerPingService
import io.github.freewebmovement.igniter.theme.IgniterTheme
import io.github.freewebmovement.igniter.ui.home.HomeScreen

/**
 * Proxy servers page: current state, a big start/stop toggle, a connection test,
 * the proxy route summary and the server list with add/select/delete/import.
 * UI is Jetpack Compose via [HomeScreen].
 */
class HomeFragment : Fragment() {

    private val homeViewModel: HomeViewModel by activityViewModels()
    private val serverViewModel: ServerListViewModel by viewModels()

    private var mProxyState by mutableStateOf(ProxyService.STATE_NONE)
    private var mServerSummary by mutableStateOf("")
    private var mRouteSummary by mutableStateOf("")
    private var mDomainRouteCount by mutableStateOf(0)
    private var mServers by mutableStateOf<List<Server>>(emptyList())
    private var mPingData by mutableStateOf<Map<String, ServerPingManager.PingInfo>>(emptyMap())
    private var mCurrentHost by mutableStateOf("")
    private var mCurrentPort by mutableStateOf(0)

    private val addServerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(requireContext(), R.string.add_server_success, Toast.LENGTH_SHORT).show()
        }
    }

    private val editServerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(requireContext(), R.string.common_save_success, Toast.LENGTH_SHORT).show()
        }
    }

    private val fileImportLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            serverViewModel.importConfigsFromFile(uri)
            Toast.makeText(requireContext(), R.string.add_server_success, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                IgniterTheme {
                    HomeScreen(
                        proxyState = mProxyState,
                        serverSummary = mServerSummary,
                        routeSummary = mRouteSummary,
                        domainRouteCount = mDomainRouteCount,
                        servers = mServers,
                        pingData = mPingData,
                        currentHost = mCurrentHost,
                        currentPort = mCurrentPort,
                        onConnectClick = {
                            val activity = activity as? MainActivity ?: return@HomeScreen
                            if (activity.isProxyRunning()) {
                                activity.stopProxy()
                            } else {
                                activity.startProxy()
                            }
                        },
                        onTestClick = {
                            (activity as? MainActivity)?.testConnection()
                        },
                        onDomainRouteClick = {
                            (activity as? MainActivity)?.openRulesTab()
                        },
                        onAddServerClick = {
                            addServerLauncher.launch(
                                Intent(requireContext(), AddServerActivity::class.java))
                        },
                        onServerSelected = { server ->
                            (activity as? MainActivity)?.onServerSelected(toTrojanConfig(server))
                        },
                        onServerPlay = { server ->
                            val activity = activity as? MainActivity ?: return@HomeScreen
                            activity.onServerSelected(toTrojanConfig(server))
                            activity.startProxy()
                        },
                        onServerStop = {
                            (activity as? MainActivity)?.stopProxy()
                        },
                        onServerEdit = { server ->
                            val intent = Intent(requireContext(), AddServerActivity::class.java)
                            intent.putExtra(AddServerActivity.EXTRA_SERVER_ID, server.id)
                            editServerLauncher.launch(intent)
                        },
                        onServerDelete = { server ->
                            serverViewModel.deleteServer(server)
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        homeViewModel.proxyState.observe(viewLifecycleOwner) {
            mProxyState = it
            refreshCurrentServer()
        }
        homeViewModel.serverSummary.observe(viewLifecycleOwner) { mServerSummary = it }
        homeViewModel.routeSummary.observe(viewLifecycleOwner) { mRouteSummary = it }
        homeViewModel.domainRouteCount.observe(viewLifecycleOwner) { mDomainRouteCount = it }
        homeViewModel.refreshServer()
        homeViewModel.refreshRoute()

        serverViewModel.servers.observe(viewLifecycleOwner) { servers ->
            mServers = servers
            refreshCurrentServer()
        }

        ServerPingManager.pingData.observe(viewLifecycleOwner) { mPingData = it }

        refreshCurrentServer()
    }

    private fun refreshCurrentServer() {
        val config = IgniterApplication.getApplication().trojanConfig
        mCurrentHost = config.getRemoteAddr()
        mCurrentPort = config.getRemotePort()
    }

    private fun toTrojanConfig(server: Server): TrojanConfig {
        val config = TrojanConfig()
        config.setRemoteAddr(server.hostname)
        config.setRemotePort(server.port)
        config.setPassword(server.password)
        config.setLocalPort(server.local_port)
        return config
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
        if (id == R.id.action_import_from_file) {
            fileImportLauncher.launch("text/plain")
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        homeViewModel.refreshServer()
        homeViewModel.refreshRoute()
        ServerPingService.start(requireContext())
    }

    override fun onPause() {
        super.onPause()
        ServerPingService.stop(requireContext())
    }

    companion object {
        @JvmStatic
        fun newInstance(): HomeFragment {
            return HomeFragment()
        }
    }
}
