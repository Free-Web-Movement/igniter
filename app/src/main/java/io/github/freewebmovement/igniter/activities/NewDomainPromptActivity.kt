package io.github.freewebmovement.igniter.activities

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.github.freewebmovement.igniter.IgniterApplication
import io.github.freewebmovement.igniter.R
import io.github.freewebmovement.igniter.connection.Socks5Gate
import io.github.freewebmovement.igniter.persistence.DomainRulesManager

/**
 * Full-screen choice page for new domains discovered in the tunnel.
 *
 * Runs in the main process. It lists the domains the [Socks5Gate] (in the
 * ":proxy" process) parked, and each one gets a 使用代理 / 直连 button. Picking
 * one saves the policy as a manual rule and removes the domain from the pending
 * list; the gate polls the rules every few hundred ms, finds the new rule and
 * wakes the held connection. When the list empties the page closes itself.
 *
 * If the page is dismissed without choosing, the connections stay held and the
 * persistent pending notification (PENDING_NOTIFY_ID) remains so the user can
 * come back and choose later.
 */
class NewDomainPromptActivity : AppCompatActivity() {

    private val app: IgniterApplication
        get() = IgniterApplication.getApplication()

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var listContainer: LinearLayout
    private lateinit var emptyView: TextView
    private var refreshing = false

    private val refreshTask = object : Runnable {
        override fun run() {
            if (refreshing) {
                render()
                handler.postDelayed(this, REFRESH_INTERVAL_MS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(16), dp(16), dp(16), dp(16))
        root.setBackgroundColor(0xFFFFFFFF.toInt())

        val title = TextView(this)
        title.text = getString(R.string.domain_prompt_screen_title)
        title.setTextColor(0xFF008577.toInt())
        title.textSize = 18f
        title.setTypeface(title.typeface, Typeface.BOLD)
        root.addView(title, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT))

        val hint = TextView(this)
        hint.text = getString(R.string.domain_prompt_screen_hint)
        hint.setTextColor(0xFF757575.toInt())
        hint.textSize = 13f
        val hintLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)
        hintLp.topMargin = dp(6)
        root.addView(hint, hintLp)

        val scrollContent = LinearLayout(this)
        scrollContent.orientation = LinearLayout.VERTICAL

        listContainer = LinearLayout(this)
        listContainer.orientation = LinearLayout.VERTICAL
        scrollContent.addView(listContainer, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT))

        emptyView = TextView(this)
        emptyView.text = getString(R.string.domain_prompt_screen_empty)
        emptyView.setTextColor(0xFF757575.toInt())
        emptyView.textSize = 14f
        emptyView.gravity = Gravity.CENTER
        val emptyLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(120))
        emptyLp.topMargin = dp(24)
        scrollContent.addView(emptyView, emptyLp)

        val scroll = ScrollView(this)
        scroll.addView(scrollContent, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT))
        val scrollLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f)
        scrollLp.topMargin = dp(12)
        root.addView(scroll, scrollLp)

        setContentView(root)
        render()
    }

    override fun onResume() {
        super.onResume()
        refreshing = true
        handler.removeCallbacks(refreshTask)
        handler.post(refreshTask)
        render()
    }

    override fun onPause() {
        super.onPause()
        refreshing = false
        handler.removeCallbacks(refreshTask)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        render()
    }

    private fun render() {
        val pending = Socks5Gate.getPending(this)
        if (pending.isEmpty()) {
            listContainer.removeAllViews()
            listContainer.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
            finish()
            return
        }
        listContainer.visibility = View.VISIBLE
        emptyView.visibility = View.GONE
        listContainer.removeAllViews()
        for (domain in pending) {
            listContainer.addView(domainRow(domain))
        }
    }

    private fun domainRow(domain: String): View {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL

        val name = TextView(this)
        name.text = domain
        name.setTextColor(0xFF212121.toInt())
        name.textSize = 15f
        name.setTypeface(name.typeface, Typeface.BOLD)
        row.addView(name, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        row.addView(policyButton(domain, DomainRulesManager.POLICY_PROXY))
        row.addView(spacer(dp(8)))
        row.addView(policyButton(domain, DomainRulesManager.POLICY_DIRECT))

        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.topMargin = dp(10)
        row.layoutParams = lp
        return row
    }

    private fun policyButton(domain: String, policy: String): View {
        val button = TextView(this)
        button.text = getString(if (policy == DomainRulesManager.POLICY_PROXY) {
            R.string.domain_prompt_proxy
        } else {
            R.string.domain_prompt_direct
        })
        button.setBackgroundResource(R.drawable.bg_domain_tab)
        button.setTextColor(0xFF212121.toInt())
        button.gravity = Gravity.CENTER
        button.textSize = 14f
        button.setPadding(dp(16), dp(8), dp(16), dp(8))
        button.isClickable = true
        button.isFocusable = true
        button.setOnClickListener {
            choose(domain, policy)
        }
        return button
    }

    private fun choose(domain: String, policy: String) {
        DomainRulesManager(this).setRule(domain, policy)
        Socks5Gate.removePending(this, domain)
        render()
    }

    private fun spacer(width: Int): View {
        val space = View(this)
        space.layoutParams = LinearLayout.LayoutParams(width, 1)
        return space
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val REFRESH_INTERVAL_MS = 1000L
    }
}
