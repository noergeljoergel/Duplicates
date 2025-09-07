package duplicates.model;

import java.time.LocalDate;

/**
 * Basis-Klasse für die Einstellungen der Duplikatsuche.
 */
public class FileSearchOptionsModel {
    private double minFileSize;
    private double maxFileSize;
    private String fileExtention;
    private String fileNameString;
    private String fileCreationDateOperator;
    private LocalDate creationDate;
    private String fileModificationDateOperator;
    private LocalDate modificationDate;
    private boolean subFolderBoo;

    public FileSearchOptionsModel() {
        // Leerer Konstruktor für einfaches Befüllen per Setter
    }

    public FileSearchOptionsModel(double minFileSize, double maxFileSize,
                                  String fileExtention, String fileNameString,
                                  String fileCreationDateOperator, LocalDate creationDate,
                                  String fileModificationDateOperator, LocalDate modificationDate,
                                  boolean subFolderBoo) {
        this.minFileSize = minFileSize;
        this.maxFileSize = maxFileSize;
        this.fileExtention = fileExtention;
        this.fileNameString = fileNameString;
        this.fileCreationDateOperator = fileCreationDateOperator;
        this.creationDate = creationDate;
        this.fileModificationDateOperator = fileModificationDateOperator;
        this.modificationDate = modificationDate;
        this.subFolderBoo = subFolderBoo;
    }

    public double getMinFileSize() {
        return minFileSize;
    }

    public void setMinFileSize(double minFileSize) {
        this.minFileSize = minFileSize;
    }

    public double getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(double maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public String getFileExtention() {
        return fileExtention;
    }

    public void setFileExtention(String fileExtention) {
        this.fileExtention = fileExtention;
    }

    public String getFileNameString() {
        return fileNameString;
    }

    public void setFileNameString(String fileNameString) {
        this.fileNameString = fileNameString;
    }

    public String getFileCreationDateOperator() {
        return fileCreationDateOperator;
    }

    public void setFileCreationDateOperator(String fileCreationDateOperator) {
        this.fileCreationDateOperator = fileCreationDateOperator;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDate creationDate) {
        this.creationDate = creationDate;
    }

    public String getFileModificationDateOperator() {
        return fileModificationDateOperator;
    }

    public void setFileModificationDateOperator(String fileModificationDateOperator) {
        this.fileModificationDateOperator = fileModificationDateOperator;
    }

    public LocalDate getModificationDate() {
        return modificationDate;
    }

    public void setModificationDate(LocalDate modificationDate) {
        this.modificationDate = modificationDate;
    }

    public boolean isSubFolderBoo() {
        return subFolderBoo;
    }

    public void setSubFolderBoo(boolean subFolderBoo) {
        this.subFolderBoo = subFolderBoo;
    }

    @Override
    public String toString() {
        return "DuplicateSearchOptions{" +
                "minFileSize=" + minFileSize +
                ", maxFileSize=" + maxFileSize +
                ", fileExtention='" + fileExtention + '\'' +
                ", fileNameString='" + fileNameString + '\'' +
                ", fileCreationDateOperator='" + fileCreationDateOperator + '\'' +
                ", creationDate=" + creationDate +
                ", fileModificationDateOperator='" + fileModificationDateOperator + '\'' +
                ", modificationDate=" + modificationDate +
                ", subFolderBoo=" + subFolderBoo +
                '}';
    }
}