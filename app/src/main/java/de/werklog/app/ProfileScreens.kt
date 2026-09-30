package de.werklog.app

import androidx.compose.runtime.saveable.rememberSaveable

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Base64

/** Outlined power station: cooling tower, turbine hall, chimney and electric bolt. */
val PowerPlantIcon: ImageVector by lazy {
    ImageVector.Builder("Kraftwerk", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.7f,
            strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(2f, 21f); lineTo(22f, 21f)
            moveTo(3f, 21f); curveTo(5f, 15f, 6f, 10f, 5f, 5f); lineTo(11f, 5f)
            curveTo(10f, 9f, 11f, 13f, 12f, 16f)
            moveTo(12f, 21f); lineTo(12f, 14f); lineTo(21f, 14f); lineTo(21f, 21f)
            moveTo(17f, 14f); lineTo(17f, 4f); lineTo(20f, 4f); lineTo(20f, 14f)
            moveTo(8.5f, 11f); lineTo(6.5f, 15f); lineTo(9f, 15f); lineTo(7f, 19f)
            moveTo(15f, 17f); lineTo(15f, 18f); moveTo(18f, 17f); lineTo(18f, 18f)
        }
    }.build()
}

@Composable internal fun TourStrip(step: Int, title: String, hint: String, back: () -> Unit, skip: () -> Unit, next: () -> Unit) {
    // Part of Scaffold's bottom bar: reserves space instead of covering or dimming the page.
    val maxHeight = (LocalConfiguration.current.screenHeightDp * .30f).dp
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 4.dp) {
        Column(Modifier.fillMaxWidth().heightIn(max = maxHeight).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text("${step + 1}/4 · $title", color = Mint, fontWeight = FontWeight.Bold)
            Text(hint, style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = skip) { Text("Überspringen") }
                Row {
                    if (step > 0) TextButton(onClick = back) { Text("Zurück") }
                    TextButton(onClick = next) { Text(if (step == 3) "Loslegen" else "Weiter") }
                }
            }
        }
    }
}

@Composable private fun ProfileAvatar(p: LocalProfile) {
    val bitmap = remember(p.image) { runCatching {
        if (p.image.isEmpty()) null else Base64.getDecoder().decode(p.image).let { bytes ->
            try {
                val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                require(bounds.outWidth in 1..4096 && bounds.outHeight in 1..4096)
                var sample = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 384) sample *= 2
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, android.graphics.BitmapFactory.Options().apply { inSampleSize = sample })
            } finally { bytes.fill(0) }
        }
    }.getOrNull() }
    if (bitmap != null) Image(bitmap.asImageBitmap(), "Profilbild", contentScale = ContentScale.Crop, modifier = Modifier.size(64.dp).clip(CircleShape))
    else Icon(Icons.Outlined.Person, "Lokales Profil", tint = Mint, modifier = Modifier.size(64.dp))
}

@Composable internal fun ProfilePanel(d: Data, busy: Boolean, save: DataSaver, photo: (PhotoTarget) -> Unit) {
    val p = d.profile
    var editing by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ProfileAvatar(p)
            Column(Modifier.weight(1f)) {
                Text(p.name.ifBlank { "Dein lokales Profil" }, fontWeight = FontWeight.Bold)
                Text(listOf(p.role, p.team).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Persönlich · nur auf diesem Gerät" }, color = Muted)
            }
        }
        Hint("Im Tresor geschützt. In Vollsicherungen enthalten, nicht in Anlagenfreigaben oder automatisch in E-Mails.")
        OutlinedButton(onClick = { editing = true }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(if (p.name.isEmpty()) "Profil erstellen" else "Profil bearbeiten") }
        TextButton(onClick = { photo(PhotoTarget("profile", "local")) }, enabled = !busy) { Text(if (p.image.isEmpty()) "Profilbild hinzufügen" else "Profilbild ändern") }
        if (p.image.isNotEmpty()) TextButton(onClick = { save(d.copy(profile = p.copy(image = ""))) }, enabled = !busy) { Text("Profilbild entfernen") }
        if (p != LocalProfile()) TextButton(onClick = { deleting = true }, enabled = !busy) { Text("Profil löschen") }
    }
    if (editing) {
        var name by rememberSaveable { mutableStateOf(p.name) }; var role by rememberSaveable { mutableStateOf(p.role) }
        var team by rememberSaveable { mutableStateOf(p.team) }; var company by rememberSaveable { mutableStateOf(p.company) }
        var phone by rememberSaveable { mutableStateOf(p.phone) }; var email by rememberSaveable { mutableStateOf(p.email) }
        Form("Lokales Profil", name.isNotBlank() && !busy, { editing = false }, {
            save(d.copy(profile = p.copy(name = name.trim(), role = role.trim(), team = team.trim(), company = company.trim(), phone = phone.trim(), email = email.trim()))) { editing = false }
        }) {
            Field(name, { name = it.take(200) }, "Name *")
            Field(role, { role = it.take(200) }, "Funktion / Beruf")
            Field(team, { team = it.take(200) }, "Team / Bereich")
            Field(company, { company = it.take(200) }, "Betrieb / Standort")
            Field(phone, { phone = it.take(200) }, "Diensttelefon / Durchwahl")
            Field(email, { email = it.take(200) }, "Dienstliche E-Mail")
            Hint("Außer deinem Namen ist alles freiwillig. Kein Online-Konto, keine Anmeldung.")
        }
    }
    if (deleting) AlertDialog(onDismissRequest = { deleting = false }, title = { Text("Profil löschen?") },
        text = { Text("Name, Profilbild und Kontaktdaten entfernen? Deine Anlagen und Arbeitsdaten bleiben erhalten.") },
        confirmButton = { TextButton(onClick = { save(d.copy(profile = LocalProfile())) { deleting = false } }, enabled = !busy) { Text("Löschen") } },
        dismissButton = { TextButton(onClick = { deleting = false }) { Text("Abbrechen") } })
}
