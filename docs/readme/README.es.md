<div align="center">

<img src="../../assets/graphics/readme-banner.png" alt="NoveLauncher — un launcher de Android privado con IA en el dispositivo" width="100%">

<br>

**Un launcher de Android que aprende tus hábitos — sin enviarlos a ninguna parte.**

[![Versión](https://img.shields.io/badge/version-9.3.0-7C7CFF?style=for-the-badge)](https://github.com/DrummingBird1/NoveLauncher/releases/latest)
[![Plataforma](https://img.shields.io/badge/Android-8.0%2B-4ECDC4?style=for-the-badge&logo=android&logoColor=white)](#requisitos)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)

[**Sitio web**](https://drummingbird1.github.io/NoveLauncher/) ·
[**Descargar**](https://github.com/DrummingBird1/NoveLauncher/releases/latest) ·
[**Privacidad**](https://drummingbird1.github.io/NoveLauncher/privacy.html) ·
[**Registro de cambios**](../../CHANGELOG.md) ·
[**Contribuir**](../../CONTRIBUTING.md)

**Otros idiomas:**
[English](../../README.md) ·
[עברית](README.he.md) ·
[العربية](README.ar.md) ·
[Français](README.fr.md) ·
[Русский](README.ru.md) ·
[Deutsch](README.de.md)

</div>

---

## Qué es

NoveLauncher reemplaza la pantalla de inicio de Android. Ordena y agrupa tus
aplicaciones según tu uso real — qué abres, cuándo y con qué frecuencia — para
que lo que necesitas ya esté delante de ti.

La clasificación se ejecuta **por completo en tu dispositivo**. Sin cuenta,
sin servidor, sin analíticas, sin anuncios. Tus datos de uso nunca salen del
teléfono.

## Características

| | |
|---|---|
| 🧠 **Clasificación inteligente** | Apps ordenadas por uso reciente, frecuencia, hora del día y categoría — calculado localmente |
| 📁 **Carpetas inteligentes** | Agrupación automática en 11 categorías, sin ordenar a mano |
| 🎨 **Personalización profunda** | 12 paletas, Material You, colores propios, fuentes, 12 formas de icono, packs de iconos |
| 🔒 **Bloqueo de apps** | PIN, contraseña, patrón o biometría por aplicación, carpeta privada y apps ocultas |
| 🌍 **7 idiomas** | Hebreo, inglés, árabe, francés, ruso, español y alemán — totalmente compatible con RTL |
| 📰 **Noticias** | Fuentes integradas y tus propios feeds RSS |
| 💾 **Copias de seguridad** | Local, Google Drive o NAS/WebDAV, programadas, con cifrado por contraseña opcional |
| 📊 **Estadísticas de uso** | Tiempo de pantalla y uso de apps, almacenado solo en el dispositivo |
| 🔍 **Búsqueda global** | Apps, contactos y ajustes desde un único cuadro de búsqueda |
| 🧩 **Widgets y dock** | Widgets estándar de Android y un dock inferior fijo |

## Capturas de pantalla

<div align="center">

<img src="../../assets/screenshots/google-play/04-home.png" width="24%" alt="Pantalla de inicio">
<img src="../../assets/screenshots/google-play/10-apps.png" width="24%" alt="Cajón de aplicaciones">
<img src="../../assets/screenshots/google-play/07-themes.png" width="24%" alt="Temas">
<img src="../../assets/screenshots/google-play/08-security.png" width="24%" alt="Seguridad">

</div>

## Privacidad

Es la razón de ser del proyecto, así que conviene ser preciso:

- **No se recopila nada.** Sin analíticas, sin telemetría, sin informes de
  fallos en las versiones distribuidas, sin cuenta, sin anuncios.
- **Los datos de uso son locales.** Las estadísticas se guardan en una base de
  datos del dispositivo y solo sirven para ordenar tus aplicaciones.
- **La única llamada de red** es el widget del tiempo, que envía coordenadas
  aproximadas a [Open-Meteo](https://open-meteo.com) — y solo si concedes el
  permiso de ubicación. Si lo deniegas, se usa una ciudad predeterminada.
- **Tus copias son tuyas.** En local, en tu Google Drive o en tu NAS, con
  cifrado mediante una contraseña que solo tú conoces.

Detalle completo: [**Política de privacidad**](https://drummingbird1.github.io/NoveLauncher/privacy.html) ·
[Términos de servicio](https://drummingbird1.github.io/NoveLauncher/terms.html)

> **Sobre el bloqueo de apps:** el bloqueo, la carpeta privada y las apps
> ocultas son **disuasorios, no un espacio aislado seguro**. Una app
> «bloqueada» sigue siendo accesible desde recientes, notificaciones, otro
> launcher o ADB — algo cierto en cualquier launcher de terceros. Para un
> aislamiento real, usa el Perfil de trabajo integrado de Android.

## Descarga

Obtén el APK firmado desde la [**última versión**](https://github.com/DrummingBird1/NoveLauncher/releases/latest).

Tendrás que permitir la instalación desde orígenes desconocidos. Todas las
versiones están firmadas con la misma clave, así que las actualizaciones se
instalan sin conflictos.

### Requisitos

- Android 8.0 (API 26) o superior
- unos 25 MB de almacenamiento

## Compilar desde el código

```bash
git clone https://github.com/DrummingBird1/NoveLauncher.git
cd NoveLauncher/android
./gradlew assembleDebug
```

Abre la carpeta **`android/`** en Android Studio (Ladybug o superior) — no la
raíz del repositorio, que no es un proyecto Gradle.

## Contribuir

Issues y pull requests son bienvenidos — consulta
[CONTRIBUTING.md](../../CONTRIBUTING.md). Para temas de seguridad, lee primero
[SECURITY.md](../../SECURITY.md) y repórtalo en privado.

## Apoya el proyecto

NoveLauncher es gratuito, sin anuncios y sin compras integradas. Si quieres
apoyar el desarrollo:

<div align="center">

[![Patreon](https://img.shields.io/badge/Patreon-Apoyar-FF424D?style=for-the-badge&logo=patreon&logoColor=white)](https://www.patreon.com/cw/MrIdan)
[![Buy Me a Coffee](https://img.shields.io/badge/Buy%20Me%20a%20Coffee-Apoyar-FFDD00?style=for-the-badge&logo=buymeacoffee&logoColor=black)](https://buymeacoffee.com/novelauncher)

</div>

## Contacto

Preguntas, errores o sugerencias: **solvaris2@gmail.com** o
[abre una issue](https://github.com/DrummingBird1/NoveLauncher/issues).

## Licencia

Consulta [LICENSE](../../LICENSE). El código se publica por transparencia y
para su revisión — no se distribuye bajo una licencia de código abierto.
