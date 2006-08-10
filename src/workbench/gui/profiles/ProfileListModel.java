/*
 * ProfileListModel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.profiles;

import java.util.ArrayList;
import java.util.Collection;
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
	private boolean changed = false;
	private	DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Profiles");
	private int size;
	
	public ProfileListModel()
	{
		super(null, true);
		buildTree();
	}

	private void sortList(List toSort)
	{
		if (toSort == null) return;
		Collections.sort(toSort, ConnectionProfile.getNameComparator());
	}
	
	public void profileGroupModified(ConnectionProfile profile)
	{
		this.changed = true;
	}
	
	public void profileChanged(ConnectionProfile profile)
	{
		this.changed = true;
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
		this.changed = true;
		DefaultMutableTreeNode group = findGroupNode(profile.getGroup());
		DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(profile, false);
		this.insertNodeInto(newNode, group, group.getChildCount());
		TreePath newPath = new TreePath(new Object[] { this.rootNode, group, newNode });
		return newPath;
	}

	private DefaultMutableTreeNode findGroupNode(String group)
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
	
	public List getGroups()
	{
		if (this.rootNode == null) return null;
		List result = new LinkedList();
		int children = this.getChildCount(this.rootNode);
		for (int i = 1; i < children; i++)
		{
			DefaultMutableTreeNode n = (DefaultMutableTreeNode)getChild(rootNode, i);
			if (n == null) continue;
			String group = (String)n.getUserObject();
			result.add(group);
		}
		return result;
	}
	
	
	public void deleteProfile(ConnectionProfile prof)
	{
		TreePath path = getPath(prof);
		MutableTreeNode node = (MutableTreeNode)path.getLastPathComponent();
		if (!node.isLeaf()) return;
			
		ConnectionMgr conn = ConnectionMgr.getInstance();
		conn.removeProfile(prof);
		this.removeNodeFromParent(node);
		
		
		this.changed = true;
	}

	public TreePath getFirstProfile()
	{
		TreeNode defGroup = (TreeNode)this.rootNode.getChildAt(0);
		Object profile = defGroup.getChildAt(0);
		return new TreePath( new Object[] { rootNode, defGroup, profile });
	}
	
	public TreePath getPath(String profileName)
	{
		if (profileName == null) return null;
		Collection profiles = ConnectionMgr.getInstance().getProfiles().values();
		Iterator itr = profiles.iterator();
		while (itr.hasNext())
		{
			ConnectionProfile prof = (ConnectionProfile)itr.next();
			if (prof.getName().equals(profileName))
			{
				return getPath(prof);
			}
		}
		return null;
	}
	
	public TreePath getPath(ConnectionProfile prof)
	{
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
		if (changed) return true;
		Collection profiles = ConnectionMgr.getInstance().getProfiles().values();
		
		Iterator itr = profiles.iterator();
		while (itr.hasNext())
		{
			ConnectionProfile profile = (ConnectionProfile)itr.next();
			if (profile.isChanged()) return true;
		}
		return false;
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
		dummy.setUsername(ResourceMgr.getString("TxtEmptyProfileUser"));
		ConnectionMgr.getInstance().addProfile(dummy);
		this.changed = true;
		buildTree();
	}
	
	private void buildTree()
	{
		ArrayList profiles = new ArrayList();
		profiles.addAll(ConnectionMgr.getInstance().getProfiles().values());
		if (profiles.size() == 0) return;
		
		sortList(profiles);
		
		Map groupMap = new HashMap(profiles.size());
		List defaultProfiles = new ArrayList();
		
		Iterator itr = profiles.iterator();
		
		this.size = 0;
		
		String defGroupName = ResourceMgr.getString("LblDefGroup");
		while (itr.hasNext())
		{
			ConnectionProfile profile = (ConnectionProfile)itr.next();
			String group = profile.getGroup();
			if (StringUtil.isEmptyString(group) || defGroupName.equals(group)) 
			{
				defaultProfiles.add(profile);
			}
			else
			{
				List l = (List)groupMap.get(group);
				if (l == null)
				{
					l = new ArrayList();
					groupMap.put(group, l);
				}
				l.add(profile);
			}
			this.size ++;
		}
		
		// Make sure the "default" profiles (i.e. those without a group) 
		// are put at the front, this is for backward compatibility to older
		// versions that do not have profile groups
		if (defaultProfiles.size() > 0)
		{
			DefaultMutableTreeNode defaultNode = new DefaultMutableTreeNode(defGroupName, true);
			Collections.sort(defaultProfiles, StringUtil.getCaseInsensitiveComparator());
			rootNode.add(defaultNode);
			Iterator dg = defaultProfiles.iterator();
			while (dg.hasNext())
			{
				ConnectionProfile prof = (ConnectionProfile)dg.next();
				DefaultMutableTreeNode profNode = new DefaultMutableTreeNode(prof, false);
				defaultNode.add(profNode);
			}
		}
		
		// Make sure the default group is added as the first item!
		List groups = new ArrayList();
		groups.addAll(groupMap.keySet());
		Collections.sort(groups, StringUtil.getCaseInsensitiveComparator());
		
		// Now add all the other groups
		itr = groups.iterator();
		while (itr.hasNext())
		{
			String group = (String)itr.next();
			
			DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(group, true);
			rootNode.add(groupNode);
			List groupProfiles = (List)groupMap.get(group);
			
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
	
}
