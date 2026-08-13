package io.github.freewebmovement.igniter.persistence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClashRouteBuilderTest {

    private val builder = ClashRouteBuilder()

    private fun configWithRules(): String {
        return """
            |# Hand-written clash config with comments
            |mode: Rule
            |
            |rules:
            |  - GEOIP,CN,DIRECT
            |  - MATCH,Proxy
            |
            |other-key: value
            |""".trimMargin()
    }

    @Test
    fun toClashRuleLines_usesMarker() {
        val lines = builder.toClashRuleLines(linkedMapOf("example.com" to "Proxy", "foo.cn" to "DIRECT"))
        assertEquals(listOf(
            "  - DOMAIN-SUFFIX,example.com,Proxy  # user rule",
            "  - DOMAIN-SUFFIX,foo.cn,DIRECT  # user rule"
        ), lines)
    }

    @Test
    fun build_insertsAfterRulesAndPreservesComments() {
        val out = builder.build(configWithRules(), mapOf("example.com" to "Proxy"))
        assertTrue(out.contains("# Hand-written clash config with comments"))
        assertTrue(out.contains("  - GEOIP,CN,DIRECT"))
        assertTrue(out.contains("  - DOMAIN-SUFFIX,example.com,Proxy  # user rule"))
        // The user rule must sit inside the rules: list, not at the file end.
        assertTrue(out.indexOf("rules:") < out.indexOf("DOMAIN-SUFFIX,example.com"))
        assertTrue(out.indexOf("DOMAIN-SUFFIX,example.com") < out.indexOf("other-key: value"))
    }

    @Test
    fun build_replacesStaleInjectedLines() {
        val stale = configWithRules() +
            "  - DOMAIN-SUFFIX,old.com,DIRECT  # user rule\n" +
            "  - DOMAIN-SUFFIX,gone.net,Proxy  # user rule\n"
        val out = builder.build(stale, mapOf("new.com" to "Proxy"))
        assertFalse(out.contains("old.com"))
        assertFalse(out.contains("gone.net"))
        assertTrue(out.contains("new.com"))
        // Only one injected rule remains.
        assertEquals(1, out.split("\n").count { it.trim().endsWith("# user rule") })
    }

    @Test
    fun build_noRulesKeyLeavesContentUnchanged() {
        val input = "mode: Global\nother: 1\n"
        assertEquals(input, builder.build(input, mapOf("example.com" to "Proxy")))
    }

    @Test
    fun build_secondInjectionKeepsRulesOnItsOwnLine() {
        // A comment directly precedes `rules:`. The first injection drops the
        // blank line that separated them; a second injection must not merge the
        // `rules:` header into the comment (which would absorb the whole rules
        // block into the previous YAML key and make Clash abort startup).
        val input = "# from .../clash.yaml\n\nrules:\n  - GEOIP,CN,DIRECT\n"
        val once = builder.build(input, mapOf("a.com" to "Proxy"))
        val twice = builder.build(once, mapOf("b.com" to "Proxy"))
        assertFalse("rules header merged into comment", twice.contains("clash.yamlrules:"))
        assertEquals(1, twice.split("\n").count { it.trim() == "rules:" })
        assertTrue(twice.contains("  - DOMAIN-SUFFIX,b.com,Proxy  # user rule"))
    }
}
