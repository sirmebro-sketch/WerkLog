# Stand 0.5.0 — Umsetzung und Grenzen

## Ergänzungen 0.4.2

- Kamera-Zwischenstand/Zielzuordnung bleiben bei Activity-Neuerstellung erhalten; Konfigurationswechsel sperren die laufende Sitzung nicht. Echter Prozessneustart benötigt weiterhin Entsperren.
- Große Bildansicht mit Zoom/Verschieben, Zurücksetzen und geschütztem Vollbildfenster.
- Anleitungs-Einstellungen unter den Schritten; „+ Nächster Schritt“ bleibt oben sichtbar.
- 44 JVM-Tests und erweiterter Emulatorablauf mit simulierter Kamera-Rückgabe erfolgreich. Physische Kamera und Zoom-Gesten auf S24 noch prüfen; siehe DEVICE-TEST.md.

## Ergänzungen 0.4.1

44 JVM-Tests, Android-Build und Emulator-Bedienablauf erfolgreich; Führung und Kraftwerk-Symbol anhand der Emulatoraufnahmen visuell geprüft. Details: [DEVICE-TEST.md](DEVICE-TEST.md).

- Kompakte Führung reserviert Platz oberhalb der Navigation, ohne die Seite abzudunkeln; Zurück/Weiter/Überspringen.
- Nach erstmaligem Erstellen des Passworts freiwilliges Fingerabdruck-Angebot vor der Führung; Android prüft die verfügbare starke Biometrie.
- Lokales Profil mit Foto und optionalen dienstlichen Angaben; bearbeiten, Foto entfernen, Profil löschen. Keine automatische Weitergabe.
- Eigene Kraftwerk-Vektorgrafik für den mittleren Betrieb-Knopf.

## Vorherige automatisierte Abnahme 0.4.0

Android-Build (40 JVM-Tests, Lint, APK-Netzwerkprüfung) und Android-15-Bedienablauf erfolgreich am 29.09.2026. Details und noch offene Gerätetests: [DEVICE-TEST.md](DEVICE-TEST.md).

- Drei Hauptpunkte: Heute, hervorgehobener Betrieb, Einstellung; Start auf Heute.
- Betrieb mit zwei Kacheln pro Zeile, Symbolen und kurzen Bezeichnungen.
- Vierstufige, überspringbare und erneut startbare Einführung mit Seitensprüngen.
- Heute mit deutschem Wochentag/Datum, offenen Vorgängen, Wartungsdaten und kommenden Terminen.
- Lokales Passwort ändern: neue Ableitung, neuer Salt, neue Verschlüsselung aller Bilder; atomarer Wechsel erst nach Erfolg. Bisherige Backups behalten ihr altes Passwort.
- Optionale starke Biometrie über Android Keystore und authentifizierte Cipher-Freigabe; Passwort bleibt für Backups erforderlich.
- Beim gewöhnlichen Verlassen sofort sperren. Dateiauswahl/Kollegenimport/-export erlauben pro Hintergrundwechsel höchstens zwei Minuten; Bildschirm aus/manuelle Sperre sofort. Keine Aufrechterhaltung über einen Prozessneustart.
- 32 MiB verschlüsselte Text-/Metadaten plus bis zu 500 separat verschlüsselte Bilder, je maximal 512 KiB/1920 Pixel. Kein Zählerfotoarchiv.
- Bildweises Backup/Restore/Passwortwechsel und neuer bildweiser Kollegenaustausch, Containerlimit 384 MiB. Alte Sicherungen und WRKSHR01-Dateien lesbar; neue Archive benötigen neue App-Version.
- Anlagenkennzeichen, Hierarchie, Favoriten, zuletzt geöffnete Anlagen, Suche über Stammdaten/Wissen/Anleitungen; QR-Code anzeigen/teilen und offline aus einem Foto lesen.
- Zählerdifferenzen, Verlaufskurve, manuell konfigurierte Differenz-Prüfgrenze, bestätigte Ausreißer und markierter Zählerwechsel.
- Wiederkehrende Termine und generische lokale Erinnerungen ohne Betriebsdaten auf dem Sperrbildschirm; bewusste Übergabe einer Kopie an den Systemkalender.
- Eigene Tätigkeitsvorlagen und anpassbare Beispiele; Tätigkeiten duplizieren.
- Anleitungsversion und Prüfdatum; Inhaltsänderungen entfernen den Prüfvermerk.
- Bestellstatus und Lieferdatum, Wiederverwendung früherer Positionen, Folgetermin/Teileanforderung aus einem Vorgang.
- Anlagen, Tätigkeiten, Messwerte, Zähler, Termine, Anleitungen, Bestellungen, Rundgangsvorlagen bearbeitbar; Löschbestätigungen für Datensätze. Abgeschlossene Rundgänge bleiben historische Protokolle (löschbar, Vorlagenänderung wirkt nicht rückwirkend).
- Import in neue oder bestehende Anlage; bekannte Herkunfts-IDs nicht doppeln. Bestehende Inhalte nur bei ausdrücklich gewähltem Ersetzen überschreiben. Keine automatische Lösch-Synchronisation.

## Konkrete externe Voraussetzungen / noch offen

1. **Dauerhafte Android-Signatur:** Release-Workflow und Konfiguration vorhanden. Private Repository-Secrets fehlen; die verfügbare GitHub-Verbindung kann keine Secrets anlegen. Einrichtung: RELEASE-SIGNING.md. Keine Schlüssel in öffentliche Commits.
2. **S24-Ultra-Praxistest:** Kamera, tatsächliche OCR-Qualität, Fingerabdruck, Mail-Anhänge, große Schrift, Akkusparen/Terminerinnerungen und Wechsel zum Code aus einer anderen App. Ein Emulator ersetzt diesen Test nicht.
3. **Apple:** kompatibler Swift-Dateiformat-Kern samt gemeinsamen Krypto-Testvektoren ist vorhanden. Es fehlt die eigentliche iOS-Oberfläche und Plattformintegration. Noch keine iOS-App/APK-Umwandlung oder App-Store-Verfügbarkeit; siehe apple/README.md.
4. **Feiner Importvergleich:** Vorschau zählt neue/geänderte/unveränderte Einträge inklusive Bildinhalt. Wahl „nur ergänzen“/„bekannte Inhalte ersetzen“ gilt für das Paket. Kein feldweiser Konfliktdialog mit individuellen Entscheidungen.
5. **Automatische Foto-Zählerzuordnung:** Eine Zahl wird lokal erkannt; der Zähler wird gewählt und bestätigt. Keine verlässliche automatische Identifizierung beliebiger Zählerschilder. Anlagen-QR-Codes funktionieren separat.
6. **Terminserien:** Bearbeitung/Löschung betrifft aktuell die ganze Serie. Einzelne Ausnahmen, Feiertagsregeln, „nur dieser Termin erledigt“ und echte bidirektionale Kalender-Synchronisation fehlen. Kalenderkopien sind bewusst externe Daten.
7. **Bildbedarf oberhalb 500:** Architektur speichert bereits getrennt. Grenzwerte erst nach Ressourcen-/Backup-Prüfung erhöhen; nicht unkontrolliert ausweiten.

Die offenen Funktionen sind kein Nachweis einer Gerätefreigabe. Keine echten Betriebsdaten im öffentlichen Repository. Alle Testdaten synthetisch.

## Ergänzungen 0.5.0

Umgesetzt: verschlüsselte Wiederaufnahme offener Formulare nach Sperren; automatische Biometrie-Anforderung; Sicherung nur unter Einstellungen; verständlichere Bezeichnungen Arbeitsprotokoll und Textvorlagen. Bestellungen aus Vorgängen enthalten separate editierbare Grundinformationen und beginnen ohne künstliche Teileposition. Anlagenbilder, kompakte Anlagenzeilen mit Gewerke-/Favoritenfilter, Gewerkeverwaltung und lokales Adressbuch. Kontakte sind mit Anlagen und Vorgängen in beide Richtungen verknüpft. Anlagen zeigen Anleitungen/Bestellungen, Vorgänge zeigen explizit zugeordnete und anlagenbezogene Anleitungen sowie Bestellungen.

Bewusste Grenzen: keine Buchhaltung, keine automatische Bestellung, keine automatische Weitergabe des Adressbuchs. Eine Anleitung kann einer Anlage und mehreren Vorgängen zugeordnet werden; allgemeine Anleitungen bleiben möglich. Keine Datenbankintegration in SAP. Anlage bleibt fachlicher Bezug, ein Vorgang beschreibt Arbeit/Störung und eine Bestellung tatsächlichen Materialbedarf. SAPs Objektbezüge dienten lediglich als fachliche Orientierung: https://help.sap.com/docs/SAP_S4HANA_ON-PREMISE/efc7922405fd4d56b7571930c5eaa798/f2c7b65334e6b54ce10000000a174cb4.html

Weiterhin offen: reale Geräteabnahme, stabile Release-Signierung, vollständige iOS-App. Keine biometrische Gerätefreigabe aus Emulatorprüfungen ableiten.
