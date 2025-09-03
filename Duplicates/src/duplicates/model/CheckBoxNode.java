package duplicates.model;

import java.io.File;

public class CheckBoxNode {
    private final File file;
    private boolean selected;

    public CheckBoxNode(File file) {
        this.file = file;
        this.selected = false;
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

    @Override
    public String toString() {
        return file.getName().isEmpty() ? file.getPath() : file.getName();
    }
}