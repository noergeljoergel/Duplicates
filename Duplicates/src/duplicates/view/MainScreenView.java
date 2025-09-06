package duplicates.view;

import duplicates.controller.FileAccessController;
import duplicates.controller.XMLController;
import duplicates.model.FolderTreeModel;

import javax.swing.*;
import java.awt.*;
import javax.swing.text.NumberFormatter;
import java.text.NumberFormat;
import javax.swing.text.DefaultFormatter;


public class MainScreenView extends JFrame {

    // ✅ Feld für die Liste der ausgewählten Ordner
    private final DefaultListModel<String> selectedFoldersModel = new DefaultListModel<>();
    private final JList<String> selectedFoldersList = new JList<>(selectedFoldersModel);

    // ✅ Felder und Checkboxen als Instanzvariablen, damit sie auch im Menü verfügbar sind
    private JTextField minField;
    private JTextField maxField;
    private JTextField fileExtention;
    private JCheckBox chkFileSize;
    private JCheckBox chkFileName;
    private JCheckBox chkSubFolder;
    private JCheckBox chkFileExtention;

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
        gbc.weightx = 0;

        int row = 0;

        // --- Felder & Checkboxen als Instanzvariablen
        minField = new JTextField(15);
        maxField = new JTextField(15);
        fileExtention = new JTextField(15);

        chkFileSize = new JCheckBox("Dateigröße berücksichtigen");
        chkFileName = new JCheckBox("Dateiname berücksichtigen");
        chkSubFolder = new JCheckBox("Unterordner berücksichtigen");
        chkFileExtention = new JCheckBox("Dateierweiterungen berücksichtigen");

        // Min File Size
        gbc.gridy = row;
        gbc.gridx = 0;
        panel.add(new JLabel("Min File Size:"), gbc);

        NumberFormat numberFormat = NumberFormat.getIntegerInstance();
        numberFormat.setGroupingUsed(true); // sorgt für 1000er-Punkte
        numberFormat.setMaximumFractionDigits(0); // keine Nachkommastellen

        NumberFormatter formatterNumbers = new NumberFormatter(numberFormat);
        formatterNumbers.setValueClass(Integer.class);
        formatterNumbers.setAllowsInvalid(false); // verhindert ungültige Eingaben
        formatterNumbers.setMinimum(0);          // keine negativen Zahlen
        
        JFormattedTextField minField = new JFormattedTextField(formatterNumbers);
        minField.setColumns(10);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(minField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        panel.add(new JLabel("MB"), gbc);

        // Max File Size
        row++;
        gbc.gridy = row;
        gbc.gridx = 0;
        panel.add(new JLabel("Max File Size:"), gbc);
        
        JFormattedTextField maxField = new JFormattedTextField(formatterNumbers);
        maxField.setColumns(10);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(maxField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        panel.add(new JLabel("MB"), gbc);
        
        
        
        
        // Max File Size
        row++;
        gbc.gridy = row;
        gbc.gridx = 0;
        panel.add(new JLabel("File Extentions (divided by ', ')"), gbc);
        
        DefaultFormatter formatterText = new DefaultFormatter();
        formatterText.setOverwriteMode(false); // Eingaben anhängen statt überschreiben
        formatterText.setAllowsInvalid(true);  // alles erlauben (freie Texteingabe)
        
        JFormattedTextField fileExtention = new JFormattedTextField(formatterText);
        fileExtention.setColumns(10);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(fileExtention, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        panel.add(new JLabel("MB"), gbc);
        
        
        
        

        // Checkboxes
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;

        gbc.gridy = ++row; panel.add(chkFileSize, gbc);
        gbc.gridy = ++row; panel.add(chkFileName, gbc);
        gbc.gridy = ++row; panel.add(chkSubFolder, gbc);
        gbc.gridy = ++row; panel.add(chkFileExtention, gbc);
        
//        gbc.gridy = ++row; panel.add(new JCheckBox("Wildcard 1"), gbc);
//        gbc.gridy = ++row; panel.add(new JCheckBox("Wildcard 2"), gbc);
//        gbc.gridy = ++row; panel.add(new JCheckBox("Wildcard 3"), gbc);

        
        
        // --- Spacer
        gbc.gridy = ++row;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(Box.createVerticalGlue(), gbc);

        // Button Panel
        JPanel buttonPanel = new JPanel(new BorderLayout());

        // Save Button mit ActionListener
        JButton btnSave = new JButton("Save Settings");
        btnSave.addActionListener(e -> {
            try {
            		double min = 0;
            		double max = 0;
            		String fileExt = "";

            		String minText = minField.getText().trim();
            		if (!minText.isEmpty()) min = Double.parseDouble(minText);

                String maxText = maxField.getText().trim();
                if (!maxText.isEmpty()) max = Double.parseDouble(maxText);
                
                String fExtTxt = fileExtention.getText().trim();
                if (!fExtTxt.isEmpty()) fileExt = fExtTxt;
  
                
                
                boolean fileSize = chkFileSize.isSelected();
                boolean fileName = chkFileName.isSelected();
                boolean subFolder = chkSubFolder.isSelected();
                boolean fExtention = chkFileExtention.isSelected();

                XMLController.saveSettingsToXML(min, max, fileExt, fileSize, fileName, subFolder, fExtention);

                JOptionPane.showMessageDialog(panel, "Einstellungen gespeichert!",
                        "Info", JOptionPane.INFORMATION_MESSAGE);

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(panel,
                        "Bitte gültige Zahlen eingeben!",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        leftButtons.add(btnSave);
        leftButtons.add(new JButton("Reset"));

        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        rightButtons.add(new JButton("Start"));

        buttonPanel.add(leftButtons, BorderLayout.WEST);
        buttonPanel.add(rightButtons, BorderLayout.EAST);

        gbc.gridy = ++row;
        gbc.gridx = 0;
        gbc.gridwidth = 3;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.SOUTH;
        panel.add(buttonPanel, gbc);

        return panel;
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
                JOptionPane.showMessageDialog(this, "Duplicates v1.0\nAutor: Jörg Hesse",
                        "Über Duplicates", JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(viewMenu);
        menuBar.add(helpMenu);
        return menuBar;
    }

    // Hilfsmethode: Settings in UI eintragen
    private void applySettings(double min, double max, String ext, boolean fileSize, boolean fileName, boolean subFolder, boolean fExt) {
        minField.setText(String.valueOf(min));
        maxField.setText(String.valueOf(max));
        fileExtention.setText(ext);       
        chkFileSize.setSelected(fileSize);
        chkFileName.setSelected(fileName);
        chkSubFolder.setSelected(subFolder);
        chkFileExtention.setSelected(fExt);
    }
}