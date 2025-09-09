package duplicates.view;

import duplicates.controller.FileAccessController;
import duplicates.controller.XMLController;
import duplicates.model.DuplicateSearchOptionsModel;
import duplicates.model.FileSearchOptionsModel;
import duplicates.model.FolderTreeModel;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MainScreenView extends JFrame {

    private final DefaultListModel<String> selectedFoldersModel = new DefaultListModel<>();
    private final JList<String> selectedFoldersList = new JList<>(selectedFoldersModel);

    private JTabbedPane optionsTabs;
    private DuplicateSearchOptionPanel duplicateOptionsPanel;
    private FileSearchOptionPanel fileSearchOptionsPanel;

    private final FileAccessController controller = new FileAccessController();

    public MainScreenView() {
        super("Duplicates – Dateisuche");
        try {
            // --- 1. Setze Look and Feel auf Windows
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
        
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setJMenuBar(createMenuBar());

        // Tree
        JTree folderTree = new JTree();
        FolderTreeModel treeModel = new FolderTreeModel(controller, folderTree);
        folderTree.setModel(treeModel);
        folderTree.setRootVisible(true);
        folderTree.setCellRenderer(new CheckBoxNodeRenderer());
        folderTree.setCellEditor(new CheckBoxNodeEditor(folderTree));
        folderTree.setEditable(true);
        folderTree.setToggleClickCount(0);
        folderTree.setInvokesStopCellEditing(true);
        folderTree.setRowHeight(0);

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

                if (cbNode.isSelected()) {
                    if (!selectedFoldersModel.contains(folderPath)) {
                        selectedFoldersModel.addElement(folderPath);
                    }
                } else {
                    selectedFoldersModel.removeElement(folderPath);
                }
                treeModel.rememberSelection(folderPath, cbNode.isSelected());
            }
        });

        JScrollPane treeScroll = new JScrollPane(folderTree);

        // Tabs
        optionsTabs = new JTabbedPane();

        duplicateOptionsPanel = new DuplicateSearchOptionPanel();
        optionsTabs.addTab("Duplicate Search", duplicateOptionsPanel);

        fileSearchOptionsPanel = new FileSearchOptionPanel();
        optionsTabs.addTab("File Search", fileSearchOptionsPanel);

        JPanel infoPanel = new JPanel(new BorderLayout());
        JLabel infoLabel = new JLabel(
                "<html><div style='text-align:center;'>Duplicates v0.1<br><br>" +
                        "Tool zum Suchen nach Dateiduplikaten oder Dateien mit Kriterien<br><br>&#169; Jörg Hesse</div></html>",
                SwingConstants.CENTER);
        infoPanel.add(infoLabel, BorderLayout.CENTER);
        optionsTabs.addTab("Info", infoPanel);

        JScrollPane selectedFoldersScroll = new JScrollPane(selectedFoldersList);

        JSplitPane leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, optionsTabs, selectedFoldersScroll);
        leftSplit.setResizeWeight(0.55);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplit, treeScroll);
        mainSplit.setResizeWeight(0.45);

        add(mainSplit, BorderLayout.CENTER);

        setVisible(true);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("Datei");

        JMenuItem loadDuplicateSettings = new JMenuItem("Duplikat-Suche laden");
        loadDuplicateSettings.addActionListener(e -> {
            try {
                DuplicateSearchOptionsModel ds = XMLController.readDSSettingsFromXML();
                if (ds != null) {
                    applyDuplicateSearchSettings(ds);
                    JOptionPane.showMessageDialog(this, "Duplikat-Suchoptionen geladen!",
                            "Info", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Keine Duplikat-Einstellungen gefunden.",
                            "Warnung", JOptionPane.WARNING_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Fehler beim Laden der Duplikat-Einstellungen:\n" + ex.getMessage(),
                        "Fehler", JOptionPane.ERROR_MESSAGE);
            }
        });
        fileMenu.add(loadDuplicateSettings);

        JMenuItem loadFileSearchSettings = new JMenuItem("Dateisuche laden");
        loadFileSearchSettings.addActionListener(e -> {
            try {
                FileSearchOptionsModel fs = XMLController.readFSSettingsFromXML();
                if (fs != null) {
                    applyFileSearchSettings(fs);
                    JOptionPane.showMessageDialog(this, "Datei-Suchoptionen geladen!",
                            "Info", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Keine Datei-Sucheinstellungen gefunden.",
                            "Warnung", JOptionPane.WARNING_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Fehler beim Laden der Datei-Sucheinstellungen:\n" + ex.getMessage(),
                        "Fehler", JOptionPane.ERROR_MESSAGE);
            }
        });
        fileMenu.add(loadFileSearchSettings);

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

    private void applyDuplicateSearchSettings(DuplicateSearchOptionsModel model) {
        if (duplicateOptionsPanel != null) {
            duplicateOptionsPanel.applySettings(model);
            optionsTabs.setSelectedComponent(duplicateOptionsPanel);
        }
    }

    private void applyFileSearchSettings(FileSearchOptionsModel model) {
        if (fileSearchOptionsPanel != null) {
            fileSearchOptionsPanel.applySettings(model);
            optionsTabs.setSelectedComponent(fileSearchOptionsPanel);
        }
    }

    /** Hilfsmethode: ausgewählte Ordner holen */
    private List<String> getSelectedFolders() {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < selectedFoldersModel.size(); i++) {
            list.add(selectedFoldersModel.get(i));
        }
        return list;
    }
}
