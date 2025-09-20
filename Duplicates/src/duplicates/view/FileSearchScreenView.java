package duplicates.view;

import duplicates.controller.FileSearchController;
import duplicates.model.FileSearchModel;
import duplicates.model.FileSearchOptionsModel;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;

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

    // --- Whitelist für "Öffnen" ---
    private static final Set<String> OPENABLE_EXTENSIONS = Set.of(
            "exe", "bat", "com", "msi",
            "txt", "log", "csv", "xml", "json", "ini",
            "jpg", "jpeg", "png", "gif", "bmp", "tiff",
            "mp3", "wav", "flac", "ogg",
            "mp4", "mov", "avi", "mkv", "wmv", "webm",
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx"
    );

    public FileSearchScreenView(FileSearchOptionsModel options, List<String> selectedFolders) {
        super("Dateisuche");
        this.options = options;
        this.selectedFolders = selectedFolders;

        setSize(1024, 768);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // --- Tabelle ---
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
            public boolean isCellEditable(int row, int column) {
                return column == 0;
            }
        };

        resultTable = new JTable(tableModel);
        resultTable.setAutoCreateRowSorter(true);
        resultTable.setFillsViewportHeight(true);

        // Spaltenbreiten
        TableColumnModel columnModel = resultTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(30);
        columnModel.getColumn(1).setPreferredWidth(170);
        columnModel.getColumn(2).setPreferredWidth(330);

        addHeaderCheckBox(columnModel.getColumn(0));

        // Renderer für Größe
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

        // Renderer für Boolean
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

        // --- Fortschrittsbalken ---
        progressBar = new JProgressBar(0, 100);
        progressBar.setPreferredSize(new Dimension(getWidth(), 15));
        progressBar.setStringPainted(true);

        // --- Buttons ---
        btnAbort = new JButton("Suche abbrechen");
        btnDelete = new JButton("Ausgewählte löschen");

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(btnAbort);
        buttonPanel.add(btnDelete);
        btnDelete.setEnabled(false);

        // --- Layout ---
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(buttonPanel, BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(resultTable), BorderLayout.CENTER);
        mainPanel.add(progressBar, BorderLayout.SOUTH);
        add(mainPanel);

        // --- Events ---
        btnAbort.addActionListener(e -> {
            if (worker != null && !worker.isDone()) {
                worker.cancel(true);
                controller.cancel();
                progressBar.setIndeterminate(false);
                progressBar.setString("Abgebrochen");
            }
        });
        btnDelete.addActionListener(e -> deleteSelectedFiles());

        // Kontextmenü hinzufügen
        installContextMenu();

        // --- Suche starten ---
        startSearch();
    }

    /** Header-Checkbox **/
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

    /** Suche starten **/
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
                for (FileSearchModel model : chunks) addResultRow(model);
            }

            @Override
            protected void done() {
                btnAbort.setVisible(false);
                btnDelete.setEnabled(true);
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

    /** Ergebniszeile hinzufügen **/
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

    /** Dateien löschen **/
    private void deleteSelectedFiles() {
        // deine vorherige Delete-Methode hier unverändert
    }

    /** Kontextmenü **/
    private void installContextMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem openItem = new JMenuItem("Öffnen");
        JMenuItem showItem = new JMenuItem("Im Ordner anzeigen");
        JMenuItem cutItem = new JMenuItem("Ausschneiden");
        JMenuItem copyItem = new JMenuItem("Kopieren");
        JMenuItem deleteItem = new JMenuItem("Löschen");
        JMenuItem deleteDirItem = new JMenuItem("Verzeichnis löschen");
        JMenuItem propsItem = new JMenuItem("Eigenschaften");

        // hinzufügen in Blöcken
        menu.add(openItem);
        menu.add(showItem);
        menu.addSeparator();
        menu.add(cutItem);
        menu.add(copyItem);
        menu.add(deleteItem);
        menu.add(deleteDirItem);
        menu.addSeparator();
        menu.add(propsItem);

        resultTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                showPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                showPopup(e);
            }

            private void showPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = resultTable.rowAtPoint(e.getPoint());
                    if (row >= 0 && row < resultTable.getRowCount()) {
                        resultTable.setRowSelectionInterval(row, row);

                        // Pfad der Datei
                        String name = (String) tableModel.getValueAt(row, 1);
                        String parent = (String) tableModel.getValueAt(row, 2);
                        Path path = (parent == null || parent.isBlank())
                                ? Paths.get(name)
                                : Paths.get(parent, name);
                        File file = path.toFile();

                        // --- Aktivierung prüfen ---
                        openItem.setEnabled(isOpenableFile(file));
                        deleteDirItem.setEnabled(!isRootDirectory(file));

                        menu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });

        // Aktionen
        openItem.addActionListener(e -> performAction("open"));
        showItem.addActionListener(e -> performAction("show"));
        deleteItem.addActionListener(e -> performAction("delete"));
        deleteDirItem.addActionListener(e -> performAction("deleteDir"));
        propsItem.addActionListener(e -> performAction("props"));
    }

    /** Aktionen **/
    private void performAction(String action) {
        int row = resultTable.getSelectedRow();
        if (row < 0) return;

        String name = (String) tableModel.getValueAt(row, 1);
        String parent = (String) tableModel.getValueAt(row, 2);
        Path path = (parent == null || parent.isBlank())
                ? Paths.get(name)
                : Paths.get(parent, name);

        try {
            switch (action) {
                case "open" -> Desktop.getDesktop().open(path.toFile());
                case "show" -> {
                    if (System.getProperty("os.name").toLowerCase().contains("win")) {
                        new ProcessBuilder("explorer.exe", "/select,", path.toAbsolutePath().toString()).start();
                    } else if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                        new ProcessBuilder("open", "-R", path.toAbsolutePath().toString()).start();
                    } else {
                        Desktop.getDesktop().open(path.getParent().toFile());
                    }
                }
                case "delete" -> {
                    int confirm = JOptionPane.showConfirmDialog(this,
                            "Datei wirklich löschen?\n" + path,
                            "Löschen bestätigen",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE);
                    if (confirm == JOptionPane.YES_OPTION) {
                        Files.deleteIfExists(path);
                    }
                }
                case "deleteDir" -> {
                    Path dirPath = path.getParent();
                    int confirm = JOptionPane.showConfirmDialog(this,
                            "Soll das gesamte Verzeichnis wirklich gelöscht werden?\n" + dirPath,
                            "Verzeichnis löschen",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE);
                    if (confirm != JOptionPane.YES_OPTION) return;

                    String input = JOptionPane.showInputDialog(this,
                            "Bitte tippe 'löschen' ein, um den Vorgang zu bestätigen:",
                            "Sicherheitsabfrage",
                            JOptionPane.WARNING_MESSAGE);
                    if (input == null || !input.equalsIgnoreCase("löschen")) {
                        JOptionPane.showMessageDialog(this,
                                "Löschvorgang abgebrochen.",
                                "Abbruch",
                                JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }

                    Files.walk(dirPath)
                            .sorted((p1, p2) -> p2.compareTo(p1))
                            .forEach(p -> {
                                try {
                                    Files.deleteIfExists(p);
                                } catch (IOException ex) {
                                    System.err.println("Fehler beim Löschen: " + p + " - " + ex.getMessage());
                                }
                            });
                }
                case "props" -> SwingUtilities.invokeLater(() -> new FilePropertiesView(path.toFile()).setVisible(true));
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Fehler: " + ex.getMessage(),
                    "Aktion fehlgeschlagen",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // --- Hilfsfunktionen ---
    private boolean isOpenableFile(File file) {
        String name = file.getName().toLowerCase();
        int dot = name.lastIndexOf('.');
        if (dot == -1) return false;
        String ext = name.substring(dot + 1);
        return OPENABLE_EXTENSIONS.contains(ext);
    }

    private boolean isRootDirectory(File file) {
        Path parent = file.toPath().getParent();
        return parent == null || parent.getParent() == null;
    }
}
