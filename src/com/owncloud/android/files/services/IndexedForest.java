/* ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2015 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.files.services;

import android.accounts.Account;
import android.util.Pair;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.utils.Log_OC;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *  Helper structure to keep the trees of folders containing any file downloading or synchronizing.
 *
 *  A map provides the indexation based in hashing.
 *
 *  A tree is created per account.
 */
public class IndexedForest<V> {

    private ConcurrentMap<String, Node<V>> mMap = new ConcurrentHashMap<String, Node<V>>();

    private class Node<V> {
        String mKey = null;
        Node<V> mParent = null;
        Set<Node<V>> mChildren = new HashSet<Node<V>>();    // TODO be careful with hash()
        V mPayload = null;

        // payload is optional
        public Node(String key, V payload) {
            if (key == null) {
                throw new IllegalArgumentException("Argument key MUST NOT be null");
            }
            mKey = key;
            mPayload = payload;
        }

        public Node<V> getParent() {
            return mParent;
        };

        public Set<Node<V>> getChildren() {
            return mChildren;
        }

        public String getKey() {
            return mKey;
        }

        public V getPayload() {
            return mPayload;
        }

        public void addChild(Node<V> child) {
            mChildren.add(child);
            child.setParent(this);
        }

        private void setParent(Node<V> parent) {
            mParent = parent;
        }

        public boolean hasChildren() {
            return mChildren.size() > 0;
        }

        public void removeChild(Node<V> removed) {
            mChildren.remove(removed);
        }

        public void clearPayload() {
            mPayload = null;
        }
    }


    public /* synchronized */ Pair<String, String> putIfAbsent(Account account, String remotePath, V value) {
        String targetKey = buildKey(account, remotePath);
        Node<V> valuedNode = new Node(targetKey, value);
        mMap.putIfAbsent(
                targetKey,
                valuedNode
        );

        String currentPath = remotePath, parentPath = null, parentKey = null;
        Node<V> currentNode = valuedNode, parentNode = null;
        boolean linked = false;
        while (!OCFile.ROOT_PATH.equals(currentPath) && !linked) {
            parentPath = new File(currentPath).getParent();
            if (!parentPath.endsWith(OCFile.PATH_SEPARATOR)) {
                parentPath += OCFile.PATH_SEPARATOR;
            }
            parentKey = buildKey(account, parentPath);
            parentNode = mMap.get(parentKey);
            if (parentNode == null) {
                parentNode = new Node(parentKey, null);
                parentNode.addChild(currentNode);
                mMap.put(parentKey, parentNode);
            } else {
                parentNode.addChild(currentNode);
                linked = true;
            }
            currentPath = parentPath;
            currentNode = parentNode;
        }

        String linkedTo = OCFile.ROOT_PATH;
        if (linked) {
            linkedTo = parentNode.getKey().substring(account.name.length());
        }
        return new Pair<String, String>(targetKey, linkedTo);
    };


    public Pair<V, String> removePayload(Account account, String remotePath) {
        String targetKey = buildKey(account, remotePath);
        Node<V> target = mMap.get(targetKey);
        if (target != null) {
            target.clearPayload();
            if (!target.hasChildren()) {
                return remove(account, remotePath);
            }
        }
        return new Pair<V, String>(null, null);
    }


    public /* synchronized */ Pair<V, String> remove(Account account, String remotePath) {
        String targetKey = buildKey(account, remotePath);
        Node<V> firstRemoved = mMap.remove(targetKey);
        String unlinkedFrom = null;

        if (firstRemoved != null) {
            /// remove children
            removeDescendants(firstRemoved);

            /// remove ancestors if only here due to firstRemoved
            Node<V> removed = firstRemoved;
            Node<V> parent = removed.getParent();
            boolean unlinked = false;
            while (parent != null) {
                parent.removeChild(removed);
                if (!parent.hasChildren()) {
                    removed = mMap.remove(parent.getKey());
                    parent = removed.getParent();
                } else {
                    break;
                }
            }

            if (parent != null) {
                unlinkedFrom = parent.getKey().substring(account.name.length());
            }

            return new Pair<V, String>(firstRemoved.getPayload(), unlinkedFrom);
        }

        return new Pair<V, String>(null, null);
    }

    private void removeDescendants(Node<V> removed) {
        Iterator<Node<V>> childrenIt = removed.getChildren().iterator();
        Node<V> child = null;
        while (childrenIt.hasNext()) {
            child = childrenIt.next();
            mMap.remove(child.getKey());
            removeDescendants(child);
        }
    }

    public boolean contains(Account account, String remotePath) {
        String targetKey = buildKey(account, remotePath);
        return mMap.containsKey(targetKey);
    }

    public /* synchronized */ V get(String key) {
        Node<V> node = mMap.get(key);
        if (node != null) {
            return node.getPayload();
        } else {
            return null;
        }
    }

    public V get(Account account, String remotePath) {
        String key = buildKey(account, remotePath);
        return get(key);
    }


    /**
     * Remove the elements that contains account as a part of its key
     * @param account
     */
    public void remove(Account account){
        Iterator<String> it = mMap.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            Log_OC.d("IndexedForest", "Number of pending downloads= "  + mMap.size());
            if (key.startsWith(account.name)) {
                mMap.remove(key);
            }
        }
    }

    /**
     * Builds a key to index files
     *
     * @param account       Account where the file to download is stored
     * @param remotePath    Path of the file in the server
     */
    private String buildKey(Account account, String remotePath) {
        return account.name + remotePath;
    }

}
