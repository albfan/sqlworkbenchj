/*
 * MacroListModel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.macros;

import java.util.Collection;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import workbench.resource.ResourceMgr;
import workbench.sql.macros.MacroDefinition;
import workbench.sql.macros.MacroGroup;
import workbench.sql.macros.MacroStorage;
import workbench.sql.macros.Sortable;

/**
 *
 * @author Thomas Kellerer
 */
public class MacroListModel
	extends DefaultTreeModel
{
	private	MacroTreeNode rootNode;
	private MacroStorage macros;

	public MacroListModel(MacroStorage original)
	{
		super(null, true);
		this.macros = original.createCopy();
		buildTree();
	}

	public MacroStorage getMacros()
	{
		return macros;
	}

	public TreePath deleteNode(MacroTreeNode node)
	{
		if (node == null) return null;
		Object data = node.getDataObject();
		if (data instanceof MacroDefinition)
		{
			macros.removeMacro((MacroDefinition)data);
		}
		else
		{
			macros.removeGroup((MacroGroup)data);
		}
		
		MacroTreeNode parent = (MacroTreeNode)node.getParent();
		if (parent == null) return null;

		MacroTreeNode newSelection = parent == null ? null : (MacroTreeNode)parent.getChildBefore(node);
		if (newSelection == null)
		{
			newSelection = parent == null ? null : (MacroTreeNode)parent.getChildAfter(node);
		}

		removeNodeFromParent(node);
		
		if (parent != null && newSelection != null)
		{
			return new TreePath(new Object[] { rootNode, parent, newSelection});
		}
		else if (parent != null)
		{
			return new TreePath(new Object[] { rootNode, parent});
		}
		return null;
	}

	private MacroTreeNode findNode(Object userObject)
	{
		return findNode(rootNode, userObject);
	}
	
	private MacroTreeNode findNode(MacroTreeNode startWith, Object userObject)
	{
		if (userObject == null) return null;
		int children = startWith.getChildCount();

		MacroTreeNode node = null;
		for (int i = 0; i < children; i++)
		{
			MacroTreeNode n = (MacroTreeNode)startWith.getChildAt(i);
			if (n == null) continue;
			Object uo = n.getDataObject();
			if (uo != null && uo.equals(userObject))
			{
				return n;
			}
			if (n.getAllowsChildren()) node = findNode(n, userObject);
			if (node != null) return node;
		}
		return null;
	}
	
	public TreePath addMacro(MacroGroup group, MacroDefinition macro)
	{
		if (macro == null) return null;
		if (group == null) return null;

		MacroTreeNode groupNode = findNode(group);
		
		if (groupNode != null)
		{
			MacroTreeNode macroNode = new MacroTreeNode(macro, false);
			insertNodeInto(macroNode, groupNode, groupNode.getChildCount());
			return new TreePath(new Object[] { rootNode, groupNode, macroNode });
		}
		return null;
	}

	public TreePath addGroup(String name)
	{
		if (name == null) return null;
		MacroGroup group = new MacroGroup(name);
		MacroTreeNode node = new MacroTreeNode(group, true);
		macros.addGroup(group);
		insertNodeInto(node, this.rootNode, this.rootNode.getChildCount() );
		return new TreePath(new Object[] { rootNode, node });
	}
	
	public TreePath[] getGroupNodes()
	{
		if (this.rootNode == null) return null;
		int children = rootNode.getChildCount();
		TreePath[] nodes = new TreePath[children];
		for (int i = 0; i < children; i++)
		{
			TreeNode n = (TreeNode)rootNode.getChildAt(i);
			if (n == null) continue;
			nodes[i] = new TreePath(new Object[] { this.rootNode, n } );
		}
		return nodes;
	}
	
	private void buildTree()
	{
		rootNode = new MacroTreeNode("Macros", true);
		Collection<MacroGroup> groups = macros.getGroups();

		for (MacroGroup group : groups)
		{
			MacroTreeNode groupNode = new MacroTreeNode(group, true);
			rootNode.add(groupNode);
			Collection<MacroDefinition> groupMacros = group.getMacros();

			for (MacroDefinition macro : groupMacros)
			{
				MacroTreeNode profNode = new MacroTreeNode(macro, false);
				groupNode.add(profNode);
			}
		}
		this.setRoot(rootNode);
	}

	protected void putInFront(MacroTreeNode source, MacroTreeNode target)
	{
		if (source == null) return;
		if (target == null) return;

		MacroTreeNode targetGroupNode = (MacroTreeNode)target.getParent();
		if (targetGroupNode == null) return;

		MacroTreeNode sourceGroupNode = (MacroTreeNode)source.getParent();
		
		if (!sourceGroupNode.equals(targetGroupNode))
		{
			MacroDefinition sourceMacro = (MacroDefinition)source.getDataObject();
			MacroGroup targetGroup = (MacroGroup)targetGroupNode.getDataObject();
			MacroGroup sourceGroup = (MacroGroup)sourceGroupNode.getDataObject();
			sourceGroup.removeMacro(sourceMacro);
			targetGroup.addMacro(sourceMacro);
		}
		
		int targetIndex = targetGroupNode.getIndex(target);
		removeNodeFromParent(source);
		insertNodeInto(source, (MacroTreeNode)target.getParent(), targetIndex);
		int nodes = targetGroupNode.getChildCount();
		for (int i=0; i < nodes; i++)
		{
			MacroTreeNode node = (MacroTreeNode)targetGroupNode.getChildAt(i);
			Sortable macro = (Sortable)node.getDataObject();
			macro.setSortOrder(i);
		}
		macros.applySort();
	}

	public void copyMacrosToGroup(MacroTreeNode[] source, MacroTreeNode target)
	{
		if (!target.getAllowsChildren()) return;

		MacroGroup targetGroup = (MacroGroup)target.getDataObject();

		for (MacroTreeNode node : source)
		{
			if (node.getAllowsChildren()) continue;
			MacroDefinition macro = (MacroDefinition)node.getDataObject();
			MacroDefinition copy = macro.createCopy();
			copy.setName(ResourceMgr.getString("TxtCopyOf") + " " + macro.getName());
			macros.addMacro(targetGroup, copy);
			int index = target.getChildCount();
			copy.setSortOrder(index + 1);
			insertNodeInto(new MacroTreeNode(copy, false), target, index);
		}
		macros.applySort();
	}

	public void moveMacrosToGroup(MacroTreeNode[] source, MacroTreeNode target)
	{
		if (!target.getAllowsChildren()) return;

		MacroGroup targetGroup = (MacroGroup)target.getDataObject();
		
		for (MacroTreeNode node : source)
		{
			if (node.getAllowsChildren()) continue;
			MacroDefinition macro = (MacroDefinition)node.getDataObject();
			macros.moveMacro(macro, targetGroup);
			removeNodeFromParent(node);
			insertNodeInto(node, target, target.getChildCount());
		}
		
		for (int i=0; i < target.getChildCount(); i++)
		{
			MacroTreeNode node = (MacroTreeNode)target.getChildAt(i);
			MacroDefinition macro = (MacroDefinition)node.getDataObject();
			macro.setSortOrder(i+1);
		}
		macros.applySort();
	}
	
	public void moveNodes(MacroTreeNode[] source, MacroTreeNode target)
	{
		boolean sourceIsGroup = true;
		for (MacroTreeNode node : source)
		{
			sourceIsGroup = sourceIsGroup && node.getAllowsChildren();
		}
		if (sourceIsGroup && target.getAllowsChildren() ||
			 source.length == 1 && !target.getAllowsChildren())
		{
			putInFront(source[0], target);
		}
		else if (target.getAllowsChildren() && !sourceIsGroup)
		{
			moveMacrosToGroup(source, target);
		}
	}
	
}
