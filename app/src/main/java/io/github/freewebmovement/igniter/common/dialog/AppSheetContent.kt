package io.github.freewebmovement.igniter.common.dialog

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SheetCardShape = RoundedCornerShape(15.dp)
private val SheetTextPrimary = Color(0xFF212121)
private val SheetTextSecondary = Color(0xFF616161)
private val SheetAccent = Color(0xFF008577)
private val SheetBorder = Color(0xFFE0E0E0)

/** Replicates res/layout/sheet_dialog.xml: title, message, optional content and a button row. */
@Composable
internal fun AppSheetDialog(
    title: String?,
    message: String?,
    content: (@Composable () -> Unit)?,
    neutral: SheetButton?,
    negative: SheetButton?,
    positive: SheetButton?,
    onButtonClick: () -> Unit
) {
    Surface(
        shape = SheetCardShape,
        color = Color.White,
        modifier = Modifier.widthIn(min = 168.dp, max = 300.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            if (!title.isNullOrEmpty()) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = SheetTextPrimary
                )
            }
            if (!message.isNullOrEmpty()) {
                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = SheetTextSecondary,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            if (content != null) {
                Box(modifier = Modifier.padding(top = 8.dp)) { content() }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                neutral?.let { SheetButtonView(it, onButtonClick) }
                negative?.let { SheetButtonView(it, onButtonClick) }
                positive?.let { SheetButtonView(it, onButtonClick) }
            }
        }
    }
}

@Composable
private fun SheetButtonView(button: SheetButton, onButtonClick: () -> Unit) {
    val text = button.text
    if (text.isNullOrEmpty()) {
        return
    }
    Text(
        text = text,
        fontSize = 14.sp,
        color = SheetAccent,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable {
                onButtonClick()
                button.onClick?.invoke()
            }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    )
}

/** Replicates res/layout/sheet_menu.xml + item_sheet_menu.xml: a vertical list of actions. */
@Composable
internal fun AppSheetMenu(items: List<Pair<String, () -> Unit>>, onItemClick: () -> Unit) {
    Surface(
        shape = SheetCardShape,
        color = Color.White,
        modifier = Modifier.width(220.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            items.forEach { (label, action) ->
                Text(
                    text = label,
                    fontSize = 14.sp,
                    color = SheetTextPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            onItemClick()
                            action()
                        }
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }
        }
    }
}

/**
 * Replicates res/layout/dialog_loading.xml: a determinate teal progress bar
 * animating 0 -> 100% over 800ms, signalling [onAnimationDone] on completion.
 */
@Composable
internal fun AppSheetLoading(message: String, onAnimationDone: () -> Unit) {
    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = LinearEasing)
        ) { value, _ ->
            progress = value
        }
        onAnimationDone()
    }
    Surface(
        shape = SheetCardShape,
        color = Color.White,
        border = BorderStroke(1.dp, SheetBorder),
        shadowElevation = 8.dp,
        modifier = Modifier.widthIn(min = 168.dp)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(
                progress = { progress },
                color = SheetAccent,
                trackColor = SheetBorder,
                modifier = Modifier.width(168.dp)
            )
            Text(
                text = message,
                fontSize = 14.sp,
                color = SheetTextSecondary,
                textAlign = TextAlign.Center,
                maxLines = 3,
                modifier = Modifier
                    .padding(top = 18.dp)
                    .widthIn(max = 200.dp)
            )
        }
    }
}

/** Full-screen dimming scrim that hosts a centered sheet, replicating res/layout/activity_main.xml's appDialogLayer. */
@Composable
internal fun AppSheetOverlay(
    dismissOnOutsideTap: Boolean,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x66000000))
    ) {
        if (dismissOnOutsideTap) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )
        }
        Box(
            Modifier
                .align(Alignment.Center)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
        ) {
            content()
        }
    }
}
