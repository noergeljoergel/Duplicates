package duplicates.view;

import duplicates.model.CheckBoxNode;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

public class CheckBoxNodeRenderer extends DefaultTreeCellRenderer {

    private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
    private final JCheckBox checkBox = new JCheckBox();
    private final JLabel label = new JLabel();

    public CheckBoxNodeRenderer() {
        panel.setOpaque(false);
        checkBox.setOpaque(false);

        // Checkbox zuerst, dann Label
        panel.add(checkBox);
        panel.add(label);
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                  boolean selected, boolean expanded,
                                                  boolean leaf, int row, boolean hasFocus) {

        // Hintergrundfarbe bei Auswahl
        if (selected) {
            panel.setBackground(new Color(0, 120, 215)); // Windows-Explorer-Blau
            panel.setOpaque(true);
            checkBox.setForeground(Color.WHITE);
            label.setForeground(Color.WHITE);
        } else {
            panel.setOpaque(false);
            checkBox.setForeground(Color.BLACK);
            label.setForeground(Color.BLACK);
        }

        if (value instanceof DefaultMutableTreeNode node &&
            node.getUserObject() instanceof CheckBoxNode cbNode) {

            // ✅ Checkbox nur als Haken, kein Text
            checkBox.setSelected(cbNode.isSelected());

            // Label: Icon + Text
            Icon folderIcon = expanded ? getOpenIcon() : getClosedIcon();
            label.setIcon(folderIcon);
            label.setText(cbNode.toString());

        } else {
            checkBox.setSelected(false);
            label.setIcon(getClosedIcon());
            label.setText(value.toString());
        }

        return panel;
    }
}
