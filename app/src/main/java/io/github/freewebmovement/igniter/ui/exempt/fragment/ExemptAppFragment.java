package io.github.freewebmovement.igniter.ui.exempt.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import io.github.freewebmovement.igniter.IgniterApplication;
import io.github.freewebmovement.igniter.R;
import io.github.freewebmovement.igniter.common.app.BaseFragment;
import io.github.freewebmovement.igniter.common.dialog.LoadingDialog;
import io.github.freewebmovement.igniter.ui.exempt.adapter.AppInfoAdapter;
import io.github.freewebmovement.igniter.ui.exempt.contract.ExemptAppContract;
import io.github.freewebmovement.igniter.ui.exempt.data.AppInfo;

public class ExemptAppFragment extends BaseFragment implements ExemptAppContract.View {
    public static final String TAG = "ExemptAppFragment";
    private ExemptAppContract.Presenter mPresenter;
    private Toolbar mTopBar;
    private RecyclerView mAppRv;
    private AppInfoAdapter mAppInfoAdapter;
    private LoadingDialog mLoadingDialog;
    IgniterApplication app;
    public ExemptAppFragment() {
        // Required empty public constructor
    }

    public static ExemptAppFragment newInstance() {
        return new ExemptAppFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_exempt_app, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        app = IgniterApplication.getApplication();
        super.onViewCreated(view, savedInstanceState);
        findViews();
        initViews();
        initListeners();
        mPresenter.start();
    }

    private void findViews() {
        mTopBar = findViewById(R.id.exemptAppTopBar);
        mAppRv = findViewById(R.id.exemptAppRv);
    }

    private void initViews() {
        FragmentActivity activity = getActivity();
        if (activity instanceof AppCompatActivity) {
            ((AppCompatActivity) activity).setSupportActionBar(mTopBar);
            setHasOptionsMenu(true);
        }
        mAppInfoAdapter = new AppInfoAdapter();
        mAppRv.setAdapter(mAppInfoAdapter);
        mAppRv.addItemDecoration(new DividerItemDecoration(mContext, LinearLayoutManager.VERTICAL));
    }

    private void initListeners() {
        mAppInfoAdapter.setOnItemOperationListener((exempt, appInfo, position) -> mPresenter.updateAppInfo(appInfo, position, exempt));
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_exempt_app, menu);
        if(menu instanceof MenuBuilder){
            MenuBuilder m = (MenuBuilder) menu;
            m.setOptionalIconsVisible(true);
        }

        initHideSystemSwitch(menu);

        MenuItem item = menu.findItem(R.id.action_search_app);
        SearchView searchView = null;
        if (item != null) {
            searchView = (SearchView) item.getActionView();
        }
        if (searchView != null) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String s) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String s) {
                    mPresenter.filterAppsByName(s);
                    return true;
                }
            });
        }
    }

    public void initHideSystemSwitch(Menu menu) {
        MenuItem item = (MenuItem) menu.findItem(R.id.hide_system_apps);
        if (item != null) {
            SwitchCompat switchCompat = (SwitchCompat)item.getActionView();
            switchCompat.setChecked(app.trojanPreferences.getShowSystemApps());
            switchCompat.setOnCheckedChangeListener((buttonView, isChecked) -> {
                app.trojanPreferences.setShowSystemApps(isChecked);
                mPresenter.start();
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_save_exempt_apps) {
            mPresenter.saveExemptAppInfoList();
            return true;
        }
        return false;
    }

    @Override
    public void showSaveSuccess() {
        Snackbar.make(mRootView, R.string.common_save_success, Snackbar.LENGTH_SHORT).setAction(R.string.exempt_app_exit,
                v -> mPresenter.exit()).show();
    }

    @Override
    public void showExitConfirm() {
        new AlertDialog.Builder(mContext)
                .setTitle(R.string.common_alert)
                .setMessage(R.string.exempt_app_exit_without_saving_confirm)
                .setNegativeButton(R.string.common_cancel, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.common_confirm, (dialog, which) -> {
                    dialog.dismiss();
                    mPresenter.exit();
                }).create().show();
    }

    @Override
    public void showAppList(final List<AppInfo> appInfoList) {
        mAppInfoAdapter.refreshData(appInfoList);
    }

    @Override
    public void showLoading() {
        if (mLoadingDialog == null) {
            mLoadingDialog = new LoadingDialog(requireContext());
            mLoadingDialog.setMsg(getString(R.string.exempt_app_loading_tip));
        }
        mLoadingDialog.show();
    }

    @Override
    public void dismissLoading() {
        if (mLoadingDialog != null && mLoadingDialog.isShowing()) {
            mLoadingDialog.dismiss();
        }
    }

    @Override
    public void exit(boolean configurationChanged) {
        Activity activity = getActivity();
        if (activity != null) {
            activity.setResult(configurationChanged ? Activity.RESULT_OK : Activity.RESULT_CANCELED);
            activity.finish();
        }
    }

    @Override
    public void setPresenter(ExemptAppContract.Presenter presenter) {
        mPresenter = presenter;
    }
}
