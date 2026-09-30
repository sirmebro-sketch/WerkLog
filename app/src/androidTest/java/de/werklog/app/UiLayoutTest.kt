package de.werklog.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class UiLayoutTest {
    @get:Rule val ui = createComposeRule()
    @Test fun allTilesHaveEqualDimensionsAndReadableLabelsAtLargeFontScale() {
        val fontScale = mutableFloatStateOf(1f)
        val theme = mutableStateOf(WerkTheme.PETROL)
        var opened: String? = null
        ui.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale.floatValue)) {
                WerkLogTheme(theme.value) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
                            Box(Modifier.width(320.dp)) { OperationTiles { opened = it } }
                        }
                    }
                }
            }
        }
        val names = listOf("Anlagen", "Arbeitsprotokoll", "Zähler", "Rundgang", "Anleitungen", "Bestellungen", "Kalender", "Adressbuch", "Textvorlagen")
        val hints = listOf("Technik & Wissen", "Störungen & Arbeiten", "Ablesen & Verlauf", "Prüfen & abhaken", "Schritt für Schritt", "Teile & Ausrüstung", "Termine & Firmen", "Kontakte & Aufgaben", "Texte wiederverwenden")
        for (concept in WerkTheme.entries) for (scale in listOf(1f, 1.8f)) {
            ui.runOnIdle { theme.value = concept; fontScale.floatValue = scale }
            ui.waitForIdle()
            var expectedWidth: Float? = null; var expectedHeight: Float? = null
            for ((index, name) in names.withIndex()) {
                val tile = ui.onNodeWithTag("operation-tile-$name").performScrollTo().assertIsDisplayed().assertHasClickAction()
                val bounds = tile.fetchSemanticsNode().boundsInRoot
                if (expectedWidth == null) { expectedWidth = bounds.width; expectedHeight = bounds.height }
                assertEquals("Tile width: $name / $scale", expectedWidth!!, bounds.width, 1.1f)
                assertEquals("Tile height: $name / $scale", expectedHeight!!, bounds.height, 1.1f)
                for (text in listOf(name, hints[index])) {
                    val layouts = mutableListOf<TextLayoutResult>()
                    ui.onNodeWithText(text, useUnmergedTree = true).assertIsDisplayed()
                        .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action -> action(layouts) }
                    val layout = layouts.single()
                    assertFalse("Clipped label: $text / $scale / ${layout.size} / ${layout.multiParagraph.width} x ${layout.multiParagraph.height}", layout.hasVisualOverflow)
                }
                tile.performClick()
                ui.runOnIdle { assertEquals(name, opened) }
            }
            ui.onNodeWithTag("operation-tile-Anlagen").performScrollTo()
            ui.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
                val folder = File(requireNotNull(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")))
                File(folder, if (scale == 1f) "raster-${concept.id}.png" else "raster-${concept.id}-grosse-schrift.png").apply { parentFile?.mkdirs() }
                    .outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            }
        }
    }
}
