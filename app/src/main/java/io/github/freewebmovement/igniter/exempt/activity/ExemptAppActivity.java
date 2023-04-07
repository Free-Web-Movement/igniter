package io.github.freewebmovement.igniter.exempt.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import android.view.Window;

import io.github.freewebmovement.igniter.IgniterApplication;
import io.github.freewebmovement.igniter.R;
import io.github.freewebmovement.igniter.exempt.contract.ExemptAppContract;
import io.github.freewebmovement.igniter.exempt.data.ExemptAppDataManager;
import io.github.freewebmovement.igniter.exempt.fragment.ExemptAppFragment;
import io.github.freewebmovement.igniter.exempt.presenter.ExemptAppPresenter;
import io.github.freewebmovement.igniter.persistence.Storage;

public class ExemptAppActivity extends AppCompatActivity {
    private ExemptAppContract.Presenter mPresenter;
    IgniterApplication app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_exempt_app);
        app = (IgniterApplication)getApplication();
        FragmentManager fm = getSupportFragmentManager();
        ExemptAppFragment fragment = (ExemptAppFragment) fm.findFragmentByTag(ExemptAppFragment.TAG);
        if (fragment == null) {
            fragment = ExemptAppFragment.newInstance();
        }
        mPresenter = new ExemptAppPresenter(fragment,
                new ExemptAppDataManager(app));
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
