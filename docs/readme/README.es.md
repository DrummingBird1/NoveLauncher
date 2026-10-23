<div align="center">

<img src="../../assets/graphics/readme-banner.png" alt="NoveLauncher — un launcher de Android privado con IA en el dispositivo" width="100%">

<br>

**Un launcher de Android que aprende tus hábitos — sin enviarlos a ninguna parte.**

[![Versión](https://img.shields.io/badge/version-10.0.0-7C7CFF?style=for-the-badge)](https://github.com/DrummingBird1/NoveLauncher/releases/latest)
[![Plataforma](https://img.shields.io/badge/Android-8.0%2B-4ECDC4?style=for-the-badge&logo=android&logoColor=white)](#requisitos)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)

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
| 🧠 **Sugerencias que aprenden** | Apps ordenadas por uso reciente, frecuencia, hora del día y categoría; «Recomendadas ahora» también aprende de las sugerencias que de verdad abres — todo calculado localmente |
| 📁 **Carpetas inteligentes** | Agrupación automática en 11 categorías, sin ordenar a mano |
| 💼 **Perfil de trabajo y Espacio privado** | Apps de trabajo con su insignia y un interruptor para pausarlas; el Espacio privado de Android 15 en su propia sección bloqueable, nunca mezclado con las sugerencias y oculto en la búsqueda mientras está bloqueado |
| ⚡ **Accesos directos de apps** | Mantén pulsada una app para ver sus accesos directos — nuevo mensaje, ruta guardada — desde la pantalla de inicio |
| 🔍 **Búsqueda inteligente** | Apps, contactos y ajustes desde un solo cuadro. Coincidencia entre hebreo y alfabeto latino («וואטסאפ» encuentra WhatsApp), tolerancia a erratas e iniciales («gm» → Google Maps), además de calculadora y conversión de unidades |
| 🕯️ **Calendario hebreo y modo Shabat** | Fecha hebrea, festividades (calendario de Israel o de la diáspora), horarios de encendido de velas y Havdalá calculados en el teléfono; el modo Shabat oculta las sugerencias que distraen y las insignias de notificación desde el encendido de velas hasta la noche |
| 🖼️ **Fondos de pantalla** | Tus propias fotos o degradados integrados — uno para todo, por página, por hora, por día de la semana o el fondo del sistema (también animado) — con brillo, contraste, saturación, calidez, desenfoque y viñeta |
| 👆 **Gestos** | Desliza hacia arriba para las apps, hacia abajo para las notificaciones, toca dos veces para apagar la pantalla |
| 🎨 **Personalización profunda** | 12 paletas, Material You, colores propios, 16 fuentes, 12 formas de icono |
| 🧷 **Paquetes de iconos** | Aplica cualquier paquete de iconos compatible con Nova / ADW / Lawnchair; las apps que no cubre conservan su icono |
| 🔒 **Bloqueo de apps** | PIN, contraseña, patrón o biometría por aplicación, carpeta privada y apps ocultas |
| 🌍 **7 idiomas** | Hebreo, inglés, árabe, francés, ruso, español y alemán — totalmente compatible con RTL |
| 📰 **Noticias** | Fuentes integradas y tus propios feeds RSS |
| 💾 **Copias de seguridad** | En local o en tu NAS/WebDAV, programadas, con cifrado por contraseña opcional |
| 📊 **Estadísticas de uso** | Tiempo de pantalla y uso de apps por día, semana o mes, leídos de Android en tu dispositivo |
| 🧩 **Widgets y dock** | Widgets estándar de Android (con su pantalla de configuración y de 1 a 4 filas de alto) y un dock inferior con hasta 5 apps fijadas |

## Capturas de pantalla

<div align="center">

<img src="../../assets/screenshots/google-play/01-home.png" width="24%" alt="Pantalla de inicio">
<img src="../../assets/screenshots/google-play/04-apps-profiles.png" width="24%" alt="Cajón de aplicaciones">
<img src="../../assets/screenshots/google-play/07-themes.png" width="24%" alt="Temas">
<img src="../../assets/screenshots/google-play/08-security.png" width="24%" alt="Seguridad">

</div>

## Privacidad

Es la razón de ser del proyecto, así que conviene ser preciso:

- **No se recopila nada.** Sin analíticas, sin telemetría, sin informes de
  fallos en las versiones distribuidas, sin cuenta, sin anuncios.
- **Los datos de uso son locales.** El uso de las apps se lee de las
  estadísticas de uso de Android y se procesa en tu dispositivo; lo que aprenden
  las sugerencias se guarda en una base de datos local. Nada de ello se sube.
- **Solo cuatro funciones usan la red — nada más:**
  - **Tiempo** — pide las condiciones actuales a
    [Open-Meteo](https://open-meteo.com). Envía tus coordenadas aproximadas solo
    si concedes el permiso de ubicación; sin él, pide el tiempo de una ciudad
    predeterminada.
  - **Noticias** — la página de Noticias descarga los feeds RSS que hayas
    activado, directamente de cada medio.
  - **Copias de seguridad** — solo si eliges un NAS: tu propio servidor, por
    HTTPS.
  - **Búsqueda de actualizaciones** — desactivada por defecto. Si la activas, la
    app consulta la API de versiones de GitHub como máximo una vez al día. No se
    envía nada sobre ti ni sobre tu dispositivo, y no se descarga nada
    automáticamente.

  Todo lo demás se queda en tu dispositivo.
- **Tus copias son tuyas.** En el teléfono o en tu NAS, con cifrado mediante
  una contraseña que solo tú conoces.
- **El calendario hebreo se calcula en el teléfono.** Los horarios de
  festividades, encendido de velas y Havdalá usan tu ubicación aproximada si lo
  permites (si no, una ciudad predeterminada) y no se envían a ningún sitio.
- **El asistente de gestos no lee nada.** Tocar dos veces para apagar la
  pantalla usa un servicio de accesibilidad opcional que realiza exactamente dos
  acciones (apagar la pantalla, abrir las notificaciones) y no puede ver lo que
  hay en pantalla.

Detalle completo: [**Política de privacidad**](https://drummingbird1.github.io/NoveLauncher/privacy.html) ·
[Términos de servicio](https://drummingbird1.github.io/NoveLauncher/terms.html)

> **Sobre el bloqueo de apps:** el bloqueo, la carpeta privada y las apps
> ocultas son **disuasorios, no un espacio aislado seguro**. Una app
> «bloqueada» sigue siendo accesible desde recientes, notificaciones, otro
> launcher o ADB — algo cierto en cualquier launcher de terceros. Para un
> aislamiento real, usa el Perfil de trabajo integrado de Android o el Espacio
> privado (Android 15 o superior); NoveLauncher es compatible con ambos.

## Descarga

Obtén el APK firmado desde la [**última versión**](https://github.com/DrummingBird1/NoveLauncher/releases/latest).

Tendrás que permitir la instalación desde orígenes desconocidos. Todas las
versiones están firmadas con la misma clave, así que las actualizaciones se
instalan sin conflictos.

<a href="https://github.com/ImranR98/Obtainium"><img src="../assets/badge-obtainium.png" alt="Get it on Obtainium" height="62"></a>

**Actualizaciones automáticas:** [Obtainium](https://github.com/ImranR98/Obtainium) instala NoveLauncher
directamente desde estas versiones de GitHub y lo mantiene actualizado. En
Obtainium, añade una app y pega `https://github.com/DrummingBird1/NoveLauncher`.

### Requisitos

- Android 8.0 (API 26) o superior
- unos 25 MB de almacenamiento

Algunas funciones necesitan más: el Espacio privado requiere Android 15, y tocar
dos veces para apagar la pantalla, Android 9. El Espacio privado, los accesos
directos de apps y la pausa de las apps de trabajo solo funcionan si
NoveLauncher es tu app de inicio predeterminada.

## Compilar desde el código

```bash
git clone https://github.com/DrummingBird1/NoveLauncher.git
cd NoveLauncher/android
./gradlew assembleDebug
```

Abre la carpeta **`android/`** en una versión de Android Studio compatible con
Android Gradle Plugin 9.4 — no la raíz del repositorio, que no es un proyecto
Gradle. Gradle necesita **JDK 21** (configúralo como Gradle JDK en Android
Studio); la app en sí se compila a bytecode de Java 17.

## Contribuir

Issues y pull requests son bienvenidos — consulta
[CONTRIBUTING.md](../../CONTRIBUTING.md). Para temas de seguridad, lee primero
[SECURITY.md](../../SECURITY.md) y repórtalo en privado.

## Apoya el proyecto

NoveLauncher es gratuito, sin anuncios y sin compras integradas. Si quieres
apoyar el desarrollo:

<div align="center">

[![Patreon](https://img.shields.io/badge/Patreon-Apoyar-FF424D?style=for-the-badge&logo=patreon&logoColor=white)](https://www.patreon.com/c/Solvaris)
[![Buy Me a Coffee](https://img.shields.io/badge/Buy%20Me%20a%20Coffee-Apoyar-FFDD00?style=for-the-badge&logo=buymeacoffee&logoColor=black)](https://buymeacoffee.com/solvaris)

</div>

## Contacto

Preguntas, errores o sugerencias: **solvaris2@gmail.com** o
[abre una issue](https://github.com/DrummingBird1/NoveLauncher/issues).

## Licencia

Consulta [LICENSE](../../LICENSE). El código se publica por transparencia y
para su revisión — no se distribuye bajo una licencia de código abierto.
