package io.github.freewebmovement.igniter.activities.servers.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import io.github.freewebmovement.igniter.persistence.TrojanConfig
import io.github.freewebmovement.igniter.persistence.database.AccessDatabase
import io.github.freewebmovement.igniter.persistence.database.Server
import io.github.freewebmovement.igniter.persistence.database.ServerDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class ServerListViewModel(application: Application) : AndroidViewModel(application) {
    private val serverDao: ServerDao = AccessDatabase.getDatabase().serverDao()

    val servers: LiveData<List<Server>> = serverDao.observeAllLive()

    fun deleteServer(server: Server) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { serverDao.delete(server) }
        }
    }

    fun importConfigsFromFile(fileUri: Uri) {
        viewModelScope.launch {
            val imported = withContext(Dispatchers.IO) { readConfigsFromFile(fileUri) }
            if (imported.isEmpty()) {
                return@launch
            }
            withContext(Dispatchers.IO) {
                val existing = serverDao.all()
                    .map { it.hostname to it.port }
                    .toSet()
                val toInsert = imported.filter { (it.hostname to it.port) !in existing }
                if (toInsert.isNotEmpty()) {
                    serverDao.insert(*toInsert.toTypedArray())
                }
            }
        }
    }

    fun insertIfMissing(config: TrojanConfig?) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { AccessDatabase.insertServerIfMissing(config) }
        }
    }

    private fun readConfigsFromFile(fileUri: Uri): List<Server> {
        val sb = StringBuilder()
        try {
            BufferedReader(
                InputStreamReader(getApplication<Application>().contentResolver.openInputStream(fileUri))
            ).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return parseConfigsFromFileContent(sb.toString())
    }

    private fun parseConfigsFromFileContent(fileContent: String): List<Server> {
        return try {
            val jsonObject = JSONObject(fileContent)
            val configs = jsonObject.optJSONArray("configs")
            if (configs == null) {
                return emptyList()
            }
            val len = configs.length()
            val list = ArrayList<Server>(len)
            for (i in 0 until len) {
                val config = configs.getJSONObject(i)
                val remoteAddr = config.optString("server", "")
                if (remoteAddr.isEmpty()) {
                    continue
                }
                val server = Server()
                server.hostname = remoteAddr
                server.port = config.optInt("server_port")
                server.password = config.optString("password")
                list.add(server)
            }
            list
        } catch (e: JSONException) {
            e.printStackTrace()
            emptyList()
        }
    }
}
