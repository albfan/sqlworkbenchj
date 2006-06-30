/*
 * ConnectionSelector.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.sql.SQLException;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.WbConnection;
import workbench.util.ExceptionUtil;
import workbench.gui.WbSwingUtilities;
import workbench.gui.profiles.ProfileSelectionDialog;
import workbench.interfaces.Connectable;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.util.StrBuffer;
import workbench.util.WbThread;

/**
 *
 * @author  support@sql-workbench.net
 */
public class ConnectionSelector
{
	private Connectable client;
	private boolean connectInProgress;
	private Frame parent;
	private JDialog connectingInfo;
	private JLabel connectLabel;
	private String propertyKey;

	/** Creates a new instance of ConnectionSelector */
	public ConnectionSelector(Frame parent, Connectable conn)
	{
		this.client = conn;
		this.parent = parent;
	}

	public void setPropertyKey(String key)
	{
		this.propertyKey = key;
	}

	public boolean isConnectInProgress()
	{
		synchronized (this)
		{
			return this.connectInProgress;
		}
	}

	public void selectConnection()
	{
		if (this.isConnectInProgress()) return;
		try
		{
			WbSwingUtilities.showWaitCursor(this.parent);
			final ProfileSelectionDialog dialog = new ProfileSelectionDialog(this.parent, true, this.propertyKey);
			WbSwingUtilities.center(dialog, this.parent);
			WbSwingUtilities.showDefaultCursor(this.parent);
			WbSwingUtilities.invoke(new Runnable()
			{
				public void run()
				{
					dialog.setVisible(true);
				}
			});
			ConnectionProfile prof = dialog.getSelectedProfile();
			boolean cancelled = dialog.isCancelled();
			
			WbSwingUtilities.invoke(new Runnable()
			{
				public void run()
				{
					dialog.setVisible(false);
					parent.repaint();
				}
			});
			dialog.dispose();
			
			if (cancelled || prof == null)
			{
				this.client.connectCancelled();
			}
			else
			{
				this.connectTo(prof);
			}
		}
		catch (Throwable th)
		{
			LogMgr.logError("ConnectionSelector.selectConnection()", "Error during connect", th);
		}
	}

	public void connectTo(final ConnectionProfile aProfile)
	{
		this.connectTo(aProfile, false);
	}

	public void connectTo(final ConnectionProfile aProfile, final boolean showDialogOnError)
	{
		if (this.isConnectInProgress()) return;
		this.showConnectingInfo();

		Thread t = new WbThread("Connection thread")
		{
			public void run()
			{
				doConnect(aProfile, showDialogOnError);
			}
		};
		t.start();
	}


	public void closeConnectingInfo()
	{
		WbSwingUtilities.showDefaultCursor(this.parent);
		if (this.connectingInfo != null)
		{
			WbSwingUtilities.showDefaultCursor(this.connectingInfo);
			try
			{
				this.connectingInfo.setVisible(false);
				this.connectingInfo.dispose();
			}
			catch (Throwable th)
			{
			}
			this.connectingInfo = null;
			this.parent.repaint();
		}
	}

	public void showDisconnectInfo()
	{
		WbSwingUtilities.invoke(new Runnable()
		{
			public void run()
			{
				showPopupMessagePanel(ResourceMgr.getString("MsgDisconnecting"));
			}
		});
	}

	public void showConnectingInfo()
	{
		WbSwingUtilities.invoke(new Runnable()
		{
			public void run()
			{
				showPopupMessagePanel(ResourceMgr.getString("MsgConnecting"));
			}
		});
	}

	private void showPopupMessagePanel(String aMsg)
	{
		if (this.connectingInfo != null)
		{
			this.connectLabel.setText(aMsg);
			this.connectingInfo.repaint();
			Thread.yield();
			return;
		}
		JPanel p = new JPanel();
		p.setBorder(WbSwingUtilities.BEVEL_BORDER_RAISED);
		p.setLayout(new BorderLayout());
		this.connectLabel = new JLabel(aMsg);
		this.connectLabel.setHorizontalAlignment(SwingConstants.CENTER);
		p.add(this.connectLabel, BorderLayout.CENTER);
		this.connectingInfo = new JDialog(this.parent, false);
		this.connectingInfo.getContentPane().setLayout(new BorderLayout());
		this.connectingInfo.getContentPane().add(p, BorderLayout.CENTER);
		this.connectingInfo.setUndecorated(true);
		this.connectingInfo.setSize(200,50);
		WbSwingUtilities.center(this.connectingInfo, this.parent);
		this.connectingInfo.setVisible(true);
		WbSwingUtilities.showWaitCursor(this.parent);
		WbSwingUtilities.showWaitCursor(this.connectingInfo);
		Thread.yield();
	}

	private void doConnect(final ConnectionProfile aProfile, final boolean showSelectDialogOnError)
	{
		boolean connected = false;
		WbConnection conn = null;
		String error = null;
		this.setConnectIsInProgress();
		
		this.client.connectBegin(aProfile);
		String id = this.client.getConnectionId(aProfile);
		try
		{
			ConnectionMgr mgr = ConnectionMgr.getInstance();

			WbSwingUtilities.showWaitCursor(this.parent);
			conn = mgr.getConnection(aProfile, id);
			connected = true;
			if (this.propertyKey != null)
			{
				Settings.getInstance().setProperty(this.propertyKey, aProfile.getName());
			}
		}
		catch (ClassNotFoundException cnf)
		{
			error = ResourceMgr.getString("ErrDriverNotFound");
		}
		catch (SQLException se)
		{
			error = se.getMessage();

			StrBuffer logmsg = new StrBuffer(200);
			logmsg.append(se.getMessage());
			SQLException next = se.getNextException();
			while (next != null)
			{
				logmsg.append("\n");
				logmsg.append(next.getMessage());
				next = next.getNextException();
			}

			LogMgr.logError("ConnectionSelector.doConnect()", "SQL Exception when connecting", se);
		}
		catch (Throwable e)
		{
			error = ExceptionUtil.getDisplay(e);
		}
		finally
		{
			WbSwingUtilities.showDefaultCursor(this.parent);
		}

		try
		{
			this.closeConnectingInfo();
			if (connected)
			{
				final WbConnection theConnection = conn;
				WbSwingUtilities.invoke(new Runnable()
				{
					public void run()
					{
						client.connected(theConnection);
					}
				});
			}
			else
			{
				final String theError = error;
				WbSwingUtilities.invoke(new Runnable()
				{
					public void run()
					{
						client.connectFailed(theError);
					}
				});
				if (showSelectDialogOnError)
				{
					selectConnection();
				}
			}
		}
		catch (Throwable th)
		{
			LogMgr.logError("ConnectionSelector.doConnect()", "Error ending connection process", th);
		}
		finally
		{
			client.connectEnded();
			this.clearConnectIsInProgress();
		}
	}

	private void setConnectIsInProgress()
	{
		synchronized (this)
		{
			this.connectInProgress = true;
		}
	}
	
	private void clearConnectIsInProgress()
	{
		synchronized (this)
		{
			this.connectInProgress = false;
		}
	}

}
