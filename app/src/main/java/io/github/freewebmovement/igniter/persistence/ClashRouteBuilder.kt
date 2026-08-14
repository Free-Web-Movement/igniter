package io.github.freewebmovement.igniter.persistence

/**
 * Builds the Clash `rules:` lines for user-controlled domain routing and
 * injects them into the config file.
 *
 * The Clash config may be hand-written (block-style `rules:`) or have been
 * re-dumped by SnakeYAML (flow-style `rules: ['a', 'b', ...]`, possibly split
 * across several lines). The builder re-emits the file textually, replacing
 * only the lines it previously marked with [RULE_MARKER] and inserting fresh
 * ones right after the `rules:` key, in whichever style the section uses.
 * Comments and unrelated sections are preserved byte-for-byte.
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
     * given rules inserted after the `rules:` key. Handles both block-style
     * (`- rule` entries) and flow-style (`['rule', ...]`, possibly multi-line)
     * rules sections, so repeated injection is idempotent and the output always
     * remains valid YAML for the embedded Clash parser.
     */
    fun build(configContent: String, rules: Map<String, String>): String {
        val lines = configContent.split('\n')
        val rulesIdx = lines.indexOfFirst { it.trimStart().startsWith("rules:") }
        if (rulesIdx < 0) {
            return configContent
        }
        val newRuleLines = toClashRuleLines(rules)
        return if (lines[rulesIdx].contains('[')) {
            buildFlow(lines, rulesIdx, newRuleLines)
        } else {
            buildBlock(lines, rulesIdx, newRuleLines)
        }
    }

    private fun buildBlock(
        lines: List<String>,
        rulesIdx: Int,
        newRuleLines: List<String>
    ): String {
        val keep = lines.filterNot { it.trim().endsWith(RULE_MARKER) }
        val sb = StringBuilder(lines.size * 40)
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
                for (ruleLine in newRuleLines) {
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

    private fun buildFlow(
        lines: List<String>,
        rulesIdx: Int,
        newRuleLines: List<String>
    ): String {
        // A SnakeYAML dump emits the rules list in flow style, optionally wrapped
        // across several lines. Collect every line belonging to the flow sequence
        // (from the header up to the line holding the closing ']').
        var endIdx = rulesIdx
        while (endIdx < lines.size && !lines[endIdx].contains(']')) {
            endIdx++
        }
        val flowLines = lines.subList(rulesIdx, endIdx + 1)
        val originalRules = parseFlowRules(flowLines)
        val keep = lines.subList(0, rulesIdx) + lines.subList(endIdx + 1, lines.size)

        val sb = StringBuilder(lines.size * 40)
        var first = true
        for (line in keep) {
            if (!first) {
                sb.append('\n')
            }
            sb.append(line)
            first = false
        }
        sb.append('\n')
        sb.append("rules:")
        for (ruleLine in newRuleLines) {
            sb.append('\n').append(ruleLine)
        }
        for (rule in originalRules) {
            sb.append('\n').append("  - ").append(rule)
        }
        return sb.toString()
    }

    /**
     * Extracts the rule strings from a flow-style rules section, dropping stale
     * injected lines and the enclosing brackets.
     */
    private fun parseFlowRules(flowLines: List<String>): List<String> {
        // Stale injected block lines may be embedded inside a previously broken
        // flow section; drop them at the line level so they never reach the
        // comma splitter and get fragmented.
        val kept = flowLines.filterNot { it.trim().endsWith(RULE_MARKER) }
        val text = kept.joinToString("\n")
        val start = text.indexOf('[')
        val end = text.lastIndexOf(']')
        if (start < 0 || end < start) {
            return emptyList()
        }
        return splitFlow(text.substring(start + 1, end))
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { unquote(it) }
            .filter { it.isNotEmpty() }
            .toList()
    }

    /** Splits flow-sequence content on commas that are not inside quotes. */
    private fun splitFlow(content: String): List<String> {
        val parts = ArrayList<String>()
        val sb = StringBuilder()
        var inQuote = false
        for (c in content) {
            when {
                c == '\'' -> {
                    inQuote = !inQuote
                    sb.append(c)
                }
                c == ',' && !inQuote -> {
                    parts.add(sb.toString())
                    sb.setLength(0)
                }
                else -> sb.append(c)
            }
        }
        parts.add(sb.toString())
        return parts
    }

    private fun unquote(s: String): String {
        if (s.length >= 2 && s.startsWith('\'') && s.endsWith('\'')) {
            return s.substring(1, s.length - 1).replace("''", "'")
        }
        return s
    }

    /** Reads, rewrites and persists the config file with the given rules. */
    fun inject(configFile: String, rules: Map<String, String>) {
        val content = Storage.read(configFile) ?: return
        Storage.write(configFile, build(String(content), rules).toByteArray())
    }
}
