/*
 * TableIndexPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.dbobjects;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import workbench.gui.actions.ReloadAction;
import workbench.gui.components.WbScrollPane;
import workbench.gui.components.WbTable;
import workbench.gui.components.WbToolbar;
import workbench.interfaces.Reloadable;

/**
 * @author Thomas Kellerer
 */
public class TableIndexPanel
	extends JPanel
{
	private ReloadAction reloadIndex;

	public TableIndexPanel(WbTable indexTable, Reloadable reloader)
	{
		super();
		this.setLayout(new BorderLayout());
		WbScrollPane p = new WbScrollPane(indexTable);
		this.add(p, BorderLayout.CENTER);
		if (reloader != null)
		{
			reloadIndex = new ReloadAction(reloader);
			reloadIndex.setEnabled(true);
			WbToolbar toolbar = new WbToolbar();
			toolbar.add(reloadIndex);
			this.add(toolbar, BorderLayout.NORTH);
		}
	}


}
