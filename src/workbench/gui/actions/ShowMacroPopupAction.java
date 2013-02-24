/*
 * ShowMacroPopupAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

import workbench.resource.ResourceMgr;

import workbench.gui.MainWindow;
import workbench.gui.macros.MacroPopup;

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

	public void workspaceChanged()
	{
		if (this.macroWindow != null)
		{
			macroWindow.workspaceChanged();
		}
	}

	private void createPopup()
	{
		if (this.macroWindow == null)
		{
			macroWindow = new MacroPopup(client);
			EventQueue.invokeLater(new Runnable()
			{

				@Override
				public void run()
				{
					client.addWindowFocusListener(ShowMacroPopupAction.this);
					macroWindow.addWindowListener(ShowMacroPopupAction.this);
				}
			});
		}
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		showPopup();
	}

	@Override
	public void windowGainedFocus(WindowEvent e)
	{
		if (macroWindow != null && e.getWindow() == client && !macroWindow.isShowing())
		{
			macroWindow.setVisible(true);
			client.requestFocus();
			EventQueue.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					client.requestEditorFocus();
				}
			});
		}
	}

	@Override
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

	@Override
	public void windowOpened(WindowEvent e)
	{
	}

	@Override
	public void windowClosing(WindowEvent e)
	{
		if (e.getWindow() == macroWindow)
		{
			client.removeWindowFocusListener(this);
		}
	}

	@Override
	public void windowClosed(WindowEvent e)
	{
		if (e.getWindow() == macroWindow)
		{
			macroWindow.removeWindowListener(this);
			macroWindow = null;
		}
	}

	@Override
	public void windowIconified(WindowEvent e)
	{
	}

	@Override
	public void windowDeiconified(WindowEvent e)
	{
	}

	@Override
	public void windowActivated(WindowEvent e)
	{
	}

	@Override
	public void windowDeactivated(WindowEvent e)
	{
		if (e.getWindow() == macroWindow && e.getOppositeWindow() != client
				&& macroWindow.isShowing() && !macroWindow.isClosing())
		{
			macroWindow.setVisible(false);
		}
	}
}
