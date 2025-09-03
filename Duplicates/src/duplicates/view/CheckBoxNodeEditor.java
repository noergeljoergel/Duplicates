package duplicates.view;

import duplicates.model.CheckBoxNode;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;    // ✅ wichtig
import javax.swing.tree.TreeCellEditor;
import java.awt.*;
import java.util.EventObject;

public class CheckBoxNodeEditor extends AbstractCellEditor implements TreeCellEditor {

    private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
    private final JCheckBox checkBox = new JCheckBox();
    private final JLabel label = new JLabel();

    private CheckBoxNode currentNode;
    private DefaultMutableTreeNode treeNode;
    private JTree currentTree;              // ✅ Referenz auf den Baum

    public CheckBoxNodeEditor() {
        panel.setOpaque(false);
        checkBox.setOpaque(false);
        panel.add(checkBox);
        panel.add(label);

        // Klick auf die Checkbox übernimmt den Wert ins Model und refresht den Node
        checkBox.addActionListener(e -> {
            if (currentNode != null) {
                currentNode.setSelected(checkBox.isSelected());
                if (currentTree != null && currentTree.getModel() instanceof DefaultTreeModel dtm) {
                    dtm.nodeChanged(treeNode);        // ✅ Renderer neu zeichnen
                }
            }
            stopCellEditing();                        // Editor schließen
        });
    }

    @Override
    public Component getTreeCellEditorComponent(JTree tree, Object value,
                                                boolean selected, boolean expanded,
                                                boolean leaf, int row) {
        currentTree = tree;                           // ✅ Baum merken

        if (value instanceof DefaultMutableTreeNode node &&
            node.getUserObject() instanceof CheckBoxNode cbNode) {

            treeNode = node;
            currentNode = cbNode;

            checkBox.setSelected(cbNode.isSelected());
            label.setIcon(expanded ? UIManager.getIcon("Tree.openIcon")
                                   : UIManager.getIcon("Tree.closedIcon"));
            label.setText(cbNode.toString());
        } else {
            // Fallback, sollte eigentlich nicht vorkommen
            treeNode = null;
            currentNode = null;
            checkBox.setSelected(false);
            label.setIcon(UIManager.getIcon("Tree.closedIcon"));
            label.setText(value != null ? value.toString() : "");
        }
        return panel;
    }

    @Override
    public Object getCellEditorValue() {
        return currentNode;                            // aktueller Node-Zustand
    }

    @Override
    public boolean isCellEditable(EventObject event) {
        return true;                                   // Klick überall erlaubt
    }
}