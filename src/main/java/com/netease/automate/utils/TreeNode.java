package com.netease.automate.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Tree node.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public class TreeNode<T> {
    private T data;
    private TreeNode<T> parent;

    private Map<T, TreeNode<T>> nodeMap = new HashMap<T, TreeNode<T>>();

    private List<TreeNode<T>> children = new ArrayList<TreeNode<T>>();

    public TreeNode(T data, TreeNode<T> parent) {
        this.data = data;
        this.parent = parent;

        addMapEntry(this);
    }

    public TreeNode(T data) {
        this.data = data;
        this.parent = null;

        addMapEntry(this);
    }

    public T getData() {
        return data;
    }

    public TreeNode<T> getParent() {
        return parent;
    }

    public void setParent(TreeNode<T> parent) {
        this.parent = parent;

        addMapEntry(this);
    }

    public List<TreeNode<T>> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public List<T> getChildrenData() {
        List<T> childrenData = new ArrayList<T>();
        for (TreeNode<T> subNode : children) {
            childrenData.add(subNode.getData());
        }

        return Collections.unmodifiableList(childrenData);
    }

    public TreeNode<T> getNode(T nodeData) {
        TreeNode<T> node = nodeMap.get(nodeData);
        if (node == null) {
            throw new IllegalArgumentException("node not found");
        }

        return node;
    }

    public TreeNode<T> addChild(TreeNode<T> child) {
        children.add(child);

        addMapEntry(child);

        return child;
    }

    public TreeNode<T> addChild(T childData) {
        TreeNode<T> node = new TreeNode<T>(childData, this);

        return addChild(node);
    }

    public void deleteChildren() {
        for (TreeNode<T> node : children) {
            deleteMapEntry(node);
        }

        children.clear();
    }

    public void deleteChild(T childData) {
        for (Iterator<TreeNode<T>> iter = children.iterator(); iter.hasNext();) {
            TreeNode<T> node = iter.next();
            if (node.getData().equals(childData)) {
                deleteMapEntry(node);
                iter.remove();
            }
        }
    }

    public void deleteChild(TreeNode<T> child) {
        for (Iterator<TreeNode<T>> iter = children.iterator(); iter.hasNext();) {
            TreeNode<T> node = iter.next();
            if (node.getData().equals(child.getData())) {
                deleteMapEntry(node);
                iter.remove();
            }
        }
    }

    public void addMapEntry(TreeNode<T> node) {
        nodeMap.put(node.getData(), node);

        if (parent != null) {
            parent.addMapEntry(node);
        }
    }

    public void deleteMapEntry(TreeNode<T> node) {
        nodeMap.remove(node.getData());
        if (parent != null) {
            parent.deleteMapEntry(node);
        }
    }
}
