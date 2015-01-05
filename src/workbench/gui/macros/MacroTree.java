/*
 * MacroTree.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
package workbench.gui.macros;

import java.awt.Dimension;
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
import javax.swing.JToolTip;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import workbench.interfaces.ClipboardSupport;
import workbench.interfaces.ExpandableTree;
import workbench.interfaces.GroupTree;
import workbench.resource.ResourceMgr;

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.WbAction;
import workbench.gui.components.MultiLineToolTip;
import workbench.gui.menu.CutCopyPastePopup;

import workbench.sql.macros.MacroDefinition;
import workbench.sql.macros.MacroGroup;
import workbench.sql.macros.MacroManager;
import workbench.sql.macros.MacroStorage;

import workbench.util.StringUtil;

/**
 * A Tree to display macro groups
 * It supports drag & drop from macaros into different groups
 *
 * @author Thomas Kellerer
 */
public class MacroTree
	extends JTree
	implements TreeModelListener,
	           MouseListener,
	           ClipboardSupport,
	           ActionListener,
	           TreeSelectionListener,
						 GroupTree,
						 ExpandableTree
{
	private MacroListModel macroModel;
	private MacroTreeNode[] clipboardNodes;
	private static final int CLIP_COPY = 1;
	private static final int CLIP_CUT = 2;
	private int clipboardType;
	private CutCopyPastePopup popup;
	private WbAction pasteToFolderAction;

	private Insets autoscrollInsets = new Insets(20, 20, 20, 20);
	private final int macroClientId;

	public MacroTree(int clientId, boolean forPopup)
	{
		super();
		macroClientId = clientId;
		loadMacros(forPopup);
		setRootVisible(false);
		putClientProperty("JTree.lineStyle", "Angled");
		setShowsRootHandles(true);
		setEditable(false);
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
		popup.addAction(pasteToFolderAction, false);

		MacroTreeCellRenderer renderer = new MacroTreeCellRenderer();
		setCellRenderer(renderer);
		setAutoscrolls(true);
		this.setMinimumSize(new Dimension(200, 400));
		new MacroTreeDragHandler(this, DnDConstants.ACTION_COPY_OR_MOVE);
		WbSwingUtilities.adjustTreeRowHeight(this);
	}

	public final void loadMacros(boolean forPopup)
	{
		if (macroModel != null)
		{
			macroModel.removeTreeModelListener(this);
		}
		macroModel = new MacroListModel(MacroManager.getInstance().getMacros(macroClientId), forPopup);
		setModel(macroModel);
		macroModel.addTreeModelListener(this);
	}

	public void addPopupActionAtTop(WbAction a)
	{
		this.popup.insert(a, 0);
	}

	public void addPopupAction(WbAction action, boolean withSeparator)
	{
		if (withSeparator) this.popup.addSeparator();
		this.popup.add(action);
		InputMap im = this.getInputMap(WHEN_FOCUSED);
		ActionMap am = this.getActionMap();
		action.addToInputMap(im, am);
	}

	@Override
	public void treeNodesChanged(TreeModelEvent e)
	{
		Object[] changed = e.getChildren();
		if (changed == null) return;

		MacroTreeNode node = (MacroTreeNode)changed[0];

		Object data = node.getDataObject();
		if (data == null) return;

		if (node.getAllowsChildren())
		{
			// If a node is edited in a JTree, the new value that has been
			// entered by the user is passed in as the UserObject of the changed
			// node (that's the reason why a separate MacroTreeNode class is used
			// that preserves the original "user object"
			MacroGroup group = (MacroGroup)node.getDataObject();
			group.setName(data.toString());
		}
	}

	@Override
	public void expandAll()
	{
		TreePath[] groups = this.macroModel.getGroupNodes();
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
		TreePath[] groups = this.macroModel.getGroupNodes();
		for (TreePath group : groups)
		{
			if (group != null)
			{
				collapsePath(group);
			}
		}
	}

	public void selectMacro(String groupName, String macroName)
	{
		TreePath[] groupNodes = this.macroModel.getGroupNodes();
		for (TreePath path : groupNodes)
		{
			MacroTreeNode node = (MacroTreeNode)path.getLastPathComponent();
			if (node.isLeaf()) continue;

			MacroGroup group = (MacroGroup)node.getDataObject();
			if (group.getName().equals(groupName))
			{
				expandPath(path);
				selectNode(node);

				int elements = node.getChildCount();
				for (int i=0; i < elements; i++)
				{
					MacroTreeNode macroNode = (MacroTreeNode)node.getChildAt(i);
					if (!macroNode.isLeaf()) continue;
					MacroDefinition macro = (MacroDefinition)macroNode.getDataObject();
					if (macro != null && macro.getName().equals(macroName))
					{
						selectNode(macroNode);
						break;
					}
				}
			}
		}
	}

	public boolean isModified()
	{
		MacroStorage current = this.macroModel.getMacros();
		return current.isModified();
	}

	public void saveChanges()
	{
		MacroStorage current = this.macroModel.getMacros();
		MacroManager.getInstance().getMacros(macroClientId).copyFrom(current);
		MacroManager.getInstance().save(macroClientId);
		current.resetModified();
	}

	/**
	 * Expand the groups that are contained in th list.
	 * The list is expected to contain Sting objects that identify
	 * the names of the groups.
	 */
	public void expandGroups(List<String> groupList)
	{
		if (groupList == null) return;
		TreePath[] groupNodes = this.macroModel.getGroupNodes();
		if (groupNodes == null) return;
		for (TreePath groupNode : groupNodes)
		{
			MacroTreeNode node = (MacroTreeNode) groupNode.getLastPathComponent();
			if (!node.getAllowsChildren()) continue;
			MacroGroup g = (MacroGroup)node.getDataObject();
			if (groupList.contains(g.getName()))
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
		TreePath[] groupNodes = this.macroModel.getGroupNodes();
		for (TreePath groupNode : groupNodes)
		{
			if (isExpanded(groupNode))
			{
				MacroTreeNode node = (MacroTreeNode) groupNode.getLastPathComponent();
				MacroGroup g = (MacroGroup)node.getDataObject();
				result.add(g.getName());
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
		boolean canCopy = onlyMacrosSelected();

		pasteToFolderAction.setEnabled(canPaste);

		WbAction a = popup.getPasteAction();
		a.setEnabled(canPaste);

		a = popup.getCopyAction();
		a.setEnabled(canCopy);

		a = popup.getCutAction();
		a.setEnabled(canCopy);

	}

	/**
	 * Checks if the current selection contains only profiles
	 */
	public boolean onlyMacrosSelected()
	{
		TreePath[] selection = getSelectionPaths();
		if (selection == null) return false;
		for (TreePath tp : selection)
		{
			TreeNode n = (TreeNode) tp.getLastPathComponent();
			if (n.getAllowsChildren()) return false;
		}
		return true;
	}

	public boolean onlyGroupSelected()
	{
		if (getSelectionCount() > 1) return false;
		TreePath[] selection = getSelectionPaths();
		return onlyGroupSelected(selection);
	}

	/**
	 * Checks if the current selection contains only groups
	 */
	public boolean onlyGroupSelected(TreePath[] selection)
	{
		if (selection == null) return false;
		for (TreePath tp : selection)
		{
			TreeNode n = (TreeNode) tp.getLastPathComponent();
			if (!n.getAllowsChildren()) return false;
		}
		return true;
	}

	protected MacroTreeNode getSelectedNode()
	{
		TreePath[] selection = getSelectionPaths();
		if (selection == null) return null;
		if (selection.length != 1) return null;

		MacroTreeNode node = (MacroTreeNode)getLastSelectedPathComponent();
		return node;
	}


	/**
	 * Returns the current group. That is either the currently selected group
	 * or the group of the currently selected macro(s).
	 */
	public MacroTreeNode getCurrentGroupNode()
	{
		TreePath[] selection = getSelectionPaths();
		if (selection == null) return null;

		MacroTreeNode node = (MacroTreeNode)getLastSelectedPathComponent();
		if (node == null) return null;

		if (node.getAllowsChildren()) return node;

		MacroTreeNode parent = (MacroTreeNode)node.getParent();
		return parent;
	}

	/**
	 * Returns the current group. That is either the currently selected group
	 * or the group of the currently selected macro(s).
	 */
	public MacroGroup getCurrentGroup()
	{
		MacroTreeNode node = getCurrentGroupNode();
		if (node == null) return null;

		Object userData = node.getDataObject();

		if (userData instanceof MacroGroup)
		{
			return (MacroGroup)userData;
		}
		return null;
	}

	/**
	 * Returns the currently selected Macro. If either more then one
	 * entry is selected or a group is selected, null is returned
	 *
	 * @return the selected profile if any
	 */
	public MacroDefinition getSelectedMacro()
	{
		TreePath[] selection = getSelectionPaths();
		if (selection == null) return null;
		if (selection.length != 1) return null;

		MacroTreeNode node = (MacroTreeNode)getLastSelectedPathComponent();
		if (node == null) return null;

		Object o = node.getDataObject();
		if (o instanceof MacroDefinition)
		{
			return (MacroDefinition)o;
		}
		return null;
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

		this.clipboardNodes = new MacroTreeNode[p.length];
		for (int i = 0; i < p.length; i++)
		{
			this.clipboardNodes[i] = (MacroTreeNode)p[i].getLastPathComponent();
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

		MacroTreeNode group = (MacroTreeNode)getLastSelectedPathComponent();
		if (group == null) return;
		if (!group.getAllowsChildren()) return;

		try
		{
			if (clipboardType == CLIP_CUT)
			{
				macroModel.moveMacrosToGroup(clipboardNodes, group);
			}
			else if (clipboardType == CLIP_COPY)
			{
				macroModel.copyMacrosToGroup(clipboardNodes, group);
			}
		}
		finally
		{
			this.clipboardType = 0;
			this.clipboardNodes = null;
		}
	}

	public void handleDroppedNodes(MacroTreeNode[] nodes, MacroTreeNode newParent, int action)
	{
		if (nodes == null || nodes.length < 1) return;
		if (newParent == null) return;

		if (action == DnDConstants.ACTION_MOVE)
		{
			macroModel.moveNodes(nodes, newParent);
		}
		else if (action == DnDConstants.ACTION_COPY)
		{
			macroModel.copyMacrosToGroup(nodes, newParent);
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

	public void deleteSelection()
	{
		MacroTreeNode node = getSelectedNode();
		TreePath newSelection = null;
		if (node != null)
		{
			newSelection = macroModel.deleteNode(node);
		}
		selectPath(newSelection);
	}

	public boolean addMacro(boolean copyCurrent)
	{
		MacroDefinition current = getSelectedMacro();
		MacroGroup group = getCurrentGroup();
		if (group == null)
		{
			String newName = addGroup();
			if (newName == null) return false;
			group = getCurrentGroup();
		}
		MacroDefinition newMacro = null;
		if (current != null && copyCurrent)
		{
			newMacro = current.createCopy();
			newMacro.setShortcut(null);
			newMacro.setName(ResourceMgr.getString("TxtCopyOf") + " " + current.getName());
		}
		else
		{
			newMacro = new MacroDefinition(ResourceMgr.getString("LblNewMacro"), "");
		}
		group.addMacro(newMacro);
		TreePath newItem = macroModel.addMacro(group, newMacro);
		selectPath(newItem);

		return newItem != null;
	}

	/**
	 * Prompts the user for a group name and creates a new group
	 * with the provided name. The new group node is automatically
	 * selected after creation.
	 * @return the name of the new group or null if the user cancelled the name input
	 */
	@Override
	public String addGroup()
	{
		String group = WbSwingUtilities.getUserInput(SwingUtilities.getWindowAncestor(this), ResourceMgr.getString("LblNewProfileGroup"), "");
		if (StringUtil.isEmptyString(group)) return null;

		if (macroModel.getMacros().containsGroup(group))
		{
			WbSwingUtilities.showErrorMessageKey(SwingUtilities.getWindowAncestor(this), "ErrGroupNotUnique");
			return null;
		}
		TreePath path = this.macroModel.addGroup(group);
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

	private void selectNode(MacroTreeNode node)
	{
		TreeNode[] nodes = this.macroModel.getPathToRoot(node);
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

	@Override
	public JToolTip createToolTip()
	{
		JToolTip tip = new MultiLineToolTip();
		tip.setComponent(this);
		return tip;
	}

}
