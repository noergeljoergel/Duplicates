package duplicates.view;

import duplicates.controller.FileAccessController;
import duplicates.model.FolderTreeModel;
import duplicates.model.CheckBoxNode;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;

public class MainScreenView extends JFrame {

    // ✅ Feld für die Liste der ausgewählten Ordner
    private final DefaultListModel<String> selectedFoldersModel = new DefaultListModel<>();
    private final JList<String> selectedFoldersList = new JList<>(selectedFoldersModel);

    public MainScreenView() {
        super("Duplicates – Dateisuche");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Menüleiste hinzufügen
        setJMenuBar(createMenuBar());

        // --- Controller + Baum erstellen
        FileAccessController controller = new FileAccessController();
        JTree folderTree = new JTree();
        FolderTreeModel treeModel = new FolderTreeModel(controller, folderTree);
        folderTree.setModel(treeModel);
        folderTree.setRootVisible(true);

        // Renderer + Editor setzen (Checkboxen)
        folderTree.setCellRenderer(new CheckBoxNodeRenderer());
        folderTree.setCellEditor(new CheckBoxNodeEditor());
        folderTree.setEditable(true);

        // ✅ Listener: Auswahländerungen in Liste unten eintragen
        folderTree.getCellEditor().addCellEditorListener(new javax.swing.event.CellEditorListener() {
            @Override
            public void editingStopped(javax.swing.event.ChangeEvent e) {
                Object value = folderTree.getLastSelectedPathComponent();
                if (value instanceof javax.swing.tree.DefaultMutableTreeNode node &&
                    node.getUserObject() instanceof duplicates.model.CheckBoxNode cbNode) {

                    String folderPath = cbNode.getFile().getAbsolutePath();
                    if (cbNode.isSelected()) {
                        if (!selectedFoldersModel.contains(folderPath)) {
                            selectedFoldersModel.addElement(folderPath);
                        }
                    } else {
                        selectedFoldersModel.removeElement(folderPath);
                    }
                }
            }

            @Override
            public void editingCanceled(javax.swing.event.ChangeEvent e) {
                // nichts tun
            }
        });


        // --- Rechte Seite: ScrollPane mit Ordnerbaum
        JScrollPane treeScroll = new JScrollPane(folderTree);

        // --- Linke Seite: oben Optionen, unten Liste
        JPanel optionsPanel = createOptionsPanel();
        JScrollPane selectedFoldersScroll = new JScrollPane(selectedFoldersList);

        JSplitPane leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, optionsPanel, selectedFoldersScroll);
        leftSplit.setResizeWeight(0.4);

        // --- Haupt-SplitPane
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplit, treeScroll);
        mainSplit.setResizeWeight(0.4);

        add(mainSplit, BorderLayout.CENTER);
        setVisible(true);
    }

    private JPanel createOptionsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int row = 0;
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Min File Size:"), gbc);
        gbc.gridx = 1;
        panel.add(new JTextField(), gbc);

        gbc.gridx = 0; gbc.gridy = ++row;
        panel.add(new JLabel("Max File Size:"), gbc);
        gbc.gridx = 1;
        panel.add(new JTextField(), gbc);

        gbc.gridx = 0; gbc.gridy = ++row; gbc.gridwidth = 2;
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

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        buttonPanel.add(new JButton("Reset"));
        buttonPanel.add(new JButton("Starten"));

        gbc.gridy = ++row;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(buttonPanel, gbc);

        return panel;
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("Datei");
        JMenuItem exitItem = new JMenuItem("Beenden");
        exitItem.addActionListener(e -> {
            dispose();
            System.exit(0);
        });
        fileMenu.add(exitItem);

        JMenu editMenu = new JMenu("Bearbeiten");
        JMenu viewMenu = new JMenu("Ansicht");
        JMenu helpMenu = new JMenu("Hilfe");
        JMenuItem aboutItem = new JMenuItem("Über Duplicates...");
        aboutItem.addActionListener(e ->
                JOptionPane.showMessageDialog(this, "Duplicates v1.0\nAutor: Du", "Über Duplicates", JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(viewMenu);
        menuBar.add(helpMenu);
        return menuBar;
    }
}