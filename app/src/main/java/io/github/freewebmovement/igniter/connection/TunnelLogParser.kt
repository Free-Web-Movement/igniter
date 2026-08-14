package io.github.freewebmovement.igniter.connection

import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale
import java.util.regex.Pattern

/**
 * Parses tunnel traffic logs produced by go-tun2socks / Clash (logcat tag
 * "GoLog") to extract the domains currently being accessed through the proxy
 * tunnel. Shared by the rules-page live monitor ([io.github.freewebmovement.
 * igniter.activities.RulesFragment]) and the background new-URL prompter
 * ([io.github.freewebmovement.igniter.services.ProxyService]).
 */
class TunnelLogParser {

    data class Hit(val domain: String?, val policy: String?)

    /**
     * Dumps the tunnel logcat. [since] is a "MM-dd HH:mm:ss.SSS" device-time
     * watermark: when provided only lines logged after it are returned (via
     * `-T`), otherwise the most recent lines are returned. A plain `-t` count
     * is avoided because it truncates the raw buffer *before* filtering, which
     * silently drops Socks5Gate lines whenever other tags dominate the tail.
     */
    fun readLogcat(since: String?): String {
        val cmd = if (since == null) {
            arrayOf("logcat", "-d", "-t", "2000", "-s", "GoLog:V", "Socks5Gate:I", "*:S")
        } else {
            arrayOf("logcat", "-d", "-T", since, "-s", "GoLog:V", "Socks5Gate:I", "*:S")
        }
        val p = Runtime.getRuntime().exec(cmd)
        val sb = StringBuilder(16384)
        BufferedReader(InputStreamReader(p.inputStream)).use { r ->
            val buf = CharArray(8192)
            while (true) {
                val n = r.read(buf)
                if (n == -1) {
                    break
                }
                sb.append(buf, 0, n)
            }
        }
        p.waitFor()
        return sb.toString()
    }

    fun parseHits(log: String): List<Hit> {
        val hits = mutableListOf<Hit>()
        for (p in listOf(TUN2SOCKS_TCP, TUN2SOCKS_UDP)) {
            val m = p.matcher(log)
            while (m.find()) {
                hits.add(Hit(m.group(1)?.trim()?.lowercase(Locale.US), null))
            }
        }
        for (p in listOf(CLASH_TCP, CLASH_UDP)) {
            val m = p.matcher(log)
            while (m.find()) {
                hits.add(Hit(m.group(1)?.trim()?.lowercase(Locale.US), cleanPolicy(m.group(2))))
            }
        }
        // Socks5Gate auto-detection decisions: auto-decision <host> -> <state>
        val g = GATE_DECISION.matcher(log)
        while (g.find()) {
            hits.add(Hit(g.group(1)?.trim()?.lowercase(Locale.US), g.group(2)))
        }
        return hits
    }

    companion object {
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
        // Socks5Gate auto-detection: auto-decision www.google.com -> DNS_POLLUTED
        private val GATE_DECISION = Pattern.compile(
            "auto-decision\\s+([A-Za-z0-9_.-]+)\\s+->\\s+(\\S+)")

        fun isIpLike(s: String): Boolean {
            if ("N/A" == s) {
                return true
            }
            return Regex("[0-9a-fA-F:.]+").matches(s)
        }

        fun cleanPolicy(p: String?): String? {
            if (p == null) {
                return null
            }
            val i = p.indexOf('[')
            return if (i > 0) p.substring(0, i) else p
        }
    }
}
