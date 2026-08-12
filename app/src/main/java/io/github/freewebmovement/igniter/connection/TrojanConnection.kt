package io.github.freewebmovement.igniter.connection

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.RemoteException
import androidx.annotation.NonNull
import io.github.freewebmovement.igniter.R
import io.github.freewebmovement.igniter.proxy.aidl.ITrojanService
import io.github.freewebmovement.igniter.proxy.aidl.ITrojanServiceCallback
import io.github.freewebmovement.igniter.services.ProxyService

/**
 * A class that delegates interaction with [ProxyService]. You should call [connect] (Context, Callback)
 * when you are ready for interacting with [ProxyService] and call [disconnect]
 * in the end. [TrojanConnection] would bind [ProxyService] and register [ITrojanServiceCallback].
 * You can easily obtain [ProxyService] and get state change as well as connection test result
 * by implementing [Callback].
 */
class TrojanConnection(private val mListenToDeath: Boolean) : ServiceConnection, IBinder.DeathRecipient {
    private val mHandler = Handler()
    private var mTrojanService: ITrojanService? = null
    private var mCallback: Callback? = null
    private var mServiceCallbackRegistered = false
    private var mAlreadyConnected = false
    private var mBinder: IBinder? = null

    /**
     * Implementation of [ITrojanServiceCallback]. The callback is registered in [onServiceConnected],
     * and unregistered in [onServiceDisconnected]. The callback is considered
     * to be invoked by [ITrojanService], in this case, a field of [ProxyService] implements
     * [ITrojanService].
     */
    private val mTrojanServiceCallback = object : ITrojanServiceCallback.Stub() {
        override fun onStateChanged(state: Int, msg: String) {
            mCallback?.let { callback ->
                mHandler.post { callback.onStateChanged(state, msg) }
            }
        }

        override fun onTestResult(testUrl: String, connected: Boolean, delay: Long, error: String) {
            mCallback?.let { callback ->
                mHandler.post { callback.onTestResult(testUrl, connected, delay, error) }
            }
        }
    }

    /**
     * Callback for events that are relative to [ProxyService].
     */
    interface Callback {
        fun onServiceConnected(service: ITrojanService)

        fun onServiceDisconnected()

        fun onStateChanged(state: Int, msg: String?)

        fun onTestResult(testUrl: String?, connected: Boolean, delay: Long, @NonNull error: String)

        fun onBinderDied()
    }

    fun connect(context: Context, callback: Callback) {
        if (mAlreadyConnected) {
            return
        }
        mAlreadyConnected = true
        if (mCallback != null) {
            throw IllegalStateException("Required to call disconnect(Context) first.")
        }
        mCallback = callback

        // todo: choose the service class dynamically.
        val intent = Intent(context, ProxyService::class.java)
        intent.action = context.getString(R.string.bind_service)
        context.bindService(intent, this, Context.BIND_AUTO_CREATE)
    }

    fun disconnect(context: Context) {
        unregisterServiceCallback()
        if (mAlreadyConnected) {
            try {
                context.unbindService(this)
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            }
            mAlreadyConnected = false
            if (mListenToDeath && mBinder != null) {
                mBinder!!.unlinkToDeath(this, 0)
            }
            mBinder = null
            mTrojanService = null
            mCallback = null
        }
    }

    private fun unregisterServiceCallback() {
        val service = mTrojanService
        if (service != null && mServiceCallbackRegistered) {
            try {
                service.unregisterCallback(mTrojanServiceCallback)
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
            mServiceCallbackRegistered = false
        }
    }

    fun getService(): ITrojanService? {
        return mTrojanService
    }

    /**
     * Obtain the binder [ITrojanService] returned by [ProxyService.onBind] and
     * register callback [mTrojanServiceCallback] with the binder.
     */
    override fun onServiceConnected(name: ComponentName, binder: IBinder) {
        mBinder = binder
        val service = ITrojanService.Stub.asInterface(binder)
        mTrojanService = service
        try {
            if (mListenToDeath) {
                binder.linkToDeath(this, 0)
            }
            if (mServiceCallbackRegistered) {
                throw IllegalStateException("TrojanServiceCallback already registered!")
            }
            service.registerCallback(mTrojanServiceCallback)
            mServiceCallbackRegistered = true
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
        mCallback?.onServiceConnected(service)
    }

    override fun onServiceDisconnected(name: ComponentName) {
        unregisterServiceCallback()
        mCallback?.onServiceDisconnected()
        mTrojanService = null
        mBinder = null
    }

    override fun binderDied() {
        mTrojanService = null
        mServiceCallbackRegistered = false
        mCallback?.let { callback ->
            mHandler.post { callback.onBinderDied() }
        }
    }
}
