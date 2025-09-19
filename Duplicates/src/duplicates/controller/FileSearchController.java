package duplicates.controller;

import duplicates.model.FileSearchModel;
import duplicates.model.FileSearchOptionsModel;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Controller für die Dateisuche.
 * Skalierbare, iterative Traversierung mit Streaming-Ergebnissen, Fortschritt und Abbruch.
 */
public class FileSearchController {

    // --- Steuerung für Async-Varianten / Cancel ---
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Duplicates-FileSearch");
        t.setDaemon(true);
        return t;
    });

    /**
     * Asynchron starten (bequem aus der UI nutzbar).
     * Abbruch via {@link #cancel()}.
     */
    public Future<?> searchFilesAsync(List<String> selectedFolders,
                                      FileSearchOptionsModel options,
                                      Consumer<FileSearchModel> resultConsumer,
                                      IntConsumer progressUpdater) {
        cancelled.set(false);
        return executor.submit(() -> searchFiles(selectedFolders, options, resultConsumer, progressUpdater));
    }

    /** Suche abbrechen. */
    public void cancel() {
        cancelled.set(true);
    }

    /** Executor sauber beenden (z. B. beim App-Shutdown). */
    public void shutdownNow() {
        executor.shutdownNow();
    }

    /**
     * Startet eine Dateisuche (synchron). Für UI: unbedingt im Hintergrund-Thread aufrufen!
     *
     * @param selectedFolders Startordner (Liste)
     * @param options         Suchoptionen
     * @param resultConsumer  Callback für jedes gefundene Ergebnis
     * @param progressUpdater Callback für Fortschritt (0–100)
     */
    public void searchFiles(List<String> selectedFolders,
                            FileSearchOptionsModel options,
                            Consumer<FileSearchModel> resultConsumer,
                            IntConsumer progressUpdater) {

        cancelled.set(false);

        // 1) Gesamtzahl der Dateien bestimmen (für Prozentanzeige)
        int total = countFiles(selectedFolders, options.isSubFolderBoo());
        final int[] counter = {0};
        final int[] lastProgress = {-1};
        final long[] nextUpdateNs = {0L}; // Drosselung für UI-Updates

        updateProgress(progressUpdater, 0, total, lastProgress, nextUpdateNs, true);

        // 2) Iterative Traversierung aller Startordner
        for (String folderPath : selectedFolders) {
            if (cancelled.get()) break;

            File root = new File(folderPath);
            if (root.exists() && root.isDirectory()) {
                collectAndProcessFilesIterative(
                        root,
                        options.isSubFolderBoo(),
                        options,
                        resultConsumer,
                        progressUpdater,
                        counter,
                        total,
                        lastProgress,
                        nextUpdateNs
                );
            }
        }

        if (!cancelled.get()) {
            safeAccept(progressUpdater, 100);
        }
    }

    // --- Iterative Traversierung: robust & cancel-freundlich ---
    private void collectAndProcessFilesIterative(File startFolder,
                                                 boolean includeSubfolders,
                                                 FileSearchOptionsModel options,
                                                 Consumer<FileSearchModel> resultConsumer,
                                                 IntConsumer progressUpdater,
                                                 int[] counter,
                                                 int total,
                                                 int[] lastProgress,
                                                 long[] nextUpdateNs) {

        Deque<File> stack = new ArrayDeque<>();
        stack.push(startFolder);

        while (!stack.isEmpty()) {
            if (cancelled.get()) return;

            File dir = stack.pop();
            File[] entries = safeListFiles(dir);
            if (entries == null) continue;

            for (File f : entries) {
                if (cancelled.get()) return;

                if (f.isDirectory()) {
                    if (includeSubfolders) {
                        stack.push(f);
                    }
                    continue;
                }

                if (f.isFile()) {
                    counter[0]++;

                    // Filter prüfen & ggf. melden
                    if (matchesOptions(f, options)) {
                        safeAccept(resultConsumer, new FileSearchModel(f));
                    }

                    updateProgress(progressUpdater, counter[0], total, lastProgress, nextUpdateNs, false);
                }
            }
        }
    }

    // --- Dateien zählen (für Prozentberechnung), ebenfalls iterativ & cancel-aware ---
    private int countFiles(List<String> selectedFolders, boolean includeSubfolders) {
        int total = 0;

        for (String folderPath : selectedFolders) {
            if (cancelled.get()) break;

            File root = new File(folderPath);
            if (!(root.exists() && root.isDirectory())) continue;

            Deque<File> stack = new ArrayDeque<>();
            stack.push(root);

            while (!stack.isEmpty()) {
                if (cancelled.get()) return total;

                File dir = stack.pop();
                File[] entries = safeListFiles(dir);
                if (entries == null) continue;

                for (File f : entries) {
                    if (cancelled.get()) return total;

                    if (f.isDirectory()) {
                        if (includeSubfolders) stack.push(f);
                    } else if (f.isFile()) {
                        total++;
                    }
                }
            }
        }
        return total;
    }

    private File[] safeListFiles(File dir) {
        try {
            return dir.listFiles();
        } catch (SecurityException | OutOfMemoryError ignored) {
            return null;
        }
    }

    // --- Filterlogik (beibehaltener, aber effizienter Ablauf) ---
    private boolean matchesOptions(File file, FileSearchOptionsModel options) {
        try {
            // 1) Frühe & günstige Checks (ohne teure Attribute):
            // Größe
            if (options.getMinFileSize() > 0 || options.getMaxFileSize() > 0) {
                double sizeMB = file.length() / (1024.0 * 1024.0);
                if (options.getMinFileSize() > 0 && sizeMB < options.getMinFileSize()) return false;
                if (options.getMaxFileSize() > 0 && sizeMB > options.getMaxFileSize()) return false;
            }

            // Dateiendung
            if (options.getFileExtention() != null && !options.getFileExtention().isBlank()) {
                String ext = getFileExtension(file.getName());
                String[] allowed = options.getFileExtention().toLowerCase().split("\\s*,\\s*");
                boolean match = false;
                for (String a : allowed) {
                    String clean = a.replace(".", "").toLowerCase();
                    if (ext.equals(clean)) {
                        match = true;
                        break;
                    }
                }
                if (!match) return false;
            }

            // Dateiname enthält …
            if (options.getFileNameString() != null && !options.getFileNameString().isBlank()) {
                if (!file.getName().toLowerCase().contains(options.getFileNameString().toLowerCase())) {
                    return false;
                }
            }

            // 2) Nur wenn Datumsfilter gesetzt sind, teure Attribute lesen
            boolean needsCreation = (options.getCreationDate() != null);
            boolean needsModified = (options.getModificationDate() != null);

            if (needsCreation || needsModified) {
                BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);

                if (needsCreation) {
                    LocalDate creationDate = attrs.creationTime()
                            .toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    if (!compareDate(creationDate, options.getCreationDate(), options.getFileCreationDateOperator())) {
                        return false;
                    }
                }

                if (needsModified) {
                    LocalDate modifiedDate = attrs.lastModifiedTime()
                            .toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    if (!compareDate(modifiedDate, options.getModificationDate(), options.getFileModificationDateOperator())) {
                        return false;
                    }
                }
            }

            return true;
        } catch (Exception e) {
            // Im Fehlerfall Datei überspringen (z. B. keine Berechtigung)
            return false;
        }
    }

    // --- Vergleichslogik für Datumsoperatoren (unverändert beibehalten) ---
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

    // --- Fortschrittsanzeige (gedrosselt & nur bei Änderungen) ---
    private void updateProgress(IntConsumer progressUpdater,
                                int current,
                                int total,
                                int[] lastProgress,
                                long[] nextUpdateNs,
                                boolean force) {

        if (progressUpdater == null || total <= 0) return;

        int p = (int) Math.floor((current * 100.0) / total);
        long now = System.nanoTime();

        // UI nicht fluten: nur bei Änderung ODER alle ~50ms
        boolean timeOk = now >= nextUpdateNs[0];
        if (force || p != lastProgress[0] || timeOk) {
            lastProgress[0] = p;
            nextUpdateNs[0] = now + 50_000_000L; // 50ms
            safeAccept(progressUpdater, p);
        }
    }

    // --- Hilfsfunktionen ---
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return (lastDot > 0 && lastDot < fileName.length() - 1)
                ? fileName.substring(lastDot + 1).toLowerCase()
                : "";
    }

    private static void safeAccept(Consumer<FileSearchModel> consumer, FileSearchModel model) {
        try {
            consumer.accept(model);
        } catch (Exception ignored) {
        }
    }

    private static void safeAccept(IntConsumer consumer, int value) {
        try {
            consumer.accept(value);
        } catch (Exception ignored) {
        }
    }
}
