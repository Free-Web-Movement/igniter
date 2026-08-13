package io.github.freewebmovement.igniter.common.dialog

import android.app.Activity
import android.content.Context
import android.view.View
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import io.github.freewebmovement.igniter.R

/**
 * Hosts Compose-rendered AppSheet overlays (dialog / menu / loading).
 *
 * The activity renders the overlay inside its own Compose content, above the
 * bottom navigation bar, so no app window ever covers the bottom tab.
 */
interface AppSheetHost {
    /**
     * Presents [content] as an in-layout overlay. Returns a callback that
     * removes the overlay.
     */
    fun presentSheet(
        content: @Composable () -> Unit,
        dismissOnOutsideTap: Boolean
    ): () -> Unit
}

data class SheetButton(val text: String?, val onClick: (() -> Unit)?)

/**
 * In-layout dialog / menu / loading host backed by Jetpack Compose.
 *
 * The builder API mirrors the legacy XML overlay so existing call sites keep
 * working unchanged.
 */
class AppSheet private constructor(
    private val context: Context,
    private val host: AppSheetHost
) {
    private var title: String? = null
    private var message: String? = null
    private var content: (@Composable () -> Unit)? = null
    private var neutral: SheetButton? = null
    private var negative: SheetButton? = null
    private var positive: SheetButton? = null
    private var showing = false
    private var remove: (() -> Unit)? = null
    private var loadingDone = true
    private var dismissPending = false

    fun setTitle(text: String?): AppSheet {
        title = text
        return this
    }

    fun setTitle(resId: Int): AppSheet = setTitle(if (resId == 0) null else context.getString(resId))

    fun setMessage(text: String?): AppSheet {
        message = text
        return this
    }

    fun setMessage(resId: Int): AppSheet = setMessage(if (resId == 0) null else context.getString(resId))

    fun setContent(view: View): AppSheet {
        content = {
            AndroidView(factory = { view }, modifier = Modifier.fillMaxWidth())
        }
        return this
    }

    fun setContent(composable: @Composable () -> Unit): AppSheet {
        content = composable
        return this
    }

    fun setNeutralButton(text: String?, onClick: (() -> Unit)? = null): AppSheet {
        neutral = SheetButton(text, onClick)
        return this
    }

    fun setNeutralButton(resId: Int, onClick: (() -> Unit)? = null): AppSheet =
        setNeutralButton(if (resId == 0) null else context.getString(resId), onClick)

    fun setNegativeButton(text: String?, onClick: (() -> Unit)? = null): AppSheet {
        negative = SheetButton(text, onClick)
        return this
    }

    fun setNegativeButton(resId: Int, onClick: (() -> Unit)? = null): AppSheet =
        setNegativeButton(if (resId == 0) null else context.getString(resId), onClick)

    fun setPositiveButton(text: String?, onClick: (() -> Unit)? = null): AppSheet {
        positive = SheetButton(text, onClick)
        return this
    }

    fun setPositiveButton(resId: Int, onClick: (() -> Unit)? = null): AppSheet =
        setPositiveButton(if (resId == 0) null else context.getString(resId), onClick)

    fun show() {
        showing = true
        loadingDone = true
        dismissPending = false
        val sheet = this
        remove = host.presentSheet({
            AppSheetDialog(
                title = title,
                message = message,
                content = content,
                neutral = neutral,
                negative = negative,
                positive = positive,
                onButtonClick = { sheet.dismiss() }
            )
        }, dismissOnOutsideTap = true)
        active = this
    }

    fun showMenu(items: List<Pair<String, () -> Unit>>) {
        showing = true
        loadingDone = true
        dismissPending = false
        val sheet = this
        remove = host.presentSheet({
            AppSheetMenu(items = items, onItemClick = { sheet.dismiss() })
        }, dismissOnOutsideTap = true)
        active = this
    }

    fun showLoading(msg: String) {
        showing = true
        loadingDone = false
        dismissPending = false
        val sheet = this
        remove = host.presentSheet({
            AppSheetLoading(
                message = msg,
                onAnimationDone = {
                    sheet.loadingDone = true
                    if (sheet.dismissPending) {
                        sheet.doDismiss()
                    }
                }
            )
        }, dismissOnOutsideTap = false)
        active = this
    }

    fun dismiss() {
        if (!loadingDone) {
            dismissPending = true
            return
        }
        doDismiss()
    }

    private fun doDismiss() {
        remove?.invoke()
        remove = null
        showing = false
        if (active === this) {
            active = null
        }
    }

    companion object {
        private var active: AppSheet? = null

        @JvmStatic
        fun builder(activity: Activity): AppSheet {
            val host = activity as? AppSheetHost
                ?: throw IllegalStateException("Activity must implement AppSheetHost")
            return AppSheet(activity, host)
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
