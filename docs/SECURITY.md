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

## Anlagenzugänge ab 0.2.0

Zugangseinträge werden zusammen mit allen Daten per AES-GCM verschlüsselt, nie als separate Klartextdatei. Mehrere Zugänge werden über die Anlagen-ID zugeordnet. Anzeige standardmäßig maskiert, gezieltes Anzeigen maximal 20 Sekunden. Beim Verlassen der App greift weiterhin die Sitzungssperre. Es gibt bewusst keine Zwischenablagefunktion. Ein Passwortvorschlag ist ein noch nicht gespeicherter Formularwert; seine Erzeugung setzt das reale Anlagenpasswort nicht zurück. Die erzeugten Zeichen müssen mit dem jeweiligen Anlagen-System kompatibel sein.

Die E-Mail-Funktion erhält nur Vorgänge und Anlagennamen. Zugangseinträge, Wissenseinträge und Servicekontakte werden nicht automatisch übernommen. Geheimnisse deshalb nicht in allgemeine Vorgangsnotizen kopieren. Diese Notizen werden bei Übergaben bewusst geteilt.

Schema 2 liest alte Schema-1-Sicherungen. Version 0.1.0 kann neue Sicherungen nicht lesen. Vor einer Neuinstallation verschlüsselt sichern; die bisherigen CI-Debug-Signaturen sind nicht dauerhaft identisch. Neuinstallation löscht den lokalen Tresor, Wiederherstellung braucht Sicherung und deren Passwort.

## Verschlüsselter Kollegenaustausch

`.werkshare` enthält ausschließlich einen getrennten WRKSHR01-Header, zufälligen Salt und IV sowie AES-256-GCM-verschlüsselten Inhalt. PBKDF2-HMAC-SHA256 (310.000 Iterationen) leitet den Dateischlüssel aus einem zufälligen 20-Zeichen-Code ab (rund 99 Bit Entropie). Pro Freigabe neuer Code, Salt und IV. Header authentifiziert, manipulierte Inhalte werden abgewiesen. Vollsicherungen WRKLOG01 und Austauschdateien sind getrennte Formate.

Das Gerätepasswort wird nie weitergegeben. Der Code wird nur während der Erstellung angezeigt, nicht in Datei, Dateiname, Nachricht, Logs oder dauerhaften Einstellungen gespeichert. Datei und Code getrennt übergeben. Keine App-exklusive Entschlüsselungsgarantie: Jeder mit Datei, Code und kompatibler Software kann entschlüsseln. Das ist beabsichtigt und sicherer als geheime Dateiformate. Kein Widerruf und keine Verfallszeit für exportierte Dateien. Der Code ist kein Nachweis einer bestimmten Absenderidentität.

Standardmäßig werden Stammdaten und Wissen geteilt. Zugangsdaten und Vorgangshistorie müssen ausdrücklich eingeschlossen werden. Beim Teilen eines einzelnen Wissenseintrags werden nur dieser und Anlagenname/Gewerk als Zuordnung exportiert. Die Vorschau des Empfängers maskiert Passwörter; nach bestätigtem Import sind sie im eigenen verschlüsselten Tresor. Import erzeugt eine neue Anlagenkopie mit neuen IDs, niemals automatisches Überschreiben.

Android FileProvider gewährt nur Leserechte für einen Unterordner mit bereits verschlüsselten Austauschdateien. Temporäre Freigaben älter als 24 Stunden werden beim nächsten App-Start entfernt. Kopien in Mail- oder Datei-Apps bleiben davon unberührt. Keine INTERNET-Berechtigung. Ausgewählte Dateianbieter oder die gewählte Versand-App können Daten entsprechend ihrer eigenen Funktionen übertragen.
