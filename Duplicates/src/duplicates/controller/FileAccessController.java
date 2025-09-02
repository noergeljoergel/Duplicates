package duplicates.controller;

import java.io.File;

public class FileAccessController {

    /**
     * Liefert die Unterordner eines Verzeichnisses.
     */
    public File[] getSubFolders(File folder) {
        if (folder != null && folder.isDirectory()) {
            return folder.listFiles(File::isDirectory);
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
