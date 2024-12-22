package io.github.freewebmovement.igniter

object JNIHelper {
    private var isStarted = false

    init {
        System.loadLibrary("jni-helper")
    }

    private external fun trojan(config: String)

    private external fun stop()

    @JvmStatic
    fun start(filename: String) {
        if (isStarted) {
            return
        }
        isStarted = true
        trojan(filename)
    }

    @JvmStatic
    fun terminate() {
        if (isStarted) {
            stop()
            isStarted = false
        }
    }
}
