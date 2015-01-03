/*
 * DbExplorerWindow.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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
package workbench.gui.dbobjects;

import java.awt.EventQueue;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import workbench.WbManager;
import workbench.interfaces.Connectable;
import workbench.interfaces.DbExecutionListener;
import workbench.interfaces.StatusBar;
import workbench.interfaces.ToolWindow;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.ConnectionProfile;
import workbench.db.WbConnection;

import workbench.gui.WbSwingUtilities;
import workbench.gui.components.ConnectionSelector;
import workbench.gui.components.RunningJobIndicator;

/**
 *
 * @author  Thomas Kellerer
 */
public class DbExplorerWindow
	extends JFrame
	implements WindowListener, Connectable, ToolWindow, DbExecutionListener
{
	private DbExplorerPanel panel;
	private boolean standalone;
	protected ConnectionSelector connectionSelector;
	protected RunningJobIndicator jobIndicator;

	public DbExplorerWindow(DbExplorerPanel aPanel)
	{
		this(aPanel, null);
	}

	public DbExplorerWindow(DbExplorerPanel aPanel, String aProfileName)
	{
		super();
		this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		this.panel = aPanel;
		this.addWindowListener(this);
		this.getContentPane().add(this.panel);
		ResourceMgr.setWindowIcons(this, "database");

		this.setProfileName(aProfileName);
		this.restorePosition();
		this.jobIndicator = new RunningJobIndicator(this);
		this.panel.setDbExecutionListener(this);
	}

	@Override
	public JFrame getWindow()
	{
		return this;
	}

	@Override
	public void activate()
	{
		setVisible(true);
		toFront();
	}

	@Override
	public String getDefaultIconName()
	{
		return "database";
	}

	public final void setProfileName(String aProfileName)
	{
		if (aProfileName != null)
		{
			this.setTitle(ResourceMgr.getString("TxtDbExplorerTitel") + " - [" + aProfileName + "]");
		}
		else
		{
			this.setTitle(ResourceMgr.getString("TxtDbExplorerTitel"));
		}
	}

	public void setStandalone(boolean flag)
	{
		this.standalone = flag;
		if (flag)
		{
			WbManager.getInstance().registerToolWindow(this);
			this.connectionSelector = new ConnectionSelector(this, this);
			this.connectionSelector.setPropertyKey("workbench.dbexplorer.connection.last");
			this.panel.showConnectButton(this.connectionSelector);
		}
	}

	public void selectConnection()
	{
		connectionSelector.selectConnection();
	}

	@Override
	public void executionEnd(WbConnection conn, Object source)
	{
		jobIndicator.jobEnded();
	}

	@Override
	public void executionStart(WbConnection conn, Object source)
	{
		jobIndicator.jobStarted();
	}

	@Override
	public WbConnection getConnection()
	{
		return panel.getConnection();
	}

	@Override
	public void closeWindow()
	{
		this.saveSettings();
		this.disconnect();
		this.panel.explorerWindowClosed();
		this.setVisible(false);
		this.dispose();
	}

	@Override
	public void disconnect()
	{
		this.panel.disconnect();
	}

	public void saveSettings()
	{
		Settings.getInstance().storeWindowPosition(this);
		Settings.getInstance().storeWindowSize(this);
		this.panel.saveSettings();
	}

	public final void restorePosition()
	{
		Settings s = Settings.getInstance();

		if (!s.restoreWindowSize(this))
		{
			this.setSize(950,750);
		}

		if (!s.restoreWindowPosition(this))
		{
			WbSwingUtilities.center(this, null);
		}
	}

	@Override
	public void windowActivated(WindowEvent e)
	{
	}

	@Override
	public void windowClosed(WindowEvent e)
	{
		if (standalone)
		{
			WbManager.getInstance().unregisterToolWindow(this);
		}
		else
		{
			if (this.panel != null)
			{
				panel.explorerWindowClosed();
			}
		}
	}

	@Override
	public void windowClosing(WindowEvent e)
	{
		if (panel.canClosePanel(true))
		{
			closeWindow();
		}
	}

	@Override
	public void windowDeactivated(WindowEvent e)
	{
	}

	@Override
	public void windowDeiconified(WindowEvent e)
	{
	}

	@Override
	public void windowIconified(WindowEvent e)
	{
	}

	@Override
	public void windowOpened(WindowEvent e)
	{
	}

	public static void showWindow()
	{
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				DbExplorerPanel dbPanel = new DbExplorerPanel();
				DbExplorerWindow window = new DbExplorerWindow(dbPanel);
				window.setStandalone(true);

				window.restorePosition();
				dbPanel.restoreSettings();

				window.setVisible(true);
				window.selectConnection();
			}
		});
	}

	@Override
	public boolean connectBegin(ConnectionProfile profile, StatusBar info, final boolean loadWorkspace)
	{
		return true;
	}

	@Override
	public void connectCancelled()
	{
	}

	@Override
	public void connectFailed(String error)
	{
		this.setProfileName(null);
		this.panel.setConnection(null);
		WbSwingUtilities.showFriendlyErrorMessage(this, ResourceMgr.getString("ErrConnectFailed"), error.trim());
	}

	@Override
	public void connected(WbConnection conn)
	{
		this.setProfileName(conn.getProfile().getName());
		this.panel.setConnection(conn);
	}

	@Override
	public String getConnectionId(ConnectionProfile profile)
	{
		if (this.panel == null) return "DbExplorerWindow";
		return this.panel.getId();
	}

	@Override
	public void connectEnded()
	{
	}
}
