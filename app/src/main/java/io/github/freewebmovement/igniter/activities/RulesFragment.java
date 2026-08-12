package io.github.freewebmovement.igniter.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.freewebmovement.igniter.IgniterApplication;
import io.github.freewebmovement.igniter.R;
import io.github.freewebmovement.igniter.persistence.DomainRulesManager;

/**
 * Rules page with three tabs:
 * <ul>
 *     <li>手动 (manual) - the user's explicit per-domain overrides;</li>
 *     <li>自动 (auto) - domains currently going through the tunnel, live;</li>
 *     <li>国外大网站 (major foreign websites) - a curated list that defaults
 *         to Proxy.</li>
 * </ul>
 * Locked domains (manual + curated) are injected into the Clash rules on the
 * next connection.
 */
public class RulesFragment extends Fragment {

    private static final long POLL_INTERVAL_MS = 1500;

    // logcat -d re-reads the same tail of the buffer on every poll, so raw
    // lines must be deduplicated or every hit would be recounted each time.
    private static final int SEEN_LINES_CAPACITY = 2000;

    private static final int TAB_MANUAL = 0;
    private static final int TAB_AUTO = 1;
    private static final int TAB_FOREIGN = 2;

    private static final int COLOR_DIRECT = 0xFF2E7D32;
    private static final int COLOR_PROXY = 0xFFC62828;
    private static final int COLOR_DEFAULT = 0xFF757575;

    // go-tun2socks format: [tun2socks] [proxy] [tcp] [N/A] www.google.com:443
    // domain sits right after the bracket (bracket content is always "N/A")
    private static final Pattern TUN2SOCKS_TCP = Pattern.compile(
            "\\[tun2socks\\] \\[proxy\\] \\[tcp\\] \\[[^\\]]*\\]\\s+([A-Za-z0-9_.-]+):\\d+");
    private static final Pattern TUN2SOCKS_UDP = Pattern.compile(
            "\\[tun2socks\\] \\[proxy\\] \\[udp\\] \\[[^\\]]*\\]\\s+([A-Za-z0-9_.-]+):\\d+");
    // Clash rule log lines: [TCP] 127.0.0.1:59270 --> cdnws.api.huya.com:443 match DomainSuffix(huya.com) using DIRECT
    private static final Pattern CLASH_TCP = Pattern.compile(
            "\\[TCP\\]\\s+[0-9a-fA-F.:]+\\s*-->\\s*([A-Za-z0-9_.-]+):\\d+\\s+match\\s+.*using\\s+(\\S+)");
    private static final Pattern CLASH_UDP = Pattern.compile(
            "\\[UDP\\]\\s+[0-9a-fA-F.:]+\\s*-->\\s*([A-Za-z0-9_.-]+):\\d+\\s+match\\s+.*using\\s+(\\S+)");

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final LinkedHashMap<String, Entry> mEntries = new LinkedHashMap<>();
    private final List<Entry> mList = new ArrayList<>();
    // Tracks raw logcat lines already counted so a line is never counted twice.
    private final ArrayDeque<String> mSeenOrder = new ArrayDeque<>();
    private final HashSet<String> mSeenSet = new HashSet<>();

    private DomainRulesManager mRules;
    private RecyclerView mRv;
    private EditText mSearch;
    private TextView mEmptyHint;
    private TextView mTabManual;
    private TextView mTabAuto;
    private TextView mTabForeign;
    private Adapter mAdapter;
    private String mFilter = "";
    private int mTab = TAB_MANUAL;
    private volatile boolean mRunning = true;
    private Thread mPollThread;

    private static class Entry {
        String domain;
        String clashPolicy;
        boolean isIp;
        int count;
    }

    public static RulesFragment newInstance() {
        return new RulesFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_rules, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRules = new DomainRulesManager(IgniterApplication.getApplication());
        setHasOptionsMenu(true);

        mRv = view.findViewById(R.id.domainRuleRv);
        mRv.setLayoutManager(new LinearLayoutManager(getContext()));
        mAdapter = new Adapter();
        mRv.setAdapter(mAdapter);

        mEmptyHint = view.findViewById(R.id.emptyHint);
        mSearch = view.findViewById(R.id.searchInput);
        mSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                mFilter = s == null ? "" : s.toString().toLowerCase(Locale.US);
                rebuildList();
            }
        });

        mTabManual = view.findViewById(R.id.tabManual);
        mTabAuto = view.findViewById(R.id.tabAuto);
        mTabForeign = view.findViewById(R.id.tabForeign);
        mTabManual.setOnClickListener(v -> switchTab(TAB_MANUAL));
        mTabAuto.setOnClickListener(v -> switchTab(TAB_AUTO));
        mTabForeign.setOnClickListener(v -> switchTab(TAB_FOREIGN));
        switchTab(TAB_MANUAL);

        mPollThread = new Thread(this::pollLoop, "igniter-domain-monitor");
        mPollThread.start();
    }

    private void switchTab(int tab) {
        mTab = tab;
        mTabManual.setSelected(tab == TAB_MANUAL);
        mTabAuto.setSelected(tab == TAB_AUTO);
        mTabForeign.setSelected(tab == TAB_FOREIGN);
        rebuildList();
    }

    private void pollLoop() {
        while (mRunning) {
            try {
                String output = readLogcat();
                if (output != null && !output.isEmpty()) {
                    String fresh = filterNewLines(output);
                    if (!fresh.isEmpty()) {
                        final List<String[]> hits = parseHits(fresh);
                        mHandler.post(() -> {
                            if (!mRunning || !isAdded()) {
                                return;
                            }
                            for (String[] hit : hits) {
                                Entry e = mEntries.get(hit[0]);
                                if (e == null) {
                                    e = new Entry();
                                    e.domain = hit[0];
                                    e.isIp = isIpLike(hit[0]);
                                    mEntries.put(hit[0], e);
                                }
                                e.count++;
                                if (hit[1] != null) {
                                    e.clashPolicy = hit[1];
                                }
                            }
                            if (mTab == TAB_AUTO) {
                                rebuildList();
                            }
                        });
                    }
                }
            } catch (Exception ignored) {
            }
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    /**
     * Returns only the logcat lines that have not been seen yet, recording the
     * returned lines so they are skipped on subsequent polls.
     */
    private String filterNewLines(String output) {
        StringBuilder sb = new StringBuilder(output.length());
        for (String line : output.split("\n")) {
            if (line.isEmpty() || mSeenSet.contains(line)) {
                continue;
            }
            if (mSeenOrder.size() >= SEEN_LINES_CAPACITY) {
                mSeenSet.remove(mSeenOrder.pollFirst());
            }
            mSeenOrder.addLast(line);
            mSeenSet.add(line);
            sb.append(line).append('\n');
        }
        return sb.toString();
    }

    private static String readLogcat() throws Exception {
        Process p = Runtime.getRuntime().exec(
                new String[]{"logcat", "-d", "-t", "1000", "-s", "GoLog:V", "*:S"});
        StringBuilder sb = new StringBuilder(16384);
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            char[] buf = new char[8192];
            int n;
            while ((n = r.read(buf)) != -1) {
                sb.append(buf, 0, n);
            }
        }
        p.waitFor();
        return sb.toString();
    }

    private static boolean isIpLike(String s) {
        if ("N/A".equals(s)) {
            return true;
        }
        return s.matches("[0-9a-fA-F:.]+");
    }

    private static String cleanPolicy(String p) {
        if (p == null) {
            return null;
        }
        int i = p.indexOf('[');
        return i > 0 ? p.substring(0, i) : p;
    }

    private static List<String[]> parseHits(String log) {
        List<String[]> hits = new ArrayList<>();
        for (Pattern p : new Pattern[]{TUN2SOCKS_TCP, TUN2SOCKS_UDP}) {
            Matcher m = p.matcher(log);
            while (m.find()) {
                hits.add(new String[]{m.group(1).trim().toLowerCase(Locale.US), null});
            }
        }
        for (Pattern p : new Pattern[]{CLASH_TCP, CLASH_UDP}) {
            Matcher m = p.matcher(log);
            while (m.find()) {
                hits.add(new String[]{m.group(1).trim().toLowerCase(Locale.US), cleanPolicy(m.group(2))});
            }
        }
        return hits;
    }

    private void rebuildList() {
        if (mList == null) {
            return;
        }
        mList.clear();
        boolean empty;
        if (mTab == TAB_MANUAL) {
            for (Map.Entry<String, String> rule : mRules.getRules().entrySet()) {
                if (!mFilter.isEmpty() && !rule.getKey().contains(mFilter)) {
                    continue;
                }
                Entry e = new Entry();
                e.domain = rule.getKey();
                e.clashPolicy = rule.getValue();
                mList.add(e);
            }
            empty = mList.isEmpty();
        } else if (mTab == TAB_FOREIGN) {
            for (String site : mRules.getMajorForeignSites()) {
                if (!mFilter.isEmpty() && !site.contains(mFilter)) {
                    continue;
                }
                Entry e = new Entry();
                e.domain = site;
                e.clashPolicy = mRules.getPolicy(site);
                mList.add(e);
            }
            empty = mList.isEmpty();
        } else {
            for (Entry e : mEntries.values()) {
                if (!mFilter.isEmpty() && !e.domain.contains(mFilter)) {
                    continue;
                }
                mList.add(e);
            }
            mList.sort((a, b) -> {
                if (a.isIp != b.isIp) {
                    return a.isIp ? 1 : -1;
                }
                return Long.compare(b.count, a.count);
            });
            empty = mList.isEmpty();
        }
        if (mEmptyHint != null) {
            mEmptyHint.setVisibility(empty ? View.VISIBLE : View.GONE);
            if (empty) {
                mEmptyHint.setText(mTab == TAB_MANUAL ? R.string.domain_monitor_empty_manual
                        : mTab == TAB_FOREIGN ? R.string.domain_monitor_empty_foreign
                        : R.string.domain_monitor_empty_auto);
            }
        }
        mAdapter.notifyDataSetChanged();
    }

    private void showAddDialog() {
        final EditText input = new EditText(getContext());
        input.setHint(R.string.domain_monitor_add_hint);
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.domain_monitor_add_title)
                .setView(input)
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.domain_monitor_add_direct,
                        (d, w) -> addDomain(input.getText().toString(), DomainRulesManager.POLICY_DIRECT))
                .setPositiveButton(R.string.domain_monitor_add_proxy,
                        (d, w) -> addDomain(input.getText().toString(), DomainRulesManager.POLICY_PROXY))
                .show();
    }

    private void addDomain(String domain, String policy) {
        domain = domain == null ? "" : domain.trim().toLowerCase(Locale.US);
        if (domain.isEmpty()) {
            return;
        }
        mRules.setRule(domain, policy);
        switchTab(TAB_MANUAL);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_domain_monitor, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_add_domain) {
            showAddDialog();
            return true;
        }
        if (id == R.id.action_clear_domains) {
            mEntries.clear();
            rebuildList();
            return true;
        }
        if (id == R.id.action_view_clash_editor) {
            startActivity(new Intent(getContext(), ClashFileEditorActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mRunning = false;
        if (mPollThread != null) {
            mPollThread.interrupt();
        }
    }

    private class Adapter extends RecyclerView.Adapter<Adapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_domain_rule, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            final Entry e = mList.get(position);
            final String domain = e.domain;
            h.domain.setText(domain);

            String locked = mRules.getPolicy(domain);
            String policy = locked != null ? locked : e.clashPolicy;
            if (mTab == TAB_MANUAL) {
                h.status.setText(getString(R.string.domain_monitor_manual_locked,
                        locked != null ? locked : policy));
                h.status.setTextColor((locked != null ? locked : policy).contains("DIRECT")
                        ? COLOR_DIRECT : COLOR_PROXY);
            } else if (mTab == TAB_FOREIGN) {
                if (locked != null) {
                    h.status.setText(getString(R.string.domain_monitor_foreign_locked, locked));
                    h.status.setTextColor(locked.contains("DIRECT") ? COLOR_DIRECT : COLOR_PROXY);
                } else {
                    h.status.setText(getString(R.string.domain_monitor_foreign_default,
                            DomainRulesManager.POLICY_PROXY));
                    h.status.setTextColor(COLOR_DEFAULT);
                }
            } else if (locked != null) {
                h.status.setText(getString(R.string.domain_monitor_locked, locked, e.count));
                h.status.setTextColor(locked.contains("DIRECT") ? COLOR_DIRECT : COLOR_PROXY);
            } else if (policy != null) {
                h.status.setText(getString(R.string.domain_monitor_auto, policy, e.count));
                h.status.setTextColor(policy.contains("DIRECT") ? COLOR_DIRECT : COLOR_PROXY);
            } else {
                h.status.setText(getString(R.string.domain_monitor_unknown, e.count));
                h.status.setTextColor(COLOR_DEFAULT);
            }
            h.btnProxy.setOnClickListener(v -> {
                mRules.setRule(domain, DomainRulesManager.POLICY_PROXY);
                rebuildList();
            });
            h.btnDirect.setOnClickListener(v -> {
                mRules.setRule(domain, DomainRulesManager.POLICY_DIRECT);
                rebuildList();
            });
            boolean showUnlock = mTab == TAB_MANUAL || locked != null;
            h.btnUnlock.setVisibility(showUnlock ? View.VISIBLE : View.GONE);
            h.btnUnlock.setOnClickListener(v -> {
                mRules.removeRule(domain);
                rebuildList();
            });

            String activePolicy = locked != null ? locked : e.clashPolicy;
            if (mTab == TAB_FOREIGN && locked == null) {
                activePolicy = DomainRulesManager.POLICY_PROXY;
            }
            h.btnProxy.setSelected(DomainRulesManager.POLICY_PROXY.equals(activePolicy));
            h.btnDirect.setSelected(DomainRulesManager.POLICY_DIRECT.equals(activePolicy));
            h.btnUnlock.setSelected(false);
        }

        @Override
        public int getItemCount() {
            return mList.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView domain;
            TextView status;
            Button btnProxy;
            Button btnDirect;
            Button btnUnlock;

            VH(View v) {
                super(v);
                domain = v.findViewById(R.id.domainText);
                status = v.findViewById(R.id.statusText);
                btnProxy = v.findViewById(R.id.btnProxy);
                btnDirect = v.findViewById(R.id.btnDirect);
                btnUnlock = v.findViewById(R.id.btnUnlock);
            }
        }
    }
}
