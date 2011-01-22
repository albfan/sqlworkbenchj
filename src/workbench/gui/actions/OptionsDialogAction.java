/*
 * OptionsDialogAction.java
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

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import javax.swing.JFrame;
import workbench.WbManager;
import workbench.gui.settings.SettingsPanel;
import workbench.resource.ResourceMgr;

/**
 * @author Thomas Kellerer
 */
public class OptionsDialogAction
	extends WbAction
{
	public OptionsDialogAction()
	{
		super();
		initMenuDefinition(ResourceMgr.MNU_TXT_OPTIONS);
		this.removeIcon();
	}
	
	public void executeAction(ActionEvent e)
	{
		showOptionsDialog();
	}
	
	public static void showOptionsDialog()
	{
		final JFrame parent = WbManager.getInstance().getCurrentWindow();
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
