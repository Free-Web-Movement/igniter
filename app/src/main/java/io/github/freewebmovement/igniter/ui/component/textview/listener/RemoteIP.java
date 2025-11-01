package io.github.freewebmovement.igniter.ui.component.textview.listener;

import android.widget.TextView;

import io.github.freewebmovement.igniter.IgniterApplication;

public class RemoteIP extends TextViewListener {
    public RemoteIP(TextView tv, IgniterApplication app) {
        super(tv, app);
    }

    @Override
    protected void onTextChanged(String before, String old, String aNew, String after) {
        // update TextView
        startUpdates(); // to prevent infinite loop.
        if (tv.hasFocus()) {
            app.trojanConfig.setRemoteIP(tv.getText().toString());
        }
        endUpdates();
    }
};