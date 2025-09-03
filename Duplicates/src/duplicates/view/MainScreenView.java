package duplicates.view;

import duplicates.controller.FileAccessController;
import duplicates.model.FolderTreeModel;

import javax.swing.*;
import java.awt.*;

public class MainScreenView extends JFrame {

	/**
	 * 
	 */
    public MainScreenView() {
        super("Duplicate Finder – Suche nach doppelten Dateien");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Menüleiste erstellen
        setJMenuBar(createMenuBar());
        
        // Controller + Baum erstellen
        FileAccessController controller = new FileAccessController();
        JTree folderTree = new JTree();
        FolderTreeModel treeModel = new FolderTreeModel(controller, folderTree);
        folderTree.setModel(treeModel);
        folderTree.setRootVisible(true);
        folderTree.setCellRenderer(new FolderTreeCellRenderer());

        // --- Rechts: ScrollPane mit Verzeichnisbaum
        JScrollPane treeScroll = new JScrollPane(folderTree);

        // --- Oben links: Optionspanel
        JPanel optionsPanel = createOptionsPanel();

        // --- Unten links: Liste der ausgewählten Ordner (Platzhalter)
        DefaultListModel<String> selectedFoldersModel = new DefaultListModel<>();
        JList<String> selectedFoldersList = new JList<>(selectedFoldersModel);
        JScrollPane selectedFoldersScroll = new JScrollPane(selectedFoldersList);

        // Linker Bereich: vertikal geteilt (oben Optionen, unten Liste)
        JSplitPane leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, optionsPanel, selectedFoldersScroll);
        leftSplit.setResizeWeight(0.4); // Oben ca. 40%, unten 60%

        // Haupt-SplitPane: links (Optionen + Liste) und rechts (Baum)
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplit, treeScroll);
        mainSplit.setResizeWeight(0.4); // Linker Bereich ca. 40% der Breite

        add(mainSplit, BorderLayout.CENTER);
        setVisible(true);
    }
    
    /**
     * Menübereich oben links
     * @return
     */
    private JPanel createOptionsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int row = 0;

        // Zeile 1: Min File Size
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Min File Size:"), gbc);
        gbc.gridx = 1;
        panel.add(new JTextField(), gbc);

        // Zeile 2: Max File Size
        gbc.gridx = 0; gbc.gridy = ++row;
        panel.add(new JLabel("Max File Size:"), gbc);
        gbc.gridx = 1;
        panel.add(new JTextField(), gbc);

        // Zeile 3 ff: Checkboxes
        gbc.gridx = 0; gbc.gridy = ++row;
        gbc.gridwidth = 2;
        panel.add(new JCheckBox("Dateigröße"), gbc);

        gbc.gridy = ++row;
        panel.add(new JCheckBox("Dateiname"), gbc);

        gbc.gridy = ++row;
        panel.add(new JCheckBox("Unterordner"), gbc);

        gbc.gridy = ++row;
        panel.add(new JCheckBox("Wildcard 1"), gbc);

        gbc.gridy = ++row;
        panel.add(new JCheckBox("Wildcard 2"), gbc);

        gbc.gridy = ++row;
        panel.add(new JCheckBox("Wildcard 3"), gbc);

        // --- Buttons (Reset / Starten)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        JButton resetButton = new JButton("Reset");
        JButton startButton = new JButton("Starten");

        buttonPanel.add(resetButton);
        buttonPanel.add(startButton);

        gbc.gridy = ++row;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(buttonPanel, gbc);

        return panel;
    }
    
    /**
     * Menübar
     * @return
     */
    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // --- Datei-Menü
        JMenu fileMenu = new JMenu("Datei");
        JMenuItem openItem = new JMenuItem("Öffnen");
        JMenuItem saveItem = new JMenuItem("Speichern");
        JMenuItem exitItem = new JMenuItem("Beenden");

        // Aktion für "Beenden"
        exitItem.addActionListener(e -> {
            // Fenster schließen
            dispose();
            // Programm beenden (optional)
            System.exit(0);
        });

        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // --- Bearbeiten-Menü
        JMenu editMenu = new JMenu("Bearbeiten");
        editMenu.add(new JMenuItem("Ausschneiden"));
        editMenu.add(new JMenuItem("Kopieren"));
        editMenu.add(new JMenuItem("Einfügen"));

        // --- Ansicht-Menü
        JMenu viewMenu = new JMenu("Ansicht");
        viewMenu.add(new JCheckBoxMenuItem("Details anzeigen"));
        viewMenu.add(new JCheckBoxMenuItem("Versteckte Dateien"));

        // --- Hilfe-Menü
        JMenu helpMenu = new JMenu("Hilfe");
        JMenuItem aboutItem = new JMenuItem("Über Duplicates...");
        aboutItem.addActionListener(e ->
                JOptionPane.showMessageDialog(
                        this,
                        "Duplicate Finder v0.1\nAutor: Jörg Hesse\n© 2025",
                        "Über Duplicate Finder",
                        JOptionPane.INFORMATION_MESSAGE
                )
        );
        helpMenu.add(aboutItem);

        // Menüs zur Menüleiste hinzufügen
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(viewMenu);
        menuBar.add(helpMenu);

        return menuBar;
    }
}