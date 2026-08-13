package io.github.freewebmovement.igniter.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.freewebmovement.igniter.R

private val Teal = Color(0xFF008577)

@Composable
private fun settingSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = Teal,
    checkedBorderColor = Teal,
    uncheckedThumbColor = Color(0xFF757575),
    uncheckedTrackColor = Color(0xFFE0E0E0),
    uncheckedBorderColor = Color(0xFFBDBDBD)
)

private val ClashLinkPattern = Regex("<a href=\"([^\"]+)\">([^<]+)</a>")

/** Renders [stringResource(R.string.label_clash)] with a tappable Clash link. */
@Composable
private fun clashLabelText(): androidx.compose.ui.text.AnnotatedString {
    val html = stringResource(R.string.label_clash)
    return remember(html) {
        val match = ClashLinkPattern.find(html) ?: return@remember buildAnnotatedString {
            append(html)
        }
        buildAnnotatedString {
            append(html.substring(0, match.range.first))
            val linkIndex = pushLink(
                LinkAnnotation.Clickable(
                    tag = match.groupValues[1],
                    styles = androidx.compose.ui.text.TextLinkStyles(
                        SpanStyle(color = Teal, textDecoration = TextDecoration.Underline)
                    ),
                    linkInteractionListener = null
                )
            )
            append(match.groupValues[2])
            pop(linkIndex)
            append(html.substring(match.range.last + 1))
        }
    }
}

/** Settings page: IPv6, Clash, LAN and start behaviour. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    ipv6Enabled: Boolean,
    clashEnabled: Boolean,
    lanEnabled: Boolean,
    autoStartEnabled: Boolean,
    bootStartEnabled: Boolean,
    onIpv6Change: (Boolean) -> Unit,
    onClashChange: (Boolean) -> Unit,
    onLanChange: (Boolean) -> Unit,
    onAutoStartChange: (Boolean) -> Unit,
    onBootStartChange: (Boolean) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_settings)) },
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
        ) {
            Text(
                text = stringResource(R.string.settings_page_desc),
                fontSize = 12.sp,
                color = Color(0xFF757575),
                modifier = Modifier.padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 4.dp)
            )
            SettingSwitch(
                text = stringResource(R.string.enable_ipv6),
                checked = ipv6Enabled,
                onCheckedChange = onIpv6Change
            )
            HorizontalDivider(color = Color(0xFFE0E0E0))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 8.dp)
            ) {
                val clashLabel = clashLabelText()
                Text(
                    text = clashLabel,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = clashEnabled, onCheckedChange = onClashChange)
            }
            HorizontalDivider(color = Color(0xFFE0E0E0))
            SettingSwitch(
                text = stringResource(R.string.enable_lan),
                checked = lanEnabled,
                onCheckedChange = onLanChange
            )
            HorizontalDivider(color = Color(0xFFE0E0E0))
            SettingSwitch(
                text = stringResource(R.string.enable_auto_start),
                checked = autoStartEnabled,
                onCheckedChange = onAutoStartChange
            )
            HorizontalDivider(color = Color(0xFFE0E0E0))
            SettingSwitch(
                text = stringResource(R.string.enable_boot_start),
                checked = bootStartEnabled,
                onCheckedChange = onBootStartChange
            )
        }
    }
}

@Composable
private fun SettingSwitch(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 8.dp)
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = settingSwitchColors())
    }
}
