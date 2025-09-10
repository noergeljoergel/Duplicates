package duplicates.model;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Modell-Klasse für eine einzelne Datei, die auf Duplikate geprüft wird.
 */
public class DuplicateSearchModel {

    private final File file;
    private final String fileName;
    private final String parentPath;
    private final long fileSizeBytes;
    private final String fileType;
    private final LocalDate creationDate;
    private final LocalDate modificationDate;
    private String fileHash; // wird bei Bedarf berechnet (lazy)

    public DuplicateSearchModel(File file) {
        this.file = file;
        this.fileName = file.getName();
        this.parentPath = file.getParent();
        this.fileSizeBytes = file.length();
        this.fileType = extractExtension(file.getName());

        LocalDate created = null;
        LocalDate modified = null;
        try {
            BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            created = attrs.creationTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            modified = attrs.lastModifiedTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        } catch (Exception ignored) {}

        this.creationDate = created;
        this.modificationDate = modified;
    }

    // --- Getter ---
    public File getFile() { return file; }
    public String getFileName() { return fileName; }
    public String getParentPath() { return parentPath; }
    public long getFileSizeBytes() { return fileSizeBytes; }
    public String getFileType() { return fileType; }
    public LocalDate getCreationDate() { return creationDate; }
    public LocalDate getModificationDate() { return modificationDate; }

    /**
     * Berechnet einen MD5-Hash der Datei (lazy: nur beim ersten Aufruf).
     */
    public String getFileHash() {
        if (fileHash == null) {
            fileHash = calculateHash(file);
        }
        return fileHash;
    }

    // --- Hilfsmethoden ---
    private String extractExtension(String name) {
        int dot = name.lastIndexOf('.');
        return (dot >= 0 && dot < name.length() - 1) ? name.substring(dot + 1).toLowerCase() : "";
    }

    private String calculateHash(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            try (var is = Files.newInputStream(file.toPath())) {
                int read;
                while ((read = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "ERROR";
        }
    }
}
