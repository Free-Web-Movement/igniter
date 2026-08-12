package io.github.freewebmovement.igniter.activities.exempt.presenter

import android.annotation.SuppressLint
import android.text.TextUtils
import io.github.freewebmovement.igniter.activities.exempt.contract.ExemptAppContract
import io.github.freewebmovement.igniter.common.os.Task
import io.github.freewebmovement.igniter.common.os.Threads
import io.github.freewebmovement.igniter.persistence.data.AppInfo
import io.github.freewebmovement.igniter.persistence.data.ExemptAppDataSource

class ExemptAppPresenter(
    private val mView: ExemptAppContract.View,
    private val mDataSource: ExemptAppDataSource
) : ExemptAppContract.Presenter {
    private var mDirty = false
    private var mConfigurationChanged = false
    private var mAllAppInfoList: List<AppInfo> = emptyList()
    private var mExemptAppPackageNameSet: MutableSet<String> = mutableSetOf()

    init {
        mView.setPresenter(this)
    }

    override fun updateAppInfo(appInfo: AppInfo, position: Int, enable: Boolean) {
        mDirty = true
        val packageName = appInfo.packageName!!
        if (mExemptAppPackageNameSet.contains(packageName)) {
            if (enable) {
                mExemptAppPackageNameSet.remove(packageName)
            }
        } else if (!enable) {
            mExemptAppPackageNameSet.add(packageName)
        }
        appInfo.enabled = enable
    }

    override fun filterAppsByName(name: String) {
        if (TextUtils.isEmpty(name)) {
            mView.showAppList(mAllAppInfoList)
            return
        }
        Threads.runOnWorkThread(object : Task() {
            override fun onRun() {
                val tmpInfoList = ArrayList<AppInfo>()
                for (appInfo in mAllAppInfoList) {
                    if (appInfo.appName?.contains(name) == true) {
                        tmpInfoList.add(appInfo)
                    }
                }
                Threads.runOnUiThread { mView.showAppList(tmpInfoList) }
            }
        })
    }

    override fun saveExemptAppInfoList() {
        if (!mDirty) {
            mView.showSaveSuccess()
            return
        }
        mConfigurationChanged = true
        mView.showLoading()
        Threads.runOnWorkThread(object : Task() {
            override fun onRun() {
                mDataSource.saveExemptAppInfoSet(mExemptAppPackageNameSet)
                mDirty = false
                Threads.runOnUiThread {
                    mView.dismissLoading()
                    mView.showSaveSuccess()
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
        mExemptAppPackageNameSet = mDataSource.loadExemptAppPackageNameSet().toMutableSet()
        val exemptApps = ArrayList<AppInfo>()
        val enabledApps = ArrayList<AppInfo>()
        for (appInfo in allAppInfoList) {
            if (mExemptAppPackageNameSet.contains(appInfo.packageName!!)) {
                exemptApps.add(appInfo)
            } else {
                enabledApps.add(appInfo)
                appInfo.enabled = true
            }
        }
        // cluster exempted apps.
        exemptApps.sortBy { it.appName }
        enabledApps.sortBy { it.appName }

        enabledApps.addAll(exemptApps)
        mAllAppInfoList = enabledApps
        Threads.runOnUiThread {
            mView.showAppList(mAllAppInfoList)
            mView.dismissLoading()
        }
    }
}
