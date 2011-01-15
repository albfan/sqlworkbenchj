/*
 * CreateDeleteScriptAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.event.ListSelectionListener;
import workbench.db.DeleteScriptGenerator;
import workbench.db.WbConnection;
import workbench.gui.components.WbTable;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

/**
 * Create a SQL script to delete the selected row from a WbTable.
 * @see workbench.db.DeleteScriptGenerator
 * @author  Thomas Kellerer
 */
public class CreateDeleteScriptAction 
	extends WbAction
	implements ListSelectionListener
{
	private WbTable client;

	public CreateDeleteScriptAction(WbTable aClient)
	{
		super();
		this.initMenuDefinition("MnuTxtCreateDeleteScript", null);
		this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
		setClient(aClient);
	}

	public void executeAction(ActionEvent e)
	{
		WbConnection con = client.getDataStore().getOriginalConnection();
		if (con.isBusy()) return;
		
		try
		{
			boolean hasPK = client.checkPkColumns(true);
			if (!hasPK) return;
			DeleteScriptGenerator gen = new DeleteScriptGenerator(con);
			gen.setSource(client);
			gen.startGenerate();
		}
		catch (Exception ex)
		{
			LogMgr.logError("SqlPanel.generateDeleteScript()", "Error initializing DeleteScriptGenerator", ex);
		}
	}

	public void valueChanged(javax.swing.event.ListSelectionEvent e)
	{
		if (e.getValueIsAdjusting()) return;
		checkSelection();
	}
		
	private void checkSelection()
	{
		if (this.client == null) return;
		int rows = this.client.getSelectedRowCount();
		this.setEnabled(rows > 0);
	}
	
	public void setClient(WbTable w)
	{
		if (this.client != null)
		{
			this.client.getSelectionModel().removeListSelectionListener(this);
		}
		this.client = w;
		if (this.client != null)
		{
			this.client.getSelectionModel().addListSelectionListener(this);
			checkSelection();
		}
		this.setEnabled(this.client != null);
		checkSelection();
	}
}
