/*
 * FileConnectAction.java
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
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import workbench.gui.MainWindow;

/**
 * Display the connect dialog
 * @author Thomas Kellerer
 */
public class FileConnectAction
	extends WbAction
{
	private MainWindow window;

	public FileConnectAction(MainWindow aWindow)
	{
		super();
		this.window = aWindow;
		this.initMenuDefinition("MnuTxtConnect", KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.ALT_MASK));
	}

	public void executeAction(ActionEvent e)
	{
		this.window.selectConnection();
	}
}
