package io.github.freewebmovement.igniter.activities.exempt.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Nullable
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import io.github.freewebmovement.igniter.IgniterApplication
import io.github.freewebmovement.igniter.R
import io.github.freewebmovement.igniter.activities.MainActivity
import io.github.freewebmovement.igniter.activities.exempt.adapter.AppInfoAdapter
import io.github.freewebmovement.igniter.activities.exempt.contract.ExemptAppContract
import io.github.freewebmovement.igniter.common.app.BaseFragment
import io.github.freewebmovement.igniter.common.dialog.LoadingDialog
import io.github.freewebmovement.igniter.persistence.data.AppInfo

class ExemptAppFragment : BaseFragment(), ExemptAppContract.View {
    private var mPresenter: ExemptAppContract.Presenter? = null
    private lateinit var mAppRv: RecyclerView
    private lateinit var mAppInfoAdapter: AppInfoAdapter
    private var mLoadingDialog: LoadingDialog? = null
    private lateinit var app: IgniterApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_exempt_app, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        app = IgniterApplication.getApplication()
        super.onViewCreated(view, savedInstanceState)
        findViews()
        initViews()
        initListeners()
        mPresenter!!.start()
    }

    private fun findViews() {
        mAppRv = findViewById(R.id.exemptAppRv)
    }

    private fun initViews() {
        mAppInfoAdapter = AppInfoAdapter()
        mAppRv.adapter = mAppInfoAdapter
        mAppRv.addItemDecoration(DividerItemDecoration(mContext!!, LinearLayoutManager.VERTICAL))
    }

    private fun initListeners() {
        mAppInfoAdapter.setOnItemOperationListener(object : AppInfoAdapter.OnItemOperationListener {
            override fun onToggle(enabled: Boolean, appInfo: AppInfo, position: Int) {
                mPresenter!!.updateAppInfo(appInfo, position, enabled)
            }
        })
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_exempt_app, menu)
        if (menu is MenuBuilder) {
            menu.setOptionalIconsVisible(true)
        }

        initHideSystemSwitch(menu)

        val item = menu.findItem(R.id.action_search_app)
        val searchView = item?.actionView as? SearchView
        if (searchView != null) {
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(s: String): Boolean {
                    return false
                }

                override fun onQueryTextChange(s: String): Boolean {
                    mPresenter!!.filterAppsByName(s)
                    return true
                }
            })
        }
    }

    fun initHideSystemSwitch(menu: Menu) {
        val item = menu.findItem(R.id.hide_system_apps)
        if (item != null) {
            val switchCompat = item.actionView as SwitchCompat
            switchCompat.isChecked = app.trojanPreferences.getShowSystemApps()
            switchCompat.setOnCheckedChangeListener { _, isChecked ->
                app.trojanPreferences.setShowSystemApps(isChecked)
                mPresenter!!.start()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.action_save_exempt_apps) {
            mPresenter!!.saveExemptAppInfoList()
            true
        } else {
            false
        }
    }

    override fun showSaveSuccess() {
        Snackbar.make(mRootView!!, R.string.common_save_success, Snackbar.LENGTH_SHORT)
            .setAction(R.string.exempt_app_exit) { mPresenter!!.exit() }
            .show()
    }

    override fun showExitConfirm() {
        AlertDialog.Builder(mContext!!)
            .setTitle(R.string.common_alert)
            .setMessage(R.string.exempt_app_exit_without_saving_confirm)
            .setNegativeButton(R.string.common_cancel) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(R.string.common_confirm) { dialog, _ ->
                dialog.dismiss()
                mPresenter!!.exit()
            }
            .create()
            .show()
    }

    override fun showAppList(appInfoList: List<AppInfo>) {
        mAppInfoAdapter.refreshData(appInfoList)
    }

    override fun showLoading() {
        if (mLoadingDialog == null) {
            mLoadingDialog = LoadingDialog(requireContext())
            mLoadingDialog!!.setMsg(getString(R.string.exempt_app_loading_tip))
        }
        mLoadingDialog!!.show()
    }

    override fun dismissLoading() {
        if (mLoadingDialog != null && mLoadingDialog!!.isShowing) {
            mLoadingDialog!!.dismiss()
        }
    }

    override fun exit(configurationChanged: Boolean) {
        val activity = activity
        if (activity is MainActivity) {
            activity.openHomeTab()
            return
        }
        if (activity != null) {
            activity.setResult(if (configurationChanged) Activity.RESULT_OK else Activity.RESULT_CANCELED)
            activity.finish()
        }
    }

    override fun setPresenter(presenter: ExemptAppContract.Presenter) {
        mPresenter = presenter
    }

    companion object {
        const val TAG = "ExemptAppFragment"

        @JvmStatic
        fun newInstance(): ExemptAppFragment {
            return ExemptAppFragment()
        }
    }
}
