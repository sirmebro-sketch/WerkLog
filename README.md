# WerkLog

Native Android-App in **Kotlin und Jetpack Compose** für den persönlichen Technikalltag. Kein Java-Quellcode, kein Konto, keine Internetberechtigung, keine Werbung oder Telemetrie.

## Stand 0.1.0 – Testversion, noch keine Gerätefreigabe

Am 29.09.2026 waren Android-Build, acht JVM-Tests und Android-Lint erfolgreich. [Geprüfter Build](https://github.com/sirmebro-sketch/WerkLog/actions/runs/36547530644). Den aktuellen Status zeigt [GitHub Actions](https://github.com/sirmebro-sketch/WerkLog/actions). Ein echter Android-Gerätetest und eine visuelle Abnahme stehen noch aus; dies ist eine Testversion, keine produktionsreife App.

## Funktionen

- Übersicht offener und dringender Vorgänge
- Anlagenbuch mit Gewerk, Standort und Notizen; Suche und Bearbeitung
- Störungen/Tätigkeiten mit Maßnahmen, Priorität, Status und Zeitaufwand
- Messwerte mit Messpunkt, Einheit, Datum und Notiz; deutsche Dezimalkommas
- Eigene Rundgangschecklisten mit „Ungeprüft“, „In Ordnung“ und „Auffällig“; abgeschlossene Protokolle
- E-Mail-Vorschau für offene Vorgänge und heute erledigte Tätigkeiten; explizite Übergabe an eine E-Mail-App, kein automatischer Versand
- Verschlüsseltes Backup und Wiederherstellung über den Android-Dateidialog
- Lokales Passwort, AES-256-GCM-Datentresor, Sperre beim Wechsel in den Hintergrund, Screenshot-Schutz

Alle Anlagen werden vom Nutzer angelegt. Es sind weder echte Betriebsdaten noch erfundene Sicherheitsgrenzwerte enthalten. Die App ist keine offizielle UKE-/KFE-Anwendung und steuert keine Anlagen.

## Bauen

Voraussetzungen: JDK 17, Gradle **8.9**, Android SDK mit Plattform **35**; Internet ist nur zum Einrichten/Bauen nötig. Laufzeit ab Android 8.0 (API 26).

```sh
gradle testDebugUnitTest lintDebug assembleDebug
```

Alternativ den Projektordner in Android Studio öffnen und Gradle 8.9 als lokale Distribution wählen. Ein Gradle-Wrapper-Binary ist in diesem Paket nicht enthalten. Mit installiertem Gradle kann `gradle wrapper --gradle-version 8.9` den offiziellen Wrapper erzeugen.

Der GitHub-Workflow läuft für Push auf main, Pull Requests und manuell. Test-APK: `app/build/outputs/apk/debug/app-debug.apk`. Artefakte bleiben nur **3 Tage**, Gradle-Cache ist deaktiviert. Keine automatische Veröffentlichung.

**Signatur:** Debug-APKs sind nur für Tests. Auf wechselnden CI-Runnern können unterschiedliche Debug-Schlüssel entstehen. Für dauerhaft updatefähige Installationen vor echtem Einsatz einen eigenen Release-Schlüssel sicher verwahren und eine Release-Signierung einrichten. Nie Schlüssel oder Betriebsdaten ins Repository hochladen.

## Erste Schritte

1. Passwort mit mindestens 10 Zeichen festlegen und sicher aufbewahren.
2. Unter Anlagen eigene Anlage anlegen.
3. Störung oder Messwert erfassen; Status im Journal bearbeiten.
4. Unter Rundgang eigene Prüfpunkte anlegen.
5. Unter Mehr eine verschlüsselte Sicherung an einem bewusst gewählten Ort speichern.

Beim Öffnen einer externen App oder des Dateidialogs wird WerkLog gesperrt. Nicht gespeicherte Formularentwürfe gehen dabei absichtlich verloren. Auch bei Prozessende werden keine unverschlüsselten Entwürfe wiederhergestellt.

## Grenzen der ersten Version

Keine Fotos, Dokumentanhänge, Erinnerungen, Messwertdiagramme, automatische Grenzwertbewertung, GLT-Verbindung, Synchronisation oder Mehrbenutzerverwaltung. Keine revisionssichere Historie: Einträge sind bearbeitbar. Keine automatische Löschung und kein Passwortwechsel in der UI. Rundgangsvorlagen sind derzeit nach Erstellung nicht editierbar. Ungeprüfte Punkte bleiben im Ergebnis sichtbar; Auffälligkeiten benötigen eine Notiz, erzeugen aber keine Störung automatisch. Datenumfang ist auf 8 MiB pro verschlüsselter Datei begrenzt. Für große Bestände sind Pagination und eine andere verschlüsselte Speicherarchitektur ein späterer Ausbau.

Datenschutzdetails: [docs/SECURITY.md](docs/SECURITY.md). Abnahme: [docs/DEVICE-TEST.md](docs/DEVICE-TEST.md).
