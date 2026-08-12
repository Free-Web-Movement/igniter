package io.github.freewebmovement.igniter.persistence

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import io.github.freewebmovement.igniter.R

class Storage(context: Context) {
    companion object {
        const val TAG = "STORAGE"

        @JvmStatic
        fun print(filename: String, tag: String) {
            val result = String(read(filename)!!)
            Log.v(tag, result)
        }

        @JvmStatic
        fun read(filename: String): ByteArray? {
            val file = File(filename)
            if (!file.exists()) {
                return null
            }
            try {
                FileInputStream(file).use { fis ->
                    val length = file.length().toInt()
                    val content = ByteArray(length)
                    var offset = 0
                    while (offset < length) {
                        val read = fis.read(content, offset, length - offset)
                        if (read < 0) {
                            break
                        }
                        offset += read
                    }
                    if (offset < length) {
                        val truncated = ByteArray(offset)
                        System.arraycopy(content, 0, truncated, 0, offset)
                        return truncated
                    }
                    return content
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        @JvmStatic
        fun write(filename: String, bytes: ByteArray) {
            try {
                val file = File(filename)
                FileOutputStream(file).use { fos ->
                    fos.write(bytes)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        @JvmStatic
        fun readJSON(filename: String): JSONObject? {
            return try {
                val jsonStr = String(read(filename)!!)
                JSONObject(jsonStr)
            } catch (e: JSONException) {
                e.printStackTrace()
                null
            }
        }

        @JvmStatic
        fun readLines(filename: String): Array<String>? {
            val records = ArrayList<String>()
            try {
                val file = File(filename)
                if (file.exists()) {
                    val is_ = FileInputStream(file)
                    val reader = BufferedReader(InputStreamReader(is_))
                    var line = reader.readLine()
                    while (line != null) {
                        records.add(line)
                        line = reader.readLine()
                    }
                    return records.toTypedArray()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }
    }

    @JvmField
    val path = Path(context)

    fun reset(filename: String, resId: Int) {
        val file = File(filename)
        try {
            if (!file.exists()) {
                file.createNewFile()
            }
            val bytes = readRawBytes(resId)
            write(filename, bytes!!)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating file: $filename")
            e.printStackTrace()
        }
    }

    fun isExternalWritable(): Boolean {
        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(path.context, permission) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    fun reset() {
        val paths = arrayOf(
            path.caCert,
            path.countryMmdb,
            path.clashConfig
        )
        val ids = intArrayOf(
            R.raw.cacert,
            R.raw.country,
            R.raw.clash_config
        )
        for (i in ids.indices) {
            reset(paths[i]!!, ids[i])
        }
    }

    fun readRawBytes(id: Int): ByteArray? {
        try {
            val res = path.context.resources
            res.openRawResource(id).use { inputStream ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                var len: Int
                while (inputStream.read(buffer).also { len = it } != -1) {
                    output.write(buffer, 0, len)
                }
                return output.toByteArray()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun readRawText(id: Int): String {
        return String(readRawBytes(id)!!)
    }

    fun readRawJSON(id: Int): JSONObject? {
        return try {
            val rawText = readRawText(id)
            JSONObject(rawText)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteConfigs() {
        val paths = arrayOf(
            path.caCert,
            path.countryMmdb,
            path.clashConfig
        )
        for (filename in paths) {
            val file = File(filename!!)
            file.delete()
        }
    }

    fun check() {
        val paths = arrayOf(
            path.caCert,
            path.countryMmdb,
            path.clashConfig,
            path.trojanConfig,
            path.systemApps
        )
        val ids = intArrayOf(
            R.raw.cacert,
            R.raw.country,
            R.raw.clash_config,
            R.raw.config,
            R.raw.system_apps
        )
        for (i in 0 until ids.size - 1) {
            check(paths[i]!!, ids[i])
        }
        // Keep this line before system apps filters can be edited.
        reset(paths[ids.size - 1]!!, ids[ids.size - 1])
    }

    fun check(filename: String, resId: Int) {
        val file = File(filename)
        Log.v(TAG, "Checking file: $filename")
        if (!file.exists()) {
            Log.v(TAG, "File: $filename not found! Resetting...")
            reset(filename, resId)
        }
    }
}
