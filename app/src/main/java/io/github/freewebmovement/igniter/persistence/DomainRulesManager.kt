package io.github.freewebmovement.igniter.persistence

import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils

/**
 * Stores the user's per-domain overrides ("URL selection mode").
 *
 * Each entry maps a domain (lowercased) to an explicit policy:
 * [POLICY_PROXY] or [POLICY_DIRECT]. These override whatever
 * the automatic Clash rules would decide for that domain.
 */
class DomainRulesManager(context: Context) {
    companion object {
        const val POLICY_PROXY = "Proxy"
        const val POLICY_DIRECT = "DIRECT"

        private const val PREF_NAME = "domain_rules"
        private const val KEY_RULES = "rules"

        /**
         * Curated list of major foreign websites. They default to [POLICY_PROXY]
         * (see [getEffectiveRules]) so these sites always go through the
         * proxy unless the user explicitly overrides them.
         */
        @JvmField
        val MAJOR_FOREIGN_SITES = arrayOf(
            "youtube.com",
            "googlevideo.com",
            "google.com",
            "googleapis.com",
            "gstatic.com",
            "googleusercontent.com",
            "facebook.com",
            "instagram.com",
            "whatsapp.com",
            "messenger.com",
            "twitter.com",
            "x.com",
            "tiktok.com",
            "tiktokcdn.com",
            "wikipedia.org",
            "github.com",
            "githubusercontent.com",
            "gitlab.com",
            "twitch.tv",
            "reddit.com",
            "netflix.com",
            "openai.com",
            "chatgpt.com",
            "anthropic.com",
            "claude.ai",
            "cloudflare.com",
            "medium.com",
            "quora.com",
            "pinterest.com",
            "telegram.org",
            "t.me",
            "discord.com",
            "spotify.com",
            "apple.com",
            "icloud.com",
            "microsoft.com",
            "live.com",
            "bing.com",
            "amazon.com",
            "imdb.com"
        )
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    /** @return the curated list of major foreign websites. */
    fun getMajorForeignSites(): List<String> {
        return MAJOR_FOREIGN_SITES.toList()
    }

    /**
     * @return rules to inject into Clash: the curated foreign sites defaulting
     *         to Proxy, overridden by any manual rule the user has set.
     */
    @Synchronized
    fun getEffectiveRules(): MutableMap<String, String> {
        val out = LinkedHashMap<String, String>()
        for (site in MAJOR_FOREIGN_SITES) {
            out[site] = POLICY_PROXY
        }
        out.putAll(getRules())
        return out
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
}
