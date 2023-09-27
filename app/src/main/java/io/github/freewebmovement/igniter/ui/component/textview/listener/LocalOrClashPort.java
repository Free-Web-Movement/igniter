package io.github.freewebmovement.igniter.ui.component.textview.listener;

import android.widget.TextView;

import io.github.freewebmovement.igniter.IgniterApplication;
import io.github.freewebmovement.igniter.persistence.NetWorkConfig;

public class LocalOrClashPort extends TextViewListener {
    public LocalOrClashPort(TextView tv, IgniterApplication app) {
        super(tv, app);
    }

    @Override
    protected void onTextChanged(String before, String old, String aNew, String after) {
        // update TextView
        startUpdates(); // to prevent infinite loop.
        if (tv.hasFocus()) {
            String portStr = tv.getText().toString();
            int port = Integer.parseInt(portStr);
            NetWorkConfig.setPort(app, port);
        }
        endUpdates();
    }
}
