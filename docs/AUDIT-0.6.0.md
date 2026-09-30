# WerkLog 0.6.0 — Gesamtprüfung und Verbesserungen

Stand: 30.09.2026. Alle Android-Quellmodule, Datenmodelle, UI-Speicherpfade, Verknüpfungen und Schutz-/Übergabepunkte wurden geprüft. Ergänzt wurden konkrete Alltagsfunktionen und Fehlerkorrekturen; neue Prüffälle verwenden ausschließlich synthetische Daten.

## Behobene Fehler

| Bereich | Befund | Änderung |
| --- | --- | --- |
| Formulare und Löschen | Mehrere Dialoge schlossen vor Abschluss des Schreibvorgangs. | Schließen erst nach erfolgreichem Commit; bei Fehler bleiben Formular und Eingaben erhalten. Während des Schreibens sind Felder und Hauptnavigation gesperrt. |
| Neuer Kollegenimport | Das Anlagenbild konnte eine Referenz in der temporären Bildablage des Senders/Imports behalten. | Sämtliche Bildbezüge einschließlich Titelbild werden auf lokale verschlüsselte Dateien umgestellt. |
| Importfehler | Die Originaldatei und Vorschau wurden zu früh geschlossen. | Erneutes Entschlüsseln/Importieren bleibt möglich; neu erzeugte, unreferenzierte Bilder werden bereinigt. Neue Dateiauswahl startet einen eigenen Dialogzustand. |
| Bildspeicherung | Vor einem Metadatenfehler geschriebene Bilder konnten liegen bleiben. | Vollständige Vorvalidierung und Entfernen neu erzeugter Dateien bei einem Fehler. Aufräumen nach erfolgreichem Commit ist nachgelagert. |
| Passwort und Restore | Alte biometrische Bindung bzw. Keystore-Aufräumfehler konnten den neuen Schlüsselstand überlagern. | Alte Bindung löschen; ein Aufräumfehler verwirft keinen erfolgreich neu verschlüsselten Tresor. |
| Eingabe-Wiederholung | Sperren während des Speicherns konnte beim erneuten Bestätigen neue IDs erzeugen. | IDs für neue Formulare, Messwerte, Schritte, Positionen und Rundgänge bleiben während des Entwurfs stabil; erneutes Speichern aktualisiert denselben Datensatz. |
| Hierarchie und Integrität | Unpassende Elternauswahl, doppelte IDs und fehlende Bezüge waren teilweise erst spät erkennbar. | Eigene Unteranlagen aus Elternauswahl ausschließen; kreisfreie Hierarchie effizient prüfen; IDs, Daten und Anleitungs-/Vorgangsbezüge vor Speichern validieren. |
| QR und Bilder | Sehr lange Kennzeichen konnten die QR-Kapazität überschreiten; Vorschauen brauchten engere Grenzen. | Fallback auf Anlagen-ID; Dateigröße und Bildabmessungen vor Vorschau prüfen. QR-Rohbild bis Abschluss für Wiederaufnahme erhalten. |
| Wiederholte Termine | Sehr alte Startdaten konnten wegen der Iterationsgrenze aktuelle Termine ausblenden. | Berechnung beginnt nahe dem angefragten Datum; Monatsserien bleiben am ursprünglichen Tag verankert. |
| Textvorlagen | Das Gewerk ging beim Bearbeiten verloren. | Gewerk auswählen und beim Bearbeiten beibehalten. |

## Ergänzungen für den Arbeitsalltag

- **Fälligkeit bei Störungen/Arbeiten:** optionales Datum, gültige Datumseingabe, überfällige Kennzeichnung, Übersicht auf Heute und Filter „Fällig“ im Arbeitsprotokoll.
- **Lieferungen im Blick:** angefragte/bestellte/teilgelieferte Listen mit erwartetem Datum auf Heute; erledigte oder abgesagte Bestellungen erscheinen dort nicht.
- **Bessere Suche:** Anleitungen über Titel, Anlage und Schrittexte; Bestelllisten über Titel, Hintergrund, Artikel und Anlage. Offene oder erledigte Bestellungen filtern.
- **Große Übersichten:** Anlagen, Arbeitsprotokoll, Anleitungen, Bestellungen, Kontakte, Kalender und Rundgänge zunächst mit 40 Datensätzen, danach „Weitere anzeigen“. Die Suche läuft weiterhin über den vollständigen Bestand.
- **Adressbuch im Einsatz:** direkt Nummer in der Telefon-App wählen oder eine E-Mail vorbereiten. Die App tätigt keine Anrufe und versendet keine Nachrichten automatisch.
- **Wiederaufnahme:** gesicherte Dateinamen von Import-/Sicherungsdialogen sowie offene Bildansicht wiederverwenden; gültige temporäre Aufnahmen bleiben nach Sperren bestätigbar.

## Prüfungen

Ergebnis 30.09.2026:
- **54 JVM-Tests, Lint und APK-Berechtigungsprüfung erfolgreich:** [Build 36733266485](https://github.com/sirmebro-sketch/WerkLog/actions/runs/36733266485).
- **Zwei Android-15-Instrumentierungstests erfolgreich:** [Emulator 36733266511](https://github.com/sirmebro-sketch/WerkLog/actions/runs/36733266511).
- Betriebskacheln, kompakte Führung und beidseitiges Adressbuch anhand der Emulatoraufnahmen visuell geprüft.
- APK ohne INTERNET- und ACCESS_NETWORK_STATE-Berechtigung.
- App-Quellstand: `40fd5ee2370e82ae02c35e6eb7d827671330d1db`.
- APK: `WerkLog-0.6.0-Test.apk` (61080092 Bytes), SHA-256 `800cbd04c8fe99f47ddaa9c6d9af2ac2f3d358e8db39679b6b57c187dc9ecf9f`.

Zusätzliche Prüffälle: provozierter Dateisystem-Schreibfehler mit erhaltenem Formulartext, Import eines echten JPEG-Anlagenbilds durch den produktiven ViewModel-Pfad, Bild-Rollback nach gescheitertem Commit, Fälligkeiten/Lieferstatus, Hierarchiezyklen, lange QR-Kennzeichen, alte Terminserien und ungültige Verknüpfungen. Der bestehende Test mit 500 separat verschlüsselten Bildern und einer Sicherung oberhalb 8 MiB bleibt enthalten.

## Grenzen und nächste sinnvolle Schritte

1. **Reale Geräteabnahme:** Kamera-Rückkehr, Zähler-OCR, Fingerabdruck, Zoom-Gesten, Mail-Anhänge, große Schrift und Akkusparen auf dem S24 Ultra prüfen. Emulatorprüfungen decken diesen Gerätepfad nicht vollständig ab.
2. **Feste Release-Signatur:** vor dauerhafter Nutzung private GitHub-Secrets nach RELEASE-SIGNING.md einrichten. Test-APK-Signaturen können wechseln; vor einer möglichen Neuinstallation Vollsicherung exportieren. Private Schlüssel gehören nicht ins öffentliche Repository.
3. **iOS:** bisher Swift-Dateiformat-Kern; vollständige SwiftUI-App und Plattformintegration fehlen.
4. **Gezielte Vertiefung:** einzelne Terminserien-Ausnahmen, feldweise Importkonflikte und bewusste PDF-Berichte wären sinnvoll. Sie brauchen eine eigene Umsetzung/Abnahme und werden nicht als vorhanden ausgegeben. Kein unkontrollierter Ausbau in Richtung ERP oder Buchhaltung.

Neue Exporte enthalten Schema 7 und brauchen beim Empfänger mindestens 0.6.0. Alte Sicherungen bleiben lesbar. Offene Formulare vor einem Versionswechsel speichern; Entwürfe gehören nicht zur Vollsicherung. Masterpasswort-Eingaben werden aus Schutzgründen nicht als Entwurf gespeichert. Keine unabhängige Sicherheitszertifizierung oder produktive Gerätefreigabe aus dieser Prüfung ableiten.
