package duplicates.model;

import java.time.LocalDate;

/**
 * Erweiterte Einstellungen mit zusätzlichen Checkboxen.
 */
public class DuplicateSearchOptionsModel extends FileSearchOptionsModel {
    private boolean fileSizeBoo;
    private boolean fileNameBoo;
    private boolean fileExtentionBoo;

    public DuplicateSearchOptionsModel() {
        super();
    }

    public DuplicateSearchOptionsModel(double minFileSize, 
    										double maxFileSize,
                                         String fileExtention, 
                                         String fileNameString,
                                         String fileCreationDateOperator, 
                                         LocalDate creationDate,
                                         String fileModificationDateOperator, 
                                         LocalDate modificationDate,
                                         boolean subFolderBoo,
                                         boolean fileSizeBoo, 
                                         boolean fileNameBoo, 
                                         boolean fileExtentionBoo) {
        super(minFileSize, maxFileSize, fileExtention, fileNameString,
              fileCreationDateOperator, creationDate,
              fileModificationDateOperator, modificationDate,
              subFolderBoo);
        this.fileSizeBoo = fileSizeBoo;
        this.fileNameBoo = fileNameBoo;
        this.fileExtentionBoo = fileExtentionBoo;
    }

    public boolean isFileSizeBoo() {
        return fileSizeBoo;
    }

    public void setFileSizeBoo(boolean fileSizeBoo) {
        this.fileSizeBoo = fileSizeBoo;
    }

    public boolean isFileNameBoo() {
        return fileNameBoo;
    }

    public void setFileNameBoo(boolean fileNameBoo) {
        this.fileNameBoo = fileNameBoo;
    }

    public boolean isFileExtentionBoo() {
        return fileExtentionBoo;
    }

    public void setFileExtentionBoo(boolean fileExtentionBoo) {
        this.fileExtentionBoo = fileExtentionBoo;
    }

    @Override
    public String toString() {
        return super.toString() + " | Extended{" +
                "fileSizeBoo=" + fileSizeBoo +
                ", fileNameBoo=" + fileNameBoo +
                ", fileExtentionBoo=" + fileExtentionBoo +
                '}';
    }
}