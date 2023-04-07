package io.github.freewebmovement.igniter;

public class JNIHelper {
    private static boolean isStarted = false;
    static {
        System.loadLibrary("jni-helper");
    }

    private static native void trojan(String config);

    private static native void stop();

    public static void start(String filename) {
        if (isStarted) {
            return;
        }
        isStarted = true;
        trojan(filename);
    }

    public static void terminate() {
        if (isStarted) {
            stop();
            isStarted = false;
        }
    }
}
