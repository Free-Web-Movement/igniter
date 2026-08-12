package io.github.freewebmovement.igniter.ui.component.textview.listener

import android.widget.TextView
import io.github.freewebmovement.igniter.IgniterApplication

class RemotePort(tv: TextView, app: IgniterApplication) : TextViewListener(tv, app) {
    override fun onTextChanged(before: String, old: String, aNew: String, after: String) {
        // update TextView
        if (tv.hasFocus()) {
            val portStr = tv.text.toString()
            try {
                val port = Integer.parseInt(portStr)
                app.trojanConfig.setRemotePort(port)
            } catch (e: NumberFormatException) {
                // Ignore when we get invalid number
                e.printStackTrace()
            }
        }
        endUpdates()
    }
}
