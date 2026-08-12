package io.github.freewebmovement.igniter.common.os

import android.os.Process
import androidx.annotation.WorkerThread

/**
 * A wrapper of Runnable.
 */
abstract class Task : Runnable {
    private var priority = Process.THREAD_PRIORITY_BACKGROUND + Process.THREAD_PRIORITY_MORE_FAVORABLE

    constructor()

    /**
     * Construct a task with priority.
     *
     * @param priority [Process.THREAD_PRIORITY_BACKGROUND]
     */
    constructor(priority: Int) {
        this.priority = priority
    }

    override fun run() {
        Process.setThreadPriority(priority)
        onRun()
    }

    @WorkerThread
    abstract fun onRun()
}
