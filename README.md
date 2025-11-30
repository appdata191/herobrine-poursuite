# herobrine-poursuite

Ce projet est une application Java basée sur LibGDX, proposant trois modules :
1. **Jeu Solo (Single Player)**
2. **Jeu Multijoueur (Multiplayer)**
3. **Créateur/Éditeur de cartes**

Le but est de fournir une base de jeu fonctionnelle ainsi qu’un outil de création de niveaux.

---

## 🏁 Lancer l’application

Depuis la racine du projet :

```bash
./gradlew desktop:run
```

**Prérequis :**

- Java 17 ou supérieur
- Gradle (géré automatiquement par `./gradlew`)

## 🎮 Mode Solo (Single Player)
Après avoir choisi Single Player, vous pouvez sélectionner une carte et lancer une partie.

### Contrôles
- `W` / `A` / `S` / `D` : déplacements
- Toucher un Creeper ou un piège → mort instantanée
- Gameplay inspiré de Terraria

### Plaques de pression
- Marcher sur une plaque de pression ouvre une porte.
- Lorsqu’on la quitte, la porte reste ouverte 5 secondes, puis se referme.
- Le joueur gagne en atteignant la fin du niveau.

## 🌐 Mode Multijoueur
Permet de jouer à plusieurs.

### Connexion
1. Choisir **Multiplayer**
2. Entrer l’adresse IP
3. Rejoindre la session

Le multijoueur utilise KryoNet intégré au projet.

## 🛠 Mode Création / Édition de Cartes
Permet de créer ou modifier vos propres niveaux.

### Contrôles de l’éditeur
- `1` / `2` / `3` : sélectionner un item / bloc
- Clic gauche : retirer un bloc
- Clic droit : placer un bloc

Les cartes créées peuvent être testées immédiatement dans le mode solo.

## 📁 Structure du projet
- `desktop/` – point d’entrée de l’application
- `core/` – logique du jeu (entités, moteur, cartes)
- `assets/` – ressources (textures, cartes, données)

## 👥 Auteurs
Projet réalisé dans le cadre du module informatique – INSA Rouen.

Technologies : Java / LibGDX / Gradle.
