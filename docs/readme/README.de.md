<div align="center">

<img src="../../assets/graphics/readme-banner.png" alt="NoveLauncher — ein privater Android-Launcher mit KI auf dem Gerät" width="100%">

<br>

**Ein Android-Launcher, der deine Gewohnheiten lernt — ohne sie irgendwohin zu senden.**

[![Version](https://img.shields.io/badge/version-9.3.0-7C7CFF?style=for-the-badge)](https://github.com/DrummingBird1/NoveLauncher/releases/latest)
[![Plattform](https://img.shields.io/badge/Android-8.0%2B-4ECDC4?style=for-the-badge&logo=android&logoColor=white)](#systemvoraussetzungen)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)

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
| 🧠 **Intelligente Sortierung** | Apps nach Aktualität, Häufigkeit, Tageszeit und Kategorie — lokal berechnet |
| 📁 **Intelligente Ordner** | Automatische Gruppierung in 11 Kategorien, ohne manuelles Sortieren |
| 🎨 **Umfassende Anpassung** | 12 Farbpaletten, Material You, eigene Farben, Schriften, 12 Symbolformen, Icon-Pack-Unterstützung |
| 🔒 **App-Sperre** | PIN, Passwort, Muster oder Biometrie pro App, privater Ordner und ausgeblendete Apps |
| 🌍 **7 Sprachen** | Hebräisch, Englisch, Arabisch, Französisch, Russisch, Spanisch, Deutsch — vollständig RTL-fähig |
| 📰 **Nachrichten** | Integrierte Quellen sowie eigene RSS-Feeds |
| 💾 **Sicherungen** | Lokal, Google Drive oder NAS/WebDAV, zeitgesteuert, optional passwortverschlüsselt |
| 📊 **Nutzungsstatistik** | Bildschirmzeit und App-Nutzung, ausschließlich auf dem Gerät gespeichert |
| 🔍 **Globale Suche** | Apps, Kontakte und Einstellungen aus einem einzigen Suchfeld |
| 🧩 **Widgets und Dock** | Standard-Android-Widgets und ein angeheftetes unteres Dock |

## Screenshots

<div align="center">

<img src="../../assets/screenshots/google-play/04-home.png" width="24%" alt="Startbildschirm">
<img src="../../assets/screenshots/google-play/10-apps.png" width="24%" alt="App-Übersicht">
<img src="../../assets/screenshots/google-play/07-themes.png" width="24%" alt="Designs">
<img src="../../assets/screenshots/google-play/08-security.png" width="24%" alt="Sicherheit">

</div>

## Datenschutz

Das ist der eigentliche Zweck des Projekts, deshalb hier präzise:

- **Es wird nichts erfasst.** Keine Analyse, keine Telemetrie, keine
  Absturzberichte in ausgelieferten Builds, kein Konto, keine Werbung.
- **Nutzungsdaten bleiben lokal.** Die Statistiken liegen in einer Datenbank
  auf deinem Gerät und dienen ausschließlich der App-Sortierung.
- **Der einzige Netzwerkaufruf** ist das Wetter-Widget, das ungefähre
  Koordinaten an [Open-Meteo](https://open-meteo.com) sendet — und nur, wenn
  du die Standortberechtigung erteilst. Andernfalls wird eine Standardstadt
  verwendet.
- **Deine Sicherungen gehören dir.** Lokal, in deinem Google Drive oder auf
  deinem NAS, mit Verschlüsselung durch ein Passwort, das nur du kennst.

Vollständige Details: [**Datenschutzerklärung**](https://drummingbird1.github.io/NoveLauncher/privacy.html) ·
[Nutzungsbedingungen](https://drummingbird1.github.io/NoveLauncher/terms.html)

> **Zur App-Sperre:** App-Sperre, privater Ordner und ausgeblendete Apps
> wirken **abschreckend, sind aber keine sichere Sandbox**. Eine „gesperrte"
> App bleibt über die letzten Apps, Benachrichtigungen, einen anderen Launcher
> oder ADB erreichbar — das gilt für jeden Drittanbieter-Launcher. Für echte
> Isolation nutze das in Android integrierte Arbeitsprofil.

## Download

Lade die signierte APK aus der [**neuesten Version**](https://github.com/DrummingBird1/NoveLauncher/releases/latest).

Du musst die Installation aus unbekannten Quellen erlauben. Alle Versionen
sind mit demselben Schlüssel signiert, Updates lassen sich also sauber
übereinander installieren.

### Systemvoraussetzungen

- Android 8.0 (API 26) oder neuer
- etwa 25 MB Speicherplatz

## Aus dem Quellcode bauen

```bash
git clone https://github.com/DrummingBird1/NoveLauncher.git
cd NoveLauncher/android
./gradlew assembleDebug
```

Öffne den Ordner **`android/`** in Android Studio (Ladybug oder neuer) — nicht
das Repository-Stammverzeichnis, das kein Gradle-Projekt ist.

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
