package io.github.freewebmovement.igniter.common.dialog

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.app.Activity
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import io.github.freewebmovement.igniter.R

/**
 * In-layout dialog / menu / loading host.
 *
 * Everything is rendered inside the activity's content area, above the bottom
 * navigation bar, so no app window ever covers the bottom tab.
 */
class AppSheet private constructor(private val layer: FrameLayout) {

    private val cardView: View =
        LayoutInflater.from(layer.context).inflate(R.layout.sheet_dialog, layer, false)
    private val titleTv: TextView = cardView.findViewById(R.id.sheetTitle)
    private val messageTv: TextView = cardView.findViewById(R.id.sheetMessage)
    private val contentFrame: FrameLayout = cardView.findViewById(R.id.sheetContent)
    private val neutralBtn: TextView = cardView.findViewById(R.id.sheetNeutral)
    private val negativeBtn: TextView = cardView.findViewById(R.id.sheetNegative)
    private val positiveBtn: TextView = cardView.findViewById(R.id.sheetPositive)

    private var showing = false
    private var loadingView: View? = null
    private var loadingAnimator: ObjectAnimator? = null

    fun setTitle(text: String?): AppSheet {
        titleTv.text = text ?: ""
        titleTv.visibility = if (text.isNullOrEmpty()) View.GONE else View.VISIBLE
        return this
    }

    fun setTitle(resId: Int): AppSheet = setTitle(layer.context.getString(resId))

    fun setMessage(text: String?): AppSheet {
        messageTv.text = text ?: ""
        messageTv.visibility = if (text.isNullOrEmpty()) View.GONE else View.VISIBLE
        return this
    }

    fun setMessage(resId: Int): AppSheet = setMessage(layer.context.getString(resId))

    fun setContent(view: View?): AppSheet {
        contentFrame.removeAllViews()
        if (view != null) {
            contentFrame.addView(view)
        }
        contentFrame.visibility = if (view == null) View.GONE else View.VISIBLE
        return this
    }

    fun setNeutralButton(text: String?, onClick: ((View) -> Unit)? = null): AppSheet {
        configButton(neutralBtn, text, onClick)
        return this
    }

    fun setNeutralButton(resId: Int, onClick: ((View) -> Unit)? = null): AppSheet =
        setNeutralButton(if (resId == 0) null else layer.context.getString(resId), onClick)

    fun setNegativeButton(text: String?, onClick: ((View) -> Unit)? = null): AppSheet {
        configButton(negativeBtn, text, onClick)
        return this
    }

    fun setNegativeButton(resId: Int, onClick: ((View) -> Unit)? = null): AppSheet =
        setNegativeButton(if (resId == 0) null else layer.context.getString(resId), onClick)

    fun setPositiveButton(text: String?, onClick: ((View) -> Unit)? = null): AppSheet {
        configButton(positiveBtn, text, onClick)
        return this
    }

    fun setPositiveButton(resId: Int, onClick: ((View) -> Unit)? = null): AppSheet =
        setPositiveButton(if (resId == 0) null else layer.context.getString(resId), onClick)

    private fun configButton(button: TextView, text: String?, onClick: ((View) -> Unit)?) {
        if (text.isNullOrEmpty()) {
            button.visibility = View.GONE
            return
        }
        button.text = text
        button.visibility = View.VISIBLE
        button.setOnClickListener {
            onClick?.invoke(it)
            dismiss()
        }
    }

    fun show() {
        showView(cardView)
    }

    fun showMenu(items: List<Pair<String, () -> Unit>>) {
        val menuView = LayoutInflater.from(layer.context)
            .inflate(R.layout.sheet_menu, layer, false)
        val list = menuView.findViewById<LinearLayout>(R.id.sheetMenuList)
        for ((label, action) in items) {
            val row = LayoutInflater.from(layer.context)
                .inflate(R.layout.item_sheet_menu, list, false) as TextView
            row.text = label
            row.setOnClickListener {
                dismiss()
                action()
            }
            list.addView(row)
        }
        showView(menuView)
    }

    fun showLoading(msg: String) {
        loadingAnimator?.cancel()
        val loading = LayoutInflater.from(layer.context)
            .inflate(R.layout.dialog_loading, layer, false)
        loading.findViewById<TextView>(R.id.dialogLoadingMsgTv).text = msg
        loadingView = loading
        loadingAnimator = ObjectAnimator.ofInt(
            loading.findViewById<ProgressBar>(R.id.dialogLoadingPb),
            "progress",
            0,
            100
        ).apply {
            duration = 800
            interpolator = LinearInterpolator()
            start()
        }
        layer.removeAllViews()
        layer.addView(
            loading,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        )
        showLayer(false)
    }

    private fun showView(view: View) {
        layer.removeAllViews()
        layer.addView(view)
        showLayer(true)
    }

    private fun showLayer(dismissOnOutsideTap: Boolean) {
        layer.visibility = View.VISIBLE
        showing = true
        active = this
        if (dismissOnOutsideTap) {
            layer.setOnClickListener { dismiss() }
        } else {
            layer.setOnClickListener(null)
        }
    }

    fun dismiss() {
        val animator = loadingAnimator
        val view = loadingView
        if (animator != null && animator.isRunning && view != null && view.isAttachedToWindow) {
            animator.removeAllListeners()
            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (layer.getChildAt(0) === view) doDismiss()
                }
            })
            return
        }
        doDismiss()
    }

    private fun doDismiss() {
        loadingAnimator = null
        loadingView = null
        showing = false
        if (active === this) {
            active = null
        }
        layer.removeAllViews()
        layer.visibility = View.GONE
    }

    companion object {
        private var active: AppSheet? = null

        @JvmStatic
        fun builder(activity: Activity): AppSheet {
            val layer = activity.findViewById<FrameLayout>(R.id.appDialogLayer)
                ?: throw IllegalStateException("appDialogLayer is missing in the activity layout")
            return AppSheet(layer)
        }

        @JvmStatic
        fun builder(fragment: Fragment): AppSheet = builder(fragment.requireActivity())

        @JvmStatic
        fun isShowing(): Boolean = active?.showing == true

        @JvmStatic
        fun dismissActive() {
            active?.dismiss()
        }
    }
}
