/*
 * DisplayDataFormAction.java
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
package workbench.gui.actions;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;

import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.ValidatingDialog;
import workbench.gui.components.WbTable;
import workbench.gui.sql.RecordFormPanel;
import workbench.resource.ResourceMgr;

/**
 * Delete the currently highlighted row(s) from a table
 * @see workbench.interfaces.DbData
 * @see workbench.gui.sql.DwPanel
 * @author  Thomas Kellerer
 */
public class DisplayDataFormAction
	extends WbAction
	implements TableModelListener
{
	private WbTable client;

	public DisplayDataFormAction(WbTable aClient)
	{
		super();
		this.setEnabled(false);
		this.initMenuDefinition("MnuTxtShowRecord");
		this.removeIcon();
		this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
		setTable(aClient);
	}

	@Override
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

		int maxWidth = (int)(screen.width * 0.6);
		int maxHeight = (int)(screen.height * 0.6);

		Dimension maxSize = new Dimension(maxWidth, maxHeight);

		panel.setMaximumSize(maxSize);
		dialog.setMaximumSize(maxSize);

		boolean doLimit = false;

		if (d.height > maxSize.height)
		{
			doLimit = true;

			// make the form wider, so that the vertical scrollbar does not
			// force a horizontal scrollbar to appear because the vertical space is now smaller
			UIDefaults def = UIManager.getDefaults();
			int scrollwidth = def.getInt("ScrollBar.width");
			if (scrollwidth <= 0) scrollwidth = 32; // this should leave enough room...
			d.width += scrollwidth + 2;
		}

		if (d.width > maxSize.width)
		{
			doLimit = true;
		}

		if (doLimit)
		{
			dialog.setPreferredSize(maxSize);
			dialog.pack();
		}

		dialog.pack();

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
		if (client != null && client != table)
		{
			client.removeTableModelListener(this);
		}
		this.client = table;
		setEnabled(client != null && client.getRowCount() > 0);
		if (client != null)
		{
			client.addTableModelListener(this);
		}
	}

	@Override
	public void tableChanged(TableModelEvent e)
	{
		setEnabled(client != null && client.getRowCount() > 0);
	}

}
