package io.github.freewebmovement.igniter.common.os

import java.util.concurrent.Executor

/**
 * Interface of thread pool. Provides methods to run [Task] or [Runnable] in main thread
 * or in background.
 */
interface IThreads {
    val threadPoolExecutor: Executor

    fun runOnUiThread(runnable: Runnable)

    fun runOnUiThread(runnable: Runnable, delayMillis: Long)

    fun runOnWorkThread(task: Task)

    fun runOnWorkThread(task: Task, delayMillis: Long)

    fun removeDelayedAction(action: Runnable)
}
