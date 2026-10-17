<div align="center">

<img src="../../assets/graphics/readme-banner.png" alt="NoveLauncher — un launcher Android privé avec IA locale" width="100%">

<br>

**Un launcher Android qui apprend vos habitudes — sans jamais les envoyer ailleurs.**

[![Version](https://img.shields.io/badge/version-10.0.0-7C7CFF?style=for-the-badge)](https://github.com/DrummingBird1/NoveLauncher/releases/latest)
[![Plateforme](https://img.shields.io/badge/Android-8.0%2B-4ECDC4?style=for-the-badge&logo=android&logoColor=white)](#configuration-requise)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)

[**Site web**](https://drummingbird1.github.io/NoveLauncher/) ·
[**Télécharger**](https://github.com/DrummingBird1/NoveLauncher/releases/latest) ·
[**Confidentialité**](https://drummingbird1.github.io/NoveLauncher/privacy.html) ·
[**Journal des modifications**](../../CHANGELOG.md) ·
[**Contribuer**](../../CONTRIBUTING.md)

**Autres langues :**
[English](../../README.md) ·
[עברית](README.he.md) ·
[العربية](README.ar.md) ·
[Русский](README.ru.md) ·
[Español](README.es.md) ·
[Deutsch](README.de.md)

</div>

---

## Présentation

NoveLauncher remplace l'écran d'accueil d'Android. Il classe et regroupe vos
applications selon votre usage réel — ce que vous ouvrez, quand et à quelle
fréquence — pour que l'essentiel soit déjà devant vous.

Le classement s'exécute **entièrement sur votre appareil**. Aucun compte,
aucun serveur, aucune analyse d'audience, aucune publicité. Vos données
d'utilisation ne quittent jamais votre téléphone.

## Fonctionnalités

| | |
|---|---|
| 🧠 **Suggestions qui apprennent** | Applications classées par récence, fréquence, heure de la journée et catégorie ; « Recommandés maintenant » apprend aussi des suggestions que vous ouvrez vraiment — tout est calculé localement |
| 📁 **Dossiers intelligents** | Regroupement automatique en 11 catégories, sans tri manuel |
| 💼 **Profil professionnel et Espace privé** | Applis pro avec leur badge et un bouton de mise en pause ; l'Espace privé d'Android 15 dans sa propre section verrouillable, jamais mêlé aux suggestions et masqué de la recherche tant qu'il est verrouillé |
| ⚡ **Raccourcis d'applications** | Appui long sur une appli pour accéder à ses raccourcis — nouveau message, itinéraire enregistré — directement depuis l'écran d'accueil |
| 🔍 **Recherche intelligente** | Applications, contacts et paramètres depuis un seul champ. Correspondance hébreu ↔ alphabet latin (« וואטסאפ » trouve WhatsApp), tolérance aux fautes de frappe et aux initiales (« gm » → Google Maps), plus une calculatrice et la conversion d'unités |
| 🕯️ **Calendrier hébraïque et mode Chabbat** | Date hébraïque, fêtes (calendrier d'Israël ou de la diaspora), heures d'allumage des bougies et de Havdala calculées sur le téléphone ; le mode Chabbat masque les suggestions distrayantes et les badges, de l'allumage des bougies à la tombée de la nuit |
| 🖼️ **Fonds d'écran** | Vos propres photos ou des dégradés intégrés — un pour tout, par page, par heure, par jour de la semaine, ou le fond d'écran du système (animé compris) — avec luminosité, contraste, saturation, chaleur, flou et vignettage |
| 👆 **Gestes** | Balayer vers le haut pour les applis, vers le bas pour les notifications, double toucher pour éteindre l'écran |
| 🎨 **Personnalisation poussée** | 12 palettes, Material You, couleurs personnalisées, 16 polices, 12 formes d'icônes |
| 🧷 **Packs d'icônes** | Appliquez n'importe quel pack d'icônes compatible Nova / ADW / Lawnchair ; les applis qu'il ne couvre pas gardent leur icône |
| 🔒 **Verrouillage d'applications** | Code PIN, mot de passe, schéma ou biométrie par application, dossier privé et applications masquées |
| 🌍 **7 langues** | Hébreu, anglais, arabe, français, russe, espagnol, allemand — entièrement compatible RTL |
| 📰 **Fil d'actualités** | Sources intégrées et vos propres flux RSS |
| 💾 **Sauvegardes** | En local ou sur votre NAS/WebDAV, planifiées, avec chiffrement par mot de passe en option |
| 📊 **Statistiques d'usage** | Temps d'écran et utilisation par jour, semaine ou mois, lus depuis Android sur votre appareil |
| 🧩 **Widgets et dock** | Widgets Android standard (avec leur écran de configuration, redimensionnables) et dock inférieur épinglé |

## Captures d'écran

<div align="center">

<img src="../../assets/screenshots/google-play/01-home.png" width="24%" alt="Écran d'accueil">
<img src="../../assets/screenshots/google-play/04-apps-profiles.png" width="24%" alt="Tiroir d'applications">
<img src="../../assets/screenshots/google-play/07-themes.png" width="24%" alt="Thèmes">
<img src="../../assets/screenshots/google-play/08-security.png" width="24%" alt="Sécurité">

</div>

## Confidentialité

C'est la raison d'être du projet, autant être précis :

- **Rien n'est collecté.** Aucune analyse d'audience, aucune télémétrie,
  aucun rapport de plantage dans les versions distribuées, aucun compte,
  aucune publicité.
- **Les données d'usage restent locales.** L'utilisation des applis est lue
  dans les statistiques d'usage d'Android et traitée sur votre appareil ; ce que
  les suggestions apprennent est conservé dans une base de données locale. Rien
  n'est envoyé.
- **Quatre fonctions utilisent le réseau — rien d'autre :**
  - **Météo** — demande les conditions actuelles à
    [Open-Meteo](https://open-meteo.com). Vos coordonnées approximatives ne sont
    envoyées que si vous accordez la permission de localisation ; sinon, c'est
    la météo d'une ville par défaut qui est demandée.
  - **Actualités** — la page Actualités récupère les flux RSS que vous avez
    activés, directement auprès de chaque éditeur.
  - **Sauvegardes** — seulement si vous choisissez un NAS : votre propre
    serveur, en HTTPS.
  - **Recherche de mises à jour** — désactivée par défaut. Une fois activée,
    l'appli interroge l'API des versions de GitHub au plus une fois par jour.
    Rien sur vous ni sur votre appareil n'est envoyé, et rien n'est téléchargé
    automatiquement.

  Tout le reste reste sur votre appareil.
- **Vos sauvegardes vous appartiennent.** Sur le téléphone ou sur votre NAS,
  avec un chiffrement par mot de passe que vous seul détenez.
- **Le calendrier hébraïque est calculé sur le téléphone.** Les heures des
  fêtes, de l'allumage des bougies et de la Havdala utilisent votre position
  approximative si vous l'autorisez (une ville par défaut sinon), et ne sont
  envoyées nulle part.
- **L'assistant de gestes ne lit rien.** Le double toucher pour éteindre l'écran
  passe par un service d'accessibilité facultatif qui effectue exactement deux
  actions (éteindre l'écran, ouvrir les notifications) et ne peut pas voir ce
  qui s'affiche.

Détail complet : [**Politique de confidentialité**](https://drummingbird1.github.io/NoveLauncher/privacy.html) ·
[Conditions d'utilisation](https://drummingbird1.github.io/NoveLauncher/terms.html)

> **À propos du verrouillage :** le verrouillage d'applications, le dossier
> privé et les applications masquées sont **dissuasifs, pas un bac à sable
> sécurisé**. Une application « verrouillée » reste accessible via les
> applications récentes, les notifications, un autre launcher ou ADB — c'est
> vrai de tout launcher tiers. Pour une isolation réelle, utilisez le profil
> professionnel intégré à Android ou l'Espace privé (Android 15 et plus) —
> NoveLauncher prend en charge les deux.

## Téléchargement

Récupérez l'APK signé depuis la [**dernière version**](https://github.com/DrummingBird1/NoveLauncher/releases/latest).

Vous devrez autoriser l'installation depuis des sources inconnues. Toutes les
versions sont signées avec la même clé, les mises à jour s'installent donc
proprement les unes sur les autres.

<a href="https://github.com/ImranR98/Obtainium"><img src="../assets/badge-obtainium.png" alt="Get it on Obtainium" height="62"></a>

**Mises à jour automatiques :** [Obtainium](https://github.com/ImranR98/Obtainium) installe NoveLauncher
directement depuis ces versions GitHub et le tient à jour. Dans Obtainium,
ajoutez une application et collez `https://github.com/DrummingBird1/NoveLauncher`.

### Configuration requise

- Android 8.0 (API 26) ou plus récent
- Environ 25 Mo de stockage

Certaines fonctions demandent davantage : l'Espace privé nécessite Android 15,
et le double toucher pour éteindre l'écran Android 9. L'Espace privé, les
raccourcis d'applications et la mise en pause des applis pro ne fonctionnent que
si NoveLauncher est votre application d'accueil par défaut.

## Compiler depuis les sources

```bash
git clone https://github.com/DrummingBird1/NoveLauncher.git
cd NoveLauncher/android
./gradlew assembleDebug
```

Ouvrez le dossier **`android/`** dans une version d'Android Studio compatible
avec l'Android Gradle Plugin 9.4 — pas la racine du dépôt, qui n'est pas un
projet Gradle. Gradle a besoin du **JDK 21** (à définir comme Gradle JDK dans
Android Studio) ; l'application elle-même est compilée en bytecode Java 17.

## Contribuer

Les issues et pull requests sont les bienvenues — voir
[CONTRIBUTING.md](../../CONTRIBUTING.md). Pour tout problème de sécurité,
lisez [SECURITY.md](../../SECURITY.md) et signalez-le en privé.

## Soutenir le projet

NoveLauncher est gratuit, sans publicité et sans achats intégrés. Pour
soutenir le développement :

<div align="center">

[![Patreon](https://img.shields.io/badge/Patreon-Soutenir-FF424D?style=for-the-badge&logo=patreon&logoColor=white)](https://www.patreon.com/c/Solvaris)
[![Buy Me a Coffee](https://img.shields.io/badge/Buy%20Me%20a%20Coffee-Soutenir-FFDD00?style=for-the-badge&logo=buymeacoffee&logoColor=black)](https://buymeacoffee.com/solvaris)

</div>

## Contact

Questions, bugs ou suggestions : **solvaris2@gmail.com** ou
[ouvrez une issue](https://github.com/DrummingBird1/NoveLauncher/issues).

## Licence

Voir [LICENSE](../../LICENSE). Le code est publié à des fins de transparence
et de revue — il n'est pas sous licence open source.
