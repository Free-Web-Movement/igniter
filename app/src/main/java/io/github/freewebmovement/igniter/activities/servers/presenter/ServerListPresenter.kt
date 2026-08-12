package io.github.freewebmovement.igniter.activities.servers.presenter

import android.content.Context
import android.net.Uri
import io.github.freewebmovement.igniter.activities.servers.contract.ServerListContract
import io.github.freewebmovement.igniter.activities.servers.data.ServerListDataSource
import io.github.freewebmovement.igniter.common.os.Task
import io.github.freewebmovement.igniter.common.os.Threads
import io.github.freewebmovement.igniter.persistence.TrojanConfig
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class ServerListPresenter(
    private val mView: ServerListContract.View,
    private val mDataManager: ServerListDataSource
) : ServerListContract.Presenter {

    init {
        mView.setPresenter(this)
    }

    override fun hideImportFileDescription() {
        mView.dismissImportFileDescription()
    }

    override fun displayImportFileDescription() {
        mView.showImportFileDescription()
    }

    override fun importConfigFromFile() {
        mView.openFileChooser()
    }

    override fun parseConfigsInFileStream(context: Context, fileUri: Uri) {
        Threads.runOnWorkThread(object : Task() {
            override fun onRun() {
                val sb = StringBuilder()
                try {
                    BufferedReader(InputStreamReader(context.contentResolver.openInputStream(fileUri))).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            sb.append(line)
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                val trojanConfigs = parseTrojanConfigsFromFileContent(sb.toString())
                val currentConfigs = mDataManager.loadServerConfigList().toMutableList()
                currentConfigs.addAll(trojanConfigs)
                // remove repeated configurations
                val newTrojanConfigRemoteAddrSet = HashSet<String?>()
                for (config in trojanConfigs) {
                    newTrojanConfigRemoteAddrSet.add(config.getRemoteAddr())
                }
                for (i in currentConfigs.indices.reversed()) {
                    if (newTrojanConfigRemoteAddrSet.contains(currentConfigs[i].getRemoteAddr())) {
                        currentConfigs.removeAt(i)
                    }
                }
                currentConfigs.addAll(trojanConfigs)
                mDataManager.replaceServerConfigs(currentConfigs)
                loadConfigs()
                mView.showAddTrojanConfigSuccess()
            }
        })
    }

    private fun parseTrojanConfigsFromFileContent(fileContent: String): List<TrojanConfig> {
        return try {
            val jsonObject = JSONObject(fileContent)
            val configs = jsonObject.optJSONArray("configs")
            if (configs == null) {
                return emptyList()
            }
            val len = configs.length()
            val list = ArrayList<TrojanConfig>(len)
            for (i in 0 until len) {
                val config = configs.getJSONObject(i)
                val remoteAddr = config.optString("server", "")
                if (remoteAddr.isEmpty()) {
                    continue
                }

                val tmp = TrojanConfig()
                tmp.setRemoteAddr(remoteAddr)
                tmp.setRemoteIP(config.optString("server_ip"))
                tmp.setRemotePort(config.optInt("server_port"))
                tmp.setPassword(config.optString("password"))
                tmp.setVerifyCert(config.optBoolean("verify"))
                list.add(tmp)
            }
            list
        } catch (e: JSONException) {
            e.printStackTrace()
            emptyList()
        }
    }

    override fun handleServerSelection(config: TrojanConfig) {
        mView.selectServerConfig(config)
    }

    override fun deleteServerConfig(config: TrojanConfig, pos: Int) {
        Threads.runOnWorkThread(object : Task() {
            override fun onRun() {
                mDataManager.deleteServerConfig(config)
                mView.removeServerConfig(config, pos)
            }
        })
    }

    override fun start() {
        Threads.runOnWorkThread(object : Task() {
            override fun onRun() {
                loadConfigs()
            }
        })
    }

    private fun loadConfigs() {
        val trojanConfigs = mDataManager.loadServerConfigList()
        mView.showServerConfigList(trojanConfigs)
    }
}
