package io.github.freewebmovement.igniter.ui.server

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.freewebmovement.igniter.R

private val Teal = Color(0xFF008577)

/** Add / edit a trojan server: quick paste or scan, manual fields and QR sharing. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServerScreen(
    uri: String,
    host: String,
    port: String,
    localPort: String,
    password: String,
    onUriChange: (String) -> Unit,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onLocalPortChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onScanQr: () -> Unit,
    onGenerateQr: (String) -> Unit,
    onSave: (String, String, String, String) -> Unit,
    onBack: () -> Unit
) {
    val trojanUri = if (host.isNotEmpty() && port.isNotEmpty() && password.isNotEmpty()) {
        "trojan://$password@$host:$port"
    } else {
        ""
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_add_server)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Teal,
                    titleContentColor = Color.White
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp)
            ) {
                TextField(
                    value = uri,
                    onValueChange = onUriChange,
                    placeholder = { Text(stringResource(R.string.quick_add_server_uri_hint)) },
                    singleLine = true,
                    colors = fieldColors(),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onScanQr,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(stringResource(R.string.scan_qr_code))
                }
            }
            TextField(
                value = host,
                onValueChange = onHostChange,
                placeholder = { Text(stringResource(R.string.remote_address)) },
                singleLine = true,
                colors = fieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 12.dp, end = 16.dp)
            )
            TextField(
                value = port,
                onValueChange = onPortChange,
                label = { Text(stringResource(R.string.remote_port)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = fieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 12.dp, end = 16.dp)
            )
            TextField(
                value = password,
                onValueChange = onPasswordChange,
                placeholder = { Text(stringResource(R.string.remote_password)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                colors = fieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 12.dp, end = 16.dp)
            )
            TextField(
                value = localPort,
                onValueChange = onLocalPortChange,
                label = { Text(stringResource(R.string.local_port)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = fieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 12.dp, end = 16.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 16.dp, end = 20.dp)
            ) {
                Text(
                    text = stringResource(R.string.verify_certificate),
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = true, onCheckedChange = {})
            }
            HorizontalDivider(
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 12.dp, end = 16.dp)
            ) {
                TextField(
                    value = trojanUri,
                    onValueChange = {},
                    placeholder = { Text(stringResource(R.string.trojan_uri)) },
                    singleLine = true,
                    readOnly = true,
                    colors = fieldColors(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { if (trojanUri.isNotEmpty()) onGenerateQr(trojanUri) },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(stringResource(R.string.qr_generate_button))
                }
            }
            Button(
                onClick = { onSave(host, port, password, localPort) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp)
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}

@Composable
private fun fieldColors() = TextFieldDefaults.colors(
    focusedIndicatorColor = Teal,
    unfocusedIndicatorColor = Color(0xFFBDBDBD)
)
