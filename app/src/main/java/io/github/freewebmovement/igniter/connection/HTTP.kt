package io.github.freewebmovement.igniter.connection

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection

class HTTP {
    @Throws(UnsupportedEncodingException::class)
    private fun getPostDataString(params: HashMap<String, String>): String {
        val result = StringBuilder()
        var first = true
        for ((key, value) in params) {
            if (first)
                first = false
            else
                result.append("&")

            result.append(URLEncoder.encode(key, "UTF-8"))
            result.append("=")
            result.append(URLEncoder.encode(value, "UTF-8"))
        }
        return result.toString()
    }

    fun post(requestURL: String, postDataParams: HashMap<String, String>): String {
        var response = ""
        try {
            val url = URL(requestURL)

            val conn = url.openConnection() as HttpURLConnection
            conn.readTimeout = 15000
            conn.connectTimeout = 15000
            conn.requestMethod = "POST"
            conn.doInput = true
            conn.doOutput = true

            val os: OutputStream = conn.outputStream
            val writer = BufferedWriter(
                OutputStreamWriter(os, "UTF-8")
            )
            writer.write(getPostDataString(postDataParams))

            writer.flush()
            writer.close()
            os.close()
            val responseCode = conn.responseCode

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                var line: String?
                val br = BufferedReader(InputStreamReader(conn.inputStream))
                while (br.readLine().also { line = it } != null) {
                    response += line
                }
            } else {
                response = ""
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return response
    }
}
