/*
 * TableDependencyTreeDisplay.java
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
import workbench.interfaces.Resettable;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.db.DependencyNode;
import workbench.db.TableDependency;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.gui.WbSwingUtilities;
import workbench.gui.components.WbScrollPane;
import workbench.gui.renderer.DependencyTreeCellRenderer;

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
	private TableDependency dependencyReader;
	private boolean retrieveAll = true;

	public TableDependencyTreeDisplay(TableLister lister)
	{
		super();
		setLayout(new BorderLayout());
		setBorder(WbSwingUtilities.EMPTY_BORDER);
		tables = lister;
	}

	public void cancelRetrieve()
	{
		if (dependencyReader != null)
		{
			dependencyReader.cancel();
		}
	}

	public void setRetrieveAll(boolean flag)
	{
		retrieveAll = flag;
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
		reset();
		this.showExported = exportedKeys;
		try
		{
			WbSwingUtilities.showWaitCursor(this);
			dependencyReader = new TableDependency(this.connection, aTable);
			dependencyReader.setRetrieveDirectChildrenOnly(!retrieveAll);
			if (exportedKeys)
			{
				dependencyReader.readTreeForParents();
			}
			else
			{
				dependencyReader.readTreeForChildren();
			}

			DependencyNode root = dependencyReader.getRootNode();
			if (root != null)
			{
				this.readTreeNodes(root);
			}
		}
		catch (OutOfMemoryError mem)
		{
			WbManager.getInstance().showOutOfMemoryError();
		}
		catch (Exception e)
		{
			LogMgr.logError("TableDependencyTreeDisplay.readTree()", "Error reading three", e);
		}
		finally
		{
			dependencyReader = null;
		}
		WbSwingUtilities.showDefaultCursor(this);
	}

	@Override
	public void reset()
	{
		createTree();
		DefaultTreeModel model = new DefaultTreeModel(new DefaultMutableTreeNode(), false);
		tree.setModel(model);
	}

	private void createTree()
	{
		if (tree == null)
		{
			tree = new JTree();
			renderer = new DependencyTreeCellRenderer();
			tree.putClientProperty("JTree.lineStyle", "Angled");
			tree.setCellRenderer(renderer);
			tree.addMouseListener(this);
			tree.setRowHeight(0);
			ToolTipManager.sharedInstance().registerComponent(tree);
			WbScrollPane scroll = new WbScrollPane(tree);
			add(scroll, BorderLayout.CENTER);
		}
	}

	private void createTreeDisplay(final DefaultMutableTreeNode root)
	{
		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				createTree();
				DefaultTreeModel model = new DefaultTreeModel(root, false);
				tree.setModel(model);
				expandNodes();
			}
		});
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

		this.nodesToExpand = new ArrayList<>();

		DefaultMutableTreeNode treeNode = null;
		String table = null;

		List<DependencyNode> children = parent.getChildren();
		for (DependencyNode child : children)
		{
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
				this.nodesToExpand.add(path);
			}
			else if (!retrieveAll)
			{
				nodesToExpand.add(treeNode.getPath());
			}
		}
	}

	private void expandNodes()
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

	@Override
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
