<div align="center">

<img src="../../assets/graphics/readme-banner.png" alt="NoveLauncher — приватный лаунчер для Android с ИИ на устройстве" width="100%">

<br>

**Лаунчер для Android, который изучает ваши привычки — и никуда их не отправляет.**

[![Версия](https://img.shields.io/badge/version-9.3.0-7C7CFF?style=for-the-badge)](https://github.com/DrummingBird1/NoveLauncher/releases/latest)
[![Платформа](https://img.shields.io/badge/Android-8.0%2B-4ECDC4?style=for-the-badge&logo=android&logoColor=white)](#системные-требования)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)

[**Сайт**](https://drummingbird1.github.io/NoveLauncher/) ·
[**Скачать**](https://github.com/DrummingBird1/NoveLauncher/releases/latest) ·
[**Конфиденциальность**](https://drummingbird1.github.io/NoveLauncher/privacy.html) ·
[**История изменений**](../../CHANGELOG.md) ·
[**Участие в проекте**](../../CONTRIBUTING.md)

**Другие языки:**
[English](../../README.md) ·
[עברית](README.he.md) ·
[العربية](README.ar.md) ·
[Français](README.fr.md) ·
[Español](README.es.md) ·
[Deutsch](README.de.md)

</div>

---

## Что это

NoveLauncher заменяет домашний экран Android. Он ранжирует и группирует ваши
приложения по реальному использованию — что вы открываете, когда и как часто —
чтобы нужное уже было перед глазами.

Ранжирование выполняется **полностью на вашем устройстве**. Без аккаунта, без
сервера, без аналитики, без рекламы. Данные об использовании никогда не
покидают телефон.

## Возможности

| | |
|---|---|
| 🧠 **Умное ранжирование** | Приложения сортируются по давности, частоте, времени суток и категории — всё считается локально |
| 📁 **Умные папки** | Автоматическая группировка по 11 категориям, без ручной сортировки |
| 🎨 **Глубокая настройка** | 12 цветовых пресетов, Material You, свои цвета, шрифты, 12 форм значков, поддержка наборов значков |
| 🔒 **Блокировка приложений** | PIN, пароль, графический ключ или биометрия для отдельных приложений, приватная папка и скрытые приложения |
| 🌍 **7 языков** | Иврит, английский, арабский, французский, русский, испанский, немецкий — с полной поддержкой RTL |
| 📰 **Лента новостей** | Встроенные источники и ваши собственные RSS-ленты |
| 💾 **Резервные копии** | Локально, Google Drive или NAS/WebDAV, по расписанию, с опциональным шифрованием паролем |
| 📊 **Статистика использования** | Экранное время и использование приложений, хранится только на устройстве |
| 🔍 **Глобальный поиск** | Приложения, контакты и настройки из одной строки поиска |
| 🧩 **Виджеты и док** | Стандартные виджеты Android и закреплённый нижний док |

## Скриншоты

<div align="center">

<img src="../../assets/screenshots/google-play/04-home.png" width="24%" alt="Домашний экран">
<img src="../../assets/screenshots/google-play/10-apps.png" width="24%" alt="Список приложений">
<img src="../../assets/screenshots/google-play/07-themes.png" width="24%" alt="Темы">
<img src="../../assets/screenshots/google-play/08-security.png" width="24%" alt="Безопасность">

</div>

## Конфиденциальность

Это смысл всего проекта, поэтому стоит быть точным:

- **Ничего не собирается.** Ни аналитики, ни телеметрии, ни отчётов о сбоях в
  распространяемых сборках, ни аккаунта, ни рекламы.
- **Данные об использовании остаются локальными.** Статистика хранится в базе
  данных на устройстве и используется только для ранжирования приложений.
- **Единственный сетевой запрос** — виджет погоды, отправляющий примерные
  координаты в [Open-Meteo](https://open-meteo.com), и только если вы выдали
  разрешение на геолокацию. Без него используется город по умолчанию.
- **Резервные копии — ваши.** Локально, в вашем Google Drive или на вашем NAS,
  с шифрованием паролем, который знаете только вы.

Подробнее: [**Политика конфиденциальности**](https://drummingbird1.github.io/NoveLauncher/privacy.html) ·
[Условия использования](https://drummingbird1.github.io/NoveLauncher/terms.html)

> **О блокировке приложений:** блокировка, приватная папка и скрытые
> приложения — это **сдерживающая мера, а не безопасная песочница**.
> «Заблокированное» приложение всё ещё доступно через недавние приложения,
> уведомления, другой лаунчер или ADB — это верно для любого стороннего
> лаунчера. Для настоящей изоляции используйте встроенный рабочий профиль Android.

## Загрузка

Скачайте подписанный APK из [**последнего релиза**](https://github.com/DrummingBird1/NoveLauncher/releases/latest).

Потребуется разрешить установку из неизвестных источников. Все релизы
подписаны одним ключом, поэтому обновления устанавливаются поверх друг друга
без проблем.

### Системные требования

- Android 8.0 (API 26) или новее
- около 25 МБ свободного места

## Сборка из исходников

```bash
git clone https://github.com/DrummingBird1/NoveLauncher.git
cd NoveLauncher/android
./gradlew assembleDebug
```

Откройте папку **`android/`** в Android Studio (Ladybug или новее) — не корень
репозитория, который не является проектом Gradle.

## Участие в проекте

Issues и pull requests приветствуются — см.
[CONTRIBUTING.md](../../CONTRIBUTING.md). По вопросам безопасности сначала
прочитайте [SECURITY.md](../../SECURITY.md) и сообщайте приватно.

## Поддержать проект

NoveLauncher бесплатен, без рекламы и встроенных покупок. Если хотите
поддержать разработку:

<div align="center">

[![Patreon](https://img.shields.io/badge/Patreon-Support-FF424D?style=for-the-badge&logo=patreon&logoColor=white)](https://www.patreon.com/cw/MrIdan)
[![Buy Me a Coffee](https://img.shields.io/badge/Buy%20Me%20a%20Coffee-Support-FFDD00?style=for-the-badge&logo=buymeacoffee&logoColor=black)](https://buymeacoffee.com/novelauncher)

</div>

## Контакты

Вопросы, ошибки или предложения: **solvaris2@gmail.com** или
[создайте issue](https://github.com/DrummingBird1/NoveLauncher/issues).

## Лицензия

См. [LICENSE](../../LICENSE). Исходный код опубликован для прозрачности и
аудита — он не распространяется под лицензией с открытым исходным кодом.
