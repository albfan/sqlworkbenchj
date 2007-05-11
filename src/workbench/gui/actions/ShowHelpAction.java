/*
 * ShowHelpAction.java
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
import java.awt.event.KeyEvent;
import java.lang.reflect.Constructor;
import javax.swing.JFrame;
import javax.swing.KeyStroke;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

/**
 * @author support@sql-workbench.net
 */
public class ShowHelpAction
	extends WbAction
{
	private static JFrame helpWindow;
	public ShowHelpAction()
	{
		super();
		initMenuDefinition("MnuTxtHelpContents",KeyStroke.getKeyStroke(KeyEvent.VK_F1,0));
		setIcon(ResourceMgr.getImage("help"));
	}
	
	public synchronized void executeAction(ActionEvent e)
	{
		if (helpWindow != null)
		{
			helpWindow.setVisible(true);
			helpWindow.requestFocus();
		}
		else
		{
			showHelp();
		}
	}
	
	public synchronized void closeHelp()
	{
		if (this.helpWindow != null)
		{
			this.helpWindow.setVisible(false);
			this.helpWindow.dispose();
			this.helpWindow = null;
		}
	}
	
	public synchronized void showHelp()
	{
		try
		{
			// Use reflection to load the dialog in order to
			// avoid unnecessary class loading during startup
			Class cls = Class.forName("workbench.gui.help.HelpViewerFrame");
			Constructor cons = cls.getConstructor((Class[])null);
			helpWindow = (JFrame)cons.newInstance((Object[])null);
			helpWindow.setVisible(true);
		}
		catch (Exception ex)
		{
			LogMgr.logError("WbManager.showDialog()", "Error when loading HelpViewerFrame", ex);
		}
	}

}
