package io.github.freewebmovement.igniter.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.freewebmovement.igniter.IgniterApplication
import io.github.freewebmovement.igniter.R
import io.github.freewebmovement.igniter.common.dialog.AppSheet
import io.github.freewebmovement.igniter.databinding.FragmentRulesBinding
import io.github.freewebmovement.igniter.databinding.ItemDomainRuleBinding
import io.github.freewebmovement.igniter.persistence.DomainRulesManager
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

    private var _binding: FragmentRulesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        _binding = FragmentRulesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mRules = DomainRulesManager(IgniterApplication.getApplication())
        setHasOptionsMenu(true)

        binding.domainRuleRv.layoutManager = LinearLayoutManager(context)
        mAdapter = Adapter()
        binding.domainRuleRv.adapter = mAdapter

        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                mFilter = s?.toString()?.lowercase(Locale.US) ?: ""
                rebuildList()
            }
        })

        binding.tabManual.setOnClickListener { switchTab(TAB_MANUAL) }
        binding.tabAuto.setOnClickListener { switchTab(TAB_AUTO) }
        binding.tabForeign.setOnClickListener { switchTab(TAB_FOREIGN) }
        switchTab(TAB_MANUAL)

        mPollThread = Thread({ pollLoop() }, "igniter-domain-monitor")
        mPollThread!!.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mRunning = false
        mPollThread?.interrupt()
        _binding = null
    }

    private fun switchTab(tab: Int) {
        mTab = tab
        binding.tabManual.isSelected = tab == TAB_MANUAL
        binding.tabAuto.isSelected = tab == TAB_AUTO
        binding.tabForeign.isSelected = tab == TAB_FOREIGN
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
        mList.clear()
        val empty: Boolean
        if (mTab == TAB_MANUAL) {
            for ((key, value) in mRules.getRules()) {
                if (mFilter.isNotEmpty() && !key.contains(mFilter)) {
                    continue
                }
                val e = Entry()
                e.domain = key
                e.clashPolicy = value
                mList.add(e)
            }
            empty = mList.isEmpty()
        } else if (mTab == TAB_FOREIGN) {
            for (site in mRules.getMajorForeignSites()) {
                if (mFilter.isNotEmpty() && !site.contains(mFilter)) {
                    continue
                }
                val e = Entry()
                e.domain = site
                e.clashPolicy = mRules.getPolicy(site)
                mList.add(e)
            }
            empty = mList.isEmpty()
        } else {
            for (e in mEntries.values) {
                if (mFilter.isNotEmpty() && !e.domain.contains(mFilter)) {
                    continue
                }
                mList.add(e)
            }
            mList.sortWith(Comparator { a, b ->
                if (a.isIp != b.isIp) {
                    if (a.isIp) 1 else -1
                } else {
                    b.count - a.count
                }
            })
            empty = mList.isEmpty()
        }
        if (binding.emptyHint.visibility == View.VISIBLE || empty) {
            binding.emptyHint.visibility = if (empty) View.VISIBLE else View.GONE
            if (empty) {
                binding.emptyHint.setText(
                    when (mTab) {
                        TAB_MANUAL -> R.string.domain_monitor_empty_manual
                        TAB_FOREIGN -> R.string.domain_monitor_empty_foreign
                        else -> R.string.domain_monitor_empty_auto
                    })
            }
        }
        mAdapter.notifyDataSetChanged()
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_domain_monitor, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add_domain -> {
                showAddDialog()
                true
            }
            R.id.action_clear_domains -> {
                mEntries.clear()
                rebuildList()
                true
            }
            R.id.action_view_clash_editor -> {
                startActivity(Intent(context, ClashFileEditorActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private inner class Adapter : RecyclerView.Adapter<Adapter.VH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val b = ItemDomainRuleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VH(b)
        }

        override fun onBindViewHolder(h: VH, position: Int) {
            val e = mList[position]
            val domain = e.domain
            h.b.domainText.text = domain

            val locked = mRules.getPolicy(domain)
            val policy = locked ?: e.clashPolicy
            if (mTab == TAB_MANUAL) {
                h.b.statusText.text = getString(R.string.domain_monitor_manual_locked,
                    (locked ?: policy).toString())
                h.b.statusText.setTextColor(if ((locked ?: policy).toString().contains("DIRECT"))
                    COLOR_DIRECT else COLOR_PROXY)
            } else if (mTab == TAB_FOREIGN) {
                if (locked != null) {
                    h.b.statusText.text = getString(R.string.domain_monitor_foreign_locked, locked)
                    h.b.statusText.setTextColor(if (locked.contains("DIRECT"))
                        COLOR_DIRECT else COLOR_PROXY)
                } else {
                    h.b.statusText.text = getString(R.string.domain_monitor_foreign_default,
                        DomainRulesManager.POLICY_PROXY)
                    h.b.statusText.setTextColor(COLOR_DEFAULT)
                }
            } else if (locked != null) {
                h.b.statusText.text = getString(R.string.domain_monitor_locked, locked, e.count)
                h.b.statusText.setTextColor(if (locked.contains("DIRECT"))
                    COLOR_DIRECT else COLOR_PROXY)
            } else if (policy != null) {
                h.b.statusText.text = getString(R.string.domain_monitor_auto, policy, e.count)
                h.b.statusText.setTextColor(if (policy.contains("DIRECT"))
                    COLOR_DIRECT else COLOR_PROXY)
            } else {
                h.b.statusText.text = getString(R.string.domain_monitor_unknown, e.count)
                h.b.statusText.setTextColor(COLOR_DEFAULT)
            }
            h.b.btnProxy.setOnClickListener {
                mRules.setRule(domain, DomainRulesManager.POLICY_PROXY)
                rebuildList()
            }
            h.b.btnDirect.setOnClickListener {
                mRules.setRule(domain, DomainRulesManager.POLICY_DIRECT)
                rebuildList()
            }
            val showUnlock = mTab == TAB_MANUAL || locked != null
            h.b.btnUnlock.visibility = if (showUnlock) View.VISIBLE else View.GONE
            h.b.btnUnlock.setOnClickListener {
                mRules.removeRule(domain)
                rebuildList()
            }

            var activePolicy: String? = locked ?: e.clashPolicy
            if (mTab == TAB_FOREIGN && locked == null) {
                activePolicy = DomainRulesManager.POLICY_PROXY
            }
            h.b.btnProxy.isSelected = DomainRulesManager.POLICY_PROXY == activePolicy
            h.b.btnDirect.isSelected = DomainRulesManager.POLICY_DIRECT == activePolicy
            h.b.btnUnlock.isSelected = false
        }

        override fun getItemCount(): Int = mList.size

        inner class VH(val b: ItemDomainRuleBinding) : RecyclerView.ViewHolder(b.root)
    }

    companion object {
        private const val POLL_INTERVAL_MS = 1500L

        // logcat -d re-reads the same tail of the buffer on every poll, so raw
        // lines must be deduplicated or every hit would be recounted each time.
        private const val SEEN_LINES_CAPACITY = 2000

        private const val TAB_MANUAL = 0
        private const val TAB_AUTO = 1
        private const val TAB_FOREIGN = 2

        private val COLOR_DIRECT = 0xFF2E7D32.toInt()
        private val COLOR_PROXY = 0xFFC62828.toInt()
        private val COLOR_DEFAULT = 0xFF757575.toInt()

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
    private val mList = mutableListOf<Entry>()
    // Tracks raw logcat lines already counted so a line is never counted twice.
    private val mSeenOrder = ArrayDeque<String>()
    private val mSeenSet = HashSet<String>()

    private lateinit var mRules: DomainRulesManager
    private lateinit var mAdapter: Adapter
    private var mFilter = ""
    private var mTab = TAB_MANUAL
    @Volatile
    private var mRunning = true
    private var mPollThread: Thread? = null
}
