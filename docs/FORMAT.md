# Offline-Formate, Version 0.4

Alle Integer-Felder im JSON; Text UTF-8. Datenschema 5. Decoder akzeptiert Schema 1–5. Schema 5 ergänzt das optionale lokale Profil (Name, Funktion, Team, Betrieb, Diensttelefon, E-Mail, JPEG-Profilfoto als Base64 bis 96 KiB). Das Profilfoto bleibt innerhalb der verschlüsselten Metadaten und belegt keinen der 500 Plätze für Arbeitsbilder. Ältere Dateien erhalten ein leeres Profil. Anlagenpakete enthalten stets ein leeres Profil; Importe bewahren das Profil des Empfängers. Die kryptografischen Testvektoren unter app/src/test/resources/interop.json und apple/WerkLogCore/Tests sind synthetisch und identisch.

## Metadaten (`data.vault`)

`WRKLOG01` (8 ASCII-Bytes), Salt (16 Bytes), Nonce (12 Bytes), Ciphertext, GCM-Tag (16 Bytes). PBKDF2-HMAC-SHA256, 310000 Iterationen, UTF-8-Passwort, 256-Bit-Schlüssel. AES-256-GCM, AAD = `WRKLOG01`. Höchstens 32 MiB einschließlich Hülle. Keine Unicode-Normalisierung des Passworts. Jedes Schreiben neue zufällige Nonce.

## Bilder

JSON enthält `img:<UUID>`. Dateiname `<UUID>.image`. Inhalt: Nonce (12), Ciphertext, Tag (16). Derselbe Tresorschlüssel, AAD = UTF-8 `WRKIMG01:img:<UUID>`; Vertauschen von Bilddateien schlägt dadurch fehl. Neue UUID bei Bildersetzung. JPEG maximal 512 KiB. Keine Klartextkopie dauerhaft gespeichert.

## Vollsicherung

ZIP mit `data.vault` und den referenzierten `<UUID>.image`-Dateien. Alle Inhalte bereits einzeln authentifiziert verschlüsselt; ZIP-Kompression ist keine Verschlüsselung. Anzahl/Größen der Dateien sind sichtbar. Import begrenzt Eintragsnamen, Anzahl, jedes Bild, Metadaten und Gesamtmenge (384 MiB); keine Pfade oder Verzeichnisse akzeptiert. Bilder werden vor Aktivierung entschlüsselt/geprüft. Alte einzelne WRKLOG01-Dateien bleiben importierbar.

## Kollegenaustausch

Neue Datei: 8 ASCII-Bytes `WRKSHR02`, danach ZIP wie oben, mit einem eigens abgeleiteten Schlüssel aus dem zufälligen Freigabecode. Code: 20 Zeichen aus `ABCDEFGHJKLMNPQRSTUVWXYZ23456789`, visuell gruppiert. Zur Ableitung Leerraum/Bindestriche entfernen und Großschreibung verwenden. Nicht das App-Passwort. Sender exportiert bewusst ausgewählte Datensätze; Empfänger prüft den erlaubten Paketumfang und legt neue lokale Bildreferenzen an. WRKSHR01 bleibt lesbar.

## Atomare Speicherung

Unveränderliche verschlüsselte Bilder zuerst schreiben, Metadaten zuletzt atomar ersetzen. Verwaiste Bilder nach erfolgreichem Commit entfernen. Restore/Passwortwechsel in separatem `store-<UUID>` aufbauen, sämtliche Inhalte prüfen und erst danach den `active`-Zeiger atomar ersetzen. Fehler vor diesem Punkt verändern den aktiven Tresor nicht. Für einen vollen Wechsel/Export genügend freien Gerätespeicher für eine weitere Kopie vorhalten.
