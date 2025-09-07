package duplicates.view;

import duplicates.model.DuplicateSearchOptionsModel;

import javax.swing.*;
import java.awt.*;

/**
 * Fenster für die Duplikat-Suche.
 * Öffnet sich beim Klick auf "Start" im DuplicateSearchOptionPanel.
 * Nimmt die Suchoptionen entgegen und kann damit später die Duplikat-Suche starten.
 */
public class DuplicateSearchScreenView extends JFrame {

    private final DuplicateSearchOptionsModel options;

    /**
     * Konstruktor: Übergabe der aktuellen Suchoptionen
     * @param options DuplicateSearchOptionsModel mit allen aktuellen Filtereinstellungen
     */
    public DuplicateSearchScreenView(DuplicateSearchOptionsModel options) {
        super("Duplikat-Suche");
        this.options = options;

        // --- 1. Fensterbasis konfigurieren ---
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // --- 2. UI aufbauen ---
        initUI();
    }

    /**
     * Baut die Benutzeroberfläche auf (zunächst leer)
     */
    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout());

        JLabel infoLabel = new JLabel("Duplikat-Suche wurde gestartet...", SwingConstants.CENTER);
        infoLabel.setFont(infoLabel.getFont().deriveFont(Font.BOLD, 16f));

        mainPanel.add(infoLabel, BorderLayout.CENTER);
        add(mainPanel);
    }

    /**
     * Später: eigentliche Duplikat-Suche starten
     */
    public void startSearch() {
        // TODO: Algorithmus implementieren
        System.out.println("[DEBUG] Duplikat-Suche gestartet mit Optionen: " + options);
    }
}
