package duplicates.view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;

public class DuplicateContextMenu {

    public DuplicateContextMenu(JTable table, DefaultTableModel model) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem openItem = new JMenuItem("Öffnen");
        JMenuItem showItem = new JMenuItem("Im Ordner anzeigen");
        JMenuItem deleteItem = new JMenuItem("Löschen");
        JMenuItem deleteDirItem = new JMenuItem("Verzeichnis löschen");
        JMenuItem propsItem = new JMenuItem("Eigenschaften");

        menu.add(openItem);
        menu.add(showItem);
        menu.addSeparator();
        menu.add(deleteItem);
        menu.add(deleteDirItem);
        menu.addSeparator();
        menu.add(propsItem);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) { showPopup(e); }
            @Override
            public void mouseReleased(MouseEvent e) { showPopup(e); }

            private void showPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0 && row < table.getRowCount()) {
                        table.setRowSelectionInterval(row, row);
                        menu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });

        openItem.addActionListener(e -> performAction(table, model, "open"));
        showItem.addActionListener(e -> performAction(table, model, "show"));
        deleteItem.addActionListener(e -> performAction(table, model, "delete"));
        deleteDirItem.addActionListener(e -> performAction(table, model, "deleteDir"));
        propsItem.addActionListener(e -> performAction(table, model, "props"));
    }

    private void performAction(JTable table, DefaultTableModel model, String action) {
        int row = table.getSelectedRow();
        if (row < 0) return;

        int modelRow = table.convertRowIndexToModel(row);
        String name = (String) model.getValueAt(modelRow, 2);
        String parent = (String) model.getValueAt(modelRow, 3);
        Path path = Paths.get(parent, name);

        try {
            switch (action) {
                case "open" -> Desktop.getDesktop().open(path.toFile());
                case "show" -> {
                    if (System.getProperty("os.name").toLowerCase().contains("win")) {
                        new ProcessBuilder("explorer.exe", "/select,", path.toAbsolutePath().toString()).start();
                    } else {
                        Desktop.getDesktop().open(path.getParent().toFile());
                    }
                }
                case "delete" -> Files.deleteIfExists(path);
                case "deleteDir" -> {
                    Path dirPath = path.getParent();
                    int confirm = JOptionPane.showConfirmDialog(table,
                            "Soll das gesamte Verzeichnis gelöscht werden?\n" + dirPath,
                            "Bestätigung", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        Files.walk(dirPath)
                                .sorted((p1, p2) -> p2.compareTo(p1)) // erst Dateien, dann Ordner
                                .forEach(p -> {
                                    try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                                });
                    }
                }
                case "props" -> JOptionPane.showMessageDialog(table,
                        "Pfad: " + path.toAbsolutePath(), "Eigenschaften", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(table, "Fehler: " + ex.getMessage(),
                    "Aktion fehlgeschlagen", JOptionPane.ERROR_MESSAGE);
        }
    }
}
