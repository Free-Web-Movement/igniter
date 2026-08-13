package io.github.freewebmovement.igniter.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.freewebmovement.igniter.R
import io.github.freewebmovement.igniter.connection.ServerPingManager
import io.github.freewebmovement.igniter.persistence.database.Server

/** Home tab scaffold: teal top bar with overflow actions + the server page. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    proxyState: Int,
    serverSummary: String,
    routeSummary: String,
    domainRouteCount: Int,
    servers: List<Server>,
    pingData: Map<String, ServerPingManager.PingInfo>,
    currentHost: String,
    currentPort: Int,
    onTestConnection: () -> Unit,
    onImportFromFile: () -> Unit,
    onConnectClick: () -> Unit,
    onTestClick: () -> Unit,
    onDomainRouteClick: () -> Unit,
    onAddServerClick: () -> Unit,
    onServerSelected: (Server) -> Unit,
    onServerPlay: (Server) -> Unit,
    onServerStop: (Server) -> Unit,
    onServerEdit: (Server) -> Unit,
    onServerDelete: (Server) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                painterResource(R.drawable.ic_more_vert),
                                contentDescription = null
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(stringResource(R.string.main_menu_action_test_connection))
                                },
                                onClick = {
                                    menuExpanded = false
                                    onTestConnection()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(stringResource(R.string.server_list_menu_import_from_file))
                                },
                                onClick = {
                                    menuExpanded = false
                                    onImportFromFile()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF008577),
                    titleContentColor = Color.White
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { padding ->
        HomeScreen(
            proxyState = proxyState,
            serverSummary = serverSummary,
            routeSummary = routeSummary,
            domainRouteCount = domainRouteCount,
            servers = servers,
            pingData = pingData,
            currentHost = currentHost,
            currentPort = currentPort,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            onConnectClick = onConnectClick,
            onTestClick = onTestClick,
            onDomainRouteClick = onDomainRouteClick,
            onAddServerClick = onAddServerClick,
            onServerSelected = onServerSelected,
            onServerPlay = onServerPlay,
            onServerStop = onServerStop,
            onServerEdit = onServerEdit,
            onServerDelete = onServerDelete
        )
    }
}
