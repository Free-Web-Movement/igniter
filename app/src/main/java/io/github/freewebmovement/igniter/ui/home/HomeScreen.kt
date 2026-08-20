package io.github.freewebmovement.igniter.ui.home

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.freewebmovement.igniter.IgniterApplication
import io.github.freewebmovement.igniter.R
import io.github.freewebmovement.igniter.common.util.QrCodeUtils
import io.github.freewebmovement.igniter.connection.ServerPingManager
import io.github.freewebmovement.igniter.persistence.database.Server
import io.github.freewebmovement.igniter.services.ProxyService

private val ColorStatusStarted = Color(0xFF2E7D32)
private val ColorStatusTransition = Color(0xFFF57F17)
private val ColorStatusStopped = Color(0xFF757575)

private val ColorPingExcellent = Color(0xFF2E7D32)
private val ColorPingGood = Color(0xFF00897B)
private val ColorPingFair = Color(0xFFF57C00)
private val ColorPingPoor = Color(0xFFC62828)
private val ColorPingUnreachable = Color(0xFF757575)

/**
 * Compose port of [io.github.freewebmovement.igniter.activities.HomeFragment]:
 * the proxy status card, connect/test buttons, route summary and server list.
 */
@Composable
fun HomeScreen(
    proxyState: Int,
    serverSummary: String,
    routeSummary: String,
    domainRouteCount: Int,
    servers: List<Server>,
    pingData: Map<String, ServerPingManager.PingInfo>,
    testResults: Map<String, Pair<Boolean, Long>>,
    currentHost: String,
    currentPort: Int,
    modifier: Modifier = Modifier,
    onConnectClick: () -> Unit,
    onDomainRouteClick: () -> Unit,
    onAddServerClick: () -> Unit,
    onServerSelected: (Server) -> Unit,
    onServerPlay: (Server) -> Unit,
    onServerStop: (Server) -> Unit,
    onServerTest: (Server) -> Unit,
    onServerEdit: (Server) -> Unit,
    onServerDelete: (Server) -> Unit
) {
    val context = LocalContext.current
    var qrServer by remember { mutableStateOf<Server?>(null) }
    var moreServer by remember { mutableStateOf<Server?>(null) }
    var deleteServer by remember { mutableStateOf<Server?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.home_page_desc),
            fontSize = 12.sp,
            color = Color(0xFF757575),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        HomeStatusCard(
            state = proxyState,
            summary = when {
                servers.isEmpty() -> stringResource(R.string.home_no_server_info)
                serverSummary.isEmpty() -> stringResource(R.string.home_server_unknown)
                else -> serverSummary
            }
        )

        if (servers.isEmpty()) {
            HomeEmptyState(onAddServerClick = onAddServerClick)
        } else {
            Button(
                onClick = onConnectClick,
                enabled = proxyState != ProxyService.STOPPING,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008577)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
                    .height(48.dp)
            ) {
                Text(
                    text = when (proxyState) {
                        ProxyService.STARTING, ProxyService.STARTED, ProxyService.STOPPING ->
                            stringResource(R.string.home_btn_stop)
                        else -> stringResource(R.string.home_btn_start)
                    },
                    fontSize = 20.sp
                )
            }

            HomeRouteCard(
                routeSummary = routeSummary,
                domainRouteCount = domainRouteCount,
                onDomainRouteClick = onDomainRouteClick,
                modifier = Modifier.padding(top = 20.dp)
            )

            HomeServerSection(
                servers = servers,
                pingData = pingData,
                testResults = testResults,
                currentHost = currentHost,
                currentPort = currentPort,
                running = proxyState == ProxyService.STARTED,
                onAddServerClick = onAddServerClick,
                onServerSelected = onServerSelected,
                onServerPlay = onServerPlay,
                onServerStop = onServerStop,
                onServerTest = onServerTest,
                onServerMore = { moreServer = it },
                modifier = Modifier.padding(top = 20.dp)
            )
        }
    }

    moreServer?.let { server ->
        HomeServerMoreSheet(
            onDismiss = { moreServer = null },
            onQr = { moreServer = null; qrServer = server },
            onEdit = { moreServer = null; onServerEdit(server) },
            onDelete = { moreServer = null; deleteServer = server }
        )
    }

    qrServer?.let { server ->
        ServerQrDialog(server = server, onDismiss = { qrServer = null })
    }

    deleteServer?.let { server ->
        AlertDialog(
            onDismissRequest = { deleteServer = null },
            title = { Text(stringResource(R.string.warning_delete_server)) },
            text = { Text(stringResource(R.string.warngin_delete_server_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    deleteServer = null
                    onServerDelete(server)
                }) { Text(stringResource(android.R.string.yes)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteServer = null }) {
                    Text(stringResource(android.R.string.no))
                }
            }
        )
    }
}

@Composable
private fun HomeStatusCard(state: Int, summary: String) {
    val (title, color) = when (state) {
        ProxyService.STARTED -> stringResource(R.string.home_status_started) to ColorStatusStarted
        ProxyService.STARTING -> stringResource(R.string.home_status_starting) to ColorStatusTransition
        ProxyService.STOPPING -> stringResource(R.string.home_status_stopping) to ColorStatusTransition
        else -> stringResource(R.string.home_status_stopped) to ColorStatusStopped
    }
    HomeCard {
        Text(
            text = title,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Text(
            text = summary,
            fontSize = 14.sp,
            color = Color(0xFF616161),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun HomeEmptyState(onAddServerClick: () -> Unit) {
    HomeCard(modifier = Modifier.padding(top = 20.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.home_empty_no_server),
                fontSize = 16.sp,
                color = Color(0xFF757575),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                text = stringResource(R.string.home_empty_add_prompt),
                fontSize = 13.sp,
                color = Color(0xFF9E9E9E),
                modifier = Modifier.padding(top = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Button(
                onClick = onAddServerClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008577)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
                    .height(48.dp)
            ) {
                Text(
                    text = stringResource(R.string.home_add_server),
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun HomeRouteCard(
    routeSummary: String,
    domainRouteCount: Int,
    onDomainRouteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    HomeCard(modifier) {
        Text(
            text = stringResource(R.string.home_route_title),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF212121)
        )
        Text(
            text = routeSummary,
            fontSize = 13.sp,
            color = Color(0xFF616161),
            modifier = Modifier.padding(top = 8.dp)
        )
        HorizontalDivider(
            color = Color(0xFFE0E0E0),
            modifier = Modifier.padding(top = 12.dp)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onDomainRouteClick)
                .padding(top = 12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_domain_route_title),
                    fontSize = 14.sp,
                    color = Color(0xFF212121)
                )
                Text(
                    text = stringResource(R.string.home_domain_route_count, domainRouteCount),
                    fontSize = 12.sp,
                    color = Color(0xFF9E9E9E),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Text(
                text = stringResource(R.string.home_manage_domain_route),
                fontSize = 13.sp,
                color = Color(0xFF1565C0)
            )
        }
    }
}

@Composable
private fun HomeServerSection(
    servers: List<Server>,
    pingData: Map<String, ServerPingManager.PingInfo>,
    testResults: Map<String, Pair<Boolean, Long>>,
    currentHost: String,
    currentPort: Int,
    running: Boolean,
    onAddServerClick: () -> Unit,
    onServerSelected: (Server) -> Unit,
    onServerPlay: (Server) -> Unit,
    onServerStop: (Server) -> Unit,
    onServerTest: (Server) -> Unit,
    onServerMore: (Server) -> Unit,
    modifier: Modifier = Modifier
) {
    HomeCard(modifier) {
        Text(
            text = stringResource(R.string.home_server_section),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF212121)
        )
        OutlinedButton(
            onClick = onAddServerClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text(stringResource(R.string.home_add_server))
        }
        servers.forEach { server ->
            val key = "${server.hostname}:${server.port}"
            ServerCard(
                server = server,
                pingText = formatPingText(server, pingData),
                pingColor = formatPingColor(server, pingData),
                testResult = testResults[key],
                isCurrent = server.hostname == currentHost && server.port == currentPort,
                isActive = running && server.hostname == currentHost && server.port == currentPort,
                onSelect = { onServerSelected(server) },
                onPlay = { onServerPlay(server) },
                onStop = { onServerStop(server) },
                onTest = { onServerTest(server) },
                onMore = { onServerMore(server) },
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun ServerCard(
    server: Server,
    pingText: String,
    pingColor: Color,
    testResult: Pair<Boolean, Long>?,
    isCurrent: Boolean,
    isActive: Boolean,
    onSelect: () -> Unit,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    onTest: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(start = 15.dp, top = 6.dp, end = 6.dp, bottom = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = server.hostname,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (pingText.isNotEmpty()) {
                Text(
                    text = pingText,
                    fontSize = 12.sp,
                    color = pingColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFE3F2FD), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.server_list_current),
                        fontSize = 11.sp,
                        color = Color(0xFF1565C0)
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            IconButton(
                onClick = onPlay,
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (isActive) Color(0xFF008577).copy(alpha = 0.15f) else Color.Transparent,
                        RoundedCornerShape(22.dp)
                    )
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_action_play),
                    contentDescription = stringResource(R.string.server_list_play),
                    colorFilter = if (isActive) ColorFilter.tint(Color(0xFF008577)) else null,
                    alpha = 1f
                )
            }
            IconButton(
                onClick = onStop,
                enabled = isActive,
                modifier = Modifier.size(44.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_action_stop),
                    contentDescription = stringResource(R.string.server_list_stop),
                    alpha = if (isActive) 1f else 0.4f
                )
            }
            if (isActive) {
                IconButton(
                    onClick = onTest,
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFF1565C0).copy(alpha = 0.1f), RoundedCornerShape(22.dp))
                ) {
                    Text(
                        text = "G",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1565C0)
                    )
                }
            }
            IconButton(
                onClick = onMore,
                modifier = Modifier.size(44.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_more_vert),
                    contentDescription = stringResource(R.string.server_list_more)
                )
            }
        }
        Text(
            text = "trojan://${server.password}@${server.hostname}:${server.port}",
            fontSize = 12.sp,
            color = Color(0xFF757575),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp)
        )
        if (testResult != null) {
            val (success, delay) = testResult
            Text(
                text = if (success) {
                    stringResource(R.string.server_test_success, delay)
                } else {
                    stringResource(R.string.server_test_failed)
                },
                fontSize = 12.sp,
                color = if (success) ColorPingExcellent else ColorPingPoor,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun HomeCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = Color(0xFFF5F5F5),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content = content
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeServerMoreSheet(
    onDismiss: () -> Unit,
    onQr: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            MoreSheetItem(stringResource(R.string.server_list_qr), onQr)
            MoreSheetItem(stringResource(R.string.server_list_edit), onEdit)
            MoreSheetItem(stringResource(R.string.server_list_delete_btn), onDelete)
        }
    }
}

@Composable
private fun MoreSheetItem(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        fontSize = 16.sp,
        color = Color(0xFF212121),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    )
}

@Composable
private fun ServerQrDialog(server: Server, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val uri = "trojan://${server.password}@${server.hostname}:${server.port}"
    val bitmap = remember(uri) { QrCodeUtils.generateQrBitmap(uri) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.qr_dialog_title)) },
        text = {
            if (bitmap == null) {
                Text(
                    text = stringResource(R.string.qr_generate_failed),
                    color = Color(0xFFC62828)
                )
            } else {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.qr_dialog_title),
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, uri)
                }
                context.startActivity(
                    Intent.createChooser(send, context.getString(R.string.qr_share_uri))
                )
            }) { Text(stringResource(R.string.qr_share_action)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

private fun formatPingText(
    server: Server,
    pingData: Map<String, ServerPingManager.PingInfo>
): String {
    val info = pingData["${server.hostname}:${server.port}"] ?: return ""
    val current = info.currentMs
    val avg = info.avgMs
    if (current == null || avg == null) {
        return stringResourceValue(R.string.server_ping_unreachable)
    }
    return stringResourceValue(R.string.server_ping_format, current, avg)
}

private fun formatPingColor(
    server: Server,
    pingData: Map<String, ServerPingManager.PingInfo>
): Color {
    val info = pingData["${server.hostname}:${server.port}"] ?: return ColorPingUnreachable
    return when (info.connectivity) {
        ServerPingManager.Connectivity.EXCELLENT -> ColorPingExcellent
        ServerPingManager.Connectivity.GOOD -> ColorPingGood
        ServerPingManager.Connectivity.FAIR -> ColorPingFair
        ServerPingManager.Connectivity.POOR -> ColorPingPoor
        ServerPingManager.Connectivity.UNREACHABLE -> ColorPingUnreachable
    }
}

private fun stringResourceValue(id: Int, vararg args: Any): String {
    return IgniterApplication.getApplication().getString(id, *args)
}
