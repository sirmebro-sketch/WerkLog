# Dauerhaft installierbare Updates

Der manuelle Workflow „Signierte Android-Version“ baut APK und AAB mit demselben privaten Schlüssel. Vor dem ersten Lauf müssen Repository-Secrets gesetzt werden. Diese Sitzung hat keinen Zugriff auf GitHub-Secrets-Verwaltung; die Einrichtung ist deshalb noch offen. Ein Debug-Build wird nicht als signiertes Release ausgegeben.

1. Auf einem vertrauenswürdigen Rechner mit JDK einen privaten Schlüssel erzeugen:

```sh
keytool -genkeypair -keystore werklog-release.jks -alias werklog -keyalg RSA -keysize 3072 -validity 10000
```

2. Schlüssel und Passwörter sicher außerhalb des öffentlichen Repositorys sichern. Bei PKCS12 üblicherweise dasselbe Store-/Key-Passwort verwenden.
3. Unter Repository → Settings → Secrets and variables → Actions anlegen:
   - `WERKLOG_KEYSTORE_B64`: Base64-Inhalt der Keystore-Datei (kein Dateipfad).
   - `WERKLOG_STORE_PASSWORD`: gewähltes Passwort.
   - `WERKLOG_KEY_PASSWORD`: Passwort des Schlüssels.
4. Workflow „Signierte Android-Version“ starten. APK/AAB aus dessen Artefakt installieren/verwahren.
5. Vor dem Wechsel von einer Debug-Version eine vollständige Sicherung exportieren und deren Passwort sicherstellen. Abweichende Signaturen erfordern Deinstallation. Spätere Releases mit diesem Schlüssel können direkt aktualisieren.

Keine Keystore-Datei, Passwörter oder Base64-Schlüssel in Commits, Issues oder Chatnachrichten einfügen. Das Geheimnis ist nicht der Quellcode, sondern der private Schlüssel. Bei Verlust kann die installierte App nicht mehr regulär aktualisiert werden.
