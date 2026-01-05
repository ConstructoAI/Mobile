# Constructo AI - Application Android

Application Android native pour Constructo AI, un ERP intelligent pour la construction au Québec.

## Prérequis

- Android Studio Hedgehog (2023.1.1) ou plus récent
- JDK 17 ou plus récent
- SDK Android API 34 (Android 14)

## Installation

1. Ouvrez Android Studio
2. Sélectionnez "Open an existing project"
3. Naviguez vers ce dossier et ouvrez-le
4. Attendez que Gradle synchronise les dépendances

## Compilation

### Debug APK
```
Build > Build Bundle(s) / APK(s) > Build APK(s)
```
L'APK sera généré dans `app/build/outputs/apk/debug/`

### Release APK (signé)
1. Créez un keystore de signature :
   ```
   Build > Generate Signed Bundle / APK
   ```
2. Suivez les instructions pour créer ou utiliser un keystore
3. L'APK signé sera généré dans `app/build/outputs/apk/release/`

## Fonctionnalités

- WebView optimisée chargeant constructoai.ca
- Splash screen avec logo
- Pull-to-refresh pour recharger
- Gestion du bouton retour (navigation WebView)
- Support caméra, microphone et géolocalisation
- Upload de fichiers depuis la galerie ou caméra
- Mode plein écran pour les vidéos
- Écran d'erreur en cas de problème de connexion
- Deep links vers constructoai.ca

## Configuration

- **Package** : ca.constructoai.app
- **Min SDK** : 24 (Android 7.0)
- **Target SDK** : 34 (Android 14)
- **Version** : 1.0.0

## Structure du projet

```
ConstructoAI-Android/
├── app/
│   ├── src/main/
│   │   ├── java/ca/constructoai/app/
│   │   │   └── MainActivity.java
│   │   ├── res/
│   │   │   ├── layout/activity_main.xml
│   │   │   ├── values/(colors, strings, themes)
│   │   │   ├── drawable/(boutons, icônes)
│   │   │   ├── mipmap-*/(icônes app)
│   │   │   └── xml/(config réseau, file paths)
│   │   └── AndroidManifest.xml
│   ├── build.gradle
│   └── proguard-rules.pro
├── gradle/
├── build.gradle
├── settings.gradle
└── gradle.properties
```
