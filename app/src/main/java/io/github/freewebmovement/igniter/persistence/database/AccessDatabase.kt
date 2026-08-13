package io.github.freewebmovement.igniter.persistence.database

import androidx.room.Room
import io.github.freewebmovement.igniter.IgniterApplication
import io.github.freewebmovement.igniter.persistence.TrojanConfig

object AccessDatabase {
    const val databaseName = "trojan.db"
    private lateinit var db: AppDatabase

    @JvmStatic
    fun getDatabase(): AppDatabase {
        if (!::db.isInitialized) {
            db = Room.databaseBuilder(
                IgniterApplication.getApplication().applicationContext,
                AppDatabase::class.java,
                databaseName
            ).build()
        }
        return db
    }

    @JvmStatic
    fun deleteServer(remoteAddress: String, port: Int) {
        val appDatabase = getDatabase()
        val serverDao = appDatabase.serverDao()
        serverDao.deleteByUniquePair(remoteAddress, port)
    }

    @JvmStatic
    fun readServers(): List<TrojanConfig> {
        val appDatabase = getDatabase()
        val serverDao = appDatabase.serverDao()
        val servers = serverDao.all()
        val configList = ArrayList<TrojanConfig>(servers.size)
        for (server in servers) {
            val tc = TrojanConfig()
            tc.setRemoteAddr(server.hostname)
            tc.setRemotePort(server.port)
            tc.setPassword(server.password)
            tc.setLocalPort(server.local_port)
            configList.add(tc)
        }
        return configList
    }

    /** Replaces the whole server list with the given configs, atomically. */
    @JvmStatic
    fun replaceServers(configs: List<TrojanConfig>) {
        val appDatabase = getDatabase()
        val serverDao = appDatabase.serverDao()
        appDatabase.runInTransaction {
            serverDao.deleteAll()
            for (config in configs) {
                serverDao.insert(toServer(config))
            }
        }
    }

    /** Persists a server unless one with the same hostname/port already exists. */
    @JvmStatic
    fun insertServerIfMissing(config: TrojanConfig?) {
        if (config == null || config.getRemoteAddr().isEmpty()) {
            return
        }
        val appDatabase = getDatabase()
        val serverDao = appDatabase.serverDao()
        if (serverDao.findByHostAndPort(config.getRemoteAddr(), config.getRemotePort()) != null) {
            return
        }
        serverDao.insert(toServer(config))
    }

    private fun toServer(config: TrojanConfig): Server {
        val server = Server()
        server.hostname = config.getRemoteAddr()
        server.port = config.getRemotePort()
        server.password = config.getPassword() ?: ""
        server.local_port = config.getLocalPort()
        server.localhost = "127.0.0.1"
        return server
    }
}
