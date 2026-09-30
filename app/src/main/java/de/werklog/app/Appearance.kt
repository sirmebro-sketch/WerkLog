package de.werklog.app

/** Opaque sRGB values; kept platform-independent so every theme's contrast can be checked. */
data class ThemePalette(
    val background: Long, val surface: Long, val raised: Long, val text: Long, val muted: Long,
    val accent: Long, val onAccent: Long, val accentContainer: Long, val onAccentContainer: Long,
    val outline: Long, val cardOutline: Long,
    val warning: Long, val warningContainer: Long, val critical: Long, val criticalContainer: Long,
    val success: Long, val successContainer: Long, val info: Long, val infoContainer: Long
)

private fun darkPalette(background: Long, surface: Long, raised: Long, text: Long, muted: Long,
    accent: Long, onAccent: Long, accentContainer: Long, onAccentContainer: Long,
    outline: Long, cardOutline: Long) = ThemePalette(background, surface, raised, text, muted,
    accent, onAccent, accentContainer, onAccentContainer, outline, cardOutline,
    warning = 0xFFFFCC80, warningContainer = 0xFF4E3714,
    critical = 0xFFFFB4AB, criticalContainer = 0xFF60251E,
    success = 0xFF98D4A5, successContainer = 0xFF153E27,
    info = 0xFFA9C9FF, infoContainer = 0xFF1A3658)

enum class WerkTheme(val id: String, val title: String, val description: String,
    val isDark: Boolean, val palette: ThemePalette) {
    PETROL("petrol", "Petrol & Mint", "Dunkles Petrol · frisches Mint", true,
        darkPalette(0xFF0C191E, 0xFF1B3038, 0xFF243F49, 0xFFE8F2F3, 0xFFABC1C7,
            0xFF64DECB, 0xFF00382F, 0xFF164F48, 0xFFB3F2E6, 0xFF70919C, 0xFF496873)),
    STAHLBLAU("stahlblau", "Stahlblau", "Tiefes Marineblau · klares Eisblau", true,
        darkPalette(0xFF10182B, 0xFF202F4C, 0xFF2D4163, 0xFFF0F4FF, 0xFFB7C6E2,
            0xFF9BC4FF, 0xFF102A4D, 0xFF1E4675, 0xFFD6E7FF, 0xFF8097BD, 0xFF526D94)),
    KUPFER("kupfer", "Graphit & Kupfer", "Warmer Graphit · weiches Kupfer", true,
        darkPalette(0xFF211A17, 0xFF352B26, 0xFF483930, 0xFFF6EDE5, 0xFFD2BFB0,
            0xFFF3BB92, 0xFF3D2312, 0xFF64432D, 0xFFFFDCC4, 0xFFAB8C76, 0xFF836B5C)),
    TAGESLICHT("tageslicht", "Tageslicht", "Helle Flächen · kräftiges Petrol", false,
        ThemePalette(0xFFE9EFF4, 0xFFFFFFFF, 0xFFDEE8EF, 0xFF1C2D38, 0xFF4B6373,
            0xFF006B67, 0xFFFFFFFF, 0xFFC9EEE9, 0xFF003D39, 0xFF5C7684, 0xFF9EB3C1,
            warning = 0xFF825000, warningContainer = 0xFFFFEDCE,
            critical = 0xFFB3261E, criticalContainer = 0xFFFFDAD5,
            success = 0xFF23643B, successContainer = 0xFFD4EEDD,
            info = 0xFF1D548C, infoContainer = 0xFFDAE9FF));

    companion object {
        fun fromId(id: String?) = entries.find { it.id == id } ?: PETROL
    }
}

/** Only appearance IDs are saved outside the vault. No workplace data belongs here. */
data class AppearanceChoice(val selected: WerkTheme = WerkTheme.PETROL,
    val followSystem: Boolean = false, val night: WerkTheme = WerkTheme.PETROL) {
    fun effective(systemDark: Boolean): WerkTheme = if (!followSystem) selected
        else if (!systemDark) WerkTheme.TAGESLICHT else night.takeIf { it.isDark } ?: WerkTheme.PETROL
    fun choose(theme: WerkTheme) = copy(selected = theme, followSystem = false,
        night = if (theme.isDark) theme else night)
}

enum class StatusTone { NEUTRAL, INFO, WARNING, CRITICAL, SUCCESS }

fun entryTone(status: String, priority: String, overdue: Boolean = false) = when {
    status == "Erledigt" -> StatusTone.SUCCESS
    priority == "Dringend" || overdue -> StatusTone.CRITICAL
    priority == "Wichtig" -> StatusTone.WARNING
    status == "In Arbeit" -> StatusTone.INFO
    else -> StatusTone.NEUTRAL
}
fun orderTone(status: String) = when (status) {
    "Geliefert" -> StatusTone.SUCCESS
    "Teilgeliefert" -> StatusTone.WARNING
    "Angefragt", "Bestellt" -> StatusTone.INFO
    else -> StatusTone.NEUTRAL
}
fun appointmentTone(status: String) = when (status) {
    "Erledigt" -> StatusTone.SUCCESS
    "Geplant" -> StatusTone.INFO
    else -> StatusTone.NEUTRAL
}
fun serviceTone(state: String?) = when (state) {
    "Überfällig" -> StatusTone.CRITICAL
    "Heute fällig", "In den nächsten 30 Tagen" -> StatusTone.WARNING
    else -> StatusTone.NEUTRAL
}
