/*
 * DeleteRowAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.Frame;
import java.awt.event.ActionEvent;

import javax.swing.SwingUtilities;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.ValidatingDialog;
import workbench.gui.components.WbTable;
import workbench.gui.sql.RecordFormPanel;
import workbench.interfaces.DbData;
import workbench.resource.ResourceMgr;

/**
 * Delete the currently highlighted row(s) from a table
 * @see workbench.interfaces.DbData
 * @see workbench.gui.sql.DwPanel
 * @author  support@sql-workbench.net
 */
public class DisplayDataFormAction
	extends WbAction
{
	private WbTable client;

	public DisplayDataFormAction(WbTable aClient)
	{
		super();
		this.client = aClient;
		this.setEnabled(false);
		this.initMenuDefinition("MnuTxtShowRecord");
		this.removeIcon();
		this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
		setTable(aClient);
	}

	public void executeAction(ActionEvent e)
	{

		int row = client.getEditingRow();
		if (row < 0) row = client.getSelectedRow();
		if (row < 0) row = 0;

		int col = client.getEditingColumn();
		if (client.getShowStatusColumn()) col --;
		if (col < 0) col = 0;

		RecordFormPanel panel = new RecordFormPanel(client, row, col);

		Frame window = (Frame)SwingUtilities.getWindowAncestor(client);

		ValidatingDialog dialog = new ValidatingDialog(window, ResourceMgr.getString("TxtWindowTitleForm"), panel);
		try
		{
			WbSwingUtilities.center(dialog, window);
			dialog.setVisible(true);
		}
		finally
		{
			dialog.dispose();
		}
	}

	public void setTable(WbTable table)
	{
		this.client = table;
		setEnabled(client != null);
	}

}
