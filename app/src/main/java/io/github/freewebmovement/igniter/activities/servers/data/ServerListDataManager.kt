package io.github.freewebmovement.igniter.activities.servers.data

import io.github.freewebmovement.igniter.IgniterApplication
import io.github.freewebmovement.igniter.persistence.TrojanConfig
import io.github.freewebmovement.igniter.persistence.database.AccessDatabase

class ServerListDataManager : ServerListDataSource {

    override fun loadServerConfigList(): List<TrojanConfig> {
        val list = AccessDatabase.readServers(IgniterApplication.getApplication()).toMutableList()
        val current = IgniterApplication.getApplication().trojanConfig
        if (!current.getRemoteAddr().isEmpty() && !containsServer(list, current)) {
            list.add(0, current)
            AccessDatabase.insertServerIfMissing(IgniterApplication.getApplication(), current)
        }
        return list
    }

    private fun containsServer(list: List<TrojanConfig>, config: TrojanConfig): Boolean {
        for (c in list) {
            if (c.getRemoteAddr() == config.getRemoteAddr() && c.getRemotePort() == config.getRemotePort()) {
                return true
            }
        }
        return false
    }

    override fun deleteServerConfig(config: TrojanConfig) {
        AccessDatabase.deleteServer(IgniterApplication.getApplication(),
            config.getRemoteAddr(),
            config.getRemotePort())
    }

    override fun saveServerConfig(config: TrojanConfig) {
        var configExisted = false
        val trojanConfigs = loadServerConfigList().toMutableList()
        for (i in trojanConfigs.indices.reversed()) {
            val remoteAddress = arrayOfNulls<String>(2)
            val remotePort = IntArray(2)

            remoteAddress[0] = trojanConfigs[i].getRemoteAddr()
            remoteAddress[1] = config.getRemoteAddr()
            remotePort[0] = trojanConfigs[i].getRemotePort()
            remotePort[1] = config.getRemotePort()

            if (remoteAddress[0] == remoteAddress[1] && remotePort[0] == remotePort[1]) {
                trojanConfigs[i] = config
                configExisted = true
                break
            }
        }
        if (!configExisted) {
            trojanConfigs.add(config)
        }
        replaceServerConfigs(trojanConfigs)
    }

    override fun replaceServerConfigs(list: List<TrojanConfig>) {
        AccessDatabase.replaceServers(IgniterApplication.getApplication(), list)
    }
}
