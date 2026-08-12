package io.github.freewebmovement.igniter.activities.servers.data

import androidx.annotation.WorkerThread
import io.github.freewebmovement.igniter.persistence.TrojanConfig

interface ServerListDataSource {
    @WorkerThread
    fun loadServerConfigList(): List<TrojanConfig>

    @WorkerThread
    fun deleteServerConfig(config: TrojanConfig)

    @WorkerThread
    fun saveServerConfig(config: TrojanConfig)

    @WorkerThread
    fun replaceServerConfigs(list: List<TrojanConfig>)
}
