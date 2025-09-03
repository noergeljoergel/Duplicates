package duplicates.controller;

import java.io.File;

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
}
