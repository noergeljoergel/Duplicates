package duplicates.view;

import duplicates.model.FileSearchOptionsModel;

import javax.swing.*;
import java.awt.*;

/**
 * Fenster für die Dateisuche.
 * Öffnet sich beim Klick auf "Start" im FileSearchOptionPanel.
 * Nimmt die Suchoptionen entgegen und kann damit später die Suche starten.
 */
public class FileSearchScreenView extends JFrame {

    private final FileSearchOptionsModel options;

    /**
     * Konstruktor: Übergabe der aktuellen Suchoptionen
     * @param options FileSearchOptionsModel mit allen aktuellen Filtereinstellungen
     */
    public FileSearchScreenView(FileSearchOptionsModel options) {
        super("Dateisuche"); // Fenstertitel
        this.options = options;

        // --- 1. Basis-Fenstereinstellungen ---
        setSize(800, 600); // Standardgröße, später ggf. anpassbar
        setLocationRelativeTo(null); // zentriert auf Bildschirm
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // nur dieses Fenster schließen

        // --- 2. UI initialisieren ---
        initUI();
    }

    /**
     * Baut die Benutzeroberfläche auf (zunächst leer)
     */
    private void initUI() {
        // Hauptpanel mit BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout());

        // --- Platzhalter für späteren Inhalt ---
        JLabel infoLabel = new JLabel("Dateisuche wurde gestartet...", SwingConstants.CENTER);
        infoLabel.setFont(infoLabel.getFont().deriveFont(Font.BOLD, 16f));

        mainPanel.add(infoLabel, BorderLayout.CENTER);

        add(mainPanel); // Panel ins Fenster einsetzen
    }

    /**
     * Später: hier kannst du die eigentliche Suche starten
     */
    public void startSearch() {
        // TODO: Suchalgorithmus implementieren
        System.out.println("[DEBUG] Suche gestartet mit Optionen: " + options);
    }
}
