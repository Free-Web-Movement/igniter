package io.github.freewebmovement.igniter.activities.exempt.contract

import androidx.annotation.AnyThread
import androidx.annotation.UiThread
import io.github.freewebmovement.igniter.common.mvp.BasePresenter
import io.github.freewebmovement.igniter.common.mvp.BaseView
import io.github.freewebmovement.igniter.persistence.data.AppInfo

interface ExemptAppContract {
    interface Presenter : BasePresenter {
        fun updateAppInfo(appInfo: AppInfo, position: Int, exempt: Boolean)

        fun saveExemptAppInfoList()

        /**
         * @return true if exit directly, false to cancel exiting.
         */
        fun handleBackPressed(): Boolean

        fun filterAppsByName(name: String)

        fun exit()
    }

    interface View : BaseView<Presenter> {
        @UiThread
        fun showLoading()

        @UiThread
        fun dismissLoading()

        @UiThread
        fun showSaveSuccess()

        @UiThread
        fun showExitConfirm()

        @UiThread
        fun showAppList(packageNames: List<AppInfo>)

        @AnyThread
        fun exit(configurationChanged: Boolean)
    }
}
