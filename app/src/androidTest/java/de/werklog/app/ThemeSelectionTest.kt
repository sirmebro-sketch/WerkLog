package de.werklog.app

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.core.view.WindowCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Production settings, system bars, persistence and an unfinished form across color changes/lock. */
class ThemeSelectionTest {
    @get:Rule val ui = createEmptyComposeRule()
    private fun screenshot(name: String, expected: WerkTheme? = null, dialog: Boolean = false) {
        ui.waitForIdle()
        val bitmap = ui.onNode(if (dialog) isDialog() else isRoot()).assertIsDisplayed().captureToImage().asAndroidBitmap()
        if (expected != null) assertEquals("Actual app background: ${expected.title}", expected.palette.background.toInt(), bitmap.getPixel(2, 2))
        val folder = File(requireNotNull(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")))
        File(folder, name).apply { parentFile?.mkdirs() }.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }
    @Test fun themesApplyImmediatelyPersistAndKeepDraftsAcrossLockAndRecreation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.filesDir.listFiles()?.forEach { it.deleteRecursively() }
        for (name in listOf("appearance", "onboarding", "biometric")) context.getSharedPreferences(name, 0).edit().clear().commit()
        context.getSharedPreferences("onboarding", 0).edit().putBoolean("done-v1", true).commit()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var model: WorkModel
            scenario.onActivity { model = androidx.lifecycle.ViewModelProvider(it)[WorkModel::class.java] }
            ui.onNodeWithText("Passwort", substring = false).performTextInput("FarbenTest2026")
            ui.onNodeWithText("Passwort wiederholen").performTextInput("FarbenTest2026")
            ui.onNodeWithText("Tresor erstellen").performClick()
            ui.waitUntil(60000) { ui.onAllNodesWithText("Später").fetchSemanticsNodes().isNotEmpty() }
            ui.onNodeWithText("Später").performClick()
            ui.onNodeWithText("Einstellung", useUnmergedTree = true).performClick()
            for (theme in WerkTheme.entries) {
                ui.onNodeWithTag("theme-choice-${theme.id}").performScrollTo().performClick().assertIsSelected()
                scenario.onActivity {
                    assertEquals(theme, it.appearance.choice.selected)
                    assertFalse(it.appearance.choice.followSystem)
                    val bars = WindowCompat.getInsetsController(it.window, it.window.decorView)
                    assertEquals(!theme.isDark, bars.isAppearanceLightStatusBars)
                    assertEquals(!theme.isDark, bars.isAppearanceLightNavigationBars)
                }
                ui.onNodeWithText("Betrieb", useUnmergedTree = true).performClick()
                screenshot("betrieb-${theme.id}.png", theme)
                ui.onNodeWithText("Einstellung", useUnmergedTree = true).performClick()
            }
            ui.onNodeWithText("Darstellung").performScrollTo()
            screenshot("einstellungen-tageslicht.png", WerkTheme.TAGESLICHT)
            ui.onNodeWithTag("theme-follow-system").performScrollTo().performClick().assertIsOn()
            scenario.onActivity { assertTrue(it.appearance.choice.followSystem); assertEquals(WerkTheme.KUPFER, it.appearance.choice.night) }
            ui.onNodeWithTag("theme-choice-tageslicht").performScrollTo().performClick().assertIsSelected()
            assertFalse(context.getSharedPreferences("appearance", 0).getBoolean("system", true))
            ui.onNodeWithText("Betrieb", useUnmergedTree = true).performClick()
            ui.onNodeWithText("Anlagen", substring = false).performClick()
            ui.onNodeWithText("+ Anlage anlegen").performClick()
            screenshot("formular-tageslicht.png", dialog = true)
            ui.onNodeWithText("Anlagenname *").performTextInput("Ungespeicherte Testanlage")
            scenario.onActivity { assertTrue(it.appearance.select(WerkTheme.STAHLBLAU)) }
            ui.onNodeWithText("Ungespeicherte Testanlage", substring = false).assertExists()
            assertTrue(model.data!!.assets.isEmpty())
            scenario.onActivity { assertTrue(it.appearance.select(WerkTheme.TAGESLICHT)); model.lock() }
            ui.waitUntil(10000) { ui.onAllNodesWithText("Passwort", substring = false).fetchSemanticsNodes().isNotEmpty() }
            ui.onAllNodesWithText("Ungespeicherte Testanlage").assertCountEquals(0)
            scenario.recreate()
            ui.waitForIdle()
            scenario.onActivity {
                model = androidx.lifecycle.ViewModelProvider(it)[WorkModel::class.java]
                assertEquals(WerkTheme.TAGESLICHT, it.appearance.choice.selected)
            }
            screenshot("sperrbildschirm-tageslicht.png", WerkTheme.TAGESLICHT)
            val draft = File(context.filesDir, "ui-draft.vault")
            assertTrue(draft.isFile)
            assertFalse(draft.readText().contains("Ungespeicherte Testanlage"))
            ui.onNodeWithText("Passwort", substring = false).performTextInput("FarbenTest2026")
            ui.onNodeWithText("Passwort", substring = false).performImeAction()
            ui.waitUntil(60000) { ui.onAllNodesWithText("Ungespeicherte Testanlage", substring = false).fetchSemanticsNodes().isNotEmpty() }
            assertEquals(1, model.workspacePage.intValue)
            assertTrue(model.data!!.assets.isEmpty())
            ui.onNodeWithText("Speichern").performClick()
            ui.waitUntil(15000) { !model.busy && model.data!!.assets.singleOrNull()?.name == "Ungespeicherte Testanlage" }
            ui.onNodeWithText("Einstellung", useUnmergedTree = true).performClick()
            ui.onNodeWithTag("theme-choice-tageslicht").performScrollTo().assertIsSelected()
            // Synthetic content makes light-mode statuses, calendar and the photo viewer reviewable.
            val today = java.time.LocalDate.now()
            val dateFormat = java.time.format.DateTimeFormatter.ofPattern("dd.MM.uuuu")
            val photo = android.graphics.Bitmap.createBitmap(400, 240, android.graphics.Bitmap.Config.ARGB_8888)
            photo.eraseColor(android.graphics.Color.rgb(24, 93, 105))
            android.graphics.Canvas(photo).drawText("TESTBILD", 38f, 135f,
                android.graphics.Paint().apply { color = android.graphics.Color.WHITE; textSize = 48f; isAntiAlias = true })
            val bytes = java.io.ByteArrayOutputStream().also { photo.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, it) }.toByteArray()
            photo.recycle()
            val image = java.util.Base64.getEncoder().encodeToString(bytes)
            scenario.onActivity {
                val asset = model.data!!.assets.single().copy(name = "Prüfanlage", location = "Testbereich", nextService = today.minusDays(1).format(dateFormat), coverImage = image)
                val entries = listOf(
                    Entry(assetId = asset.id, title = "Offene Kontrolle", note = "Synthetischer Test", status = "Offen"),
                    Entry(assetId = asset.id, title = "Laufende Arbeit", note = "", status = "In Arbeit"),
                    Entry(assetId = asset.id, title = "Wichtige Rückfrage", note = "", priority = "Wichtig"),
                    Entry(assetId = asset.id, title = "Dringende Prüfung", note = "", priority = "Dringend", dueDate = today.minusDays(1).format(dateFormat)),
                    Entry(assetId = asset.id, title = "Abgeschlossene Arbeit", note = "", status = "Erledigt"))
                model.update(model.data!!.copy(assets = listOf(asset), entries = entries,
                    work = WorkData(appointments = listOf(Appointment(title = "Servicebesuch", start = formatAppointment(today.atTime(10, 0)), company = "Testfirma", responsible = "Testperson", assetId = asset.id)),
                        orders = listOf(PartsOrder(title = "Testmaterial", status = "Teilgeliefert")),
                        guides = listOf(Guide(title = "Prüfschritte", assetId = asset.id, checked = today.format(dateFormat), steps = listOf(GuideStep(title = "Vorbereitung", body = "Synthetisches Beispiel", image = image)))))))
            }
            ui.waitUntil(15000) { !model.busy && model.data!!.entries.size == 5 }
            ui.onNodeWithText("Heute", useUnmergedTree = true).performClick()
            screenshot("heute-tageslicht.png", WerkTheme.TAGESLICHT)
            ui.onNodeWithText("Betrieb", useUnmergedTree = true).performClick()
            ui.onNodeWithText("Kalender", substring = false).performScrollTo().performClick()
            screenshot("kalender-tageslicht.png", WerkTheme.TAGESLICHT)
            ui.onNodeWithText("Betrieb", useUnmergedTree = true).performClick()
            ui.onNodeWithText("Arbeitsprotokoll", substring = false).performClick()
            ui.onNodeWithText("Abgeschlossene Arbeit").performScrollTo()
            ui.onNodeWithText("NORMAL · Erledigt").assertExists()
            screenshot("arbeitsprotokoll-tageslicht.png", WerkTheme.TAGESLICHT)
            ui.onNodeWithText("Betrieb", useUnmergedTree = true).performClick()
            ui.onNodeWithText("Anleitungen", substring = false).performScrollTo().performClick()
            ui.onNodeWithText("Anleitung öffnen").performScrollTo().performClick()
            ui.onNodeWithText("Bild anzeigen").performScrollTo().performClick()
            ui.waitUntil(15000) { ui.onAllNodesWithText("Groß öffnen / vergrößern").fetchSemanticsNodes().isNotEmpty() }
            screenshot("anleitung-tageslicht.png", WerkTheme.TAGESLICHT)
            ui.onNodeWithText("Groß öffnen / vergrößern").performScrollTo().performClick()
            ui.onNodeWithContentDescription("Vergrößertes Bild").assertIsDisplayed()
            screenshot("bildansicht-tageslicht.png", dialog = true)
            ui.onNodeWithText("Schließen").assertIsDisplayed().performClick()
        }
    }
}
