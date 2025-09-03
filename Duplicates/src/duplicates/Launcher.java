package duplicates;

import duplicates.view.MainScreenView;

public class Launcher {
    public static void main(String[] args) {
        // GUI Start im Event Dispatch Thread
        javax.swing.SwingUtilities.invokeLater(() -> {
            new MainScreenView(); // Erstellt und zeigt das Hauptfenster
        });
    }
}
