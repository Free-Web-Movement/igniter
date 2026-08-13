package io.github.freewebmovement.igniter.ui.clash

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.freewebmovement.igniter.R

private val Teal = Color(0xFF008577)

/** YAML editor for the Clash config with load / reset / save actions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClashEditorScreen(
    text: String,
    onTextChange: (String) -> Unit,
    onLoad: () -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.main_menu_action_clash_editor_file_editor)) },
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
            Row(modifier = Modifier.padding(8.dp)) {
                Button(onClick = onLoad, modifier = Modifier.padding(end = 4.dp)) {
                    Text(stringResource(R.string.clash_editor_load))
                }
                Button(onClick = onReset, modifier = Modifier.padding(end = 4.dp)) {
                    Text(stringResource(R.string.clash_editor_reset))
                }
                Button(onClick = onSave) {
                    Text(stringResource(R.string.clash_editor_save))
                }
            }
            TextField(
                value = text,
                onValueChange = onTextChange,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}
