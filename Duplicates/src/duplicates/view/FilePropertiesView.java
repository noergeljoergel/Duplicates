package duplicates.view;

import duplicates.controller.FilePropertiesController;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

public class FilePropertiesView extends JFrame {

    private final JTabbedPane tabbedPane = new JTabbedPane();

    public FilePropertiesView(File file) {
        setTitle("Eigenschaften - " + file.getName());
        setSize(600, 500);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();

        // Tab 1: Allgemeine Eigenschaften
        tabs.add("Eigenschaften", createFilePropertiesTab(file));

        // Tab 2: Nur wenn Medien-Datei
        if (isMediaFile(file)) {
            tabs.add("Metadaten", createMetadataTab(file));
        }

        add(tabs, BorderLayout.CENTER);
    }

    /** Prüft, ob Datei ein Bild, Audio oder Video ist */
    private boolean isMediaFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg") ||
               name.endsWith(".png") || name.endsWith(".gif") ||
               name.endsWith(".bmp") || name.endsWith(".tiff") ||
               name.endsWith(".mp3") || name.endsWith(".wav") ||
               name.endsWith(".flac") || name.endsWith(".ogg") ||
               name.endsWith(".mp4") || name.endsWith(".mov") ||
               name.endsWith(".avi") || name.endsWith(".mkv") ||
               name.endsWith(".wmv") || name.endsWith(".webm");
    }

    /** Basis-Dateieigenschaften (kompakt, oben ausgerichtet) **/
    private JPanel createFilePropertiesTab(File file) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        try {
            BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

            int row = 0;
            row = addCompactProperty(panel, row, "Dateiname", file.getName());
            row = addCompactProperty(panel, row, "Pfad", file.getAbsolutePath());
            row = addCompactProperty(panel, row, "Größe (Byte)", String.valueOf(file.length()));
            row = addCompactProperty(panel, row, "Erstellt am", fmt.format(attrs.creationTime().toInstant().atZone(java.time.ZoneId.systemDefault())));
            row = addCompactProperty(panel, row, "Geändert am", fmt.format(attrs.lastModifiedTime().toInstant().atZone(java.time.ZoneId.systemDefault())));
            row = addCompactProperty(panel, row, "Versteckt", String.valueOf(file.isHidden()));
            row = addCompactProperty(panel, row, "Schreibgeschützt", String.valueOf(!file.canWrite()));
            row = addCompactProperty(panel, row, "Systemdatei", String.valueOf(!file.canWrite() && !file.canExecute()));

            // --- Füller für vertikale Top-Ausrichtung ---
            GridBagConstraints filler = new GridBagConstraints();
            filler.gridx = 0;
            filler.gridy = row;
            filler.weighty = 1.0;
            filler.fill = GridBagConstraints.BOTH;
            panel.add(Box.createGlue(), filler);

        } catch (Exception e) {
            addCompactProperty(panel, 0, "Fehler", e.getMessage());
        }

        return panel;
    }

    private int addCompactProperty(JPanel panel, int row, String key, String value) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Label links
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        panel.add(new JLabel(key + ":"), gbc);

        // Wert rechts
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JLabel(value), gbc);

        return row + 1;
    }

    /** Metadaten mit Gruppierung + kompakt **/
    private JComponent createMetadataTab(File file) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        Map<String, String> metadata = new LinkedHashMap<>(FilePropertiesController.getImageMetadata(file));

        int row = 0;

        // --- GPS immer zuerst ---
        row = addCompactMetadataRow(panel, row, "GPS Latitude", metadata.remove("GPS Latitude"), true);
        row = addCompactMetadataRow(panel, row, "GPS Longitude", metadata.remove("GPS Longitude"), true);

        String lastGroup = "";

        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (key.toLowerCase().contains("fehler")) {
                JLabel errorLabel = new JLabel(value);
                errorLabel.setForeground(Color.RED);
                gbc.gridx = 0;
                gbc.gridy = row;
                gbc.gridwidth = 3;
                panel.add(errorLabel, gbc);
                row++;
                continue;
            }

            // Directory-Name extrahieren (alles vor dem "-")
            String group = key.contains("-") ? key.split("-", 2)[0].trim() : "Allgemein";
            String shortKey = key.contains("-") ? key.split("-", 2)[1].trim() : key;

            // Neue Gruppe einfügen, wenn sie wechselt
            if (!group.equals(lastGroup)) {
                JLabel groupLabel = new JLabel(group);
                groupLabel.setFont(groupLabel.getFont().deriveFont(Font.BOLD));
                gbc.gridx = 0;
                gbc.gridy = row;
                gbc.gridwidth = 3;
                panel.add(groupLabel, gbc);
                row++;
                lastGroup = group;
            }

            // Metadatenzeile
            row = addCompactMetadataRow(panel, row, shortKey, value, false);
        }

        // --- Füller unten für Top-Ausrichtung ---
        GridBagConstraints filler = new GridBagConstraints();
        filler.gridx = 0;
        filler.gridy = row;
        filler.weighty = 1.0;
        filler.fill = GridBagConstraints.BOTH;
        panel.add(Box.createGlue(), filler);

        return new JScrollPane(panel);
    }

    private int addCompactMetadataRow(JPanel panel, int row, String key, String value, boolean alwaysShow) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;

        // Label
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("  " + key + ":"), gbc);

        // Wertfeld (kompakt)
        JTextField field = new JTextField(value != null ? value : "–");
        field.setEditable(false);
        field.setPreferredSize(new Dimension(250, 22));
        gbc.gridx = 1;
        panel.add(field, gbc);

        // Bearbeiten-Button
        if (!alwaysShow) {
            JButton editBtn = new JButton("Bearbeiten");
            editBtn.setPreferredSize(new Dimension(100, 22));
            editBtn.addActionListener(e -> openEditDialog(key, field));
            gbc.gridx = 2;
            panel.add(editBtn, gbc);
        }

        return row + 1;
    }

    /** Editor-Dialog für Metadaten (Text oder Boolean) **/
    private void openEditDialog(String key, JTextField boundField) {
        String current = boundField.getText();

        final JDialog dlg = new JDialog(this, "Metadaten bearbeiten: " + key, true);
        dlg.setLayout(new BorderLayout(10, 10));
        dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JPanel center = new JPanel(new FlowLayout());
        JLabel lbl = new JLabel(key + ":");

        boolean isBool = looksBoolean(current);
        JTextField tf = new JTextField(current, 20);
        JCheckBox cb = new JCheckBox();

        if (isBool) {
            cb.setSelected(parseBoolean(current));
            center.add(lbl);
            center.add(cb);
        } else {
            center.add(lbl);
            center.add(tf);
        }

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton ok = new JButton("OK");
        JButton cancel = new JButton("Abbrechen");

        ok.addActionListener(e -> {
            String newVal = isBool ? String.valueOf(cb.isSelected()) : tf.getText().trim();
            boundField.setText(newVal);
            dlg.dispose();
        });
        cancel.addActionListener(e -> dlg.dispose());

        buttons.add(ok);
        buttons.add(cancel);

        dlg.add(center, BorderLayout.CENTER);
        dlg.add(buttons, BorderLayout.SOUTH);
        dlg.pack();
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    private boolean looksBoolean(String s) {
        if (s == null) return false;
        return s.trim().matches("(?i)^(true|false|wahr|falsch|ja|nein|yes|no|1|0)$");
    }

    private boolean parseBoolean(String s) {
        if (s == null) return false;
        String v = s.trim().toLowerCase();
        return v.equals("true") || v.equals("wahr") || v.equals("ja") || v.equals("yes") || v.equals("1");
    }
}
