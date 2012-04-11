package com.netease.automate.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic tree implementation.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public class Tree<T> {
    private TreeNode<T> root;

    public Tree() {
        // do nothing
    }

    public Tree(T rootData) {
        setRoot(rootData);
    }

    public Tree(TreeNode<T> root) {
        setRoot(root);
    }

    public void setRoot(T rootData) {
        root = new TreeNode<T>(rootData, null);
    }

    public void setRoot(TreeNode<T> root) {
        this.root = root;
    }

    public T getRootData() {
        return root.getData();
    }

    public TreeNode<T> getRoot() {
        return root;
    }

    public void addNode(T parentData, T childData) {
        TreeNode<T> parentNode = root.getNode(parentData);
        if (parentNode == null) {
            throw new IllegalArgumentException("parent node not found");
        }

        parentNode.addChild(childData);
    }

    public List<T> getChildrenData(T parentData) {
        TreeNode<T> parentNode = root.getNode(parentData);
        if (parentNode == null) {
            throw new IllegalArgumentException("parent node not found");
        }

        List<TreeNode<T>> childrenNode = parentNode.getChildren();

        List<T> childrenData = new ArrayList<T>();
        for (TreeNode<T> node : childrenNode) {
            childrenData.add(node.getData());
        }

        return childrenData;
    }

    public Tree<T> getSubTree(T nodeData) {
        TreeNode<T> node = root.getNode(nodeData);
        if (node == null) {
            throw new IllegalArgumentException("node not found");
        }

        return new Tree<T>(node);
    }
}
