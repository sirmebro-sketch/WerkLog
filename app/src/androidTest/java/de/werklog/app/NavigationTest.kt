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
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var diagnosticModel: WorkModel
            scenario.onActivity { diagnosticModel = androidx.lifecycle.ViewModelProvider(it)[WorkModel::class.java] }
            try {
            ui.onNodeWithText("Passwort", substring = false).performTextInput("Testpasswort2026")
            ui.onNodeWithText("Passwort wiederholen").performTextInput("Testpasswort2026")
            ui.onNodeWithText("Tresor erstellen").performClick()
            ui.waitUntil(60000) { ui.onAllNodesWithText("Später").fetchSemanticsNodes().isNotEmpty() }
            ui.onNodeWithText("Später").performClick()
            ui.waitUntil(15000) { ui.onAllNodesWithText("Überspringen").fetchSemanticsNodes().isNotEmpty() }
            ui.onNodeWithText("Weiter").performClick()
            ui.onNodeWithText("Anlagen", substring = false).assertIsDisplayed()
            ui.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
                File(requireNotNull(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")), "fuehrung.png").apply { parentFile?.mkdirs() }.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            }
            ui.onNodeWithText("Überspringen").performClick()
            ui.onNodeWithText("Betrieb", useUnmergedTree = true).performClick()
            ui.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
                File(requireNotNull(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")), "betrieb.png").apply { parentFile?.mkdirs() }.outputStream().use { out -> bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out) }
            }
            ui.onNodeWithText("Anlagen", substring = false).performClick()
            ui.onNodeWithText("+ Anlage anlegen").performClick()
            ui.onNodeWithText("Anlagenname *").performTextInput("Prüfanlage")
            ui.onNodeWithText("Speichern").performClick()
            ui.waitUntil(10000) { ui.onAllNodesWithContentDescription("Anlagenakte öffnen").fetchSemanticsNodes().isNotEmpty() }
            ui.onNodeWithContentDescription("Anlagenakte öffnen").performScrollTo().performClick()
            ui.onNodeWithText("Anlageninformationen bearbeiten").performScrollTo().performClick()
            ui.onNodeWithText("Anlagenname *").performTextReplacement("Prüfanlage geändert")
            // A real disk-write failure must keep the editor and its entered text.
            val activeDir = File(context.filesDir, File(context.filesDir, "active").readText())
            val blockedWrite = File(activeDir, "data.vault.tmp").apply { mkdir() }
            ui.onNodeWithText("Speichern").performClick()
            ui.waitUntil(15000) { ui.onAllNodesWithText("Verstanden").fetchSemanticsNodes().isNotEmpty() }
            ui.onNodeWithText("Verstanden").performClick()
            ui.onNodeWithText("Prüfanlage geändert", substring = false).assertExists()
            org.junit.Assert.assertEquals("Prüfanlage", diagnosticModel.data!!.assets.single().name)
            blockedWrite.delete()
            ui.onNodeWithText("Speichern").performClick()
            ui.waitUntil(10000) { ui.onAllNodesWithText("Prüfanlage geändert").fetchSemanticsNodes().isNotEmpty() }
            ui.onNodeWithText("Einstellung", useUnmergedTree = true).performClick()
            ui.onNodeWithText("Profil erstellen").performScrollTo().performClick()
            ui.onNodeWithText("Name *").performTextInput("Alex Test")
            ui.onNodeWithText("Team / Bereich").performTextInput("Kraftwerk")
            ui.onNodeWithText("Speichern").performClick()
            ui.waitUntil(10000) { ui.onAllNodesWithText("Profil bearbeiten").fetchSemanticsNodes().isNotEmpty() }
            ui.onNodeWithText("Alex Test", substring = false).assertExists()
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
            ui.waitUntil(60000) { ui.onAllNodesWithText("Profil bearbeiten").fetchSemanticsNodes().isNotEmpty() }
            ui.onNodeWithText("Heute", useUnmergedTree = true).performClick()
            ui.onNodeWithText("Hallo, Alex Test.").assertExists()
            ui.onAllNodesWithText("Jetzt sichern").assertCountEquals(0)
            ui.onNodeWithText("Betrieb", useUnmergedTree = true).performClick()
            ui.onNodeWithText("Anleitungen", substring = false).performClick()
            ui.onNodeWithText("+ Anleitung").performScrollTo().performClick()
            ui.onNodeWithText("Titel *").performTextInput("Pumpenprüfung")
            ui.onNodeWithText("Speichern").performClick()
            ui.waitUntil(10000) { ui.onAllNodesWithText("+ Nächster Schritt").fetchSemanticsNodes().isNotEmpty() }
            lateinit var scenarioModel: WorkModel
            scenario.onActivity { scenarioModel = androidx.lifecycle.ViewModelProvider(it)[WorkModel::class.java] }
            for ((index, name) in listOf("Vorbereiten", "Kontrollieren").withIndex()) {
                ui.onNodeWithText("+ Nächster Schritt").assertIsDisplayed().performClick()
                ui.onNodeWithText("Schritt *").performTextInput(name)
                ui.onNodeWithText("Speichern").performClick()
                ui.waitUntil(10000) { !scenarioModel.busy && scenarioModel.data!!.work.guides.last().steps.size == index + 1 }
            }
            ui.onNodeWithText("Anleitungseinstellungen").performScrollTo()
            ui.onNodeWithText("+ Nächster Schritt").assertIsDisplayed()
            var capture: File? = null
            scenario.onActivity { activity ->
                val model = androidx.lifecycle.ViewModelProvider(activity)[WorkModel::class.java]
                val step = model.data!!.work.guides.last().steps.last()
                val file = File(File(activity.cacheDir, "camera").apply { mkdirs() }, "test-capture.jpg")
                val bitmap = android.graphics.Bitmap.createBitmap(1200, 800, android.graphics.Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.BLUE)
                file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, it) }; bitmap.recycle()
                capture = file
                fun field(name: String, value: Any) { MainActivity::class.java.getDeclaredField(name).apply { isAccessible = true }.set(activity, value) }
                field("photoFile", file); field("photoTarget", PhotoTarget("guide", step.id))
                field("handoffUntil", android.os.SystemClock.elapsedRealtime() + 120000L)
            }
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
            scenario.recreate()
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
            scenario.onActivity { activity ->
                org.junit.Assert.assertNotNull(androidx.lifecycle.ViewModelProvider(activity)[WorkModel::class.java].data)
                org.junit.Assert.assertTrue(capture!!.isFile)
                activity.completeCameraCapture(true)
            }
            ui.waitUntil(15000) { ui.onAllNodesWithText("Groß öffnen / vergrößern").fetchSemanticsNodes().isNotEmpty() }
            ui.onNodeWithText("Groß öffnen / vergrößern").performScrollTo().performClick()
            ui.onNodeWithContentDescription("Vergrößertes Bild").assertIsDisplayed()
            ui.onNodeWithText("Schließen").performClick()
            ui.onNodeWithText("Speichern").performClick()
            ui.waitUntil(15000) { !scenarioModel.busy && scenarioModel.data!!.work.guides.last().steps.last().image.startsWith("img:") }
            org.junit.Assert.assertFalse(capture!!.exists())
            ui.waitForIdle()
            scenario.onActivity { activity ->
                val guide = androidx.lifecycle.ViewModelProvider(activity)[WorkModel::class.java].data!!.work.guides.last()
                org.junit.Assert.assertEquals("", guide.steps.first().image)
                org.junit.Assert.assertTrue(guide.steps.last().image.startsWith("img:"))
            }

            ui.onNodeWithText("+ Nächster Schritt").performClick()
            ui.onNodeWithText("Schritt *").performTextInput("Unfertiger Prüfschritt")
            ui.onNodeWithText("Beschreibung / Voraussetzungen / Kontrolle").performTextInput("Notiz bleibt beim Sperren erhalten")
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
            org.junit.Assert.assertNull(scenarioModel.data)
            val checkpoint = File(context.filesDir, "ui-draft.vault")
            org.junit.Assert.assertTrue(checkpoint.isFile)
            org.junit.Assert.assertFalse(checkpoint.readText().contains("Unfertiger Prüfschritt"))
            scenarioModel.workspacePage.intValue = 0; scenarioModel.workspaceTool.value = null
            scenario.recreate()
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
            ui.waitUntil(10000) { ui.onAllNodesWithText("Passwort", substring = false).fetchSemanticsNodes().isNotEmpty() }
            ui.onAllNodesWithText("Unfertiger Prüfschritt").assertCountEquals(0)
            ui.onNodeWithText("Passwort", substring = false).performTextInput("NeuesPasswort2026")
            ui.onNodeWithText("Entsperren", substring = false).performClick()
            ui.waitUntil(60000) { ui.onAllNodesWithText("Unfertiger Prüfschritt").fetchSemanticsNodes().isNotEmpty() }
            ui.onNodeWithText("Notiz bleibt beim Sperren erhalten").assertExists()
            org.junit.Assert.assertEquals(2, scenarioModel.data!!.work.guides.last().steps.size)
            ui.onNodeWithText("Speichern").performClick()
            ui.waitUntil(15000) { !scenarioModel.busy && scenarioModel.data!!.work.guides.last().steps.size == 3 }
            ui.onNodeWithText("Heute", useUnmergedTree = true).performClick()
            ui.onNodeWithText("+ Störung").performScrollTo().performClick()
            ui.onNodeWithText("Kurzbeschreibung *").performTextInput("Prüfstörung")
            ui.onNodeWithText("Beobachtung, Maßnahmen, nächste Schritte").performTextInput("Interner Bestellhintergrund")
            ui.onNodeWithText("Speichern").performClick()
            ui.waitUntil(15000) { !scenarioModel.busy && scenarioModel.data!!.entries.size == 1 }
            ui.onNodeWithText("Öffnen & bearbeiten").performScrollTo().performClick()
            ui.onNodeWithText("Teileanforderung aus diesem Vorgang").performScrollTo().performClick()
            ui.waitUntil(15000) { ui.onAllNodesWithText("Grundinformationen bearbeiten").fetchSemanticsNodes().isNotEmpty() }
            org.junit.Assert.assertTrue(scenarioModel.data!!.work.orders.single().items.isEmpty())
            org.junit.Assert.assertEquals("Interner Bestellhintergrund", scenarioModel.data!!.work.orders.single().context)
            ui.onNodeWithText("+ Nächstes Teil").performScrollTo().performClick()
            ui.onNodeWithText("Teil / Ausrüstung / Artikelnummer *").performTextInput("Testdichtung")
            ui.onNodeWithText("Speichern").performClick()
            ui.waitUntil(15000) { !scenarioModel.busy && scenarioModel.data!!.work.orders.single().items.size == 1 }
            org.junit.Assert.assertEquals(scenarioModel.data!!.assets.single().id, scenarioModel.data!!.work.orders.single().items.single().assetId)
            ui.onNodeWithText("Betrieb", useUnmergedTree = true).performClick()
            ui.onNodeWithText("Adressbuch", substring = false).performScrollTo().performClick()
            ui.onNodeWithText("+ Kontakt").performClick()
            ui.onNodeWithText("Name *").performTextInput("Service Testperson")
            ui.onNodeWithText("Anlagen · 0 zugeordnet ▾").performScrollTo().performClick()
            ui.onNodeWithContentDescription("Anlagen: Prüfanlage geändert").performScrollTo().assertIsOff().performClick().assertIsOn()
            ui.onNodeWithText("Störungen / Arbeiten · 0 zugeordnet ▾").performScrollTo().performClick()
            ui.onNodeWithContentDescription("Störungen / Arbeiten: Prüfstörung · Prüfanlage geändert").performScrollTo().assertIsOff().performClick().assertIsOn()
            ui.onNodeWithText("Speichern").performClick()
            ui.waitUntil(15000) { !scenarioModel.busy && scenarioModel.data!!.contacts.size == 1 }
            org.junit.Assert.assertEquals(1, scenarioModel.data!!.contacts.single().assetIds.size)
            org.junit.Assert.assertEquals(1, scenarioModel.data!!.contacts.single().entryIds.size)
            ui.onNodeWithText("Prüfanlage geändert", substring = false).performScrollTo().performClick()
            ui.onNodeWithText("Service Testperson", substring = false).assertExists()
            ui.onNodeWithText("Service Testperson", substring = false).performScrollTo().performClick()
            ui.onNodeWithText("Zugeordnete Störungen / Arbeiten").assertExists()
            ui.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
                File(requireNotNull(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")), "kontakte.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            }
            // Import a colleague package through the production ViewModel, including a cover image.
            val imageBytes = java.io.ByteArrayOutputStream().also { out ->
                val bitmap = android.graphics.Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.GREEN); bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, out); bitmap.recycle()
            }.toByteArray()
            val sourceAsset = Asset(name = "Freigabe Testanlage", trade = "Wasser", location = "Test", note = "", coverImage = java.util.Base64.getEncoder().encodeToString(imageBytes))
            val source = Data(assets = listOf(sourceAsset))
            val sharedFile = File(context.cacheDir, "test-cover.werkshare"); val shareCode = Exchange.newCode()
            kotlinx.coroutines.runBlocking { ShareArchive.create(source, shareCode, { error("Inline fixture") }, sharedFile, context.cacheDir) }
            val opened = ShareArchive.open(sharedFile, shareCode, context.cacheDir)
            scenario.onActivity { scenarioModel.importShare(opened, null, false) }
            ui.waitUntil(30000) { !scenarioModel.busy && scenarioModel.data!!.assets.size == 2 }
            val imported = scenarioModel.data!!.assets.single { it.name.startsWith("Freigabe Testanlage") }
            org.junit.Assert.assertTrue(isImageRef(imported.coverImage))
            val importedBytes = kotlinx.coroutines.runBlocking { scenarioModel.image(imported.coverImage) }
            org.junit.Assert.assertArrayEquals(imageBytes, importedBytes)
            sharedFile.delete()
            } catch (failure: Throwable) {
                println("TEST DIAGNOSTIC busy=${diagnosticModel.busy} unlocked=${diagnosticModel.data != null} exists=${diagnosticModel.exists} error=${diagnosticModel.error} page=${diagnosticModel.workspacePage.intValue}")
                runCatching { println(ui.onRoot().printToString()) }
                runCatching { ui.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
                    File(requireNotNull(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")), "failure.png").apply { parentFile?.mkdirs() }.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
                } }
                throw failure
            }
        }
    }
}
