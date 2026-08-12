package io.github.freewebmovement.igniter.ui.component.textview

import android.view.inputmethod.EditorInfo
import android.widget.EditText
import io.github.freewebmovement.igniter.IgniterApplication
import io.github.freewebmovement.igniter.persistence.TrojanConfig
import io.github.freewebmovement.igniter.ui.component.textview.listener.TextViewListener

class URIEditText(private val et: EditText, private val app: IgniterApplication) {

    fun init() {
        et.setOnLongClickListener {
            et.selectAll()
            false
        }

        et.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                et.setInputType(EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_PASSWORD)
            } else {
                // it seems we don't have to place cursor on the end for Trojan URL
                et.setInputType(EditorInfo.TYPE_CLASS_TEXT)
            }
        }

        et.addTextChangedListener(object : TextViewListener(et, app) {
            override fun onTextChanged(before: String, old: String, aNew: String, after: String) {
                // update TextView
                val parsedConfig = TrojanConfig.fromURIString(before + aNew + after)
                if (parsedConfig != null) {
                    val remoteAddress = parsedConfig.getRemoteAddr()
                    val remotePort = parsedConfig.getRemotePort()
                    val password = parsedConfig.getPassword()

                    app.trojanConfig.setRemoteAddr(remoteAddress)
                    app.trojanConfig.setRemotePort(remotePort)
                    app.trojanConfig.setPassword(password ?: "")
                }
            }
        })
    }
}
