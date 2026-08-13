package io.github.freewebmovement.igniter.persistence

/**
 * Builds the Clash `rules:` lines for user-controlled domain routing and
 * injects them into the config file.
 *
 * The Clash config is hand-written and heavily commented, so the full file is
 * never re-dumped (SnakeYAML would strip every comment). Instead the builder
 * re-emits the file textually, replacing only the lines it previously marked
 * with [RULE_MARKER] and inserting fresh ones right after the `rules:` line.
 */
class ClashRouteBuilder {

    companion object {
        const val RULE_MARKER = "# user rule"
    }

    /**
     * The rules to inject: the curated major-foreign-website list defaulting to
     * [DomainRulesManager.POLICY_PROXY], overridden by the user's manual rules.
     */
    fun effectiveRules(manager: DomainRulesManager): Map<String, String> {
        return manager.getEffectiveRules()
    }

    /** e.g. `"  - DOMAIN-SUFFIX,example.com,Proxy  # user rule"`. */
    fun toClashRuleLines(rules: Map<String, String>): List<String> {
        return rules.map { (domain, policy) ->
            "  - DOMAIN-SUFFIX,$domain,$policy  $RULE_MARKER"
        }
    }

    /**
     * Returns the config file content with stale injected lines removed and the
     * given rules inserted after the `rules:` key. Re-emits the file byte-for
     * -byte otherwise, so comments are preserved and repeated injection is
     * idempotent.
     */
    fun build(configContent: String, rules: Map<String, String>): String {
        val keep = configContent.split('\n')
            .filterNot { it.trim().endsWith(RULE_MARKER) }
        val sb = StringBuilder(configContent.length + rules.size * 48)
        var inserted = false
        val last = keep.lastIndex
        for ((index, line) in keep.withIndex()) {
            if (!inserted && line.startsWith("rules:")) {
                // Keep the header on its own line. Without this newline a second
                // injection would merge `rules:` into the preceding comment line,
                // turning the whole rules block into a misplaced list and making
                // Clash abort startup (native os.Exit).
                if (index > 0) {
                    sb.append('\n')
                }
                sb.append(line)
                for (ruleLine in toClashRuleLines(rules)) {
                    sb.append('\n').append(ruleLine)
                }
                if (index < last) {
                    sb.append('\n')
                }
                inserted = true
            } else {
                if (index > 0) {
                    sb.append('\n')
                }
                sb.append(line)
            }
        }
        return sb.toString()
    }

    /** Reads, rewrites and persists the config file with the given rules. */
    fun inject(configFile: String, rules: Map<String, String>) {
        val content = Storage.read(configFile) ?: return
        Storage.write(configFile, build(String(content), rules).toByteArray())
    }
}
