/*
 * ShowMacroPopupAction.java
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

import java.awt.EventQueue;
import java.awt.event.ActionEvent;


import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import workbench.gui.MainWindow;
import workbench.gui.macros.MacroPopup;
import workbench.resource.ResourceMgr;

/**
 *	@author  Thomas Kellerer
 */
public class ShowMacroPopupAction
	extends WbAction
	implements WindowFocusListener, WindowListener
{
	private MainWindow client;
	private MacroPopup macroWindow;

	public ShowMacroPopupAction(MainWindow aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtMacroPopup");
		this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
		this.setIcon(null);
		setEnabled(true);
	}

	public boolean isPopupVisible()
	{
		return (macroWindow != null && macroWindow.isVisible());
	}

	public void showPopup()
	{
		createPopup();
		macroWindow.setVisible(true);
	}
	
	private void createPopup()
	{
		if (this.macroWindow == null)
		{
			macroWindow = new MacroPopup(client);
			EventQueue.invokeLater(new Runnable()
			{

				public void run()
				{
					client.addWindowFocusListener(ShowMacroPopupAction.this);
					macroWindow.addWindowListener(ShowMacroPopupAction.this);
				}
			});
		}
	}

	public void executeAction(ActionEvent e)
	{
		showPopup();
	}

	public void windowGainedFocus(WindowEvent e)
	{
		if (macroWindow != null && e.getWindow() == client && !macroWindow.isShowing())
		{
			macroWindow.setVisible(true);
			EventQueue.invokeLater(new Runnable()
			{
				public void run()
				{
					client.requestFocus();
				}
			});
		}
	}

	public void windowLostFocus(WindowEvent e)
	{
		if (macroWindow != null
				&& e.getOppositeWindow() != macroWindow
				&& (e.getOppositeWindow() == null || e.getOppositeWindow() != null && e.getOppositeWindow().getOwner() != client)
				&& !macroWindow.isClosing())
		{
			macroWindow.setVisible(false);
		}
	}

	public void windowOpened(WindowEvent e)
	{
	}

	public void windowClosing(WindowEvent e)
	{
		if (e.getWindow() == macroWindow)
		{
			client.removeWindowFocusListener(this);
		}
	}

	public void windowClosed(WindowEvent e)
	{
		if (e.getWindow() == macroWindow)
		{
			macroWindow.removeWindowListener(this);
			macroWindow = null;
		}
	}

	public void windowIconified(WindowEvent e)
	{
	}

	public void windowDeiconified(WindowEvent e)
	{
	}

	public void windowActivated(WindowEvent e)
	{
	}

	public void windowDeactivated(WindowEvent e)
	{
		if (e.getWindow() == macroWindow && e.getOppositeWindow() != client 
				&& macroWindow.isShowing() && !macroWindow.isClosing())
		{
			macroWindow.setVisible(false);
		}
	}
}
