package io.github.freewebmovement.igniter.activities.exempt.activity

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import io.github.freewebmovement.igniter.IgniterApplication
import io.github.freewebmovement.igniter.activities.exempt.contract.ExemptAppContract
import io.github.freewebmovement.igniter.activities.exempt.fragment.ExemptAppFragment
import io.github.freewebmovement.igniter.activities.exempt.presenter.ExemptAppPresenter

class ExemptAppActivity : AppCompatActivity() {
    private var mPresenter: ExemptAppContract.Presenter? = null
    lateinit var app: IgniterApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = FrameLayout(this)
        container.id = View.generateViewId()
        setContentView(container)
        app = application as IgniterApplication
        val fm: FragmentManager = supportFragmentManager
        val fragment = fm.findFragmentByTag(ExemptAppFragment.TAG) as? ExemptAppFragment
            ?: ExemptAppFragment.newInstance()
        mPresenter = ExemptAppPresenter(fragment, app.exemptAppDataManager)
        fm.beginTransaction()
            .replace(container.id, fragment, ExemptAppFragment.TAG)
            .commitAllowingStateLoss()
    }

    override fun onBackPressed() {
        if (mPresenter == null || !mPresenter!!.handleBackPressed()) {
            super.onBackPressed()
        }
    }
}
