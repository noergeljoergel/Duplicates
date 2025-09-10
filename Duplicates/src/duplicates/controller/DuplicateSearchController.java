package duplicates.controller;

import duplicates.model.DuplicateSearchModel;
import duplicates.model.DuplicateSearchOptionsModel;

import java.io.File;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

/**
 * Controller für die Duplikat-Suche.
 * Liest Dateien aus, gruppiert sie nach Größe + Hash und meldet Duplikate zurück.
 */
public class DuplicateSearchController {

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

        // --- 1. Dateien aus den ausgewählten Ordnern sammeln ---
        List<File> allFiles = collectFiles(selectedFolders, options.isSubFolderBoo());

        // --- 2. Optional: nach Min/Max-Größe filtern ---
        if (options.getMinFileSize() > 0 || options.getMaxFileSize() > 0) {
            allFiles.removeIf(f -> {
                double sizeMB = f.length() / (1024.0 * 1024.0);
                if (options.getMinFileSize() > 0 && sizeMB < options.getMinFileSize()) return true;
                if (options.getMaxFileSize() > 0 && sizeMB > options.getMaxFileSize()) return true;
                return false;
            });
        }

        int total = allFiles.size();
        int processed = 0;
        int groupId = 1;

        // --- 3. Gruppieren nach Dateigröße ---
        Map<Long, List<File>> sizeGroups = new HashMap<>();
        for (File file : allFiles) {
            sizeGroups.computeIfAbsent(file.length(), k -> new ArrayList<>()).add(file);
        }

        // --- 4. Jede Größen-Gruppe auf Hash-Duplikate prüfen ---
        for (List<File> group : sizeGroups.values()) {
            if (group.size() < 2) {
                processed += group.size();
                updateProgress(progressUpdater, processed, total);
                continue;
            }

            Map<String, List<DuplicateSearchModel>> hashGroups = new HashMap<>();
            for (File file : group) {
                DuplicateSearchModel model = new DuplicateSearchModel(file);
                String hash = model.getFileHash();
                hashGroups.computeIfAbsent(hash, k -> new ArrayList<>()).add(model);
                processed++;
                updateProgress(progressUpdater, processed, total);
            }

            // --- 5. Nur Gruppen mit echten Duplikaten melden ---
            for (List<DuplicateSearchModel> duplicates : hashGroups.values()) {
                if (duplicates.size() > 1) {
                    for (DuplicateSearchModel model : duplicates) {
                        resultConsumer.accept(model, groupId);
                    }
                    groupId++;
                }
            }
        }
    }

    // --- Hilfsmethoden ---
    private List<File> collectFiles(List<String> roots, boolean includeSubfolders) {
        List<File> result = new ArrayList<>();
        for (String rootPath : roots) {
            File root = new File(rootPath);
            if (root.exists() && root.isDirectory()) {
                collectRecursive(root, result, includeSubfolders);
            }
        }
        return result;
    }

    private void collectRecursive(File folder, List<File> result, boolean includeSubfolders) {
        File[] files = folder.listFiles();
        if (files == null) return;
        for (File f : files) {
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
}
