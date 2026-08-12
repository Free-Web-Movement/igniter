package io.github.freewebmovement.igniter

import androidx.test.runner.AndroidJUnit4
import io.github.freewebmovement.igniter.connection.API
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class APITest {

    @Test
    fun shouldGetServer() {
        val api = API()
        val servers = api.server()
        assert(servers.size >= 1)
    }

    @Test
    fun shouldGetQuota() {
        val username = "sammy"
        val password = "1234"
        val api = API()
        val quotaStr = api.quota(username, password)
        val quotaJSON = JSONObject(quotaStr)
        assert(quotaJSON.getString("username") == username)
        assert(quotaJSON.getInt("quota") == 0)
        assert(quotaJSON.getInt("upload") == 0)
        assert(quotaJSON.getInt("download") == 0)
    }
}
