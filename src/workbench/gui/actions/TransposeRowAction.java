/*
 * CopyAsTextAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import workbench.gui.components.WbTable;
import workbench.gui.sql.SqlPanel;
import workbench.log.LogMgr;

import workbench.storage.DataStore;
import workbench.storage.DatastoreTransposer;

/**
 * An action to transpose the selected row in a WbTable.
 *
 * @author  Thomas Kellerer
 */
public class TransposeRowAction
	extends WbAction
	implements ListSelectionListener
{
	private WbTable client;

	public TransposeRowAction(WbTable aClient)
	{
		super();
		this.client = aClient;
		if (client != null)
		{
			client.getSelectionModel().addListSelectionListener(this);
		}
		this.initMenuDefinition("MnuTxtTransposeRow");
		this.setEnabled(false);
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		DatastoreTransposer transpose = new DatastoreTransposer(client.getDataStore());
		int row = client.getSelectedRow();
		DataStore ds = transpose.transposeRow(row);
		showDatastore(ds);
	}

	private void showDatastore(DataStore ds)
	{
		SqlPanel p = findPanel();
		if (p != null)
		{
			try
			{
				p.showData(ds);
			}
			catch (SQLException sql)
			{
				LogMgr.logError("TransposeRowAction.showDataStore()", "Could not display datastore", sql);
			}
		}
	}

	private SqlPanel findPanel()
	{
		if (client == null) return null;
		Component c = client.getParent();
		while (c != null)
		{
			if (c instanceof SqlPanel)
			{
				return (SqlPanel)c;
			}
			c = c.getParent();
		}
		return null;
	}

	@Override
	public void valueChanged(ListSelectionEvent e)
	{
		boolean singleRow = false;
		if (client != null)
		{
			singleRow = client.getSelectedRowCount() == 1;
		}
		setEnabled(singleRow);
	}
}
