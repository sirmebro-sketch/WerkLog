# Apple-Port: geprüfter Dateiformat-Kern, noch keine iOS-App

`WerkLogCore` ist ein Swift-Package für iOS 16+/macOS 13+. Es liest/schreibt die gleichen AES-GCM-Metadaten wie Android und prüft Bildverschlüsselung sowie Unicode-Passwörter mit einem gemeinsamen Testvektor. Keine Fremdbibliothek oder Internetfunktion.

```sh
cd apple/WerkLogCore
swift test
```

Dieser Kern ist **keine installierbare App**. Offen: SwiftUI-Oberfläche, vollständige Datenmodelle/Validierung, sichere ZIP-Container-Verarbeitung mit denselben Grenzen, atomare Medienablage, Keychain/Face ID, Kamera/OCR, Benachrichtigungen, E-Mail-Übergabe, Migrationstests und Gerätesignierung. Für die Veröffentlichung müssen Apple-Team, Bereitstellung und Testgeräte eingerichtet werden; ein Test der Codec-Bibliothek belegt keine fertige iOS-Version.

Der Android-Ausbau wird nicht als Apple-Port ausgegeben. Der gemeinsame Kern verhindert, dass später ein inkompatibles Austauschformat neu erfunden werden muss.
