# Projet OS - Simulation d'un pont mobile bidirectionnel (Java)

Projet R3.05 « Programmation Système » - Département Informatique, IUT Grand Ouest Normandie.
Année 2026, M. Khayata.

**Binôme :** Iria DE MEYER & Paul MUNOZ

## Description

Simulation concurrente d'un pont mobile partagé par quatre flux de circulation :

- véhicules terrestres de gauche à droite et de droite à gauche ;
- bateaux du nord vers le sud et du sud vers le nord.

Chaque véhicule et chaque bateau est un thread Java indépendant. Le pont est une ressource
partagée protégée par un verrou (`synchronized` / `wait` / `notifyAll`). Le projet couvre :

- **Partie 2** : les quatre états du pont (`FERME`, `OUVERTURE`, `OUVERT`, `FERMETURE`) et
  leurs transitions ;
- **Partie 3** : les 10 segments du pont ; voitures (1 segment), camions (2 segments),
  convois exceptionnels (4 segments, sens opposé bloqué) ;
- **Partie 4** : file d'attente FIFO commune des bateaux et cycle d'ouverture/fermeture ;
- **Partie 5** : délai minimal d'une minute entre deux ouvertures, géré par un `Timer` ;
- **Partie 6** : véhicules prioritaires (ambulances, pompiers) et bande d'urgence.

## Compilation

Depuis la racine du projet :

```sh
javac -d out -sourcepath src src/Main.java src/Tests/TestConvoi.java src/Tests/TestPrioritaire.java
```

## Exécution

**Important :** lancer la simulation dans un **vrai terminal** (Windows Terminal, cmd,
PowerShell, bash…). La console « Run » d'IntelliJ n'interprète pas les séquences ANSI
utilisées pour l'animation et l'affichage sera illisible.

```sh
java -jar PontMobile.jar
```

Pour relancer la compilation et reconstruire le `.jar` :

```sh
javac -d out -sourcepath src src/Main.java src/Tests/*.java
jar --create --file PontMobile.jar --main-class Main -C out .
```

### Scénarios de test

```sh
java -cp PontMobile.jar Tests.TestConvoi       # partie 3 / convois exceptionnels
java -cp PontMobile.jar Tests.TestPrioritaire  # partie 6 / véhicules prioritaires
```

## Affichage

L'affichage est une animation console (séquences ANSI) qui tient dans un terminal standard
de 80 colonnes sur environ 20 lignes :

- état du pont et blocage éventuel des entrées ;
- décompte avant la prochaine ouverture autorisée (rafraîchi une fois par seconde) ;
- état des véhicules prioritaires ;
- schéma du pont : deux voies de 10 segments, la voie droite→gauche étant dessinée de
  droite à gauche ;
- rivière et file d'attente des bateaux ;
- journal des 5 derniers événements.

L'écran n'est pas entièrement effacé à chaque événement (curseur replacé en haut et fin de
ligne effacée via `\033[K`) afin d'éviter le clignotement.

## Contenu du dossier

| Fichier | Description |
|---|---|
| `src/` | Sources Java (`.java`) |
| `UML.png` | Image du Schéma UML des classes |
| `UML.mdj` | Schéma UML des classes |
| `PontMobile.jar` | Archive exécutable (`java -jar PontMobile.jar`) |
| `parties_non_faites.txt` | Parties non réalisées / simplifiées |
| `README.md` | Ce fichier |
