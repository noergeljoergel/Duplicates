package duplicates.model;

import duplicates.controller.FileAccessController;

import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class FolderTreeModel extends DefaultTreeModel {

    private static final String DUMMY = "Loading...";

    private final FileAccessController fileAccess;

    // ✅ Merkt sich Auswahlzustände pro Pfad, damit Häkchen nicht „verschwinden“
    private final Map<String, Boolean> selectionCache = new HashMap<>();

    /**
     * @param controller Datenquelle (Filesystem)
     * @param tree       JTree für Lazy Loading Listener
     */
    public FolderTreeModel(FileAccessController controller, JTree tree) {
        super(new DefaultMutableTreeNode("Arbeitsplatz"));
        this.fileAccess = controller;

        buildRootLevel();

        // Listener für Lazy Loading
        tree.addTreeWillExpandListener(new TreeWillExpandListener() {
            @Override
            public void treeWillExpand(TreeExpansionEvent event) {
                DefaultMutableTreeNode node =
                        (DefaultMutableTreeNode) event.getPath().getLastPathComponent();
                loadChildren(node);
            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent event) {
                // nichts tun
            }
        });
    }

    /**
     * Nur Root-Ebene einmalig aufbauen
     */
    private void buildRootLevel() {
        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) getRoot();
        File[] roots = fileAccess.getRootFolders();
        if (roots == null) return;

        Arrays.stream(roots).forEach(root -> rootNode.add(createLazyNode(root)));
        nodeStructureChanged(rootNode);
    }

    /**
     * Erstellt einen Node (mit CheckBoxNode als userObject) plus Dummy-Kind für Lazy Loading
     */
    private DefaultMutableTreeNode createLazyNode(File folder) {
        boolean selected = selectionCache.getOrDefault(folder.getAbsolutePath(), false);

        // Kein (File, boolean)-Konstruktor vorhanden → 1-Arg verwenden + Setter
        duplicates.model.CheckBoxNode nodeData = new duplicates.model.CheckBoxNode(folder);
        nodeData.setSelected(selected); // <- Zustand aus dem Cache wiederherstellen

        DefaultMutableTreeNode node = new DefaultMutableTreeNode(nodeData);

        File[] subs = fileAccess.getSubFolders(folder);
        if (subs != null && subs.length > 0) {
            node.add(new DefaultMutableTreeNode(DUMMY)); // Platzhalter fürs Lazy Loading
        }
        return node;
    }

    /**
     * Lädt echte Unterordner, wenn der Node aufgeklappt wird
     */
    private void loadChildren(DefaultMutableTreeNode node) {
        // Nur laden, wenn genau EIN Dummy-Kind vorhanden ist
        if (node.getChildCount() == 1 && DUMMY.equals(node.getChildAt(0).toString())) {
            node.removeAllChildren();

            Object uo = node.getUserObject();
            if (!(uo instanceof CheckBoxNode cb)) return;

            File folder = cb.getFile();
            File[] subFolders = fileAccess.getSubFolders(folder);
            if (subFolders != null) {
                Arrays.stream(subFolders).forEach(sub -> node.add(createLazyNode(sub)));
            }

            nodeStructureChanged(node);
        }
    }

    /**
     * Von außen aufrufen, wenn der Editor eine Auswahl ändert.
     * So bleibt der Zustand bei Lazy-Reloads erhalten.
     */
    public void rememberSelection(String absolutePath, boolean selected) {
        selectionCache.put(absolutePath, selected);
    }
    public void clearAllSelections() {
        clearNode((DefaultMutableTreeNode) getRoot());
        nodeStructureChanged((DefaultMutableTreeNode) getRoot());
    }

    private void clearNode(DefaultMutableTreeNode node) {
        Object uo = node.getUserObject();
        if (uo instanceof duplicates.model.CheckBoxNode cbNode) {
            cbNode.setSelected(false);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            clearNode((DefaultMutableTreeNode) node.getChildAt(i));
        }
    }
}
