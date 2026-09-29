# Produktentscheidungen: persönlicher Offline-Technikbegleiter

Stand: 29.09.2026. Eigenständige Umsetzung, keine kopierten Oberflächen oder Inhalte.

## Branchenmuster als Inspiration

- MaintainX beschreibt verknüpfte Anlagen, Arbeitsaufträge, Verfahren und Ersatzteile: https://help.getmaintainx.com/work-order-form-fields
- MaintainX Facility Management: Informationen, Seriennummern und Verfahren am Einsatzort verfügbar machen: https://www.getmaintainx.com/industries/facility-management
- UpKeep Anlagenmanagement: Anlagenhistorie, Dokumente, Ersatzteile und Messwerte in einer Akte, optional per QR erreichbar: https://upkeep.com/product/asset-management
- UpKeep Energy & Utilities: Wartung nach Datum/Betriebsstunden und Ausfallhistorie: https://upkeep.com/solutions/energy-utilities/

WerkLog übernimmt das Prinzip der anlagenbezogenen Dokumentation, ohne Cloud, Teamkonto, KI-Auswertung oder Anlagenverbindung. Anbieterfunktionen und Werbeaussagen sind keine eigenen Leistungsversprechen.

## In 0.2.0 umgesetzt

Anlagenakte mit Übersicht, Wissen, Zugängen und Verlauf. Hersteller, Typ, Seriennummer, Standort, Servicekontakt, Ersatzteil-/Lagerhinweise, nächster Wartungstermin. Eigene Wissenseinträge mit Änderungsdatum. Mehrere vertrauliche Zugangseinträge pro Anlage inklusive lokalem Passwortgenerator. Historie von Störungen und Messwerten, neue Erfassung mit vorausgewählter Anlage. Wartungen bis 30 Tage und überfällige Termine im Dashboard. Suchbare Anlagenstammdaten; keine globale Passwortsuche. Verschlüsselte Datensicherung mit Migration aus 0.1.0.

Zusätzlich: expliziter verschlüsselter Dateiaustausch zwischen Kollegen, selektiver Inhalt, Zugangsdaten standardmäßig ausgeschlossen, Importvorschau ohne Überschreiben. Kein Server und keine Hintergrundsynchronisation.

## Sinnvolle Inhalte für Versorgungstechnik

- Allgemein: Anlagenkennzeichnung, Raum, Hersteller/Typ/Seriennummer, Plan-/Handbuch-Ablage, Servicekontakt, Ersatzteilnummer und Lagerort.
- Störungen: Zeitpunkt, beobachtetes Fehlerbild, Meldung/Fehlercode, Befund, durchgeführte Maßnahmen, Ergebnis, offener Folgeschritt, Zeitaufwand.
- Kälte/Heizung: eindeutig benannte Vor-/Rücklauf-Messpunkte, Temperaturen, Drücke, Differenzdruck, Betriebszustand zur Messung.
- Dampf/BHKW/Notstrom: Betriebsstunden, Prüf-/Probelaufbeobachtungen, Datum der letzten Maßnahme, verbrauchte Teile und Folgearbeiten.
- Wasseraufbereitung: eindeutig benannte Messstellen, Leitfähigkeit, Drücke/Differenzdrücke, Verbrauchsmaterial, Analyseverweis und Wartungsnotizen.
- GLT: Anlagen-/Meldekennzeichnung, Zeitstempel, genaue Meldung, Dokumentationsablage. Zugangsangaben ausschließlich im dafür vorgesehenen Bereich.

Keine vorgegebenen Sollwerte, Alarmgrenzen oder sicherheitsrelevanten Handlungsempfehlungen. Die App dokumentiert; freigegebene betriebliche Unterlagen und Verfahren bleiben maßgeblich. Datensätze werden nicht automatisch angelegt.

## In 0.3.0 ergänzt

Offline-Zählerfoto-Erkennung mit verpflichtender manueller Bestätigung und ohne Fotoarchiv. Lokaler Kraftwerkkalender mit Fremdfirma, Arbeit und Zuständigem. Allgemeine/anlagenbezogene Anleitungen mit sortierten Schritten und komprimierten Fotos. Bestelllisten mit mehreren Positionen, lokalen Anlagenzuordnungen, Bildanhängen und manueller E-Mail-Übergabe. Mehrere Bilder werden intern verschlüsselt gespeichert, maximal 160 KiB pro Bild und 30 Bilder insgesamt. Vor dem atomaren Speichern validiert die App das neue Datenmodell.

## In 0.4.0 ergänzt

Dreier-Navigation, Betriebskacheln, Einführung, Passwortwechsel, Biometrie, gezielte Entsperr-Wechselphase, getrennte verschlüsselte Bildablage, 500 Bilder, große Backups/Freigaben, Anlagenkennzeichen/Hierarchie/Favoriten/QR, Suche, Zählerverlauf/-wechsel, Terminserien/Erinnerungen, Vorlagen, Bestellstatus und Folgevorgänge. Aktueller Umsetzungsstand und offene Punkte stehen in ROADMAP.md.

Weitere bisher nicht beauftragte Vertiefungen: strukturierter Bereitschaftseinsatz (Anruf/Abfahrt/Ankunft/Arbeitsende/Rückkehr, ohne tarifliche Bewertung), PDF-Berichte und PDF-Anlagen. Nicht als vorhandene Funktionen ausgeben.
