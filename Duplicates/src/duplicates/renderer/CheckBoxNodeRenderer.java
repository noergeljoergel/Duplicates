package duplicates.renderer;

import duplicates.model.CheckBoxNode;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.io.File;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;

public class CheckBoxNodeRenderer implements TreeCellRenderer {
    private final JPanel panel = new JPanel(new BorderLayout());
    final JCheckBox checkBox = new JCheckBox();
    private final JLabel label = new JLabel();

    private final FileSystemView fsv = FileSystemView.getFileSystemView();

    public CheckBoxNodeRenderer() {
        panel.setOpaque(false);
        checkBox.setOpaque(false);
        panel.add(checkBox, BorderLayout.WEST);
        panel.add(label, BorderLayout.CENTER);
    }

    @Override
    public Component getTreeCellRendererComponent(
            JTree tree, Object value, boolean selected, boolean expanded,
            boolean leaf, int row, boolean hasFocus) {

        if (value instanceof DefaultMutableTreeNode node) {
            Object uo = node.getUserObject();

            if (uo instanceof duplicates.model.CheckBoxNode cbNode) {
                File f = cbNode.getFile();

                // Text: zuerst SystemDisplayName, sonst Pfad als Fallback
                String name = fsv.getSystemDisplayName(f);
                if (name == null || name.isBlank()) {
                    name = f.getAbsolutePath();
                    // optional: abschließenden Separator bei Laufwerken entfernen
                    if (name.endsWith(File.separator) && name.length() > 1) {
                        name = name.substring(0, name.length() - 1);
                    }
                }

                checkBox.setVisible(true);
                checkBox.setSelected(cbNode.isSelected());
                label.setText(name);
                label.setIcon(fsv.getSystemIcon(f)); // hübsches Laufwerks-/Ordnersymbol

            } else {
                // z. B. "Arbeitsplatz" oder "Loading..." → ohne Checkbox
                checkBox.setVisible(false);
                label.setIcon(null);
                label.setText(String.valueOf(uo));
            }
        }
        return panel;
    }
}
