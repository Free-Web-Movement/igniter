package io.github.freewebmovement.igniter.common.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ContentLoadingProgressBar;
import android.view.Window;
import android.widget.TextView;

import io.github.freewebmovement.igniter.R;

public class LoadingDialog extends Dialog {
    private TextView mMsgTv;

    public LoadingDialog(@NonNull Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        Window window =getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        setContentView(R.layout.dialog_loading);
        ContentLoadingProgressBar pb = findViewById(R.id.dialogLoadingPb);
        pb.getIndeterminateDrawable().setColorFilter(ContextCompat.getColor(context, R.color.colorPrimary),
                PorterDuff.Mode.MULTIPLY);
        mMsgTv = findViewById(R.id.dialogLoadingMsgTv);
    }

    public void setMsg(String msg) {
        mMsgTv.setText(msg);
    }
}
