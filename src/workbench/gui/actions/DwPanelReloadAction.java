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

package workbench.gui.actions;

import java.awt.event.ActionEvent;
import workbench.gui.sql.DwPanel;
import workbench.gui.sql.SqlPanel;
import workbench.log.LogMgr;
import workbench.storage.DataStore;

/**
 *
 * @author support@sql-workbench.net
 */
public class DwPanelReloadAction
	extends WbAction
{
	private SqlPanel client;

	public DwPanelReloadAction(SqlPanel panel)
	{
		client = panel;
		initMenuDefinition("TxtReload");
	}

	@Override
	public boolean isEnabled()
	{
		if (getSql() != null)
		{
			DwPanel dw =  client.getCurrentResult();
			if (dw == null) return false;
			DataStore ds = dw.getDataStore();
			if (ds == null) return false;
			return ds.getOriginalConnection() != null;
		}
		return false;
	}

	protected String getSql()
	{
		if (client == null) return null;
		if (client.getCurrentResult() == null) return null;
		DataStore ds = client.getCurrentResult().getDataStore();
		if (ds == null) return null;

		String sql = ds.getGeneratingSql();
		return sql;
	}

	@Override
	public void executeAction(ActionEvent evt)
	{
		client.reloadCurrent();
	}

}
