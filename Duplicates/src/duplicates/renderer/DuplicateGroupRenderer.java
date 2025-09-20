package duplicates.renderer;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;

/**
 * Renderer für Gruppenfärbung, Zebra-Streifen und Gruppentrennlinien.
 */
public class DuplicateGroupRenderer extends DefaultTableCellRenderer {

    private final TableModel model;

    public DuplicateGroupRenderer(TableModel model) {
        this.model = model;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {

        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (!(c instanceof JLabel label)) return c;

        int modelRow = table.convertRowIndexToModel(row);
        int groupId = (Integer) model.getValueAt(modelRow, model.getColumnCount() - 1); // groupId ist letzte Spalte

        // --- Standardfarben (abwechselnd je Gruppe und Zeile in der Gruppe) ---
        Color groupEvenA = new Color(230, 240, 255); // hellblau
        Color groupEvenB = new Color(245, 250, 255); // fast weiß
        Color groupOddA  = new Color(255, 245, 230); // hellbeige
        Color groupOddB  = new Color(255, 250, 240); // fast weiß

        boolean isGroupEven = (groupId % 2 == 0);
        boolean isRowEven = (row % 2 == 0);

        if (!isSelected) {
            if (isGroupEven) {
                label.setBackground(isRowEven ? groupEvenA : groupEvenB);
            } else {
                label.setBackground(isRowEven ? groupOddA : groupOddB);
            }
        } else {
            label.setBackground(table.getSelectionBackground());
        }

        // --- Horizontale Linie oben, wenn neue Gruppe beginnt ---
        int prevGroupId = -1;
        if (row > 0) {
            int prevModelRow = table.convertRowIndexToModel(row - 1);
            prevGroupId = (Integer) model.getValueAt(prevModelRow, model.getColumnCount() - 1);
        }

        if (groupId != prevGroupId) {
            label.setBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, Color.GRAY));
        } else {
            label.setBorder(null);
        }

        return label;
    }
}
