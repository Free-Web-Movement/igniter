package io.github.freewebmovement.igniter.activities.servers.activity

import android.app.Activity
import android.content.Intent
import android.database.sqlite.SQLiteConstraintException
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.freewebmovement.igniter.R
import io.github.freewebmovement.igniter.common.util.QrCodeUtils
import io.github.freewebmovement.igniter.persistence.TrojanConfig
import io.github.freewebmovement.igniter.persistence.database.AccessDatabase
import io.github.freewebmovement.igniter.persistence.database.AppDatabase
import io.github.freewebmovement.igniter.persistence.database.Server
import io.github.freewebmovement.igniter.persistence.database.ServerDao
import io.github.freewebmovement.igniter.theme.IgniterTheme
import io.github.freewebmovement.igniter.ui.server.AddServerScreen

class AddServerActivity : AppCompatActivity() {

    private var mUri by mutableStateOf("")
    private var mHost by mutableStateOf("")
    private var mPort by mutableStateOf("")
    private var mLocalPort by mutableStateOf("")
    private var mPassword by mutableStateOf("")

    private val scanQrLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.getStringExtra(ScanQrCodeActivity.EXTRA_QR_RESULT)
            if (!uri.isNullOrEmpty()) {
                mUri = uri
                parseQuickAdd(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (mLocalPort.isEmpty()) {
            mLocalPort = getString(R.string.default_local_or_clash_port)
        }
        setContent {
            IgniterTheme {
                AddServerScreen(
                    uri = mUri,
                    host = mHost,
                    port = mPort,
                    localPort = mLocalPort,
                    password = mPassword,
                    onUriChange = { newUri ->
                        mUri = newUri
                        parseQuickAdd(newUri)
                    },
                    onHostChange = { mHost = it },
                    onPortChange = { mPort = it },
                    onLocalPortChange = { mLocalPort = it },
                    onPasswordChange = { mPassword = it },
                    onScanQr = {
                        scanQrLauncher.launch(Intent(this, ScanQrCodeActivity::class.java))
                    },
                    onGenerateQr = { uri -> onGenerateQr(uri) },
                    onSave = { host, port, password, localPort ->
                        onSave(host, port, password, localPort)
                    },
                    onBack = { finish() }
                )
            }
        }

        val editId = intent.getIntExtra(EXTRA_SERVER_ID, NO_SERVER_ID)
        if (editId != NO_SERVER_ID) {
            loadServerForEdit(editId)
        }
    }

    /** Fills the manual fields automatically when a trojan:// URI is pasted. */
    private fun parseQuickAdd(uri: String) {
        val config = try {
            TrojanConfig.fromURIString(uri)
        } catch (e: Exception) {
            null
        } ?: return
        val host = config.getRemoteAddr()
        val password = config.getPassword()
        if (host == "0.0.0.0" || password.isNullOrEmpty()) {
            return
        }
        val port = config.getRemotePort()
        mHost = host
        mPort = if (port > 0) port.toString() else getString(R.string.default_port)
        mPassword = password
    }

    /** Generates a QR code from the current form values and shows it for sharing. */
    private fun onGenerateQr(uri: String) {
        showQrDialog(uri)
    }

    private fun showQrDialog(uri: String) {
        val bitmap = QrCodeUtils.generateQrBitmap(uri)
        if (bitmap == null) {
            Toast.makeText(this, R.string.qr_generate_failed, Toast.LENGTH_LONG).show()
            return
        }
        val imageView = ImageView(this)
        imageView.setPadding(
            (24 * resources.displayMetrics.density).toInt(),
            (24 * resources.displayMetrics.density).toInt(),
            (24 * resources.displayMetrics.density).toInt(),
            (24 * resources.displayMetrics.density).toInt()
        )
        imageView.setImageBitmap(bitmap)
        AlertDialog.Builder(this)
            .setTitle(R.string.qr_dialog_title)
            .setView(imageView)
            .setPositiveButton(R.string.qr_share_action) { _, _ ->
                shareUri(uri)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun shareUri(uri: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, uri)
        }
        startActivity(Intent.createChooser(send, getString(R.string.qr_share_uri)))
    }

    private fun loadServerForEdit(id: Int) {
        Thread {
            val server = AccessDatabase.getDatabase().serverDao().findById(id)
            runOnUiThread {
                if (server == null || isFinishing) {
                    return@runOnUiThread
                }
                mHost = server.hostname
                mPort = server.port.toString()
                mLocalPort = if (server.local_port > 0) {
                    server.local_port.toString()
                } else {
                    getString(R.string.default_local_or_clash_port)
                }
                mPassword = server.password
            }
        }.start()
    }

    private fun checkText(text: String, resId: Int): Boolean {
        if (text.length < 1) {
            Toast.makeText(this, resId, Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }

    private fun onSave(host: String, port: String, password: String, localPort: String) {
        if (!checkText(host, R.string.error_remote_address) ||
            !checkText(port, R.string.error_remote_port) ||
            !checkText(password, R.string.error_remote_password) ||
            !checkText(localPort, R.string.error_local_port)
        ) {
            return
        }

        val editId = intent.getIntExtra(EXTRA_SERVER_ID, NO_SERVER_ID)
        Thread {
            try {
                val db: AppDatabase = AccessDatabase.getDatabase()
                val serverDao: ServerDao = db.serverDao()
                val server = if (editId == NO_SERVER_ID) Server() else
                    (serverDao.findById(editId) ?: Server())
                server.hostname = host
                server.port = Integer.parseInt(port)
                server.password = password
                server.local_port = Integer.parseInt(localPort)
                if (editId == NO_SERVER_ID) {
                    serverDao.insert(server)
                } else {
                    serverDao.update(server)
                }
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

    companion object {
        const val EXTRA_SERVER_ID = "server_id"
        const val NO_SERVER_ID = -1
    }
}
