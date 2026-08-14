package io.github.freewebmovement.igniter.activities.exempt.presenter

import android.annotation.SuppressLint
import android.text.TextUtils
import io.github.freewebmovement.igniter.activities.exempt.contract.AppTab
import io.github.freewebmovement.igniter.activities.exempt.contract.ExemptAppContract
import io.github.freewebmovement.igniter.common.os.Task
import io.github.freewebmovement.igniter.common.os.Threads
import io.github.freewebmovement.igniter.constants.DefaultInternationalApps
import io.github.freewebmovement.igniter.persistence.data.AppInfo
import io.github.freewebmovement.igniter.persistence.data.ExemptAppDataSource

class ExemptAppPresenter(
    private val mView: ExemptAppContract.View,
    private val mDataSource: ExemptAppDataSource
) : ExemptAppContract.Presenter {
    private var mDirty = false
    private var mConfigurationChanged = false
    private var mAllAppInfoList: List<AppInfo> = emptyList()
    // Apps the user manually set to bypass the tunnel (不代理). Every other app
    // is proxied by default, so the checkbox in the UI means "proxy this app".
    private var mBypassAppPackageNameSet: MutableSet<String> = mutableSetOf()
    private var mTab = AppTab.NORMAL
    private var mFilterName = ""

    init {
        mView.setPresenter(this)
    }

    override fun updateAppInfo(appInfo: AppInfo, position: Int, enable: Boolean) {
        mDirty = true
        val packageName = appInfo.packageName ?: return
        if (enable) {
            mBypassAppPackageNameSet.remove(packageName)
        } else {
            mBypassAppPackageNameSet.add(packageName)
        }
        appInfo.enabled = enable
        applyFilter()
    }

    override fun filterAppsByName(name: String) {
        mFilterName = name
        applyFilter()
    }

    override fun switchTab(tab: AppTab) {
        mTab = tab
        applyFilter()
    }

    override fun selectAll() {
        mDirty = true
        for (appInfo in currentFilteredList()) {
            appInfo.packageName ?: continue
            mBypassAppPackageNameSet.remove(appInfo.packageName!!)
            appInfo.enabled = true
        }
        applyFilter()
    }

    override fun deselectAll() {
        mDirty = true
        for (appInfo in currentFilteredList()) {
            appInfo.packageName ?: continue
            mBypassAppPackageNameSet.add(appInfo.packageName!!)
            appInfo.enabled = false
        }
        applyFilter()
    }

    override fun saveExemptAppInfoList() {
        if (!mDirty) {
            mView.showSaveSuccess()
            return
        }
        mConfigurationChanged = true
        mView.showSaving()
        Threads.runOnWorkThread(object : Task() {
            override fun onRun() {
                mDataSource.saveExemptAppInfoSet(mBypassAppPackageNameSet)
                mDirty = false
                Threads.runOnUiThread {
                    mView.dismissLoading()
                    mView.showSaveSuccess()
                    // Restarting a running proxy applies the new exempt-app list
                    // to the VPN interface immediately.
                    mView.restartProxyIfRunning()
                }
            }
        })
    }

    override fun handleBackPressed(): Boolean {
        if (mDirty) {
            mView.showExitConfirm()
        }
        return mDirty
    }

    override fun exit() {
        mView.exit(mConfigurationChanged)
    }

    override fun start() {
        mView.showLoading()
        Threads.runOnWorkThread(object : Task() {
            override fun onRun() {
                showData()
            }
        })
    }

    @SuppressLint("NewApi")
    private fun showData() {
        val allAppInfoList = mDataSource.getAllAppInfoList()
        mBypassAppPackageNameSet = mDataSource.loadExemptAppPackageNameSet().toMutableSet()
        val proxyApps = ArrayList<AppInfo>()
        val directApps = ArrayList<AppInfo>()
        for (appInfo in allAppInfoList) {
            appInfo.enabled = !mBypassAppPackageNameSet.contains(appInfo.packageName)
            if (appInfo.enabled) {
                proxyApps.add(appInfo)
            } else {
                directApps.add(appInfo)
            }
        }
        proxyApps.sortBy { it.appName }
        directApps.sortBy { it.appName }

        proxyApps.addAll(directApps)
        mAllAppInfoList = proxyApps
        Threads.runOnUiThread {
            mView.showAppList(currentFilteredList())
            mView.dismissLoading()
        }
    }

    private fun currentFilteredList(): List<AppInfo> {
        return mAllAppInfoList.filter { appInfo ->
            val inTab = when (mTab) {
                AppTab.NORMAL -> !appInfo.isSystemApp
                AppTab.SYSTEM -> appInfo.isSystemApp
                AppTab.INTERNATIONAL -> DefaultInternationalApps.packageNames.contains(appInfo.packageName)
            }
            inTab &&
                (TextUtils.isEmpty(mFilterName) || appInfo.appName?.contains(mFilterName, ignoreCase = true) == true)
        }
    }

    private fun applyFilter() {
        Threads.runOnUiThread {
            mView.showAppList(currentFilteredList())
        }
    }
}
