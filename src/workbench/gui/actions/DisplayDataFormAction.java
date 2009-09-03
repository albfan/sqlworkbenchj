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

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;

import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.ValidatingDialog;
import workbench.gui.components.WbTable;
import workbench.gui.sql.RecordFormPanel;
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
		if(client.getRowCount() == 0) return;
		int row = client.getEditingRow();
		if (row < 0) row = client.getSelectedRow();
		if (row < 0) row = 0;

		int col = client.getEditingColumn();
		if (client.getShowStatusColumn()) col --;
		if (col < 0) col = 0;

		RecordFormPanel panel = new RecordFormPanel(client, row, col);

		Frame window = (Frame)SwingUtilities.getWindowAncestor(client);

		ValidatingDialog dialog = new ValidatingDialog(window, ResourceMgr.getString("TxtWindowTitleForm"), panel);
		Dimension d = dialog.getPreferredSize();
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

		boolean doLimit = false;
		if (d.height > screen.height)
		{
			d.height = screen.height - 150;
			doLimit = true;

			// make the form wider, so that the vertical scrollbar does not
			// force a horizontal scrollbar to appear because the vertical space is now smaller
			UIDefaults def = UIManager.getDefaults();
			int scrollwidth = def.getInt("ScrollBar.width");
			if (scrollwidth <= 0) scrollwidth = 32; // this should leave enough room...
			d.width += scrollwidth + 2;
		}

		if (d.width > screen.width)
		{
			d.width = screen.width - 100;
			doLimit = true;
		}

		if (doLimit)
		{
			dialog.setPreferredSize(d);
			dialog.setMaximumSize(d);
			dialog.pack();
		}

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
		setEnabled(client != null && client.getRowCount() > 0);
	}

}
