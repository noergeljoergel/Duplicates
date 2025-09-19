package duplicates.view;

import duplicates.controller.FileSearchController;
import duplicates.model.FileSearchModel;
import duplicates.model.FileSearchOptionsModel;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.util.List;
import java.util.Vector;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.IOException;

/**
 * Fenster für die Dateisuche.
 * Zeigt Suchfortschritt und Ergebnisse in einer Tabelle.
 */
public class FileSearchScreenView extends JFrame {

    private final FileSearchOptionsModel options;
    private final List<String> selectedFolders;
    private final FileSearchController controller = new FileSearchController();

    private final JTable resultTable;
    private final DefaultTableModel tableModel;
    private final JProgressBar progressBar;
    private final JButton btnAbort;
    private final JButton btnDelete;

    private SwingWorker<Void, FileSearchModel> worker; // SwingWorker für asynchrone Suche

    /**
     * Konstruktor: Übergabe der Suchoptionen und der zu durchsuchenden Ordner.
     */
    public FileSearchScreenView(FileSearchOptionsModel options, List<String> selectedFolders) {
        super("Dateisuche");
        this.options = options;
        this.selectedFolders = selectedFolders;

        setSize(1024, 768);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // --- 1. Tabellenmodell erstellen ---
        String[] columns = {
                "✓", "Dateiname", "Dateipfad", "Größe (Byte)", "Typ", "Erstellt", "Geändert", "Systemdatei", "Versteckt"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return switch (columnIndex) {
                    case 0 -> Boolean.class; // Checkbox
                    case 3 -> Long.class;    // Dateigröße
                    case 7, 8 -> Boolean.class; // Systemdatei, Versteckt
                    default -> String.class;
                };
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0; // Nur Checkbox editierbar
            }
        };

        resultTable = new JTable(tableModel);
        resultTable.setAutoCreateRowSorter(true);
        resultTable.setFillsViewportHeight(true);

        // --- 1.1 Spaltenbreiten optimieren ---
        TableColumnModel columnModel = resultTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(30);  // Checkbox
        columnModel.getColumn(1).setPreferredWidth(170); // Dateiname
        columnModel.getColumn(2).setPreferredWidth(330); // Dateipfad
        columnModel.getColumn(3).setPreferredWidth(90);  // Größe
        columnModel.getColumn(4).setPreferredWidth(80);  // Typ
        columnModel.getColumn(5).setPreferredWidth(100); // Erstellt
        columnModel.getColumn(6).setPreferredWidth(100); // Geändert
        columnModel.getColumn(7).setPreferredWidth(80);  // Systemdatei
        columnModel.getColumn(8).setPreferredWidth(80);  // Versteckt

        // --- 1.2 Header-Checkbox hinzufügen ---
        addHeaderCheckBox(columnModel.getColumn(0));

        // --- 1.3 Zahlenformat für Spalte "Größe" ---
        DefaultTableCellRenderer numberRenderer = new DefaultTableCellRenderer() {
            private final NumberFormat nf = NumberFormat.getIntegerInstance();

            @Override
            protected void setValue(Object value) {
                if (value instanceof Number n) {
                    setHorizontalAlignment(SwingConstants.RIGHT);
                    setText(nf.format(n.longValue()));
                } else {
                    super.setValue(value);
                }
            }
        };
        columnModel.getColumn(3).setCellRenderer(numberRenderer);

        // --- 1.4 Renderer für Systemdatei/Versteckt als Checkboxen ---
        DefaultTableCellRenderer booleanRenderer = new DefaultTableCellRenderer() {
            @Override
            public void setValue(Object value) {
                if (value instanceof Boolean b) {
                    setHorizontalAlignment(SwingConstants.CENTER);
                    setText(b ? "✓" : "");
                } else {
                    super.setValue(value);
                }
            }
        };
        columnModel.getColumn(7).setCellRenderer(booleanRenderer);
        columnModel.getColumn(8).setCellRenderer(booleanRenderer);

        // --- 2. Fortschrittsbalken erstellen ---
        progressBar = new JProgressBar(0, 100);
        progressBar.setPreferredSize(new Dimension(getWidth(), 15));
        progressBar.setStringPainted(true);

        // --- 3. Buttons erstellen ---
        btnAbort = new JButton("Suche abbrechen");
        btnDelete = new JButton("Ausgewählte löschen");
        JButton placeholder1 = new JButton("Platzhalter 1");
        JButton placeholder2 = new JButton("Platzhalter 2");

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(btnAbort);
        buttonPanel.add(btnDelete);
        buttonPanel.add(placeholder1);
        buttonPanel.add(placeholder2);

        btnDelete.setEnabled(false);

        // --- 4. Layout zusammenbauen ---
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(buttonPanel, BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(resultTable), BorderLayout.CENTER);
        mainPanel.add(progressBar, BorderLayout.SOUTH);

        add(mainPanel);

     // --- 5. Events binden ---
        btnAbort.addActionListener(e -> {
            if (worker != null && !worker.isDone()) {
                worker.cancel(true);
                controller.cancel(); // Controller-Abbruch
                progressBar.setIndeterminate(false);
                progressBar.setString("Abgebrochen");
            }
            // Fenster bleibt offen -> kein dispose()!
        });

        btnDelete.addActionListener(e -> deleteSelectedFiles());

        // --- 6. Suche starten ---
        startSearch();
    }

    /**
     * Fügt eine Header-Checkbox für "alle auswählen" hinzu.
     */
    private void addHeaderCheckBox(TableColumn checkboxColumn) {
        JCheckBox selectAll = new JCheckBox();
        selectAll.setHorizontalAlignment(SwingConstants.CENTER);

        // Renderer für die Checkbox im Header
        checkboxColumn.setHeaderRenderer((table, value, isSelected, hasFocus, row, column) -> selectAll);

        // Listener: Klick auf Header-Checkbox = alle Zeilen an/aus
        selectAll.addActionListener(e -> {
            boolean checked = selectAll.isSelected();
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                tableModel.setValueAt(checked, i, 0);
            }
        });

        // Maus-Listener: Header-Klick toggelt Checkbox
        resultTable.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int viewColumn = resultTable.columnAtPoint(e.getPoint());
                int modelColumn = resultTable.convertColumnIndexToModel(viewColumn);
                if (modelColumn == 0) {
                    selectAll.doClick();
                    resultTable.getTableHeader().repaint();
                }
            }
        });
    }

    /**
     * Startet die Suche asynchron mit SwingWorker.
     */
    private void startSearch() {
        progressBar.setValue(0);
        progressBar.setIndeterminate(false);

        worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                controller.searchFiles(
                        selectedFolders,
                        options,
                        model -> {
                            if (!isCancelled()) publish(model);
                        },
                        this::setProgress
                );
                return null;
            }

            @Override
            protected void process(List<FileSearchModel> chunks) {
                for (FileSearchModel model : chunks) {
                    addResultRow(model);
                }
            }

            @Override
            protected void done() {
                btnAbort.setVisible(false);
                btnDelete.setEnabled(true);
                progressBar.setIndeterminate(false);
                progressBar.setValue(100);
            }
        };

        // Fortschrittsanzeige verbinden
        worker.addPropertyChangeListener(evt -> {
            if ("progress".equals(evt.getPropertyName())) {
                int value = (Integer) evt.getNewValue();
                progressBar.setValue(value);
            }
        });

        worker.execute();
    }

    /**
     * Fügt eine neue Zeile mit Suchergebnis hinzu.
     */
    private void addResultRow(FileSearchModel model) {
        SwingUtilities.invokeLater(() -> {
            Vector<Object> row = new Vector<>();
            row.add(false);
            row.add(model.getFileName());
            row.add(model.getFile().getParent());
            row.add(model.getFileSizeBytes());
            row.add(model.getDisplayType());
            row.add(model.getCreationDate() != null ? model.getCreationDate().toString() : "-");
            row.add(model.getModificationDate() != null ? model.getModificationDate().toString() : "-");
            row.add(model.getFile().isFile() && !model.getFile().canWrite()); // Boolean
            row.add(model.getFile().isHidden()); // Boolean
            tableModel.addRow(row);
        });
    }

    /**
     * Löscht die ausgewählten Dateien (Platzhalter-Implementierung).
     */
    private void deleteSelectedFiles() {
        int rowCount = tableModel.getRowCount();
        if (rowCount == 0) return;

        java.util.List<Integer> rowsToRemove = new java.util.ArrayList<>();
        java.util.List<Path> filesToDelete = new java.util.ArrayList<>();

        // 1) Ausgewählte Dateien einsammeln (aus dem *Model*, nicht aus der View)
        for (int i = 0; i < rowCount; i++) {
            Boolean selected = (Boolean) tableModel.getValueAt(i, 0);
            if (Boolean.TRUE.equals(selected)) {
                String name   = (String) tableModel.getValueAt(i, 1);
                String parent = (String) tableModel.getValueAt(i, 2);
                Path p = (parent == null || parent.isBlank())
                        ? Paths.get(name)
                        : Paths.get(parent, name);
                filesToDelete.add(p);
                rowsToRemove.add(i);
            }
        }

        if (filesToDelete.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Keine Dateien ausgewählt.", "Hinweis", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // 2) Sicherheitsabfrage
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Möchten Sie " + filesToDelete.size() + " Datei(en) endgültig löschen?",
                "Löschen bestätigen",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) return;

        // 3) Löschen + UI aktualisieren (von hinten nach vorne, damit Indizes passen)
        int ok = 0, failed = 0;
        StringBuilder errors = new StringBuilder();

        for (int idx = rowsToRemove.size() - 1; idx >= 0; idx--) {
            int modelRow = rowsToRemove.get(idx);
            Path p = filesToDelete.get(idx);
            try {
                // dauerhaft löschen:
                if (java.awt.Desktop.isDesktopSupported()) {
                    boolean moved = java.awt.Desktop.getDesktop().moveToTrash(p.toFile());
                    if (moved) { tableModel.removeRow(modelRow); ok++; continue; }
                }
            		Files.delete(p); // wirft IOException/SecurityException bei Fehler

                tableModel.removeRow(modelRow);
                ok++;
            } catch (IOException | SecurityException ex) {
                failed++;
                errors.append(p.toString()).append(" — ").append(ex.getMessage()).append("\n");
            }
        }

        if (failed > 0) {
            JTextArea area = new JTextArea(errors.toString());
            area.setEditable(false);
            area.setRows(Math.min(10, failed));
            JOptionPane.showMessageDialog(
                    this,
                    new JScrollPane(area),
                    "Einige Dateien konnten nicht gelöscht werden (" + failed + ")",
                    JOptionPane.ERROR_MESSAGE
            );
        } else {
            JOptionPane.showMessageDialog(this, ok + " Datei(en) gelöscht.", "Erfolg", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
