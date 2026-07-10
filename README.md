<img width="346" height="346" alt="Icon HMM" src="https://github.com/user-attachments/assets/433fb6ff-27d3-49a8-af2f-a6d24f47beb0" />

# Hytale Mod Manager

Application de bureau Java/JavaFX permettant la mise à jour automatique du
champ `ServerVersion` des fichiers `manifest.json` des mods Hytale.


<img width="1919" height="1006" alt="Capture d’écran 2026-07-08 151405" src="https://github.com/user-attachments/assets/ee1e7668-7b80-41a7-b25a-8f6ce95f2c69" />

**Version : 1.1.0**

**Version : 1.2.0**


---

## Sommaire

- [Fonctionnalités](#fonctionnalités)
- [Prérequis](#prérequis)
- [Lancer l'application](#lancer-lapplication)
- [Distribution Windows — Créer un installateur](#distribution-windows--créer-un-installateur)
- [Distribution Linux / macOS](#distribution-linux--macos)
- [Support multilingue](#support-multilingue)
- [Architecture du projet](#architecture-du-projet)
- [Règles de mise à jour du champ ServerVersion](#règles-de-mise-à-jour-du-champ-serverversion)
- [Sauvegarde du mod mis à jour](#sauvegarde-du-mod-mis-à-jour)
- [Formats supportés](#formats-supportés)
- [Problème connu](#problème-connu)
- [Documentation technique complète](#documentation-technique-complète)
- [Pistes d'extension futures](#pistes-dextension-futures)

---

## Fonctionnalités

- Import de mods : dossier, `.zip`, `.jar`, `.rar` (glisser-déposer ou boutons).
- Scan automatique du dossier de mods Hytale au démarrage, avec surveillance
  en temps réel (`WatchService`) et rafraîchissement automatique.
- Détection automatique du `manifest.json`, y compris dans les sous-dossiers
  (jusqu'à 4 niveaux de profondeur).
- Mise à jour du champ `ServerVersion`, mod par mod ou en lot, avec barre de
  progression réelle.
- Export du mod mis à jour en `.zip` ou `.jar`.
- Page « Gestion des versions » : CRUD, définition d'une version
  recommandée, import/export JSON.
- Filtre/recherche en temps réel sur le tableau des mods.
- Menu contextuel clic droit sur chaque mod (mettre à jour, exporter,
  actualiser, ouvrir le dossier, retirer).
- Journal des opérations (interface + fichier `journal.log`).
- Sauvegarde automatique du choix de langue et du dossier de mods entre les
  sessions.
- **Interface entièrement traduite en français et en anglais** — changement
  à chaud, sans redémarrage.
- Confirmation avant de fermer l'application, de retirer un mod ou de
  vider la liste si des modifications n'ont pas encore été exportées.
- Icône personnalisée sur la fenêtre, la barre des tâches et le raccourci
  Windows.
- Installateur natif Windows (`.msi` ou `.exe`) via `build.bat`, et paquet
  natif Linux/macOS (`.deb`/`.dmg`) via `build.sh`.

---

## Prérequis

| Outil | Version minimale | Utilité |
|---|---|---|
| JDK | 17 (testé jusqu'à 21 et 25) | Compilation, `jpackage`, exécution |
| Maven | 3.8+ | Build (`mvn package`, `mvn javafx:run`) |
| WiX Toolset | **7** (nécessite `wix eula accept wix7` une fois) | Format `.msi`/`.exe` sous Windows uniquement |
| 7-Zip ou WinRAR | toute version récente | Import `.rar` (optionnel — sinon extraire manuellement) |

> **Correction par rapport à une version antérieure de ce document** : le
> script `build.bat` cible **WiX Toolset 7** (installation via
> `dotnet tool install --global wix`), et non WiX 3.x. WiX 7 impose une
> acceptation unique de licence (`wix eula accept wix7`) avant de pouvoir
> générer un `.msi`/`.exe` ; sans cela, `jpackage` échoue avec le code
> d'erreur `WIX7015` (diagnostic automatiquement affiché par `build.bat`).

---

## Lancer l'application

```bash
# Depuis les sources (développement, sans passer par le fat-jar)
mvn clean javafx:run

# Depuis le fat-jar (après build)
java -jar target/hytale-mod-manager.jar
```

---

## Distribution Windows — Créer un installateur

### En une commande

```bat
build.bat
```

Le script (8 étapes, avec vérifications et diagnostics à chaque étape) :
1. Vérifie Java (≥ 17), `jpackage`, Maven et WiX Toolset 7.
2. Compile le projet via Maven (`mvn clean package`).
3. Valide la présence et la taille de `target/hytale-mod-manager.jar`
   (fat-jar autonome contenant JavaFX et toutes les ressources).
4. Invoque `jpackage` pour produire l'installateur dans
   `target\installer\` (format détecté automatiquement : `.msi` si WiX 7
   est présent, sinon repli sur un dossier `app-image` autonome).

L'installateur Windows (MSI/EXE) crée :
- Un **raccourci sur le bureau**.
- Une entrée dans le **menu Démarrer** (groupe *Hytale Mod Manager*).
- Une entrée dans **Applications installées** pour une désinstallation propre.
- Un identifiant de mise à niveau (`APP_UUID` dans `build.bat`) qui permet à
  Windows de reconnaître les versions futures comme des mises à jour de la
  même application plutôt que des installations séparées.

### Format EXE ou app-image (sans WiX Toolset)

```bat
build.bat exe
build.bat app-image
```

`app-image` produit un dossier autonome (`target\installer\Hytale Mod
Manager\`) sans installateur ni entrée de désinstallation — utile pour
tester rapidement sans installer WiX.

### Personnaliser avant de distribuer

Trois fichiers doivent être tenus synchronisés à chaque changement de
version ou d'éditeur (voir `CLAUDE.md` pour le détail) :

```xml
<!-- pom.xml -->
<app.version>1.2.0</app.version>
<app.publisher>Votre pseudo ici</app.publisher>
```

```bat
:: build.bat
set "APP_VERSION=1.2.0"
set "APP_VENDOR=Votre pseudo ici"
```

```java
// BuildInfo.java
public static final String VERSION   = "1.2.0";
public static final String PUBLISHER = "Votre pseudo ici";
```

> **Important :** ne jamais changer `APP_UUID` dans `build.bat` une fois
> l'application distribuée — cet identifiant permet à Windows de
> reconnaître les mises à jour comme des *upgrades* de la même application,
> et non des installations séparées. (Cette valeur vit uniquement dans
> `build.bat` ; il n'existe pas de propriété équivalente dans `pom.xml`.)

---

## Distribution Linux / macOS

```bash
./build.sh          # .deb sous Linux, .dmg sous macOS (détection automatique via uname)
./build.sh rpm       # ou tout autre format jpackage valide, en argument
```

Le script vérifie la présence de `java`, `javac`, `mvn` et `jpackage`, puis
compile et empaquette en une seule passe.

---

## Support multilingue

### Langues disponibles

| Langue | Code | Fichier | Clés |
|---|---|---|---|
| Français | `fr` | `messages_fr.properties` | 178 |
| English | `en` | `messages_en.properties` | 178 |

Les deux fichiers sont actuellement **strictement synchronisés** (autant de
clés de chaque côté, aucune traduction manquante).

**Détection automatique** : au premier lancement, la langue du système
d'exploitation est détectée. Le choix est sauvegardé dans
`%APPDATA%\HytaleModManager\settings.json` (Windows) ou l'équivalent
`~/Library/Application Support/HytaleModManager` (macOS) /
`~/.config/HytaleModManager` (Linux).

**Changement à chaud** : menu **Paramètres → Langue** — aucun redémarrage
requis. Tous les menus, boutons, messages d'erreur, journal et dialogues se
mettent à jour instantanément.

### Ajouter une nouvelle langue (espagnol, allemand, italien…)

1. Copier `messages_fr.properties` → `messages_es.properties`
   (dans `src/main/resources/com/hytale/modmanager/i18n/`).
2. Traduire toutes les valeurs (ne pas modifier les clés).
3. Dans `I18n.java`, ajouter la locale dans `SUPPORTED_LOCALES` :
   ```java
   public static final List<Locale> SUPPORTED_LOCALES = List.of(
       Locale.FRENCH,
       Locale.ENGLISH,
       new Locale("es")  // ← ajouter ici
   );
   ```
4. Dans `MainController.java` et `main.fxml`, ajouter l'entrée de menu
   correspondante (un `RadioMenuItem` dans `menuLanguage`, son handler
   `onSetLangXx()`, et sa traduction dans `applyTranslations()`).

**Aucune modification du code métier n'est nécessaire.**

---

## Architecture du projet

```
com.hytale.modmanager
├── Main / MainApp           Point d'entrée et bootstrap JavaFX
├── BuildInfo                Version et éditeur (à synchroniser avant release)
├── model/
│   ├── HytaleVersion        Représentation d'une version Hytale
│   ├── ModEntry             Un mod importé (propriétés JavaFX pour le tableau)
│   ├── UpdateReport         Rapport de mise à jour (simple ou par lots)
│   ├── UpdateStatus         Enum des statuts (traduits via I18n)
│   └── VersionUtils         Comparaison de versions de type semver
├── json/                    Mini-moteur JSON interne (sans dépendance externe)
│   ├── JsonParser
│   ├── JsonWriter
│   └── JsonParseException
├── service/
│   ├── AppSettings          Persistance des paramètres (langue, dossier de mods)
│   ├── ManifestService      Détection et lecture des manifest.json
│   ├── ManifestUpdater      Les 3 règles de mise à jour de ServerVersion
│   ├── ManifestUpdateResult Résultat structuré d'une mise à jour
│   ├── ArchiveExtractor     Import dossier / ZIP / JAR / RAR + export ZIP/JAR
│   ├── VersionManager       CRUD + persistance de la liste des versions
│   ├── ModsFolderService    Détection, scan et surveillance du dossier de mods
│   └── ModUpdateService     Orchestration import + mise à jour + export
├── ui/
│   ├── MainController       Fenêtre principale (traduite à chaud)
│   └── VersionManagerController  Gestion des versions (traduite à chaud)
└── util/
    ├── I18n                 Moteur de localisation (chargement, hot-swap)
    ├── AppLogger            Journal (interface + fichier)
    ├── ConfigPaths          Chemins de configuration multiplateformes
    └── IndentDetector       Préservation de l'indentation des manifests
```

Pour le détail des flux de données, du modèle de concurrence et des
observations d'architecture : voir **`ARCHITECTURE.md`**.

---

## Règles de mise à jour du champ ServerVersion

1. **Champ existant avec valeur** → seul le contenu entre guillemets est
   remplacé ; le reste du fichier est préservé à l'identique (octet près).
2. **Champ existant vide** → rempli avec la nouvelle valeur.
3. **Champ absent** → créé après le champ `Version`, avec détection et
   préservation de l'indentation (espaces ou tabulations) et des fins de
   ligne (LF/CRLF) du fichier d'origine.

Si la clé `ServerVersion` apparaît plusieurs fois dans le fichier (objet
imbriqué portant le même nom), l'application bascule automatiquement sur
une reconstruction via l'arbre JSON (qui n'agit que sur la clé racine) pour
éviter toute ambiguïté.

---

## Sauvegarde du mod mis à jour

Pour les mods importés via une **archive** (`.zip`, `.jar`, `.rar`),
la mise à jour s'effectue dans un dossier temporaire interne — le fichier
d'origine n'est jamais modifié automatiquement.

Utilisez le bouton **« Exporter le mod »** après la mise à jour pour
récupérer l'archive mise à jour. La colonne **« Sauvegarde »** du tableau
affiche un avertissement ⚠ tant que l'export n'est pas effectué, et
l'application demande confirmation avant de fermer, de retirer un mod ou
de vider la liste si des modifications n'ont pas encore été exportées.

Pour les mods importés depuis un **dossier**, la modification s'écrit
directement dedans — aucun export nécessaire.

---

## Formats supportés

| Format | Import | Export | Remarque |
|---|---|---|---|
| Dossier | ✅ | — (modification directe) | |
| `.zip` | ✅ | ✅ | Format natif Java (`java.util.zip`) |
| `.jar` | ✅ | ✅ | Format ZIP renommé |
| `.rar` | ✅ | `.zip` | Extraction via un outil externe (7-Zip ou WinRAR) déjà installé sur la machine ; ré-export en `.zip` faute de bibliothèque d'écriture RAR en Java |

---

## Problème connu (corrigé)

Le script `build.bat` affichait un message parasite `: était inattendu.`
juste après `BUILD SUCCESSFUL`, bien que le build (compilation, fat-jar,
MSI/EXE) ait toujours été entièrement fonctionnel. La cause a été
identifiée avec certitude (nombre impair de guillemets sur une ligne de
nettoyage de la version Java, désynchronisant le comptage de `cmd.exe`) et
corrigée par une modification d'une seule ligne, sans changement de
comportement. Détail complet de l'analyse et de la correction dans
`CLAUDE.md` (section 5) et `DEVELOPER_GUIDE.md` (section Dépannage).

---

## Documentation technique complète

| Fichier | Contenu |
|---|---|
| `ARCHITECTURE.md` | Cartographie complète des packages, flux de données, modèle de concurrence, dette technique |
| `CHANGELOG.md` | Historique des versions |
| `ROADMAP.md` | Objectifs court/moyen/long terme |
| `DEVELOPER_GUIDE.md` | Guide pratique : mise en place de l'environnement, conventions, dépannage |

---

## Pistes d'extension futures

- **Vérification en ligne des versions** Hytale (API officielle quand elle sera disponible).
- **Détection automatique des incompatibilités** entre mods dépendants.
- **Gestion de packs de mods** (groupe de mods mis à jour ensemble).
- **Espagnol, Allemand, Italien** : ajouter un fichier `.properties` + une entrée `SUPPORTED_LOCALES`.
- **Thème clair** alternatif.

Pour la liste complète et priorisée (court/moyen/long terme), voir
**`ROADMAP.md`**.
