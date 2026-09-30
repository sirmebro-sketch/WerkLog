# WerkLog

Lokaler Android-Companion für Servicetechniker, in **Kotlin und Jetpack Compose**. Kein Konto, keine Internetberechtigung, keine Java-Quelldateien. Öffentlich ist der Quellcode; Arbeitsdaten liegen ausschließlich im verschlüsselten Gerätespeicher.

## 0.5.0 — Testversion

Neu: lokale Kontakte mit beidseitigen Anlagen-/Vorgangsbezügen, Anlagenbilder, kompakte gefilterte Anlagenliste und eigene Gewerke. Bestellungen aus Störungen starten mit leerer Teileliste und separaten editierbaren Grundinformationen. Anleitungen sind aus Anlage und Vorgang erreichbar. Entsperren stellt offene Formulare einschließlich Eingaben wieder her; eingerichtete Biometrie wird automatisch angeboten. Arbeitsprotokoll = Störungen und Arbeiten; Textvorlagen = wiederverwendbare Texte. Sicherungen liegen nur in Einstellungen.

Entwürfe vor Gerätewechsel speichern; sie sind lokal verschlüsselt, aber nicht Teil des Sicherungsexports. Neue Anlagenfreigaben benötigen auf beiden Geräten mindestens 0.5.0.

### Ergänzungen aus 0.4.2

Kamera-Ziel und temporäres Foto überleben die Android-Neuerstellung. Normale Bildschirmdrehung sperrt den Tresor nicht; Bildschirm aus und regulärer App-Wechsel bleiben geschützt. Nach einem Prozessneustart ist weiterhin das Passwort nötig, eine wiederhergestellte ausstehende Aufnahme kann danach bestätigt werden (temporäre Wiederaufnahme maximal 15 Minuten). Fotos öffnen auf Wunsch bildschirmfüllend mit Zwei-Finger-Zoom bis 6×. In Anleitungen bleibt „+ Nächster Schritt“ oben erreichbar; Anleitungseinstellungen stehen unter den Schritten.

### Ergänzungen aus 0.4.1

Neu: kompakte App-Führung ohne Abdunklung, automatisches freiwilliges Fingerabdruck-Angebot nach dem Anlegen des Passworts, lokales Profil unter Einstellung (Name, Foto, Funktion, Bereich und dienstliche Kontakte) sowie ein Kraftwerk-Symbol für Betrieb. Profilinformationen liegen im verschlüsselten Tresor, sind in Vollsicherungen enthalten und werden nicht automatisch per Mail oder Anlagenfreigabe weitergegeben. Profilfoto bis 384 Pixel/96 KiB. Ältere Dateien bleiben lesbar.

Drei Hauptpunkte: **Heute · Betrieb · Einstellung**. Betrieb ist der hervorgehobene mittlere Knopf mit einer zweispaltigen Kachelübersicht. Die kurze Einführung erklärt die wichtigsten Bereiche und ist in Einstellung erneut verfügbar. Beim ersten Start erscheint Heute; nach einer Sperre wird der letzte Arbeitsstand wieder aufgenommen.

### Alltag

- Anlagenakten mit Kennzeichen, Hierarchie, Favoriten, Verlauf, Wissenseinträgen und lokalen Zugangsdaten; Bearbeiten/Löschen mit Bestätigung.
- Suche über Anlagen, Wissen und Anleitungen; QR-Code je Anlage anzeigen/teilen oder offline fotografieren.
- Tätigkeiten/Störungen, eigene Vorlagen, Duplizieren, Folgetermin und Teileanforderung direkt aus dem Vorgang.
- Zählerfoto-Texterkennung offline mit Bestätigung; kein gespeichertes Zählerfoto. Differenzen, Verlauf, eigene Prüfgrenzen und dokumentierter Zählerwechsel.
- Eigene Rundgangsvorlagen bearbeiten/löschen, historische Protokolle erhalten.
- Kalender mit Fremdfirma, Ansprechpartner, Zuständigkeit, Anlage, Wiederholungen und lokalen Erinnerungen. Einzelne Termine bewusst an den Systemkalender übergeben.
- Anleitungen mit geordneten Schritten, Bildern, Version und Prüfdatum.
- Bestelllisten mit mehreren Positionen/Bildern, privater Anlagenzuordnung, Status und Lieferdatum; Mail-Vorschau, kein automatischer Versand.

### Tresor und Bilder

**32 MiB Metadaten plus bis zu 500 Bilder à 512 KiB** (etwa 250 MiB Bilder, abhängig von Kompression weniger). Bilder liegen einzeln AES-GCM-verschlüsselt; Textänderungen schreiben sie nicht erneut. Kamera-/Galeriebilder werden verkleinert, Metadaten entfernt. Ein ausgewähltes Galerie-Original bleibt außerhalb der App unverändert.

Vollsicherungen und Kollegenaustausch werden bildweise verarbeitet (Container höchstens 384 MiB), sodass nicht alle Bilddaten zugleich im Speicher liegen. Alte Sicherungen lesbar; neue Archive benötigen 0.5.0. Speicherübersicht unter Einstellung, Erinnerung an fehlende Sicherung nach sieben Tagen.

Lokales Passwort ändern verschlüsselt den gesamten Tresor neu. **Alte Sicherungen behalten ihr altes Passwort.** Starke Biometrie optional über Android Keystore. Beim gewöhnlichen Verlassen Sperre; Dateiauswahl/Kollegenaustausch erlauben Wechsel von maximal zwei Minuten. Bildschirm aus oder manuelle Sperre sperrt sofort. Kein Wiederherstellen eines vergessenen Passworts.

### Austausch

Anlage oder Wissenseintrag verschlüsselt mit separatem Zufallscode teilen; Passwörter nur nach ausdrücklicher Auswahl. Importvorschau, neue Anlage oder Zuordnung zu vorhandener Anlage. Standardmäßig nur ergänzen; bestehende Inhalte nur mit ausdrücklichem Ersetzen ändern. Kein Cloud-Abgleich. Jede Person mit Datei **und** Code kann die Inhalte entschlüsseln.

## Qualität und verbleibende Arbeit

Am 30.09.2026 für 0.5.0 erfolgreich: [Android-Build mit 49 JVM-Tests, Lint und APK-Netzwerkprüfung](https://github.com/sirmebro-sketch/WerkLog/actions/runs/36726158921) und [Bedienablauf im Android-15-Emulator](https://github.com/sirmebro-sketch/WerkLog/actions/runs/36726159176). Der Emulator prüft Einrichtung, Anlagenbearbeitung, Profil, Passwortwechsel, Kamera-Rückgabe nach Activity-Neuerstellung, Bildansicht, verschlüsselte Formularwiederaufnahme nach Sperren, leere Bestellung aus einer Störung mit separatem Hintergrund, vorbelegte Anlagenzuordnung eines Teils und beidseitige Kontaktverknüpfungen. Ein zweiter Gerätetest prüft den verschlüsselten Entwurfs-Roundtrip einschließlich primitiver Compose-Zustände und Manipulationserkennung. Kamera, reale Zählererkennung, Biometrie und Mail-App müssen auf dem S24 Ultra erprobt werden. Keine produktive Gerätefreigabe behauptet.

APK `WerkLog-0.5.0-Test.apk`, SHA-256 `de508d7c9b74a760ad262dcf89247deab5ee71c71147096b1bc3a0881afc0c83`. App-Quellstand: `cec8c6d7655076daf8a0c5fa709dc6782df0435f`.

- [Umsetzung und offene Punkte](docs/ROADMAP.md)
- [Geräteabnahme](docs/DEVICE-TEST.md)
- [Datenschutz und Sicherheitsgrenzen](docs/SECURITY.md)
- [Offline-Dateiformat](docs/FORMAT.md)
- [Dauerhafte Release-Signatur einrichten](docs/RELEASE-SIGNING.md)
- [Apple-Port: bisher nur Dateiformat-Kern](apple/README.md)

**Vor einer möglichen Neuinstallation verschlüsselte Sicherung erstellen und Passwort bereithalten.** CI-Debugsignaturen können wechseln, Deinstallation löscht App-Daten. Der Release-Workflow braucht einmalig private GitHub-Secrets; keinesfalls Schlüssel ins öffentliche Repository legen.

## Bauen

JDK 17, Gradle 8.9, Android SDK 35; ab Android 8.0/API 26. Netzwerk nur beim Einrichten/Bauen nötig.

```sh
gradle testDebugUnitTest lintDebug assembleDebug
```

Kein Gradle-Wrapper-Binary im Repository. Mit lokal installiertem Gradle kann `gradle wrapper --gradle-version 8.9` erzeugt werden. APK-Artefakte drei Tage aufbewahrt, keine Gradle-Caches. Keine automatische Veröffentlichung in Stores.
