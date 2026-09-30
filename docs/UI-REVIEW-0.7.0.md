# Farbkonzepte und Farbrollen — 0.7.0

Stand 30.09.2026. Die bisherige Petrol-/Mint-Gestaltung bleibt die Voreinstellung. Sie bekommt hellere Kartenflächen, deutlichere neutrale Konturen und eine vollständige Farbzuordnung für Material-Komponenten. Drei zusätzliche Konzepte verändern die ganze Oberfläche, einschließlich einer echten hellen Variante.

## Auswahl

**Einstellung → Darstellung**, mit kleinen Vorschauen und Auswahlmarkierung. Wechsel sofort, ohne Activity-Neustart oder Schließen eines Formulars; Seite, Scrollposition und Eingaben bleiben erhalten. Nur Konzept-IDs und die Systemoption werden außerhalb des Tresors lokal gespeichert. Keine Betriebsdaten, kein Konto und keine Netzverbindung. Die Auswahl wird nicht gesichert oder an Kollegen weitergegeben.

Optional **Geräteeinstellung folgen**: Tageslicht im hellen Gerätemodus, das zuletzt gewählte dunkle Konzept im dunklen Gerätemodus. Eine manuelle Auswahl deaktiviert die Automatik. Alte Installationen beginnen unverändert mit Petrol & Mint. Unbekannte gespeicherte IDs fallen darauf zurück.

| Konzept | Hintergrund | Karten | Akzent | Charakter |
| --- | --- | --- | --- | --- |
| Petrol & Mint | `#0C191E` | `#1B3038` | `#64DECB` | Dunkles Petrol, frisches Mint |
| Stahlblau | `#10182B` | `#202F4C` | `#9BC4FF` | Marineblau, kühles Eisblau |
| Graphit & Kupfer | `#211A17` | `#352B26` | `#F3BB92` | Warmer Graphit, weiches Kupfer |
| Tageslicht | `#E9EFF4` | `#FFFFFF` | `#006B67` | Helle Flächen, dunkle Schrift, kräftiges Petrol |

## Durchgängige Gestaltung

- Alle Material-Farbrollen liegen zentral, einschließlich Flächen für Menüs/Dialogs, Eingabefelder, aktive Auswahl, Fehler, Rückseiten und Container. Keine violetten Standardakzente zwischen den Seiten.
- Haupttext, ergänzende Angaben und Aktionen haben getrennte Rollen. Gewöhnliche Datenbeschriftungen und Kalenderzahlen sind neutral; Akzente kennzeichnen Aktionen, Auswahl, Symbole und Marke.
- Karten haben neutrale Konturen; Eingabefelder kräftigere Linien. Betrieb behält seine einheitlichen Maße bei allen vier Konzepten und großer Schrift.
- Kalenderauswahl hat einen eigenen Hintergrund und kontrastierenden Text, ohne die Warnfarbe für gewöhnliche Tage zu verwenden.
- Der Kraftwerk-Knopf verwendet die zum Akzent passende Vordergrundfarbe. Status- und Navigationsleisten wechseln ihre Symbolhelligkeit passend zur Variante.
- Sperrbildschirm, Führung, Formulare, Austausch, E-Mail-Vorschau und große geschützte Fotoansicht folgen der Auswahl. QR-Codes behalten für ihre Lesbarkeit Schwarz/Weiß.
- Ein Zählerwechsel wird im Verlauf als Quadrat statt nur durch Orange dargestellt. Normale Messpunkte bleiben Kreise; die Legende erklärt die Form.

## Fachliche Farbrollen

| Bedeutung | Farbe | Anwendung |
| --- | --- | --- |
| Offen / Entwurf / abgesagt / noch nicht geprüft | Neutral | Offene normale Vorgänge und entsprechende Status |
| Laufend / geplant | Blau | In Arbeit, angefragt/bestellt, geplanter Termin |
| Wichtig / Hinweis | Orange | Wichtige Arbeit, bald/heute fällige Wartung, Teilbelieferung, Klartext-/Importhinweise |
| Dringend / überfällig / fehlerhaft / löschen | Rot | Dringende offene Arbeit, Überfälligkeit, Fehler und Entfernen-Aktionen |
| Fertig / geprüft | Grün | Erledigte Arbeit/Termin, vollständige Lieferung, aktuell geprüfte Anleitung |

Statusmarkierungen kombinieren Farbe, Beschriftung und unterschiedliche Symbole. Bei Vorgängen hat „Erledigt“ Vorrang vor alter Priorität/Fälligkeit; abgeschlossene dringende Arbeiten bleiben grün. Danach haben Dringlichkeit/Überfälligkeit und Wichtigkeit Vorrang vor dem laufenden Status. Die drei dunklen Konzepte teilen ihre Statusfarben; Tageslicht verwendet dunklere Status-Schrift auf hellen Containern. So bleiben Bedeutung und Lesbarkeit erhalten, auch wenn der Markenakzent Kupfer oder Blau ist.

## Kontrastprüfung

Die automatisierten JVM-Prüfungen berechnen sRGB-Kontraste aus den tatsächlichen zentralen Farben: Haupt-/Hinweis-/Aktions- und Statusschrift auf Hintergrund, Karten und erhöhten Flächen; Text auf gefüllten Aktionen, aktiver Auswahl und sämtlichen Statuscontainern. Ziel mindestens 4,5 : 1 für diese normal großen Texte. Eingabekonturen mindestens 3 : 1, ergänzende dekorative Kartenlinien mindestens 2 : 1. Letzteres ist keine Aussage zur Erfüllung einer Eingabe-Konturvorgabe.

| Konzept | Kleinster geprüfter Textkontrast | Kleinster Eingabekontur-Kontrast |
| --- | --- | --- |
| Petrol & Mint | 4,98 : 1 | 3,30 : 1 |
| Stahlblau | 5,57 : 1 | 3,46 : 1 |
| Graphit & Kupfer | 4,96 : 1 | 3,54 : 1 |
| Tageslicht | 5,06 : 1 | 3,86 : 1 |

Dies bewertet konkrete Farbpaarungen, keine Zertifizierung der ganzen App. Transparente deaktivierte Elemente, Fotos, Android-Prompts und reale Display-/Lichtverhältnisse sind nicht durch diese Zahlen abgedeckt.

## Validierung

Am 30.09.2026 erfolgreich am Quellstand `4d3762dffb753ab2421bc39197484a4f9bd60bbd`:

- [Android-Build](https://github.com/sirmebro-sketch/WerkLog/actions/runs/36776822398): alle **64 JVM-Tests**, Lint, APK-Build und Prüfung ohne Internet-/Netzwerkstatus-Berechtigung bestanden.
- [Android-15-Emulator](https://github.com/sirmebro-sketch/WerkLog/actions/runs/36776822241): alle **vier Instrumentierungstests** bestanden.
- Acht Rasterdurchläufe: alle vier produktiven Themes bei 320 dp Breite und 100 %/180 % Schrift. Alle neun Kacheln gleichmäßig und ohne Textüberläufe, jeder Aufruf geprüft.
- Tatsächliche Hintergründe der vier App-Themes, passende Leistenhelligkeit, sofortiger Wechsel, Auswahl nach Activity-Neuerstellung und Systemoption geprüft.
- Ein ungespeichertes Anlagenformular über Farbwechsel, Sperren und Activity-Neuerstellung erhalten, verborgen während der Sperre, danach erfolgreich gespeichert. Der Zwischenstand bleibt verschlüsselt.
- Bildschirmaufnahmen der vier Betrieb-Varianten, Raster mit großer Schrift sowie der hellen Einstellungen, Heute-Ansicht, Formular- und Kalenderansicht, Statusanzeigen, Anleitung, großen Fotoansicht und Sperrmaske visuell geprüft. Der Fototest öffnet ein zunächst eingeklapptes Bild ausdrücklich, wie im normalen Bedienablauf.
- Bestehende Navigation, Rückwege, Speicherfehler, verschlüsselte Formularwiederaufnahme, simulierte Kamera-Rückgabe, Passwortwechsel, Bestellverknüpfung und JPEG-Kollegenimport weiterhin erfolgreich geprüft.

Ausgelieferte APK: `WerkLog-0.7.0-Test.apk`, **61.129.244 Bytes**, SHA-256 `240bc65f3c9d4a969d9d25b6b00f3220bc1f851e01df4be52ae91315642d9df1`. Der heruntergeladene APK-Buildcontainer entspricht dem GitHub-Artefakt mit SHA-256 `2eb27ec98fae3ea26cf4418ed107fd166763a50d8e3b0d703555be4f958037eb`. Der Layoutcontainer wurde ebenfalls auf seine Artefakt-Prüfsumme geprüft.

Weiterhin erforderlich: S24 Ultra bei tatsächlichem Umgebungslicht, Samsung-Schrift-/Anzeigeeinstellungen, TalkBack, Gesten-/Drei-Tasten-Navigation und reale Biometrie/Kamera/Mail-App. Datenschema 7 und Offline-Schutz bleiben unverändert. Vor einer Installation offene Formulare speichern und Vollsicherung erstellen; Debug-Signaturen können einen Neuinstallationsschritt nötig machen, der App-Daten löscht.
