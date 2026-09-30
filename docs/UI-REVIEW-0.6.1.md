# Design, Bedienung und Navigation — 0.6.1

Stand 30.09.2026. Die unterschiedlich hohen Betriebskacheln aus 0.6.0 waren ein sichtbarer Layoutfehler. Zusatzbeschreibungen nur bei Arbeitsprotokoll und Textvorlagen vergrößerten diese Karten. Die letzte Einzelkachel spannte außerdem über die gesamte Zeile. Dieser Befund wurde bei der vorherigen Sichtprüfung übersehen.

## Änderungen

- Alle neun Betriebskacheln haben dieselbe Höhe, Breite, Innenabstände und feste gemeinsame Bereiche für Symbol, Titel und Beschreibung. Auch die letzte Einzelkachel belegt nur einen regulären Rasterplatz.
- Die Höhe wird aus den tatsächlich gemessenen Texten und der Systemschrift berechnet. Texte werden nicht abgeschnitten oder künstlich verkleinert. Zwei Spalten auf gewöhnlichen Telefonen; bei schmalem Platz/großer Systemschrift eine Spalte; drei auf breiten Flächen.
- Einheitliche dunkelblaue Karten, dezente Kontur, mintfarbene Symbole, zentrierte Beschriftung. Jede Karte ist als ganze Fläche bedienbar und für die Vorlesefunktion als Schaltfläche mit Titel/Beschreibung markiert.
- Die Auswahl in der unteren Navigation verwendet dieselben Blau-/Mintfarben. Die zuvor sichtbare violette Standardmarkierung ist ersetzt; die aktive Beschriftung ist hervorgehoben.
- Reihenfolge nach Alltagsbezug: Anlagen/Arbeitsprotokoll, Zähler/Rundgang, Anleitungen/Bestellungen, Kalender/Adressbuch, Textvorlagen.
- Ein fester Zurück-Pfeil im Kopf ersetzt die wechselnden doppelten Zurück-Schaltflächen innerhalb der Seiten. Android-Zurück verwendet denselben Rückweg.
- In Anleitungen, Bestellungen und Kontakten geht Zurück vom Datensatz zur jeweiligen Liste, danach zu Betrieb und Heute. Bei Verknüpfungen geht es stattdessen zum tatsächlichen Ausgangsdatensatz, einschließlich zuvor geöffneter Störung und Scrollposition.
- Navigation über die drei Hauptpunkte öffnet bewusst deren Startseite. Die vorherige Kette wird dabei abgeschlossen. Der Rückweg zwischen verknüpften Datensätzen bleibt beim Sperren verschlüsselt wiederaufnehmbar.
- Folgetermine aus einer Störung öffnen sofort die Bearbeitung des neu erzeugten Termins. Zuvor wurde nur der Kalender aufgerufen, dessen ausgewählter Tag den Termin unter Umständen gar nicht zeigte.
- Entsperren funktioniert auch über „Fertig“ an der Bildschirmtastatur; beim Einrichten führt „Weiter“ zur Passwortwiederholung. Schaltfläche und Tastatur verwenden dieselbe Passwortprüfung.

## Bewertung

| Thema | Ergebnis und Grenze |
| --- | --- |
| Orientierung | Heute, Betrieb, Einstellung bleiben die drei klaren Hauptbereiche. Kacheln erklären nun alle Werkzeuge mit demselben kurzen Aufbau. |
| Gestaltung | Die uneinheitlichen Kachelgrößen sind behoben; Symbole, Flächen und Typografie verwenden gemeinsame Maße. |
| Lesbarkeit | Grundtext/Hinweistext/Akzent auf der Kartenfläche haben rechnerisch Kontraste von ca. 13,70 / 8,32 / 9,56 : 1. Dies ist eine Prüfung dieser Farbpaarungen, keine Zertifizierung der gesamten Oberfläche. |
| Erreichbarkeit | Die ganze Kachel ist klickbar, auch bei großer Schrift; Zurück und App-Sperre bleiben im Kopf erreichbar. Wichtige Vorgangslinks führen direkt zu ihrem Datensatz. |
| Logik | Listen, Detailansichten und bereichsübergreifende Verweise besitzen nachvollziehbare Rückwege. Ein aus einer Störung angelegter Termin ist sofort bearbeitbar. |
| Noch zu erproben | Physische Einhandbedienung, TalkBack, Querformat und maximale Samsung-Schrift-/Anzeigeeinstellungen benötigen weiterhin den S24-Gerätetest. |

## Validierung

Am 30.09.2026 erfolgreich am App-Quellstand `c82aa315b164816439663b2b8c597b9ad0229d5d`:

- [Android-Build](https://github.com/sirmebro-sketch/WerkLog/actions/runs/36741464594): 56 JVM-Tests, Lint, APK-Build und Prüfung ohne Internet-/Netzwerkstatus-Berechtigung bestanden.
- [Android-15-Emulator](https://github.com/sirmebro-sketch/WerkLog/actions/runs/36741464566): alle drei Instrumentierungstests bestanden.
- Alle neun Kacheln bei 320 dp Breite und 100 %/180 % Schriftgröße mit gleichen Abmessungen, vollständig dargestellten Titeln/Hinweisen und funktionierendem Aufruf geprüft.
- Bildschirmaufnahmen der produktiven Betrieb-Ansicht, Führung und Kontaktansicht sowie des Rasters mit normaler/großer Schrift visuell geprüft. Die Auswahlmarkierung verwendet Blau/Mint; die letzte Einzelkachel besitzt dieselben Maße wie die übrigen.

Der neue Layouttest prüft tatsächliche gerenderte Kachelgrößen bei 320 dp Breite und 100 % bzw. 180 % Schriftgröße, einschließlich der letzten Einzelkachel. Er prüft Titel und Beschreibungen auf sichtbare Textüberläufe und ruft jede Kachel auf. Der erweiterte Bedienablauf prüft Kontakt → Anlage → Zurück, Störung → Anleitung → Sperren/Entsperren → Zurück zur Störung sowie direkte Folgeterminbearbeitung. Bestehende Fach-, Speicher-, Import- und Kamera-Rückgabetests bleiben enthalten.

Ausgelieferte APK: `WerkLog-0.6.1-Test.apk`, 61.096.476 Bytes; SHA-256 `2bf0cbc9c45dcd3c21f8ee8de26528c24f68d52edba973f2b7d0664cc57dc750`. Der heruntergeladene Build-Container stimmt mit der SHA-256-Prüfsumme des GitHub-Artefakts überein. Die APK ist ein Debug-Build; vor einem gegebenenfalls erforderlichen Neuinstallieren immer Vollsicherung erstellen, weil Deinstallation App-Daten löscht.

Datenschema 7 und Offline-Schutz bleiben bestehen. Die neue Navigation liegt nur im verschlüsselten Formularentwurf. Vor einer App-Aktualisierung offene Formulare speichern und Vollsicherung erstellen; Entwürfe gehören nicht zum Sicherungsexport. Reale Geräteabnahme und stabile Release-Signatur bleiben erforderlich.
