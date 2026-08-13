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
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.freewebmovement.igniter.R
import io.github.freewebmovement.igniter.activities.servers.activity.AddServerActivity
import io.github.freewebmovement.igniter.activities.servers.fragment.ServerListAdapter
import io.github.freewebmovement.igniter.activities.servers.viewmodel.ServerListViewModel
import io.github.freewebmovement.igniter.common.dialog.AppSheet
import io.github.freewebmovement.igniter.common.util.QrCodeUtils
import io.github.freewebmovement.igniter.connection.ServerPingManager
import io.github.freewebmovement.igniter.databinding.FragmentHomeBinding
import io.github.freewebmovement.igniter.persistence.TrojanConfig
import io.github.freewebmovement.igniter.persistence.database.Server
import io.github.freewebmovement.igniter.services.ProxyService
import io.github.freewebmovement.igniter.services.ServerPingService

/**
 * Proxy servers page: current state, a big start/stop toggle, a connection test,
 * the proxy route summary and the server list with add/select/delete/import.
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val homeViewModel: HomeViewModel by activityViewModels()
    private val serverViewModel: ServerListViewModel by viewModels()
    private var mServers: List<Server> = emptyList()

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
                              savedInstanceState: Bundle?): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.homeConnectBtn.setOnClickListener {
            val activity = activity as? MainActivity ?: return@setOnClickListener
            if (activity.isProxyRunning()) {
                activity.stopProxy()
            } else {
                activity.startProxy()
            }
        }
        binding.homeTestBtn.setOnClickListener {
            val activity = activity as? MainActivity
            if (activity != null) {
                activity.testConnection()
            }
        }
        binding.homeDomainRouteRow.setOnClickListener {
            val activity = activity as? MainActivity
            if (activity != null) {
                activity.openRulesTab()
            }
        }
        binding.homeAddServerBtn.setOnClickListener {
            addServerLauncher.launch(Intent(requireContext(), AddServerActivity::class.java))
        }

        initServerList()

        homeViewModel.proxyState.observe(viewLifecycleOwner) {
            renderState(it)
            (binding.homeServerListRv.adapter as? ServerListAdapter)?.setRunning(
                it == ProxyService.STARTED)
        }
        homeViewModel.serverSummary.observe(viewLifecycleOwner) { renderServer(it) }
        homeViewModel.routeSummary.observe(viewLifecycleOwner) {
            binding.homeRouteSummary.text = it
        }
        homeViewModel.domainRouteCount.observe(viewLifecycleOwner) {
            binding.homeDomainRouteCount.text =
                getString(R.string.home_domain_route_count, it)
        }
        homeViewModel.refreshServer()
        homeViewModel.refreshRoute()

        ServerPingManager.pingData.observe(viewLifecycleOwner) { data ->
            renderPingData(data)
        }
    }

    private fun renderPingData(data: Map<String, ServerPingManager.PingInfo>) {
        val adapter = binding.homeServerListRv.adapter as? ServerListAdapter ?: return
        for (server in mServers) {
            val info = data["${server.hostname}:${server.port}"] ?: continue
            adapter.updatePing(
                server.hostname, server.port,
                formatPingText(info), formatPingColor(info)
            )
        }
    }

    private fun formatPingText(info: ServerPingManager.PingInfo): String {
        val current = info.currentMs
        val avg = info.avgMs
        if (current == null || avg == null) {
            return getString(R.string.server_ping_unreachable)
        }
        return getString(R.string.server_ping_format, current, avg)
    }

    private fun formatPingColor(info: ServerPingManager.PingInfo): Int {
        return when (info.connectivity) {
            ServerPingManager.Connectivity.EXCELLENT -> 0xFF2E7D32.toInt()
            ServerPingManager.Connectivity.GOOD -> 0xFF00897B.toInt()
            ServerPingManager.Connectivity.FAIR -> 0xFFF57C00.toInt()
            ServerPingManager.Connectivity.POOR -> 0xFFC62828.toInt()
            ServerPingManager.Connectivity.UNREACHABLE -> 0xFF757575.toInt()
        }
    }

    private fun initServerList() {
        binding.homeServerListRv.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        val adapter = ServerListAdapter(ArrayList())
        adapter.setOnItemClickListener(object : ServerListAdapter.OnItemClickListener {
            override fun onItemSelected(server: Server, pos: Int) {
                val activity = activity as? MainActivity ?: return
                activity.onServerSelected(toTrojanConfig(server))
            }

            override fun onItemPlay(server: Server, pos: Int) {
                val activity = activity as? MainActivity ?: return
                activity.onServerSelected(toTrojanConfig(server))
                activity.startProxy()
            }

            override fun onItemStop(server: Server, pos: Int) {
                val activity = activity as? MainActivity ?: return
                activity.stopProxy()
            }

            override fun onItemMore(server: Server, anchor: View, pos: Int) {
                showServerMoreMenu(server, anchor)
            }

            override fun onItemDelete(server: Server, pos: Int) {
                confirmDeleteServer(server)
            }
        })
        binding.homeServerListRv.adapter = adapter
        serverViewModel.servers.observe(viewLifecycleOwner) { servers ->
            mServers = servers
            adapter.replaceData(servers)
            renderServer(homeViewModel.serverSummary.value.orEmpty())
        }
    }

    private fun showServerMoreMenu(server: Server, anchor: View) {
        AppSheet.builder(this).showMenu(listOf(
            getString(R.string.server_list_qr) to {
                showServerQr(server)
            },
            getString(R.string.server_list_edit) to {
                val intent = Intent(requireContext(), AddServerActivity::class.java)
                intent.putExtra(AddServerActivity.EXTRA_SERVER_ID, server.id)
                editServerLauncher.launch(intent)
            },
            getString(R.string.server_list_delete_btn) to {
                confirmDeleteServer(server)
            }
        ))
    }

    private fun showServerQr(server: Server) {
        val uri = "trojan://${server.password}@${server.hostname}:${server.port}"
        val bitmap = QrCodeUtils.generateQrBitmap(uri)
        if (bitmap == null) {
            Toast.makeText(requireContext(), R.string.qr_generate_failed, Toast.LENGTH_LONG).show()
            return
        }
        val imageView = ImageView(requireContext())
        val pad = (24 * resources.displayMetrics.density).toInt()
        imageView.setPadding(pad, pad, pad, pad)
        imageView.setImageBitmap(bitmap)
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.qr_dialog_title)
            .setView(imageView)
            .setPositiveButton(R.string.qr_share_action) { _, _ ->
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, uri)
                }
                startActivity(Intent.createChooser(send, getString(R.string.qr_share_uri)))
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun confirmDeleteServer(server: Server) {
        AppSheet.builder(this)
            .setTitle(R.string.warning_delete_server)
            .setMessage(R.string.warngin_delete_server_confirm)
            .setPositiveButton(android.R.string.yes) {
                serverViewModel.deleteServer(server)
            }
            .setNegativeButton(android.R.string.no, null)
            .show()
    }

    private fun toTrojanConfig(server: Server): TrojanConfig {
        val config = TrojanConfig()
        config.setRemoteAddr(server.hostname)
        config.setRemotePort(server.port)
        config.setPassword(server.password)
        config.setLocalPort(server.local_port)
        return config
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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

    private fun renderState(state: Int) {
        when (state) {
            ProxyService.STARTED -> {
                binding.homeStatusTitle.setText(R.string.home_status_started)
                binding.homeStatusTitle.setTextColor(0xFF2E7D32.toInt())
                binding.homeConnectBtn.setText(R.string.home_btn_stop)
                binding.homeConnectBtn.isEnabled = true
            }
            ProxyService.STARTING, ProxyService.STOPPING -> {
                binding.homeStatusTitle.setText(if (state == ProxyService.STARTING)
                    R.string.home_status_starting else R.string.home_status_stopping)
                binding.homeStatusTitle.setTextColor(0xFFF57F17.toInt())
                binding.homeConnectBtn.isEnabled = false
            }
            else -> {
                binding.homeStatusTitle.setText(R.string.home_status_stopped)
                binding.homeStatusTitle.setTextColor(0xFF757575.toInt())
                binding.homeConnectBtn.setText(R.string.home_btn_start)
                binding.homeConnectBtn.isEnabled = true
            }
        }
    }

    private fun renderServer(summary: String) {
        if (mServers.isEmpty()) {
            binding.homeStatusSub.setText(R.string.home_no_server_info)
        } else if (summary.isEmpty()) {
            binding.homeStatusSub.setText(R.string.home_server_unknown)
        } else {
            binding.homeStatusSub.text = summary
        }
        (binding.homeServerListRv.adapter as? ServerListAdapter)?.notifyDataSetChanged()
    }

    companion object {
        @JvmStatic
        fun newInstance(): HomeFragment {
            return HomeFragment()
        }
    }
}
