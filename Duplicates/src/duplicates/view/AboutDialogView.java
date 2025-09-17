package duplicates.view;

import javax.swing.*;
import java.awt.*;

public class AboutDialogView extends JDialog {

    public AboutDialogView(JFrame parent) {
        super(parent, "About...", true);

        // --- Panel für Inhalt ---
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Bild laden ---
        ImageIcon icon = new ImageIcon(getClass().getResource("/img/KroneKlein.png"));
        JLabel iconLabel = new JLabel(icon);

        // --- Text ---
        JLabel textLabel = new JLabel("<br><html><h2>Duplicates v0.1</h2>"
                                    + "<p>Autor: Jörg Hesse</p>"
                                    + "<p>© 2025 TechNova GmbH</p></html>");

        // --- Layout ---
        panel.add(iconLabel, BorderLayout.WEST);
        panel.add(textLabel, BorderLayout.CENTER);

        // --- Schließen-Button ---
        JButton closeButton = new JButton("OK");
        closeButton.addActionListener(e -> dispose());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(closeButton);

        add(panel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // --- Größe & Position ---
        setSize(100, 80);
        setLocationRelativeTo(parent);
    }
}
