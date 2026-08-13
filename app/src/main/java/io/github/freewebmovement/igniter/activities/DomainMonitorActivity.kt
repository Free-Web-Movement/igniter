package io.github.freewebmovement.igniter.activities

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager

/**
 * Thin wrapper keeping the standalone entry point alive; the actual UI lives
 * in [RulesFragment] which is also used by the main tab shell.
 */
class DomainMonitorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = FrameLayout(this)
        container.id = View.generateViewId()
        setContentView(container)
        val fm: FragmentManager = supportFragmentManager
        val fragment = fm.findFragmentByTag("rules") as? RulesFragment
            ?: RulesFragment.newInstance()
        fm.beginTransaction()
            .replace(container.id, fragment, "rules")
            .commitAllowingStateLoss()
    }
}
