package io.github.freewebmovement.igniter.activities.servers.activity

import android.app.Activity
import android.database.sqlite.SQLiteConstraintException
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.github.freewebmovement.igniter.R
import io.github.freewebmovement.igniter.persistence.database.AccessDatabase
import io.github.freewebmovement.igniter.persistence.database.AppDatabase
import io.github.freewebmovement.igniter.persistence.database.Server
import io.github.freewebmovement.igniter.persistence.database.ServerDao

class AddServerActivity : AppCompatActivity() {

    val TAG = "ADD SERVER"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_server)
    }

    private fun checkText(text: EditText, resId: Int): Boolean {
        if (text.text.length < 1) {
            Toast.makeText(this, resId, Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }

    fun onSave(view: View) {
        val etRemoteHost = findViewById<EditText>(R.id.remote_address)
        val etRemotePort = findViewById<EditText>(R.id.remote_port)
        val etRemotePassword = findViewById<EditText>(R.id.remote_password)
        val etLocalPort = findViewById<EditText>(R.id.local_port)
        val tvTrojanURI = findViewById<TextView>(R.id.trojan_uri)

        if (!checkText(etRemoteHost, R.string.error_remote_address) ||
            !checkText(etRemotePort, R.string.error_remote_port) ||
            !checkText(etRemotePassword, R.string.error_remote_password) ||
            !checkText(etLocalPort, R.string.error_local_port)
        ) {
            return
        }

        val remoteAddress = etRemoteHost.text.toString()
        val remotePort = etRemotePort.text.toString()
        val remotePassword = etRemotePassword.text.toString()
        val localPort = etLocalPort.text.toString()

        val trojanURI = "trojan://$remotePassword@$remoteAddress:$remotePort"

        tvTrojanURI.text = trojanURI
        Thread {
            // A potentially time consuming task.
            try {
                val db: AppDatabase = AccessDatabase.getDatabase(applicationContext)
                val serverDao: ServerDao = db.serverDao()
                val server = Server()
                server.hostname = remoteAddress
                server.port = Integer.parseInt(remotePort)
                server.password = remotePassword
                server.local_port = Integer.parseInt(localPort)
                serverDao.insert(server)
                setResult(Activity.RESULT_OK)
            } catch (e: SQLiteConstraintException) {
                e.printStackTrace()
                if (e.message?.startsWith("UNIQUE constraint failed") == true) {
                    runOnUiThread {
                        Toast.makeText(this, R.string.error_dabase_unique_constraint_failed,
                            Toast.LENGTH_LONG).show()
                    }
                }
                setResult(Activity.RESULT_CANCELED)
            }
            finish()
        }.start()
    }
}
