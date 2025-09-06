package duplicates.model;

import java.io.File;
import java.util.Objects;

public class CheckBoxNode {
    private final File file;
    private boolean selected;

    /** Bequemer 1-Argument-Konstruktor (standardmäßig nicht ausgewählt). */
    public CheckBoxNode(File file) {
        this(file, false);
    }

    /** Bequemer 2-Argument-Konstruktor (inkl. Anfangszustand). */
    public CheckBoxNode(File file, boolean selected) {
        this.file = Objects.requireNonNull(file, "file must not be null");
        this.selected = selected;
    }

    public File getFile() {
        return file;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /** Für Renderer/Editor – zeigt den Namen, bei leeren Namen (Root) den Pfad. */
    @Override
    public String toString() {
        String name = file.getName();
        return (name == null || name.isEmpty()) ? file.getPath() : name;
    }

    /** Hilfreich, wenn du Knoten vergleichst oder in Sets/Maps nutzt. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CheckBoxNode that)) return false;
        // identifiziere Datei über ihren absoluten Pfad
        return file.getAbsolutePath().equals(that.file.getAbsolutePath());
    }

    @Override
    public int hashCode() {
        return file.getAbsolutePath().hashCode();
    }
}
