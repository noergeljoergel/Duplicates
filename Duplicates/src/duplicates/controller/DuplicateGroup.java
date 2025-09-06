package duplicates.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class DuplicateGroup {
    private final String hash;
    private final List<File> files;

    public DuplicateGroup(String hash) {
        this.hash = Objects.requireNonNull(hash, "hash must not be null");
        this.files = new ArrayList<>();
    }

    /** Fügt eine Datei hinzu (ohne doppelte Einträge). */
    public boolean addFile(File file) {
        if (file == null) return false;
        if (!files.contains(file)) {
            return files.add(file);
        }
        return false;
    }

    public boolean removeFile(File file) {
        return files.remove(file);
    }

    /** Unveränderliche Sicht auf die Dateien der Gruppe. */
    public List<File> getFiles() {
        return Collections.unmodifiableList(files);
    }

    public String getHash() {
        return hash;
    }

    public int size() {
        return files.size();
    }

    @Override
    public String toString() {
        return "DuplicateGroup{hash='" + hash + "', files=" + files.size() + "}";
    }

    // Optional – sinnvoll, wenn du DuplicateGroup in Sets/Maps nutzen willst:
    // @Override
    // public boolean equals(Object o) {
    //     if (this == o) return true;
    //     if (!(o instanceof DuplicateGroup)) return false;
    //     DuplicateGroup that = (DuplicateGroup) o;
    //     return Objects.equals(hash, that.hash);
    // }
    //
    // @Override
    // public int hashCode() {
    //     return Objects.hash(hash);
    // }
}