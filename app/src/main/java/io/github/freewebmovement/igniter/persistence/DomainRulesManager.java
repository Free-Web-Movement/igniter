package io.github.freewebmovement.igniter.persistence;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores the user's per-domain overrides ("URL selection mode").
 *
 * <p>Each entry maps a domain (lowercased) to an explicit policy:
 * {@link #POLICY_PROXY} or {@link #POLICY_DIRECT}. These override whatever
 * the automatic Clash rules would decide for that domain.
 */
public class DomainRulesManager {
    public static final String POLICY_PROXY = "Proxy";
    public static final String POLICY_DIRECT = "DIRECT";

    private static final String PREF_NAME = "domain_rules";
    private static final String KEY_RULES = "rules";

    /**
     * Curated list of major foreign websites. They default to {@link #POLICY_PROXY}
     * (see {@link #getEffectiveRules()}) so these sites always go through the
     * proxy unless the user explicitly overrides them.
     */
    public static final String[] MAJOR_FOREIGN_SITES = {
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
    };

    private final SharedPreferences prefs;

    public DomainRulesManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /** @return the curated list of major foreign websites. */
    public List<String> getMajorForeignSites() {
        List<String> sites = new ArrayList<>(MAJOR_FOREIGN_SITES.length);
        for (String site : MAJOR_FOREIGN_SITES) {
            sites.add(site);
        }
        return sites;
    }

    /**
     * @return rules to inject into Clash: the curated foreign sites defaulting
     *         to Proxy, overridden by any manual rule the user has set.
     */
    public synchronized Map<String, String> getEffectiveRules() {
        Map<String, String> out = new LinkedHashMap<>();
        for (String site : MAJOR_FOREIGN_SITES) {
            out.put(site, POLICY_PROXY);
        }
        out.putAll(getRules());
        return out;
    }

    /** @return an ordered map of domain(lowercase) -> policy. */
    public synchronized Map<String, String> getRules() {
        Map<String, String> rules = new LinkedHashMap<>();
        String raw = prefs.getString(KEY_RULES, "");
        if (TextUtils.isEmpty(raw)) {
            return rules;
        }
        for (String entry : raw.split("\\|\\|")) {
            String[] parts = entry.split("\\|", 2);
            if (parts.length == 2 && !parts[0].isEmpty() && !parts[1].isEmpty()) {
                rules.put(parts[0].toLowerCase(), parts[1]);
            }
        }
        return rules;
    }

    public synchronized void setRule(String domain, String policy) {
        Map<String, String> rules = getRules();
        rules.put(domain.trim().toLowerCase(),
                POLICY_PROXY.equals(policy) ? POLICY_PROXY : POLICY_DIRECT);
        save(rules);
    }

    public synchronized void removeRule(String domain) {
        Map<String, String> rules = getRules();
        rules.remove(domain.trim().toLowerCase());
        save(rules);
    }

    /** @return the stored policy for the domain, or {@code null} if not overridden. */
    public String getPolicy(String domain) {
        return getRules().get(domain.trim().toLowerCase());
    }

    /** Clash rule lines to inject, e.g. {@code "  - DOMAIN-SUFFIX,example.com,Proxy"}. */
    public List<String> toClashRuleLines() {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, String> e : getRules().entrySet()) {
            lines.add("  - DOMAIN-SUFFIX," + e.getKey() + "," + e.getValue() + "  # user rule");
        }
        return lines;
    }

    private void save(Map<String, String> rules) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : rules.entrySet()) {
            if (sb.length() > 0) {
                sb.append("||");
            }
            sb.append(e.getKey()).append('|').append(e.getValue());
        }
        prefs.edit().putString(KEY_RULES, sb.toString()).apply();
    }
}
