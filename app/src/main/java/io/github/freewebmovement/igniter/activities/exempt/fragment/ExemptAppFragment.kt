package io.github.freewebmovement.igniter.activities.exempt.fragment

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import io.github.freewebmovement.igniter.R
import io.github.freewebmovement.igniter.activities.MainActivity
import io.github.freewebmovement.igniter.activities.exempt.contract.ExemptAppContract
import io.github.freewebmovement.igniter.common.app.BaseFragment
import io.github.freewebmovement.igniter.common.dialog.AppSheet
import io.github.freewebmovement.igniter.persistence.data.AppInfo
import io.github.freewebmovement.igniter.theme.IgniterTheme
import io.github.freewebmovement.igniter.ui.exempt.ExemptAppScreen

class ExemptAppFragment : BaseFragment(), ExemptAppContract.View {
    private var mPresenter: ExemptAppContract.Presenter? = null
    private var mAppList by mutableStateOf(emptyList<AppInfo>(), neverEqualPolicy())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                IgniterTheme {
                    ExemptAppScreen(
                        apps = mAppList,
                        onSearchChange = { mPresenter?.filterAppsByName(it) },
                        onTabSelected = { mPresenter?.switchTab(it) },
                        onSelectAll = { mPresenter?.selectAll() },
                        onDeselectAll = { mPresenter?.deselectAll() },
                        onSave = { mPresenter?.saveExemptAppInfoList() },
                        onToggle = { app, position, enabled ->
                            mPresenter?.updateAppInfo(app, position, enabled)
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mPresenter!!.start()
    }

    override fun showSaveSuccess() {
        Toast.makeText(requireContext(), R.string.common_save_success, Toast.LENGTH_SHORT).show()
    }

    override fun restartProxyIfRunning() {
        (activity as? MainActivity)?.restartProxy()
    }

    override fun showExitConfirm() {
        AppSheet.builder(this)
            .setTitle(R.string.common_alert)
            .setMessage(R.string.exempt_app_exit_without_saving_confirm)
            .setNegativeButton(R.string.common_cancel, null)
            .setPositiveButton(R.string.common_confirm) {
                mPresenter!!.exit()
            }
            .show()
    }

    override fun showAppList(appInfoList: List<AppInfo>) {
        mAppList = appInfoList
    }

    override fun showLoading() {
        AppSheet.builder(this).showLoading(getString(R.string.exempt_app_loading_tip))
    }

    override fun showSaving() {
        AppSheet.builder(this).showLoading(getString(R.string.exempt_app_saving_tip))
    }

    override fun dismissLoading() {
        AppSheet.dismissActive()
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
