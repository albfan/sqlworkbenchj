/*
 * DbExplorerWindow.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
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
import workbench.db.ConnectionProfile;
import workbench.db.WbConnection;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.ConnectionSelector;
import workbench.gui.components.RunningJobIndicator;
import workbench.interfaces.Connectable;
import workbench.interfaces.DbExecutionListener;
import workbench.interfaces.StatusBar;
import workbench.interfaces.ToolWindow;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

/**
 *
 * @author  support@sql-workbench.net
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
		this.setIconImage(ResourceMgr.getImage("Database").getImage());
		this.setProfileName(aProfileName);
		this.restorePosition();
		this.jobIndicator = new RunningJobIndicator(this);
		this.panel.setDbExecutionListener(this);
	}

	public JFrame getWindow()
	{
		return this;
	}
	
	public void activate()
	{
		setVisible(true);
		toFront();
	}
	
	public void setProfileName(String aProfileName)
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
	
	public void executionEnd(WbConnection conn, Object source)
	{
		jobIndicator.jobEnded();
	}

	public void executionStart(WbConnection conn, Object source)
	{
		jobIndicator.jobStarted();
	}
	
	public WbConnection getConnection()
	{
		return panel.getConnection();
	}
	
	public void closeWindow()
	{
		this.saveSettings();
		this.disconnect();
		this.panel.explorerWindowClosed();
		this.setVisible(false);
		this.dispose();
	}
	
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

	public void restorePosition()
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

	public void windowActivated(WindowEvent e)
	{
	}

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

	public void windowClosing(WindowEvent e)
	{
		if (panel.canClosePanel())
		{
			closeWindow();
		}
	}

	public void windowDeactivated(WindowEvent e)
	{
	}

	public void windowDeiconified(WindowEvent e)
	{
	}

	public void windowIconified(WindowEvent e)
	{
	}

	public void windowOpened(WindowEvent e)
	{
	}

	public static void showWindow()
	{
		EventQueue.invokeLater(new Runnable()
		{
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
	
	public void connectBegin(ConnectionProfile profile, StatusBar info)
	{
	}
	
	public void connectCancelled()
	{
	}
	
	public void connectFailed(String error)
	{
		this.setProfileName(null);
		this.panel.setConnection(null);
		String msg = ResourceMgr.getFormattedString("ErrConnectFailed", error.trim());
		WbSwingUtilities.showErrorMessage(this, msg);
	}
	
	public void connected(WbConnection conn)
	{
		this.setProfileName(conn.getProfile().getName());
		this.panel.setConnection(conn);
	}
	
	public String getConnectionId(ConnectionProfile profile)
	{
		if (this.panel == null) return "DbExplorerWindow";
		return this.panel.getId();
	}

	public void connectEnded()
	{
	}
}
