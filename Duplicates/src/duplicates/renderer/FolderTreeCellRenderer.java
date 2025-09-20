package duplicates.renderer;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.io.File;

public class FolderTreeCellRenderer extends DefaultTreeCellRenderer {

    private final Icon folderIcon;
    private final Icon driveIcon;

    public FolderTreeCellRenderer() {
        // Standard-Icons von Swing verwenden (später erweiterbar)
        folderIcon = UIManager.getIcon("FileView.directoryIcon");
        driveIcon = UIManager.getIcon("FileView.hardDriveIcon");
    }

    @Override
    public Component getTreeCellRendererComponent(
            JTree tree, Object value, boolean selected,
            boolean expanded, boolean leaf, int row, boolean hasFocus) {

        JLabel label = (JLabel) super.getTreeCellRendererComponent(
                tree, value, selected, expanded, leaf, row, hasFocus);

        Object userObject = ((javax.swing.tree.DefaultMutableTreeNode) value).getUserObject();

        if (userObject instanceof File file) {
            // Setze den Namen als Text
            label.setText(file.getName().isEmpty() ? file.getPath() : file.getName());

            // Icon setzen: Root-Laufwerk bekommt anderes Icon
            if (file.getParentFile() == null) {
                label.setIcon(driveIcon);
            } else {
                label.setIcon(folderIcon);
            }
        }

        return label;
    }
}