package duplicates.view;

import duplicates.controller.XMLController;

import javax.swing.*;
import javax.swing.text.DefaultFormatter;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.lang.reflect.Array;
import java.text.NumberFormat;
import java.util.Arrays;


/**
 * Optionen für die Duplikatsuche (aus MainScreenView ausgelagert).
 * Später kann hier parallel ein FileSearchOptionFrame existieren.
 */
public class FileSearchOptionFrame extends JPanel {

    // Felder & Checkboxen (intern verwaltet)
    private JFormattedTextField minField;
    private JFormattedTextField maxField;
    private JFormattedTextField fileExtention;
    private JFormattedTextField fileNameString1;
    private JFormattedTextField fileNameString2;
    private JFormattedTextField fileNameString3;
    private JFormattedTextField fileNameString4;
    
    private JCheckBox chkSubFolder;

    public FileSearchOptionFrame() {
        super(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0;

        int row = 0;

        // Checkboxes (vorab, damit sie verfügbar sind)
        chkSubFolder = new JCheckBox("Unterordner berücksichtigen");

        // --- Min File Size ---
        gbc.gridy = row;
        gbc.gridx = 0;
        add(new JLabel("Min File Size:"), gbc);

        NumberFormat numberFormat = NumberFormat.getIntegerInstance();
        numberFormat.setGroupingUsed(true);         // 1000er-Punkte
        numberFormat.setMaximumFractionDigits(0);   // keine Nachkommastellen

        NumberFormatter formatterNumbers = new NumberFormatter(numberFormat);
        formatterNumbers.setValueClass(Long.class);
        formatterNumbers.setAllowsInvalid(false);   // verhindert ungültige Eingaben
        formatterNumbers.setMinimum(0L);            // keine negativen Zahlen

        minField = new JFormattedTextField(formatterNumbers);
        minField.setColumns(10);

        gbc.gridx = 1; gbc.weightx = 1.0;
        add(minField, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        add(new JLabel("MB"), gbc);

        // --- Max File Size ---
        row++;
        gbc.gridy = row;
        gbc.gridx = 0;
        add(new JLabel("Max File Size:"), gbc);

        maxField = new JFormattedTextField(formatterNumbers);
        maxField.setColumns(10);

        gbc.gridx = 1; gbc.weightx = 1.0;
        add(maxField, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        add(new JLabel("MB"), gbc);

        // --- File Extensions ---
        row++;
        gbc.gridy = row;
        gbc.gridx = 0;
        add(new JLabel("File Extentions (divided by ', ')"), gbc);

        DefaultFormatter formatterText = new DefaultFormatter();
        formatterText.setOverwriteMode(false); // Eingaben anhängen statt überschreiben
        formatterText.setAllowsInvalid(true);  // freie Texteingabe

        fileExtention = new JFormattedTextField(formatterText);
        fileExtention.setColumns(10);

        gbc.gridx = 1; gbc.weightx = 1.0;
        add(fileExtention, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        add(new JLabel(""), gbc); // kein "MB" hier nötig
        
        
        // --- String #1 ---
        row++;
        gbc.gridy = row;
        gbc.gridx = 0;
        add(new JLabel("String #1"), gbc);

        fileNameString1 = new JFormattedTextField(formatterText);
        fileNameString1.setColumns(10);

        gbc.gridx = 1; gbc.weightx = 1.0;
        add(fileNameString1, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        add(new JLabel(""), gbc);   
        
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

        // Save Button mit ActionListener
        JButton btnSave = new JButton("Save Settings");
        btnSave.addActionListener(e -> {
            try {
                double min = numberFrom(minField);
                double max = numberFrom(maxField);
                String ext = fileExtention.getText() == null ? "" : fileExtention.getText().trim();

                boolean subFolder = chkSubFolder.isSelected();

//                XMLController.saveSettingsToXML(min, max, ext, fileSize, fileName, subFolder, fExtention);

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

    // Hilfsmethode: robust Zahlenwert lesen (0 bei leer)
    private double numberFrom(JFormattedTextField f) {
        Object v = f.getValue();
        if (v instanceof Number n) return n.doubleValue();
        String t = f.getText();
        if (t == null || t.isBlank()) return 0d;
        try {
            // Fallback: Gruppierungszeichen entfernen, Komma zu Punkt
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

    }
    
    public String[] splitExtentionString(String input) {
        if (input == null || input.trim().isEmpty()) {
            return new String[0];
        }

        
        return Arrays.stream(input.split("\\s*,\\s*"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.replaceFirst("^\\.", "")) // führenden Punkt bei ".jpg" entfernen
                .map(String::toLowerCase)             // klein schreiben
                .distinct()                           // doppelte entfernen (optional)
                .toArray(String[]::new);
    }
}
