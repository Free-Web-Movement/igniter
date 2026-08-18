package io.github.freewebmovement.igniter.persistence

import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import io.github.freewebmovement.igniter.IgniterApplication
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

        /**
         * Major foreign websites grouped by company.
         * The default policy for every company is [POLICY_PROXY] unless
         * the user overrides it via [setCompanyDefaultPolicy].
         */
        @JvmField
        val MAJOR_FOREIGN_COMPANIES = linkedMapOf(
            "Google" to listOf(
                "youtube.com", "googlevideo.com", "google.com",
                "googleapis.com", "gstatic.com", "googleusercontent.com"
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

        /** Flat list of all foreign domains. */
        @JvmField
        val MAJOR_FOREIGN_SITES: List<String> =
            MAJOR_FOREIGN_COMPANIES.values.flatten()
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
    fun getMajorForeignCompanies(): Map<String, List<String>> = MAJOR_FOREIGN_COMPANIES

    /** @return all curated foreign domains (flat list). */
    fun getMajorForeignSites(): List<String> = MAJOR_FOREIGN_SITES

    /**
     * Returns the effective company map after filtering out user-deleted
     * companies and individual domains.
     */
    fun getVisibleForeignCompanies(): Map<String, List<String>> {
        val deletedCompanies = getDeletedCompanies()
        val deletedDomains = getDeletedDomains()
        val result = linkedMapOf<String, List<String>>()
        for ((company, domains) in MAJOR_FOREIGN_COMPANIES) {
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
        for ((company, domains) in MAJOR_FOREIGN_COMPANIES) {
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
        for ((company, domains) in MAJOR_FOREIGN_COMPANIES) {
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
        for ((company, domains) in MAJOR_FOREIGN_COMPANIES) {
            if (company in getDeletedCompanies()) continue
            val policy = getCompanyDefaultPolicy(company)
            for (site in domains) {
                if (site !in deletedDomains) {
                    out[site] = policy
                }
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
