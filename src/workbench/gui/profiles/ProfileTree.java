/*
 * ProfileTree.java
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

import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.dnd.DnDConstants;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.LinkedList;
import java.util.List;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import workbench.interfaces.ClipboardSupport;
import workbench.interfaces.ExpandableTree;
import workbench.interfaces.GroupTree;
import workbench.resource.ResourceMgr;

import workbench.db.ConnectionProfile;

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.DeleteListEntryAction;
import workbench.gui.actions.WbAction;
import workbench.gui.menu.CutCopyPastePopup;

import workbench.util.StringUtil;

/**
 * A tree to display connection profiles and profile groups.
 *
 * It supports drag & drop from profiles into different groups.
 *
 * @author Thomas Kellerer
 */
public class ProfileTree
	extends JTree
	implements TreeModelListener, MouseListener, ClipboardSupport, ActionListener, TreeSelectionListener,
						 GroupTree, ExpandableTree
{
	private ProfileListModel profileModel;
	private DefaultMutableTreeNode[] clipboardNodes;
	private static final int CLIP_COPY = 1;
	private static final int CLIP_CUT = 2;
	private int clipboardType;
	private CutCopyPastePopup popup;
	private WbAction pasteToFolderAction;

	private Insets autoscrollInsets = new Insets(20, 20, 20, 20);

	public ProfileTree()
	{
		super(ProfileListModel.getDummyModel());
		setRootVisible(false);
		putClientProperty("JTree.lineStyle", "Angled");
		setShowsRootHandles(true);
		setEditable(true);
		setExpandsSelectedPaths(true);
		addMouseListener(this);
		getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
		addTreeSelectionListener(this);

		InputMap im = this.getInputMap(WHEN_FOCUSED);
		ActionMap am = this.getActionMap();

    this.popup = new CutCopyPastePopup(this);

    WbAction a = popup.getPasteAction();
    a.addToInputMap(im, am);

    a = popup.getCopyAction();
    a.addToInputMap(im, am);

    a = popup.getCutAction();
    a.addToInputMap(im, am);

    pasteToFolderAction = new WbAction(this, "pasteToFolder");
    pasteToFolderAction.removeIcon();
    pasteToFolderAction.initMenuDefinition("MnuTxtPasteNewFolder");
    popup.addAction(pasteToFolderAction, true);

    RenameGroupAction renameGroup = new RenameGroupAction(this);
    popup.addAction(renameGroup, false);

		setCellRenderer(new ProfileTreeCellRenderer(getCellRenderer()));
//    System.out.println(getCellRenderer().getClass());
    new ProfileTreeDragHandler(this, DnDConstants.ACTION_COPY_OR_MOVE);
		setAutoscrolls(true);

		// setting the row height to 0 makes it dynamic
		// so it will adjust properly to the font of the renderer
		setRowHeight(0);
	}

  public void setDeleteAction(DeleteListEntryAction delete)
  {
    this.popup.addSeparator();
    this.popup.add(delete);
    InputMap im = this.getInputMap(WHEN_FOCUSED);
    ActionMap am = this.getActionMap();
    delete.addToInputMap(im, am);
  }

	@Override
	public void setModel(TreeModel model)
	{
		super.setModel(model);
		if (model instanceof ProfileListModel)
		{
			this.profileModel = (ProfileListModel)model;
		}
		model.addTreeModelListener(this);
	}

	@Override
	public boolean isPathEditable(TreePath path)
	{
		if (path == null) return false;
		// Only allow editing of groups
		if (path.getPathCount() != 2) return false;

		DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();

		return node.getAllowsChildren();
	}

	@Override
	public void treeNodesChanged(TreeModelEvent e)
	{
		Object[] changed = e.getChildren();
		DefaultMutableTreeNode group = (DefaultMutableTreeNode)changed[0];
		Object data = group.getUserObject();

		if (group.getAllowsChildren())
		{
			String newGroupName = (String)data;
			renameGroup(group, newGroupName);
		}
		else if (data instanceof ConnectionProfile)
		{
			// If the connection profile has changed, the title
			// of the profile possibly changed as well, so we need to
			// trigger a repaint to display the correct title
			// in the tree
			WbSwingUtilities.repaintLater(this);
		}
	}

	@Override
	public void expandAll()
	{
		TreePath[] groups = this.profileModel.getGroupNodes();
		for (TreePath group : groups)
		{
			if (group != null)
			{
				expandPath(group);
			}
		}
	}

	@Override
	public void collapseAll()
	{
		TreePath[] groups = this.profileModel.getGroupNodes();
		for (TreePath group : groups)
		{
			if (group != null)
			{
				collapsePath(group);
			}
		}
	}

	/**
	 * Expand the groups that are contained in th list.
	 * The list is expected to contain Sting objects that identify
	 * the names of the groups.
	 */
	public void expandGroups(List groupList)
	{
		if (groupList == null) return;
		TreePath[] groupNodes = this.profileModel.getGroupNodes();
		if (groupNodes == null) return;
		for (TreePath groupNode : groupNodes)
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) groupNode.getLastPathComponent();
			String g = (String)node.getUserObject();
			if (groupList.contains(g))
			{
				if (!isExpanded(groupNode))
				{
					expandPath(groupNode);
				}
			}
		}
	}

	/**
	 * Return the names of the expaned groups.
	 */
	public List<String> getExpandedGroupNames()
	{
		LinkedList<String> result = new LinkedList<>();
		TreePath[] groupNodes = this.profileModel.getGroupNodes();
		for (TreePath groupNode : groupNodes)
		{
			if (isExpanded(groupNode))
			{
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) groupNode.getLastPathComponent();
				String g = (String)node.getUserObject();
				result.add(g);
			}
		}
		return result;
	}

	@Override
	public void treeNodesInserted(TreeModelEvent e)
	{
	}

	@Override
	public void treeNodesRemoved(TreeModelEvent e)
	{
	}

	@Override
	public void treeStructureChanged(TreeModelEvent e)
	{
	}

	public boolean isGroup(TreePath p)
	{
		if (p == null) return false;
		TreeNode n = (TreeNode)p.getLastPathComponent();
		return n.getAllowsChildren();
	}

	/**
	 * Enable/disable the cut/copy/paste actions
	 * according to the current selection and the content
	 * of the "clipboard"
	 */
	private void checkActions()
	{
		boolean groupSelected = onlyGroupSelected();
		boolean canPaste = this.clipboardNodes != null && groupSelected;
		boolean canCopy = onlyProfilesSelected();

		pasteToFolderAction.setEnabled(canPaste);

		WbAction a = popup.getPasteAction();
		a.setEnabled(canPaste);

		a = popup.getCopyAction();
		a.setEnabled(canCopy);

		a = popup.getCutAction();
		a.setEnabled(canCopy);

	}

	@Override
	public void mouseClicked(MouseEvent e)
	{
		if (e.getButton() == MouseEvent.BUTTON3 && e.getClickCount() == 1)
		{
			TreePath p = this.getClosestPathForLocation(e.getX(), e.getY());
			if (p == null) return;

			if (this.getSelectionCount() == 1 || isGroup(p))
			{
				setSelectionPath(p);
			}
			checkActions();
			popup.show(this, e.getX(), e.getY());
		}
	}

	/**
	 * Finds and selects the connection profile with the given name.
	 *
	 * If the profile is not found, the first profile
	 * will be selected and its group expanded
	 */
	public void selectProfile(ProfileKey key)
	{
		selectProfile(key, true);
	}

	public void selectProfile(ProfileKey key, boolean selectFirst)
	{
		if (profileModel == null) return;
		TreePath path = this.profileModel.getPath(key);
		if (path == null && selectFirst)
		{
			path = this.profileModel.getFirstProfile();
		}
		selectPath(path); // selectPath can handle a null value
	}

	/**
	 * Checks if the current selection contains only profiles
	 */
	public boolean onlyProfilesSelected()
	{
		TreePath[] selection = getSelectionPaths();
		if (selection == null) return false;

		for (TreePath element : selection)
		{
			TreeNode n = (TreeNode)element.getLastPathComponent();
			if (n.getAllowsChildren()) return false;
		}
		return true;
	}

	/**
	 * Checks if the current selection contains only groups
	 */
	public boolean onlyGroupSelected()
	{
		if (getSelectionCount() > 1) return false;
		TreePath[] selection = getSelectionPaths();
		if (selection == null) return false;
		for (TreePath element : selection)
		{
			TreeNode n = (TreeNode) element.getLastPathComponent();
			if (!n.getAllowsChildren()) return false;
		}
		return true;
	}

	protected DefaultMutableTreeNode getSelectedGroupNode()
	{
		TreePath[] selection = getSelectionPaths();
		if (selection == null) return null;
		if (selection.length != 1) return null;

		DefaultMutableTreeNode node = (DefaultMutableTreeNode)getLastSelectedPathComponent();
		if (node != null && node.getAllowsChildren()) return node;
		return null;
	}

	/**
	 * Returns the currently selected Profile. If either more then one
	 * entry is selected or a group is selected, null is returned
	 *
	 * @return the selected profile if any
	 */
	public ConnectionProfile getSelectedProfile()
	{
		TreePath[] selection = getSelectionPaths();
		if (selection == null) return null;
		if (selection.length != 1) return null;

		DefaultMutableTreeNode node = (DefaultMutableTreeNode)getLastSelectedPathComponent();
		if (node == null) return null;

		Object o = node.getUserObject();
		if (o instanceof ConnectionProfile)
		{
			ConnectionProfile prof = (ConnectionProfile)o;
			return prof;
		}
		return null;
	}

	@Override
	public void mousePressed(MouseEvent e)
	{
	}

	@Override
	public void mouseReleased(MouseEvent e)
	{
	}

	@Override
	public void mouseEntered(MouseEvent e)
	{
	}

	@Override
	public void mouseExited(MouseEvent e)
	{
	}

	/**
	 * Stores the selected nodes in the internal "clipboard"
	 */
	private void storeSelectedNodes()
	{
		TreePath[] p = getSelectionPaths();

		this.clipboardNodes = new DefaultMutableTreeNode[p.length];
		for (int i = 0; i < p.length; i++)
		{
			this.clipboardNodes[i] = (DefaultMutableTreeNode)p[i].getLastPathComponent();
		}
	}

	@Override
	public void copy()
	{
		storeSelectedNodes();
		this.clipboardType = CLIP_COPY;
	}

	@Override
	public void selectAll()
	{
	}

	@Override
	public void clear()
	{
	}

	@Override
	public void cut()
	{
		storeSelectedNodes();
		this.clipboardType = CLIP_CUT;
	}

	@Override
	public void paste()
	{
		if (clipboardNodes == null) return;
		if (clipboardNodes.length == 0) return;

		DefaultMutableTreeNode group = (DefaultMutableTreeNode)getLastSelectedPathComponent();
		if (group == null) return;
		if (!group.getAllowsChildren()) return;

		try
		{
			if (clipboardType == CLIP_CUT)
			{
				profileModel.moveProfilesToGroup(clipboardNodes, group);
			}
			else if (clipboardType == CLIP_COPY)
			{
				profileModel.copyProfilesToGroup(clipboardNodes, group);
			}
		}
		finally
		{
			this.clipboardType = 0;
			this.clipboardNodes = null;
		}
	}

  public void handleCopiedNodes(DefaultMutableTreeNode[] nodes, DefaultMutableTreeNode newParent)
  {
    if (nodes == null || nodes.length < 1) return;
    if (newParent == null) return;
    
    profileModel.copyProfilesToGroup(nodes, newParent);
  }

	public void handleDroppedNodes(DefaultMutableTreeNode[] nodes, DefaultMutableTreeNode newParent, int action)
	{
		if (nodes == null || nodes.length < 1) return;
		if (newParent == null) return;

		if (action == DnDConstants.ACTION_MOVE)
		{
			profileModel.moveProfilesToGroup(nodes, newParent);
		}
		else if (action == DnDConstants.ACTION_COPY)
		{
			profileModel.copyProfilesToGroup(nodes, newParent);
		}
		selectNode(nodes[0]);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		// invoked from the "paste into new folder" action
		String group = addGroup();
		if (group != null)
		{
			paste();
		}
	}

	/**
	 * Prompts the user for a new group name and renames the currently selected group
	 * to the supplied name.
	 */
	public void renameGroup()
	{
		DefaultMutableTreeNode group = this.getSelectedGroupNode();
		if (group == null) return;
		String oldName = (String)group.getUserObject();
		String newName = WbSwingUtilities.getUserInput(SwingUtilities.getWindowAncestor(this), ResourceMgr.getString("LblNewProfileGroup"), oldName);
		if (StringUtil.isEmptyString(newName)) return;
		group.setUserObject(newName);
		renameGroup(group, newName);
	}

	private void renameGroup(DefaultMutableTreeNode group, String newGroupName)
	{
		if (StringUtil.isEmptyString(newGroupName)) return;
		int count = profileModel.getChildCount(group);
		for (int i = 0; i < count; i++)
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)profileModel.getChild(group,i);
			ConnectionProfile prof = (ConnectionProfile)node.getUserObject();
			prof.setGroup(newGroupName);
		}
	}

	/**
	 * Prompts the user for a group name and creates a new group
	 * with the provided name. The new group node is automatically
	 * after creation.
	 * @return the name of the new group or null if the user cancelled the name input
	 */
	@Override
	public String addGroup()
	{
		String group = WbSwingUtilities.getUserInput(SwingUtilities.getWindowAncestor(this), ResourceMgr.getString("LblNewProfileGroup"), "");
		if (StringUtil.isEmptyString(group)) return null;
		List groups = this.profileModel.getGroups();
		if (groups.contains(group))
		{
			WbSwingUtilities.showErrorMessageKey(SwingUtilities.getWindowAncestor(this), "ErrGroupNotUnique");
			return null;
		}
		TreePath path = this.profileModel.addGroup(group);
		selectPath(path);
		return group;
	}

	public void selectPath(TreePath path)
	{
		if (path == null) return;
		expandPath(path);
		setSelectionPath(path);
		scrollPathToVisible(path);
	}

	private void selectNode(DefaultMutableTreeNode node)
	{
		TreeNode[] nodes = this.profileModel.getPathToRoot(node);
		TreePath path = new TreePath(nodes);
		this.selectPath(path);
	}

	@Override
	public void valueChanged(TreeSelectionEvent e)
	{
		checkActions();
	}

	public void autoscroll(Point cursorLocation)
	{
		Rectangle outer = getVisibleRect();
		Rectangle inner = new Rectangle(
						outer.x + autoscrollInsets.left,
						outer.y + autoscrollInsets.top,
						outer.width - (autoscrollInsets.left + autoscrollInsets.right),
						outer.height - (autoscrollInsets.top+autoscrollInsets.bottom)
					);

		if (!inner.contains(cursorLocation))
		{
			Rectangle scrollRect = new Rectangle(
							cursorLocation.x - autoscrollInsets.left,
							cursorLocation.y - autoscrollInsets.top,
							autoscrollInsets.left + autoscrollInsets.right,
							autoscrollInsets.top + autoscrollInsets.bottom
						);
			scrollRectToVisible(scrollRect);
		}
	}

}
