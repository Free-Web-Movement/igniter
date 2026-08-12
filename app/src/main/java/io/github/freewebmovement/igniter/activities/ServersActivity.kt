package io.github.freewebmovement.igniter.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.github.freewebmovement.igniter.R
import io.github.freewebmovement.igniter.connection.API
import io.github.freewebmovement.igniter.models.Server

class ServersActivity : AppCompatActivity() {

    var servers: Array<Server>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_servers)
    }

    fun fetchJSON() {
        val api = API()
        servers = api.server()
    }
}
