package io.github.freewebmovement.igniter.persistence

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PathTest {
    private val appContext: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun shouldGetServer() {
        val path = Path(appContext)
        assert(path.get(0, "dodod") != null)
        assert(path.get(1, "dodod") != null)
        assert(path.get(2, "dodod") == null)
        assert(path.systemApps != null)
        assert(path.caCert != null)
        assert(path.clashConfig != null)
        assert(path.dirs.size == 2)
        assert(path.trojanConfig != null)
        assert(path.exemptedAppList != null)
        assert(path.countryMmdb != null)
    }
}
