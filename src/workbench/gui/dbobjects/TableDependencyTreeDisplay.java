/*
 * TableDependencyTreeDisplay.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.dbobjects;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.swing.JMenuItem;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import workbench.WbManager;

import workbench.db.DependencyNode;
import workbench.db.TableDependency;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.WbScrollPane;
import workbench.gui.renderer.DependencyTreeCellRenderer;
import workbench.interfaces.Resettable;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

/**
 *
 * @author  Thomas Kellerer
 */
public class TableDependencyTreeDisplay
  extends JPanel
	implements Resettable, MouseListener
{
	private WbConnection connection;
	private DependencyTreeCellRenderer renderer;
	private List<TreeNode[]> nodesToExpand;
	private boolean showExported;
	private JTree tree;
	private TableLister tables;

	public TableDependencyTreeDisplay(TableLister lister)
	{
		super();
		setLayout(new BorderLayout());
		setBorder(WbSwingUtilities.EMPTY_BORDER);
		tables = lister;
	}

	public void setConnection(WbConnection aConn)
	{
		connection = aConn;
	}

	public void readReferencedTables(TableIdentifier table)
	{
		if (table == null) return;
		readTree(table, true);
	}

	public void readReferencingTables(TableIdentifier table)
	{
		if (table == null) return;
		readTree(table, false);
	}

	private void readTree(TableIdentifier aTable, boolean exportedKeys)
	{
		this.showExported = exportedKeys;
		try
		{
			WbSwingUtilities.showWaitCursor(this);
			TableDependency dep = new TableDependency(this.connection, aTable);
			if (exportedKeys)
			{
				dep.readTreeForParents();
			}
			else
			{
				dep.readTreeForChildren();
			}

			DependencyNode root = dep.getRootNode();
			this.readTreeNodes(root);
		}
		catch (OutOfMemoryError mem)
		{
			WbManager.getInstance().showOutOfMemoryError();
		}
		catch (Exception e)
		{
			LogMgr.logError("TableDependencyTreeDisplay.readTree()", "Error reading three", e);
		}
		WbSwingUtilities.showDefaultCursor(this);
	}

	public void reset()
	{
		if (tree != null)
		{
			DefaultTreeModel model = new DefaultTreeModel(new DefaultMutableTreeNode(), false);
			tree.setModel(model);
		}
	}

	private void createTreeDisplay(DefaultMutableTreeNode root)
	{
		if (tree == null)
		{
			tree = new JTree();
			renderer = new DependencyTreeCellRenderer();
			tree.putClientProperty("JTree.lineStyle", "Angled");
			tree.setCellRenderer(renderer);
			tree.addMouseListener(this);
			ToolTipManager.sharedInstance().registerComponent(tree);
			WbScrollPane scroll = new WbScrollPane(tree);
			add(scroll, BorderLayout.CENTER);
		}
		DefaultTreeModel model = new DefaultTreeModel(root, false);
		tree.setModel(model);
		this.expandNodes(tree);
		WbSwingUtilities.repaintLater(this);
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
		String parenttable = parent.getTable().getTableName();

		DependencyNode child = null;
		DefaultMutableTreeNode treeNode = null;
		String table = null;

		List children = parent.getChildren();
		int count = children.size();
		for (int i=0; i<count; i++)
		{
			child = (DependencyNode)children.get(i);

			treeNode = new DefaultMutableTreeNode(child, true);
			treeNode.setAllowsChildren(true);
			treeParent.add(treeNode);

			int childrenCount = child.getChildren().size();

			Map columns = child.getColumns();
			Iterator entries = columns.entrySet().iterator();
			while (entries.hasNext())
			{
				table = child.getTable().getTableName();
				Entry entry = (Entry)entries.next();
				StringBuilder coldef = new StringBuilder(100);
				coldef.append("<html><b>");
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
				coldef.append("</b> REFERENCES <b>");
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
				coldef.append("</b></html>");
				DefaultMutableTreeNode colnode = new DefaultMutableTreeNode(coldef.toString());
				colnode.setAllowsChildren(false);
				treeNode.add(colnode);
			}

			if (childrenCount > 0)
			{
				this.buildTree(child, treeNode);
				TreeNode[] path = treeNode.getPath();
				if (this.nodesToExpand == null) this.nodesToExpand = new ArrayList<TreeNode[]>();
				this.nodesToExpand.add(path);
			}
		}
	}

	private void expandNodes(JTree tree)
	{
		if (this.nodesToExpand == null) return;
		for (TreeNode[] nodes : nodesToExpand)
		{
			TreePath path = new TreePath(nodes);
			tree.expandPath(path);
		}
		this.nodesToExpand.clear();
		this.nodesToExpand = null;
	}

	public void mouseClicked(MouseEvent e)
	{
		if (e.getButton() == MouseEvent.BUTTON3 && e.getClickCount() == 1)
		{
			TreePath p = tree.getClosestPathForLocation(e.getX(), e.getY());
			if (p == null) return;

			tree.setSelectionPath(p);

			Object pathObject = p.getLastPathComponent();
			if (pathObject == tree.getModel().getRoot()) return;

			Object o = ((DefaultMutableTreeNode)pathObject).getUserObject();
			if (o instanceof DependencyNode)
			{
				JPopupMenu popup = new JPopupMenu();
				JMenuItem item = new JMenuItem(ResourceMgr.getString("MnuTextSelectInList"));
				DependencyNode node = (DependencyNode)o;
				final TableIdentifier table = node.getTable();
				item.addActionListener(new ActionListener()
				{
					@Override
					public void actionPerformed(ActionEvent e)
					{
						tables.selectTable(table);
					}
				});
				popup.add(item);
				popup.show(tree, e.getX(), e.getY());
			}
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
}
