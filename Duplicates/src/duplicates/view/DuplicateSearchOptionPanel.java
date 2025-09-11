package duplicates.view;

import duplicates.controller.XMLController;
import duplicates.model.DuplicateSearchOptionsModel;

import javax.swing.*;
import javax.swing.text.DefaultFormatter;
import javax.swing.text.MaskFormatter;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.List;

public class DuplicateSearchOptionPanel extends JPanel {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd.MM.uuuu").withResolverStyle(ResolverStyle.STRICT);

    // --- Eingabefelder ---
    private JFormattedTextField minField;
    private JFormattedTextField maxField;
    private JFormattedTextField fileExtention;
    private JFormattedTextField fileNameString1;
    private JComboBox<String> createdDateOperator;
    private JFormattedTextField createdDateField;
    private JComboBox<String> modifiedDateOperator;
    private JFormattedTextField modificationDate;
    private JCheckBox chkFileSize;
    private JCheckBox chkFileName;
    private JCheckBox chkSubFolder;
    private JCheckBox chkFileExtention;

    // --- Referenz auf MainScreenView, um Ordnerliste zu bekommen ---
    private final MainScreenView mainScreenView;

    public DuplicateSearchOptionPanel(MainScreenView mainScreenView) {
        super(new BorderLayout(5, 5));
        this.mainScreenView = mainScreenView; // Referenz speichern

        // --- Titel ---
        JLabel title = new JLabel("Suche nach Duplikaten");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        title.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        add(title, BorderLayout.NORTH);

        // --- Optionspanel ---
        JPanel centerPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;

        int row = 0;

        // --- 1: Min File Size ---
        gbc.gridy = row;
        gbc.gridx = 0;
        centerPanel.add(new JLabel("Min. Dateigröße (MB):"), gbc);

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

        // --- 2: Max File Size ---
        row++;
        gbc.gridy = row;
        gbc.gridx = 0;
        centerPanel.add(new JLabel("Max. Dateigröße (MB):"), gbc);

        maxField = new JFormattedTextField(formatterNumbers);
        maxField.setColumns(12);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        centerPanel.add(maxField, gbc);
        gbc.fill = GridBagConstraints.NONE;

        // --- 3: File Extensions ---
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

        // --- 4: File Name ---
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

        // --- Created Date ---
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

        // --- Modified Date ---
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

        // --- Checkboxes in zwei Reihen ---
        row++;
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.gridwidth = 2;

        JPanel checkPanel = new JPanel(new GridLayout(2, 2, 8, 4));
        chkSubFolder = new JCheckBox("Unterordner berücksichtigen");
        chkFileSize = new JCheckBox("Dateigröße prüfen");
        chkFileName = new JCheckBox("Dateiname prüfen");
        chkFileExtention = new JCheckBox("Dateiendungen prüfen");

        checkPanel.add(chkSubFolder);
        checkPanel.add(chkFileSize);
        checkPanel.add(chkFileName);
        checkPanel.add(chkFileExtention);

        centerPanel.add(checkPanel, gbc);

        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        wrapper.add(centerPanel);
        add(wrapper, BorderLayout.CENTER);

        // --- Button Panel ---
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

    private void handleSave() {
        try {
            DuplicateSearchOptionsModel model = toModel();
            XMLController.saveDSSettingsToXML(model);
            JOptionPane.showMessageDialog(this, "Einstellungen gespeichert!", "Info",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Fehler beim Speichern: " + ex.getMessage(),
                    "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleLoad() {
        try {
            DuplicateSearchOptionsModel model = XMLController.readDSSettingsFromXML();
            if (model != null) {
                applySettings(model);
                JOptionPane.showMessageDialog(this, "Einstellungen geladen!", "Info",
                        JOptionPane.INFORMATION_MESSAGE);
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

    private void handleStart() {
        try {
            DuplicateSearchOptionsModel model = toModel();

            List<String> selectedFolders = mainScreenView.getSelectedFolders();
            if (selectedFolders.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Bitte wähle mindestens einen Ordner aus, bevor du die Suche startest.",
                        "Keine Ordner ausgewählt", JOptionPane.WARNING_MESSAGE);
                return;
            }

            SwingUtilities.invokeLater(() -> {
                DuplicateSearchScreenView searchView =
                        new DuplicateSearchScreenView(model, selectedFolders);
                searchView.setVisible(true);
            });

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Fehler beim Start: " + ex.getMessage(),
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
        chkFileSize.setSelected(false);
        chkFileName.setSelected(false);
        chkFileExtention.setSelected(false);
        mainScreenView.clearFolderSelection();
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

    private String normalizeDateString(String s) {
        return s == null || isDateBlank(s) ? "" : s;
    }

    private boolean isDateBlank(String s) {
        String t = s.replace('.', ' ').trim();
        for (int i = 0; i < t.length(); i++) {
            if (Character.isDigit(t.charAt(i))) return false;
        }
        return true;
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

    public DuplicateSearchOptionsModel toModel() {
        DuplicateSearchOptionsModel model = new DuplicateSearchOptionsModel();
        model.setMinFileSize(numberFrom(minField));
        model.setMaxFileSize(numberFrom(maxField));
        model.setFileExtention(textOrEmpty(fileExtention));
        model.setFileNameString(textOrEmpty(fileNameString1));

        model.setFileCreationDateOperator((String) createdDateOperator.getSelectedItem());
        String createdStr = normalizeDateString(createdDateField.getText());
        if (!createdStr.isBlank()) {
            model.setCreationDate(LocalDate.parse(createdStr, DATE_FMT));
        }

        model.setFileModificationDateOperator((String) modifiedDateOperator.getSelectedItem());
        String modifiedStr = normalizeDateString(modificationDate.getText());
        if (!modifiedStr.isBlank()) {
            model.setModificationDate(LocalDate.parse(modifiedStr, DATE_FMT));
        }

        model.setSubFolderBoo(chkSubFolder.isSelected());
        model.setFileSizeBoo(chkFileSize.isSelected());
        model.setFileNameBoo(chkFileName.isSelected());
        model.setFileExtentionBoo(chkFileExtention.isSelected());
        return model;
    }

    public void applySettings(DuplicateSearchOptionsModel model) {
        if (model == null) return;

        minField.setValue(model.getMinFileSize());
        maxField.setValue(model.getMaxFileSize());
        fileExtention.setText(model.getFileExtention() != null ? model.getFileExtention() : "");
        fileNameString1.setText(model.getFileNameString() != null ? model.getFileNameString() : "");
        createdDateOperator.setSelectedItem(model.getFileCreationDateOperator() != null
                ? model.getFileCreationDateOperator() : "=");
        createdDateField.setText(model.getCreationDate() != null
                ? model.getCreationDate().format(DATE_FMT) : "");
        modifiedDateOperator.setSelectedItem(model.getFileModificationDateOperator() != null
                ? model.getFileModificationDateOperator() : "=");
        modificationDate.setText(model.getModificationDate() != null
                ? model.getModificationDate().format(DATE_FMT) : "");
        chkSubFolder.setSelected(model.isSubFolderBoo());
        chkFileSize.setSelected(model.isFileSizeBoo());
        chkFileName.setSelected(model.isFileNameBoo());
        chkFileExtention.setSelected(model.isFileExtentionBoo());
    }

    private String textOrEmpty(JFormattedTextField f) {
        return f.getText() == null ? "" : f.getText().trim();
    }
}
