package duplicates.view;

import duplicates.controller.XMLController;
import duplicates.model.FileSearchOptionsModel;

import javax.swing.*;
import javax.swing.text.DefaultFormatter;
import javax.swing.text.MaskFormatter;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Panel für die Eingabe der Suchoptionen.
 * Bietet Felder für Dateigröße, Filter, Datumsangaben etc.
 */
public class FileSearchOptionPanel extends JPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.uuuu");

    // --- Eingabefelder ---
    private JFormattedTextField minField;
    private JFormattedTextField maxField;
    private JFormattedTextField fileExtention;
    private JFormattedTextField fileNameString1;
    private JComboBox<String> createdDateOperator;
    private JFormattedTextField createdDateField;
    private JComboBox<String> modifiedDateOperator;
    private JFormattedTextField modificationDate;
    private JCheckBox chkSubFolder;

    // --- Referenz auf MainScreenView, um Ordnerliste auszulesen ---
    private final MainScreenView mainScreenView;

    public FileSearchOptionPanel(MainScreenView mainScreenView) {
        super(new BorderLayout(5, 5));
        this.mainScreenView = mainScreenView;

        // --- Titel ---
        JLabel title = new JLabel("Suche nach Dateien");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        title.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        add(title, BorderLayout.NORTH);

        // --- Inneres Panel mit GridBagLayout ---
        JPanel centerPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;

        int row = 0;

        // --- Min File Size ---
        gbc.gridy = row;
        gbc.gridx = 0;
        centerPanel.add(new JLabel("Min. Dateigröße (Byte):"), gbc);

        NumberFormat numberFormat = NumberFormat.getIntegerInstance();
        numberFormat.setGroupingUsed(true);
        numberFormat.setMaximumFractionDigits(0);
        NumberFormatter formatterNumbers = new NumberFormatter(numberFormat);
        formatterNumbers.setValueClass(Long.class);
        formatterNumbers.setAllowsInvalid(false);
        formatterNumbers.setMinimum(0L);

        minField = new JFormattedTextField(formatterNumbers);
        minField.setColumns(12);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        centerPanel.add(minField, gbc);
        gbc.fill = GridBagConstraints.NONE;

        // --- Max File Size ---
        row++;
        gbc.gridy = row;
        gbc.gridx = 0;
        centerPanel.add(new JLabel("Max. Dateigröße (Byte):"), gbc);

        maxField = new JFormattedTextField(formatterNumbers);
        maxField.setColumns(23);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        centerPanel.add(maxField, gbc);
        gbc.fill = GridBagConstraints.NONE;

        // --- File Extensions ---
        row++;
        gbc.gridy = row;
        gbc.gridx = 0;
        centerPanel.add(new JLabel("Dateiendungen:"), gbc);

        DefaultFormatter formatterText = new DefaultFormatter();
        formatterText.setOverwriteMode(false);
        formatterText.setAllowsInvalid(true);

        fileExtention = new JFormattedTextField(formatterText);
        fileExtention.setColumns(12);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        centerPanel.add(fileExtention, gbc);
        gbc.fill = GridBagConstraints.NONE;

        // --- File Name ---
        row++;
        gbc.gridy = row;
        gbc.gridx = 0;
        centerPanel.add(new JLabel("Dateiname enthält:"), gbc);

        fileNameString1 = new JFormattedTextField(formatterText);
        fileNameString1.setColumns(12);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        centerPanel.add(fileNameString1, gbc);
        gbc.fill = GridBagConstraints.NONE;

        // --- Creation Date ---
        row++;
        gbc.gridy = row;
        gbc.gridx = 0;
        centerPanel.add(new JLabel("Erstellt am:"), gbc);

        JPanel createdPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        createdDateOperator = createOpCombo();
        createdPanel.add(createdDateOperator);
        createdDateField = createStrictDateField();
        createdPanel.add(createdDateField);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        centerPanel.add(createdPanel, gbc);
        gbc.fill = GridBagConstraints.NONE;

        // --- Modification Date ---
        row++;
        gbc.gridy = row;
        gbc.gridx = 0;
        centerPanel.add(new JLabel("Letzte Änderung:"), gbc);

        JPanel modifiedPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        modifiedDateOperator = createOpCombo();
        modifiedPanel.add(modifiedDateOperator);
        modificationDate = createStrictDateField();
        modifiedPanel.add(modificationDate);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        centerPanel.add(modifiedPanel, gbc);
        gbc.fill = GridBagConstraints.NONE;

        // --- Checkbox ---
        row++;
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        centerPanel.add(chkSubFolder = new JCheckBox("Unterordner berücksichtigen"), gbc);
        gbc.gridwidth = 1;

        // --- Center Panel in Wrapper setzen ---
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        wrapper.add(centerPanel);
        add(wrapper, BorderLayout.CENTER);

        // --- Button Panel unten ---
        JPanel buttonPanel = new JPanel(new BorderLayout());

        JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton btnSave = new JButton("Speichern");
        btnSave.addActionListener(e -> handleSave());
        JButton btnLoad = new JButton("Laden");
        btnLoad.addActionListener(e -> handleLoad());
        JButton btnReset = new JButton("Reset");
        btnReset.addActionListener(e -> resetFields());
        leftButtons.add(btnSave);
        leftButtons.add(btnLoad);
        leftButtons.add(btnReset);

        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        JButton btnStart = new JButton("Start");
        btnStart.addActionListener(e -> handleStart());
        rightButtons.add(btnStart);

        buttonPanel.add(leftButtons, BorderLayout.WEST);
        buttonPanel.add(rightButtons, BorderLayout.EAST);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JComboBox<String> createOpCombo() {
        JComboBox<String> combo = new JComboBox<>(new String[]{"<", "<=", "=", ">=", ">"});
        combo.setEditable(false);
        combo.setPrototypeDisplayValue(" >=");
        return combo;
    }

    private JFormattedTextField createStrictDateField() {
        try {
            MaskFormatter mask = new MaskFormatter("##.##.####");
            mask.setPlaceholderCharacter('_');
            mask.setValidCharacters("0123456789");
            JFormattedTextField f = new JFormattedTextField(mask);
            f.setFocusLostBehavior(JFormattedTextField.PERSIST);
            return f;
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    // --- Start-Button Eventhandler: erstellt das Suchfenster und startet die Suche
    private void handleStart() {
        try {
            FileSearchOptionsModel model = toModel();

            List<String> selectedFolders = mainScreenView.getSelectedFolders();
            if (selectedFolders.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Bitte wähle mindestens einen Ordner aus, bevor du die Suche startest.",
                        "Keine Ordner ausgewählt", JOptionPane.WARNING_MESSAGE);
                return;
            }

            SwingUtilities.invokeLater(() -> {
                FileSearchScreenView searchView = new FileSearchScreenView(model, selectedFolders);
                searchView.setVisible(true);
            });

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Fehler beim Start: " + ex.getMessage(),
                    "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }


    // --- Übertragene Einstellungen in UI anwenden
    public void applySettings(FileSearchOptionsModel model) {
        minField.setValue(model.getMinFileSize());
        maxField.setValue(model.getMaxFileSize());
        fileExtention.setText(model.getFileExtention() != null ? model.getFileExtention() : "");
        fileNameString1.setText(model.getFileNameString() != null ? model.getFileNameString() : "");
        createdDateOperator.setSelectedItem(model.getFileCreationDateOperator() != null ? model.getFileCreationDateOperator() : "=");
        createdDateField.setText(model.getCreationDate() != null ? model.getCreationDate().format(DATE_FMT) : "");
        modifiedDateOperator.setSelectedItem(model.getFileModificationDateOperator() != null ? model.getFileModificationDateOperator() : "=");
        modificationDate.setText(model.getModificationDate() != null ? model.getModificationDate().format(DATE_FMT) : "");
        chkSubFolder.setSelected(model.isSubFolderBoo());
    }

    // --- Erstellt ein Model-Objekt aus den Eingaben
    public FileSearchOptionsModel toModel() {
        FileSearchOptionsModel model = new FileSearchOptionsModel();
        model.setMinFileSize(numberFrom(minField));
        model.setMaxFileSize(numberFrom(maxField));
        model.setFileExtention(fileExtention.getText() != null ? fileExtention.getText().trim() : "");
        model.setFileNameString(fileNameString1.getText() != null ? fileNameString1.getText().trim() : "");
        model.setFileCreationDateOperator((String) createdDateOperator.getSelectedItem());
        model.setFileModificationDateOperator((String) modifiedDateOperator.getSelectedItem());
        model.setSubFolderBoo(chkSubFolder.isSelected());
        return model;
    }

    private double numberFrom(JFormattedTextField f) {
        Object v = f.getValue();
        if (v instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(f.getText().replace(".", "").replace(",", "."));
        } catch (Exception e) {
            return 0d;
        }
    }

    private void handleLoad() {
        try {
            FileSearchOptionsModel model = XMLController.readFSSettingsFromXML();
            if (model != null) {
                applySettings(model);
                JOptionPane.showMessageDialog(this, "Dateisuche-Einstellungen geladen.",
                        "Info", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Keine gespeicherten Einstellungen gefunden.",
                        "Info", JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Fehler beim Laden: " + ex.getMessage(),
                    "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void resetFields() {
        minField.setValue(null);
        maxField.setValue(null);
        fileExtention.setText("");
        fileNameString1.setText("");
        createdDateOperator.setSelectedItem("=");
        createdDateField.setText("");
        modifiedDateOperator.setSelectedItem("=");
        modificationDate.setText("");
        chkSubFolder.setSelected(false);
        mainScreenView.clearFolderSelection();
    }

    private void handleSave() {
        try {
            FileSearchOptionsModel model = toModel();
            XMLController.saveFSSettingsToXML(model);
            JOptionPane.showMessageDialog(this, "Dateisuche-Einstellungen gespeichert.",
                    "Info", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Fehler beim Speichern: " + ex.getMessage(),
                    "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }
}
