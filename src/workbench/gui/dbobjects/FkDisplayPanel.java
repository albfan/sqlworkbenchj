/*
 * FkDisplayPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.dbobjects;

import java.awt.BorderLayout;
import java.sql.SQLException;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import workbench.db.FKHandler;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.WbScrollPane;
import workbench.gui.components.WbSplitPane;
import workbench.gui.components.WbTable;
import workbench.interfaces.Resettable;
import workbench.log.LogMgr;
import workbench.util.ExceptionUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class FkDisplayPanel
	extends JPanel
	implements Resettable
{

	protected WbTable keys;
	private TableDependencyTreeDisplay dependencyTree;
	private WbSplitPane splitPanel;
	private boolean showImportedKeys;
	private WbConnection dbConnection;

	public FkDisplayPanel(TableLister lister, boolean showImported)
	{
		super(new BorderLayout());
		this.keys = new WbTable();
		this.keys.setAdjustToColumnLabel(false);
		WbScrollPane scroll = new WbScrollPane(this.keys);
		this.splitPanel = new WbSplitPane(JSplitPane.VERTICAL_SPLIT);
		this.splitPanel.setDividerLocation(100);
		this.splitPanel.setDividerSize(8);
		this.splitPanel.setTopComponent(scroll);
		this.dependencyTree = new TableDependencyTreeDisplay(lister);
		this.splitPanel.setBottomComponent(this.dependencyTree);
		this.add(splitPanel, BorderLayout.CENTER);
		showImportedKeys = showImported;
	}

	public void setConnection(WbConnection conn)
	{
		dbConnection = conn;
		dependencyTree.setConnection(conn);
	}

	public int getDividerLocation()
	{
		return splitPanel.getDividerLocation();
	}

	public void setDividerLocation(int location)
	{
		splitPanel.setDividerLocation(location);
	}

	public TableDependencyTreeDisplay getTree()
	{
		return dependencyTree;
	}

	public WbTable getKeyDisplay()
	{
		return keys;
	}

	@Override
	public void reset()
	{
		keys.reset();
		dependencyTree.reset();
	}


	protected void retrieve(TableIdentifier table)
		throws SQLException
	{
		FKHandler handler = new FKHandler(dbConnection);
		final DataStoreTableModel model;
		if (showImportedKeys)
		{
			model = new DataStoreTableModel(handler.getForeignKeys(table, false));
		}
		else
		{
			model = new DataStoreTableModel(handler.getReferencedBy(table));
		}
		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				keys.setModel(model, true);
				keys.adjustRowsAndColumns();
			}
		});
		retrieveTree(table);
	}

	protected void retrieveTree(TableIdentifier table)
	{
		if (showImportedKeys)
		{
			dependencyTree.readReferencedTables(table);
		}
		else
		{
			dependencyTree.readReferencingTables(table);
		}
	}
}
