package io.github.freewebmovement.igniter.ui.rules

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.freewebmovement.igniter.R
import io.github.freewebmovement.igniter.persistence.DomainRulesManager

/** A single rendered rule row (status text precomputed by the fragment). */
data class RuleItem(
    val domain: String,
    val statusText: String,
    val statusColor: Color,
    val policy: String?,
    val showUnlock: Boolean,
    val showHide: Boolean = false,
    val showDelete: Boolean = false,
    val showBlock: Boolean = false,
    val isGroupHeader: Boolean = false,
    val companyName: String? = null,
    val companyDefaultPolicy: String? = null
)

private val Teal = Color(0xFF008577)
private val UnselectedTab = Color(0xFFE0E0E0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(
    items: List<RuleItem>,
    emptyHint: String,
    onSearchChange: (String) -> Unit,
    onTabSelected: (Int) -> Unit,
    onSetProxy: (String) -> Unit,
    onSetDirect: (String) -> Unit,
    onSetBlock: (String) -> Unit,
    onUnlock: (String) -> Unit,
    onHideDomain: (String) -> Unit,
    onUnhideDomain: (String) -> Unit,
    onDeleteDomain: (String) -> Unit,
    onAddDomain: () -> Unit,
    onClearDomains: () -> Unit,
    onOpenClashEditor: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf(0) }
    var menuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_rules)) },
                actions = {
                    IconButton(onClick = onAddDomain) {
                        Icon(
                            painterResource(R.drawable.ic_baseline_add_24),
                            contentDescription = stringResource(R.string.domain_monitor_add_title),
                            tint = Color.White
                        )
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                painterResource(R.drawable.ic_more_vert),
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.domain_monitor_clear)) },
                                onClick = {
                                    menuExpanded = false
                                    onClearDomains()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(stringResource(R.string.main_menu_action_clash_editor_file_editor))
                                },
                                onClick = {
                                    menuExpanded = false
                                    onOpenClashEditor()
                                }
                            )
                        }
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
        ) {
            Text(
                text = stringResource(R.string.rules_page_desc),
                fontSize = 12.sp,
                color = Color(0xFF757575),
                modifier = Modifier.padding(start = 12.dp, top = 8.dp, end = 12.dp)
            )
            Text(
                text = stringResource(R.string.domain_monitor_apply_hint),
                fontSize = 12.sp,
                color = Color(0xFF757575),
                modifier = Modifier.padding(start = 12.dp, top = 8.dp, end = 12.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, top = 10.dp, end = 12.dp)
            ) {
                RuleTab(
                    text = stringResource(R.string.domain_monitor_tab_proxy),
                    selected = tab == 0,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        tab = 0
                        onTabSelected(0)
                    }
                )
                RuleTab(
                    text = stringResource(R.string.domain_monitor_tab_direct),
                    selected = tab == 1,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        tab = 1
                        onTabSelected(1)
                    }
                )
                RuleTab(
                    text = stringResource(R.string.domain_monitor_tab_blocked),
                    selected = tab == 2,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        tab = 2
                        onTabSelected(2)
                    }
                )
                RuleTab(
                    text = stringResource(R.string.domain_monitor_tab_unreachable),
                    selected = tab == 3,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        tab = 3
                        onTabSelected(3)
                    }
                )
            }
            TextField(
                value = search,
                onValueChange = {
                    search = it
                    onSearchChange(it.lowercase())
                },
                placeholder = { Text(stringResource(R.string.domain_monitor_search_hint)) },
                singleLine = true,
                maxLines = 1,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Teal,
                    unfocusedIndicatorColor = Color(0xFFBDBDBD)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, top = 8.dp, end = 12.dp)
            )
            if (items.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Text(
                        text = emptyHint,
                        color = Color(0xFF757575),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(items) { item ->
                        if (item.isGroupHeader) {
                            GroupHeaderRow(text = item.domain)
                        } else {
                            RuleRow(
                                item = item,
                                onSetProxy = { onSetProxy(item.domain) },
                                onSetDirect = { onSetDirect(item.domain) },
                                onSetBlock = { onSetBlock(item.domain) },
                                onUnlock = { onUnlock(item.domain) },
                                onHide = { onHideDomain(item.domain) },
                                onUnhide = { onUnhideDomain(item.domain) },
                                onDelete = { onDeleteDomain(item.domain) }
                            )
                        }
                        HorizontalDivider(color = Color(0xFFE0E0E0))
                    }
                }
            }
        }
    }
}

@Composable
private fun RuleTab(
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
private fun GroupHeaderRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE8F5E9))
            .padding(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 8.dp)
    ) {
        Text(
            text = text,
            color = Color(0xFF2E7D32),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RuleRow(
    item: RuleItem,
    onSetProxy: () -> Unit,
    onSetDirect: () -> Unit,
    onSetBlock: () -> Unit,
    onUnlock: () -> Unit,
    onHide: () -> Unit,
    onUnhide: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.domain,
                color = Color(0xFF212121),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = item.statusText,
                color = item.statusColor,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        PolicyChip(
            text = stringResource(R.string.domain_monitor_btn_proxy),
            selected = item.policy == DomainRulesManager.POLICY_PROXY,
            onClick = onSetProxy
        )
        PolicyChip(
            text = stringResource(R.string.domain_monitor_btn_direct),
            selected = item.policy == DomainRulesManager.POLICY_DIRECT,
            onClick = onSetDirect,
            modifier = Modifier.padding(start = 4.dp)
        )
        if (item.showBlock) {
            PolicyChip(
                text = stringResource(R.string.domain_monitor_btn_block),
                selected = false,
                onClick = onSetBlock,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        if (item.showUnlock) {
            PolicyChip(
                text = stringResource(R.string.domain_monitor_btn_unlock),
                selected = false,
                onClick = onUnlock,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        if (item.showHide) {
            PolicyChip(
                text = stringResource(R.string.domain_monitor_btn_privacy),
                selected = false,
                onClick = onHide,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        if (item.showDelete) {
            PolicyChip(
                text = stringResource(R.string.common_delete),
                selected = false,
                onClick = onDelete,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
private fun PolicyChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) Teal else UnselectedTab)
            .clickable(onClick = onClick)
            .padding(start = 12.dp, top = 6.dp, end = 12.dp, bottom = 6.dp)
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color(0xFF212121),
            fontSize = 13.sp
        )
    }
}
