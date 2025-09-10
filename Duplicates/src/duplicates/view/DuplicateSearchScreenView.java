package duplicates.view;

import duplicates.controller.DuplicateSearchController;
import duplicates.model.DuplicateSearchModel;
import duplicates.model.DuplicateSearchOptionsModel;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.text.NumberFormat;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fenster für die Duplikat-Suche.
 * Zeigt Suchfortschritt und gruppierte Duplikate in einer Tabelle.
 */
public class DuplicateSearchScreenView extends JFrame {

    private final DuplicateSearchOptionsModel options;
    private final List<String> selectedFolders;
    private final DuplicateSearchController controller = new DuplicateSearchController();

    private final JTable resultTable;
    private final DefaultTableModel tableModel;
    private final JProgressBar progressBar;
    private final JButton btnAbort;
    private final JButton btnDelete;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * Konstruktor: Übergabe der Suchoptionen und der zu durchsuchenden Ordner.
     */
    public DuplicateSearchScreenView(DuplicateSearchOptionsModel options, List<String> selectedFolders) {
        super("Duplikat-Suche");
        this.options = options;
        this.selectedFolders = selectedFolders;

        setSize(1024, 768);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // --- 1. Tabellenmodell erstellen ---
        String[] columns = {
                "Gruppe", "✓", "Dateiname", "Dateipfad", "Größe (Byte)", "Typ", "Erstellt", "Geändert"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return switch (columnIndex) {
                    case 0 -> Integer.class; // Gruppennummer
                    case 1 -> Boolean.class; // Checkbox
                    case 4 -> Long.class;    // Dateigröße
                    default -> String.class;
                };
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 1; // Nur Checkbox editierbar
            }
        };

        resultTable = new JTable(tableModel);
        resultTable.setAutoCreateRowSorter(true);
        resultTable.setFillsViewportHeight(true);

        // --- 2. Spaltenbreiten optimieren ---
        TableColumnModel columnModel = resultTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(50);
        columnModel.getColumn(1).setPreferredWidth(30);
        columnModel.getColumn(2).setPreferredWidth(160);
        columnModel.getColumn(3).setPreferredWidth(330);
        columnModel.getColumn(4).setPreferredWidth(90);
        columnModel.getColumn(5).setPreferredWidth(80);
        columnModel.getColumn(6).setPreferredWidth(100);
        columnModel.getColumn(7).setPreferredWidth(100);

        // --- 2.1 Zahlenformat für Dateigröße ---
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
        columnModel.getColumn(4).setCellRenderer(numberRenderer);

        // --- 3. Fortschrittsbalken ---
        progressBar = new JProgressBar(0, 100);
        progressBar.setPreferredSize(new Dimension(getWidth(), 15));
        progressBar.setStringPainted(true);

        // --- 4. Buttons ---
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

        // --- 5. Layout ---
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(buttonPanel, BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(resultTable), BorderLayout.CENTER);
        mainPanel.add(progressBar, BorderLayout.SOUTH);
        add(mainPanel);

        // --- 6. Event-Handler ---
        btnAbort.addActionListener(e -> dispose());
        btnDelete.addActionListener(e -> deleteSelectedFiles());

        // --- 7. Suche starten ---
        startSearch();
    }

    /**
     * Startet die Duplikat-Suche asynchron.
     */
    private void startSearch() {
        executor.submit(() -> {
            controller.searchDuplicates(options, selectedFolders,
                    this::addDuplicateResult,
                    this::updateProgress);
            SwingUtilities.invokeLater(() -> {
                btnAbort.setVisible(false);
                btnDelete.setEnabled(true);
            });
        });
    }

    /**
     * Fügt eine neue Zeile mit einem gefundenen Duplikat hinzu.
     */
    private void addDuplicateResult(DuplicateSearchModel model, int groupId) {
        SwingUtilities.invokeLater(() -> {
            Vector<Object> row = new Vector<>();
            row.add(groupId);
            row.add(false);
            row.add(model.getFileName());
            row.add(model.getParentPath());
            row.add(model.getFileSizeBytes());
            row.add(model.getFileType());
            row.add(model.getCreationDate() != null ? model.getCreationDate().toString() : "-");
            row.add(model.getModificationDate() != null ? model.getModificationDate().toString() : "-");
            tableModel.addRow(row);
        });
    }

    /**
     * Aktualisiert den Fortschrittsbalken.
     */
    private void updateProgress(int progress) {
        SwingUtilities.invokeLater(() -> progressBar.setValue(progress));
    }

    /**
     * Löscht die ausgewählten Duplikate (Platzhalter).
     */
    private void deleteSelectedFiles() {
        int rowCount = tableModel.getRowCount();
        for (int i = rowCount - 1; i >= 0; i--) {
            Boolean selected = (Boolean) tableModel.getValueAt(i, 1);
            if (Boolean.TRUE.equals(selected)) {
                String filePath = tableModel.getValueAt(i, 3) + "/" + tableModel.getValueAt(i, 2);
                System.out.println("[DEBUG] Lösche Duplikat: " + filePath);
                tableModel.removeRow(i);
            }
        }
    }
}
