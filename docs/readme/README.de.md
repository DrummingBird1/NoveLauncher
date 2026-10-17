<div align="center">

<img src="../../assets/graphics/readme-banner.png" alt="NoveLauncher — ein privater Android-Launcher mit KI auf dem Gerät" width="100%">

<br>

**Ein Android-Launcher, der deine Gewohnheiten lernt — ohne sie irgendwohin zu senden.**

[![Version](https://img.shields.io/badge/version-9.3.0-7C7CFF?style=for-the-badge)](https://github.com/DrummingBird1/NoveLauncher/releases/latest)
[![Plattform](https://img.shields.io/badge/Android-8.0%2B-4ECDC4?style=for-the-badge&logo=android&logoColor=white)](#systemvoraussetzungen)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)

[**Website**](https://drummingbird1.github.io/NoveLauncher/) ·
[**Herunterladen**](https://github.com/DrummingBird1/NoveLauncher/releases/latest) ·
[**Datenschutz**](https://drummingbird1.github.io/NoveLauncher/privacy.html) ·
[**Änderungsprotokoll**](../../CHANGELOG.md) ·
[**Mitwirken**](../../CONTRIBUTING.md)

**Weitere Sprachen:**
[English](../../README.md) ·
[עברית](README.he.md) ·
[العربية](README.ar.md) ·
[Français](README.fr.md) ·
[Русский](README.ru.md) ·
[Español](README.es.md)

</div>

---

## Was es ist

NoveLauncher ersetzt den Android-Startbildschirm. Er sortiert und gruppiert
deine Apps nach deiner tatsächlichen Nutzung — was du öffnest, wann und wie
oft — damit das Wichtige bereits vor dir liegt.

Die Sortierung läuft **vollständig auf deinem Gerät**. Kein Konto, kein
Server, keine Analyse, keine Werbung. Deine Nutzungsdaten verlassen dein
Telefon nie.

## Funktionen

| | |
|---|---|
| 🧠 **Vorschläge, die dazulernen** | Apps sortiert nach Aktualität, Häufigkeit, Tageszeit und Kategorie; „Jetzt empfohlen“ lernt außerdem, welche Vorschläge du tatsächlich öffnest — alles lokal berechnet |
| 📁 **Intelligente Ordner** | Automatische Gruppierung in 11 Kategorien, ohne manuelles Sortieren |
| 💼 **Arbeitsprofil und Privater Bereich** | Arbeits-Apps mit ihrem Badge und einem Pausenschalter; der Private Bereich von Android 15 in einem eigenen, sperrbaren Abschnitt — nie mit Vorschlägen vermischt und in der Suche ausgeblendet, solange er gesperrt ist |
| ⚡ **App-Shortcuts** | Lange auf eine App drücken, um ihre Shortcuts zu öffnen — neue Nachricht, gespeicherte Route — direkt vom Startbildschirm |
| 🔍 **Intelligente Suche** | Apps, Kontakte und Einstellungen aus einem Feld. Abgleich zwischen Hebräisch und lateinischer Schrift („וואטסאפ“ findet WhatsApp), Tippfehlertoleranz und Initialen („gm“ → Google Maps), dazu ein Rechner und Einheitenumrechnung |
| 🕯️ **Jüdischer Kalender und Schabbat-Modus** | Hebräisches Datum, Feiertage (Israel oder Diaspora), Zeiten für Kerzenzünden und Hawdala, auf dem Handy berechnet; der Schabbat-Modus blendet ablenkende Vorschläge und Benachrichtigungs-Badges vom Kerzenzünden bis zum Einbruch der Nacht aus |
| 🖼️ **Hintergründe** | Eigene Fotos oder integrierte Verläufe — einer für alles, pro Seite, nach Uhrzeit, nach Wochentag oder der System-Hintergrund (auch Live) — mit Helligkeit, Kontrast, Sättigung, Wärme, Unschärfe und Vignette |
| 👆 **Gesten** | Nach oben wischen für Apps, nach unten für Benachrichtigungen, Doppeltippen schaltet den Bildschirm aus |
| 🎨 **Umfassende Anpassung** | 12 Farbpaletten, Material You, eigene Farben, 16 Schriften, 12 Symbolformen |
| 🧷 **Icon-Pakete** | Jedes mit Nova / ADW / Lawnchair kompatible Icon-Paket anwenden; Apps, die es nicht abdeckt, behalten ihr eigenes Symbol |
| 🔒 **App-Sperre** | PIN, Passwort, Muster oder Biometrie pro App, privater Ordner und ausgeblendete Apps |
| 🌍 **7 Sprachen** | Hebräisch, Englisch, Arabisch, Französisch, Russisch, Spanisch, Deutsch — vollständig RTL-fähig |
| 📰 **Nachrichten** | Integrierte Quellen sowie eigene RSS-Feeds |
| 💾 **Sicherungen** | Lokal, Google Drive oder NAS/WebDAV, zeitgesteuert, optional passwortverschlüsselt |
| 📊 **Nutzungsstatistik** | Bildschirmzeit und App-Nutzung pro Tag, Woche oder Monat, direkt auf dem Gerät aus Android gelesen |
| 🧩 **Widgets und Dock** | Standard-Android-Widgets (mit ihrem Einrichtungsbildschirm, in der Größe anpassbar) und ein angeheftetes unteres Dock |

## Screenshots

<div align="center">

<img src="../../assets/screenshots/google-play/01-home.png" width="24%" alt="Startbildschirm">
<img src="../../assets/screenshots/google-play/04-apps-profiles.png" width="24%" alt="App-Übersicht">
<img src="../../assets/screenshots/google-play/07-themes.png" width="24%" alt="Designs">
<img src="../../assets/screenshots/google-play/08-security.png" width="24%" alt="Sicherheit">

</div>

## Datenschutz

Das ist der eigentliche Zweck des Projekts, deshalb hier präzise:

- **Es wird nichts erfasst.** Keine Analyse, keine Telemetrie, keine
  Absturzberichte in ausgelieferten Builds, kein Konto, keine Werbung.
- **Nutzungsdaten bleiben lokal.** Die App-Nutzung wird aus Androids
  Nutzungsstatistik gelesen und auf deinem Gerät verarbeitet; was die Vorschläge
  lernen, liegt in einer lokalen Datenbank. Nichts davon wird hochgeladen.
- **Nur vier Funktionen nutzen das Netzwerk — sonst nichts:**
  - **Wetter** — fragt die aktuellen Bedingungen bei
    [Open-Meteo](https://open-meteo.com) ab. Deine ungefähren Koordinaten werden
    nur gesendet, wenn du die Standortberechtigung erteilst; sonst wird das
    Wetter einer Standardstadt abgefragt.
  - **Nachrichten** — die Nachrichtenseite lädt die RSS-Feeds, die du aktiviert
    hast, direkt vom jeweiligen Anbieter.
  - **Sicherungen** — nur, wenn du sie wählst: dein eigenes Google Drive
    (Bereich `drive.file`, die App sieht also nur die Sicherungsdateien, die sie
    selbst anlegt — nie den Rest deines Drive) oder dein eigenes NAS über HTTPS.
  - **Update-Prüfung** — standardmäßig aus. Wenn du sie einschaltest, fragt die
    App höchstens einmal am Tag die Release-API von GitHub nach der neuesten
    Version. Über dich oder dein Gerät wird nichts gesendet, und nichts wird
    automatisch heruntergeladen.

  Alles andere bleibt auf deinem Gerät.
- **Deine Sicherungen gehören dir.** Lokal, in deinem Google Drive oder auf
  deinem NAS, mit Verschlüsselung durch ein Passwort, das nur du kennst.
- **Der jüdische Kalender wird auf dem Handy berechnet.** Feiertags-,
  Kerzenzünd- und Hawdala-Zeiten nutzen deinen ungefähren Standort, wenn du ihn
  erlaubst (sonst eine Standardstadt), und werden nirgendwohin gesendet.
- **Der Gesten-Helfer liest nichts.** Doppeltippen zum Ausschalten des
  Bildschirms nutzt einen optionalen Bedienungshilfen-Dienst, der genau zwei
  Aktionen ausführt (Bildschirm aus, Benachrichtigungen öffnen) und nicht sehen
  kann, was auf dem Bildschirm ist.

Vollständige Details: [**Datenschutzerklärung**](https://drummingbird1.github.io/NoveLauncher/privacy.html) ·
[Nutzungsbedingungen](https://drummingbird1.github.io/NoveLauncher/terms.html)

> **Zur App-Sperre:** App-Sperre, privater Ordner und ausgeblendete Apps
> wirken **abschreckend, sind aber keine sichere Sandbox**. Eine „gesperrte"
> App bleibt über die letzten Apps, Benachrichtigungen, einen anderen Launcher
> oder ADB erreichbar — das gilt für jeden Drittanbieter-Launcher. Für echte
> Isolation nutze das in Android integrierte Arbeitsprofil oder den Privaten
> Bereich (ab Android 15) — NoveLauncher unterstützt beide.

## Download

Lade die signierte APK aus der [**neuesten Version**](https://github.com/DrummingBird1/NoveLauncher/releases/latest).

Du musst die Installation aus unbekannten Quellen erlauben. Alle Versionen
sind mit demselben Schlüssel signiert, Updates lassen sich also sauber
übereinander installieren.

<a href="https://github.com/ImranR98/Obtainium"><img src="../assets/badge-obtainium.png" alt="Get it on Obtainium" height="62"></a>

**Automatische Updates:** [Obtainium](https://github.com/ImranR98/Obtainium) installiert NoveLauncher direkt
aus diesen GitHub-Releases und hält es aktuell. Füge in Obtainium eine neue App
hinzu und trage `https://github.com/DrummingBird1/NoveLauncher` als Quelle ein.

### Systemvoraussetzungen

- Android 8.0 (API 26) oder neuer
- etwa 25 MB Speicherplatz

Manche Funktionen brauchen mehr: Der Private Bereich setzt Android 15 voraus,
Doppeltippen zum Ausschalten des Bildschirms Android 9. Privater Bereich,
App-Shortcuts und das Pausieren von Arbeits-Apps funktionieren nur, wenn
NoveLauncher deine Standard-Startbildschirm-App ist.

## Aus dem Quellcode bauen

```bash
git clone https://github.com/DrummingBird1/NoveLauncher.git
cd NoveLauncher/android
./gradlew assembleDebug
```

Öffne den Ordner **`android/`** in einer Android-Studio-Version, die das
Android Gradle Plugin 9.4 unterstützt — nicht das Repository-Stammverzeichnis,
das kein Gradle-Projekt ist. Gradle braucht **JDK 21** (in Android Studio als
Gradle-JDK einstellen); die App selbst wird zu Java-17-Bytecode kompiliert.

## Mitwirken

Issues und Pull Requests sind willkommen — siehe
[CONTRIBUTING.md](../../CONTRIBUTING.md). Sicherheitsprobleme bitte zuerst in
[SECURITY.md](../../SECURITY.md) nachlesen und privat melden.

## Projekt unterstützen

NoveLauncher ist kostenlos, werbefrei und ohne In-App-Käufe. Wenn du die
Entwicklung unterstützen möchtest:

<div align="center">

[![Patreon](https://img.shields.io/badge/Patreon-Unterst%C3%BCtzen-FF424D?style=for-the-badge&logo=patreon&logoColor=white)](https://www.patreon.com/cw/MrIdan)
[![Buy Me a Coffee](https://img.shields.io/badge/Buy%20Me%20a%20Coffee-Unterst%C3%BCtzen-FFDD00?style=for-the-badge&logo=buymeacoffee&logoColor=black)](https://buymeacoffee.com/novelauncher)

</div>

## Kontakt

Fragen, Fehler oder Wünsche: **solvaris2@gmail.com** oder
[eröffne ein Issue](https://github.com/DrummingBird1/NoveLauncher/issues).

## Lizenz

Siehe [LICENSE](../../LICENSE). Der Quellcode wird zur Transparenz und
Überprüfung veröffentlicht — er steht nicht unter einer Open-Source-Lizenz.
