package io.github.freewebmovement.igniter.activities.exempt.activity

import android.os.Bundle
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import io.github.freewebmovement.igniter.IgniterApplication
import io.github.freewebmovement.igniter.R
import io.github.freewebmovement.igniter.activities.exempt.contract.ExemptAppContract
import io.github.freewebmovement.igniter.activities.exempt.fragment.ExemptAppFragment
import io.github.freewebmovement.igniter.activities.exempt.presenter.ExemptAppPresenter

class ExemptAppActivity : AppCompatActivity() {
    private var mPresenter: ExemptAppContract.Presenter? = null
    lateinit var app: IgniterApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_exempt_app)
        app = application as IgniterApplication
        val fm: FragmentManager = supportFragmentManager
        val fragment = fm.findFragmentByTag(ExemptAppFragment.TAG) as? ExemptAppFragment
            ?: ExemptAppFragment.newInstance()
        mPresenter = ExemptAppPresenter(fragment, app.exemptAppDataManager)
        fm.beginTransaction()
            .replace(R.id.parent_fl, fragment, ExemptAppFragment.TAG)
            .commitAllowingStateLoss()
    }

    override fun onBackPressed() {
        if (mPresenter == null || !mPresenter!!.handleBackPressed()) {
            super.onBackPressed()
        }
    }
}
