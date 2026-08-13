package io.github.freewebmovement.igniter.activities.exempt.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import io.github.freewebmovement.igniter.IgniterApplication
import io.github.freewebmovement.igniter.R
import io.github.freewebmovement.igniter.activities.MainActivity
import io.github.freewebmovement.igniter.activities.exempt.adapter.AppInfoAdapter
import io.github.freewebmovement.igniter.activities.exempt.contract.ExemptAppContract
import io.github.freewebmovement.igniter.common.app.BaseFragment
import io.github.freewebmovement.igniter.common.dialog.AppSheet
import io.github.freewebmovement.igniter.databinding.FragmentExemptAppBinding
import io.github.freewebmovement.igniter.persistence.data.AppInfo

class ExemptAppFragment : BaseFragment(), ExemptAppContract.View {
    private var mPresenter: ExemptAppContract.Presenter? = null
    private var _binding: FragmentExemptAppBinding? = null
    private val binding get() = _binding!!
    private lateinit var mAppInfoAdapter: AppInfoAdapter
    private val app: IgniterApplication
        get() = IgniterApplication.getApplication()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        _binding = FragmentExemptAppBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        initListeners()
        initTabs()
        mPresenter!!.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initViews() {
        mAppInfoAdapter = AppInfoAdapter()
        binding.exemptAppRv.adapter = mAppInfoAdapter
        binding.exemptAppRv.addItemDecoration(DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL))
    }

    private fun initTabs() {
        binding.proxyTabNormal.isSelected = true
        binding.proxyTabSystem.isSelected = false
        binding.proxyTabNormal.setOnClickListener {
            binding.proxyTabNormal.isSelected = true
            binding.proxyTabSystem.isSelected = false
            mPresenter!!.switchTab(false)
        }
        binding.proxyTabSystem.setOnClickListener {
            binding.proxyTabNormal.isSelected = false
            binding.proxyTabSystem.isSelected = true
            mPresenter!!.switchTab(true)
        }
    }

    private fun initListeners() {
        mAppInfoAdapter.setOnItemOperationListener(object : AppInfoAdapter.OnItemOperationListener {
            override fun onToggle(enabled: Boolean, appInfo: AppInfo, position: Int) {
                mPresenter!!.updateAppInfo(appInfo, position, enabled)
            }
        })

        binding.searchExemptAppEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                mPresenter!!.filterAppsByName(s?.toString().orEmpty())
            }
        })

        binding.btnSelectAll.setOnClickListener { mPresenter!!.selectAll() }
        binding.btnDeselectAll.setOnClickListener { mPresenter!!.deselectAll() }
        binding.btnSaveProxyApps.setOnClickListener { mPresenter!!.saveExemptAppInfoList() }
    }

    override fun showSaveSuccess() {
        Snackbar.make(binding.root, R.string.common_save_success, Snackbar.LENGTH_SHORT).show()
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
        mAppInfoAdapter.refreshData(appInfoList)
    }

    override fun showLoading() {
        AppSheet.builder(this).showLoading(getString(R.string.exempt_app_loading_tip))
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
