package duplicates.model;

import duplicates.controller.FileAccessController;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.io.File;

public class FolderTreeModel extends DefaultTreeModel {

    private final FileAccessController fileAccess;

    public FolderTreeModel(FileAccessController controller) {
        super(new DefaultMutableTreeNode());
        this.fileAccess = controller;
        buildTree();
    }

    private void buildTree() {
        File rootFolder = fileAccess.getRootFolder();
        DefaultMutableTreeNode rootNode = createNode(rootFolder);
        setRoot(rootNode);
    }

    private DefaultMutableTreeNode createNode(File folder) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(folder);

        File[] subFolders = fileAccess.getSubFolders(folder);
        for (File sub : subFolders) {
            node.add(createNode(sub)); // rekursiv
        }

        return node;
    }
}