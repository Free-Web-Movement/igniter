package io.github.freewebmovement.igniter.common.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.ContentLoadingProgressBar
import io.github.freewebmovement.igniter.R

class LoadingDialog(context: Context) : Dialog(context) {
    private var mMsgTv: TextView? = null

    init {
        init(context)
    }

    private fun init(context: Context) {
        val window = window
        if (window != null) {
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        setContentView(R.layout.dialog_loading)
        val pb: ContentLoadingProgressBar = findViewById(R.id.dialogLoadingPb)
        pb.indeterminateDrawable.setColorFilter(ContextCompat.getColor(context, R.color.colorPrimary),
            PorterDuff.Mode.MULTIPLY)
        mMsgTv = findViewById(R.id.dialogLoadingMsgTv)
    }

    fun setMsg(msg: String) {
        mMsgTv?.text = msg
    }
}
