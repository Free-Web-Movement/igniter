package io.github.freewebmovement.igniter.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import io.github.freewebmovement.igniter.IgniterApplication;
import io.github.freewebmovement.igniter.R;
import io.github.freewebmovement.igniter.services.ProxyService;

/**
 * Connect page: current state, a big start/stop toggle, the active server card
 * and a connection test.
 */
public class HomeFragment extends Fragment {

    private TextView mStatusTitle;
    private TextView mStatusSub;
    private TextView mServerValue;
    private Button mConnectBtn;

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mStatusTitle = view.findViewById(R.id.homeStatusTitle);
        mStatusSub = view.findViewById(R.id.homeStatusSub);
        mServerValue = view.findViewById(R.id.homeServerValue);
        mConnectBtn = view.findViewById(R.id.homeConnectBtn);

        mConnectBtn.setOnClickListener(v -> {
            MainActivity activity = (MainActivity) getActivity();
            if (activity == null) {
                return;
            }
            if (activity.isProxyRunning()) {
                activity.stopProxy();
            } else {
                activity.startProxy();
            }
        });
        view.findViewById(R.id.homeServerRow).setOnClickListener(v -> {
            MainActivity activity = (MainActivity) getActivity();
            if (activity != null) {
                activity.openServersTab();
            }
        });
        view.findViewById(R.id.homeTestBtn).setOnClickListener(v -> {
            MainActivity activity = (MainActivity) getActivity();
            if (activity != null) {
                activity.testConnection();
            }
        });

        refreshServerInfo();
        updateState(((MainActivity) requireActivity()).getProxyState());
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_home, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null) {
            return false;
        }
        if (id == R.id.action_view_test_connection) {
            activity.testConnection();
            return true;
        }
        if (id == R.id.action_view_settings) {
            startActivity(new Intent(activity, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshServerInfo();
    }

    public void updateState(int state) {
        if (mStatusTitle == null || mConnectBtn == null) {
            return;
        }
        switch (state) {
            case ProxyService.STARTED: {
                mStatusTitle.setText(R.string.home_status_started);
                mStatusTitle.setTextColor(0xFF2E7D32);
                mConnectBtn.setText(R.string.home_btn_stop);
                mConnectBtn.setEnabled(true);
                break;
            }
            case ProxyService.STARTING:
            case ProxyService.STOPPING: {
                mStatusTitle.setText(state == ProxyService.STARTING
                        ? R.string.home_status_starting : R.string.home_status_stopping);
                mStatusTitle.setTextColor(0xFFF57F17);
                mConnectBtn.setEnabled(false);
                break;
            }
            default: {
                mStatusTitle.setText(R.string.home_status_stopped);
                mStatusTitle.setTextColor(0xFF757575);
                mConnectBtn.setText(R.string.home_btn_start);
                mConnectBtn.setEnabled(true);
                break;
            }
        }
    }

    public void refreshServerInfo() {
        if (mServerValue == null || mStatusSub == null) {
            return;
        }
        IgniterApplication app = IgniterApplication.getApplication();
        String addr = app.trojanConfig.getRemoteAddr();
        int port = app.trojanConfig.getRemotePort();
        if (addr == null || addr.isEmpty()) {
            mServerValue.setText(R.string.home_server_unknown);
            mStatusSub.setText(R.string.home_server_unknown);
        } else {
            String text = addr + ":" + port;
            mServerValue.setText(text);
            mStatusSub.setText(text);
        }
    }
}
