# Abnahme auf Samsung Galaxy S24 Ultra

Automatisierte Ergebnisse stehen nach Versionen geordnet unten. Die Geräte-Checklisten bleiben bis zum tatsächlichen S24-Ultra-Test offen; Emulatorprüfungen schließen sie nicht ab.

- [x] Frühere Abnahme 0.3.0: 29 JVM-Tests, lintDebug, assembleDebug und APK-Netzwerkberechtigungsprüfung erfolgreich (Run 36580771959).
- [ ] Installieren; Passwort anlegen; Start ohne Internet möglich.
- [ ] Falsches Passwort entsperrt nicht. Richtiges Passwort stellt Daten wieder her.
- [ ] App-Wechsel, Home, Bildschirm aus/an und Prozessende sperren die App.
- [ ] Screenshot und Übersicht letzter Apps zeigen keine Arbeitsdaten.
- [ ] Anlage anlegen und ändern; Sonderzeichen und lange Texte testen.
- [ ] Eintrag anlegen; Offen → In Arbeit → Erledigt; nach Neustart korrekt.
- [ ] Messwert 1,25 und negativer Wert korrekt; leere/ungültige Werte abgewiesen.
- [ ] Rundgang mit ungeprüften Punkten korrekt; Auffälligkeit benötigt Notiz.
- [ ] E-Mail-Vorschau prüfen, abbrechen, erneut öffnen; keine Mail ohne eigene Versandaktion.
- [ ] Verhalten ohne installierte E-Mail-App verständlich.
- [ ] Backup exportieren; nach Entsperren weiterarbeiten.
- [ ] Backup laden mit falschem Passwort: bestehende Daten bleiben.
- [ ] Manipuliertes/zu großes Backup abweisen, bestehende Daten bleiben.
- [ ] Gültige Sicherung nach ausdrücklichem Ersetzen vollständig herstellen.
- [ ] Speichern bei wenig Speicherplatz: Fehler sichtbar, alter Stand wieder lesbar.
- [ ] App während KDF/Speichern verlassen: kein selbsttätiges Entsperren beim Zurückkehren.
- [ ] Schriftgröße 150 %, kleine Breite, Display-Ausschnitt, Tastatur, Gestennavigation.
- [ ] Update mit demselben Release-Schlüssel erhält Daten.

Nicht mit echten sensiblen Betriebsdaten testen, solange Schutz und Freigaben offen sind.

## Zusätzliche Abnahme 0.2.0

- [ ] Sicherung aus 0.1.0 importieren: Stammdaten und Verlauf bleiben vorhanden.
- [ ] Zwei Anlagen mit mehreren unterschiedlichen Zugängen; keine Vermischung.
- [ ] Passwort mit führenden/abschließenden Leerzeichen unverändert speichern.
- [ ] Passwort nach 20 Sekunden, Tabwechsel und App-Wechsel verborgen.
- [ ] Passwortvorschlag verwerfen ändert den gespeicherten Zugang nicht.
- [ ] Bearbeiten/Löschen von Wissen und Zugang nur für gewählten Datensatz.
- [ ] E-Mail-Vorschau enthält keine Zugangsdaten und keine privaten Wissenseinträge.
- [ ] Wartung überfällig/heute/in 30 Tagen/fertig gepflegt im Dashboard prüfen.
- [ ] Tätigkeit aus Anlagenakte übernimmt die gewählte Anlage.
- [ ] Backup enthält neue Felder; nach Wiederherstellung vollständig vorhanden.

## Austausch mit zweitem Gerät / zweiter Installation

- [ ] Ganze Anlagenakte ohne Zugänge/Historie senden; Empfänger sieht nur ausgewählte Inhalte.
- [ ] Einzelnes Wissen teilen: kein Standort, keine sonstigen Notizen/Zugänge/Historie enthalten.
- [ ] Freigabe inklusive Zugängen explizit wählen; Ergebnis gehört zur neu importierten Anlage.
- [ ] Falscher Code oder veränderte Datei: keine Inhalte sichtbar, keine Daten importiert.
- [ ] Share-Datei ist keine Vollsicherung und umgekehrt; beide falschen Importe werden abgewiesen.
- [ ] E-Mail-Anhang enthält weder Code noch Gerätepasswort in Betreff/Dateiname/Text.
- [ ] Dateiauswahl, Codeeingabe und Vorschau vollständig durchlaufen; ab 0.4.0 kurze Wechsel innerhalb der Austauschfrist ohne erneutes Entsperren.
- [ ] Abbruch sowie App-Wechsel während Entschlüsseln zeigen keine Vorschau nach erneutem Entsperren.
- [ ] Import als neue Kopie lässt bestehende Daten unverändert; ab 0.4.0 ist alternativ eine ausdrückliche Zuordnung zu einer vorhandenen Anlage möglich.
- [ ] Große Schrift / kleine Displays: Vorschau und Code vollständig lesbar.

## Zusätzliche Geräteabnahme 0.3.0

- [ ] Kaltstart im Flugmodus: OCR ohne vorherigen Modell-Download verfügbar.
- [ ] Zwei Zähler an verschiedenen Anlagen: Foto, korrekte Zuordnung, führende Nullen/Nachkomma prüfen; absichtliche manuelle Bestätigung.
- [ ] Unlesbares Foto → manueller Wert; keine automatische Speicherung.
- [ ] Kameraabbruch, Prozessende, Rotation und Rückkehr: temporäre Bilder gelöscht, kein Absturz, ggf. verständliche Wiederholung.
- [ ] Anleitungsfoto im Hoch-/Querformat, große Aufnahme: Orientierung korrekt, Bild klein und nach Neustart vorhanden.
- [ ] Schritt ergänzen, bearbeiten, nach oben verschieben, mit/ohne Bild; allgemeine und anlagenbezogene Anleitung.
- [ ] Historischer 0.3.0-Grenztest: 30 Bilder/8 MiB; in 0.4.0 durch 500 Bilder plus 32 MiB Metadaten ersetzt. Übergrenzen-Import erhält weiterhin den alten gültigen Stand.
- [ ] Kalender Monatswechsel, heutiger Tag, mehrere Termine, Fremdfirma, Kontakt, Zuständiger, Abgesagt/Erledigt.
- [ ] Bestellliste mit mehreren Positionen, Bildern, lokaler Anlagenzuordnung; E-Mail-Vorschau und tatsächliche Anhänge in verwendeter Mail-App prüfen.
- [ ] Mail-Entwurf nicht abgesendet: in WerkLog niemals als automatisch bestellt/gesendet markiert.
- [ ] Bestellanhang nach 15 Minuten/Prozessende ggf. erneut erstellen; keine Klartextbilder im lokalen Dateiverzeichnis.
- [ ] Schema-1-/Schema-2-Backups importieren; Schema-3-Backup mit allen neuen Modulen wiederherstellen.
- [ ] Anlagenfreigabe mit bebilderter Anleitung/Zählermessungen auf zweiter 0.3.0-Installation importieren.

## Abnahme 0.4.0

Automatisierte Abnahme vom 29.09.2026, Android-Code `427bfd08c5a695fe4d1ad8835ce70f4afe4fcfb9`:

- [x] [Android-CI](https://github.com/sirmebro-sketch/WerkLog/actions/runs/36620707570): 40 JVM-Tests, Lint, APK-Build und Prüfung ohne Internet-/Netzwerkstatus-Berechtigung erfolgreich.
- [x] Repository-Tests: 500 synthetische Bilder, Backup >8 MiB, falsches Passwort, fehlende/manipulierte Bilder, Pfadmanipulation, Migration, Passwortrotation, große Freigaben und gemeinsame Android/Apple-Kryptovektoren.
- [x] [Android-15-Emulator](https://github.com/sirmebro-sketch/WerkLog/actions/runs/36620707773): Einrichtung, Einführung überspringen, Betrieb öffnen, Anlage anlegen/bearbeiten, Passwort ändern, manuell sperren und mit neuem Passwort auf Heute entsperren.
- [x] Betrieb-Screenshot visuell geprüft: acht beschriftete Kacheln, genau drei Hauptpunkte, hervorgehobener mittlerer Betrieb-Knopf; keine abgeschnittenen Beschriftungen bei Standardschrift.
- [x] [Swift-Paket](https://github.com/sirmebro-sketch/WerkLog/actions/runs/36618330660): Codec-Test auf macOS erfolgreich; kein iOS-Oberflächentest.

Ausgelieferte Test-APK: `WerkLog-0.4.0-Test.apk`, SHA-256 `9e7af145d5f7b63ea997f5f393adb97219cb7cbfe0601ca7e3dcc52c192db5f5`.

Die folgenden Punkte brauchen weiterhin ein reales Gerät. Die synthetischen Speicherprüfungen ersetzen keine Aufnahme/Anzeige von 200 echten Fotos.

- [ ] Drei Hauptpunkte; Betrieb auffälliger und mittig, Kacheln mit Symbol; 150 % Schrift.
- [ ] Start/Entsperren auf Heute; Datum/Wochentag und wiederkehrende anstehende Termine korrekt.
- [ ] Einführung folgt den Seiten, überspringbar und erneut aufrufbar.
- [ ] Eigene 200+ Fotos erfassen, Neustart, Backup auf anderes Gerät und Wiederherstellung.
- [ ] Passwortwechsel mit 200+ Bildern; falsches altes Passwort ändert nichts; altes/neues Backup brauchen jeweils richtiges Passwort.
- [ ] Biometrie aktivieren, abbrechen, entsperren, neu eingeschriebenen Finger prüfen; Passwortfallback.
- [ ] Datei auswählen, Code aus anderer App holen: kein unnötiger Sperrbildschirm innerhalb zwei Minuten.
- [ ] Während Austausch Bildschirm aus: sofort sperren. Nach >2 Minuten Hintergrund ebenfalls sperren.
- [ ] Beim gewöhnlichen App-Wechsel weiterhin sofort sperren. Manueller Schlossknopf ebenso.
- [ ] Löschen von Anlage zeigt Folgen; verknüpfte Termine/Bestellungen/Anleitungen bleiben ohne Zuordnung.
- [ ] Rundgangsvorlage bearbeiten/löschen verändert vorhandenes Protokoll nicht.
- [ ] QR erzeugen, drucken und lesen; doppeltes Kennzeichen nicht automatisch zuordnen.
- [ ] Wiederholungen am Monatsende, Benachrichtigungen mit gesperrtem Tresor und nach Neustart.
- [ ] Empfänger-Mail-App liest alle bewusst freigegebenen Bilder; kein automatischer Versand.


## Abnahme 0.4.1

Am 29.09.2026 erfolgreich, Android-Code `99562b1a2be77b9b0baea29496a6c324193c70de`:

- [x] [Android-CI](https://github.com/sirmebro-sketch/WerkLog/actions/runs/36622947048): 44 JVM-Tests, Lint, APK-Build und keine Netzwerkberechtigung.
- [x] Profiltests: alte Daten ohne Profil, Profil-Roundtrip, verschlüsselte Sicherung/Wiederherstellung, Bildlimit, kein eigenes Profil im Anlagenpaket und kein Überschreiben des Empfängerprofils beim Import.
- [x] [Emulator-Bedienablauf](https://github.com/sirmebro-sketch/WerkLog/actions/runs/36622947260): automatisches Biometrie-Angebot mit Später, Führung mit sichtbarer Anlagen-Kachel, Profilanlage, Passwortwechsel und anschließende persönliche Begrüßung.
- [x] Screenshots visuell kontrolliert: kompakte Führung oberhalb der Navigation, acht Betrieb-Kacheln sichtbar, passendes Kraftwerk-Symbol.

APK `WerkLog-0.4.1-Test.apk`, SHA-256 `b62ae8e65b1ec66a3e505596eb988607e5f545275ccbf2f7564a9d02ab1f946d`.

Noch auf dem realen Gerät prüfen:

- [ ] Auf echtem Gerät neu einrichten: nach Passwort automatisch Fingerabdruck-Angebot; Aktivieren, Abbrechen und Später testen.
- [ ] Führung ohne Abdunklung: Seite und Navigation bleiben sichtbar; Weiter, Zurück, Überspringen, Neustart über Einstellung.
- [ ] Führung bei 150 % Schrift und im Querformat: Text und Aktionen erreichbar.
- [ ] Profil mit Name, Funktion, Bereich und Dienstkontakt erstellen, bearbeiten und löschen; Arbeitsdaten bleiben erhalten.
- [ ] Profilfoto aus Kamera/Galerie, Ausrichtung, Bildaustausch und Entfernen prüfen.
- [ ] Profil nach Sperren/Entsperren, Neustart, Passwortwechsel und Vollsicherungs-Wiederherstellung vorhanden.
- [ ] Anlagenfreigabe und Mail-Vorschau enthalten keine ungefragt übernommenen Profildaten.


## Abnahme 0.4.2

Automatisiert erfolgreich am 30.09.2026:

- [x] [Android-CI](https://github.com/sirmebro-sketch/WerkLog/actions/runs/36665143563): 44 JVM-Tests, Lint, APK und Netzwerkberechtigungsprüfung.
- [x] [Android-15-Emulator](https://github.com/sirmebro-sketch/WerkLog/actions/runs/36665143503): kompletter bisheriger Einrichtungs-/Profil-/Passwortablauf plus Anleitung mit zwei Schritten; feststehender Schritt-Knopf beim Scrollen zu den Einstellungen.
- [x] Synthetische Aufnahme samt Ziel hinterlegt, App-Hintergrund und Activity-Neuerstellung durchlaufen, ungesperrte Sitzung und erhaltene Datei geprüft, Kamera-Erfolg simuliert, Vollbild geöffnet/geschlossen und gespeichert. Nur der zweite Anleitungsschritt erhält das verschlüsselte Bild; Rohdatei danach gelöscht.

Dies simuliert die Android-Rückgabe, nicht die reale Samsung-Kamera. Echtes Prozessende, Finger-Gesten und gerätespezifische Kamera bleiben Gerätechecks.

APK `WerkLog-0.4.2-Test.apk`, SHA-256 `faa198f813930df7147d07351c447cd4d7430fac713c08779aaa30fb64db2a94`.

- [ ] S24-Kamera: Anleitungsschritt und Bestellposition fotografieren, zurückkehren und speichern; Zuordnung bleibt richtig.
- [ ] Kamera im Querformat verwenden und zur App im Hochformat zurückkehren.
- [ ] Während Kamera Bildschirm sperren: nach Entsperren Foto prüfen, keine offene Tresorsitzung ohne Authentifizierung.
- [ ] Kamera länger als zwei Minuten offen: App verlangt Passwort, ausstehendes Foto anschließend bestätigen.
- [ ] Prozessneustart während externer Kamera (Android stellt Instanzzustand wieder her): Passwort erforderlich, Aufnahme innerhalb 15 Minuten wieder aufnehmbar.
- [ ] Foto in Vollbild öffnen, mit zwei Fingern vergrößern/verschieben, Zurücksetzen, Schließen; Screenshot-Schutz.
- [ ] Lange Anleitung: Schritt-Knopf bleibt oben erreichbar, Anleitungseinstellungen stehen unter den Schritten.

## Abnahme 0.5.0

- Störung mit Notiz öffnen, Text ändern, Teileanforderung starten: geänderter Hintergrund separat, keine Position; erstes Teil leer, Anlage vorbelegt. Grundinformationen bearbeiten. E-Mail-Vorschau ohne private Notiz/Anlagenkontakt.
- Kontakt anlegen; mehreren Anlagen und einem Vorgang zuordnen. Von jeder Seite Kontakt öffnen, vom Kontakt Anlage/Vorgang öffnen. Verknüpfung entfernen und Löschung prüfen.
- Anleitung einer bestehenden Anlage zuordnen; aus Anlagenakte öffnen. Anleitung einem Vorgang zuordnen; Rückverweis in Anleitung prüfen.
- Anlagenbild aufnehmen/ersetzen, Vollbild öffnen, nach Sperren und Sicherungswiederherstellung prüfen. Gemeinsame Bildgrenze 500.
- 40+ Anlagen: kompakte Liste, Suchbegriff, Gewerkfilter, Favoriten und Bildvorschau prüfen.
- Gewerke hinzufügen/umbenennen/löschen; Ersatzgewerk wählen, betroffene Anlagen prüfen.
- Unfertigen Arbeitsschritt eingeben, App verlassen, Prozess/Activity neu erstellen und entsperren: Seite, Dialog, Schritt und Text erhalten. Danach bewusst speichern oder abbrechen.
- Auf echter Hardware: eingerichtete Biometrie startet beim Entsperrbildschirm automatisch, Abbrechen erlaubt Passworteingabe ohne Prompt-Schleife.

Ergebnis 30.09.2026: 49 JVM-Tests, Lint und Prüfung der gepackten Netzwerkberechtigungen erfolgreich ([Build](https://github.com/sirmebro-sketch/WerkLog/actions/runs/36726158921)). Beide Instrumentierungstests erfolgreich ([Emulator](https://github.com/sirmebro-sketch/WerkLog/actions/runs/36726159176)). Der Kontakt-Test scrollt vor dem Klick zum eindeutig beschrifteten Kontrollkästchen und prüft dessen ausgewählten Zustand. Reale Fingerabdruck-, Kamera-, Zähler-OCR- und Mail-Abnahme bleiben Geräteaufgaben.

APK `WerkLog-0.5.0-Test.apk`, SHA-256 `de508d7c9b74a760ad262dcf89247deab5ee71c71147096b1bc3a0881afc0c83`. Vor Installation offene Formulare speichern und vollständige Sicherung exportieren. Stabile Release-Signierung ist weiterhin offen; eine erforderliche Neuinstallation löscht lokale Daten.


## Abnahme 0.6.0 — Gesamtprüfung

54 JVM-Tests, Lint, APK-Berechtigungsprüfung und zwei Android-15-Instrumentierungstests erfolgreich am 30.09.2026. [Build](https://github.com/sirmebro-sketch/WerkLog/actions/runs/36733266485), [Emulator](https://github.com/sirmebro-sketch/WerkLog/actions/runs/36733266511).

Neue belegte Prüffälle:
- Metadaten-Dateischreiben gezielt blockiert: bestehende Anlage unverändert, Editor und eingegebener neuer Name erhalten; nach Behebung erneut erfolgreich gespeichert.
- Kollegenarchiv mit echtem JPEG-Titelbild durch den produktiven WorkModel importiert: lokaler `img:`-Bezug vorhanden, entschlüsselte Bildbytes identisch.
- Neue verschlüsselte Bilddateien bei gescheitertem Metadaten-Commit entfernt, alter Tresor weiter lesbar.
- Fachtests für Fälligkeiten, Lieferstatus, Hierarchiezyklen, fehlende Verknüpfungen, lange QR-Kennzeichen und Terminserien ab 1900.
- Bestehende 500-Bilder-Sicherungsprüfung, Passwortwechsel, Formularwiederaufnahme, Kamerarückgabe, Bestellungen und Kontakte weiter erfolgreich.
- Betrieb, Führung und Adressbuch visuell anhand aktueller Emulatoraufnahmen geprüft.

Zusätzliche Gerätechecks:
- [ ] Fälligkeit bei Tätigkeit setzen; Heute und „Fällig“-Filter prüfen; Erledigt entfernt sie aus der fälligen Übersicht.
- [ ] Lieferung mit Status Bestellt/Teilgeliefert und Datum prüfen; Geliefert/Abgesagt verschwindet aus Heute.
- [ ] Über 40 Anlagen/Anleitungen/Bestellungen: Suchtreffer außerhalb des ersten Abschnitts finden, weitere anzeigen, danach sperren/entsperren.
- [ ] Kontakt mit Telefonnummer/E-Mail bewusst öffnen; keine Kommunikation ohne eigene Aktion in der externen App.
- [ ] Kollegen-Titelbild importieren, in Vollbild öffnen, sperren/entsperren und Vollsicherung wiederherstellen.
- [ ] Nach Restore Biometrie neu aktivieren; falsches Backup-Passwort verändert die bestehende Anlage nicht.

APK `WerkLog-0.6.0-Test.apk`, SHA-256 `800cbd04c8fe99f47ddaa9c6d9af2ac2f3d358e8db39679b6b57c187dc9ecf9f`. App-Quellstand `40fd5ee2370e82ae02c35e6eb7d827671330d1db`. Vor einer möglichen Neuinstallation offene Formulare speichern und Vollsicherung exportieren. Geräteabhängige Kamera/OCR/Biometrie/Mail sowie stabile Release-Signatur bleiben offen.


## Abnahme 0.6.1 — Design und Navigation

Am 30.09.2026 erfolgreich am App-Quellstand `c82aa315b164816439663b2b8c597b9ad0229d5d`: [56 JVM-Tests, Lint, APK-Build und Netzwerkberechtigungsprüfung](https://github.com/sirmebro-sketch/WerkLog/actions/runs/36741464594) sowie [alle drei Android-15-Instrumentierungstests](https://github.com/sirmebro-sketch/WerkLog/actions/runs/36741464566). Alle neun gerenderten Kacheln bei 320 dp/100 % und 180 % Schrift haben gleiche Maße, vollständig dargestellte Texte und funktionieren beim Aufruf. Kontakt → Anlage → Zurück, Störung → Anleitung → Sperren/Entsperren → Zurück mit erhaltenem Text und direkte Folgeterminbearbeitung wurden geprüft. Aktuelle Aufnahmen von Betrieb, Führung, Kontakten und normaler/großer Schrift wurden visuell kontrolliert.

APK `WerkLog-0.6.1-Test.apk`, SHA-256 `2bf0cbc9c45dcd3c21f8ee8de26528c24f68d52edba973f2b7d0664cc57dc750`. Details zu Gestaltung, Rückwegen und Prüfgrenzen: [UI-REVIEW-0.6.1.md](UI-REVIEW-0.6.1.md). Vor einer möglichen Neuinstallation Vollsicherung erstellen; Deinstallation löscht App-Daten.

Zusätzliche Gerätechecks:

- [ ] Betrieb bei Standardschrift: neun gleich große Kacheln, letzte Kachel im regulären Rasterplatz, gleichmäßig ausgerichtete Titel/Beschreibungen; Blau-/Mint-Auswahl in der Hauptnavigation.
- [ ] Systemschrift 180 % und größere Anzeige: Raster ggf. einspaltig, alle Bezeichnungen vollständig, letzte Kachel durch Scrollen erreichbar. Querformat sowie maximale Samsung-Schrift-/Anzeigegröße gesondert prüfen.
- [ ] Mit einer Hand: ganze Kachel antippbar, feste Hauptnavigation und Zurück/Sperren erreichbar; keine versehentlichen Aktionen beim Scrollen.
- [ ] TalkBack: jede Kachel als Schaltfläche mit verständlichem Titel/Hinweis, sinnvolle Fokusreihenfolge, Navigation und Dialoge erreichbar.
- [ ] Kontakt → Anlage → Zurück: gleicher Kontakt wieder offen. Anlage → Anleitung → Zurück: gleiche Anlage und vorherige Scrollposition.
- [ ] Geöffnete Störung mit geändertem Text → zugeordnete Anleitung → App sperren/entsperren → Zurück: gespeicherter Text und geöffnete Störung wieder vorhanden.
- [ ] Folgetermin aus Störung: Bearbeitung des richtigen neu erzeugten Termins geöffnet; Termin speichern/abbrechen und zur Störung zurückkehren.
- [ ] Tastatur: Entsperren mit „Fertig“, falsches Passwort bleibt abgewiesen; beim Einrichten „Weiter“ und „Fertig“ mit Passwortwiederholung.
- [ ] Navigation Heute/Betrieb/Einstellung setzt den jeweiligen Hauptbereich auf seine Startseite; normale Android-Zurück-Geste und Kopf-Pfeil führen gleich zurück.
