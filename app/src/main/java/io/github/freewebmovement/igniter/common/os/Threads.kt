package io.github.freewebmovement.igniter.common.os

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Singleton implementation of [IThreads]. Call [instance] to get the instance.
 */
object Threads : IThreads {
    private val mThreadPool: ExecutorService = ThreadPoolExecutor(
        0, Int.MAX_VALUE,
        30L, TimeUnit.SECONDS,
        SynchronousQueue(),
        DefaultThreadFactory(), ThreadPoolExecutor.AbortPolicy()
    )
    private val mHandler = Handler(Looper.getMainLooper())

    /**
     * The default thread factory
     */
    private class DefaultThreadFactory : ThreadFactory {
        private val group: ThreadGroup
        private val threadNumber = AtomicInteger(1)
        private val namePrefix: String

        init {
            val s = System.getSecurityManager()
            group = if (s != null) s.threadGroup else Thread.currentThread().threadGroup
            namePrefix = "ThreadHelperPool-" +
                    poolNumber.getAndIncrement() +
                    "-thread-"
        }

        override fun newThread(r: Runnable): Thread {
            val t = Thread(group, r,
                namePrefix + threadNumber.getAndIncrement(),
                0)
            if (t.isDaemon)
                t.isDaemon = false
            return t
        }

        companion object {
            private val poolNumber = AtomicInteger(1)
        }
    }

    @JvmStatic
    fun instance(): IThreads = this

    override val threadPoolExecutor: Executor
        get() = mThreadPool

    override fun runOnWorkThread(task: Task) {
        mThreadPool.execute(task)
    }

    override fun runOnWorkThread(task: Task, delayMillis: Long) {
        mHandler.postDelayed({ mThreadPool.execute(task) }, delayMillis)
    }

    override fun runOnUiThread(action: Runnable) {
        mHandler.post(action)
    }

    override fun runOnUiThread(action: Runnable, delayMillis: Long) {
        mHandler.postDelayed(action, delayMillis)
    }

    override fun removeDelayedAction(action: Runnable) {
        mHandler.removeCallbacks(action)
    }
}
