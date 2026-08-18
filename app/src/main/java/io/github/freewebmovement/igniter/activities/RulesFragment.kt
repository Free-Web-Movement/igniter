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
 * Rules page with four tabs based on connection type:
 *  * 代理 (proxy) - domains going through the proxy tunnel;
 *  * 直连 (direct) - domains connecting directly;
 *  * 阻止 (blocked) - domains blocked (e.g. ad blocking);
 *  * 不可达 (unreachable) - domains where both paths failed.
 *
 * Within each tab, domains are grouped by category:
 *  * 国内 (domestic) - Chinese/domestic sites;
 *  * 国外 (foreign) - foreign sites;
 *  * 隐私 (privacy) - hidden/private domains;
 *  * 大厂 (major) - major foreign companies.
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
                        onSetBlock = { domain ->
                            mRules.addBlockedDomain(domain)
                            rebuildList()
                        },
                        onUnlock = { domain ->
                            mRules.removeRule(domain)
                            rebuildList()
                        },
                        onHideDomain = { domain ->
                            mRules.addPrivateDomain(domain)
                            rebuildList()
                        },
                        onUnhideDomain = { domain ->
                            mRules.removePrivateDomain(domain)
                            rebuildList()
                        },
                        onDeleteDomain = { domain ->
                            when (mTab) {
                                TAB_BLOCKED -> mRules.removeBlockedDomain(domain)
                                TAB_UNREACHABLE -> Socks5Gate.removeUnreachable(requireContext(), domain)
                                else -> {
                                    mRules.removeRule(domain)
                                    mRules.removeBlockedDomain(domain)
                                    mEntries.remove(rootDomain(domain))
                                }
                            }
                            rebuildList()
                        },
                        onAddDomain = { showAddDialog() },
                        onClearDomains = {
                            when (mTab) {
                                TAB_BLOCKED -> mRules.clearBlockedDomains()
                                TAB_UNREACHABLE -> Socks5Gate.clearUnreachable(requireContext())
                                else -> mEntries.clear()
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
                            if (mTab == TAB_PROXY || mTab == TAB_DIRECT) {
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
        val allDomains = collectAllDomains()
        val filtered = allDomains.filter { mFilter.isEmpty() || it.domain.contains(mFilter) }
        val connectionFiltered = filtered.filter { it.connectionType == mTab }
        mEmptyHint = getString(
            when (mTab) {
                TAB_PROXY -> R.string.domain_monitor_empty_proxy
                TAB_DIRECT -> R.string.domain_monitor_empty_direct
                TAB_BLOCKED -> R.string.domain_monitor_empty_blocked
                else -> R.string.domain_monitor_empty_unreachable
            })
        val grouped = connectionFiltered.groupBy { it.category }
        val result = mutableListOf<RuleItem>()
        for (cat in listOf(CAT_DOMESTIC, CAT_FOREIGN, CAT_PRIVACY, CAT_MAJOR)) {
            val items = grouped[cat] ?: continue
            if (items.isEmpty()) continue
            result.add(RuleItem(
                domain = "── ${categoryLabel(cat)} ──",
                statusText = "",
                statusColor = COLOR_DEFAULT,
                policy = null,
                showUnlock = false,
                showDelete = false,
                isGroupHeader = true
            ))
            for (item in items.sortedBy { it.domain }) {
                result.add(buildRuleItem(item))
            }
        }
        mRuleItems = result
    }

    private fun categoryLabel(cat: String): String = when (cat) {
        CAT_DOMESTIC -> getString(R.string.domain_monitor_group_domestic)
        CAT_FOREIGN -> getString(R.string.domain_monitor_group_foreign)
        CAT_PRIVACY -> getString(R.string.domain_monitor_group_privacy)
        CAT_MAJOR -> getString(R.string.domain_monitor_group_major)
        else -> cat
    }

    private class ClassifiedDomain(
        val domain: String,
        val connectionType: Int,
        val category: String,
        val policy: String?,
        val count: Int = 0
    )

    private fun collectAllDomains(): List<ClassifiedDomain> {
        val result = mutableListOf<ClassifiedDomain>()

        // 1. Auto-detected domains from logcat
        for (e in mEntries.values) {
            val domain = e.domain
            val clashPolicy = e.clashPolicy
            val locked = mRules.getPolicy(domain)
            val isBlocked = mRules.isBlockedDomain(domain)
            val isUnreachable = Socks5Gate.getUnreachable(requireContext()).contains(domain)
            val isPrivate = mRules.isPrivateDomain(domain)

            val connType = when {
                isUnreachable -> TAB_UNREACHABLE
                isBlocked -> TAB_BLOCKED
                locked == DomainRulesManager.POLICY_PROXY -> TAB_PROXY
                locked == DomainRulesManager.POLICY_DIRECT -> TAB_DIRECT
                clashPolicy != null && clashPolicy.contains("PROXY", ignoreCase = true) -> TAB_PROXY
                clashPolicy != null && clashPolicy.contains("DIRECT", ignoreCase = true) -> TAB_DIRECT
                else -> TAB_DIRECT
            }
            val cat = classifyDomain(domain, isPrivate)
            result.add(ClassifiedDomain(domain, connType, cat, locked ?: clashPolicy, e.count))
        }

        // 2. Manual rules not yet seen in logcat
        for ((domain, policy) in mRules.getRules()) {
            if (mEntries.containsKey(domain)) continue
            val isPrivate = mRules.isPrivateDomain(domain)
            val isBlocked = mRules.isBlockedDomain(domain)
            val connType = when {
                isBlocked -> TAB_BLOCKED
                policy == DomainRulesManager.POLICY_PROXY -> TAB_PROXY
                else -> TAB_DIRECT
            }
            val cat = classifyDomain(domain, isPrivate)
            result.add(ClassifiedDomain(domain, connType, cat, policy))
        }

        // 3. Foreign company domains not yet seen
        for ((company, domains) in mRules.getMajorForeignCompanies()) {
            val companyDefault = mRules.getCompanyDefaultPolicy(company)
            for (site in domains) {
                if (mEntries.containsKey(site) || mRules.getPolicy(site) != null) continue
                val isPrivate = mRules.isPrivateDomain(site)
                val connType = when (companyDefault) {
                    DomainRulesManager.POLICY_PROXY -> TAB_PROXY
                    else -> TAB_DIRECT
                }
                val cat = if (isPrivate) CAT_PRIVACY else CAT_MAJOR
                result.add(ClassifiedDomain(site, connType, cat, companyDefault))
            }
        }

        // 4. Blocked domains not yet seen
        for (domain in mRules.getBlockedDomains()) {
            if (result.any { it.domain == domain }) continue
            result.add(ClassifiedDomain(domain, TAB_BLOCKED, classifyDomain(domain, false), "BLOCKED"))
        }

        // 5. Unreachable domains not yet seen
        for (domain in Socks5Gate.getUnreachable(requireContext())) {
            if (result.any { it.domain == domain }) continue
            result.add(ClassifiedDomain(domain, TAB_UNREACHABLE, classifyDomain(domain, false), "UNREACHABLE"))
        }

        // 6. Private domains not yet seen
        for (domain in mRules.getPrivateDomains()) {
            if (result.any { it.domain == domain }) continue
            result.add(ClassifiedDomain(domain, TAB_DIRECT, CAT_PRIVACY, DomainRulesManager.POLICY_DIRECT))
        }

        return result
    }

    private fun classifyDomain(domain: String, isPrivate: Boolean): String {
        if (isPrivate) return CAT_PRIVACY
        if (mRules.lookupCompany(domain) != null) return CAT_MAJOR
        if (isDomesticDomain(domain)) return CAT_DOMESTIC
        return CAT_FOREIGN
    }

    private fun isDomesticDomain(domain: String): Boolean {
        if (domain.isEmpty()) return false
        val tlds = listOf(
            "cn", "com.cn", "net.cn", "org.cn", "gov.cn", "edu.cn",
            "com.hk", "com.tw", "org.tw", "com.sg", "com.my",
            "com.au", "net.au", "co.jp", "or.jp", "co.kr", "or.kr",
            "co.nz", "com.br", "com.mx", "com.tr", "com.ar",
            "com.vn", "com.ph", "com.th", "co.id", "com.id", "co.in"
        )
        val labels = domain.split('.')
        if (labels.size < 2) return false
        val tld = labels.takeLast(2).joinToString(".")
        if (tld in tlds) return true
        if (labels.size >= 3) {
            val compound = labels.takeLast(3).joinToString(".")
            if (compound in tlds) return true
        }
        return false
    }

    private fun buildRuleItem(e: ClassifiedDomain): RuleItem {
        val domain = e.domain
        val policy = e.policy
        val statusText: String
        val statusColor: Color
        if (e.connectionType == TAB_BLOCKED) {
            statusText = "Blocked"
            statusColor = COLOR_DEFAULT
        } else if (e.connectionType == TAB_UNREACHABLE) {
            statusText = getString(R.string.domain_monitor_unreachable)
            statusColor = COLOR_DEFAULT
        } else if (policy != null) {
            statusText = policy
            statusColor = if (policy.contains("DIRECT", ignoreCase = true)) COLOR_DIRECT else COLOR_PROXY
        } else {
            statusText = "${e.count} hits"
            statusColor = COLOR_DEFAULT
        }
        return RuleItem(
            domain = domain,
            statusText = statusText,
            statusColor = statusColor,
            policy = policy,
            showUnlock = mRules.getPolicy(domain) != null,
            showHide = !mRules.isPrivateDomain(domain) && e.connectionType != TAB_BLOCKED,
            showDelete = true,
            showBlock = e.connectionType != TAB_BLOCKED && e.connectionType != TAB_UNREACHABLE
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
        switchTab(TAB_PROXY)
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

        private const val TAB_PROXY = 0
        private const val TAB_DIRECT = 1
        private const val TAB_BLOCKED = 2
        private const val TAB_UNREACHABLE = 3

        private const val CAT_DOMESTIC = "domestic"
        private const val CAT_FOREIGN = "foreign"
        private const val CAT_PRIVACY = "privacy"
        private const val CAT_MAJOR = "major"

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
    private var mTab = TAB_PROXY
    @Volatile
    private var mRunning = true
    private var mPollThread: Thread? = null
}
