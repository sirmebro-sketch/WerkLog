package de.werklog.app

import androidx.compose.ui.graphics.asAndroidBitmap
import java.io.File
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import org.junit.Rule
import org.junit.Test

/** Real emulator interaction: setup, 3-tab navigation, asset CRUD, password rotation. */
class NavigationTest {
    @get:Rule val ui = createEmptyComposeRule()
    @Test fun technicianFlow() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.filesDir.listFiles()?.forEach { it.deleteRecursively() }
        context.getSharedPreferences("onboarding", 0).edit().clear().commit()
        context.getSharedPreferences("biometric", 0).edit().clear().commit()
        ActivityScenario.launch(MainActivity::class.java).use {
            ui.onNodeWithText("Passwort", substring = false).performTextInput("Testpasswort2026")
            ui.onNodeWithText("Passwort wiederholen").performTextInput("Testpasswort2026")
            ui.onNodeWithText("Tresor erstellen").performClick()
            ui.waitUntil(15000) { ui.onAllNodesWithText("Überspringen").fetchSemanticsNodes().isNotEmpty() }
            ui.onNodeWithText("Überspringen").performClick()
            ui.onNodeWithText("Betrieb", useUnmergedTree = true).performClick()
            ui.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
                File(context.getExternalFilesDir(null), "betrieb.png").outputStream().use { out -> bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out) }
            }
            ui.onNodeWithText("Anlagen", substring = false).performClick()
            ui.onNodeWithText("+ Anlage anlegen").performClick()
            ui.onNodeWithText("Anlagenname *").performTextInput("Prüfanlage")
            ui.onNodeWithText("Speichern").performClick()
            ui.waitUntil(10000) { ui.onAllNodesWithText("Anlagenakte öffnen").fetchSemanticsNodes().isNotEmpty() }
            ui.onNodeWithText("Anlagenakte öffnen").performScrollTo().performClick()
            ui.onNodeWithText("Anlageninformationen bearbeiten").performScrollTo().performClick()
            ui.onNodeWithText("Anlagenname *").performTextReplacement("Prüfanlage geändert")
            ui.onNodeWithText("Speichern").performClick()
            ui.waitUntil(10000) { ui.onAllNodesWithText("Prüfanlage geändert").fetchSemanticsNodes().isNotEmpty() }
            ui.onNodeWithText("Einstellung", useUnmergedTree = true).performClick()
            ui.onNodeWithText("App-Passwort ändern").performScrollTo().performClick()
            ui.onNodeWithText("Aktuelles Passwort").performTextInput("Testpasswort2026")
            ui.onNodeWithText("Neues Passwort (mind. 10 Zeichen)").performTextInput("NeuesPasswort2026")
            ui.onNodeWithText("Wiederholen").performTextInput("NeuesPasswort2026")
            ui.onNodeWithText("Speichern").performClick()
            ui.waitUntil(15000) { ui.onAllNodesWithText("Verstanden").fetchSemanticsNodes().isNotEmpty() }
            ui.onNodeWithText("Verstanden").performClick()
            ui.onNodeWithContentDescription("App sperren").assertIsDisplayed().performClick()
            ui.waitUntil(10000) { ui.onAllNodesWithText("Passwort", substring = false).fetchSemanticsNodes().isNotEmpty() }
            ui.onNodeWithText("Passwort", substring = false).performTextInput("NeuesPasswort2026")
            ui.onNodeWithText("Entsperren", substring = false).performClick()
            ui.waitUntil(15000) { ui.onAllNodesWithText("Alles im Blick.").fetchSemanticsNodes().isNotEmpty() }
        }
    }
}
