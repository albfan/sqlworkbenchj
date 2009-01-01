/*
 * SqlTabPopup.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.menu;

import javax.swing.JPopupMenu;
import workbench.gui.MainWindow;
import workbench.gui.actions.AddTabAction;
import workbench.gui.actions.FileDiscardAction;
import workbench.gui.actions.NewDbExplorerPanelAction;
import workbench.gui.actions.RemoveTabAction;
import workbench.gui.actions.RenameTabAction;
import workbench.gui.sql.EditorPanel;
import workbench.gui.sql.SqlPanel;
import workbench.interfaces.MainPanel;
import workbench.gui.actions.InsertTabAction;
import workbench.gui.actions.MoveSqlTabLeft;
import workbench.gui.actions.MoveSqlTabRight;
import workbench.gui.actions.ToggleExtraConnection;

/**
 * @author  support@sql-workbench.net
 */
public class SqlTabPopup
	extends JPopupMenu
{
	public SqlTabPopup(MainWindow aClient)
	{
		super();
		AddTabAction add = new AddTabAction(aClient);
		this.add(add);
		InsertTabAction insert = new InsertTabAction(aClient);
		this.add(insert);

		RemoveTabAction remove = new RemoveTabAction(aClient);
		this.add(remove);

		if (aClient.canRenameTab())
		{
			RenameTabAction rename = new RenameTabAction(aClient);
			this.add(rename);
		}

		this.addSeparator();

		NewDbExplorerPanelAction newDbExp = new NewDbExplorerPanelAction(aClient, "MnuTxtAddExplorerPanel");
		newDbExp.removeIcon();
		add(newDbExp);

		if (aClient.canUseSeparateConnection())
		{
			ToggleExtraConnection toggle = new ToggleExtraConnection(aClient);
			this.add(toggle.getMenuItem());
		}

		MainPanel panel = aClient.getCurrentPanel();
		if (panel instanceof SqlPanel)
		{
			this.addSeparator();

			SqlPanel spanel = (SqlPanel)panel;
			int currentIndex = aClient.getCurrentPanelIndex();
			MoveSqlTabLeft moveLeft = new MoveSqlTabLeft(aClient);
			moveLeft.setEnabled(currentIndex > 0);
			this.add(moveLeft);
			int lastIndex = aClient.getLastSqlPanelIndex();
			MoveSqlTabRight moveRight = new MoveSqlTabRight(aClient);
			moveRight.setEnabled(currentIndex < lastIndex);
			this.add(moveRight);

			this.addSeparator();

			EditorPanel editor = spanel.getEditor();

			this.add(editor.getFileSaveAction());
			this.add(editor.getFileOpenAction());
			this.add(editor.getReloadAction());
			this.addSeparator();
			FileDiscardAction discard = new FileDiscardAction(spanel);
			discard.removeIcon();
			this.add(discard);
			remove.setEnabled(aClient.canCloseTab());
		}
	}

}
