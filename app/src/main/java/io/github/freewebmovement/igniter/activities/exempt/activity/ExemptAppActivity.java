package io.github.freewebmovement.igniter.activities.exempt.activity;

import android.os.Bundle;
import android.view.Window;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import io.github.freewebmovement.igniter.IgniterApplication;
import io.github.freewebmovement.igniter.R;
import io.github.freewebmovement.igniter.activities.exempt.contract.ExemptAppContract;
import io.github.freewebmovement.igniter.persistence.data.ExemptAppDataManager;
import io.github.freewebmovement.igniter.activities.exempt.fragment.ExemptAppFragment;
import io.github.freewebmovement.igniter.activities.exempt.presenter.ExemptAppPresenter;

public class ExemptAppActivity extends AppCompatActivity {
    private ExemptAppContract.Presenter mPresenter;
    IgniterApplication app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_exempt_app);
        app = (IgniterApplication)getApplication();
        FragmentManager fm = getSupportFragmentManager();
        ExemptAppFragment fragment = (ExemptAppFragment) fm.findFragmentByTag(ExemptAppFragment.TAG);
        if (fragment == null) {
            fragment = ExemptAppFragment.newInstance();
        }
        mPresenter = new ExemptAppPresenter(fragment,
                app.exemptAppDataManager);
        fm.beginTransaction()
                .replace(R.id.parent_fl, fragment, ExemptAppFragment.TAG)
                .commitAllowingStateLoss();
    }

    @Override
    public void onBackPressed() {
        if (mPresenter == null || !mPresenter.handleBackPressed()) {
            super.onBackPressed();
        }
    }
}
