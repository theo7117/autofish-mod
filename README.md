# AutoFish (Fabric 1.21.1)

Mod client qui automatise la pêche.

## Fonctionnement

1. Tiens une canne à pêche et fais un **clic droit** dans le vide (comme pour lancer
   la canne normalement) → la canne est lancée **et** l'AutoFish s'active
   (message dans l'action bar : "AutoFish Activé").
2. Dès que le message *"Tu as 2 secondes pour pêcher ce poisson"* apparaît dans
   l'action bar (au-dessus de la barre d'XP), le mod fait automatiquement le
   clic droit pour toi : le poisson est récupéré, puis la canne est relancée
   automatiquement dans l'eau, en boucle.
3. Pour arrêter : fais un **clic droit** toi-même pendant que la canne est à
   l'eau (comme pour la ramener normalement) → l'AutoFish se désactive
   immédiatement (message "AutoFish Désactivé").

## Build

Prérequis : JDK 21.

Le fichier binaire `gradle/wrapper/gradle-wrapper.jar` n'est pas inclus dans
cet export. Deux options simples :

**Option A (recommandée) : ouvrir le dossier dans IntelliJ IDEA**
IntelliJ (avec le plugin Gradle, installé par défaut) régénère automatiquement
le wrapper au premier import du projet. Ouvre juste le dossier, attends la
synchronisation Gradle, puis lance la tâche `build` (panneau Gradle à droite).

**Option B : ligne de commande, si Gradle est déjà installé sur ta machine**
```bash
gradle wrapper --gradle-version 8.8
./gradlew build
```

Le `.jar` généré se trouve dans `build/libs/autofish-1.0.0.jar` (le jar
`-sources.jar` n'est pas à utiliser, prends celui sans suffixe).

## Installation

1. Installe [Fabric Loader](https://fabricmc.net/use/) pour Minecraft 1.21.1.
2. Installe [Fabric API](https://modrinth.com/mod/fabric-api) (obligatoire,
   même version majeure que le mod) dans `mods/`.
3. Copie `autofish-1.0.0.jar` dans le dossier `mods/` de ton installation
   Minecraft.
4. Lance le jeu avec le profil Fabric.

## Notes techniques / adaptation

- Le mod détecte le message via une **regex tolérante** sur les mots
  "secondes" et "pêcher" (insensible à la casse et aux accents é/ê), donc ça
  doit fonctionner même si le texte exact varie légèrement (serveur avec
  formulation custom). Si ton serveur affiche un message différent, ouvre
  `AutoFishMod.java` et ajuste le `Pattern BITE_PATTERN`.
- Le mod est **client-side only** : il ne fonctionne qu'en cliquant les
  actions que ton propre client peut faire (comme un joueur normal qui
  clique vite), il ne triche pas côté serveur.
- ⚠️ Sur la plupart des serveurs multijoueur, l'utilisation d'un mod
  d'automatisation comme celui-ci peut être considérée comme du cheat/macro
  et entraîner un bannissement si les règles du serveur l'interdisent.
  Vérifie les règles avant de l'utiliser en ligne. Aucun souci en solo/LAN.
