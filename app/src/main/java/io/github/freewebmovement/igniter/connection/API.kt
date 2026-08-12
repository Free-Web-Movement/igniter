package io.github.freewebmovement.igniter.connection

import com.google.gson.Gson
import io.github.freewebmovement.igniter.constants.API as APIConstants
import io.github.freewebmovement.igniter.models.Server
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
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

    fun quota(username: String, password: String): String {
        val formBody: RequestBody = FormBody.Builder()
            .add(APIConstants.API_QUOTA_KEY_USERNAME, username)
            .add(APIConstants.API_QUOTA_KEY_PASSWORD, password)
            .build()

        val request = Request.Builder()
            .url(APIConstants.BASE_URL + APIConstants.API_QUOTA_PATH)
            .post(formBody)
            .build()

        val call = httpClient.newCall(request)
        return try {
            val response = call.execute()
            assert(response.body != null)
            response.body!!.string()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
}
