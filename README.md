# AF Wind Widget

![AF Wind Widget](docs/screenshot.png)

Fork personnel de [AF Weather Widget](https://github.com/Gitsaibot/AF-Weather-Widget) : le même widget météo graphique pour Android, avec en plus le **vent moyen et les rafales** (km/h), et les prévisions des **modèles Météo-France** (AROME puis ARPEGE) via [Open-Meteo](https://open-meteo.com/).

## Ce qui change par rapport à l'original

- **Source météo** : Open-Meteo, modèles Météo-France (`meteofrance_seamless`) — gratuit, sans compte ni clé. MET Norway et NOAA restent disponibles dans les réglages.
- **Vent** : courbe du vent moyen (trait plein) et des rafales (pointillés), échelle en km/h à droite du graphe.
- **Seuil de vent fort** : au-dessus du seuil (40 km/h par défaut), les courbes changent de couleur et une ligne pointillée marque le seuil.
- **Ligne du haut** : le vent actuel (« Vent 18 km/h (raf. 35) ») remplace la pression (MSLP).
- **Nouveaux réglages** (Apparence → Vent) : afficher le vent, seuil, couleur du vent moyen, des rafales et au-dessus du seuil.
- **Recherche de lieux et fuseaux horaires** : via Open-Meteo au lieu de GeoNames (plus besoin de compte GeoNames).
- L'appli s'appelle « AF Wind » (`net.auborddulit.afwind`) et peut être installée à côté de l'original.

## Installer sur le téléphone

À chaque modification, GitHub fabrique automatiquement l'APK : voir la page [Releases](https://github.com/auborddulit/AF-Wind-Widget/releases), télécharger le fichier `.apk` de la dernière version depuis le téléphone et l'ouvrir. Les nouvelles versions s'installent par-dessus l'ancienne en gardant les réglages.

## Remerciements et licences

- Code d'origine : [AF Weather Widget](https://github.com/Gitsaibot/AF-Weather-Widget) par Gitsaibot, lui-même dérivé de aix weather widget — licence [CC0](https://creativecommons.org/publicdomain/zero/1.0/) (sauf `MultiKey.java` et `Pair.java`, Apache 2.0).
- Données météo : [Open-Meteo](https://open-meteo.com/) (licence [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/)), à partir des modèles de [Météo-France](https://meteofrance.com/).
- Lever/coucher du soleil et de la lune : [Institut météorologique norvégien (MET Norway)](https://api.met.no/).
- Icônes météo : propriété de MET Norway, via leur [API weathericon](https://api.met.no/weatherapi/weathericon/2.0/documentation).
- Icône de l'application : [bharathp666 (DeviantArt)](http://bharathp666.deviantart.com/).
