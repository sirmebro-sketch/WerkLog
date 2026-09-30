package de.werklog.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp

private data class OperationTile(val name: String, val hint: String, val icon: ImageVector)
private val operationTiles = listOf(
    OperationTile("Anlagen", "Technik & Wissen", Icons.Outlined.PrecisionManufacturing),
    OperationTile("Arbeitsprotokoll", "Störungen & Arbeiten", Icons.Outlined.Assignment),
    OperationTile("Zähler", "Ablesen & Verlauf", Icons.Outlined.Speed),
    OperationTile("Rundgang", "Prüfen & abhaken", Icons.Outlined.Checklist),
    OperationTile("Anleitungen", "Schritt für Schritt", Icons.Outlined.MenuBook),
    OperationTile("Bestellungen", "Teile & Ausrüstung", Icons.Outlined.ShoppingCart),
    OperationTile("Kalender", "Termine & Firmen", Icons.Outlined.CalendarMonth),
    OperationTile("Adressbuch", "Kontakte & Aufgaben", Icons.Outlined.Contacts),
    OperationTile("Textvorlagen", "Texte wiederverwenden", Icons.Outlined.ContentCopy))

/** One shared height and text slots for the entire grid, including the final incomplete row. */
@Composable fun OperationTiles(open: (String) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val columns = when {
            maxWidth < 300.dp || (density.fontScale >= 1.5f && maxWidth < 520.dp) -> 1
            maxWidth >= 680.dp -> 3
            else -> 2
        }
        val gap = 10.dp
        val width = (maxWidth - gap * (columns - 1)) / columns
        val textWidth = with(density) { (width - 24.dp).toPx().toInt().coerceAtLeast(1) }
        val measurer = rememberTextMeasurer()
        val titleStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        val hintStyle = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.Center)
        val titlePixels = operationTiles.maxOf { measurer.measure(it.name, style = titleStyle, constraints = Constraints(maxWidth = textWidth)).size.height }
        val hintPixels = operationTiles.maxOf { measurer.measure(it.hint, style = hintStyle, constraints = Constraints(maxWidth = textWidth)).size.height }
        val titleHeight = with(density) { titlePixels.toDp() + 2.dp }
        val hintHeight = with(density) { hintPixels.toDp() + 2.dp }
        val height = (24.dp + 30.dp + 8.dp + titleHeight + 4.dp + hintHeight).coerceAtLeast(112.dp)
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            operationTiles.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEach { tile ->
                        Card(onClick = { open(tile.name) },
                            modifier = Modifier.weight(1f).height(height).testTag("operation-tile-${tile.name}")
                                .semantics(mergeDescendants = true) { role = Role.Button },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, Mint.copy(alpha = .14f))) {
                            Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center) {
                                Icon(tile.icon, null, tint = Mint, modifier = Modifier.size(30.dp))
                                Spacer(Modifier.height(8.dp))
                                Box(Modifier.fillMaxWidth().height(titleHeight), contentAlignment = Alignment.Center) {
                                    Text(tile.name, style = titleStyle)
                                }
                                Spacer(Modifier.height(4.dp))
                                Box(Modifier.fillMaxWidth().height(hintHeight), contentAlignment = Alignment.Center) {
                                    Text(tile.hint, style = hintStyle, color = Muted)
                                }
                            }
                        }
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
