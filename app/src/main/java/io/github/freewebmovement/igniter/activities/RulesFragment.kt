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
import io.github.freewebmovement.igniter.persistence.DomainRulesManager
import io.github.freewebmovement.igniter.theme.IgniterTheme
import io.github.freewebmovement.igniter.ui.rules.RuleItem
import io.github.freewebmovement.igniter.ui.rules.RulesScreen
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.ArrayDeque
import java.util.LinkedHashMap
import java.util.Locale
import java.util.regex.Pattern

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
                            mEntries.clear()
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
                val output = readLogcat()
                if (output.isNotEmpty()) {
                    val fresh = filterNewLines(output)
                    if (fresh.isNotEmpty()) {
                        val hits = parseHits(fresh)
                        mHandler.post {
                            if (!mRunning || !isAdded) {
                                return@post
                            }
                            for (hit in hits) {
                                val key = hit[0] ?: continue
                                var e = mEntries[key]
                                if (e == null) {
                                    e = Entry()
                                    e.domain = key
                                    e.isIp = isIpLike(key)
                                    mEntries[key] = e
                                }
                                e.count++
                                if (hit[1] != null) {
                                    e.clashPolicy = hit[1]
                                }
                            }
                            if (mTab == TAB_AUTO) {
                                rebuildList()
                            }
                        }
                    }
                }
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

    private fun readLogcat(): String {
        val p = Runtime.getRuntime().exec(
            arrayOf("logcat", "-d", "-t", "1000", "-s", "GoLog:V", "*:S"))
        val sb = StringBuilder(16384)
        BufferedReader(InputStreamReader(p.inputStream)).use { r ->
            val buf = CharArray(8192)
            while (true) {
                val n = r.read(buf)
                if (n == -1) break
                sb.append(buf, 0, n)
            }
        }
        p.waitFor()
        return sb.toString()
    }

    private fun isIpLike(s: String): Boolean {
        if ("N/A" == s) {
            return true
        }
        return Regex("[0-9a-fA-F:.]+").matches(s)
    }

    private fun cleanPolicy(p: String?): String? {
        if (p == null) {
            return null
        }
        val i = p.indexOf('[')
        return if (i > 0) p.substring(0, i) else p
    }

    private fun parseHits(log: String): List<Array<String?>> {
        val hits = mutableListOf<Array<String?>>()
        for (p in listOf(TUN2SOCKS_TCP, TUN2SOCKS_UDP)) {
            val m = p.matcher(log)
            while (m.find()) {
                hits.add(arrayOf(m.group(1)?.trim()?.lowercase(Locale.US), null))
            }
        }
        for (p in listOf(CLASH_TCP, CLASH_UDP)) {
            val m = p.matcher(log)
            while (m.find()) {
                hits.add(arrayOf(m.group(1)?.trim()?.lowercase(Locale.US), cleanPolicy(m.group(2))))
            }
        }
        return hits
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
        val d = domain.trim().lowercase(Locale.US)
        if (d.isEmpty()) {
            return
        }
        mRules.setRule(d, policy)
        switchTab(TAB_MANUAL)
    }

    companion object {
        private const val POLL_INTERVAL_MS = 1500L

        // logcat -d re-reads the same tail of the buffer on every poll, so raw
        // lines must be deduplicated or every hit would be recounted each time.
        private const val SEEN_LINES_CAPACITY = 2000

        private const val TAB_MANUAL = 0
        private const val TAB_AUTO = 1
        private const val TAB_FOREIGN = 2

        private val COLOR_DIRECT = Color(0xFF2E7D32)
        private val COLOR_PROXY = Color(0xFFC62828)
        private val COLOR_DEFAULT = Color(0xFF757575)

        // go-tun2socks format: [tun2socks] [proxy] [tcp] [N/A] www.google.com:443
        // domain sits right after the bracket (bracket content is always "N/A")
        private val TUN2SOCKS_TCP = Pattern.compile(
            "\\[tun2socks\\] \\[proxy\\] \\[tcp\\] \\[[^\\]]*\\]\\s+([A-Za-z0-9_.-]+):\\d+")
        private val TUN2SOCKS_UDP = Pattern.compile(
            "\\[tun2socks\\] \\[proxy\\] \\[udp\\] \\[[^\\]]*\\]\\s+([A-Za-z0-9_.-]+):\\d+")
        // Clash rule log lines: [TCP] 127.0.0.1:59270 --> cdnws.api.huya.com:443 match DomainSuffix(huya.com) using DIRECT
        private val CLASH_TCP = Pattern.compile(
            "\\[TCP\\]\\s+[0-9a-fA-F.:]+\\s*-->\\s*([A-Za-z0-9_.-]+):\\d+\\s+match\\s+.*using\\s+(\\S+)")
        private val CLASH_UDP = Pattern.compile(
            "\\[UDP\\]\\s+[0-9a-fA-F.:]+\\s*-->\\s*([A-Za-z0-9_.-]+):\\d+\\s+match\\s+.*using\\s+(\\S+)")

        @JvmStatic
        fun newInstance(): RulesFragment = RulesFragment()
    }

    private val mHandler = Handler(Looper.getMainLooper())
    private val mEntries = LinkedHashMap<String, Entry>()
    // Tracks raw logcat lines already counted so a line is never counted twice.
    private val mSeenOrder = ArrayDeque<String>()
    private val mSeenSet = HashSet<String>()

    private lateinit var mRules: DomainRulesManager
    private var mFilter = ""
    private var mTab = TAB_MANUAL
    @Volatile
    private var mRunning = true
    private var mPollThread: Thread? = null
}
