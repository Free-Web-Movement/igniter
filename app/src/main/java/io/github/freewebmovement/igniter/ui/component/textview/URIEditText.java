package io.github.freewebmovement.igniter.ui.component.textview;

import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import io.github.freewebmovement.igniter.IgniterApplication;
import io.github.freewebmovement.igniter.persistence.TrojanConfig;
import io.github.freewebmovement.igniter.ui.component.textview.listener.TextViewListener;

public class URIEditText {

    EditText et;
    IgniterApplication app;

    public URIEditText(EditText et, IgniterApplication app) {
        this.et = et;
    }

    public void init() {
        et.setOnLongClickListener(v -> {
            et.selectAll();
            return false;
        });

        et.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                et.setInputType(EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
            } else {
                // it seems we don't have to place cursor on the end for Trojan URL
                et.setInputType(EditorInfo.TYPE_CLASS_TEXT);
            }
        });

        et.addTextChangedListener(new TextViewListener(et, app) {
            @Override
            protected void onTextChanged(String before, String old, String aNew, String after) {
                // update TextView
                TrojanConfig parsedConfig = TrojanConfig.fromURIString(before + aNew + after);
                if (parsedConfig != null) {
                    String remoteAddress = parsedConfig.getRemoteAddr();
                    int remotePort = parsedConfig.getRemotePort();
                    String password = parsedConfig.getPassword();

                    app.trojanConfig.setRemoteAddr(remoteAddress);
                    app.trojanConfig.setRemotePort(remotePort);
                    app.trojanConfig.setPassword(password);
                }
            }
        });

//        TextViewListener trojanConfigChangedTextViewListener = new TextViewListener(et, app) {
//            @Override
//            protected void onTextChanged(String before, String old, String aNew, String after) {
//                String str = TrojanConfig.toURIString(app.trojanConfig);
//                if (str != null) {
//                    et.setText(str);
//                }
//            }
//        };
    }
}
