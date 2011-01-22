/*
 * ShowSourceQueryAction.java
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

import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import javax.swing.SwingUtilities;
import workbench.WbManager;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.ValidatingDialog;
import workbench.gui.sql.EditorPanel;
import workbench.gui.sql.SqlPanel;
import workbench.resource.Settings;

/**
 *
 * @author Thomas Kellerer
 */
public class ShowSourceQueryAction
	extends WbAction
{
	private SqlPanel panel;
	
	public ShowSourceQueryAction(SqlPanel handler)
	{
		panel = handler;
		isConfigurable = false;
		initMenuDefinition("MnuTxtShowQuery");
	}

	public boolean isEnabled()
	{
		return (panel != null && panel.getSourceQuery() != null);
	}
	
	@Override
	public void executeAction(ActionEvent e)
	{
		showQuery();
	}

	public void showQuery()
	{
		final EditorPanel p = EditorPanel.createSqlEditor();
		String sql = panel.getSourceQuery();
		p.setText(sql);
		p.setCaretPosition(0);
		p.setEditable(false);
		Window w = SwingUtilities.getWindowAncestor(panel);
		Frame f = null;
		if (w instanceof Frame)
		{
			f = (Frame)w;
		}
		else
		{
			f = WbManager.getInstance().getCurrentWindow();
		}

		ValidatingDialog d = new ValidatingDialog(f, "SQL", p, false);
		if (!Settings.getInstance().restoreWindowSize(d, "workbench.resultquery.display"))
		{
			d.setSize(500,350);
		}
		WbSwingUtilities.center(d, f);
		WbSwingUtilities.repaintLater(p);
		d.setVisible(true);
		Settings.getInstance().storeWindowSize(d,"workbench.resultquery.display");
	}
}
