package duplicates.view;

import duplicates.controller.FileAccessController;
import duplicates.model.FolderTreeModel;

import javax.swing.*;
import java.awt.*;

public class MainScreenView extends JFrame {

    public MainScreenView() {
        super("Duplicates – Dateisuche");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Controller erstellen
        FileAccessController controller = new FileAccessController();

        // JTree erstellen (ohne Model)
        JTree folderTree = new JTree();

        // Model mit Lazy Loading an JTree binden
        FolderTreeModel treeModel = new FolderTreeModel(controller, folderTree);
        folderTree.setModel(treeModel);
        folderTree.setRootVisible(true);

        add(new JScrollPane(folderTree), BorderLayout.CENTER);
        setVisible(true);
    }
}