# Lokaler Schutz

## Umsetzung

- App-private Datei `werklog.vault`; atomarer Austausch über Android AtomicFile.
- AES-256-GCM mit zufälliger 96-Bit-IV bei jedem Speichern und 128-Bit-Authentifizierungstag.
- PBKDF2-HMAC-SHA256 mit 310.000 Iterationen und zufälligem 128-Bit-Salt.
- Versionierter Header WRKLOG01 ist zusätzliche authentifizierte Information.
- Passwort wird nicht gespeichert. Abgeleiteter Schlüssel liegt nur während der entsperrten Sitzung im RAM; sein Bytearray wird beim Sperren überschrieben.
- Sperre in Activity.onStop; keine Speicherung sensibler Compose-Formularzustände in SavedState.
- FLAG_SECURE blockiert Screenshots und Vorschauen auf unterstützten Android-Geräten.
- Kein INTERNET-Permission, keine externen Laufzeitdienste oder Analytics.
- Android-Cloudbackup deaktiviert; Dateien für Cloudbackup und Gerätetransfer ausgeschlossen.
- Exportiert wird ausschließlich die verschlüsselte Datei. Wiederherstellung prüft Passwort, Authentifizierung und Datenstruktur, bevor sie bestehende Daten atomar ersetzt.
- Importgröße maximal 8 MiB. Ein fehlgeschlagener Import soll bestehende Daten unverändert lassen.

## Bewusste Grenzen

Ein lokales Passwort ist keine Wiederherstellungsfunktion. Verlust von Gerät ohne Backup oder Passwortverlust bedeutet Datenverlust. Ein starkes, einzigartiges Passwort verwenden; kopierte Sicherungen erlauben Offline-Passwortversuche. In-App-Fehlversuchslimits würden das nicht verhindern.

Während der Nutzung existieren entschlüsselte Daten im Arbeitsspeicher. Die JVM garantiert nicht, dass jede String-Kopie sofort gelöscht wird. Root, kompromittiertes Betriebssystem, manipulierter App-Code oder eine kompromittierte Tastatur liegen außerhalb des Schutzmodells. Keine Behauptung einer Sicherheitszertifizierung.

Der Android-Dateidialog kann lokale und Cloud-Ziele anzeigen. Nur der Nutzer entscheidet, wohin eine Sicherung geschrieben wird. E-Mail-Übergaben enthalten ausdrücklich unverschlüsselten Text und werden vorher angezeigt. Die App versendet selbst nichts. E-Mail-Entwürfe verbleiben gegebenenfalls in der externen Mail-App.

Vor Nutzung realer betrieblicher Daten klären, ob das Endgerät und dieser Dokumentationsweg intern freigegeben sind. Das Repository darf ausschließlich Code und synthetische Testdaten enthalten. Die App ersetzt keine offiziellen Störungsmeldungen, Prüfprotokolle oder Betriebsanweisungen.

## Vor produktiver Nutzung

Erfolgreicher CI-Build, Sicherheits-/Lifecycle-Gerätetests, persistente Release-Signatur und interne Nutzungsfreigabe. Die implementierten Schutzmechanismen wurden hier nicht auf einem Android-Gerät verifiziert.
