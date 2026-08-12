package io.github.freewebmovement.igniter.ui.component.textview.listener

import android.widget.TextView
import io.github.freewebmovement.igniter.IgniterApplication
import io.github.freewebmovement.igniter.persistence.NetWorkConfig

class LocalOrClashPort(tv: TextView, app: IgniterApplication) : TextViewListener(tv, app) {
    override fun onTextChanged(before: String, old: String, aNew: String, after: String) {
        // update TextView
        startUpdates() // to prevent infinite loop.
        if (tv.hasFocus()) {
            val portStr = tv.text.toString()
            val port = Integer.parseInt(portStr)
            NetWorkConfig.setPort(app, port)
        }
        endUpdates()
    }
}
