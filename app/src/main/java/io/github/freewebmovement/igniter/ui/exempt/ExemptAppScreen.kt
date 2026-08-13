package io.github.freewebmovement.igniter.ui.exempt

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import io.github.freewebmovement.igniter.R
import io.github.freewebmovement.igniter.activities.exempt.contract.AppTab
import io.github.freewebmovement.igniter.persistence.data.AppInfo

private val Teal = Color(0xFF008577)
private val UnselectedTab = Color(0xFFE0E0E0)
private val SwitchUncheckedTrack = Color(0xFFE0E0E0)
private val SwitchUncheckedThumb = Color(0xFF757575)
private val SwitchUncheckedBorder = Color(0xFFBDBDBD)

@Composable
private fun appSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = Teal,
    checkedBorderColor = Teal,
    uncheckedThumbColor = SwitchUncheckedThumb,
    uncheckedTrackColor = SwitchUncheckedTrack,
    uncheckedBorderColor = SwitchUncheckedBorder
)

/** Apps that should go through the proxy: search, user/system tabs, bulk actions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExemptAppScreen(
    apps: List<AppInfo>,
    onSearchChange: (String) -> Unit,
    onTabSelected: (AppTab) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onSave: () -> Unit,
    onToggle: (AppInfo, Int, Boolean) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf(AppTab.NORMAL) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_apps)) },
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
                text = stringResource(R.string.exempt_app_page_desc),
                fontSize = 12.sp,
                color = Color(0xFF757575),
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 4.dp)
            )
            TextField(
                value = search,
                onValueChange = {
                    search = it
                    onSearchChange(it)
                },
                placeholder = { Text(stringResource(R.string.proxy_search_hint)) },
                singleLine = true,
                maxLines = 1,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Teal,
                    unfocusedIndicatorColor = Color(0xFFBDBDBD)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 8.dp, end = 16.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 8.dp, end = 16.dp)
            ) {
                TabChip(
                    text = stringResource(R.string.proxy_tab_normal),
                    selected = tab == AppTab.NORMAL,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        tab = AppTab.NORMAL
                        onTabSelected(AppTab.NORMAL)
                    }
                )
                TabChip(
                    text = stringResource(R.string.proxy_tab_system),
                    selected = tab == AppTab.SYSTEM,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        tab = AppTab.SYSTEM
                        onTabSelected(AppTab.SYSTEM)
                    }
                )
                TabChip(
                    text = stringResource(R.string.proxy_tab_international),
                    selected = tab == AppTab.INTERNATIONAL,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        tab = AppTab.INTERNATIONAL
                        onTabSelected(AppTab.INTERNATIONAL)
                    }
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 8.dp, end = 16.dp)
            ) {
                PolicyButton(stringResource(R.string.proxy_select_all), onSelectAll)
                PolicyButton(stringResource(R.string.proxy_deselect_all), onDeselectAll)
                Box(modifier = Modifier.weight(1f))
                PolicyButton(stringResource(R.string.proxy_save), onSave)
            }
            HorizontalDivider(
                color = Color(0xFFE0E0E0),
                modifier = Modifier.padding(top = 8.dp)
            )
            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(apps) { index, app ->
                    AppRow(app = app, position = index, onToggle = onToggle)
                    if (index < apps.lastIndex) {
                        HorizontalDivider(color = Color(0xFFE0E0E0))
                    }
                }
            }
        }
    }
}

@Composable
private fun TabChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) Teal else UnselectedTab)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color(0xFF212121),
            fontSize = 14.sp
        )
    }
}

@Composable
private fun PolicyButton(text: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(UnselectedTab)
            .clickable(onClick = onClick)
            .padding(start = 12.dp, top = 6.dp, end = 12.dp, bottom = 6.dp)
    ) {
        Text(text = text, color = Color(0xFF212121), fontSize = 13.sp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppRow(
    app: AppInfo,
    position: Int,
    onToggle: (AppInfo, Int, Boolean) -> Unit
) {
    val context = LocalContext.current
    val icon = remember(app.icon) { app.icon?.toBitmap()?.asImageBitmap() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = {
                    Toast.makeText(
                        context,
                        "${app.appName}\n${app.packageName}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
            .padding(horizontal = 15.dp, vertical = 5.dp)
    ) {
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
        } else {
            Box(modifier = Modifier.size(40.dp))
        }
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(text = app.appName.orEmpty(), fontSize = 18.sp)
            Text(text = app.packageName.orEmpty(), fontSize = 8.sp)
        }
        Box(modifier = Modifier.weight(1f))
        Switch(
            checked = app.enabled,
            onCheckedChange = { onToggle(app, position, it) },
            colors = appSwitchColors()
        )
    }
}
