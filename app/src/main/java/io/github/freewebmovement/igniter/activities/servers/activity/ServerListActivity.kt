package io.github.freewebmovement.igniter.activities.servers.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.github.freewebmovement.igniter.R
import io.github.freewebmovement.igniter.activities.servers.data.ServerListDataManager
import io.github.freewebmovement.igniter.activities.servers.fragment.ServerListFragment
import io.github.freewebmovement.igniter.activities.servers.presenter.ServerListPresenter

class ServerListActivity : AppCompatActivity() {
    companion object {
        const val KEY_TROJAN_CONFIG = "trojan_config"
    }

    lateinit var fragment: ServerListFragment
    private val mGetAddBack: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = intent
            finish()
            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server_list)

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener {
            val intent = Intent(this, AddServerActivity::class.java)
            mGetAddBack.launch(intent)
        }
        initView()
    }

    fun initView() {
        val fm: FragmentManager = supportFragmentManager
        fragment = fm.findFragmentByTag(ServerListFragment.TAG) as? ServerListFragment
            ?: ServerListFragment.newInstance()
        ServerListPresenter(fragment, ServerListDataManager())
        fm.beginTransaction()
            .replace(R.id.parent_fl, fragment, ServerListFragment.TAG)
            .commitAllowingStateLoss()
    }
}
