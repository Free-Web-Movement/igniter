package io.github.freewebmovement.igniter.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import io.github.freewebmovement.igniter.IgniterApplication
import io.github.freewebmovement.igniter.R
import io.github.freewebmovement.igniter.common.dialog.AppSheet
import io.github.freewebmovement.igniter.connection.Socks5Gate
import io.github.freewebmovement.igniter.connection.TunnelLogParser
import io.github.freewebmovement.igniter.persistence.DomainRulesManager
import io.github.freewebmovement.igniter.theme.IgniterTheme
import io.github.freewebmovement.igniter.ui.rules.RuleItem
import io.github.freewebmovement.igniter.ui.rules.RulesScreen
import java.util.ArrayDeque
import java.util.Date
import java.util.LinkedHashMap
import java.util.Locale

/**
 * Rules page with three tabs:
 *  * 手动 (manual) - the user's explicit per-domain overrides;
 *  * 自动 (auto) - domains currently going through the tunnel, live;
 *  * 国外大网站 (major foreign websites) - a curated list that defaults to Proxy.
 *
 * Locked domains (manual + curated) are injected into the Clash rules on the
 * next connection.
 */
class RulesFragment : Fragment() {

    private class Entry {
        var domain = ""
        var clashPolicy: String? = null
        var isIp = false
        var count = 0
    }

    private var mRuleItems by mutableStateOf<List<RuleItem>>(emptyList())
    private var mEmptyHint by mutableStateOf("")
    private val mLogParser = TunnelLogParser()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                IgniterTheme {
                    RulesScreen(
                        items = mRuleItems,
                        emptyHint = mEmptyHint,
                        onSearchChange = {
                            mFilter = it.lowercase(Locale.US)
                            rebuildList()
                        },
                        onTabSelected = { switchTab(it) },
                        onSetProxy = { domain ->
                            mRules.setRule(domain, DomainRulesManager.POLICY_PROXY)
                            rebuildList()
                        },
                        onSetDirect = { domain ->
                            mRules.setRule(domain, DomainRulesManager.POLICY_DIRECT)
                            rebuildList()
                        },
                        onUnlock = { domain ->
                            mRules.removeRule(domain)
                            rebuildList()
                        },
                        onAddDomain = { showAddDialog() },
                        onClearDomains = {
                            if (mTab == TAB_UNREACHABLE) {
                                Socks5Gate.clearUnreachable(requireContext())
                            } else {
                                mEntries.clear()
                            }
                            rebuildList()
                        },
                        onOpenClashEditor = {
                            startActivity(Intent(context, ClashFileEditorActivity::class.java))
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mRules = DomainRulesManager(IgniterApplication.getApplication())
        mEmptyHint = getString(R.string.domain_monitor_empty_manual)
        rebuildList()

        mPollThread = Thread({ pollLoop() }, "igniter-domain-monitor")
        mPollThread!!.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mRunning = false
        mPollThread?.interrupt()
    }

    private fun switchTab(tab: Int) {
        mTab = tab
        rebuildList()
    }

    private fun pollLoop() {
        while (mRunning) {
            try {
                val output = mLogParser.readLogcat(mSinceTime)
                if (output.isNotEmpty()) {
                    val fresh = filterNewLines(output)
                    if (fresh.isNotEmpty()) {
                        val hits = mLogParser.parseHits(fresh)
                        mHandler.post {
                            if (!mRunning || !isAdded) {
                                return@post
                            }
                            for (hit in hits) {
                                val full = hit.domain ?: continue
                                val key = rootDomain(full)
                                var e = mEntries[key]
                                if (e == null) {
                                    e = Entry()
                                    e.domain = key
                                    e.isIp = TunnelLogParser.isIpLike(full)
                                    mEntries[key] = e
                                }
                                e.count++
                                if (hit.policy != null) {
                                    e.clashPolicy = hit.policy
                                }
                            }
                            if (mTab == TAB_AUTO) {
                                rebuildList()
                            }
                        }
                    }
                }
                mSinceTime = sSinceFormat.format(Date(System.currentTimeMillis() - SINCE_WINDOW_MS))
            } catch (ignored: Exception) {
            }
            try {
                Thread.sleep(POLL_INTERVAL_MS)
            } catch (e: InterruptedException) {
                break
            }
        }
    }

    /**
     * Returns only the logcat lines that have not been seen yet, recording the
     * returned lines so they are skipped on subsequent polls.
     */
    private fun filterNewLines(output: String): String {
        val sb = StringBuilder(output.length)
        for (line in output.split("\n")) {
            if (line.isEmpty() || mSeenSet.contains(line)) {
                continue
            }
            if (mSeenOrder.size >= SEEN_LINES_CAPACITY) {
                mSeenOrder.pollFirst()?.let { mSeenSet.remove(it) }
            }
            mSeenOrder.addLast(line)
            mSeenSet.add(line)
            sb.append(line).append('\n')
        }
        return sb.toString()
    }

    private fun rebuildList() {
        val entries = mutableListOf<Entry>()
        if (mTab == TAB_MANUAL) {
            for ((key, value) in mRules.getRules()) {
                if (mFilter.isNotEmpty() && !key.contains(mFilter)) {
                    continue
                }
                val e = Entry()
                e.domain = key
                e.clashPolicy = value
                entries.add(e)
            }
        } else if (mTab == TAB_FOREIGN) {
            for (site in mRules.getMajorForeignSites()) {
                if (mFilter.isNotEmpty() && !site.contains(mFilter)) {
                    continue
                }
                val e = Entry()
                e.domain = site
                e.clashPolicy = mRules.getPolicy(site)
                entries.add(e)
            }
        } else if (mTab == TAB_UNREACHABLE) {
            for (site in Socks5Gate.getUnreachable(requireContext())) {
                if (mFilter.isNotEmpty() && !site.contains(mFilter)) {
                    continue
                }
                val e = Entry()
                e.domain = site
                e.clashPolicy = "UNREACHABLE"
                entries.add(e)
            }
        } else {
            for (e in mEntries.values) {
                if (mFilter.isNotEmpty() && !e.domain.contains(mFilter)) {
                    continue
                }
                entries.add(e)
            }
            entries.sortWith(Comparator { a, b ->
                if (a.isIp != b.isIp) {
                    if (a.isIp) 1 else -1
                } else {
                    b.count - a.count
                }
            })
        }
        mEmptyHint = getString(
            when (mTab) {
                TAB_MANUAL -> R.string.domain_monitor_empty_manual
                TAB_FOREIGN -> R.string.domain_monitor_empty_foreign
                TAB_UNREACHABLE -> R.string.domain_monitor_empty_unreachable
                else -> R.string.domain_monitor_empty_auto
            })
        mRuleItems = entries.map { buildRuleItem(it) }
    }

    private fun buildRuleItem(e: Entry): RuleItem {
        val domain = e.domain
        val locked = mRules.getPolicy(domain)
        val policy = locked ?: e.clashPolicy
        val activePolicy: String? = when {
            locked != null -> locked
            mTab == TAB_FOREIGN -> DomainRulesManager.POLICY_PROXY
            else -> policy
        }
        val statusText: String
        val statusColor: Color
        if (mTab == TAB_MANUAL) {
            statusText = getString(R.string.domain_monitor_manual_locked, locked ?: policy ?: "")
            statusColor = if ((locked ?: policy).toString().contains("DIRECT")) COLOR_DIRECT else COLOR_PROXY
        } else if (mTab == TAB_FOREIGN) {
            if (locked != null) {
                statusText = getString(R.string.domain_monitor_foreign_locked, locked)
                statusColor = if (locked.contains("DIRECT")) COLOR_DIRECT else COLOR_PROXY
            } else {
                statusText = getString(R.string.domain_monitor_foreign_default,
                    DomainRulesManager.POLICY_PROXY)
                statusColor = COLOR_DEFAULT
            }
        } else if (locked != null) {
            statusText = getString(R.string.domain_monitor_locked, locked, e.count)
            statusColor = if (locked.contains("DIRECT")) COLOR_DIRECT else COLOR_PROXY
        } else if (policy == "DNS_POLLUTED") {
            statusText = getString(R.string.domain_monitor_dns_polluted, e.count)
            statusColor = COLOR_PROXY
        } else if (policy == "UNREACHABLE") {
            statusText = getString(R.string.domain_monitor_unreachable)
            statusColor = COLOR_DEFAULT
        } else if (policy != null) {
            statusText = getString(R.string.domain_monitor_auto, policy, e.count)
            statusColor = if (policy.contains("DIRECT")) COLOR_DIRECT else COLOR_PROXY
        } else {
            statusText = getString(R.string.domain_monitor_unknown, e.count)
            statusColor = COLOR_DEFAULT
        }
        return RuleItem(
            domain = domain,
            statusText = statusText,
            statusColor = statusColor,
            policy = activePolicy,
            showUnlock = mTab == TAB_MANUAL || locked != null
        )
    }

    private fun showAddDialog() {
        val input = EditText(requireContext())
        input.hint = getString(R.string.domain_monitor_add_hint)
        AppSheet.builder(this)
            .setTitle(R.string.domain_monitor_add_title)
            .setContent(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton(R.string.domain_monitor_add_direct) {
                addDomain(input.text.toString(), DomainRulesManager.POLICY_DIRECT)
            }
            .setPositiveButton(R.string.domain_monitor_add_proxy) {
                addDomain(input.text.toString(), DomainRulesManager.POLICY_PROXY)
            }
            .show()
    }

    private fun addDomain(domain: String, policy: String) {
        val d = extractDomain(domain) ?: return
        mRules.setRule(d, policy)
        switchTab(TAB_MANUAL)
    }

    /**
     * Accepts either a bare domain (returned unchanged) or a URI such as
     * `https://www.example.com:443/path`, `trojan://user@example.com:443` or
     * `example.com:443`, and returns the host/domain part. Returns null when
     * the input has nothing usable as a domain.
     */
    private fun extractDomain(raw: String): String? {
        var s = raw.trim().lowercase(Locale.US)
        if (s.isEmpty()) {
            return null
        }
        val hasScheme = s.contains("://")
        val looksLikeUri = hasScheme || s.contains('/') || s.contains(':') || s.contains('@')
        if (!looksLikeUri) {
            return if (isHostLike(s)) s else null
        }
        if (hasScheme) {
            s = s.substring(s.indexOf("://") + 3)
        }
        val at = s.lastIndexOf('@')
        if (at >= 0) {
            s = s.substring(at + 1)
        }
        val slash = s.indexOf('/')
        if (slash >= 0) {
            s = s.substring(0, slash)
        }
        if (s.startsWith("[") && s.contains(']')) {
            s = s.substring(1, s.indexOf(']'))
        } else {
            val colon = s.lastIndexOf(':')
            if (colon >= 0) {
                s = s.substring(0, colon)
            }
        }
        return if (isHostLike(s)) s else null
    }

    private fun isHostLike(s: String): Boolean {
        if (s.isEmpty() || s.length > 253) {
            return false
        }
        for (c in s) {
            if (!(c.isLetterOrDigit() || c == '.' || c == '-' || c == '_')) {
                return false
            }
        }
        return true
    }

    companion object {
        private const val POLL_INTERVAL_MS = 1500L

        // Rolling -T watermark: each poll asks logcat for lines logged after
        // "now - SINCE_WINDOW_MS"; overlapping lines are deduplicated below.
        private const val SINCE_WINDOW_MS = 4000L
        private val sSinceFormat = java.text.SimpleDateFormat("MM-dd HH:mm:ss.SSS", java.util.Locale.US)

        // logcat -d re-reads the same tail of the buffer on every poll, so raw
        // lines must be deduplicated or every hit would be recounted each time.
        private const val SEEN_LINES_CAPACITY = 2000

        private const val TAB_MANUAL = 0
        private const val TAB_AUTO = 1
        private const val TAB_FOREIGN = 2
        private const val TAB_UNREACHABLE = 3

        private val COLOR_DIRECT = Color(0xFF2E7D32)
        private val COLOR_PROXY = Color(0xFFC62828)
        private val COLOR_DEFAULT = Color(0xFF757575)

        /** Compound public suffixes whose registrable domain spans three labels
         *  (a.b.com.cn stays b.com.cn; a.b.co.uk stays b.co.uk). */
        private val COMPOUND_SUFFIXES = setOf(
            "com.cn", "net.cn", "org.cn", "gov.cn", "edu.cn", "ac.cn",
            "com.hk", "com.tw", "org.tw", "com.sg", "com.my", "com.au",
            "net.au", "co.uk", "org.uk", "ac.uk", "gov.uk", "co.jp",
            "or.jp", "ne.jp", "ac.jp", "co.kr", "or.kr", "co.nz",
            "com.br", "com.mx", "com.tr", "com.ar", "com.vn", "com.ph",
            "com.th", "co.id", "com.id", "co.in"
        )

        /** Collapses subdomains to their registrable domain so a.t.com and
         *  b.t.com both surface as t.com in the auto list. IPs stay untouched. */
        private fun rootDomain(host: String): String {
            if (TunnelLogParser.isIpLike(host)) {
                return host
            }
            val labels = host.split('.')
            if (labels.size <= 2) {
                return host
            }
            val suffix = labels.takeLast(2).joinToString(".")
            return labels.takeLast(if (suffix in COMPOUND_SUFFIXES) 3 else 2).joinToString(".")
        }

        @JvmStatic
        fun newInstance(): RulesFragment = RulesFragment()
    }

    private val mHandler = Handler(Looper.getMainLooper())
    private val mEntries = LinkedHashMap<String, Entry>()
    // Tracks raw logcat lines already counted so a line is never counted twice.
    private var mSinceTime: String? = null
    private val mSeenOrder = ArrayDeque<String>()
    private val mSeenSet = HashSet<String>()

    private lateinit var mRules: DomainRulesManager
    private var mFilter = ""
    private var mTab = TAB_MANUAL
    @Volatile
    private var mRunning = true
    private var mPollThread: Thread? = null
}
