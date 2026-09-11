package io.github.freewebmovement.igniter.persistence

import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import android.util.Log
import io.github.freewebmovement.igniter.IgniterApplication
import org.yaml.snakeyaml.Yaml
import java.util.Locale

/**
 * Stores the user's per-domain overrides ("URL selection mode").
 *
 * Each entry maps a domain (lowercased) to an explicit policy:
 * [POLICY_PROXY] or [POLICY_DIRECT]. These override whatever
 * the automatic Clash rules would decide for that domain.
 *
 * Major foreign websites are grouped by company. Each company has a
 * configurable default policy (Proxy or Direct) so the user can flip
 * an entire company with one tap.
 */
class DomainRulesManager(private val context: Context) {
    companion object {
        const val POLICY_PROXY = "Proxy"
        const val POLICY_DIRECT = "DIRECT"

        private const val PREF_NAME = "domain_rules"
        private const val KEY_RULES = "rules"
        private const val KEY_COMPANY_DEFAULTS = "company_defaults"
        private const val KEY_DELETED_COMPANIES = "deleted_companies"
        private const val KEY_DELETED_DOMAINS = "deleted_domains"
        private const val KEY_PRIVATE_DOMAINS = "private_domains"
        private const val KEY_BLOCKED_DOMAINS = "blocked_domains"

        private const val TAG = "DomainRules"

        /**
         * Built-in fallback for the curated company list. The authoritative
         * list is loaded from the hand-editable YAML file
         * (`config_domain_rules.yaml`, seeded from res/raw/domain_rules.yaml).
         * This fallback is only used when that file is missing or unparsable.
         */
        @JvmField
        val DEFAULT_FOREIGN_COMPANIES = linkedMapOf(
            "Google" to listOf(
                "youtube.com", "googlevideo.com", "google.com",
                "googleapis.com", "gstatic.com", "googleusercontent.com",
                // Play Store APK/video CDN hosts. These are NOT google.com
                // subdomains, so without them plays an app download that
                // resolves to a Google CDN IP gets auto-decision DIRECT:
                // great firewall blocks those on 443 and the download dies
                // at 0 bytes with CANNOT_CONNECT.
                "gvt1.com", "gvt2.com", "gvt3.com", "gvt0.com",
                "ggpht.com", "android.clients.google.com",
                "play.googleapis.com", "dl.google.com", "market.android.com"
            ),
            "Meta" to listOf(
                "facebook.com", "instagram.com", "whatsapp.com", "messenger.com"
            ),
            "X (Twitter)" to listOf("twitter.com", "x.com"),
            "TikTok / ByteDance" to listOf(
                "tiktok.com", "tiktokcdn.com", "tiktoktv.com", "tiktokv.com",
                "byteoversea.com", "musical.ly"
            ),
            "Wikipedia" to listOf("wikipedia.org"),
            "GitHub" to listOf("github.com", "githubusercontent.com", "gitlab.com"),
            "Twitch" to listOf("twitch.tv"),
            "Reddit" to listOf("reddit.com"),
            "Netflix" to listOf("netflix.com"),
            "OpenAI" to listOf("openai.com", "chatgpt.com"),
            "Anthropic" to listOf("anthropic.com", "claude.ai"),
            "Cloudflare" to listOf("cloudflare.com"),
            "Medium" to listOf("medium.com"),
            "Quora" to listOf("quora.com"),
            "Pinterest" to listOf("pinterest.com"),
            "Telegram" to listOf("telegram.org", "t.me"),
            "Discord" to listOf("discord.com"),
            "Spotify" to listOf("spotify.com"),
            "Apple" to listOf("apple.com", "icloud.com"),
            "Microsoft" to listOf("microsoft.com", "live.com", "bing.com"),
            "Amazon" to listOf("amazon.com", "imdb.com")
        )

        /**
         * Built-in fallback for the known-unreachable list. The authoritative
         * list is loaded from the same hand-editable YAML file.
         */
        @JvmField
        val DEFAULT_UNREACHABLE: List<String> = listOf(
            "fbcdn.net", "twimg.com"
        )
    }

    /** Company -> domains, loaded from the YAML rules file. */
    private val companies: Map<String, List<String>>

    /** Domains known to be unreachable directly, loaded from the same file. */
    private val unreachableDomains: List<String>

    init {
        val rules = loadRules()
        companies = rules.first
        unreachableDomains = rules.second
    }

    /**
     * Parses the hand-editable rules file. Returns the curated company map and
     * the unreachable list; falls back to the built-in defaults when the file
     * is missing, empty or malformed.
     */
    private fun loadRules(): Pair<Map<String, List<String>>, List<String>> {
        val path = app?.storage?.path?.domainRules
        if (path != null) {
            val text = Storage.read(path)?.toString(Charsets.UTF_8)
            if (!text.isNullOrBlank()) {
                try {
                    @Suppress("UNCHECKED_CAST")
                    val root = Yaml().load<Any?>(text) as? Map<String, Any?>
                    if (root != null &&
                        (root.containsKey("companies") || root.containsKey("unreachable"))
                    ) {
                        // The file is authoritative: an explicitly emptied list
                        // stays empty instead of being repopulated with defaults.
                        return Pair(
                            parseCompanies(root["companies"]),
                            parseUnreachable(root["unreachable"])
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse domain rules, using built-in defaults", e)
                }
            }
        }
        return Pair(DEFAULT_FOREIGN_COMPANIES, DEFAULT_UNREACHABLE)
    }

    private fun parseCompanies(node: Any?): Map<String, List<String>> {
        val result = linkedMapOf<String, List<String>>()
        val map = node as? Map<*, *> ?: return result
        for ((key, value) in map) {
            val company = key as? String ?: continue
            val list = value as? List<*> ?: continue
            val domains = list.mapNotNull { (it as? String)?.trim()?.lowercase(Locale.US) }
                .filter { it.isNotEmpty() }
            if (domains.isNotEmpty()) {
                result[company] = domains
            }
        }
        return result
    }

    private fun parseUnreachable(node: Any?): List<String> {
        val list = node as? List<*> ?: return emptyList()
        return list.mapNotNull { (it as? String)?.trim()?.lowercase(Locale.US) }
            .filter { it.isNotEmpty() }
    }

    // MODE_MULTI_PROCESS: the rule list is written from the main process
    // (rules page, new-URL choice page) and read from the ":proxy" process
    // (Socks5Gate). The flag forces a reload when the file changes so both
    // processes stay in sync. This is deprecated since API 30 but remains the
    // correct tool for this cross-process scenario.
    @Suppress("DEPRECATION")
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREF_NAME, Context.MODE_PRIVATE or Context.MODE_MULTI_PROCESS)

    private val app: IgniterApplication?
        get() = context.applicationContext as? IgniterApplication

    // ── Foreign-site company queries ──────────────────────────────────

    /** @return the curated company -> domain list. */
    fun getMajorForeignCompanies(): Map<String, List<String>> = companies

    /** @return all curated foreign domains (flat list). */
    fun getMajorForeignSites(): List<String> = companies.values.flatten()

    /**
     * @return the manually maintained list of domains known to be unreachable
     *         directly. These are never probed direct: the proxy is tested and
     *         used when it connects, otherwise the domain stays unreachable.
     */
    fun getUnreachableDomains(): List<String> = unreachableDomains

    /** @return true when [host] (or a parent domain) is in the unreachable list. */
    fun isUnreachableDomain(host: String): Boolean {
        val h = host.lowercase(Locale.US)
        for (domain in unreachableDomains) {
            if (h == domain || h.endsWith(".$domain")) {
                return true
            }
        }
        return false
    }

    /**
     * Returns the effective company map after filtering out user-deleted
     * companies and individual domains.
     */
    fun getVisibleForeignCompanies(): Map<String, List<String>> {
        val deletedCompanies = getDeletedCompanies()
        val deletedDomains = getDeletedDomains()
        val result = linkedMapOf<String, List<String>>()
        for ((company, domains) in companies) {
            if (company in deletedCompanies) continue
            val visible = domains.filter { it !in deletedDomains }
            if (visible.isNotEmpty()) {
                result[company] = visible
            }
        }
        return result
    }

    /** Hides an entire company group from the foreign tab. */
    fun deleteCompany(company: String) {
        val set = getDeletedCompanies().toMutableSet()
        set.add(company)
        prefs.edit().putString(KEY_DELETED_COMPANIES, set.joinToString("||")).apply()
    }

    /** Restores a previously hidden company. */
    fun restoreCompany(company: String) {
        val set = getDeletedCompanies().toMutableSet()
        set.remove(company)
        prefs.edit().putString(KEY_DELETED_COMPANIES, set.joinToString("||")).apply()
    }

    /** @return set of company names the user has hidden. */
    fun getDeletedCompanies(): Set<String> {
        val raw = prefs.getString(KEY_DELETED_COMPANIES, "") ?: ""
        return if (raw.isEmpty()) emptySet() else raw.split("||").filter { it.isNotEmpty() }.toSet()
    }

    /** Removes a single domain from its company's list. */
    fun deleteForeignDomain(domain: String) {
        val set = getDeletedDomains().toMutableSet()
        set.add(domain)
        prefs.edit().putString(KEY_DELETED_DOMAINS, set.joinToString("||")).apply()
    }

    /** Restores a previously deleted domain. */
    fun restoreForeignDomain(domain: String) {
        val set = getDeletedDomains().toMutableSet()
        set.remove(domain)
        prefs.edit().putString(KEY_DELETED_DOMAINS, set.joinToString("||")).apply()
    }

    /** @return set of individual domains the user has removed. */
    fun getDeletedDomains(): Set<String> {
        val raw = prefs.getString(KEY_DELETED_DOMAINS, "") ?: ""
        return if (raw.isEmpty()) emptySet() else raw.split("||").filter { it.isNotEmpty() }.toSet()
    }

    /** Restores all deleted companies and domains. */
    fun restoreAllForeign() {
        prefs.edit()
            .remove(KEY_DELETED_COMPANIES)
            .remove(KEY_DELETED_DOMAINS)
            .apply()
    }

    // ── Private (hidden) domains ──────────────────────────────────────

    /** Marks a domain as private — hidden from the Auto tab log. */
    fun addPrivateDomain(domain: String) {
        val set = getPrivateDomains().toMutableSet()
        set.add(domain.lowercase())
        prefs.edit().putString(KEY_PRIVATE_DOMAINS, set.joinToString("||")).apply()
    }

    /** Removes a domain from the private list. */
    fun removePrivateDomain(domain: String) {
        val set = getPrivateDomains().toMutableSet()
        set.remove(domain.lowercase())
        prefs.edit().putString(KEY_PRIVATE_DOMAINS, set.joinToString("||")).apply()
    }

    /** @return set of domains the user has marked as private. */
    fun getPrivateDomains(): Set<String> {
        val raw = prefs.getString(KEY_PRIVATE_DOMAINS, "") ?: ""
        return if (raw.isEmpty()) emptySet() else raw.split("||").filter { it.isNotEmpty() }.toSet()
    }

    /** @return true if [domain] is in the private list. */
    fun isPrivateDomain(domain: String): Boolean {
        return getPrivateDomains().contains(domain.lowercase())
    }

    /** Clears all private domains. */
    fun clearPrivateDomains() {
        prefs.edit().remove(KEY_PRIVATE_DOMAINS).apply()
    }

    // ── Blocked (ad-blocking) domains ──────────────────────────────

    /** Marks a domain as blocked — will be rejected. */
    fun addBlockedDomain(domain: String) {
        val set = getBlockedDomains().toMutableSet()
        set.add(domain.lowercase())
        prefs.edit().putString(KEY_BLOCKED_DOMAINS, set.joinToString("||")).apply()
    }

    /** Removes a domain from the blocked list. */
    fun removeBlockedDomain(domain: String) {
        val set = getBlockedDomains().toMutableSet()
        set.remove(domain.lowercase())
        prefs.edit().putString(KEY_BLOCKED_DOMAINS, set.joinToString("||")).apply()
    }

    /** @return set of domains the user has blocked. */
    fun getBlockedDomains(): Set<String> {
        val raw = prefs.getString(KEY_BLOCKED_DOMAINS, "") ?: ""
        return if (raw.isEmpty()) emptySet() else raw.split("||").filter { it.isNotEmpty() }.toSet()
    }

    /** @return true if [domain] is in the blocked list. */
    fun isBlockedDomain(domain: String): Boolean {
        return getBlockedDomains().contains(domain.lowercase())
    }

    /** Clears all blocked domains. */
    fun clearBlockedDomains() {
        prefs.edit().remove(KEY_BLOCKED_DOMAINS).apply()
    }

    /** @return the default policy for [company] ([POLICY_PROXY] if never changed). */
    fun getCompanyDefaultPolicy(company: String): String {
        val raw = prefs.getString(KEY_COMPANY_DEFAULTS, "") ?: ""
        if (raw.isNotEmpty()) {
            for (entry in raw.split("||")) {
                val parts = entry.split("|", limit = 2)
                if (parts.size == 2 && parts[0] == company) {
                    return if (parts[1] == POLICY_DIRECT) POLICY_DIRECT else POLICY_PROXY
                }
            }
        }
        return POLICY_PROXY
    }

    /** Sets the default policy for an entire company group. */
    fun setCompanyDefaultPolicy(company: String, policy: String) {
        val map = readCompanyDefaultsMap().toMutableMap()
        map[company] = if (policy == POLICY_DIRECT) POLICY_DIRECT else POLICY_PROXY
        val sb = StringBuilder()
        for ((key, value) in map) {
            if (sb.isNotEmpty()) sb.append("||")
            sb.append(key).append("|").append(value)
        }
        prefs.edit().putString(KEY_COMPANY_DEFAULTS, sb.toString()).apply()
    }

    /**
     * Returns the default policy for a foreign domain by looking up which
     * company it belongs to. Returns null if the domain is not in any
     * company's list.
     */
    fun lookupMajorForeignPolicy(host: String): String? {
        for ((company, domains) in companies) {
            for (domain in domains) {
                if (host == domain || host.endsWith(".$domain")) {
                    return getCompanyDefaultPolicy(company)
                }
            }
        }
        return null
    }

    /** @return the company name that owns [host], or null. */
    fun lookupCompany(host: String): String? {
        for ((company, domains) in companies) {
            for (domain in domains) {
                if (host == domain || host.endsWith(".$domain")) {
                    return company
                }
            }
        }
        return null
    }

    // ── Effective rules (injected into Clash) ─────────────────────────

    /**
     * @return rules to inject into Clash: the curated foreign sites using
     *         per-company defaults, overridden by any manual rule the user
     *         has set. The proxy server itself is never included.
     */
    @Synchronized
    fun getEffectiveRules(): MutableMap<String, String> {
        val out = LinkedHashMap<String, String>()
        val deletedDomains = getDeletedDomains()
        for ((company, domains) in companies) {
            if (company in getDeletedCompanies()) continue
            val policy = getCompanyDefaultPolicy(company)
            for (site in domains) {
                if (site !in deletedDomains) {
                    out[site] = policy
                }
            }
        }
        // Known-unreachable domains must always go through the proxy in Clash;
        // a manual rule below still wins.
        for (site in unreachableDomains) {
            if (site !in deletedDomains) {
                out[site] = POLICY_PROXY
            }
        }
        out.putAll(getRules())
        val serverHost = serverHost()
        if (serverHost != null) {
            out.remove(serverHost)
        }
        return out
    }

    // ── Per-domain manual overrides ───────────────────────────────────

    /** @return the current proxy server hostname, lowercased, or null. */
    private fun serverHost(): String? {
        return try {
            val trojanConfig = app?.trojanConfig ?: return null
            val host = trojanConfig.getRemoteAddr().trim()
            if (host.isEmpty() || host == "0.0.0.0") {
                null
            } else {
                host.lowercase(Locale.US).trimEnd('.')
            }
        } catch (e: Exception) {
            null
        }
    }

    /** @return an ordered map of domain(lowercase) -> policy. */
    @Synchronized
    fun getRules(): MutableMap<String, String> {
        val rules = LinkedHashMap<String, String>()
        val raw = prefs.getString(KEY_RULES, "") ?: ""
        if (TextUtils.isEmpty(raw)) {
            return rules
        }
        for (entry in raw.split("||")) {
            val parts = entry.split("|", limit = 2)
            if (parts.size == 2 && parts[0].isNotEmpty() && parts[1].isNotEmpty()) {
                rules[parts[0].lowercase()] = parts[1]
            }
        }
        return rules
    }

    @Synchronized
    fun setRule(domain: String, policy: String) {
        val rules = getRules()
        rules[domain.trim().lowercase()] =
            if (POLICY_PROXY == policy) POLICY_PROXY else POLICY_DIRECT
        save(rules)
    }

    @Synchronized
    fun removeRule(domain: String) {
        val rules = getRules()
        rules.remove(domain.trim().lowercase())
        save(rules)
    }

    /** @return the stored policy for the domain, or `null` if not overridden. */
    fun getPolicy(domain: String): String? {
        return getRules()[domain.trim().lowercase()]
    }

    private fun save(rules: Map<String, String>) {
        val sb = StringBuilder()
        for ((key, value) in rules) {
            if (sb.isNotEmpty()) {
                sb.append("||")
            }
            sb.append(key).append('|').append(value)
        }
        prefs.edit().putString(KEY_RULES, sb.toString()).apply()
    }

    private fun readCompanyDefaultsMap(): Map<String, String> {
        val map = LinkedHashMap<String, String>()
        val raw = prefs.getString(KEY_COMPANY_DEFAULTS, "") ?: ""
        if (raw.isEmpty()) return map
        for (entry in raw.split("||")) {
            val parts = entry.split("|", limit = 2)
            if (parts.size == 2 && parts[0].isNotEmpty() && parts[1].isNotEmpty()) {
                map[parts[0]] = parts[1]
            }
        }
        return map
    }
}
