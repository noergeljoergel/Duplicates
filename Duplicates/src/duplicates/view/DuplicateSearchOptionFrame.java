package duplicates.view;

import javax.swing.*;
import javax.swing.text.DefaultFormatter;
import javax.swing.text.NumberFormatter;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

/**
 * Optionen für die Dateisuche (ohne DatePicker).
 * Datumsfelder: dd.MM.yyyy – Punkte vorgegeben, nur Ziffern erlaubt.
 */
public class DuplicateSearchOptionFrame extends JPanel {

    // Felder & Checkboxen (intern verwaltet)
    private JFormattedTextField minField;
    private JFormattedTextField maxField;
    private JFormattedTextField fileExtention;
    private JFormattedTextField fileNameString1;

    // Datum: erstellt am
    private JComboBox<String> createdDateOperator;
    private JFormattedTextField createdDateField;

    // Datum: geändert am
    private JComboBox<String> modifiedDateOperator;
    private JFormattedTextField modificationDate;

    private JCheckBox chkFileSize;
    private JCheckBox chkFileName;
    private JCheckBox chkSubFolder;
    private JCheckBox chkFileExtention;

    public DuplicateSearchOptionFrame() {
        super(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0;

        int row = 0;

        // Checkboxes (vorab, damit sie verfügbar sind)
        chkFileSize  = new JCheckBox("check file size");
        chkFileName = new JCheckBox("check file name");
        chkSubFolder = new JCheckBox("include subfolders");
        chkFileExtention = new JCheckBox("check file extentions");



        // --- Min File Size ---
        gbc.gridy = row;
        gbc.gridx = 0;
        add(new JLabel("min file size in MB:"), gbc);

        NumberFormat numberFormat = NumberFormat.getIntegerInstance();
        numberFormat.setGroupingUsed(true);
        numberFormat.setMaximumFractionDigits(0);

        NumberFormatter formatterNumbers = new NumberFormatter(numberFormat);
        formatterNumbers.setValueClass(Long.class);
        formatterNumbers.setAllowsInvalid(false);
        formatterNumbers.setMinimum(0L);

        minField = new JFormattedTextField(formatterNumbers);
        minField.setColumns(10);

        gbc.gridx = 1; gbc.weightx = 1.0;
        add(minField, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        add(new JLabel(""), gbc);

        // --- Max File Size ---
        row++;
        gbc.gridy = row;
        gbc.gridx = 0;
        add(new JLabel("max file size in MB:"), gbc);

        maxField = new JFormattedTextField(formatterNumbers);
        maxField.setColumns(10);

        gbc.gridx = 1; gbc.weightx = 1.0;
        add(maxField, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        add(new JLabel(""), gbc);

        // --- File Extensions ---
        row++;
        gbc.gridy = row;
        gbc.gridx = 0;
        add(new JLabel("file extentions"), gbc);

        DefaultFormatter formatterText = new DefaultFormatter();
        formatterText.setOverwriteMode(false);
        formatterText.setAllowsInvalid(true);

        fileExtention = new JFormattedTextField(formatterText);
        fileExtention.setColumns(10);

        gbc.gridx = 1; gbc.weightx = 1.0;
        add(fileExtention, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        add(new JLabel("                       "), gbc);

        // --- String #1 ---
        row++;
        gbc.gridy = row;
        gbc.gridx = 0;
        add(new JLabel("file name"), gbc);

        fileNameString1 = new JFormattedTextField(formatterText);
        fileNameString1.setColumns(10);

        gbc.gridx = 1; gbc.weightx = 1.0;
        add(fileNameString1, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        add(new JLabel(""), gbc);

        // --- Datum (file created) ---
        row++;
        gbc.gridy = row;

        gbc.gridx = 0; gbc.weightx = 0;
        add(new JLabel("file created"), gbc);

        // Unterpanel: Operator + Datum direkt nebeneinander
        JPanel createdPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        createdDateOperator = createOpCombo();
        createdPanel.add(createdDateOperator);

        createdDateField = createStrictDateField();
        createdPanel.add(createdDateField);

        gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 1.0;
        add(createdPanel, gbc);
        gbc.gridwidth = 1;

        // --- Datum (latest changes) ---
        row++;
        gbc.gridy = row;

        gbc.gridx = 0; gbc.weightx = 0;
        add(new JLabel("latest changes"), gbc);

        JPanel modifiedPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        modifiedDateOperator = createOpCombo();
        modifiedPanel.add(modifiedDateOperator);

        modificationDate = createStrictDateField();
        modifiedPanel.add(modificationDate);

        gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 1.0;
        add(modifiedPanel, gbc);
        gbc.gridwidth = 1;

        // --- Checkboxes ---
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;

        gbc.gridy = ++row; add(chkFileSize, gbc);
        gbc.gridy = ++row; add(chkFileName, gbc);
        gbc.gridy = ++row; add(chkSubFolder, gbc);
        gbc.gridy = ++row; add(chkFileExtention, gbc);

        // --- Spacer ---
        gbc.gridy = ++row;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        add(Box.createVerticalGlue(), gbc);

        // --- Button Panel ---
        JPanel buttonPanel = new JPanel(new BorderLayout());

        JButton btnSave = new JButton("Save Settings");
        btnSave.addActionListener(e -> {
            try {
                double min = numberFrom(minField);
                double max = numberFrom(maxField);
                String ext = fileExtention.getText() == null ? "" : fileExtention.getText().trim();

                boolean fileSize = chkFileSize.isSelected();
                boolean fileName = chkFileName.isSelected();
                boolean subFolder = chkSubFolder.isSelected();
                boolean fExtention = chkFileExtention.isSelected();

                // Datum + Operator auslesen (Strings im Format dd.MM.yyyy oder leer)
                String createdOp = (String) createdDateOperator.getSelectedItem();
                String createdStr = normalizeDateString(createdDateField.getText());

                String modifiedOp = (String) modifiedDateOperator.getSelectedItem();
                String modifiedStr = normalizeDateString(modificationDate.getText());

                JOptionPane.showMessageDialog(this, "Einstellungen gespeichert!",
                        "Info", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
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
        add(buttonPanel, gbc);
    }

    /** Mini-ComboBox ~2–3 Zeichen breit, mittig, kompakt. */
    private JComboBox<String> createOpCombo() {
        JComboBox<String> combo = new JComboBox<>(new String[]{"<", "<=", "=", ">=", ">"});
        combo.setEditable(false);
        combo.setPrototypeDisplayValue(" >= "); // etwas Puffer

        // Renderer: zentriert
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                return l;
            }
        });

        // Schmale, aber sichere Breite
        FontMetrics fm = combo.getFontMetrics(combo.getFont());
        int textW = fm.stringWidth(" >= ");
        int arrowAndInsets = 24; // bei Bedarf leicht erhöhen
        int w = textW + arrowAndInsets;

        Dimension size = new Dimension(w, combo.getPreferredSize().height);
        combo.setPreferredSize(size);
        combo.setMinimumSize(size);
        combo.setMaximumSize(size);

        return combo;
    }

    /** Datumsfeld: Punkte vorgegeben, nur Ziffern erlaubt, strikte Datum-Validierung. */
    private JFormattedTextField createStrictDateField() {
        MaskFormatter mask;
        try {
            mask = new MaskFormatter("##.##.####");
            mask.setPlaceholderCharacter('_');        // Platzhalter sichtbar
            mask.setValidCharacters("0123456789");    // nur Ziffern erlaubt
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        JFormattedTextField f = new JFormattedTextField(mask);
        f.setColumns(10);
        f.setFocusLostBehavior(JFormattedTextField.COMMIT_OR_REVERT);

        // Strenge Validierung: nur echtes Datum zulassen (bei Eingabe vorhanden)
        f.setInputVerifier(new InputVerifier() {
            final DateTimeFormatter STRICT_FMT =
                    DateTimeFormatter.ofPattern("dd.MM.uuuu").withResolverStyle(ResolverStyle.STRICT);

            @Override
            public boolean verify(JComponent input) {
                String s = ((JFormattedTextField) input).getText();
                if (isDateBlank(s)) return true; // leeres Feld ist okay
                try {
                    LocalDate.parse(s, STRICT_FMT);
                    input.setBackground(UIManager.getColor("TextField.background"));
                    return true;
                } catch (Exception ex) {
                    // Ungültig → leicht hervorheben
                    input.setBackground(new Color(255, 230, 230));
                    return false;
                }
            }

            @Override
            public boolean shouldYieldFocus(JComponent input) {
                boolean ok = verify(input);
                if (!ok) {
                    Toolkit.getDefaultToolkit().beep();
                }
                return ok; // bei false bleibt der Fokus im Feld
            }
        });

        return f;
    }

    /** true, wenn Feld effektiv leer (nur Platzhalter/Punkte/Leerzeichen). */
    private boolean isDateBlank(String s) {
        if (s == null) return true;
        String t = s.replace('.', ' ').trim();
        for (int i = 0; i < t.length(); i++) {
            if (Character.isDigit(t.charAt(i))) return false;
        }
        return true;
    }

    /** Normalisiert leere Masken-Inhalte zu leerem String. */
    private String normalizeDateString(String s) {
        return isDateBlank(s) ? "" : s;
    }

    // Hilfsmethode: robust Zahlenwert lesen (0 bei leer)
    private double numberFrom(JFormattedTextField f) {
        Object v = f.getValue();
        if (v instanceof Number n) return n.doubleValue();
        String t = f.getText();
        if (t == null || t.isBlank()) return 0d;
        try {
            return Double.parseDouble(t.replace(".", "").replace(",", "."));
        } catch (NumberFormatException e) {
            return 0d;
        }
    }

    // Öffentliche API für MainScreenView (Menü "Einstellungen laden")
    public void applySettings(double min, double max, String ext,
                              boolean fileSize, boolean fileName,
                              boolean subFolder, boolean fExt) {
        minField.setValue((long) Math.round(min));
        maxField.setValue((long) Math.round(max));
        fileExtention.setText(ext != null ? ext : "");
        chkSubFolder.setSelected(subFolder);

        // Optional: Datum/Operator setzen
        // createdDateOperator.setSelectedItem("="); createdDateField.setText("01.01.2025");
        // modifiedDateOperator.setSelectedItem("="); modificationDate.setText("01.01.2025");
    }

    public String[] splitExtentionString(String input) {
        if (input == null || input.trim().isEmpty()) return new String[0];
        return Arrays.stream(input.split("\\s*,\\s*"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.replaceFirst("^\\.", ""))
                .map(String::toLowerCase)
                .distinct()
                .toArray(String[]::new);
    }
}
