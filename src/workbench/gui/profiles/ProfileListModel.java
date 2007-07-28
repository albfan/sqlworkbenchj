/*
 * ProfileListModel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.profiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.resource.ResourceMgr;
import workbench.util.StringUtil;

/**
 *
 * @author  support@sql-workbench.net
 */
class ProfileListModel 
	extends DefaultTreeModel
{
	private	DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Profiles");
	private int size;
	
	public ProfileListModel()
	{
		super(null, true);
		buildTree();
	}

	private void sortList(List<ConnectionProfile> toSort)
	{
		if (toSort == null) return;
		Collections.sort(toSort, ConnectionProfile.getNameComparator());
	}
	
	public void profileChanged(ConnectionProfile profile)
	{
		TreePath path = getPath(profile);
		if (path == null) return;
		if (path.getPathCount() < 3) return;
		DefaultMutableTreeNode groupNode = (DefaultMutableTreeNode)path.getPathComponent(2);
		DefaultMutableTreeNode pNode = (DefaultMutableTreeNode)path.getLastPathComponent();
		int index = groupNode.getIndex(pNode);
		fireTreeNodesChanged(this.rootNode, path.getPath(), new int[] { index }, new Object[] { pNode });
	}

	public TreePath addProfile(ConnectionProfile profile)
	{
		ConnectionMgr conn = ConnectionMgr.getInstance();
		conn.addProfile(profile);
		DefaultMutableTreeNode group = findGroupNode(profile.getGroup());
		DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(profile, false);
		this.insertNodeInto(newNode, group, group.getChildCount());
		TreePath newPath = new TreePath(new Object[] { this.rootNode, group, newNode });
		this.size ++;
		return newPath;
	}

	public DefaultMutableTreeNode findGroupNode(String group)
	{
		if (this.rootNode == null) return null;
		int children = this.getChildCount(this.rootNode);
		for (int i = 0; i < children; i++)
		{
			DefaultMutableTreeNode n = (DefaultMutableTreeNode)getChild(rootNode, i);
			if (n == null) continue;
			String name = (String)n.getUserObject();
			if (name.equals(group)) 
			{
				return n;
			}
		}

		return null;
	}
	
	public TreePath[] getGroupNodes()
	{
		if (this.rootNode == null) return null;
		int children = this.getChildCount(this.rootNode);
		TreePath[] nodes = new TreePath[children];
		for (int i = 0; i < children; i++)
		{
			TreeNode n = (TreeNode)getChild(rootNode, i);
			if (n == null) continue;
			nodes[i] = new TreePath(new Object[] { this.rootNode, n } );
		}
		return nodes;
	}
	
	public List<String> getGroups()
	{
		if (this.rootNode == null) return null;
		List<String> result = new LinkedList<String>();
		int children = this.getChildCount(this.rootNode);
		for (int i = 0; i < children; i++)
		{
			DefaultMutableTreeNode n = (DefaultMutableTreeNode)getChild(rootNode, i);
			if (n == null) continue;
			String group = (String)n.getUserObject();
			result.add(group);
		}
		return result;
	}
	
	public void deleteGroup(String group)
	{
		if (group == null) return;
		DefaultMutableTreeNode node = findGroupNode(group);
		if (node == null) return;
		deleteGroupProfiles(node);
		removeGroupNode(node);
	}
	
	public void deleteGroupProfiles(DefaultMutableTreeNode node)
	{
		if (node == null) return;
		int count = node.getChildCount();
		if (count == 0) return;
		ConnectionMgr conn = ConnectionMgr.getInstance();
		for (int i = 0; i < count; i++)
		{
			DefaultMutableTreeNode child = (DefaultMutableTreeNode)node.getChildAt(i);
			ConnectionProfile prof = (ConnectionProfile)child.getUserObject();
			conn.removeProfile(prof);
		}
		node.removeAllChildren();
	}
	
	public void deleteProfile(ConnectionProfile prof)
	{
		TreePath path = getPath(prof);
		MutableTreeNode node = (MutableTreeNode)path.getLastPathComponent();
		if (!node.isLeaf()) return;
			
		ConnectionMgr conn = ConnectionMgr.getInstance();
		conn.removeProfile(prof);
		this.removeNodeFromParent(node);
		
		this.size --;
	}

	public TreePath getFirstProfile()
	{
		TreeNode defGroup = this.rootNode.getChildAt(0);
		Object profile = defGroup.getChildAt(0);
		return new TreePath( new Object[] { rootNode, defGroup, profile });
	}
	
	public TreePath getPath(ProfileKey def)
	{
		if (def == null) return null;
		ConnectionProfile prof = ConnectionMgr.getInstance().getProfile(def);
		if (prof != null)
		{
			return getPath(prof);
		}
		return null;
	}
	
	public TreePath getPath(ConnectionProfile prof)
	{
		if (prof == null) return null;
		String pGroup = prof.getGroup();
		Object groupNode = null;
		if (StringUtil.isEmptyString(pGroup))
		{
			groupNode = this.getChild(this.rootNode, 0);
		}
		else
		{
			int children = this.getChildCount(this.rootNode);
			// find the profile group
			for (int i = 0; i < children; i++)
			{
				DefaultMutableTreeNode n = (DefaultMutableTreeNode)getChild(rootNode, i);
				if (n == null) continue;
				String g = (String)n.getUserObject();
				if (pGroup.equals(g)) 
				{
					groupNode = n;
					break;
				}
			}
		}
		if (groupNode == null) return null;
		
		int children = this.getChildCount(groupNode);
		Object profileNode = null;
		for (int i = 0; i < children; i++)
		{
			DefaultMutableTreeNode node  = (DefaultMutableTreeNode)this.getChild(groupNode, i);
			ConnectionProfile p = (ConnectionProfile)node.getUserObject();
			if (p.equals(prof))
			{
				profileNode = node;
			}
		}
		if (profileNode == null) return null;
		return new TreePath(new Object[] { rootNode, groupNode, profileNode } );
	}
	
	public boolean isChanged()
	{
		return ConnectionMgr.getInstance().profilesAreModified();
	}
	
	public int getSize()
	{
		return this.size;
	}
	
	public TreePath addGroup(String name)
	{
		if (name == null) return null;
		DefaultMutableTreeNode node = new DefaultMutableTreeNode(name, true);
		this.insertNodeInto(node, this.rootNode, this.rootNode.getChildCount());
		return new TreePath(new Object[] { rootNode, node });
	}

	public void addEmptyProfile()
	{
		ConnectionProfile dummy = new ConnectionProfile();
		dummy.setName(ResourceMgr.getString("TxtEmptyProfileName"));
		dummy.setUrl("jdbc:");
		ConnectionMgr.getInstance().addProfile(dummy);
		this.size ++;
		buildTree();
	}

	public void removeGroupNode(DefaultMutableTreeNode groupNode)
	{
		deleteGroupProfiles(groupNode);
		this.removeNodeFromParent(groupNode);
	}
	
	private void buildTree()
	{
		ArrayList<ConnectionProfile> profiles = new ArrayList<ConnectionProfile>(ConnectionMgr.getInstance().getProfiles());
		if (profiles.size() == 0) return;
		
		sortList(profiles);
		
		Map<String, List<ConnectionProfile>> groupMap = new HashMap<String, List<ConnectionProfile>>(profiles.size());
		
		this.size = profiles.size();
		
		for (ConnectionProfile profile : profiles)
		{
			String group = profile.getGroup();
			List<ConnectionProfile> l = groupMap.get(group);
			if (l == null)
			{
				l = new ArrayList<ConnectionProfile>();
				groupMap.put(group, l);
			}
			l.add(profile);
		}
		
		// Make sure the default group is added as the first item!
		List<String> groups = new ArrayList<String>();
		groups.addAll(groupMap.keySet());
		Collections.sort(groups, StringUtil.getCaseInsensitiveComparator());
		
		for (String group : groups)
		{
			DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(group, true);
			rootNode.add(groupNode);
			List<ConnectionProfile> groupProfiles = groupMap.get(group);
			
			this.sortList(groupProfiles);
			Iterator p = groupProfiles.iterator();
			while (p.hasNext())
			{
				ConnectionProfile prof = (ConnectionProfile)p.next();
				DefaultMutableTreeNode profNode = new DefaultMutableTreeNode(prof, false);
				groupNode.add(profNode);
			}
		}
		this.setRoot(rootNode);
	}

	public void moveProfilesToGroup(DefaultMutableTreeNode sourceGroupNode, String newGroup)
	{
		DefaultMutableTreeNode target = findGroupNode(newGroup);
		if (target == null) return;
		int count = sourceGroupNode.getChildCount();
		if (count == 0) return;
		DefaultMutableTreeNode[] nodes = new DefaultMutableTreeNode[count];
		for (int i = 0; i < count; i++)
		{
			nodes[i] = (DefaultMutableTreeNode)sourceGroupNode.getChildAt(i);
		}
		moveProfilesToGroup(nodes, target);
	}
	
	
	public void moveProfilesToGroup(DefaultMutableTreeNode[] profiles, DefaultMutableTreeNode groupNode)
	{
		if (profiles == null) return;
		if (profiles.length == 0) return;
		if (groupNode == null) return;
		
		String groupName = (String)groupNode.getUserObject();
			
		for (int i = 0; i < profiles.length; i++)
		{
			Object o = profiles[i].getUserObject();
			ConnectionProfile original = null;
			if (o instanceof ConnectionProfile)
			{
				original = (ConnectionProfile)o;
			}
			if (original == null) continue;

			removeNodeFromParent(profiles[i]);
			insertNodeInto(profiles[i], groupNode, groupNode.getChildCount());
			original.setGroup(groupName);
		}
	}

	public void copyProfilesToGroup(DefaultMutableTreeNode[] profiles, DefaultMutableTreeNode groupNode)
	{
		if (profiles == null) return;
		if (profiles.length == 0) return;
		if (groupNode == null) return;
		
		String groupName = (String)groupNode.getUserObject();
			
		for (int i = 0; i < profiles.length; i++)
		{
			Object o = profiles[i].getUserObject();
			ConnectionProfile original = null;
			if (o instanceof ConnectionProfile)
			{
				original = (ConnectionProfile)o;
			}
			if (original == null) continue;

			ConnectionProfile copy = original.createCopy();
			copy.setGroup(groupName);
			ConnectionMgr.getInstance().addProfile(copy);
			DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(copy, false);
			insertNodeInto(newNode, groupNode, groupNode.getChildCount());
		}
	}
	
}
