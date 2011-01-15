/*
 * FileCloseAction.java
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

import workbench.WbManager;
import workbench.gui.MainWindow;

/**
 * Exit and close the application
 * @see workbench.WbManager#exitWorkbench()
 *
 * @author  Thomas Kellerer
 */
public class FileCloseAction
	extends WbAction
{
	private MainWindow window;

	public FileCloseAction(MainWindow toClose)
	{
		super();
		window = toClose;
		this.initMenuDefinition("MnuTxtFileCloseWin");
	}

	public void executeAction(ActionEvent e)
	{
		WbManager.getInstance().closeMainWindow(window);
	}
}
