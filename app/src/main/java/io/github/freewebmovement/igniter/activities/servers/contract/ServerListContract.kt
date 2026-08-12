package io.github.freewebmovement.igniter.activities.servers.contract

import android.content.Context
import android.net.Uri
import io.github.freewebmovement.igniter.common.mvp.BasePresenter
import io.github.freewebmovement.igniter.common.mvp.BaseView
import io.github.freewebmovement.igniter.persistence.TrojanConfig

interface ServerListContract {
    interface Presenter : BasePresenter {
        fun handleServerSelection(config: TrojanConfig)
        fun deleteServerConfig(config: TrojanConfig, pos: Int)
        fun displayImportFileDescription()
        fun hideImportFileDescription()
        fun importConfigFromFile()
        fun parseConfigsInFileStream(context: Context, fileUri: Uri)
    }

    interface View : BaseView<Presenter> {
        fun showAddTrojanConfigSuccess()
        fun selectServerConfig(config: TrojanConfig)
        fun showServerConfigList(configs: List<TrojanConfig>)
        fun removeServerConfig(config: TrojanConfig, pos: Int)
        fun showImportFileDescription()
        fun dismissImportFileDescription()
        fun openFileChooser()
    }
}
