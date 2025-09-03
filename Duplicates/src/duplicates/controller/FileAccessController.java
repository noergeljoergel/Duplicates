package duplicates.controller;

import java.io.File;

public class FileAccessController {

    /**
     * Liefert die Unterordner eines Verzeichnisses.
     */
    public File[] getSubFolders(File folder) {
        if (folder != null && folder.isDirectory() && folder.canRead()) {
            File[] files = folder.listFiles(File::isDirectory);
            return files != null ? files : new File[0]; // ✅ falls null -> leeres Array
        }
        return new File[0];
    }

    /**
     * Liefert das Standard-Root-Verzeichnis (z. B. Benutzerverzeichnis).
     */
    public File getRootFolder() {
        return new File(System.getProperty("user.home"));
    }
}
