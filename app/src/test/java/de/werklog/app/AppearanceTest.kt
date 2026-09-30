package de.werklog.app

import kotlin.math.pow
import org.junit.Assert.*
import org.junit.Test

class AppearanceTest {
    private fun luminance(color: Long): Double {
        fun component(shift: Int): Double {
            val srgb = ((color shr shift) and 255).toDouble() / 255
            return if (srgb <= .04045) srgb / 12.92 else ((srgb + .055) / 1.055).pow(2.4)
        }
        return .2126 * component(16) + .7152 * component(8) + .0722 * component(0)
    }
    private fun contrast(a: Long, b: Long): Double {
        val x = luminance(a); val y = luminance(b)
        return (maxOf(x, y) + .05) / (minOf(x, y) + .05)
    }
    private fun atLeast(theme: WerkTheme, role: String, a: Long, b: Long, minimum: Double) {
        val ratio = contrast(a, b)
        assertTrue("${theme.title}: $role = $ratio, expected >= $minimum", ratio >= minimum)
    }
    @Test fun ordinaryTextMetadataActionsAndWarningsHaveReadableContrast() {
        WerkTheme.entries.forEach { t ->
            val p = t.palette
            for (surface in listOf(p.background, p.surface, p.raised)) {
                for ((name, color) in listOf("text" to p.text, "metadata" to p.muted, "actions" to p.accent,
                    "warning" to p.warning, "critical" to p.critical, "success" to p.success, "info" to p.info)) {
                    atLeast(t, name, color, surface, 4.5)
                }
            }
        }
    }
    @Test fun filledActionsSelectionAndEveryStatusContainerHaveReadableText() {
        WerkTheme.entries.forEach { t ->
            val p = t.palette
            for ((label, colors) in listOf("action" to (p.onAccent to p.accent),
                "active navigation" to (p.onAccentContainer to p.accentContainer),
                "selected theme title" to (p.text to p.accentContainer),
                "selected theme description" to (p.muted to p.accentContainer),
                "neutral" to (p.muted to p.raised), "info" to (p.info to p.infoContainer),
                "warning" to (p.warning to p.warningContainer), "critical" to (p.critical to p.criticalContainer),
                "success" to (p.success to p.successContainer))) {
                atLeast(t, label, colors.first, colors.second, 4.5)
            }
        }
    }
    @Test fun inputContoursRemainVisibleAndCardsHaveDistinctBorders() {
        WerkTheme.entries.forEach { t ->
            val p = t.palette
            for (surface in listOf(p.background, p.surface, p.raised)) atLeast(t, "input contour", p.outline, surface, 3.0)
            // Decorative card borders supplement the surface and labels; these are not input outlines.
            atLeast(t, "card contour", p.cardOutline, p.surface, 2.0)
        }
    }
    @Test fun choicesAreDistinctAndUnknownStoredIdsKeepExistingAppearance() {
        assertEquals(4, WerkTheme.entries.size)
        assertEquals(4, WerkTheme.entries.map { it.id }.distinct().size)
        assertEquals(4, WerkTheme.entries.map { it.palette.background }.distinct().size)
        assertEquals(4, WerkTheme.entries.map { it.palette.accent }.distinct().size)
        assertEquals(1, WerkTheme.entries.count { !it.isDark })
        assertEquals(WerkTheme.PETROL, WerkTheme.fromId(null))
        assertEquals(WerkTheme.PETROL, WerkTheme.fromId("unknown-future-theme"))
        assertEquals(WerkTheme.PETROL, AppearanceChoice().effective(false))
        WerkTheme.entries.forEach { assertEquals(it, WerkTheme.fromId(it.id)) }
    }
    @Test fun manualSelectionWinsAndKeepsLastDarkChoice() {
        val choice = AppearanceChoice(followSystem = true).choose(WerkTheme.KUPFER).choose(WerkTheme.TAGESLICHT)
        assertFalse(choice.followSystem)
        assertEquals(WerkTheme.TAGESLICHT, choice.effective(true))
        assertEquals(WerkTheme.KUPFER, choice.night)
    }
    @Test fun systemModeUsesDaylightAndRememberedDarkChoiceWithSafeFallback() {
        val choice = AppearanceChoice().choose(WerkTheme.STAHLBLAU).copy(followSystem = true)
        assertEquals(WerkTheme.TAGESLICHT, choice.effective(false))
        assertEquals(WerkTheme.STAHLBLAU, choice.effective(true))
        assertEquals(WerkTheme.PETROL, choice.copy(night = WerkTheme.TAGESLICHT).effective(true))
    }
    @Test fun statusMeaningHandlesPriorityOverdueCompletionAndCancellation() {
        assertEquals(StatusTone.NEUTRAL, entryTone("Offen", "Normal"))
        assertEquals(StatusTone.INFO, entryTone("In Arbeit", "Normal"))
        assertEquals(StatusTone.WARNING, entryTone("Offen", "Wichtig"))
        assertEquals(StatusTone.CRITICAL, entryTone("Offen", "Dringend"))
        assertEquals(StatusTone.CRITICAL, entryTone("In Arbeit", "Normal", overdue = true))
        assertEquals(StatusTone.SUCCESS, entryTone("Erledigt", "Dringend", overdue = true))
        assertEquals(StatusTone.WARNING, orderTone("Teilgeliefert"))
        assertEquals(StatusTone.INFO, orderTone("Bestellt"))
        assertEquals(StatusTone.SUCCESS, orderTone("Geliefert"))
        assertEquals(StatusTone.NEUTRAL, orderTone("Abgesagt"))
        assertEquals(StatusTone.NEUTRAL, appointmentTone("Abgesagt"))
        assertEquals(StatusTone.SUCCESS, appointmentTone("Erledigt"))
        assertEquals(StatusTone.CRITICAL, serviceTone("Überfällig"))
        assertEquals(StatusTone.WARNING, serviceTone("Heute fällig"))
        assertEquals(StatusTone.NEUTRAL, serviceTone(null))
    }
    @Test fun darkConceptsShareSemanticColorsAndLightConceptUsesDarkerStatusText() {
        val dark = WerkTheme.entries.filter { it.isDark }.map { it.palette }
        assertEquals(1, dark.map { listOf(it.info, it.warning, it.critical, it.success) }.distinct().size)
        val light = WerkTheme.TAGESLICHT.palette
        assertTrue(luminance(light.info) < luminance(dark.first().info))
        assertTrue(luminance(light.warning) < luminance(dark.first().warning))
        assertTrue(luminance(light.critical) < luminance(dark.first().critical))
        assertTrue(luminance(light.success) < luminance(dark.first().success))
    }
}
