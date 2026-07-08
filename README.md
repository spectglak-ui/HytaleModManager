<img width="693" height="693" alt="Icon HMM" src="https://github.com/user-attachments/assets/71299bec-6a3d-4190-85e5-6e01812b882b" />
# Hytale Mod Manager

Application de bureau Java/JavaFX permettant la mise à jour automatique du
champ `ServerVersion` des fichiers `manifest.json` des mods Hytale.

**Version : 1.1.0**

---

## Sommaire

- [Fonctionnalités](#fonctionnalités)
- [Prérequis](#prérequis)
- [Lancer l'application](#lancer-lapplication)
- [Distribution Windows — Créer un installateur](#distribution-windows--créer-un-installateur)
- [Support multilingue](#support-multilingue)
- [Architecture du projet](#architecture-du-projet)
- [Règles de mise à jour du champ ServerVersion](#règles-de-mise-à-jour-du-champ-serverversion)
- [Sauvegarde du mod mis à jour](#sauvegarde-du-mod-mis-à-jour)
- [Formats supportés](#formats-supportés)
- [Pistes d'extension futures](#pistes-dextension-futures)

---

## Fonctionnalités

- Import de mods : dossier, `.zip`, `.jar`, `.rar` (glisser-déposer ou boutons).
- Détection automatique du `manifest.json`, y compris dans les sous-dossiers.
- Mise à jour du champ `ServerVersion`, mod par mod ou en lot.
- Export du mod mis à jour en `.zip` ou `.jar`.
- Page « Gestion des versions » : CRUD, version recommandée, import/export JSON.
- Filtre/recherche en temps réel sur le tableau des mods.
- Menu contextuel clic droit sur chaque mod.
- Progression réelle lors des mises à jour en lot.
- Journal des opérations (interface + fichier `journal.log`).
- Sauvegarde automatique du choix de langue entre les sessions.
- **Interface entièrement traduite en français et en anglais** — changement sans redémarrage.
- Icône personnalisée sur la fenêtre, la barre des tâches et le raccourci Windows.
- Installateur Windows natif (`.msi` ou `.exe`) via `build.bat`.

---

## Prérequis

| Outil | Version minimale | Utilité |
|---|---|---|
| JDK | 17 | Compilation, `jpackage`, exécution |
| Maven | 3.8 | Build (`mvn package`, `mvn javafx:run`) |
| WiX Toolset | 3.x | Format `.msi` uniquement ([wixtoolset.org](https://wixtoolset.org)) |
| 7-Zip ou WinRAR | toute | Import `.rar` (optionnel) |

---

## Lancer l'application

```bash
# Depuis les sources (développement)
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

Le script :
1. Compile le projet via Maven.
2. Crée `target/hytale-mod-manager.jar` (fat-jar autonome).
3. Invoque `jpackage` pour produire `target/installer/Hytale_Mod_Manager-1.1.0.msi`.

L'installateur Windows crée :
- Un **raccourci sur le bureau**.
- Une entrée dans le **menu Démarrer** (groupe *Hytale Mod Manager*).
- Une entrée dans **Applications installées** pour une désinstallation propre.

### Format EXE (sans WiX Toolset)

Si WiX Toolset n'est pas installé :
```bat
build.bat exe
```

### Personnaliser avant de distribuer

Ouvrir `pom.xml` et `build.bat`, puis modifier :

```xml
<app.version>1.1.0</app.version>
<app.publisher>VotreNom</app.publisher>
```

Modifier aussi `BuildInfo.java` :
```java
public static final String VERSION   = "1.1.0";
public static final String PUBLISHER = "VotreNom";
```

> **Important :** ne pas changer `<winUpgradeUuid>` une fois l'application
> distribuée — cet UUID permet à Windows de reconnaître les mises à jour
> comme des *upgrades* de la même application, et non des installations séparées.

---

## Support multilingue

### Langues disponibles

| Langue | Code | Fichier |
|---|---|---|
| Français | `fr` | `messages_fr.properties` |
| English | `en` | `messages_en.properties` |

**Détection automatique** : au premier lancement, la langue du système
d'exploitation est détectée. Le choix est sauvegardé dans
`%APPDATA%\HytaleModManager\settings.json`.

**Changement à chaud** : menu **Paramètres → Langue** — aucun redémarrage requis.
Tous les menus, boutons, messages d'erreur, journal et dialogues se mettent à
jour instantanément.

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
4. Dans `MainController.java`, ajouter l'entrée de menu correspondante
   (un `RadioMenuItem` dans `menuLanguage` et son handler `onSetLangXx()`).

**Aucune modification du code métier n'est nécessaire.**

---

## Architecture du projet

```
com.hytale.modmanager
├── Main / MainApp           Point d'entrée et bootstrap JavaFX
├── BuildInfo                Version et éditeur (modifiez ici avant release)
├── model/
│   ├── HytaleVersion        Représentation d'une version Hytale
│   ├── ModEntry             Un mod importé (propriétés JavaFX pour le tableau)
│   ├── UpdateReport         Rapport de mise à jour (simple ou par lots)
│   └── UpdateStatus         Enum des statuts (traduits via I18n)
├── json/                    Mini-moteur JSON interne (sans dépendance externe)
│   ├── JsonParser
│   └── JsonWriter
├── service/
│   ├── AppSettings          Persistance des paramètres (langue…)
│   ├── ManifestService      Détection et lecture des manifest.json
│   ├── ManifestUpdater      Les 3 règles de mise à jour de ServerVersion
│   ├── ArchiveExtractor     Import dossier / ZIP / JAR / RAR + export ZIP/JAR
│   ├── VersionManager       CRUD + persistance de la liste des versions
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

---

## Règles de mise à jour du champ ServerVersion

1. **Champ existant avec valeur** → seul le contenu entre guillemets est
   remplacé ; le reste du fichier est préservé à l'identique.
2. **Champ existant vide** → rempli avec la nouvelle valeur.
3. **Champ absent** → créé après le champ `Version`, avec détection et
   préservation de l'indentation (espaces ou tabulations) et des fins de
   ligne (LF/CRLF) du fichier d'origine.

---

## Sauvegarde du mod mis à jour

Pour les mods importés via une **archive** (`.zip`, `.jar`, `.rar`),
la mise à jour s'effectue dans un dossier temporaire interne — le fichier
d'origine n'est jamais modifié automatiquement.

Utilisez le bouton **« Exporter le mod »** après la mise à jour pour
récupérer l'archive mise à jour. La colonne **« Sauvegarde »** du tableau
affiche un avertissement ⚠ tant que l'export n'est pas effectué.

Pour les mods importés depuis un **dossier**, la modification s'écrit
directement dedans — aucun export nécessaire.

---

## Formats supportés

| Format | Import | Export | Remarque |
|---|---|---|---|
| Dossier | ✅ | — (modification directe) | |
| `.zip` | ✅ | ✅ | Format natif Java |
| `.jar` | ✅ | ✅ | Format ZIP renommé |
| `.rar` | ✅ | `.zip` | Extraction via 7-Zip ou WinRAR |

---

## Pistes d'extension futures

- **Vérification en ligne des versions** Hytale (API officielle quand elle sera disponible).
- **Détection automatique des incompatibilités** entre mods dépendants.
- **Gestion de packs de mods** (groupe de mods mis à jour ensemble).
- **Espagnol, Allemand, Italien** : ajouter un fichier `.properties` + une entrée `SUPPORTED_LOCALES`.
- **Thème clair** alternatif.
