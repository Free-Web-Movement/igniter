package io.github.freewebmovement.igniter.activities

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.github.freewebmovement.igniter.IgniterApplication
import io.github.freewebmovement.igniter.R
import io.github.freewebmovement.igniter.persistence.DomainRulesManager
import io.github.freewebmovement.igniter.services.ProxyService

/**
 * Shared state for the connect page. Scoped to the shell activity so the
 * fragment survives tab switches without losing the latest proxy state.
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val _proxyState = MutableLiveData<Int>(ProxyService.STATE_NONE)
    val proxyState: LiveData<Int> = _proxyState

    private val _serverSummary = MutableLiveData("")
    val serverSummary: LiveData<String> = _serverSummary

    private val _routeSummary = MutableLiveData("")
    val routeSummary: LiveData<String> = _routeSummary

    private val _domainRouteCount = MutableLiveData(0)
    val domainRouteCount: LiveData<Int> = _domainRouteCount

    data class TestResult(val serverKey: String, val connected: Boolean, val delay: Long)

    private val _testResult = MutableLiveData<TestResult?>()
    val testResult: LiveData<TestResult?> = _testResult

    var testingServerKey: String? = null
        private set

    fun setTestingServer(key: String?) {
        testingServerKey = key
    }

    fun postTestResult(connected: Boolean, delay: Long) {
        val key = testingServerKey ?: return
        _testResult.postValue(TestResult(key, connected, delay))
        testingServerKey = null
    }

    fun setProxyState(state: Int) {
        _proxyState.postValue(state)
    }

    fun refreshServer() {
        val config = IgniterApplication.getApplication().trojanConfig
        val addr = config.getRemoteAddr()
        _serverSummary.postValue(if (addr.isEmpty()) "" else "$addr:${config.getRemotePort()}")
    }

    fun refreshRoute() {
        val app = IgniterApplication.getApplication()
        val clashEnabled = app.trojanPreferences.getEnableClash()
        val server = app.trojanConfig.getRemoteAddr()
        val serverText = if (server.isEmpty())
            app.getString(R.string.home_server_unknown)
        else
            "$server:${app.trojanConfig.getRemotePort()}"
        val localPort = app.trojanConfig.getLocalPort()
        val summary = if (clashEnabled) {
            app.getString(R.string.home_route_clash, app.clashConfig.getMode(), localPort, serverText)
        } else {
            app.getString(R.string.home_route_direct, localPort, serverText)
        }
        _routeSummary.postValue(summary)
        _domainRouteCount.postValue(DomainRulesManager(app).getRules().size)
    }
}
