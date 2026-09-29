# WerkLog

Native Android-App in **Kotlin und Jetpack Compose** für den persönlichen Technikalltag. Kein Java-Quellcode, kein Konto, keine Internetberechtigung, keine Werbung oder Telemetrie.

## Stand 0.3.0 – Testversion, noch keine Gerätefreigabe

Am 29.09.2026 waren Android-Build, acht JVM-Tests und Android-Lint für 0.1.0 erfolgreich. Die neue 0.2.0 erweitert die Tests auf 20; siehe aktuellen Workflow. [Geprüfter Build](https://github.com/sirmebro-sketch/WerkLog/actions/runs/36547530644). Den aktuellen Status zeigt [GitHub Actions](https://github.com/sirmebro-sketch/WerkLog/actions). Ein echter Android-Gerätetest und eine visuelle Abnahme stehen noch aus; dies ist eine Testversion, keine produktionsreife App.

## Neu in 0.3.0

Unter **Mehr → Werkzeuge**:

- **Zähler:** Anlagenzuordnung, manueller Stand oder lokale Foto-Texterkennung, Vorschläge mit verpflichtender Bestätigung. Kein Zählerfotoarchiv.
- **Kalender:** Monatsansicht, Kraftwerktermine mit Datum/Uhrzeit, Dauer, Fremdfirma, Kontakt, Zuständigem, Anlage und Status.
- **Anleitungen:** Allgemein oder anlagenbezogen, geordnete Arbeitsschritte, optionale verkleinerte Fotos.
- **Bestellungen:** Mehrere Positionen mit Mengen, Artikelangaben, Zweck, optionaler lokaler Anlage und Foto; Mail-Vorschau mit Anhängen. Kein automatischer Versand.

Bilder werden maximal 160 KiB groß gespeichert, insgesamt höchstens 30 Bilder. Zählerfotos werden nach der Verarbeitung gelöscht. Anleitungs-/Bestellbilder sind im Tresor verschlüsselt. Das offline mitgelieferte OCR-Modell vergrößert die APK, ohne Netzwerkzugriff zu benötigen. Kalender ohne System-Synchronisation/Push-Erinnerung.

Testsignaturen können zwischen Builds wechseln: **Vor einer Neuinstallation eine verschlüsselte Sicherung erstellen und das Passwort bereithalten.** Deinstallation löscht lokale Daten.

## Neu in 0.2.0

- Verschlüsselter Kollegenaustausch: Anlagenakte oder einzelnen Wissenseintrag als `.werkshare` teilen; zufälliger Code separat; Importvorschau und neue Anlagenkopie
- Zugangsdaten und Historie nur nach ausdrücklicher Auswahl in Austauschdateien


- Anlagenakte mit **Übersicht, Wissen, Zugängen und Verlauf**
- Hersteller, Modell, Seriennummer, Servicekontakt, Ersatzteil-/Lagerangaben
- Eigene Wissenseinträge mit Bearbeitung und Löschung
- Mehrere verschlüsselte Anlagenzugänge; Passwort nur gezielt für 20 Sekunden sichtbar
- Sicherer lokaler Passwortgenerator (ändert kein Passwort an der echten Anlage)
- Wartungsdatum mit Fälligkeitsübersicht auf der Startseite
- Störungen und Messwerte direkt aus der ausgewählten Anlagenakte erfassen
- Import älterer 0.1.0-Sicherungen; neuere Sicherungen nicht mit 0.1.0 öffnen

**Test-Update:** Vor einer eventuellen Neuinstallation unbedingt eine verschlüsselte Sicherung erstellen. CI-Testsignaturen können wechseln; bei Neuinstallation werden App-Daten gelöscht. Sicherung und Passwort danach in 0.2.0 wiederherstellen.

## Funktionen

- Übersicht offener und dringender Vorgänge
- Anlagenbuch mit Gewerk, Standort und Notizen; Suche und Bearbeitung
- Störungen/Tätigkeiten mit Maßnahmen, Priorität, Status und Zeitaufwand
- Messwerte mit Messpunkt, Einheit, Datum und Notiz; deutsche Dezimalkommas
- Eigene Rundgangschecklisten mit „Ungeprüft“, „In Ordnung“ und „Auffällig“; abgeschlossene Protokolle
- E-Mail-Vorschau für offene Vorgänge und heute erledigte Tätigkeiten; explizite Übergabe an eine E-Mail-App, kein automatischer Versand
- Verschlüsseltes Backup und Wiederherstellung über den Android-Dateidialog
- Lokales Passwort, AES-256-GCM-Datentresor, Sperre beim Wechsel in den Hintergrund, Screenshot-Schutz

Alle Anlagen werden vom Nutzer angelegt. Es sind weder echte Betriebsdaten noch erfundene Sicherheitsgrenzwerte enthalten. Die App ist keine offizielle Anwendung eines Arbeitgebers und steuert keine Anlagen.

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

Keine PDF-/Dateianhänge in Anleitungen, Erinnerungen, Messwertdiagramme, automatische Grenzwertbewertung, GLT-Verbindung, Synchronisation oder Mehrbenutzerverwaltung. Fotos für Anleitungen und Bestellungen sind ab 0.3.0 enthalten. Keine revisionssichere Historie: Einträge sind bearbeitbar. Keine automatische Löschung und kein Passwortwechsel in der UI. Rundgangsvorlagen sind derzeit nach Erstellung nicht editierbar. Ungeprüfte Punkte bleiben im Ergebnis sichtbar; Auffälligkeiten benötigen eine Notiz, erzeugen aber keine Störung automatisch. Datenumfang ist auf 8 MiB pro verschlüsselter Datei begrenzt. Für große Bestände sind Pagination und eine andere verschlüsselte Speicherarchitektur ein späterer Ausbau.

## Kollegenaustausch bedienen

1. Anlage öffnen → **Anlagenakte verschlüsselt teilen**. Alternativ unter Wissen einen einzelnen Eintrag teilen.
2. Bei Bedarf Vorgangshistorie oder Zugangsdaten ausdrücklich einschließen.
3. **Datei & Code erstellen**, Code separat notieren, anschließend **Datei teilen**. Die App übergibt nur die verschlüsselte Datei an Androids Teilen-Dialog.
4. Code z. B. persönlich oder telefonisch übermitteln, nicht im selben Nachrichtenverlauf wie die Datei.
5. Empfänger: WerkLog **Mehr → Anlagenfreigabe eines Kollegen importieren**; Datei wählen, App entsperren, Code eingeben. Alternativ eine korrekt typisierte Freigabedatei direkt mit WerkLog öffnen.
6. Inhalt prüfen, dann als neue Anlage importieren. Bestehende Einträge bleiben unverändert.

Wer Datei und Code besitzt, kann den Inhalt entschlüsseln. Keine Fernlöschung, kein nachträglicher Widerruf. Die App muss beim Import offen bleiben; beim Verlassen wird die entschlüsselte Vorschau verworfen.

Datenschutzdetails: [docs/SECURITY.md](docs/SECURITY.md). Abnahme: [docs/DEVICE-TEST.md](docs/DEVICE-TEST.md).

## iPhone / iPad

Der aktuelle Build ist ausschließlich Android. Kotlin/Jetpack Compose, Android-Kamera, FileProvider, Keystore-/Speicherzugriff und OCR sind keine direkt installierbare iOS-App. Eine spätere Portierung kann Kernlogik und das dokumentierte AES-GCM-/JSON-Austauschformat teilen, benötigt aber eine eigene iOS-Oberfläche/Plattformintegration, Apple-Buildumgebung, Signierung und Gerätetests. Eine iOS-Datei wird derzeit nicht angeboten.
