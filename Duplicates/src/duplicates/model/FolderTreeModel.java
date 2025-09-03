package duplicates.model;

import duplicates.controller.FileAccessController;

import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.*;
import java.io.File;
import java.util.Arrays;

public class FolderTreeModel extends DefaultTreeModel {

    private final FileAccessController fileAccess;

    /**
     * 
     * @param controller
     * @param tree
     */
    public FolderTreeModel(FileAccessController controller, JTree tree) {
        super(new DefaultMutableTreeNode("Arbeitsplatz"));
        this.fileAccess = controller;

        buildRootLevel();

        // Listener für Lazy Loading
        tree.addTreeWillExpandListener(new TreeWillExpandListener() {
            @Override
            public void treeWillExpand(TreeExpansionEvent event) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) event.getPath().getLastPathComponent();
                loadChildren(node);
            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent event) {
                // Nichts tun beim Zuklappen
            }
        });
    }

    /**
     * Nur Root-Ebene einmalig aufbauen 
     */
    private void buildRootLevel() {
        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) getRoot();
        File[] roots = fileAccess.getRootFolders();
        Arrays.stream(roots).forEach(root -> rootNode.add(createLazyNode(root)));
    }

    /**
     * Erstellt einen Node mit Dummy-Kind für Lazy Loading
     * @param folder
     * @return
     */
    private DefaultMutableTreeNode createLazyNode(File folder) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(folder);
        if (fileAccess.getSubFolders(folder).length > 0) {
            node.add(new DefaultMutableTreeNode("Loading...")); // Platzhalter
        }
        return node;
    }

    /**
     * Lädt echte Unterordner, wenn der Node aufgeklappt wird
     * @param node
     */
    private void loadChildren(DefaultMutableTreeNode node) {
        // Wenn bereits geladen → nichts tun
        if (node.getChildCount() == 1 && "Loading...".equals(node.getChildAt(0).toString())) {
            node.removeAllChildren();
            File folder = (File) node.getUserObject();

            File[] subFolders = fileAccess.getSubFolders(folder);
            Arrays.stream(subFolders).forEach(sub -> node.add(createLazyNode(sub)));

            nodeStructureChanged(node);
        }
    }
    private DefaultMutableTreeNode createNode(File folder) {
        CheckBoxNode nodeData = new CheckBoxNode(folder);
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(nodeData);

        File[] subFolders = fileAccess.getSubFolders(folder);
        if (subFolders != null) {
            for (File sub : subFolders) {
                node.add(createNode(sub));
            }
        }
        return node;
    }
}