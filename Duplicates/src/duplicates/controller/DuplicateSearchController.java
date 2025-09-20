package duplicates.controller;

import duplicates.model.DuplicateSearchModel;
import duplicates.model.DuplicateSearchOptionsModel;

import java.io.File;
import java.time.LocalDate;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

/**
 * Controller für die Duplikat-Suche.
 * Liest Dateien aus, filtert nach Optionen, gruppiert sie nach Größe + Hash und meldet Duplikate zurück.
 */
public class DuplicateSearchController {

    // Abbruch-Flag
    private volatile boolean cancelled = false;

    /**
     * Startet eine Duplikat-Suche.
     *
     * @param options         Suchoptionen (Min/Max-Größe, Filter)
     * @param selectedFolders Liste der zu durchsuchenden Ordner
     * @param resultConsumer  Callback: erhält (Datei, Gruppen-ID) für jedes gefundene Duplikat
     * @param progressUpdater Callback: erhält Fortschritt (0–100)
     */
    public void searchDuplicates(DuplicateSearchOptionsModel options,
                                 List<String> selectedFolders,
                                 BiConsumer<DuplicateSearchModel, Integer> resultConsumer,
                                 IntConsumer progressUpdater) {

        cancelled = false; // Reset bei jedem Start

        // --- 1) Dateien aus den ausgewählten Ordnern sammeln ---
        List<File> allFiles = collectFiles(selectedFolders, options.isSubFolderBoo());

        // --- 2) Optional: nach Min/Max-Größe filtern (in MB) ---
        if (options.getMinFileSize() > 0 || options.getMaxFileSize() > 0) {
            allFiles.removeIf(f -> {
                if (cancelled) return true;
                double sizeMB = f.length();
                if (options.getMinFileSize() > 0 && sizeMB < options.getMinFileSize()) return true;
                if (options.getMaxFileSize() > 0 && sizeMB > options.getMaxFileSize()) return true;
                return false;
            });
        }

        // --- 3) Optional: nach Erstell-/Änderungsdatum filtern ---
        if (options.getCreationDate() != null || options.getModificationDate() != null) {
            allFiles.removeIf(f -> {
                if (cancelled) return true;

                DuplicateSearchModel m = new DuplicateSearchModel(f);

                // Erstellungsdatum prüfen
                LocalDate filterCreated = options.getCreationDate();
                if (filterCreated != null) {
                    LocalDate fileCreated = m.getCreationDate(); // bereits LocalDate
                    if (fileCreated == null) return true; // wenn Filter gesetzt & kein Datum vorhanden -> raus

                    String op = safe(options.getFileCreationDateOperator());
                    switch (op) {
                        case "<"  -> { if (!fileCreated.isBefore(filterCreated)) return true; }
                        case "<=" -> { if ( fileCreated.isAfter(filterCreated)) return true; }
                        case "="  -> { if (!fileCreated.isEqual(filterCreated)) return true; }
                        case ">=" -> { if ( fileCreated.isBefore(filterCreated)) return true; }
                        case ">"  -> { if (!fileCreated.isAfter(filterCreated)) return true; }
                        default   -> { /* kein Operator -> kein Filter */ }
                    }
                }

                // Änderungsdatum prüfen
                LocalDate filterModified = options.getModificationDate();
                if (filterModified != null) {
                    LocalDate fileModified = m.getModificationDate(); // bereits LocalDate
                    if (fileModified == null) return true;

                    String op = safe(options.getFileModificationDateOperator());
                    switch (op) {
                        case "<"  -> { if (!fileModified.isBefore(filterModified)) return true; }
                        case "<=" -> { if ( fileModified.isAfter(filterModified)) return true; }
                        case "="  -> { if (!fileModified.isEqual(filterModified)) return true; }
                        case ">=" -> { if ( fileModified.isBefore(filterModified)) return true; }
                        case ">"  -> { if (!fileModified.isAfter(filterModified)) return true; }
                        default   -> { /* kein Operator -> kein Filter */ }
                    }
                }

                return false;
            });
        }

        int total = allFiles.size();
        int processed = 0;
        int groupId = 1;

        // --- 4) Gruppieren nach Dateigröße ---
        Map<Long, List<File>> sizeGroups = new HashMap<>();
        for (File file : allFiles) {
            if (cancelled) return;
            sizeGroups.computeIfAbsent(file.length(), k -> new ArrayList<>()).add(file);
        }

        // --- 5) Jede Größen-Gruppe auf Hash-Duplikate prüfen ---
        for (List<File> group : sizeGroups.values()) {
            if (cancelled) return;

            if (group.size() < 2) {
                processed += group.size();
                updateProgress(progressUpdater, processed, total);
                continue;
            }

            Map<String, List<DuplicateSearchModel>> hashGroups = new HashMap<>();
            for (File file : group) {
                if (cancelled) return;
                DuplicateSearchModel model = new DuplicateSearchModel(file);
                String hash = model.getFileHash();
                hashGroups.computeIfAbsent(hash, k -> new ArrayList<>()).add(model);
                processed++;
                updateProgress(progressUpdater, processed, total);
            }

            // --- 6) Nur Gruppen mit echten Duplikaten melden ---
            for (List<DuplicateSearchModel> duplicates : hashGroups.values()) {
                if (cancelled) return;

                if (duplicates.size() > 1) {
                    for (DuplicateSearchModel model : duplicates) {
                        resultConsumer.accept(model, groupId);
                        if (cancelled) return;
                    }
                    groupId++;
                }
            }
        }
    }

    /**
     * Bricht eine laufende Suche ab.
     */
    public void cancel() {
        this.cancelled = true;
    }

    // --- Hilfsmethoden ---

    private List<File> collectFiles(List<String> roots, boolean includeSubfolders) {
        List<File> result = new ArrayList<>();
        for (String rootPath : roots) {
            if (cancelled) return result;
            File root = new File(rootPath);
            if (root.exists() && root.isDirectory()) {
                collectRecursive(root, result, includeSubfolders);
            }
        }
        return result;
    }

    private void collectRecursive(File folder, List<File> result, boolean includeSubfolders) {
        if (cancelled) return;
        File[] files = folder.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (cancelled) return;
            if (f.isFile()) {
                result.add(f);
            } else if (f.isDirectory() && includeSubfolders) {
                collectRecursive(f, result, true);
            }
        }
    }

    private void updateProgress(IntConsumer progressUpdater, int processed, int total) {
        if (total > 0) {
            int percent = (int) ((processed / (double) total) * 100);
            progressUpdater.accept(percent);
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
