/*
 * TableDependencyTreeDisplay.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2004, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.gui.dbobjects;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import workbench.db.DependencyNode;
import workbench.db.TableDependency;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.WbScrollPane;
import workbench.gui.renderer.DependencyTreeCellRenderer;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;


/**
 *
 * @author  info@sql-workbench.net
 */
public class TableDependencyTreeDisplay 
  extends JPanel
{
	private WbConnection connection;
	private DependencyTreeCellRenderer renderer;
	
	private	DefaultMutableTreeNode emptyRoot = new DefaultMutableTreeNode("");
	private ArrayList nodesToExpand;
	private boolean showExported;
	
	public TableDependencyTreeDisplay()
	{
		this.setLayout(new BorderLayout());
	}

	public void setConnection(WbConnection aConn)
	{
		this.connection = aConn;
	}
	
	public void readTree(TableIdentifier aTable, boolean exportedKeys)
	{
		this.renderer = new DependencyTreeCellRenderer();
		this.showExported = exportedKeys;
    try
    {
			WbSwingUtilities.showWaitCursor(this);
      TableDependency dep = new TableDependency();
      dep.setConnection(this.connection);
      dep.setTable(aTable);
      dep.readDependencyTree(exportedKeys);
      DependencyNode root = dep.getRootNode();
      this.readTreeNodes(root);
    }
		catch (OutOfMemoryError mem)
		{
			WbSwingUtilities.showErrorMessage(this, ResourceMgr.getString("MsgOutOfMemoryError"));
		}
    catch (Exception e)
    {
      LogMgr.logError("TableDependencyTreeDisplay.readTree()", "Error reading three", e);
    }
		WbSwingUtilities.showDefaultCursor(this);
	}

	public void reset()
	{
		this.removeAll();//this.createTreeDisplay(emptyRoot);
		this.invalidate();
		this.repaint();
		this.doLayout();
	}
	
	private void createTreeDisplay(DefaultMutableTreeNode root)
	{
		this.removeAll();
		this.invalidate();
		JTree tree = new JTree(root);
		ToolTipManager.sharedInstance().registerComponent(tree);
		tree.putClientProperty("JTree.lineStyle", "Angled");
		tree.setCellRenderer(this.renderer);
		WbScrollPane scroll = new WbScrollPane(tree);
		this.expandNodes(tree);
		this.add(scroll, BorderLayout.CENTER);
		this.updateUI();
		this.repaint();
	}
	
  private void readTreeNodes(DependencyNode root)
  {
		DefaultMutableTreeNode treeRoot = null;
		if (root.getChildren().size() > 0)
		{
			treeRoot = new DefaultMutableTreeNode(root, true);
			this.buildTree(root, treeRoot);
			this.createTreeDisplay(treeRoot);
		}
		else
		{
			this.reset();
		}
  }
	
	private void buildTree(DependencyNode parent, DefaultMutableTreeNode treeParent)
	{
		String parentcatalog = parent.getCatalog();
		String parentschema = parent.getSchema();
		String parenttable = parent.getTable();
			
		DependencyNode child = null;
		String currentfk = null;
		String currenttable = null;
		DefaultMutableTreeNode treeNode = null;
		String catalog = null;
		String schema = null;
		String table = null;
		String fkname = null;
			
		List children = parent.getChildren();
		int count = children.size();
		for (int i=0; i<count; i++)
		{
			child = (DependencyNode)children.get(i);

			treeNode = new DefaultMutableTreeNode(child, true);
			treeNode.setAllowsChildren(true);
			treeParent.add(treeNode);

			int childrenCount = child.getChildren().size();

			if (treeNode != null)
			{
				Map columns = child.getColumns();
				Iterator entries = columns.entrySet().iterator();
				while (entries.hasNext())
				{
					table = child.getTable();
					Entry entry = (Entry)entries.next();
					StringBuffer coldef = new StringBuffer(100);
					coldef.append("<html><b>");
					if (this.showExported)
					{
						coldef.append(table);
						coldef.append('.');
						coldef.append(entry.getKey());
					}
					else
					{
						coldef.append(parenttable);
						coldef.append('.');
						coldef.append(entry.getValue());
					}
					coldef.append("</b> REFERENCES <b>");
					if (this.showExported)
					{
						coldef.append(parenttable);
						coldef.append('.');
						coldef.append(entry.getValue());
					}
					else
					{
						coldef.append(table);
						coldef.append('.');
						coldef.append(entry.getKey());
					}
					coldef.append("</b></html>");
					DefaultMutableTreeNode colnode = new DefaultMutableTreeNode(coldef.toString());
					colnode.setAllowsChildren(false);
					treeNode.add(colnode);
				}
			}
			
			if (childrenCount > 0)
			{
				this.buildTree(child, treeNode);
				TreeNode[] path = treeNode.getPath();
				if (this.nodesToExpand == null) this.nodesToExpand = new ArrayList();
				this.nodesToExpand.add(path);
			}
		}	
	}
	
	private void expandNodes(JTree tree)
	{
		if (this.nodesToExpand == null) return;
		for (int i=0; i < this.nodesToExpand.size(); i++)
		{
			TreeNode[] nodes = (TreeNode[])this.nodesToExpand.get(i);
			TreePath path = new TreePath(nodes);
			tree.expandPath(path);
		}
		this.nodesToExpand.clear();
		this.nodesToExpand = null;
	}
	
}
