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

public class DuplicateSearchOptionPanel extends JPanel {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd.MM.uuuu").withResolverStyle(ResolverStyle.STRICT);

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

    public DuplicateSearchOptionPanel() {
        super(new BorderLayout(5, 5));

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
        minField.setColumns(6);
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
        maxField.setColumns(6);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        centerPanel.add(maxField, gbc);
        gbc.fill = GridBagConstraints.NONE;

        // --- 3: File Extensions ---
        row++;
        gbc.gridy = row;
        gbc.gridx = 0;
        centerPanel.add(new JLabel("Dateiendungen (CSV):"), gbc);

        DefaultFormatter formatterText = new DefaultFormatter();
        formatterText.setOverwriteMode(false);
        formatterText.setAllowsInvalid(true);

        fileExtention = new JFormattedTextField(formatterText);
        fileExtention.setColumns(6);
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
        fileNameString1.setColumns(6);
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

        // --- Button Panel (links Speichern/Laden/Reset, rechts Start) ---
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
        // noch keine Funktion → bleibt leer
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
    }

    // applySettings(...) & toModel() bleiben wie in deiner letzten Version

    private JComboBox<String> createOpCombo() {
        JComboBox<String> combo = new JComboBox<>(new String[]{"<", "<=", "=", ">=", ">"});
        combo.setEditable(false);
        combo.setPrototypeDisplayValue(" >=");
        return combo;
    }

    private JFormattedTextField createStrictDateField() {
        MaskFormatter mask;
        try {
            mask = new MaskFormatter("##.##.####");
            mask.setPlaceholderCharacter('_');        // sichtbarer Platzhalter
            mask.setValidCharacters("0123456789");    // nur Ziffern erlaubt
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        JFormattedTextField f = new JFormattedTextField(mask);
        f.setColumns(10);

        // WICHTIG: PERSIST verhindert automatisches Zurücksetzen beim Fokusverlust
        f.setFocusLostBehavior(JFormattedTextField.PERSIST);

        // Strenge Validierung: nur echtes Datum zulassen, sonst rot markieren
        f.setInputVerifier(new InputVerifier() {
            final DateTimeFormatter STRICT_FMT =
                    DateTimeFormatter.ofPattern("dd.MM.uuuu")
                            .withResolverStyle(ResolverStyle.STRICT);

            @Override
            public boolean verify(JComponent input) {
                String s = ((JFormattedTextField) input).getText();
                if (isDateBlank(s)) {
                    input.setBackground(UIManager.getColor("TextField.background"));
                    return true;
                }
                try {
                    LocalDate.parse(s, STRICT_FMT);
                    input.setBackground(UIManager.getColor("TextField.background"));
                    return true;
                } catch (Exception ex) {
                    input.setBackground(new Color(255, 230, 230)); // leicht rot
                    return false;
                }
            }

            @Override
            public boolean shouldYieldFocus(JComponent input) {
                // Beep nur bei komplett falschem Wert
                boolean ok = verify(input);
                if (!ok) Toolkit.getDefaultToolkit().beep();
                return true; // Fokus trotzdem wechseln lassen
            }
        });

        return f;
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
 // --- UI → Model
    public duplicates.model.DuplicateSearchOptionsModel toModel() {
        duplicates.model.DuplicateSearchOptionsModel model = new duplicates.model.DuplicateSearchOptionsModel();
        model.setMinFileSize(numberFrom(minField));
        model.setMaxFileSize(numberFrom(maxField));
        model.setFileExtention(textOrEmpty(fileExtention));
        model.setFileNameString(textOrEmpty(fileNameString1));

        model.setFileCreationDateOperator((String) createdDateOperator.getSelectedItem());
        String createdStr = normalizeDateString(createdDateField.getText());
        if (!createdStr.isBlank()) {
            model.setCreationDate(java.time.LocalDate.parse(createdStr, DATE_FMT));
        }

        model.setFileModificationDateOperator((String) modifiedDateOperator.getSelectedItem());
        String modifiedStr = normalizeDateString(modificationDate.getText());
        if (!modifiedStr.isBlank()) {
            model.setModificationDate(java.time.LocalDate.parse(modifiedStr, DATE_FMT));
        }

        model.setSubFolderBoo(chkSubFolder.isSelected());
        model.setFileSizeBoo(chkFileSize.isSelected());
        model.setFileNameBoo(chkFileName.isSelected());
        model.setFileExtentionBoo(chkFileExtention.isSelected());
        return model;
    }

    // --- Model → UI
    public void applySettings(duplicates.model.DuplicateSearchOptionsModel model) {
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

    // --- Helper
    private String textOrEmpty(JFormattedTextField f) {
        return f.getText() == null ? "" : f.getText().trim();
    }
}
