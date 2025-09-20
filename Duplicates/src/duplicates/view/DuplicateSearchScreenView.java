package duplicates.view;

import duplicates.controller.DuplicateSearchController;
import duplicates.controller.XMLController;
import duplicates.model.DuplicateSearchModel;
import duplicates.model.DuplicateSearchOptionsModel;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.text.NumberFormat;
import java.util.List;
import java.util.Vector;

/**
 * Ergebnisfenster für die Duplikatsuche mit Gruppierung, Zebra-Streifen und Kontextmenü.
 */
public class DuplicateSearchScreenView extends JFrame {

    private final DuplicateSearchOptionsModel options;
    private final List<String> selectedFolders;
    private final DuplicateSearchController controller = new DuplicateSearchController();

    private final JTable resultTable;
    private final DefaultTableModel tableModel;
    private final JProgressBar progressBar;
    private final JButton btnAbort, btnDelete;

    private SwingWorker<Void, DuplicateSearchModel> worker;
    private int currentGroupId = 0;

    // Spaltenkonstanten
    private static final int COL_GROUP_ID = 0;
    private static final int COL_CHECKBOX = 1;
    private static final int COL_NAME = 2;
    private static final int COL_PATH = 3;
    private static final int COL_SIZE = 4;
    private static final int COL_TYPE = 5;
    private static final int COL_CREATED = 6;
    private static final int COL_MODIFIED = 7;
    private static final int COL_HASH = 8;

    public DuplicateSearchScreenView(DuplicateSearchOptionsModel options, List<String> selectedFolders) {
        super("Duplikatsuche");
        this.options = options;
        this.selectedFolders = selectedFolders;

        setSize(1024, 768);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // --- Spalten inkl. groupId ---
        String[] columns = {
                "groupId", "✓", "Dateiname", "Pfad", "Größe (Byte)", "Typ", "Erstellt", "Geändert", "Hash"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return switch (columnIndex) {
                    case COL_GROUP_ID -> Integer.class;
                    case COL_CHECKBOX -> Boolean.class;
                    case COL_SIZE -> Long.class;
                    default -> String.class;
                };
            }
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == COL_CHECKBOX;
            }
        };

        resultTable = new JTable(tableModel);
        resultTable.setAutoCreateRowSorter(true);
        resultTable.setFillsViewportHeight(true);

        // groupId ausblenden
        resultTable.removeColumn(resultTable.getColumnModel().getColumn(COL_GROUP_ID));

        // Checkbox im Header
        addHeaderCheckBox(resultTable.getColumnModel().getColumn(0));

        // Renderer mit Gruppierung + Zebra
        GroupDecoratingRenderer renderer = new GroupDecoratingRenderer(resultTable.getDefaultRenderer(String.class));
        for (int i = 0; i < resultTable.getColumnModel().getColumnCount(); i++) {
            resultTable.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        // Sortierung: nach groupId blockweise
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(tableModel);
        sorter.setSortKeys(List.of(new RowSorter.SortKey(COL_GROUP_ID, SortOrder.ASCENDING)));
        resultTable.setRowSorter(sorter);

        // --- Fortschrittsbalken ---
        progressBar = new JProgressBar(0, 100);
        progressBar.setPreferredSize(new Dimension(getWidth(), 15));
        progressBar.setStringPainted(true);

        // --- Buttons ---
        btnAbort = new JButton("Suche abbrechen");
        btnDelete = new JButton("Löschen");
        btnDelete.addActionListener(e -> deleteSelected());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(btnDelete);
        buttonPanel.add(btnAbort);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(buttonPanel, BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(resultTable), BorderLayout.CENTER);
        mainPanel.add(progressBar, BorderLayout.SOUTH);
        add(mainPanel);

        // Events
        btnAbort.addActionListener(e -> {
            if (worker != null && !worker.isDone()) {
                worker.cancel(true);
                controller.cancel();
                progressBar.setIndeterminate(false);
                progressBar.setString("Abgebrochen");
            }
        });

        // Kontextmenü
        installContextMenu();

        // Spaltenbreiten einstellen
        optimizeColumnWidths();

        // Suche starten
        startSearch();
    }

    /** Renderer für Zebra + Gruppenfarben + Trennlinien */
    private class GroupDecoratingRenderer implements TableCellRenderer {
        private final TableCellRenderer base;
        GroupDecoratingRenderer(TableCellRenderer base) { this.base = base; }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            Component c = base.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!(c instanceof JComponent jc)) return c;

            int modelRow = table.convertRowIndexToModel(row);
            int gid = (Integer) tableModel.getValueAt(modelRow, COL_GROUP_ID);

            if (!isSelected) {
                // Zebra-Farben
                Color zebra = (row % 2 == 0) ? new Color(250, 250, 250) : new Color(240, 240, 240);

                // Gruppenfarben
                Color groupColor = (gid % 2 == 0) ? new Color(230, 240, 255) : new Color(250, 250, 240);

                // Mischung
                Color finalBg = new Color(
                        (zebra.getRed() + groupColor.getRed()) / 2,
                        (zebra.getGreen() + groupColor.getGreen()) / 2,
                        (zebra.getBlue() + groupColor.getBlue()) / 2
                );
                jc.setBackground(finalBg);
                jc.setForeground(Color.BLACK);
            }

            // Linien
            boolean sep = false;
            if (row > 0) {
                int prevModelRow = table.convertRowIndexToModel(row - 1);
                int prevGid = (Integer) tableModel.getValueAt(prevModelRow, COL_GROUP_ID);
                sep = gid != prevGid;
            }

            Color thinLine = new Color(200, 200, 200);
            Color groupLine = Color.GRAY;

            int top = sep ? 2 : 0;
            int bottom = 1;
            jc.setBorder(BorderFactory.createMatteBorder(top, 0, bottom, 0, sep ? groupLine : thinLine));

            // Tooltip für Hash
            int modelCol = table.convertColumnIndexToModel(column);
            if (modelCol == COL_HASH && value instanceof String s) {
                jc.setToolTipText(s);
            }

            return jc;
        }
    }

    /** Checkbox-Header */
    private void addHeaderCheckBox(TableColumn checkboxColumn) {
        JCheckBox selectAll = new JCheckBox();
        selectAll.setHorizontalAlignment(SwingConstants.CENTER);
        checkboxColumn.setHeaderRenderer((table, value, isSelected, hasFocus, row, col) -> selectAll);

        selectAll.addActionListener(e -> {
            boolean checked = selectAll.isSelected();
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                tableModel.setValueAt(checked, i, COL_CHECKBOX);
            }
        });
    }

    /** Suche starten */
    private void startSearch() {
        progressBar.setValue(0);
        progressBar.setIndeterminate(false);

        worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                XMLController.beginDuplicateResults("results/duplicates.xml");
                controller.searchDuplicates(
                        options,
                        selectedFolders,
                        (model, groupId) -> {
                            if (!isCancelled()) {
                                currentGroupId = groupId;
                                publish(model);
                            }
                        },
                        this::setProgress
                );
                XMLController.endDuplicateResults();
                return null;
            }

            @Override
            protected void process(List<DuplicateSearchModel> chunks) {
                for (DuplicateSearchModel model : chunks) {
                    addResultRow(model, currentGroupId);
                    XMLController.appendDuplicateResult(model, currentGroupId);
                }
            }

            @Override
            protected void done() {
                btnAbort.setVisible(false);
                progressBar.setIndeterminate(false);
                progressBar.setValue(100);
            }
        };

        worker.addPropertyChangeListener(evt -> {
            if ("progress".equals(evt.getPropertyName())) {
                int value = (Integer) evt.getNewValue();
                progressBar.setValue(value);
            }
        });

        worker.execute();
    }

    /** Zeile hinzufügen */
    private void addResultRow(DuplicateSearchModel model, int groupId) {
        SwingUtilities.invokeLater(() -> {
            Vector<Object> row = new Vector<>();
            row.add(groupId);
            row.add(false);
            row.add(model.getFileName());
            row.add(model.getParentPath());
            row.add(model.getFileSizeBytes());
            row.add(model.getFileType());
            row.add(model.getCreationDate() != null ? model.getCreationDate().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "-");
            row.add(model.getModificationDate() != null ? model.getModificationDate().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "-");
            row.add(model.getFileHash());
            tableModel.addRow(row);
        });
    }

    /** Kontextmenü */
    private void installContextMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem openItem = new JMenuItem("Öffnen");
        JMenuItem showItem = new JMenuItem("Im Ordner anzeigen");
        JMenuItem copyItem = new JMenuItem("Kopieren");
        JMenuItem cutItem = new JMenuItem("Ausschneiden");
        JMenuItem deleteItem = new JMenuItem("Löschen");
        JMenuItem dirDeleteItem = new JMenuItem("Verzeichnis löschen");
        JMenuItem propsItem = new JMenuItem("Eigenschaften");

        menu.add(openItem);
        menu.add(showItem);
        menu.addSeparator();
        menu.add(copyItem);
        menu.add(cutItem);
        menu.add(deleteItem);
        menu.add(dirDeleteItem);
        menu.addSeparator();
        menu.add(propsItem);

        resultTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) { showPopup(e); }
            @Override
            public void mouseReleased(MouseEvent e) { showPopup(e); }
            private void showPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = resultTable.rowAtPoint(e.getPoint());
                    if (row >= 0 && row < resultTable.getRowCount()) {
                        resultTable.setRowSelectionInterval(row, row);
                        menu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });

        // Aktionen
        openItem.addActionListener(e -> performAction("open"));
        showItem.addActionListener(e -> performAction("show"));
        copyItem.addActionListener(e -> performAction("copy"));
        cutItem.addActionListener(e -> performAction("cut"));
        deleteItem.addActionListener(e -> performAction("delete"));
        dirDeleteItem.addActionListener(e -> performAction("dirDelete"));
        propsItem.addActionListener(e -> performAction("props"));
    }

    /** Aktion ausführen */
    private void performAction(String action) {
        int row = resultTable.getSelectedRow();
        if (row < 0) return;

        int modelRow = resultTable.convertRowIndexToModel(row);
        String name = (String) tableModel.getValueAt(modelRow, COL_NAME);
        String parent = (String) tableModel.getValueAt(modelRow, COL_PATH);
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
                case "dirDelete" -> {
                    int confirm = JOptionPane.showConfirmDialog(this, "Verzeichnis wirklich löschen?\n" + parent, "Bestätigung", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        int confirm2 = JOptionPane.showConfirmDialog(this, "Wirklich SICHER löschen?\n" + parent, "Endgültig löschen", JOptionPane.YES_NO_OPTION);
                        if (confirm2 == JOptionPane.YES_OPTION) {
                            Files.walk(path.getParent()).map(Path::toFile).sorted((a, b) -> -a.compareTo(b)).forEach(File::delete);
                        }
                    }
                }
                case "props" -> JOptionPane.showMessageDialog(this, "Pfad: " + path, "Eigenschaften", JOptionPane.INFORMATION_MESSAGE);
                case "copy" -> Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(path.toString()), null);
                case "cut" -> { /* ggf. Implementieren */ }
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Fehler: " + ex.getMessage(), "Aktion fehlgeschlagen", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Löschen-Button */
    private void deleteSelected() {
        int[] rows = resultTable.getSelectedRows();
        if (rows.length == 0) return;
        for (int row : rows) {
            int modelRow = resultTable.convertRowIndexToModel(row);
            String name = (String) tableModel.getValueAt(modelRow, COL_NAME);
            String parent = (String) tableModel.getValueAt(modelRow, COL_PATH);
            Path path = Paths.get(parent, name);
            try {
                Files.deleteIfExists(path);
                tableModel.removeRow(modelRow);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Fehler: " + e.getMessage(), "Löschen fehlgeschlagen", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /** Spaltenbreiten optimieren */
    private void optimizeColumnWidths() {
        TableColumnModel cm = resultTable.getColumnModel();
        cm.getColumn(COL_CHECKBOX - 1).setMaxWidth(30);   // Checkbox
        cm.getColumn(COL_NAME - 1).setPreferredWidth(250);
        cm.getColumn(COL_PATH - 1).setPreferredWidth(400);
        cm.getColumn(COL_SIZE - 1).setPreferredWidth(100);
        cm.getColumn(COL_TYPE - 1).setPreferredWidth(60);
        cm.getColumn(COL_CREATED - 1).setPreferredWidth(90);
        cm.getColumn(COL_MODIFIED - 1).setPreferredWidth(90);
        cm.getColumn(COL_HASH - 1).setPreferredWidth(200);
    }
}
