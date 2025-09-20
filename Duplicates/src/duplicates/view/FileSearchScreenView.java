package duplicates.view;

import duplicates.controller.FileSearchController;
import duplicates.controller.XMLController;
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
import java.io.File;
import java.io.IOException;

/**
 * Fenster für die Dateisuche mit teilweiser Anzeige + XML-Auslagerung.
 */
public class FileSearchScreenView extends JFrame {

    private static final int MAX_ROWS_IN_TABLE = 5000;      // sichtbare Obergrenze
    private static final int BATCH_SIZE_FOR_XML = 500;      // ab wievielen zwischenspeichern

    private final FileSearchOptionsModel options;
    private final List<String> selectedFolders;
    private final FileSearchController controller = new FileSearchController();

    private final JTable resultTable;
    private final DefaultTableModel tableModel;
    private final JProgressBar progressBar;
    private final JButton btnAbort;
    private final JButton btnDelete;
    private final JButton btnLoadAll;       // NEU: „Alle Ergebnisse laden“
    private final JLabel lblStatus;         // NEU: zeigt Auslagerung

    private SwingWorker<Void, FileSearchModel> worker;

    // Pfad für ausgelagerte Ergebnisse
    private final String resultsXmlPath = "results/file_search_results.xml";

    // Zähler/Puffer
    private int offloadedCount = 0;
    private int batchCounter = 0;
    
    private int totalFoundCount = 0;


    public FileSearchScreenView(FileSearchOptionsModel options, List<String> selectedFolders) {
        super("Dateisuche");
        this.options = options;
        this.selectedFolders = selectedFolders;

        setSize(1024, 768);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        String[] columns = {
                "✓", "Dateiname", "Dateipfad", "Größe (Byte)", "Typ", "Erstellt", "Geändert", "Systemdatei", "Versteckt"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return switch (columnIndex) {
                    case 0 -> Boolean.class;
                    case 3 -> Long.class;
                    case 7, 8 -> Boolean.class;
                    default -> String.class;
                };
            }
            @Override
            public boolean isCellEditable(int row, int column) { return column == 0; }
        };

        resultTable = new JTable(tableModel);
        resultTable.setAutoCreateRowSorter(true);
        resultTable.setFillsViewportHeight(true);

        TableColumnModel columnModel = resultTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(30);
        columnModel.getColumn(1).setPreferredWidth(170);
        columnModel.getColumn(2).setPreferredWidth(330);

        addHeaderCheckBox(columnModel.getColumn(0));

        DefaultTableCellRenderer numberRenderer = new DefaultTableCellRenderer() {
            private final NumberFormat nf = NumberFormat.getIntegerInstance();
            @Override protected void setValue(Object value) {
                if (value instanceof Number n) {
                    setHorizontalAlignment(SwingConstants.RIGHT);
                    setText(nf.format(n.longValue()));
                } else super.setValue(value);
            }
        };
        columnModel.getColumn(3).setCellRenderer(numberRenderer);

        DefaultTableCellRenderer booleanRenderer = new DefaultTableCellRenderer() {
            @Override public void setValue(Object value) {
                if (value instanceof Boolean b) {
                    setHorizontalAlignment(SwingConstants.CENTER);
                    setText(b ? "✓" : "");
                } else super.setValue(value);
            }
        };
        columnModel.getColumn(7).setCellRenderer(booleanRenderer);
        columnModel.getColumn(8).setCellRenderer(booleanRenderer);

        progressBar = new JProgressBar(0, 100);
        progressBar.setPreferredSize(new Dimension(getWidth(), 15));
        progressBar.setStringPainted(true);

        btnAbort = new JButton("Suche abbrechen");
        btnDelete = new JButton("Ausgewählte löschen");
        btnLoadAll = new JButton("Alle Ergebnisse laden"); // NEU
        btnDelete.setEnabled(false);

        lblStatus = new JLabel("Gefunden: 0  ", SwingConstants.RIGHT);

        JPanel leftBtns = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftBtns.add(btnAbort);
        leftBtns.add(btnDelete);
        leftBtns.add(btnLoadAll); // NEU

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(leftBtns, BorderLayout.WEST);
        topPanel.add(lblStatus, BorderLayout.EAST);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(resultTable), BorderLayout.CENTER);
        mainPanel.add(progressBar, BorderLayout.SOUTH);
        add(mainPanel);

        btnAbort.addActionListener(e -> {
            if (worker != null && !worker.isDone()) {
                worker.cancel(true);
                controller.cancel();
                progressBar.setIndeterminate(false);
                progressBar.setString("Abgebrochen");
            }
            // Suchdatei sauber schließen
            XMLController.endSearchResults();
        });

        btnDelete.addActionListener(e -> deleteSelectedFiles());

        btnLoadAll.addActionListener(e -> loadAllFromXml()); // NEU

        installContextMenu();

        startSearch();
    }

    private void addHeaderCheckBox(TableColumn checkboxColumn) {
        JCheckBox selectAll = new JCheckBox();
        selectAll.setHorizontalAlignment(SwingConstants.CENTER);
        checkboxColumn.setHeaderRenderer((table, value, isSelected, hasFocus, row, column) -> selectAll);
        selectAll.addActionListener(e -> {
            boolean checked = selectAll.isSelected();
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                tableModel.setValueAt(checked, i, 0);
            }
        });
        resultTable.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int viewColumn = resultTable.columnAtPoint(e.getPoint());
                int modelColumn = resultTable.convertColumnIndexToModel(viewColumn);
                if (modelColumn == 0) {
                    selectAll.doClick();
                    resultTable.getTableHeader().repaint();
                }
            }
        });
    }

    private void startSearch() {
        // Ergebnis-XML frisch beginnen
        XMLController.beginSearchResults(resultsXmlPath, options);
        offloadedCount = 0;
        batchCounter = 0;
        updateStatus();

        progressBar.setValue(0);
        progressBar.setIndeterminate(false);
        btnLoadAll.setEnabled(false);

        worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                controller.searchFiles(
                        selectedFolders,
                        options,
                        model -> { if (!isCancelled()) publish(model); },
                        this::setProgress
                );
                return null;
            }

            @Override
            protected void process(List<FileSearchModel> chunks) {
                for (FileSearchModel model : chunks) handleIncomingResult(model);
            }

            @Override
            protected void done() {
                btnAbort.setVisible(false);
                btnDelete.setEnabled(true);
                progressBar.setIndeterminate(false);
                progressBar.setValue(100);
                // XML sauber schließen
                btnLoadAll.setEnabled(true);
                XMLController.endSearchResults();
            }
        };
        System.out.println();

        worker.addPropertyChangeListener(evt -> {
            if ("progress".equals(evt.getPropertyName())) {
                int value = (Integer) evt.getNewValue();
                progressBar.setValue(value);
            }
        });

        worker.execute();
    }

    /**
     * Nimmt ein Ergebnis entgegen:
     * - bis MAX_ROWS_IN_TABLE ins TableModel
     * - darüber hinaus: in XML auslagern (batchweise)
     */
    private void handleIncomingResult(FileSearchModel model) {
        totalFoundCount++;       // neuen Treffer zählen
        updateStatus();

        if (tableModel.getRowCount() < MAX_ROWS_IN_TABLE) {
            addResultRow(model);
        } else {
            // Auslagern
            XMLController.appendSearchResult(model);
            // offloadedCount++ und batchCounter kannst du komplett entfernen,
            // wenn du die ausgelagerte Zahl nicht mehr brauchst
        }
    }

    private void updateStatus() {
        SwingUtilities.invokeLater(() ->
            lblStatus.setText("Gefunden: " + totalFoundCount + "  ")
        );
    }

    /** Ergebniszeile hinzufügen (sichtbare Tabelle) */
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
            row.add(model.getFile().isFile() && !model.getFile().canWrite());
            row.add(model.getFile().isHidden());
            tableModel.addRow(row);
        });
    }

    /** „Alle Ergebnisse laden“: liest die ausgelagerten XML-Daten und füllt die Tabelle vollständig auf. */
    private void loadAllFromXml() {
        // 1) Bereits sichtbare Zeilen belassen – wir hängen die ausgelagerten *zusätzlich* an.
        //    Falls du stattdessen *nur* aus XML laden willst: tableModel.setRowCount(0);

        XMLController.readSearchResults(resultsXmlPath, rec -> {
            // Falls der Eintrag bereits sichtbar ist, könntest du hier deduplizieren (optional).
            // Wir fügen direkt eine Zeile aus den XML-Attributen hinzu (ohne teure Files.readAttributes):
            Vector<Object> row = new Vector<>();
            row.add(false);
            row.add(rec.name != null && !rec.name.isBlank()
                    ? rec.name
                    : new File(rec.fullPath).getName());
            row.add(rec.parent);
            row.add(rec.sizeBytes);
            row.add(rec.type);
            row.add(rec.created != null && !rec.created.isBlank() ? rec.created : "-");
            row.add(rec.modified != null && !rec.modified.isBlank() ? rec.modified : "-");
            row.add(rec.readOnly);
            row.add(rec.hidden);
            SwingUtilities.invokeLater(() -> tableModel.addRow(row));
        });

        // Optional: Status zurücksetzen/anzeigen
        updateStatus();
        JOptionPane.showMessageDialog(this,
                "Alle ausgelagerten Ergebnisse wurden geladen.",
                "Fertig",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /** Dateien löschen – bestehende Logik belassen/ergänzen */
    private void deleteSelectedFiles() {
        // deine vorherige Delete-Methode hier unverändert
    }

    private void installContextMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem openItem = new JMenuItem("Öffnen");
        JMenuItem showItem = new JMenuItem("Im Ordner anzeigen");
        JMenuItem propsItem = new JMenuItem("Eigenschaften");

        menu.add(openItem);
        menu.add(showItem);
        menu.add(propsItem);

        resultTable.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { showPopup(e); }
            @Override public void mouseReleased(MouseEvent e) { showPopup(e); }
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

        openItem.addActionListener(e -> performAction("open"));
        showItem.addActionListener(e -> performAction("show"));
        propsItem.addActionListener(e -> performAction("props"));
    }

    private void performAction(String action) {
        int row = resultTable.getSelectedRow();
        if (row < 0) return;

        String name = (String) tableModel.getValueAt(row, 1);
        String parent = (String) tableModel.getValueAt(row, 2);
        Path path = (parent == null || parent.isBlank()) ? Paths.get(name) : Paths.get(parent, name);

        try {
            if ("open".equals(action)) {
                Desktop.getDesktop().open(path.toFile());
            } else if ("show".equals(action)) {
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    new ProcessBuilder("explorer.exe", "/select,", path.toAbsolutePath().toString()).start();
                } else if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                    new ProcessBuilder("open", "-R", path.toAbsolutePath().toString()).start();
                } else {
                    Desktop.getDesktop().open(path.getParent().toFile());
                }
            } else if ("props".equals(action)) {
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    new ProcessBuilder("rundll32", "shell32.dll,ShellExec_RunDLL",
                            "properties", path.toAbsolutePath().toString()).start();
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Pfad: " + path.toString(),
                            "Eigenschaften",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Fehler: " + ex.getMessage(),
                    "Aktion fehlgeschlagen",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
