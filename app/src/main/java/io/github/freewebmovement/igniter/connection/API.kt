package io.github.freewebmovement.igniter.connection

import com.google.gson.Gson
import io.github.freewebmovement.igniter.constants.API as APIConstants
import io.github.freewebmovement.igniter.models.Server
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class API {
    private val httpClient = OkHttpClient()

    fun server(): Array<Server> {
        val request = Request.Builder()
            .url(APIConstants.SERVER_LIST_URI)
            .build()

        val call = httpClient.newCall(request)
        return try {
            val response = call.execute()
            assert(response.body != null)
            val body = response.body!!.string()
            val gson = Gson()
            gson.fromJson(body, Array<Server>::class.java)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
}
