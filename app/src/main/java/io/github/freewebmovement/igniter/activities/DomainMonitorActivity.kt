package io.github.freewebmovement.igniter.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import io.github.freewebmovement.igniter.R

/**
 * Thin wrapper keeping the standalone entry point alive; the actual UI lives
 * in [RulesFragment] which is also used by the main tab shell.
 */
class DomainMonitorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_domain_monitor)
        val fm: FragmentManager = supportFragmentManager
        val fragment = fm.findFragmentByTag("rules") as? RulesFragment
            ?: RulesFragment.newInstance()
        fm.beginTransaction()
            .replace(R.id.parent_fl, fragment, "rules")
            .commitAllowingStateLoss()
    }
}
