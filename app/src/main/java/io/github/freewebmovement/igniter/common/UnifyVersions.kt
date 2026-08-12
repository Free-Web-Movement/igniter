package io.github.freewebmovement.igniter.common

import android.content.Intent
import android.os.Build
import android.os.Parcelable

object UnifyVersions {

    fun <T : Parcelable> getParcelable(intent: Intent, key: String, tClass: Class<T>): T {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(key, tClass)!!
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Parcelable>(key) as T
        }
    }
}
