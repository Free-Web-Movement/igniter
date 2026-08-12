package io.github.freewebmovement.igniter.common.app

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

abstract class BaseFragment : Fragment() {
    @JvmField
    protected var mRootView: View? = null
    @JvmField
    protected var mContext: Context? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onDetach() {
        super.onDetach()
        mContext = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mRootView = view
    }

    protected fun <T : View> findViewById(@IdRes id: Int): T {
        return mRootView!!.findViewById(id)
    }

    protected fun runOnUiThread(runnable: Runnable) {
        val activity = activity
        if (activity != null) {
            activity.runOnUiThread(runnable)
        }
    }

    protected fun finishActivity() {
        val activity = activity
        if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
            activity.finish()
        }
    }
}
