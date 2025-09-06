package duplicates.view;

import javax.swing.*;
import javax.swing.text.DefaultFormatter;
import javax.swing.text.NumberFormatter;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

/**
 * Optionen für die Dateisuche (ohne DatePicker).
 * Datumsfelder: dd.MM.yyyy – Punkte vorgegeben, nur Ziffern erlaubt.
 */
public class FileSearchOptionFrame extends JPanel {

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

    private JCheckBox chkSubFolder;

    public FileSearchOptionFrame() {
        super(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL; // Standard
        gbc.weightx = 0;

        int row = 0;

        // Checkboxes (vorab, damit sie verfügbar sind)
        chkSubFolder = new JCheckBox("Unterordner berücksichtigen");

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

        gbc.gridx = 1;
        // Feld NICHT strecken -> bündig wie Datumsfelder
        gbc.weightx = 0;
        int oldFill = gbc.fill;
        gbc.fill = GridBagConstraints.NONE;
        add(minField, gbc);
        // zurücksetzen
        gbc.fill = oldFill;

        gbc.gridx = 2; gbc.weightx = 0;
        add(new JLabel(""), gbc);

        // --- Max File Size ---
        row++;
        gbc.gridy = row;
        gbc.gridx = 0;
        add(new JLabel("max file size in MB:"), gbc);

        maxField = new JFormattedTextField(formatterNumbers);
        maxField.setColumns(10);

        gbc.gridx = 1;
        gbc.weightx = 0;
        oldFill = gbc.fill;
        gbc.fill = GridBagConstraints.NONE;
        add(maxField, gbc);
        gbc.fill = oldFill;

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

        gbc.gridx = 1;
        gbc.weightx = 0;
        oldFill = gbc.fill;
        gbc.fill = GridBagConstraints.NONE;
        add(fileExtention, gbc);
        gbc.fill = oldFill;

        gbc.gridx = 2; gbc.weightx = 0;
        add(new JLabel(""), gbc);

        // --- String #1 (file name) ---
        row++;
        gbc.gridy = row;
        gbc.gridx = 0;
        add(new JLabel("file name"), gbc);

        fileNameString1 = new JFormattedTextField(formatterText);
        fileNameString1.setColumns(10);

        gbc.gridx = 1;
        gbc.weightx = 0;
        oldFill = gbc.fill;
        gbc.fill = GridBagConstraints.NONE;
        add(fileNameString1, gbc);
        gbc.fill = oldFill;

        gbc.gridx = 2; gbc.weightx = 0;
        add(new JLabel(""), gbc);

        // --- Datum (file created) ---
        row++;
        gbc.gridy = row;

        gbc.gridx = 0; gbc.weightx = 0;
        add(new JLabel("file created"), gbc);

        JPanel createdPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        createdDateOperator = createOpCombo();
        createdPanel.add(createdDateOperator);

        createdDateField = createStrictDateField();
        createdPanel.add(createdDateField);

        gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 1.0; // Panel darf breit werden, Inhalte bleiben kompakt
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

        gbc.gridy = ++row; add(chkSubFolder, gbc);

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

                boolean subFolder = chkSubFolder.isSelected();

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
        int arrowAndInsets = 24;
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
            mask.setPlaceholderCharacter('_');
            mask.setValidCharacters("0123456789");
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        JFormattedTextField f = new JFormattedTextField(mask);
        f.setColumns(10);
        f.setFocusLostBehavior(JFormattedTextField.COMMIT_OR_REVERT);

        // Strenge Validierung
        f.setInputVerifier(new InputVerifier() {
            final DateTimeFormatter STRICT_FMT =
                    DateTimeFormatter.ofPattern("dd.MM.uuuu").withResolverStyle(ResolverStyle.STRICT);

            @Override
            public boolean verify(JComponent input) {
                String s = ((JFormattedTextField) input).getText();
                if (isDateBlank(s)) return true;
                try {
                    LocalDate.parse(s, STRICT_FMT);
                    input.setBackground(UIManager.getColor("TextField.background"));
                    return true;
                } catch (Exception ex) {
                    input.setBackground(new Color(255, 230, 230));
                    return false;
                }
            }

            @Override
            public boolean shouldYieldFocus(JComponent input) {
                boolean ok = verify(input);
                if (!ok) Toolkit.getDefaultToolkit().beep();
                return ok;
            }
        });

        return f;
    }

    private boolean isDateBlank(String s) {
        if (s == null) return true;
        String t = s.replace('.', ' ').trim();
        for (int i = 0; i < t.length(); i++) {
            if (Character.isDigit(t.charAt(i))) return false;
        }
        return true;
    }

    private String normalizeDateString(String s) {
        return isDateBlank(s) ? "" : s;
    }

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
}
