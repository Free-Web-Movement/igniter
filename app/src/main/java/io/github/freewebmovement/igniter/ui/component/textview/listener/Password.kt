package io.github.freewebmovement.igniter.ui.component.textview.listener

import android.widget.TextView
import io.github.freewebmovement.igniter.IgniterApplication

class Password(tv: TextView, app: IgniterApplication) : TextViewListener(tv, app) {
    override fun onTextChanged(before: String, old: String, aNew: String, after: String) {
        // update TextView
        startUpdates() // to prevent infinite loop.
        if (tv.hasFocus()) {
            app.trojanConfig.setPassword(tv.text.toString())
        }
        endUpdates()
    }
}
