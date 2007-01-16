/*
 * ConfigureShortcutsAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import workbench.WbManager;
import workbench.gui.MainWindow;
import workbench.gui.settings.ShortcutEditor;

/**
 * Action to open the shortcut manager window.
 * @see workbench.gui.settings.ShortcutEditor
 * @author support@sql-workbench.net
 */
public class ConfigureShortcutsAction
	extends WbAction
{
	private static ConfigureShortcutsAction instance = new ConfigureShortcutsAction();
	public static ConfigureShortcutsAction getInstance() { return instance; }
	
	private ConfigureShortcutsAction()
	{
		super();
		initMenuDefinition("MnuTxtConfigureShortcuts");
		removeIcon();
	}

	public void executeAction(ActionEvent e)
	{
		final MainWindow main = WbManager.getInstance().getCurrentWindow();
		ShortcutEditor editor = new ShortcutEditor(main);
		editor.showWindow();
		//WbManager.getInstance().showDialog("workbench.gui.settings.ShortcutEditor");
	}
}
