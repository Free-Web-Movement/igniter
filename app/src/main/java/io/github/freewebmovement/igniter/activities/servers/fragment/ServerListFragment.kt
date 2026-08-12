package io.github.freewebmovement.igniter.activities.servers.fragment

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.freewebmovement.igniter.R
import io.github.freewebmovement.igniter.activities.MainActivity
import io.github.freewebmovement.igniter.activities.servers.activity.AddServerActivity
import io.github.freewebmovement.igniter.activities.servers.activity.ServerListActivity
import io.github.freewebmovement.igniter.activities.servers.contract.ServerListContract
import io.github.freewebmovement.igniter.common.app.BaseFragment
import io.github.freewebmovement.igniter.persistence.TrojanConfig

class ServerListFragment : BaseFragment(), ServerListContract.View {
    private var mPresenter: ServerListContract.Presenter? = null
    private lateinit var mServerListRv: RecyclerView
    private lateinit var mServerListAdapter: ServerListAdapter
    private var mImportConfigDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_server_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        findViews()
        initViews()
        initListeners()
        mPresenter!!.start()
    }

    private fun findViews() {
        mServerListRv = findViewById(R.id.serverListRv)
    }

    private fun initViews() {
        mServerListRv.layoutManager = LinearLayoutManager(mContext!!, LinearLayoutManager.VERTICAL, false)
        mServerListAdapter = ServerListAdapter(requireContext(), ArrayList())
        mServerListRv.adapter = mServerListAdapter
    }

    private fun initListeners() {
        findViewById<View>(R.id.fab).setOnClickListener {
            startActivityForResult(Intent(requireContext(), AddServerActivity::class.java), ADD_SERVER_REQUEST_CODE)
        }
        mServerListAdapter.setOnItemClickListener(object : ServerListAdapter.OnItemClickListener {
            override fun onItemSelected(config: TrojanConfig, pos: Int) {
                mPresenter!!.handleServerSelection(config)
            }

            override fun onItemDelete(config: TrojanConfig, pos: Int) {
                AlertDialog.Builder(mContext!!)
                    .setTitle(R.string.warning_delete_server)
                    .setMessage(R.string.warngin_delete_server_confirm)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.yes) { _, _ ->
                        Thread {
                            mPresenter!!.deleteServerConfig(config, pos)
                        }.start()
                    }
                    .setNegativeButton(android.R.string.no, null)
                    .show()
            }
        })
    }

    private fun getApplicationContext(): Context? {
        return activity?.applicationContext
    }

    override fun showAddTrojanConfigSuccess() {
        mRootView?.post { Toast.makeText(getApplicationContext()!!, R.string.add_server_success, Toast.LENGTH_SHORT).show() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (FILE_IMPORT_REQUEST_CODE == requestCode && resultCode == Activity.RESULT_OK && data != null) {
            val uri = data.data
            if (uri != null) {
                mPresenter!!.parseConfigsInFileStream(requireContext(), uri)
            }
        } else if (ADD_SERVER_REQUEST_CODE == requestCode && resultCode == Activity.RESULT_OK) {
            mPresenter!!.start()
        }
    }

    override fun selectServerConfig(config: TrojanConfig) {
        val activity = activity
        if (activity is MainActivity) {
            activity.onServerSelected(config)
            return
        }
        if (activity != null) {
            val intent = Intent()
            intent.putExtra(KEY_TROJAN_CONFIG, config)
            activity.setResult(Activity.RESULT_OK, intent)
            activity.finish()
        }
    }

    /** Reloads the server list; used when the tab is shown again. */
    fun refresh() {
        if (mPresenter != null) {
            mPresenter!!.start()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_server_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_import_from_file -> {
                mPresenter!!.displayImportFileDescription()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun showImportFileDescription() {
        mImportConfigDialog = AlertDialog.Builder(mContext!!)
            .setTitle(R.string.common_alert)
            .setMessage(R.string.server_list_import_file_desc)
            .setPositiveButton(R.string.common_confirm) { _, _ -> mPresenter!!.importConfigFromFile() }
            .setNegativeButton(R.string.common_cancel) { _, _ -> mPresenter!!.hideImportFileDescription() }
            .create()
        mImportConfigDialog!!.show()
    }

    override fun dismissImportFileDescription() {
        if (mImportConfigDialog != null && mImportConfigDialog!!.isShowing) {
            mImportConfigDialog!!.dismiss()
            mImportConfigDialog = null
        }
    }

    override fun openFileChooser() {
        val intent = Intent()
            .setType("text/plain")
            .setAction(Intent.ACTION_GET_CONTENT)
        startActivityForResult(
            Intent.createChooser(intent, getString(R.string.server_list_file_chooser_msg)),
            FILE_IMPORT_REQUEST_CODE
        )
    }

    override fun showServerConfigList(configs: List<TrojanConfig>) {
        mRootView?.post { mServerListAdapter.replaceData(configs) }
    }

    override fun removeServerConfig(config: TrojanConfig, pos: Int) {
        mRootView?.post { mServerListAdapter.removeItemOnPosition(pos) }
    }

    override fun setPresenter(presenter: ServerListContract.Presenter) {
        mPresenter = presenter
    }

    companion object {
        private const val FILE_IMPORT_REQUEST_CODE = 120
        private const val ADD_SERVER_REQUEST_CODE = 130
        const val TAG = "ServerListFragment"
        const val KEY_TROJAN_CONFIG = ServerListActivity.KEY_TROJAN_CONFIG

        @JvmStatic
        fun newInstance(): ServerListFragment {
            return ServerListFragment()
        }
    }
}
