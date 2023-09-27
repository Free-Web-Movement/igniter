package io.github.freewebmovement.igniter.ui.component.textview.listener;

import android.widget.TextView;

import io.github.freewebmovement.igniter.IgniterApplication;
import io.github.freewebmovement.igniter.persistence.TrojanConfig;

public class RemotePort extends TextViewListener {
    public RemotePort(TextView tv, IgniterApplication app) {
        super(tv, app);
    }

    protected void onTextChanged(String before, String old, String aNew, String after) {
        // update TextView
        if (tv.hasFocus()) {
            String portStr = tv.getText().toString();
            try {
                int port = Integer.parseInt(portStr);
                app.trojanConfig.setRemotePort(port);
            } catch (NumberFormatException e) {
                // Ignore when we get invalid number
                e.printStackTrace();
            }
        }
        endUpdates();
    }
}
