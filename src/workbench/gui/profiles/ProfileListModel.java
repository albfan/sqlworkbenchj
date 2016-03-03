/*
 * ProfileListModel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.profiles;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import workbench.resource.ResourceMgr;

import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.ProfileGroupMap;
import workbench.db.ProfileManager;

import workbench.util.CaseInsensitiveComparator;
import workbench.util.CollectionUtil;
import workbench.util.StringUtil;

/**
 *
 * @author  Thomas Kellerer
 */
class ProfileListModel
	extends DefaultTreeModel
{
  private File sourceFile;
	private	final DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Profiles");
	private final List<ConnectionProfile> profiles = new ArrayList<>();;
	private final List<ConnectionProfile> filtered = new ArrayList<>();

	ProfileListModel()
  {
    super(null, true);
    buildTree();
    addGroup(ResourceMgr.getString("LblDefGroup"));
  }

	ProfileListModel(List<ConnectionProfile> sourceProfiles)
	{
		super(null, true);

		for (ConnectionProfile prof : sourceProfiles)
		{
			profiles.add(prof.createStatefulCopy());
		}
		buildTree();
	}

  public void setSourceFile(File f)
  {
    sourceFile = f;
  }

  public File getSourceFile()
  {
    return sourceFile;
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
		profiles.add(profile);
		DefaultMutableTreeNode group = findGroupNode(profile.getGroup());
		DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(profile, false);
		insertNodeInto(newNode, group, group.getChildCount());
		TreePath newPath = new TreePath(new Object[] { this.rootNode, group, newNode });
		return newPath;
	}

  public DefaultMutableTreeNode findProfileNode(ConnectionProfile profile)
  {
		TreePath path = getPath(profile);
    if (path == null) return null;
    return (DefaultMutableTreeNode)path.getLastPathComponent();
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

	public boolean isFiltered()
	{
		return filtered.size() > 0;
	}

  public Set<String> getAllTags()
  {
    Set<String> allTags = CollectionUtil.caseInsensitiveSet();
    profiles.stream().forEach((prof) -> {
      allTags.addAll(prof.getTags());
    });

    filtered.stream().forEach((prof) ->
    {
      allTags.addAll(prof.getTags());
    });

    return allTags;
  }

  public void resetFilter()
  {
		profiles.addAll(filtered);
		filtered.clear();
		buildTree();
  }

	public void applyTagFilter(Set<String> tags)
	{
		profiles.addAll(filtered);
		filtered.clear();
    if (CollectionUtil.isNonEmpty(tags))
		{
			Iterator<ConnectionProfile> itr = profiles.iterator();
			while (itr.hasNext())
			{
				ConnectionProfile profile = itr.next();
        if (!profile.getTags().containsAll(tags))
        {
					filtered.add(profile);
					itr.remove();
				}
			}
		}
		buildTree();
	}

	public void applyNameFilter(String value)
	{
		profiles.addAll(filtered);
		filtered.clear();
		if (StringUtil.isNonBlank(value))
		{
			value = value.toLowerCase();
			Iterator<ConnectionProfile> itr = profiles.iterator();
			while (itr.hasNext())
			{
				ConnectionProfile profile = itr.next();
				if (!profile.getName().toLowerCase().contains(value))
				{
					filtered.add(profile);
					itr.remove();
				}
			}
		}
		buildTree();
	}

  public boolean isChanged()
  {
    return profilesAreModified() || groupsChanged();
  }

	/**
	 *	Returns true if any of the profile definitions has changed.
	 *	(Or if a profile has been deleted or added)
	 */
	public boolean profilesAreModified()
	{
    if (this.profiles.stream().anyMatch((profile) -> (profile.isChanged()))) return true;
    if (this.filtered.stream().anyMatch((profile) -> (profile.isChanged()))) return true;

		return false;
	}

	public boolean groupsChanged()
	{
    if (profiles.stream().anyMatch((profile) -> (profile.isGroupChanged()))) return true;
    if (filtered.stream().anyMatch((profile) -> (profile.isGroupChanged()))) return true;

		return false;
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
		List<String> result = new LinkedList<>();
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
		for (int i = 0; i < count; i++)
		{
			DefaultMutableTreeNode child = (DefaultMutableTreeNode)node.getChildAt(i);
			ConnectionProfile prof = (ConnectionProfile)child.getUserObject();
			profiles.remove(prof);
		}
		node.removeAllChildren();
	}

  public void deleteNodes(DefaultMutableTreeNode[] node)
  {

  }

	public void deleteProfile(ConnectionProfile prof)
	{
		TreePath path = getPath(prof);
		MutableTreeNode node = (MutableTreeNode)path.getLastPathComponent();
		if (!node.isLeaf()) return;

		profiles.remove(prof);
		this.removeNodeFromParent(node);
	}

	public TreePath getFirstProfile()
	{
		if (this.rootNode.getChildCount() == 0) return null;
		TreeNode defGroup = this.rootNode.getChildAt(0);
		Object profile = defGroup.getChildAt(0);
		return new TreePath( new Object[] { rootNode, defGroup, profile });
	}

	public TreePath getPath(ProfileKey def)
	{
		if (def == null) return null;
		ConnectionProfile prof = ProfileManager.findProfile(profiles, def);
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


	public int getSize()
	{
		return this.profiles.size();
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
		ConnectionProfile dummy = ConnectionProfile.createEmptyProfile();
		dummy.setUrl("jdbc:");
		profiles.add(dummy);
		buildTree();
	}

	public void removeGroupNode(DefaultMutableTreeNode groupNode)
	{
		deleteGroupProfiles(groupNode);
		this.removeNodeFromParent(groupNode);
	}

  public void saveTo(File file)
  {
    ProfileManager mgr = new ProfileManager(file);
    mgr.applyProfiles(getAllProfiles());
    mgr.save();
    sourceFile = file;
  }

	public void saveProfiles()
	{
		applyProfiles();
		ConnectionMgr.getInstance().saveProfiles();
		resetChanged();
	}

	public void resetChanged()
	{
		for (ConnectionProfile profile : profiles)
		{
			profile.resetChangedFlags();
		}
	}

	public List<ConnectionProfile> getAllProfiles()
	{
		List<ConnectionProfile> current = new ArrayList<>(profiles.size() + filtered.size());
		for (ConnectionProfile prof : profiles)
		{
			current.add(prof);
		}
		for (ConnectionProfile prof : filtered)
		{
			current.add(prof);
		}
		return current;
	}

	public void applyProfiles()
	{
		ConnectionMgr.getInstance().applyProfiles(getAllProfiles());
	}

	private void buildTree()
	{
		ProfileGroupMap groupMap = new ProfileGroupMap(profiles);
		rootNode.removeAllChildren();

		// Make sure the default group is added as the first item!
		List<String> groups = new ArrayList<>();
		groups.addAll(groupMap.keySet());
		Collections.sort(groups, CaseInsensitiveComparator.INSTANCE);

		for (String group : groups)
		{
			DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(group, true);
			rootNode.add(groupNode);
			List<ConnectionProfile> groupProfiles = groupMap.get(group);

			sortList(groupProfiles);
			for (ConnectionProfile prof : groupProfiles)
			{
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
		// moveProfilesToGroup(nodes, target);
	}

  public void removeNodesFromParen(DefaultMutableTreeNode[] profileNodes)
  {
		if (profileNodes == null) return;
		for (DefaultMutableTreeNode profileNode : profileNodes)
    {
      removeNodeFromParent(profileNode);
    }
  }

	public DefaultMutableTreeNode moveProfilesToGroup(List<ConnectionProfile> droppedProfiles, DefaultMutableTreeNode groupNode)
	{
		if (CollectionUtil.isEmpty(droppedProfiles)) return null;
		if (groupNode == null) return null;

		String groupName = (String)groupNode.getUserObject();

    DefaultMutableTreeNode firstNode = null;
		for (ConnectionProfile profile : droppedProfiles)
		{
			if (profile == null) continue;

			profile.setGroup(groupName);

      // this method is called as part of a Drag & Drop or Copy & Paste action
      // we only need to take care of inserting the new node. The TransferHandler
      // will take care of removing the original node from it's parent in the model
      // this is necessary to support transfer between two differen trees
      DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(profile, false);
      if (firstNode == null)
      {
        firstNode = newNode;
      }
			insertNodeInto(newNode, groupNode, groupNode.getChildCount());
		}
    return firstNode;
	}

	public DefaultMutableTreeNode copyProfilesToGroup(List<ConnectionProfile> droppedProfiles, DefaultMutableTreeNode groupNode)
	{
		if (CollectionUtil.isEmpty(droppedProfiles)) return null;
		if (groupNode == null) return null;

		String groupName = (String)groupNode.getUserObject();

    DefaultMutableTreeNode firstNode = null;
		for (ConnectionProfile profile : droppedProfiles)
		{
			if (profile == null) continue;
      ConnectionProfile copy = profile.createCopy();
      copy.setGroup(groupName);
      profiles.add(copy);

      // this method is called as part of a Drag & Drop or Copy & Paste action
      // we only need to take care of inserting the new node. The TransferHandler
      // will take care of removing the original node from it's parent in the model
      // this is necessary to support transfer between two differen trees
      DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(copy, false);
      if (firstNode == null)
      {
        firstNode = newNode;
      }
			insertNodeInto(newNode, groupNode, groupNode.getChildCount());
		}
    return firstNode;
	}

  public static ProfileListModel emptyModel()
  {
    return new ProfileListModel(new ArrayList<>());
  }

  public static ProfileListModel getDummyModel()
  {
    List<ConnectionProfile> profiles = new ArrayList<>();
    ConnectionProfile one = new ConnectionProfile();
    one.setName("Admin");
    one.setGroup("Postgres");
    profiles.add(one);

    ConnectionProfile two = new ConnectionProfile();
    two.setName("Arthur");
    two.setGroup("Postgres");
    profiles.add(two);

    ConnectionProfile three = new ConnectionProfile();
    three.setName("Zaphod");
    three.setGroup("Oracle");
    profiles.add(three);

    ConnectionProfile four = new ConnectionProfile();
    four.setName("Tricia");
    four.setGroup("Oracle");
    profiles.add(four);
    return new ProfileListModel(profiles);
  }
}
