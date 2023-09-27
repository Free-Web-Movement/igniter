package io.github.freewebmovement.igniter.ui.component.textview.listener;

import android.widget.TextView;

import io.github.freewebmovement.igniter.IgniterApplication;
import io.github.freewebmovement.igniter.persistence.TrojanConfig;

public class  RemoteAddress extends TextViewListener {
    public RemoteAddress(TextView tv, IgniterApplication app) {
        super(tv, app);
    }

    @Override
    protected void onTextChanged(String before, String old, String aNew, String after) {
        // update TextView
        startUpdates(); // to prevent infinite loop.
        if (tv.hasFocus()) {
            app.trojanConfig.setRemoteAddr(tv.getText().toString());
        }
        endUpdates();
    }
};