package duplicates.controller;

import java.io.File;
import java.util.*;

public class FileAccessController {

    /**
     * Liefert alle Root-Verzeichnisse des Systems.
     * Unter Windows: C:\, D:\, ...
     * Unter Linux/Mac: /
     */
    public File[] getRootFolders() {
        return File.listRoots(); // liefert File[]
    }

    /**
     * Liefert alle Unterordner eines gegebenen Verzeichnisses.
     */
    public File[] getSubFolders(File folder) {
        if (folder != null && folder.isDirectory() && folder.canRead()) {
            File[] files = folder.listFiles(File::isDirectory);
            return files != null ? files : new File[0];
        }
        return new File[0];
    }
    
    /**
     * Findet Dateiduplikate in den angegebenen Ordnern.
     * 
     * @param folderList           Liste der Startordner
     * @param minFileSize          minimale Dateigröße in MB (0 = egal)
     * @param maxFileSize          maximale Dateigröße in MB (0 = keine Grenze)
     * @param fileSize             true = Dateigröße berücksichtigen
     * @param fileName             true = Dateiname berücksichtigen
     * @param subFolder            true = Unterordner mit durchsuchen
     * @param fileExtensionBoolean true = nur bestimmte Dateiendungen erlauben
     * @param fileExtensionList    Liste der zulässigen Endungen (z.B. {"jpg","png"})
     * @return Liste von DuplicateGroup-Objekten, jede Gruppe enthält alle gefundenen Duplikate
     */
    public List<DuplicateGroup> getDuplicateFiles(File[] folderList,
                                                  double minFileSize,
                                                  double maxFileSize,
                                                  boolean fileSize,
                                                  boolean fileName,
                                                  boolean subFolder,
                                                  boolean fileExtensionBoolean,
                                                  String[] fileExtensionList) {

        List<File> allFiles = new ArrayList<>();
        for (File folder : folderList) {
            collectFiles(folder, allFiles, subFolder);
        }

        // --- Extensions in Set umwandeln (schneller Lookup) ---
        Set<String> allowedExtensions = new HashSet<>();
        if (fileExtensionBoolean && fileExtensionList != null) {
            for (String ext : fileExtensionList) {
                allowedExtensions.add(ext.toLowerCase());
            }
        }

        // --- Filterung nach Größe und Dateiendung ---
        List<File> filteredFiles = new ArrayList<>();
        for (File file : allFiles) {
            if (!file.isFile()) continue;

            // Dateiendung prüfen
            if (fileExtensionBoolean) {
                String name = file.getName();
                int dotIndex = name.lastIndexOf('.');
                if (dotIndex < 0) continue; // keine Endung -> ignorieren
                String ext = name.substring(dotIndex + 1).toLowerCase();
                if (!allowedExtensions.contains(ext)) continue;
            }

            // Größe prüfen
            double sizeMB = file.length() / (1024.0 * 1024.0);
            if (sizeMB < minFileSize) continue;
            if (maxFileSize > 0 && sizeMB > maxFileSize) continue;

            filteredFiles.add(file);
        }

        // --- Gruppierung ---
        Map<String, DuplicateGroup> groups = new HashMap<>();
        for (File file : filteredFiles) {
            String key = buildKey(file, fileSize, fileName, fileExtensionBoolean);
            groups.computeIfAbsent(key, DuplicateGroup::new).addFile(file);
        }

        // --- Nur Gruppen mit mehr als 1 Datei behalten ---
        List<DuplicateGroup> result = new ArrayList<>();
        for (DuplicateGroup group : groups.values()) {
            if (group.size() > 1) {
                result.add(group);
            }
        }

        return result;
    }

    /** Hilfsmethode: rekursives Sammeln aller Dateien */
    private void collectFiles(File folder, List<File> list, boolean includeSubfolders) {
        if (folder == null || !folder.exists()) return;
        File[] files = folder.listFiles();
        if (files == null) return;

        for (File f : files) {
            if (f.isDirectory() && includeSubfolders) {
                collectFiles(f, list, true);
            } else if (f.isFile()) {
                list.add(f);
            }
        }
    }

    /**
     * Erstellt einen Gruppierungsschlüssel auf Basis der gewählten Kriterien.
     * 
     * Wenn nur Dateigröße relevant ist, entsteht z.B. "SIZE=12345"
     * Wenn Name+Größe relevant sind, "NAME=bild.jpg|SIZE=12345"
     */
    private String buildKey(File file, boolean useSize, boolean useName, boolean useExt) {
        StringBuilder key = new StringBuilder();

        if (useName) {
            key.append("NAME=").append(file.getName());
        }
        if (useExt) {
            int dot = file.getName().lastIndexOf('.');
            String ext = (dot >= 0) ? file.getName().substring(dot + 1) : "";
            key.append("|EXT=").append(ext.toLowerCase());
        }
        if (useSize) {
            key.append("|SIZE=").append(file.length());
        }

        // Falls nichts zum Vergleich aktiviert ist, wird der Pfad genommen (damit keine falschen Gruppen entstehen)
        if (key.length() == 0) {
            key.append(file.getAbsolutePath());
        }
        return key.toString();
    }
}
