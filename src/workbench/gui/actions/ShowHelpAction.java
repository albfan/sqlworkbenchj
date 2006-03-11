/*
 * ShowHelpAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
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
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import workbench.WbManager;
import workbench.log.LogMgr;

/**
 * @author support@sql-workbench.net
 */
public class ShowHelpAction
	extends WbAction
{
	private static final ShowHelpAction instance = new ShowHelpAction();

	private ShowHelpAction()
	{
		super();
		initMenuDefinition("MnuTxtHelpContents",KeyStroke.getKeyStroke(KeyEvent.VK_F1,0));
		removeIcon();
	}
	
	public static ShowHelpAction getInstance() { return instance; } 
	
	public void executeAction(ActionEvent e)
	{
		showHelp();
	}
	public void showHelp()
	{
		WbManager.getInstance().showDialog("workbench.gui.help.HtmlViewer");
	}
}
