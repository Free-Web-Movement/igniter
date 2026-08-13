package io.github.freewebmovement.igniter.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.freewebmovement.igniter.R
import io.github.freewebmovement.igniter.activities.MainActivity
import io.github.freewebmovement.igniter.common.dialog.AppSheetOverlay

private val Teal = Color(0xFF008577)

/**
 * Compose shell for the four-tab layout: content area hosting the tab
 * fragments plus the AppSheet overlay, and the bottom navigation bar.
 */
@Composable
fun MainShell(
    currentTab: Int,
    onTabSelected: (Int) -> Unit,
    sheetContent: (@Composable () -> Unit)?,
    sheetDismissOnOutsideTap: Boolean,
    onDismissSheet: () -> Unit,
    fragmentContent: @Composable () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                fragmentContent()
                sheetContent?.let { content ->
                    AppSheetOverlay(
                        dismissOnOutsideTap = sheetDismissOnOutsideTap,
                        onDismiss = onDismissSheet,
                        content = content
                    )
                }
            }
            NavigationBar(containerColor = Color.White) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    MainTabItem(
                        selected = currentTab == MainActivity.TAB_HOME,
                        onClick = { onTabSelected(MainActivity.TAB_HOME) },
                        iconRes = R.drawable.ic_action_server_list,
                        label = stringResource(R.string.tab_home),
                        modifier = Modifier.weight(1f)
                    )
                    MainTabItem(
                        selected = currentTab == MainActivity.TAB_APPS,
                        onClick = { onTabSelected(MainActivity.TAB_APPS) },
                        iconRes = R.drawable.ic_action_apps,
                        label = stringResource(R.string.tab_apps),
                        modifier = Modifier.weight(1f)
                    )
                    MainTabItem(
                        selected = currentTab == MainActivity.TAB_RULES,
                        onClick = { onTabSelected(MainActivity.TAB_RULES) },
                        iconRes = R.drawable.ic_action_editor,
                        label = stringResource(R.string.tab_rules),
                        modifier = Modifier.weight(1f)
                    )
                    MainTabItem(
                        selected = currentTab == MainActivity.TAB_SETTINGS,
                        onClick = { onTabSelected(MainActivity.TAB_SETTINGS) },
                        iconRes = R.drawable.ic_action_settings,
                        label = stringResource(R.string.tab_settings),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.MainTabItem(
    selected: Boolean,
    onClick: () -> Unit,
    iconRes: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(painterResource(iconRes), contentDescription = label) },
        label = { Text(label) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color.White,
            selectedTextColor = Teal,
            indicatorColor = Teal,
            unselectedIconColor = Color(0xFF757575),
            unselectedTextColor = Color(0xFF757575)
        ),
        modifier = modifier
    )
}
