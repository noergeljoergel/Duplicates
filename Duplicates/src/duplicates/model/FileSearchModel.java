package duplicates.model;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Modell-Klasse für ein einzelnes Suchergebnis.
 * Enthält Dateipfad, Größe, Typ, Erstellungs- und Änderungsdatum.
 */
public class FileSearchModel {

    private final File file;
    private final String absolutePath;
    private final String fileName;
    private final long fileSizeBytes;
    private final String fileExtension;
    private final LocalDate creationDate;
    private final LocalDate modificationDate;

    public FileSearchModel(File file) {
        this.file = file;
        this.absolutePath = file.getAbsolutePath();
        this.fileName = file.getName();
        this.fileSizeBytes = file.length();
        this.fileExtension = extractExtension(file.getName());

        LocalDate created = null;
        LocalDate modified = null;

        try {
            BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            created = attrs.creationTime()
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            modified = attrs.lastModifiedTime()
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
        } catch (Exception ignored) {
            // Falls nicht lesbar, bleiben Werte null
        }

        this.creationDate = created;
        this.modificationDate = modified;
    }

    // --- Getter ---
    public File getFile() {
        return file;
    }

    public String getAbsolutePath() {
        return absolutePath;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    /** Liefert Dateigröße in MB mit einer Nachkommastelle */
    public double getFileSizeMB() {
        return fileSizeBytes / (1024.0 * 1024.0);
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    public LocalDate getModificationDate() {
        return modificationDate;
    }

    /** Liefert Dateityp als Klartext ("Ordner", "txt", "jpg" etc.) */
    public String getDisplayType() {
        return file.isDirectory() ? "Ordner" : fileExtension.isBlank() ? "Unbekannt" : fileExtension;
    }

    // --- Hilfsmethode ---
    private String extractExtension(String name) {
        int dot = name.lastIndexOf('.');
        return (dot >= 0 && dot < name.length() - 1) ? name.substring(dot + 1).toLowerCase() : "";
    }

    @Override
    public String toString() {
        return String.format("%s (%s MB, Typ: %s)", fileName, String.format("%.1f", getFileSizeMB()), getDisplayType());
    }
}
