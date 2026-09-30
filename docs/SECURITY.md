# Lokaler Schutz — 0.6.0

## Daten und Schlüssel

App-private, generationenbasierte Ablage: verschlüsselte Metadaten und einzelne verschlüsselte Bilddateien. AES-256-GCM, zufällige 96-Bit-Nonce pro Schreibvorgang, 128-Bit-Tag. PBKDF2-HMAC-SHA256 mit 310000 Iterationen und zufälligem 128-Bit-Salt. Details/AAD und Größenlimits: FORMAT.md.

Das Passwort wird nicht gespeichert. Der abgeleitete Tresorschlüssel liegt in der entsperrten Sitzung im RAM und wird beim Sperren überschrieben. Bei optionaler Biometrie wird eine verschlüsselte Kopie dieses Schlüssels gespeichert; der umhüllende Android-Keystore-Schlüssel verlangt starke biometrische Authentifizierung bei jeder Verwendung (CryptoObject). Kein bloßes Ja/Nein-Fingerabdruck-Gate. Neue biometrische Einschreibung invalidiert den Keystore-Schlüssel; das Tresorpasswort bleibt erforderlich/nutzbar. Nach Passwortwechsel Biometrie neu aktivieren.

Metadaten höchstens 32 MiB. Bilder maximal 500 × 512 KiB, maximal 1920 Pixel nach Verarbeitung. Neue Bilder zuerst verschlüsselt schreiben, Metadaten anschließend atomar ersetzen. Verwaiste Bilder erst danach löschen. Passwortwechsel und Wiederherstellung bauen eine neue Generation auf und schalten den aktiven Zeiger erst nach vollständiger Prüfung um. Alte Sicherungen behalten das alte Passwort. Ein starker, einzigartiger Passwortsatz schützt gegen Offline-Versuche auf kopierten Sicherungen; keine Wiederherstellung eines verlorenen Passworts.

## Sitzung und App-Wechsel

Normalerweise Sperre in `onStop`. Während bewusster Dateiauswahl, Fotoaufnahme und Kollegenaustausch darf ein Hintergrundwechsel höchstens zwei Minuten dauern. Eine verzögerte Sperre und eine Fristprüfung bei Rückkehr decken auch verzögerte Timer ab. Datei-/Code-Eingabe bleibt in dieser kurzen Phase bedienbar. Bildschirm-aus-Ereignis, gesperrtes Gerät oder manueller Schlossknopf sperren sofort. Kein dauerhaftes Entsperren, keine Speicherung sensibler Formularzustände in Android-SavedState. Ein Prozessneustart startet gesperrt. Nach Einrichtung/ohne vorherigen Entwurf startet Heute; nach einer Sperre wird der verschlüsselte Arbeitsstand wiederaufgenommen.

FLAG_SECURE schützt Screenshots/Übersicht auf unterstützten Geräten. Während der Nutzung existieren entschlüsselte Daten im RAM; JVM-Strings lassen sich nicht zuverlässig vollständig überschreiben. Root, kompromittiertes Betriebssystem/App-Code/Tastatur liegen außerhalb des Schutzmodells. Keine Sicherheitszertifizierung behauptet.

## Netzwerk und externe Apps

Keine INTERNET-/ACCESS_NETWORK_STATE-Berechtigungen; Manifest und verpackte APK werden geprüft. Das gebündelte ML-Kit-OCR-Modell läuft offline. Kein dynamischer Modelldownload, keine Cloud-OCR. Android-Cloudbackup und automatischer Gerätetransfer ausgeschlossen. Die App verwaltet kein Konto und sendet selbst keine E-Mails.

Android-Dateiauswahl, Galerie, Kamera, Mail-App und Systemkalender sind externe Anwendungen. Ein dort gewähltes Cloud-Ziel/Kalenderkonto kann Daten übertragen; WerkLog selbst synchronisiert nicht. Kalenderübergabe erfolgt nur nach bewusstem Tippen. App-Sperre sperrt keine bereits an externe Apps übergebenen Inhalte.

## Bilder und E-Mail

Zählerfotos bleiben nur zur vorübergehenden Bestätigung im privaten Kamera-Cache; bei Abschluss/Abbruch werden sie gelöscht. Weder Foto noch vollständiger OCR-Text kommen ins Archiv. Werte müssen bestätigt werden. Kamera erzeugt vorübergehend eine Datei im engen Kamera-Cacheordner. Galerieimport kopiert maximal 64 MiB; das Original bleibt unverändert. Abschluss/Abbruch löscht Arbeitskopien. Bei Activity-Neuerstellung bleiben gültige ausstehende Aufnahmen bis 15 Minuten wiederaufnehmbar; andere Kamera-Dateien werden bereinigt. Ohne wiederherstellbaren Instanzzustand kann eine neue Aufnahme nötig sein. Verhalten der externen Kamera ist gesondert auf dem Gerät zu testen.

Gespeicherte Anleitungs-/Bestellbilder werden neu als JPEG komprimiert (EXIF entfällt), separat verschlüsselt und erst bei „Bild anzeigen“ geladen. Bestell-Mailentwürfe enthalten nach Vorschau unverschlüsselten Text und bewusst beigefügte Bilder. Keine Anlagen-IDs oder Zugangsdaten automatisch im Mailtext. Ein nicht exportierter ContentProvider gewährt URI-Leserechte für kurzlebige, zusätzlich verschlüsselte Anhangsdateien (bis 15 Minuten). Nur deren zufällige Schlüssel liegen im RAM, maximal vier Bilder werden gleichzeitig entschlüsselt. Keine Klartext-Mailbilder im Dateicache. Prozessende/Fristablauf kann erneute Übergabe erfordern; verwaiste verschlüsselte Dateien werden beim nächsten Start gelöscht. Die Mail-App kann übergebene Inhalte dauerhaft speichern.

## Sicherung und Kollegenaustausch

ZIP-Dateien enthalten ausschließlich bereits verschlüsselte Metadaten/Bilder; Anzahl und Dateigrößen bleiben sichtbar. Stream-Verarbeitung, max. 384 MiB Container, enge Namen-/Anzahl-/Größenlimits, keine Verzeichnisse/Pfade. Alle benötigten Bild-Tags werden vor Aktivierung des Imports geprüft. Alte einzelne WRKLOG01-Backups bleiben lesbar. Aktuelle Schema-7-Archive benötigen 0.6.0+.

WRKSHR02-Anlagenfreigaben verwenden einen eigenen zufälligen 20-Zeichen-Code (rund 99 Bit), eigenen Salt und eigene Bildverschlüsselung. Kein App-Passwort in der Freigabe. Code nicht in der Freigabedatei, im Dateinamen, in der Begleitnachricht, in Logs oder Einstellungen. Während eines offenen Austauschdialogs kann der Code im separat verschlüsselten lokalen Formularentwurf zur Wiederaufnahme enthalten sein. Datei und Code getrennt übergeben. Jede Person mit Datei und Code kann mit kompatibler Software entschlüsseln; keine App-exklusive Garantie, kein Widerruf exportierter Dateien, kein Beweis der Absenderidentität. WRKSHR01 lesbar.

Standardumfang Stammdaten, Wissen und Anleitungen; Passwörter/Historie nur ausdrücklich. Einzelner Wissenseintrag enthält nur Basis-Anlagenzuordnung. Empfänger sieht Inhalt vor Import, Passwörter maskiert. Neue lokale Anlagenkopie oder ausgewählte bestehende Anlage; vorhandene Inhalte standardmäßig erhalten, explizite Ersetzungswahl nötig. Herkunfts-IDs verhindern Wiederholungsduplikate. Keine automatische Löschung entfernter Senderdaten.

FileProvider gibt ausschließlich eng begrenzte Freigabe-/Kameraordner frei. Verschlüsselte Freigaben älter als 24 Stunden werden beim App-Start gelöscht. QR-Code-Bilder im Freigabeordner enthalten nur bewusst geteilte Anlagen-ID/Kennzeichen. Kopien externer Apps bleiben unberührt.

## Erinnerungen

Außerhalb des Tresors liegen nur geplante Auslösezeitpunkte; Benachrichtigungen enthalten keine Titel/Firmen/Anlagen. Berechtigung ab Android 13 erforderlich. Inexact Alarms können sich verzögern, sind keine sicherheitskritische Alarmierung. Nach Geräteneustart werden gespeicherte Zeitpunkte neu geplant; nach Öffnen werden Serientermine für das nächste Jahr aktualisiert. Systemkalenderkopien werden nicht automatisch nachgeführt.

## Vor produktiver Nutzung

Dauerhafte Release-Signatur (RELEASE-SIGNING.md), erfolgreiche aktuelle CI und S24-Geräteprüfung. Kamera-/OCR-Qualität, Biometrie, Mail-App und Energiesparverhalten brauchen reale Abnahme. Keine echten Betriebsdaten im öffentlichen Repository. Interne Freigaben und offizielle Melde-/Prüfwege bleiben maßgeblich; die App dokumentiert, steuert keine Anlagen und setzt keine betrieblichen Grenzwerte voraus.


### Kamera und Bildansicht ab 0.4.2

Ein Android-Konfigurationswechsel (z. B. Drehung) erhält die laufende Tresorsitzung. Ein echter Prozessneustart stellt keinen Schlüssel wieder her. Ausstehendes Bildziel und temporärer Dateiname werden in Androids Instanzzustand gesichert, ohne Passwort/Schlüssel. Kamera-Rohbilder liegen bis zur Bestätigung/Abbruch vorübergehend im privaten Cache; bei Wiederaufnahme nach Neuerstellung werden nur Dateien unter 15 Minuten akzeptiert, andere bei Appstart bereinigt. Bei Abschluss/Abbruch/Beenden werden sie entfernt. Meterfotos bleiben kein dauerhaftes Archiv. Die Vollbildansicht erzwingt ebenfalls FLAG_SECURE. Bildschirm aus/manuelles Sperren bleiben sofort wirksam; die bestehende Zwei-Minuten-Frist bei bewusster externer Übergabe bleibt bestehen.


## Prüfung 0.6.0

Masterpasswort-Felder werden ausschließlich im Arbeitsspeicher gehalten und nicht als Formularentwurf gespeichert. Geschäftliche Formularfelder bleiben dagegen verschlüsselt wiederaufnehmbar. Nach Wiederherstellung wird die alte biometrische Umhüllung gelöscht; nach Passwortwechsel/Wiederherstellung muss Biometrie neu aktiviert werden. Fehler beim Entfernen eines Keystore-Eintrags machen einen bereits erfolgreich umgeschlüsselten Tresor nicht wieder rückgängig.

Formulare schließen erst nach erfolgreicher Speicherung. Neue Bilddateien werden bei fehlgeschlagener Speicherung entfernt, Importreste nach einem Fehler gegen den tatsächlich gespeicherten Bildbestand bereinigt. Bildvorschauen prüfen Dateigröße und Auflösung vor dem Dekodieren. Der Emulator prüft einen echten provozierten Dateisystemfehler und die lokale Übernahme eines importierten Anlagenbildes; das ist keine unabhängige Sicherheitszertifizierung. Weitere Prüfergebnisse: AUDIT-0.6.0.md.
