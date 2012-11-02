/*
 * UpdateTableSelector.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.components;

import java.util.List;

import javax.swing.SwingUtilities;

import workbench.resource.ResourceMgr;

import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.storage.DataStore;

import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class UpdateTableSelector
{
	private WbTable tableData;

	public UpdateTableSelector(WbTable client)
	{
		tableData = client;
	}

	public TableIdentifier selectUpdateTable()
	{
		if (tableData == null) return null;

		DataStore data = tableData.getDataStore();
		if (data == null) return null;

		String csql = data.getGeneratingSql();
		WbConnection conn = data.getOriginalConnection();
		List<String> tables = SqlUtil.getTables(csql, false, conn);

		TableIdentifier table = null;

		if (tables.size() > 1)
		{
			SelectTablePanel p = new SelectTablePanel(tables);

			boolean ok = ValidatingDialog.showConfirmDialog(SwingUtilities.getWindowAncestor(tableData), p, ResourceMgr.getString("MsgSelectTableTitle"));
			String selectedTable = null;
			if (ok)
			{
				selectedTable = p.getSelectedTable();
			}
			if (selectedTable != null)
			{
				table = new TableIdentifier(selectedTable, conn);
			}
		}
		else if (tables.size() == 1)
		{
			table = data.getUpdateTable();
			if (table == null)
			{
				table = new TableIdentifier(tables.get(0), conn);
			}
		}
		return table;
	}

}
