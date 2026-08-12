package io.github.freewebmovement.igniter.activities;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import io.github.freewebmovement.igniter.R;

/**
 * Thin wrapper keeping the standalone entry point alive; the actual UI lives
 * in {@link RulesFragment} which is also used by the main tab shell.
 */
public class DomainMonitorActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_domain_monitor);
        FragmentManager fm = getSupportFragmentManager();
        RulesFragment fragment = (RulesFragment) fm.findFragmentByTag("rules");
        if (fragment == null) {
            fragment = RulesFragment.newInstance();
        }
        fm.beginTransaction()
                .replace(R.id.parent_fl, fragment, "rules")
                .commitAllowingStateLoss();
    }
}
