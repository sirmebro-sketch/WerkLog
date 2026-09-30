package de.werklog.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable internal fun AppearancePanel(settings: AppearanceSettings, busy: Boolean, error: (String) -> Unit) {
    val active = LocalWerkTheme.current
    Section("Darstellung")
    Text("Farben für deinen Arbeitsalltag", style = MaterialTheme.typography.bodyMedium, color = Muted)
    Spacer(Modifier.height(12.dp))
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        WerkTheme.entries.forEach { theme ->
            val selected = active == theme
            Surface(shape = RoundedCornerShape(16.dp),
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(if (selected) 2.dp else 1.dp,
                    if (selected) Accent else MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth().testTag("theme-choice-${theme.id}")
                    .selectable(selected, enabled = !busy, role = Role.RadioButton, onClick = {
                        if (!settings.select(theme)) error("Die Farbauswahl konnte nicht gespeichert werden. Bitte erneut versuchen.")
                    })) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ThemePreview(theme)
                    Column(Modifier.weight(1f)) {
                        Text(theme.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(theme.description, style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                    RadioButton(selected, onClick = null, enabled = !busy, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
    Row(Modifier.fillMaxWidth().padding(top = 12.dp).testTag("theme-follow-system")
        .toggleable(settings.choice.followSystem, enabled = !busy, role = Role.Switch, onValueChange = {
            if (!settings.followSystem(it)) error("Die Systemeinstellung konnte nicht gespeichert werden. Bitte erneut versuchen.")
        }).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text("Geräteeinstellung folgen", fontWeight = FontWeight.Medium)
            Text("Hell: Tageslicht · Dunkel: ${settings.choice.night.title}", style = MaterialTheme.typography.bodySmall, color = Muted)
        }
        Switch(settings.choice.followSystem, onCheckedChange = null, enabled = !busy)
    }
    Hint("Aktiv: ${active.title}. Deine Auswahl gilt auch am Sperrbildschirm. Ein Farbwechsel schließt keine geöffneten Formulare.")
}

/** A small sample uses the candidate's own background, card, contour and action colors. */
@Composable private fun ThemePreview(theme: WerkTheme) {
    val p = theme.palette
    Surface(Modifier.size(width = 58.dp, height = 56.dp), shape = RoundedCornerShape(9.dp),
        color = p.background.color, border = BorderStroke(1.dp, p.outline.color)) {
        Column(Modifier.padding(7.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(Modifier.width(26.dp).height(3.dp).background(p.text.color, RoundedCornerShape(2.dp)))
            Surface(Modifier.fillMaxWidth().height(17.dp), shape = RoundedCornerShape(4.dp), color = p.surface.color,
                border = BorderStroke(1.dp, p.cardOutline.color)) {
                Box(Modifier.padding(4.dp).width(16.dp).height(2.dp).background(p.muted.color, RoundedCornerShape(2.dp)))
            }
            Box(Modifier.fillMaxWidth().height(8.dp).background(p.accent.color, RoundedCornerShape(4.dp)))
        }
    }
}

@Composable internal fun statusColors(tone: StatusTone): Pair<Color, Color> {
    val p = LocalWerkTheme.current.palette
    return when (tone) {
        StatusTone.NEUTRAL -> p.raised.color to p.muted.color
        StatusTone.INFO -> p.infoContainer.color to p.info.color
        StatusTone.WARNING -> p.warningContainer.color to p.warning.color
        StatusTone.CRITICAL -> p.criticalContainer.color to p.critical.color
        StatusTone.SUCCESS -> p.successContainer.color to p.success.color
    }
}

@Composable internal fun statusColor(tone: StatusTone): Color = statusColors(tone).second

/** Labels and distinct symbols keep status understandable without relying on color. */
@Composable internal fun StatusBadge(label: String, tone: StatusTone, modifier: Modifier = Modifier) {
    val (background, foreground) = statusColors(tone)
    val icon = when (tone) {
        StatusTone.NEUTRAL -> Icons.Outlined.RadioButtonUnchecked
        StatusTone.INFO -> Icons.Outlined.PendingActions
        StatusTone.WARNING -> Icons.Outlined.WarningAmber
        StatusTone.CRITICAL -> Icons.Outlined.ErrorOutline
        StatusTone.SUCCESS -> Icons.Outlined.CheckCircle
    }
    Surface(modifier, shape = RoundedCornerShape(8.dp), color = background, contentColor = foreground) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f, fill = false))
        }
    }
}
