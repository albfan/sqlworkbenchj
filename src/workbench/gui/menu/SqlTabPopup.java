/*
 * SqlTabPopup.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.gui.menu;

import javax.swing.JPopupMenu;
import workbench.gui.MainWindow;
import workbench.gui.actions.AddTabAction;
import workbench.gui.actions.FileDiscardAction;
import workbench.gui.actions.FileOpenAction;
import workbench.gui.actions.RemoveTabAction;
import workbench.gui.actions.RenameTabAction;
import workbench.gui.actions.WbAction;
import workbench.gui.sql.SqlPanel;
import workbench.interfaces.MainPanel;
import workbench.gui.actions.FileReloadAction;
import workbench.gui.actions.FileSaveAction;

/**
 * @author  info@sql-workbench.net
 */
public class SqlTabPopup extends JPopupMenu
{
	private MainWindow client;
	private AddTabAction add;
	private RemoveTabAction remove;
	private RenameTabAction rename;

	/** Creates new LogPanelPopup */
	public SqlTabPopup(MainWindow aClient)
	{
		MainPanel panel = aClient.getCurrentPanel();
		if (panel instanceof SqlPanel)
		{
			this.add = new AddTabAction(aClient);
			this.add(add.getMenuItem());
		}
		this.remove = new RemoveTabAction(aClient);
		this.add(remove.getMenuItem());

		if (aClient.canRenameTab())
		{
			this.rename = new RenameTabAction(aClient);
			this.add(rename.getMenuItem());
		}


		if (panel instanceof SqlPanel)
		{
			SqlPanel spanel = (SqlPanel)panel;
			this.addSeparator();

			FileSaveAction save = new FileSaveAction(spanel);
			save.removeIcon();
			this.add(save.getMenuItem());

			FileOpenAction open = new FileOpenAction(spanel);
			open.removeIcon();
			this.add(open.getMenuItem());

			FileReloadAction reload = new FileReloadAction(spanel);
			reload.removeIcon();
			this.add(reload.getMenuItem());
			this.addSeparator();
			FileDiscardAction discard = new FileDiscardAction(spanel);
			discard.removeIcon();
			this.add(discard.getMenuItem());
			this.remove.setEnabled(aClient.canCloseTab());

		}
	}

	public void addAction(WbAction anAction, boolean withSep)
	{
		if (withSep) this.addSeparator();
		this.add(anAction);
	}

	public RemoveTabAction getRemoveAction() { return this.remove; }
	public AddTabAction getAddAction() { return this.add; }
}
