package io.github.freewebmovement.igniter.common;

import android.content.Intent;
import android.os.Build;

public class UnifyVersions {

    public static <T> T getParcel(Intent intent, String key, Class<T> tClass) {
        T temp;
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            temp = intent.getParcelableExtra(key, tClass);
        } else {
            temp = intent.getParcelableExtra(key);
        }
        return temp;
    }
}
