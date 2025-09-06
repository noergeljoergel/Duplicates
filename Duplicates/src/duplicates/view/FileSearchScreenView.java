package duplicates.view;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

/**
 * Fenster zur Anzeige von Suchergebnissen einer Dateisuche.
 * Spalten: Name, Pfad, Größe, Typ, Erstellungsdatum, Änderungsdatum, Aktion(Löschen).
 */
public class FileSearchScreenView extends JFrame {

    // Tabelle + Model
    private final FileResultTableModel tableModel = new FileResultTableModel();
    private final JTable table = new JTable(tableModel);
    private final TableRowSorter<FileResultTableModel> sorter = new TableRowSorter<>(tableModel);

    // UI-Elemente
    private final JTextField filterField = new JTextField();
    private final JLabel statusLabel = new JLabel("Bereit");

    private final ZoneId zone = ZoneId.systemDefault();

    public FileSearchScreenView() {
        super("File Search – Ergebnisse");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1050, 600);
        setLocationRelativeTo(null);

        // Tabelle konfigurieren
        table.setRowSorter(sorter);
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        // Renderer für Größe (rechtsbündig & lesbares Format)
        table.getColumnModel().getColumn(2).setCellRenderer(new SizeRenderer());
        // Renderer für Datumsangaben
        DateRenderer dateRenderer = new DateRenderer(zone);
        table.getColumnModel().getColumn(4).setCellRenderer(dateRenderer);
        table.getColumnModel().getColumn(5).setCellRenderer(dateRenderer);

        // "Löschen"-Spalte: Renderer + Editor (Button)
        int actionCol = 6;
        table.getColumnModel().getColumn(actionCol).setCellRenderer(new DeleteButtonRenderer());
        table.getColumnModel().getColumn(actionCol).setCellEditor(new DeleteButtonEditor());

        // Spaltenbreiten grob
        table.getColumnModel().getColumn(0).setPreferredWidth(240); // Name
        table.getColumnModel().getColumn(1).setPreferredWidth(420); // Pfad
        table.getColumnModel().getColumn(2).setPreferredWidth(90);  // Größe
        table.getColumnModel().getColumn(3).setPreferredWidth(120); // Typ
        table.getColumnModel().getColumn(6).setPreferredWidth(90);  // Aktion

        // Filterzeile
        JPanel filterPanel = new JPanel(new BorderLayout(8, 8));
        filterPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        filterPanel.add(new JLabel("Filter (Name/Pfad):"), BorderLayout.WEST);
        filterPanel.add(filterField, BorderLayout.CENTER);
        filterField.getDocument().addDocumentListener((SimpleDocumentListener) e -> applyFilter());

        // Scrollpane
        JScrollPane scroll = new JScrollPane(table);

        // Statusleiste
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        statusPanel.add(statusLabel, BorderLayout.WEST);

        // Layout
        setLayout(new BorderLayout());
        add(filterPanel, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);

        // Doppelklick: Datei im OS öffnen (optional)
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    int viewRow = table.getSelectedRow();
                    int modelRow = table.convertRowIndexToModel(viewRow);
                    FileRecord rec = tableModel.getAt(modelRow);
                    openInDesktop(rec.file().toPath());
                }
            }
        });
    }

    /** Übergib eine Liste von Files, um die Tabelle zu füllen. */
    public void setResults(List<File> files) {
        List<FileRecord> recs = files == null ? List.of()
                : files.stream().filter(Objects::nonNull)
                .map(File::toPath)
                .map(this::toRecordSafe)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        tableModel.setData(recs);
        updateStatus();
    }

    /** Optional: Pfade statt Files. */
    public void setResultsFromPaths(List<Path> paths) {
        List<FileRecord> recs = paths == null ? List.of()
                : paths.stream().filter(Objects::nonNull)
                .map(this::toRecordSafe)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        tableModel.setData(recs);
        updateStatus();
    }

    // ---------- Helpers ----------

    private void applyFilter() {
        String text = filterField.getText();
        if (text == null || text.isBlank()) {
            sorter.setRowFilter(null);
        } else {
            String needle = Pattern.quote(text.trim());
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + needle, 0, 1)); // Name + Pfad
        }
        updateStatus();
    }

    private void updateStatus() {
        int visible = table.getRowCount();
        long total = tableModel.getRowCount();
        long bytes = tableModel.visibleBytes(sorter, table);
        statusLabel.setText(String.format("Angezeigt: %d / %d   Gesamtgröße (gefiltert): %s",
                visible, total, humanSize(bytes)));
    }

    private FileRecord toRecordSafe(Path p) {
        try {
            if (p == null) return null;
            if (!Files.isRegularFile(p)) return null;

            BasicFileAttributes attrs = Files.readAttributes(p, BasicFileAttributes.class);
            String name = p.getFileName() != null ? p.getFileName().toString() : p.toString();
            String path = p.getParent() != null ? p.getParent().toString() : p.toString();
            long size = attrs.size();

            String type = Files.probeContentType(p);
            if (type == null || type.isBlank()) {
                type = extensionOf(name).orElse("Unbekannt");
            }

            Instant created = safeInstant(attrs.creationTime() != null ? attrs.creationTime().toMillis() : 0L);
            Instant modified = safeInstant(attrs.lastModifiedTime() != null ? attrs.lastModifiedTime().toMillis() : 0L);

            return new FileRecord(p.toFile(), name, path, size, type, created, modified);
        } catch (Exception ex) {
            // Datei könnte währenddessen verschwunden sein oder Rechte fehlen -> einfach überspringen
            return null;
        }
    }

    private static Optional<String> extensionOf(String filename) {
        if (filename == null) return Optional.empty();
        int dot = filename.lastIndexOf('.');
        return (dot > 0 && dot < filename.length() - 1)
                ? Optional.of(filename.substring(dot + 1).toLowerCase(Locale.ROOT))
                : Optional.empty();
    }

    private static Instant safeInstant(long epochMillis) {
        try {
            return (epochMillis > 0) ? Instant.ofEpochMilli(epochMillis) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static void openInDesktop(Path path) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(path.toFile());
            }
        } catch (Exception ignored) {}
    }

    private static String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int unit = 1024;
        String[] units = {"KB", "MB", "GB", "TB", "PB"};
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = units[exp - 1];
        return String.format(Locale.getDefault(), "%.1f %s", bytes / Math.pow(unit, exp), pre);
    }

    // ---------- Model & Renderer/Editor für Button ----------

    private record FileRecord(File file, String name, String path,
                              long size, String type, Instant created, Instant modified) { }

    private static class FileResultTableModel extends AbstractTableModel {
        private final String[] columns = {
                "Name", "Pfad", "Größe", "Typ", "Erstellt", "Geändert", "Aktion"
        };
        private final Class<?>[] types = {
                String.class, String.class, Long.class, String.class, Instant.class, Instant.class, Object.class
        };
        private List<FileRecord> data = new ArrayList<>();

        public void setData(List<FileRecord> records) {
            this.data = new ArrayList<>(records);
            fireTableDataChanged();
        }

        public FileRecord getAt(int modelRow) {
            return data.get(modelRow);
        }

        public void removeAt(int modelRow) {
            data.remove(modelRow);
            fireTableRowsDeleted(modelRow, modelRow);
        }

        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int column) { return columns[column]; }
        @Override public Class<?> getColumnClass(int columnIndex) { return types[columnIndex]; }
        @Override public boolean isCellEditable(int rowIndex, int columnIndex) { return columnIndex == 6; }

        @Override public Object getValueAt(int row, int col) {
            FileRecord r = data.get(row);
            return switch (col) {
                case 0 -> r.name();
                case 1 -> r.path();
                case 2 -> r.size();
                case 3 -> r.type();
                case 4 -> r.created();
                case 5 -> r.modified();
                case 6 -> "Löschen"; // Button-Text
                default -> null;
            };
        }

        /** Summe der sichtbaren Bytes (für Status) */
        long visibleBytes(TableRowSorter<FileResultTableModel> sorter, JTable table) {
            if (sorter == null || table == null) return 0;
            long sum = 0;
            for (int i = 0; i < table.getRowCount(); i++) {
                int modelRow = table.convertRowIndexToModel(i);
                sum += data.get(modelRow).size();
            }
            return sum;
        }
    }

    /** Renderer: Zeigt den Button „Löschen“. */
    private static class DeleteButtonRenderer extends JButton implements TableCellRenderer {
        DeleteButtonRenderer() { setText("Löschen"); }
        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                                 boolean hasFocus, int row, int column) {
            setText("Löschen");
            return this;
        }
    }

    /** Editor: Klick auf Button führt Sicherheitsabfrage + Löschen aus. */
    private class DeleteButtonEditor extends AbstractCellEditor implements TableCellEditor {
        private final JButton button = new JButton("Löschen");
        private int editingRowView = -1;

        DeleteButtonEditor() {
            button.addActionListener(e -> {
                if (editingRowView < 0) return;
                int modelRow = table.convertRowIndexToModel(editingRowView);
                deleteRecord(modelRow);
                fireEditingStopped();
            });
        }

        @Override public Object getCellEditorValue() { return "Löschen"; }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
                                                     int row, int column) {
            editingRowView = row;
            return button;
        }
    }

    /** Löscht/verschiebt Datei in Papierkorb nach Bestätigung und aktualisiert Tabelle/Status. */
    private void deleteRecord(int modelRow) {
        if (modelRow < 0 || modelRow >= tableModel.getRowCount()) return;

        FileRecord rec = tableModel.getAt(modelRow);
        File f = rec.file();

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Diese Datei löschen?\n" + new File(rec.path(), rec.name()).getAbsolutePath(),
                "Löschen bestätigen",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (choice != JOptionPane.YES_OPTION) return;

        boolean ok = false;
        Exception error = null;

        // 1) Papierkorb versuchen (Java 9+)
        try {
            if (Desktop.isDesktopSupported()) {
                try {
                    ok = Desktop.getDesktop().moveToTrash(f);
                } catch (UnsupportedOperationException ignored) {
                    // Plattform unterstützt moveToTrash nicht -> fallback unten
                }
            }
        } catch (Exception e) {
            error = e;
        }

        // 2) Fallback: hart löschen
        if (!ok) {
            try {
                ok = Files.deleteIfExists(f.toPath());
            } catch (Exception e) {
                error = e;
            }
        }

        if (ok) {
            tableModel.removeAt(modelRow);
            updateStatus();
            JOptionPane.showMessageDialog(this, "Datei wurde gelöscht.",
                    "Info", JOptionPane.INFORMATION_MESSAGE);
        } else {
            String msg = "Datei konnte nicht gelöscht werden.";
            if (error != null && error.getMessage() != null && !error.getMessage().isBlank()) {
                msg += "\nGrund: " + error.getMessage();
            }
            JOptionPane.showMessageDialog(this, msg, "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ---------- Renderer für Größe & Datum ----------

    private static class SizeRenderer extends DefaultTableCellRenderer {
        @Override protected void setValue(Object value) {
            if (value instanceof Number n) {
                setHorizontalAlignment(SwingConstants.RIGHT);
                setText(human(n.longValue()));
            } else {
                super.setValue(value);
            }
        }
        private static String human(long bytes) {
            if (bytes < 1024) return bytes + " B";
            int unit = 1024;
            String[] units = {"KB", "MB", "GB", "TB", "PB"};
            int exp = (int) (Math.log(bytes) / Math.log(unit));
            String pre = units[exp - 1];
            return String.format(Locale.getDefault(), "%.1f %s", bytes / Math.pow(unit, exp), pre);
        }
    }

    private static class DateRenderer extends DefaultTableCellRenderer {
        private final DateTimeFormatter fmt;
        private final ZoneId zone;
        DateRenderer(ZoneId zone) {
            this.zone = zone;
            this.fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        }
        @Override protected void setValue(Object value) {
            if (value instanceof Instant i) {
                LocalDateTime ldt = LocalDateTime.ofInstant(i, zone);
                setText(fmt.format(ldt));
            } else {
                setText("");
            }
        }
    }

    // Kleiner Helfer für DocumentListener mit Lambda
    private interface SimpleDocumentListener extends javax.swing.event.DocumentListener {
        void update(javax.swing.event.DocumentEvent e);
        @Override default void insertUpdate(javax.swing.event.DocumentEvent e) { update(e); }
        @Override default void removeUpdate(javax.swing.event.DocumentEvent e) { update(e); }
        @Override default void changedUpdate(javax.swing.event.DocumentEvent e) { update(e); }
    }
}
