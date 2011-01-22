/*
 * SqlPanelReloadAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import workbench.gui.sql.DwPanel;
import workbench.gui.sql.SqlPanel;
import workbench.resource.ResourceMgr;
import workbench.storage.DataStore;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlPanelReloadAction
	extends WbAction
{
	private SqlPanel client;

	public SqlPanelReloadAction(SqlPanel panel)
	{
		initMenuDefinition("TxtReloadResult");
		setMenuItemName(ResourceMgr.MNU_TXT_DATA);
		setIcon("Refresh");
		setClient(panel);
	}

	public void setClient(SqlPanel panel)
	{
		client = panel;
		checkEnabled();
	}
	
	public void checkEnabled()
	{
		boolean enable = false;
		if (getSql() != null)
		{
			DwPanel dw =  client.getCurrentResult();
			if (dw != null)
			{
				DataStore ds = dw.getDataStore();
				enable = (ds != null ? ds.getOriginalConnection() != null : false);
			}
		}
		setEnabled(enable);
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
