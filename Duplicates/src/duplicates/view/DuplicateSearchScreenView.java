package duplicates.view;

import duplicates.controller.DuplicateSearchController;
import duplicates.controller.XMLController;
import duplicates.model.DuplicateSearchModel;
import duplicates.model.DuplicateSearchOptionsModel;
import duplicates.renderer.DuplicateGroupRenderer;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;
import java.util.Vector;

/**
 * Hauptfenster für die Duplikatsuche.
 */
public class DuplicateSearchScreenView extends JFrame {

    private final DuplicateSearchOptionsModel options;
    private final List<String> selectedFolders;
    private final DuplicateSearchController controller = new DuplicateSearchController();

    private final JTable resultTable;
    private final DefaultTableModel tableModel;
    private final JProgressBar progressBar;
    private final JButton btnAbort, btnDelete;

    /** Wir publizieren (Model + groupId) als Paket, damit die groupId nicht „verloren“ geht. */
    private static final class RowItem {
        final DuplicateSearchModel model;
        final int groupId;
        RowItem(DuplicateSearchModel m, int gid) { this.model = m; this.groupId = gid; }
    }

    private SwingWorker<Void, RowItem> worker;

    public DuplicateSearchScreenView(DuplicateSearchOptionsModel options, List<String> selectedFolders) {
        super("Duplikatsuche");
        this.options = options;
        this.selectedFolders = selectedFolders;

        setSize(1024, 768);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // --- Spalten (groupId sichtbar als letzte Spalte) ---
        String[] columns = {
                "✓", "Dateiname", "Pfad", "Größe (Byte)", "Typ", "Erstellt", "Geändert", "Hash", "groupId"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return switch (columnIndex) {
                    case 0 -> Boolean.class;   // Checkbox
                    case 3 -> Long.class;      // Größe
                    case 8 -> Integer.class;   // groupId
                    default -> String.class;
                };
            }

            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 0; // Nur Checkbox editierbar
            }
        };

        resultTable = new JTable(tableModel);
        resultTable.setFillsViewportHeight(true);
        resultTable.setAutoCreateRowSorter(true);

        // Einheitlicher Renderer für alle Spalten außer der Checkbox
        DuplicateGroupRenderer renderer = new DuplicateGroupRenderer(tableModel);
        TableColumnModel cm = resultTable.getColumnModel();
        for (int i = 1; i < cm.getColumnCount(); i++) {
            cm.getColumn(i).setCellRenderer(renderer);
        }

        // Kontextmenü installieren
        new DuplicateContextMenu(resultTable, tableModel);

        // --- Fortschritt ---
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);

        // --- Buttons ---
        btnAbort = new JButton("Abbrechen");
        btnDelete = new JButton("Löschen");

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(btnAbort);
        buttonPanel.add(btnDelete);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(buttonPanel, BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(resultTable), BorderLayout.CENTER);
        mainPanel.add(progressBar, BorderLayout.SOUTH);
        add(mainPanel);

        btnAbort.addActionListener(e -> cancelSearch());
        btnDelete.addActionListener(e -> deleteSelected());

        // Spaltenbreiten anpassen
        configureColumnWidths();

        startSearch();
    }

    /**
     * Spaltenbreiten konfigurieren (Checkbox minimal, Pfad breit, Größe kompakt, groupId schmal).
     */
    private void configureColumnWidths() {
        TableColumnModel colModel = resultTable.getColumnModel();

        // 0: Checkbox → so schmal wie möglich
        colModel.getColumn(0).setMaxWidth(30);
        colModel.getColumn(0).setMinWidth(30);

        // 1: Dateiname
        colModel.getColumn(1).setPreferredWidth(240);

        // 2: Pfad
        colModel.getColumn(2).setPreferredWidth(420);

        // 3: Größe (Byte)
        colModel.getColumn(3).setPreferredWidth(110);

        // 4: Typ
        colModel.getColumn(4).setPreferredWidth(70);

        // 5: Erstellt
        colModel.getColumn(5).setPreferredWidth(110);

        // 6: Geändert
        colModel.getColumn(6).setPreferredWidth(110);

        // 7: Hash
        colModel.getColumn(7).setPreferredWidth(260);

        // 8: groupId sichtbar, aber schmal
        colModel.getColumn(8).setMaxWidth(60);
        colModel.getColumn(8).setMinWidth(40);
    }

    private void startSearch() {
        progressBar.setValue(0);
        progressBar.setIndeterminate(false);

        worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                XMLController.beginDuplicateResults("results/duplicates.xml");

                // PUBLISHE JEDE ZEILE MIT IHRER groupId -> keine Vermischung mehr
                controller.searchDuplicates(
                        options,
                        selectedFolders,
                        (model, groupId) -> {
                            if (!isCancelled()) {
                                publish(new RowItem(model, groupId));
                            }
                        },
                        this::setProgress
                );

                XMLController.endDuplicateResults();
                return null;
            }

            @Override
            protected void process(List<RowItem> chunks) {
                for (RowItem item : chunks) {
                    addRow(item.model, item.groupId);
                    XMLController.appendDuplicateResult(item.model, item.groupId);
                }
            }

            @Override
            protected void done() {
                btnAbort.setEnabled(false);
                progressBar.setValue(100);
            }
        };

        worker.execute();
    }

    private void addRow(DuplicateSearchModel model, int groupId) {
        Vector<Object> row = new Vector<>();
        row.add(false); // Checkbox
        row.add(model.getFileName());
        row.add(model.getParentPath());
        row.add(model.getFileSizeBytes());
        row.add(model.getFileType());
        row.add(model.getCreationDate() != null ? model.getCreationDate().toString() : "-");
        row.add(model.getModificationDate() != null ? model.getModificationDate().toString() : "-");
        row.add(model.getFileHash());
        row.add(groupId); // groupId sichtbar am Ende
        tableModel.addRow(row);
    }

    private void cancelSearch() {
        if (worker != null && !worker.isDone()) {
            worker.cancel(true);
            controller.cancel();
            progressBar.setString("Abgebrochen");
        }
    }

    private void deleteSelected() {
        for (int i = tableModel.getRowCount() - 1; i >= 0; i--) {
            Object v = tableModel.getValueAt(i, 0);
            if (v instanceof Boolean b && b) {
                tableModel.removeRow(i);
            }
        }
    }
}
