# Abnahme auf Samsung Galaxy S24 Ultra

Status: CI-Build am 29.09.2026 erfolgreich (Version 0.3.0, Run 36580771959). Gerätetests weiterhin offen.

- [x] Workflow: testDebugUnitTest (29 Tests), lintDebug, assembleDebug und APK-Netzwerkberechtigungsprüfung erfolgreich.
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
- [ ] Dateiauswahl, erneutes Entsperren, Codeeingabe und Vorschau vollständig durchlaufen.
- [ ] Abbruch sowie App-Wechsel während Entschlüsseln zeigen keine Vorschau nach erneutem Entsperren.
- [ ] Import mit gleichem Anlagennamen erzeugt neue Kopie; vorhandene Daten unverändert.
- [ ] Große Schrift / kleine Displays: Vorschau und Code vollständig lesbar.

## Zusätzliche Geräteabnahme 0.3.0

- [ ] Kaltstart im Flugmodus: OCR ohne vorherigen Modell-Download verfügbar.
- [ ] Zwei Zähler an verschiedenen Anlagen: Foto, korrekte Zuordnung, führende Nullen/Nachkomma prüfen; absichtliche manuelle Bestätigung.
- [ ] Unlesbares Foto → manueller Wert; keine automatische Speicherung.
- [ ] Kameraabbruch, Prozessende, Rotation und Rückkehr: temporäre Bilder gelöscht, kein Absturz, ggf. verständliche Wiederholung.
- [ ] Anleitungsfoto im Hoch-/Querformat, große Aufnahme: Orientierung korrekt, Bild klein und nach Neustart vorhanden.
- [ ] Schritt ergänzen, bearbeiten, nach oben verschieben, mit/ohne Bild; allgemeine und anlagenbezogene Anleitung.
- [ ] 30-Bilder-/8-MiB-Limit: Speichern eines übergroßen Imports erhält den alten gültigen Stand.
- [ ] Kalender Monatswechsel, heutiger Tag, mehrere Termine, Fremdfirma, Kontakt, Zuständiger, Abgesagt/Erledigt.
- [ ] Bestellliste mit mehreren Positionen, Bildern, lokaler Anlagenzuordnung; E-Mail-Vorschau und tatsächliche Anhänge in verwendeter Mail-App prüfen.
- [ ] Mail-Entwurf nicht abgesendet: in WerkLog niemals als automatisch bestellt/gesendet markiert.
- [ ] Bestellanhang nach 15 Minuten/Prozessende ggf. erneut erstellen; keine Klartextbilder im lokalen Dateiverzeichnis.
- [ ] Schema-1-/Schema-2-Backups importieren; Schema-3-Backup mit allen neuen Modulen wiederherstellen.
- [ ] Anlagenfreigabe mit bebilderter Anleitung/Zählermessungen auf zweiter 0.3.0-Installation importieren.
