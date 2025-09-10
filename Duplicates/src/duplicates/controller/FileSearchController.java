package duplicates.controller;

import duplicates.model.FileSearchModel;
import duplicates.model.FileSearchOptionsModel;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Controller für die Dateisuche.
 * Führt eine rekursive Suche durch und filtert nach den gewählten Optionen.
 */
public class FileSearchController {

    /**
     * Startet eine Dateisuche basierend auf den übergebenen Optionen.
     *
     * @param options         Suchoptionen
     * @param resultConsumer  Callback für jedes gefundene Ergebnis
     * @param progressUpdater Callback für Fortschritt (0–100)
     * @return Liste aller gefundenen Dateien (falls vollständige Liste benötigt wird)
     */
	// --- 1. Methode anpassen: mehrere Start-Ordner
	public List<FileSearchModel> searchFiles(List<String> selectedFolders,
	                                         FileSearchOptionsModel options,
	                                         Consumer<FileSearchModel> resultConsumer,
	                                         IntConsumer progressUpdater) {

	    List<File> allFiles = new ArrayList<>();

	    // --- 1.1 Ordner iterieren und Dateien sammeln
	    for (String folderPath : selectedFolders) {
	        File root = new File(folderPath);
	        if (root.exists() && root.isDirectory()) {
	            collectFiles(root, allFiles, options.isSubFolderBoo());
	        }
	    }

	    int total = allFiles.size();
	    List<FileSearchModel> results = new ArrayList<>();

	    int counter = 0;
	    for (File file : allFiles) {
	        if (!file.isFile()) continue;

	        if (!matchesOptions(file, options)) {
	            counter++;
	            updateProgress(progressUpdater, counter, total);
	            continue;
	        }

	        FileSearchModel model = new FileSearchModel(file);
	        results.add(model);

	        // Sofort an UI liefern
	        resultConsumer.accept(model);

	        counter++;
	        updateProgress(progressUpdater, counter, total);
	    }
	    return results;
	}


    // --- 2. Dateien rekursiv sammeln ---
    private void collectFiles(File folder, List<File> allFiles, boolean includeSubfolders) {
        File[] files = folder.listFiles();
        if (files == null) return;

        for (File f : files) {
            if (f.isDirectory() && includeSubfolders) {
                collectFiles(f, allFiles, true);
            } else if (f.isFile()) {
                allFiles.add(f);
            }
        }
    }

    // --- 3. Filterlogik ---
    private boolean matchesOptions(File file, FileSearchOptionsModel options) {
        try {
            // Min/Max-Größe prüfen
            double sizeMB = file.length() / (1024.0 * 1024.0);
            if (options.getMinFileSize() > 0 && sizeMB < options.getMinFileSize()) return false;
            if (options.getMaxFileSize() > 0 && sizeMB > options.getMaxFileSize()) return false;

            // Dateiendung prüfen
            if (options.getFileExtention() != null && !options.getFileExtention().isBlank()) {
                String ext = getFileExtension(file.getName());
                String[] allowed = options.getFileExtention().toLowerCase().split("\\s*,\\s*");
                boolean match = false;
                for (String a : allowed) {
                    if (ext.equals(a.replace(".", "").toLowerCase())) {
                        match = true;
                        break;
                    }
                }
                if (!match) return false;
            }

            // Dateiname prüfen
            if (options.getFileNameString() != null && !options.getFileNameString().isBlank()) {
                if (!file.getName().toLowerCase().contains(options.getFileNameString().toLowerCase())) {
                    return false;
                }
            }

            // Erstellungs- und Änderungsdatum prüfen
            BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            LocalDate creationDate = attrs.creationTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate modifiedDate = attrs.lastModifiedTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

            if (options.getCreationDate() != null) {
                if (!compareDate(creationDate, options.getCreationDate(), options.getFileCreationDateOperator())) {
                    return false;
                }
            }

            if (options.getModificationDate() != null) {
                if (!compareDate(modifiedDate, options.getModificationDate(), options.getFileModificationDateOperator())) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            // Im Fehlerfall Datei überspringen
            return false;
        }
    }

    // --- 4. Vergleichslogik für Datumsoperatoren ---
    private boolean compareDate(LocalDate fileDate, LocalDate filterDate, String operator) {
        if (fileDate == null) return false;

        return switch (operator) {
            case "<" -> fileDate.isBefore(filterDate);
            case "<=" -> fileDate.isBefore(filterDate) || fileDate.equals(filterDate);
            case "=" -> fileDate.equals(filterDate);
            case ">=" -> fileDate.isAfter(filterDate) || fileDate.equals(filterDate);
            case ">" -> fileDate.isAfter(filterDate);
            default -> true;
        };
    }

    private void updateProgress(IntConsumer progressUpdater, int current, int total) {
        if (total > 0) {
            int progress = (int) ((current / (double) total) * 100);
            progressUpdater.accept(progress);
        }
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return (lastDot > 0) ? fileName.substring(lastDot + 1) : "";
    }
}
