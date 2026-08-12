package io.github.freewebmovement.igniter

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.runner.AndroidJUnit4
import io.github.freewebmovement.igniter.persistence.Path
import io.github.freewebmovement.igniter.persistence.Storage
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StorageTest {
    private lateinit var instrumentationContext: Context

    @Before
    fun setup() {
        instrumentationContext = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun shouldInit() {
        val storage = Storage(instrumentationContext)
        val path: Path = storage.path

        val paths = arrayOf(path.caCert, path.countryMmdb, path.clashConfig)

        val ids = intArrayOf(R.raw.cacert, R.raw.country, R.raw.clash_config)

        storage.reset()

        for (i in paths.indices) {
            val filename = paths[i]!!
            val rawString = storage.readRawText(ids[i])
            val content = String(Storage.read(filename)!!, Charsets.UTF_8)
            assertEquals(content, rawString)
        }
    }
}
