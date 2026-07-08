package com.hytale.modmanager.service;

import com.hytale.modmanager.util.ConfigPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Gère l'import d'un mod sous forme de dossier, d'archive ZIP ou d'archive RAR.
 *
 * Le format ZIP est pris en charge nativement via {@code java.util.zip}.
 * Le format RAR n'a pas d'implémentation native en Java : l'extraction s'appuie
 * sur un utilitaire externe déjà présent sur la machine de l'utilisateur
 * (WinRAR's UnRAR.exe ou 7-Zip). Si aucun de ces outils n'est détecté, une
 * exception explicite est levée et l'interface invite l'utilisateur à installer
 * 7-Zip (gratuit) ou à extraire l'archive manuellement avant import.
 */
public class ArchiveExtractor {

    public static class ExtractionException extends Exception {
        public ExtractionException(String message) {
            super(message);
        }

        public ExtractionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Prépare un dossier de mod utilisable à partir du chemin source fourni.
     * Pour un dossier, retourne directement le chemin (aucune copie). Pour une
     * archive, extrait son contenu dans un dossier temporaire dédié et retourne
     * ce dossier ; l'appelant est responsable de signaler que ce dossier doit
     * être nettoyé après usage (voir {@code ModEntry#isFromTemporaryExtraction()}).
     */
    public Path prepareModFolder(Path source) throws ExtractionException, IOException {
        if (Files.isDirectory(source)) {
            return source;
        }
        String fileName = source.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".zip") || fileName.endsWith(".jar")) {
            return extractZip(source);
        }
        if (fileName.endsWith(".rar")) {
            return extractRar(source);
        }
        throw new ExtractionException("Format de fichier non pris en charge : " + fileName
                + ". Formats acceptés : dossier, .zip, .jar, .rar");
    }

    private Path extractZip(Path zipFile) throws IOException, ExtractionException {
        Path target = newTempDir();
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = resolveSafely(target, entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    try (OutputStream os = Files.newOutputStream(entryPath)) {
                        zis.transferTo(os);
                    }
                }
                zis.closeEntry();
            }
        } catch (IOException ex) {
            throw new IOException("Échec de l'extraction de l'archive ZIP " + zipFile.getFileName() + " : " + ex.getMessage(), ex);
        }
        return target;
    }

    private Path extractRar(Path rarFile) throws ExtractionException, IOException {
        Path target = newTempDir();
        String externalTool = findExternalExtractor();
        if (externalTool == null) {
            throw new ExtractionException(
                    "Aucun outil d'extraction RAR n'a été trouvé sur cette machine (7-Zip ou WinRAR). "
                            + "Veuillez installer 7-Zip (gratuit, 7-zip.org) ou extraire l'archive manuellement "
                            + "puis importer le dossier obtenu.");
        }
        try {
            ProcessBuilder pb;
            if (externalTool.toLowerCase().contains("7z")) {
                pb = new ProcessBuilder(externalTool, "x", "-y", "-o" + target.toAbsolutePath(), rarFile.toAbsolutePath().toString());
            } else {
                // UnRAR.exe : x = extraire avec arborescence, -y = confirmer, dossier cible en dernier argument
                pb = new ProcessBuilder(externalTool, "x", "-y", rarFile.toAbsolutePath().toString(), target.toAbsolutePath() + java.io.File.separator);
            }
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output;
            try (InputStream is = process.getInputStream()) {
                output = new String(is.readAllBytes());
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new ExtractionException("L'extraction RAR a échoué (code " + exitCode + ") : " + output);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ExtractionException("Extraction RAR interrompue.", ex);
        }
        return target;
    }

    /**
     * Réempaquette le contenu d'un dossier de mod (potentiellement mis à jour)
     * dans une nouvelle archive ZIP, à l'emplacement choisi par l'utilisateur.
     *
     * Utilisé pour la fonctionnalité « Sauvegarde du mod mis à jour » : lorsqu'un
     * mod a été importé depuis une archive, ses modifications n'ont lieu que dans
     * un dossier d'extraction temporaire — il faut explicitement réempaqueter ce
     * dossier pour récupérer le résultat. Même un mod RAR d'origine est ré-exporté
     * au format ZIP, faute de bibliothèque d'écriture RAR disponible.
     */
    public void exportAsZip(Path modFolder, Path destinationZip) throws IOException {
        if (destinationZip.getParent() != null) {
            Files.createDirectories(destinationZip.getParent());
        }
        try (var zos = new java.util.zip.ZipOutputStream(Files.newOutputStream(destinationZip));
             var walk = Files.walk(modFolder)) {
            for (Path path : (Iterable<Path>) walk.filter(p -> !p.equals(modFolder))::iterator) {
                String entryName = modFolder.relativize(path).toString().replace('\\', '/');
                if (Files.isDirectory(path)) {
                    zos.putNextEntry(new ZipEntry(entryName + "/"));
                    zos.closeEntry();
                } else {
                    zos.putNextEntry(new ZipEntry(entryName));
                    Files.copy(path, zos);
                    zos.closeEntry();
                }
            }
        }
    }

    /** Recherche un exécutable 7z ou UnRAR dans le PATH système (multi-plateforme, best effort). */
    private String findExternalExtractor() {
        String[] candidates = isWindows()
                ? new String[]{"7z.exe", "7za.exe", "UnRAR.exe", "unrar.exe"}
                : new String[]{"7z", "7za", "unrar"};
        for (String candidate : candidates) {
            if (isOnPath(candidate)) {
                return candidate;
            }
        }
        // Emplacements d'installation Windows usuels.
        if (isWindows()) {
            String[] commonPaths = {
                    "C:\\Program Files\\7-Zip\\7z.exe",
                    "C:\\Program Files (x86)\\7-Zip\\7z.exe",
                    "C:\\Program Files\\WinRAR\\UnRAR.exe",
                    "C:\\Program Files (x86)\\WinRAR\\UnRAR.exe"
            };
            for (String p : commonPaths) {
                if (Files.exists(Path.of(p))) {
                    return p;
                }
            }
        }
        return null;
    }

    private boolean isOnPath(String executable) {
        try {
            ProcessBuilder pb = isWindows()
                    ? new ProcessBuilder("where", executable)
                    : new ProcessBuilder("which", executable);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            int code = p.waitFor();
            return code == 0;
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private Path newTempDir() throws IOException {
        Path base = ConfigPaths.extractionTempDir();
        ConfigPaths.ensureExists(base);
        Path target = base.resolve(UUID.randomUUID().toString());
        Files.createDirectories(target);
        return target;
    }

    /**
     * Caractères interdits dans un nom de fichier/dossier Windows (NTFS) :
     * les caractères de contrôle, ainsi que {@code < > : " | ? *}. Le format
     * ZIP n'impose aucune restriction sur les noms d'entrée : une archive
     * créée sous Linux/macOS, ou utilisant une convention de nommage type
     * {@code namespace:ressource} pour ses fichiers de ressources internes
     * (rencontré dans certains mods), peut donc contenir des noms
     * parfaitement valides en tant qu'entrées ZIP mais que Windows refuse de
     * créer sur le disque. On remplace ces caractères par {@code _} au
     * moment de l'extraction plutôt que de faire échouer tout l'import du
     * mod pour un unique fichier de ressource secondaire.
     */
    private static final java.util.regex.Pattern ILLEGAL_FILENAME_CHARS =
            java.util.regex.Pattern.compile("[\\x00-\\x1F<>:\"|?*]");

    /** Assainit chaque segment (séparé par {@code /}) d'un nom d'entrée d'archive. */
    private String sanitizeEntryName(String entryName) {
        String[] segments = entryName.split("/", -1);
        for (int i = 0; i < segments.length; i++) {
            if (segments[i].isEmpty()) {
                continue;
            }
            String sanitized = ILLEGAL_FILENAME_CHARS.matcher(segments[i]).replaceAll("_");
            segments[i] = sanitized;
        }
        return String.join("/", segments);
    }

    /** Empêche les entrées d'archive malveillantes ("zip slip") de sortir du dossier cible. */
    private Path resolveSafely(Path targetDir, String entryName) throws IOException {
        String safeEntryName = sanitizeEntryName(entryName);
        Path resolved = targetDir.resolve(safeEntryName).normalize();
        if (!resolved.startsWith(targetDir)) {
            throw new IOException("Entrée d'archive invalide (tentative d'évasion du dossier cible) : " + entryName);
        }
        return resolved;
    }

    /** Supprime récursivement un dossier d'extraction temporaire. Échec silencieux si verrouillé. */
    public void cleanupQuietly(Path dir) {
        if (dir == null) {
            return;
        }
        try (var walk = Files.walk(dir)) {
            walk.sorted(java.util.Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                            // Suppression best-effort : un fichier verrouillé n'empêche pas l'application de continuer.
                        }
                    });
        } catch (IOException ignored) {
            // Dossier déjà absent ou inaccessible : rien à faire.
        }
    }
}
