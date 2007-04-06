/*
 * OptionsDialogAction.java
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

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import workbench.WbManager;
import workbench.gui.MainWindow;
import workbench.gui.settings.SettingsPanel;
import workbench.resource.ResourceMgr;

/**
 * @author support@sql-workbench.net
 */
public class OptionsDialogAction
	extends WbAction
{
	private static OptionsDialogAction instance = new OptionsDialogAction();
	public static OptionsDialogAction getInstance() { return instance; }
	
	private OptionsDialogAction()
	{
		super();
		initMenuDefinition(ResourceMgr.MNU_TXT_OPTIONS);
		this.removeIcon();
	}
	
	public void executeAction(ActionEvent e)
	{
		showOptionsDialog();
	}
	
	public void showOptionsDialog()
	{
		final MainWindow parent = WbManager.getInstance().getCurrentWindow();
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				SettingsPanel panel = new SettingsPanel();
				panel.showSettingsDialog(parent);
			}
		});
	}
}
