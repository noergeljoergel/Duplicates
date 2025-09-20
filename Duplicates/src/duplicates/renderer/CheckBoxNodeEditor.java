package duplicates.renderer;

import duplicates.model.CheckBoxNode;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellEditor;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.EventObject;

public class CheckBoxNodeEditor extends AbstractCellEditor implements TreeCellEditor {
    private final CheckBoxNodeRenderer renderer = new CheckBoxNodeRenderer();

    private DefaultMutableTreeNode currentTreeNode; // <— den Knoten merken
    private CheckBoxNode currentModel;              // <— und sein Modell

    public CheckBoxNodeEditor(JTree tree) {
        // Checkbox-Klick sofort committen
        renderer.checkBox.addActionListener(e -> stopCellEditing());

        // Schon bei einfachem Klick editieren (und nicht expandieren)
        tree.setToggleClickCount(0);

        // Wenn Fokus wechselt, committe laufende Edits
        tree.setInvokesStopCellEditing(true);
    }

    @Override
    public boolean isCellEditable(EventObject e) {
        return (e instanceof MouseEvent me) && me.getClickCount() >= 1;
    }

    @Override
    public Object getCellEditorValue() {
        if (currentModel != null) {
            // Status in das bestehende UserObject schreiben (kein neues Objekt!)
            currentModel.setSelected(renderer.checkBox.isSelected());
        }
        return currentModel; // dieselbe Instanz zurückgeben
    }

    @Override
    public Component getTreeCellEditorComponent(
            JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row) {

        currentTreeNode = (value instanceof DefaultMutableTreeNode) ? (DefaultMutableTreeNode) value : null;
        currentModel = null;

        if (currentTreeNode != null && currentTreeNode.getUserObject() instanceof CheckBoxNode m) {
            currentModel = m;
        }
        // gleiche Darstellung wie im Renderer — so verschwindet kein Text
        return renderer.getTreeCellRendererComponent(tree, value, true, expanded, leaf, row, true);
    }
}