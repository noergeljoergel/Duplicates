package duplicates.view;

import duplicates.controller.FileAccessController;
import duplicates.model.FolderTreeModel;

import javax.swing.*;
import java.awt.*;

public class MainScreenView extends JFrame {

    public MainScreenView() {
        super("Duplicates – Dateisuche");

        // Look & Feel auf "Windows" setzen (falls verfügbar)
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Windows".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    SwingUtilities.updateComponentTreeUI(this);
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("Windows Look & Feel konnte nicht gesetzt werden.");
        }

        // Fenstergröße setzen
        setSize(800, 600);

        // Standardverhalten beim Schließen
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Fenster in der Bildschirmmitte anzeigen
        setLocationRelativeTo(null);

        // Layout und Beispiel-Komponente
        setLayout(new BorderLayout());
//        add(new JLabel("Willkommen bei Duplicates!", SwingConstants.CENTER), BorderLayout.CENTER);
        
        
        // Controller + Model erstellen
        FileAccessController controller = new FileAccessController();
        FolderTreeModel treeModel = new FolderTreeModel(controller);

        // JTree mit Model
        JTree folderTree = new JTree(treeModel);
        folderTree.setRootVisible(true);

        // In Scrollpane packen
        JScrollPane scrollPane = new JScrollPane(folderTree);

        add(scrollPane, BorderLayout.CENTER);
        setVisible(true);
    }
    
    
}
