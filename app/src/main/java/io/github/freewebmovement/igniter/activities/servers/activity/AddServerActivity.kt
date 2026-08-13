package io.github.freewebmovement.igniter.activities.servers.activity

import android.app.Activity
import android.content.Intent
import android.database.sqlite.SQLiteConstraintException
import android.graphics.Bitmap
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import io.github.freewebmovement.igniter.R
import io.github.freewebmovement.igniter.common.util.QrCodeUtils
import io.github.freewebmovement.igniter.persistence.TrojanConfig
import io.github.freewebmovement.igniter.persistence.database.AccessDatabase
import io.github.freewebmovement.igniter.persistence.database.AppDatabase
import io.github.freewebmovement.igniter.persistence.database.Server
import io.github.freewebmovement.igniter.persistence.database.ServerDao

class AddServerActivity : AppCompatActivity() {

    val TAG = "ADD SERVER"

    private val scanQrLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.getStringExtra(ScanQrCodeActivity.EXTRA_QR_RESULT)
            if (!uri.isNullOrEmpty()) {
                findViewById<EditText>(R.id.quick_add_uri).setText(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_server)
        bindTrojanUriPreview()
        bindQuickAdd()

        val editId = intent.getIntExtra(EXTRA_SERVER_ID, NO_SERVER_ID)
        if (editId != NO_SERVER_ID) {
            loadServerForEdit(editId)
        }
    }

    /** Fills the manual fields automatically when a trojan:// URI is pasted. */
    private fun bindQuickAdd() {
        val etUri = findViewById<EditText>(R.id.quick_add_uri)
        etUri.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val config = try {
                    TrojanConfig.fromURIString(s?.toString().orEmpty())
                } catch (e: Exception) {
                    null
                } ?: return
                val host = config.getRemoteAddr()
                val password = config.getPassword()
                if (host == "0.0.0.0" || password.isNullOrEmpty()) {
                    return
                }
                val port = config.getRemotePort()
                findViewById<EditText>(R.id.remote_address).setText(host)
                findViewById<EditText>(R.id.remote_port).setText(
                    if (port > 0) port.toString() else getString(R.string.default_port))
                findViewById<EditText>(R.id.remote_password).setText(password)
            }
        })
    }

    /** Launches the camera QR scanner. */
    fun onScanQr(view: View) {
        scanQrLauncher.launch(Intent(this, ScanQrCodeActivity::class.java))
    }

    /** Generates a QR code from the current form values and shows it for sharing. */
    fun onGenerateQr(view: View) {
        val host = findViewById<EditText>(R.id.remote_address).text.toString().trim()
        val port = findViewById<EditText>(R.id.remote_port).text.toString().trim()
        val pw = findViewById<EditText>(R.id.remote_password).text.toString().trim()
        if (host.isEmpty() || port.isEmpty() || pw.isEmpty()) {
            Toast.makeText(this, R.string.qr_generate_incomplete, Toast.LENGTH_LONG).show()
            return
        }
        val uri = "trojan://$pw@$host:$port"
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

    /** Shows the trojan URI live while the host/port/password fields change. */
    private fun bindTrojanUriPreview() {
        val tvTrojanURI = findViewById<TextView>(R.id.trojan_uri)
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val host = findViewById<EditText>(R.id.remote_address).text.toString().trim()
                val port = findViewById<EditText>(R.id.remote_port).text.toString().trim()
                val pw = findViewById<EditText>(R.id.remote_password).text.toString().trim()
                tvTrojanURI.text = if (host.isNotEmpty() && port.isNotEmpty() && pw.isNotEmpty()) {
                    "trojan://$pw@$host:$port"
                } else {
                    ""
                }
            }
        }
        findViewById<EditText>(R.id.remote_address).addTextChangedListener(watcher)
        findViewById<EditText>(R.id.remote_port).addTextChangedListener(watcher)
        findViewById<EditText>(R.id.remote_password).addTextChangedListener(watcher)
    }

    private fun loadServerForEdit(id: Int) {
        Thread {
            val server = AccessDatabase.getDatabase().serverDao().findById(id)
            runOnUiThread {
                if (server == null || isFinishing) {
                    return@runOnUiThread
                }
                findViewById<EditText>(R.id.remote_address).setText(server.hostname)
                findViewById<EditText>(R.id.remote_port).setText(server.port.toString())
                findViewById<EditText>(R.id.local_port).setText(server.local_port.toString())
                findViewById<EditText>(R.id.remote_password).setText(server.password)
            }
        }.start()
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
        val editId = intent.getIntExtra(EXTRA_SERVER_ID, NO_SERVER_ID)
        Thread {
            // A potentially time consuming task.
            try {
                val db: AppDatabase = AccessDatabase.getDatabase()
                val serverDao: ServerDao = db.serverDao()
                val server = if (editId == NO_SERVER_ID) Server() else
                    (serverDao.findById(editId) ?: Server())
                server.hostname = remoteAddress
                server.port = Integer.parseInt(remotePort)
                server.password = remotePassword
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
