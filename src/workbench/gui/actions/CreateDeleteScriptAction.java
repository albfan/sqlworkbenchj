/*
 * CreateDeleteScriptAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;

import javax.swing.event.ListSelectionListener;
import workbench.db.DeleteScriptGenerator;
import workbench.gui.components.WbTable;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

/**
 *	@author  support@sql-workbench.net
 */
public class CreateDeleteScriptAction 
	extends WbAction
	implements ListSelectionListener
{
	private WbTable client;

	public CreateDeleteScriptAction(WbTable aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtCreateDeleteScript", null);
		this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
		
		this.client.getSelectionModel().addListSelectionListener(this);
	}

	public void executeAction(ActionEvent e)
	{
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				try
				{
					DeleteScriptGenerator gen = new DeleteScriptGenerator(client.getDataStore().getOriginalConnection());
					gen.setSource(client);
					gen.startGenerate();
				}
				catch (Exception e)
				{
					LogMgr.logError("SqlPanel.generateDeleteScript()", "Error initializing DeleteScriptGenerator", e);
				}
			}
		});
		
	}

	public void valueChanged(javax.swing.event.ListSelectionEvent e)
	{
		if (e.getValueIsAdjusting()) return;
		
		boolean mayUpdate = this.client.isUpdateable();
		int rows = this.client.getSelectedRowCount();
		this.setEnabled(mayUpdate && rows > 0);
	}
		

}
