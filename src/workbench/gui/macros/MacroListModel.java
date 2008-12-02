/*
 * 
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * Copyright 2002-2008, Thomas Kellerer
 * 
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
import workbench.sql.macros.MacroDefinition;
import workbench.sql.macros.MacroGroup;
import workbench.sql.macros.MacroStorage;

/**
 *
 * @author support@sql-workbench.net
 */
public class MacroListModel
	extends DefaultTreeModel
{
	private	MacroTreeNode rootNode = new MacroTreeNode("Macros");
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

	public TreePath addGroup(String name)
	{
		if (name == null) return null;
		MacroGroup group = new MacroGroup(name);
		MacroTreeNode node = new MacroTreeNode(group, true);
		macros.addGroup(group);
		this.insertNodeInto(node, this.rootNode, this.rootNode.getChildCount());
		return new TreePath(new Object[] { rootNode, node });
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
	
	private void buildTree()
	{
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

		MacroTreeNode groupNode = (MacroTreeNode)target.getParent();
		if (groupNode == null) return;

		int targetIndex = groupNode.getIndex(target);
		removeNodeFromParent(source);
		insertNodeInto(source, (MacroTreeNode)target.getParent(), targetIndex);
		int nodes = groupNode.getChildCount();
		for (int i=0; i < nodes; i++)
		{
			MacroTreeNode node = (MacroTreeNode)groupNode.getChildAt(i);
			MacroDefinition macro = (MacroDefinition)node.getDataObject();
			macro.setSortOrder(i);
		}
		macros.applySort();
	}

	protected void moveToGroup(MacroTreeNode[] source, MacroTreeNode target)
	{

	}
	
	public void moveNodes(MacroTreeNode[] source, MacroTreeNode target)
	{
		if (source.length == 1 && !target.getAllowsChildren())
		{
			putInFront(source[0], target);
		}
		else if (target.getAllowsChildren())
		{
			moveToGroup(source, target);
		}
	}
	
}
