# WerkLog

Lokaler Android-Companion für Servicetechniker, in **Kotlin und Jetpack Compose**. Kein Konto, keine Internetberechtigung, keine Java-Quelldateien. Öffentlich ist der Quellcode; Arbeitsdaten liegen ausschließlich im verschlüsselten Gerätespeicher.

## 0.7.0 — Vier Farbkonzepte

Unter **Einstellung → Darstellung** stehen das überarbeitete **Petrol & Mint**, kühles **Stahlblau**, warmes **Graphit & Kupfer** und der helle Modus **Tageslicht** zur Wahl. Alle ändern Hintergrund, Karten, Text, Aktionen und Navigation, einschließlich Formulare, Dialoge, Bildansicht und Sperrbildschirm. Klare Konturen trennen Karten und Felder. Die Auswahl wirkt sofort, ohne offene Formulare zu schließen, und bleibt lokal auf dem Gerät gespeichert.

Optional **Geräteeinstellung folgen**: Bei hellem Gerätedesign gilt Tageslicht, bei dunklem das zuletzt gewählte dunkle Konzept. Eine manuelle Auswahl beendet diese Automatik. Die Darstellungspräferenz enthält nur Farb-IDs; sie gehört nicht zum Tresor, Sicherungsexport oder Kollegenaustausch.

Statusanzeigen haben klare Bedeutungen und zusätzliche Texte/Symbole: offen neutral, laufend blau, wichtig/Hinweis orange, dringend/überfällig/fehlerhaft rot und abgeschlossen grün. Diese Bedeutungen ändern sich beim Farbwechsel nicht. QR-Codes behalten Schwarz/Weiß. Details: [Farbkonzepte 0.7.0](docs/UI-REVIEW-0.7.0.md).

### Ergänzungen aus 0.6.1 — Einheitliche Kacheln und klare Rückwege

Alle Betriebskacheln haben dieselbe Größe, passende Beschreibungen und gemeinsame Text-/Symbolbereiche. Das Raster berücksichtigt große Systemschrift und lässt die letzte Kachel nicht über die ganze Zeile wachsen. Auch die aktive Markierung in der unteren Navigation verwendet die App-Farben. Ein fester Zurück-Pfeil führt aus Details zur Liste und bei Verknüpfungen zum Ausgangsdatensatz; diese Rückwege bleiben nach einer Sperre erhalten. Aus Störungen erzeugte Folgetermine öffnen direkt ihre Bearbeitung. Entsperren ist auch mit „Fertig“ an der Bildschirmtastatur möglich. Prüfung und Grenzen: [UI-Review 0.6.1](docs/UI-REVIEW-0.6.1.md).

### Ergänzungen aus 0.6.0

Neu: Fälligkeiten für Arbeiten/Störungen mit Anzeige in „Heute“, erwartete Lieferungen, Suchfelder für Anleitungen und Bestelllisten sowie Filter für offene/erledigte Bestellungen. Große Übersichten zeigen zunächst 40 Datensätze und laden weitere auf Wunsch nach; die Suche berücksichtigt alle Datensätze. Kontakte öffnen die Telefon- oder E-Mail-App ohne automatischen Anruf/Versand.

Speicherfehler lassen Formulare und Eingaben offen. Fotos und Datensätze werden erst nach bestätigter Speicherung abgeschlossen. Kollegenimporte übernehmen Anlagenbilder vollständig in den eigenen Tresor; fehlgeschlagene Importe lassen sich erneut versuchen. Hierarchie, Verknüpfungen, Bildgrenzen, QR-Codes und lange Terminserien wurden zusätzlich abgesichert. Details und Prüfgrenzen: [Gesamtprüfung 0.6.0](docs/AUDIT-0.6.0.md).

Offene Formulare vor Installation/Wechsel der App-Version speichern und eine verschlüsselte Vollsicherung erstellen. Neue Sicherungen und Anlagenpakete verwenden Schema 7 und benötigen auf Empfängerseite mindestens 0.6.0. Ältere Dateien bleiben lesbar.

### Ergänzungen aus 0.5.0

Neu: lokale Kontakte mit beidseitigen Anlagen-/Vorgangsbezügen, Anlagenbilder, kompakte gefilterte Anlagenliste und eigene Gewerke. Bestellungen aus Störungen starten mit leerer Teileliste und separaten editierbaren Grundinformationen. Anleitungen sind aus Anlage und Vorgang erreichbar. Entsperren stellt offene Formulare einschließlich Eingaben wieder her; eingerichtete Biometrie wird automatisch angeboten. Arbeitsprotokoll = Störungen und Arbeiten; Textvorlagen = wiederverwendbare Texte. Sicherungen liegen nur in Einstellungen.

Entwürfe vor Gerätewechsel speichern; sie sind lokal verschlüsselt, aber nicht Teil des Sicherungsexports. Anlagenfreigaben aus 0.5.0 benötigen auf beiden Geräten mindestens 0.5.0.

### Ergänzungen aus 0.4.2

Kamera-Ziel und temporäres Foto überleben die Android-Neuerstellung. Normale Bildschirmdrehung sperrt den Tresor nicht; Bildschirm aus und regulärer App-Wechsel bleiben geschützt. Nach einem Prozessneustart ist weiterhin das Passwort nötig, eine wiederhergestellte ausstehende Aufnahme kann danach bestätigt werden (temporäre Wiederaufnahme maximal 15 Minuten). Fotos öffnen auf Wunsch bildschirmfüllend mit Zwei-Finger-Zoom bis 6×. In Anleitungen bleibt „+ Nächster Schritt“ oben erreichbar; Anleitungseinstellungen stehen unter den Schritten.

### Ergänzungen aus 0.4.1

Neu: kompakte App-Führung ohne Abdunklung, automatisches freiwilliges Fingerabdruck-Angebot nach dem Anlegen des Passworts, lokales Profil unter Einstellung (Name, Foto, Funktion, Bereich und dienstliche Kontakte) sowie ein Kraftwerk-Symbol für Betrieb. Profilinformationen liegen im verschlüsselten Tresor, sind in Vollsicherungen enthalten und werden nicht automatisch per Mail oder Anlagenfreigabe weitergegeben. Profilfoto bis 384 Pixel/96 KiB. Ältere Dateien bleiben lesbar.

Drei Hauptpunkte: **Heute · Betrieb · Einstellung**. Betrieb ist der hervorgehobene mittlere Knopf mit einer zweispaltigen Kachelübersicht. Die kurze Einführung erklärt die wichtigsten Bereiche und ist in Einstellung erneut verfügbar. Beim ersten Start erscheint Heute; nach einer Sperre wird der letzte Arbeitsstand wieder aufgenommen.

### Alltag

- Anlagenakten mit Kennzeichen, Hierarchie, Favoriten, Verlauf, Wissenseinträgen und lokalen Zugangsdaten; Bearbeiten/Löschen mit Bestätigung.
- Suche über Anlagen, Wissen und Anleitungen; QR-Code je Anlage anzeigen/teilen oder offline fotografieren.
- Tätigkeiten/Störungen mit optionaler Fälligkeit, eigene Textvorlagen, Duplizieren, Folgetermin und Teileanforderung direkt aus dem Vorgang.
- Zählerfoto-Texterkennung offline mit Bestätigung; kein gespeichertes Zählerfoto. Differenzen, Verlauf, eigene Prüfgrenzen und dokumentierter Zählerwechsel.
- Eigene Rundgangsvorlagen bearbeiten/löschen, historische Protokolle erhalten.
- Kalender mit Fremdfirma, Ansprechpartner, Zuständigkeit, Anlage, Wiederholungen und lokalen Erinnerungen. Einzelne Termine bewusst an den Systemkalender übergeben.
- Anleitungen mit geordneten Schritten, Bildern, Version und Prüfdatum.
- Bestelllisten mit mehreren Positionen/Bildern, privater Anlagenzuordnung, Status und Lieferdatum; Mail-Vorschau, kein automatischer Versand.

### Tresor und Bilder

**32 MiB Metadaten plus bis zu 500 Bilder à 512 KiB** (etwa 250 MiB Bilder, abhängig von Kompression weniger). Bilder liegen einzeln AES-GCM-verschlüsselt; Textänderungen schreiben sie nicht erneut. Kamera-/Galeriebilder werden verkleinert, Metadaten entfernt. Ein ausgewähltes Galerie-Original bleibt außerhalb der App unverändert.

Vollsicherungen und Kollegenaustausch werden bildweise verarbeitet (Container höchstens 384 MiB), sodass nicht alle Bilddaten zugleich im Speicher liegen. Alte Sicherungen lesbar; aktuelle Archive benötigen 0.6.0. Speicherübersicht unter Einstellung, Erinnerung an fehlende Sicherung nach sieben Tagen.

Lokales Passwort ändern verschlüsselt den gesamten Tresor neu. **Alte Sicherungen behalten ihr altes Passwort.** Starke Biometrie optional über Android Keystore. Beim gewöhnlichen Verlassen Sperre; Dateiauswahl/Kollegenaustausch erlauben Wechsel von maximal zwei Minuten. Bildschirm aus oder manuelle Sperre sperrt sofort. Kein Wiederherstellen eines vergessenen Passworts.

### Austausch

Anlage oder Wissenseintrag verschlüsselt mit separatem Zufallscode teilen; Passwörter nur nach ausdrücklicher Auswahl. Importvorschau, neue Anlage oder Zuordnung zu vorhandener Anlage. Standardmäßig nur ergänzen; bestehende Inhalte nur mit ausdrücklichem Ersetzen ändern. Kein Cloud-Abgleich. Jede Person mit Datei **und** Code kann die Inhalte entschlüsseln.

## Qualität und verbleibende Arbeit

Am 30.09.2026 für 0.7.0 erfolgreich: [Android-Build mit 64 JVM-Tests, Lint und APK-Netzwerkprüfung](https://github.com/sirmebro-sketch/WerkLog/actions/runs/36776822398) und [vier Android-15-Instrumentierungstests](https://github.com/sirmebro-sketch/WerkLog/actions/runs/36776822241). Die Farbprüfungen sichern die zentralen Text- und Konturkontraste sowie die Statusbedeutungen. Alle neun Betriebskacheln wurden mit allen vier produktiven Themes bei 320 dp Breite und 100 %/180 % Schrift auf gleiche Maße, Textüberläufe und funktionierende Aufrufe geprüft. Der neue Bedienablauf prüft sofortigen Farbwechsel, gespeicherte Auswahl, Systemoption, Leistenhelligkeit und erhaltenen Formulartext nach Farbwechsel, Sperren und Activity-Neuerstellung. Die bisherige Navigation, Speicherfehlerbehandlung, Kamera-Rückgabe, Passwortrotation, Bestellverknüpfung und JPEG-Anlagenfreigabe bleiben erfolgreich geprüft.

Bildschirmaufnahmen der vier Betrieb-Varianten sowie der hellen Einstellungen, Formulare, Heute-Ansicht, Kalender, Statusanzeigen, Anleitung, Vollbildfoto und Sperrmaske wurden visuell geprüft. Physische Einhandbedienung, TalkBack, Lichtverhältnisse, maximale Samsung-Anzeigeeinstellungen, Kamera, reale Zählererkennung, Biometrie und Mail-App brauchen weiterhin die S24-Ultra-Abnahme.

APK `WerkLog-0.7.0-Test.apk`, 61.129.244 Bytes; SHA-256 `240bc65f3c9d4a969d9d25b6b00f3220bc1f851e01df4be52ae91315642d9df1`. Geprüfter App-Quellstand: `4d3762dffb753ab2421bc39197484a4f9bd60bbd`.

- [Farbkonzepte und Prüfung 0.7.0](docs/UI-REVIEW-0.7.0.md)

- [Design und Navigation 0.6.1](docs/UI-REVIEW-0.6.1.md)
- [Gesamtprüfung und Verbesserungen 0.6.0](docs/AUDIT-0.6.0.md)
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
