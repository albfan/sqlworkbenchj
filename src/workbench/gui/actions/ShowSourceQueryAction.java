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

import java.awt.Frame;
import java.awt.event.ActionEvent;
import workbench.WbManager;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.ValidatingDialog;
import workbench.gui.sql.EditorPanel;
import workbench.gui.sql.ResultHandler;
import workbench.resource.Settings;

/**
 *
 * @author support@sql-workbench.net
 */
public class ShowSourceQueryAction
	extends WbAction
{
	private ResultHandler result;
	
	public ShowSourceQueryAction(ResultHandler handler)
	{
		result = handler;
		isConfigurable = false;
		initMenuDefinition("MnuTxtShowQuery");
	}

	public boolean isEnabled()
	{
		return result.getSourceQuery() != null;
	}
	
	@Override
	public void executeAction(ActionEvent e)
	{
		showQuery();
	}

	public void showQuery()
	{
		EditorPanel p = EditorPanel.createSqlEditor();
		String sql = result.getSourceQuery();
		p.setText(sql);
		p.setEditable(false);
		Frame f = WbManager.getInstance().getCurrentWindow();

		ValidatingDialog d = new ValidatingDialog(f, "SQL", p, false);
		if (!Settings.getInstance().restoreWindowSize(d, "workbench.resultquery.display"))
		{
			d.setSize(500,350);
		}
		WbSwingUtilities.center(d, f);
		d.setVisible(true);
		Settings.getInstance().storeWindowSize(d,"workbench.resultquery.display");
	}
}
