package io.github.freewebmovement.igniter.activities.exempt.presenter;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.freewebmovement.igniter.common.os.Task;
import io.github.freewebmovement.igniter.common.os.Threads;
import io.github.freewebmovement.igniter.activities.exempt.contract.ExemptAppContract;
import io.github.freewebmovement.igniter.persistence.data.AppInfo;
import io.github.freewebmovement.igniter.persistence.data.ExemptAppDataSource;

public class ExemptAppPresenter implements ExemptAppContract.Presenter {
    private final ExemptAppContract.View mView;
    private final ExemptAppDataSource mDataSource;
    private boolean mDirty;
    private boolean mConfigurationChanged;
    private List<AppInfo> mAllAppInfoList;
    private Set<String> mExemptAppPackageNameSet;

    public ExemptAppPresenter(ExemptAppContract.View view, ExemptAppDataSource dataSource) {
        super();
        mView = view;
        mDataSource = dataSource;
        view.setPresenter(this);
    }

    @Override
    public void updateAppInfo(AppInfo appInfo, int position, boolean enable) {
        mDirty = true;
        String packageName = appInfo.getPackageName();
        if (mExemptAppPackageNameSet.contains(packageName)) {
            if (enable) {
                mExemptAppPackageNameSet.remove(packageName);
            }
        } else if (!enable) {
            mExemptAppPackageNameSet.add(packageName);
        }
        appInfo.setEnabled(enable);
    }

    @Override
    public void filterAppsByName(final String name) {
        if (TextUtils.isEmpty(name)) {
            mView.showAppList(mAllAppInfoList);
            return;
        }
        Threads.instance().runOnWorkThread(new Task() {
            @Override
            public void onRun() {
                final List<AppInfo> tmpInfoList = new ArrayList<>();
                for (AppInfo appInfo : mAllAppInfoList) {
                    if (appInfo.getAppName().contains(name)) {
                        tmpInfoList.add(appInfo);
                    }
                }
                Threads.instance().runOnUiThread(() -> mView.showAppList(tmpInfoList));
            }
        });
    }

    @Override
    public void saveExemptAppInfoList() {
        if (!mDirty) {
            mView.showSaveSuccess();
            return;
        }
        mConfigurationChanged = true;
        mView.showLoading();
        Threads.instance().runOnWorkThread(new Task() {
            @Override
            public void onRun() {
                mDataSource.saveExemptAppInfoSet(mExemptAppPackageNameSet);
                mDirty = false;
                Threads.instance().runOnUiThread(() -> {
                    mView.dismissLoading();
                    mView.showSaveSuccess();
                });
            }
        });
    }

    @Override
    public boolean handleBackPressed() {
        if (mDirty) {
            mView.showExitConfirm();
        }
        return mDirty;
    }

    @Override
    public void exit() {
        mView.exit(mConfigurationChanged);
    }

    @Override
    public void start() {
        mView.showLoading();
        Threads.instance().runOnWorkThread(new Task() {
            @Override
            public void onRun() {
                showData();
            }
        });
    }

    private void showData() {
        final List<AppInfo> allAppInfoList = mDataSource.getAllAppInfoList();
        mExemptAppPackageNameSet = mDataSource.loadExemptAppPackageNameSet();
        List<AppInfo> exemptApps = new ArrayList<AppInfo>();
        List<AppInfo> enabledApps = new ArrayList<AppInfo>();
        for (AppInfo appInfo : allAppInfoList) {
            if (mExemptAppPackageNameSet.contains(appInfo.getPackageName())) {
                exemptApps.add(appInfo);
            } else {
                enabledApps.add(appInfo);
                appInfo.setEnabled(true);
            }
        }
        // cluster exempted apps.
        Collections.sort(exemptApps, (o1, o2) -> {
            return o1.getAppName().compareTo(o2.getAppName());
        });

        Collections.sort(enabledApps, (o1, o2) -> {
            return o1.getAppName().compareTo(o2.getAppName());
        });

        enabledApps.addAll(exemptApps);
        mAllAppInfoList = enabledApps;
                Threads.instance().runOnUiThread(() -> {
            mView.showAppList(mAllAppInfoList);
            mView.dismissLoading();
        });
    }
}
