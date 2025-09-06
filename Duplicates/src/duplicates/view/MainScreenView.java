package duplicates.view;

import duplicates.controller.FileAccessController;
import duplicates.controller.XMLController;
import duplicates.model.FolderTreeModel;

import javax.swing.*;
import java.awt.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;

public class MainScreenView extends JFrame {

    // Feld für die Liste der ausgewählten Ordner
    private final DefaultListModel<String> selectedFoldersModel = new DefaultListModel<>();
    private final JList<String> selectedFoldersList = new JList<>(selectedFoldersModel);

    // Oberer linker Bereich als Tabs
    private JTabbedPane optionsTabs;
    // Reiter-Inhalte
    private DuplicateSearchOptionFrame duplicateOptionsPanel;
    private FileSearchOptionFrame fileSearchOptionsPanel; // existiert bereits

    public MainScreenView() {
        super("Duplicates – Dateisuche");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Menüleiste hinzufügen
        setJMenuBar(createMenuBar());

        // Controller + Baum erstellen
        FileAccessController controller = new FileAccessController();
        JTree folderTree = new JTree();
        FolderTreeModel treeModel = new FolderTreeModel(controller, folderTree);
        folderTree.setModel(treeModel);
        folderTree.setRootVisible(true);

        // Renderer + Editor setzen (Checkboxen)
        folderTree.setCellRenderer(new CheckBoxNodeRenderer());
        folderTree.setCellEditor(new CheckBoxNodeEditor(folderTree));
        folderTree.setEditable(true);
        folderTree.setToggleClickCount(0);
        folderTree.setInvokesStopCellEditing(true);
        folderTree.setRowHeight(0); // automatische Höhe

        // Listener: Auswahländerungen in Liste unten eintragen
        // -> deterministisch über TreeModelListener (nicht über Selektion)
        treeModel.addTreeModelListener(new TreeModelListener() {
            @Override public void treeNodesChanged(TreeModelEvent e) {
                Object[] children = e.getChildren();
                if (children == null || children.length == 0) {
                    handleNode(e.getTreePath().getLastPathComponent());
                } else {
                    for (Object child : children) handleNode(child);
                }
            }
            @Override public void treeNodesInserted(TreeModelEvent e) {}
            @Override public void treeNodesRemoved(TreeModelEvent e) {}
            @Override public void treeStructureChanged(TreeModelEvent e) {}

            private void handleNode(Object obj) {
                if (!(obj instanceof DefaultMutableTreeNode node)) return;
                Object uo = node.getUserObject();
                if (!(uo instanceof duplicates.model.CheckBoxNode cbNode)) return;

                String folderPath = cbNode.getFile().getAbsolutePath();

                // ✅ Auswahl in der Liste
                if (cbNode.isSelected()) {
                    if (!selectedFoldersModel.contains(folderPath)) {
                        selectedFoldersModel.addElement(folderPath);
                    }
                } else {
                    selectedFoldersModel.removeElement(folderPath);
                }

                // ✅ Auswahl im Model cachen (wichtig für Lazy-Loading)
                treeModel.rememberSelection(folderPath, cbNode.isSelected());
            }
        });

        // Rechte Seite: ScrollPane mit Ordnerbaum
        JScrollPane treeScroll = new JScrollPane(folderTree);

        // Linke Seite: oben Reiter mit Optionen, unten Liste
        optionsTabs = new JTabbedPane();

        // Reiter 1: Duplicate Search
        duplicateOptionsPanel = new DuplicateSearchOptionFrame();
        optionsTabs.addTab("Duplicate Search", duplicateOptionsPanel);

        // Reiter 2: File Search (deine bestehende Klasse)
        fileSearchOptionsPanel = new FileSearchOptionFrame();
        optionsTabs.addTab("File Search", fileSearchOptionsPanel);

        // Reiter 3: Info (zentrierter Text)
        JPanel infoPanel = new JPanel(new BorderLayout());
        JLabel infoLabel = new JLabel(
        	    "<html><div style='text-align:center;'>Duplicates v0.1<br><br>Dies ist ein Tool, zum Suchen nach Dateiduplikaten oder zum Suchen nach Dateien nach bestimmten Kriterien <br><br>&#169; Jörg Hesse</div></html>",
        	    SwingConstants.CENTER
        	);

   		
        
        
        infoPanel.add(infoLabel, BorderLayout.CENTER);
        optionsTabs.addTab("Info", infoPanel);

        JScrollPane selectedFoldersScroll = new JScrollPane(selectedFoldersList);

        JSplitPane leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, optionsTabs, selectedFoldersScroll);
        leftSplit.setResizeWeight(0.4);

        // Haupt-SplitPane
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplit, treeScroll);
        mainSplit.setResizeWeight(0.4);

        add(mainSplit, BorderLayout.CENTER);
        setVisible(true);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("Datei");

        // Neuer Menüeintrag "Einstellungen laden"
        JMenuItem loadSettingsItem = new JMenuItem("Einstellungen laden");
        loadSettingsItem.addActionListener(e -> {
            try {
                Object[] settings = XMLController.readSettingsFromXML();
                if (settings != null && settings.length == 7) {
                    applySettings(
                            (double) settings[0],
                            (double) settings[1],
                            (String) settings[2],
                            (boolean) settings[3],
                            (boolean) settings[4],
                            (boolean) settings[5],
                            (boolean) settings[6]
                    );
                    JOptionPane.showMessageDialog(this, "Einstellungen geladen!",
                            "Info", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Keine gültigen Einstellungen gefunden.",
                            "Warnung", JOptionPane.WARNING_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Fehler beim Laden der Einstellungen:\n" + ex.getMessage(),
                        "Fehler", JOptionPane.ERROR_MESSAGE);
            }
        });
        fileMenu.add(loadSettingsItem);

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> {
            dispose();
            System.exit(0);
        });
        fileMenu.add(exitItem);

        JMenu editMenu = new JMenu("Bearbeiten");
        JMenu viewMenu = new JMenu("View");
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About Duplicates...");
        aboutItem.addActionListener(e ->
                JOptionPane.showMessageDialog(this, "Duplicates v0.1\n\n© Jörg Hesse",
                        "Über Duplicates", JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(viewMenu);
        menuBar.add(helpMenu);
        return menuBar;
    }

    // Hilfsmethode: Settings in UI eintragen (delegiert an den aktiven Options-Frame für Duplikatsuche)
    private void applySettings(double min, double max, String ext,
                               boolean fileSize, boolean fileName,
                               boolean subFolder, boolean fExt) {
        if (duplicateOptionsPanel != null) {
            duplicateOptionsPanel.applySettings(min, max, ext, fileSize, fileName, subFolder, fExt);
            // Optional: gleich auf den passenden Reiter springen
            optionsTabs.setSelectedComponent(duplicateOptionsPanel);
        }
    }
}
