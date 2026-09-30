package de.werklog.app

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

internal val Long.color: Color get() = Color(this)
val LocalWerkTheme = staticCompositionLocalOf { WerkTheme.PETROL }
internal val Accent: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primary
internal val Muted: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurfaceVariant
internal val Warning: Color @Composable @ReadOnlyComposable get() = LocalWerkTheme.current.palette.warning.color
internal val Critical: Color @Composable @ReadOnlyComposable get() = LocalWerkTheme.current.palette.critical.color
internal val Success: Color @Composable @ReadOnlyComposable get() = LocalWerkTheme.current.palette.success.color

/** Every Material color role is explicit, including dialog/menu surfaces and light-mode text. */
fun WerkTheme.colorScheme(): ColorScheme {
    val p = palette
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = p.accent.color, onPrimary = p.onAccent.color,
        primaryContainer = p.accentContainer.color, onPrimaryContainer = p.onAccentContainer.color,
        inversePrimary = (if (isDark) p.onAccent else p.accentContainer).color,
        secondary = p.muted.color, onSecondary = p.background.color,
        secondaryContainer = p.raised.color, onSecondaryContainer = p.text.color,
        tertiary = p.info.color, onTertiary = (if (isDark) p.background else 0xFFFFFFFF).color,
        tertiaryContainer = p.infoContainer.color, onTertiaryContainer = p.info.color,
        background = p.background.color, onBackground = p.text.color,
        surface = p.surface.color, onSurface = p.text.color,
        surfaceVariant = p.raised.color, onSurfaceVariant = p.muted.color,
        surfaceTint = Color.Transparent,
        inverseSurface = p.text.color, inverseOnSurface = p.background.color,
        error = p.critical.color, onError = (if (isDark) p.background else 0xFFFFFFFF).color,
        errorContainer = p.criticalContainer.color, onErrorContainer = p.critical.color,
        outline = p.outline.color, outlineVariant = p.cardOutline.color, scrim = Color.Black,
        surfaceDim = p.background.color, surfaceBright = (if (isDark) p.raised else p.surface).color,
        surfaceContainerLowest = p.background.color, surfaceContainerLow = p.surface.color,
        surfaceContainer = p.surface.color, surfaceContainerHigh = p.raised.color,
        surfaceContainerHighest = p.raised.color)
}

@Composable fun WerkLogTheme(theme: WerkTheme, content: @Composable () -> Unit) {
    val scheme = remember(theme) { theme.colorScheme() }
    CompositionLocalProvider(LocalWerkTheme provides theme) { MaterialTheme(colorScheme = scheme, content = content) }
}

@Stable class AppearanceSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("appearance", Context.MODE_PRIVATE)
    var choice by mutableStateOf(AppearanceChoice(WerkTheme.fromId(prefs.getString("theme", null)),
        prefs.getBoolean("system", false), WerkTheme.fromId(prefs.getString("night", null))))
        private set

    fun select(theme: WerkTheme) = save(choice.choose(theme))
    fun followSystem(value: Boolean) = save(choice.copy(followSystem = value))
    private fun save(next: AppearanceChoice): Boolean {
        val saved = prefs.edit().putString("theme", next.selected.id).putString("night", next.night.id)
            .putBoolean("system", next.followSystem).commit()
        if (saved) choice = next
        return saved
    }
}
