/*
 * ConnectionSelector.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.EventQueue;
import java.awt.Frame;
import java.sql.SQLException;


import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.WbConnection;
import workbench.util.ExceptionUtil;
import workbench.gui.WbSwingUtilities;
import workbench.gui.profiles.ProfileSelectionDialog;
import workbench.interfaces.Connectable;
import workbench.interfaces.StatusBar;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.util.StringUtil;
import workbench.util.WbThread;

/**
 *
 * @author  Thomas Kellerer
 */
public class ConnectionSelector
	implements StatusBar
{
	protected Connectable client;
	private boolean connectInProgress;
	protected Frame parent;
	protected FeedbackWindow connectingInfo;
	private String propertyKey;

	public ConnectionSelector(Frame frame, Connectable conn)
	{
		this.client = conn;
		this.parent = frame;
	}

	public void setPropertyKey(String key)
	{
		this.propertyKey = key;
	}

	public boolean isConnectInProgress()
	{
		return this.connectInProgress;
	}


	public void selectConnection()
	{
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				_selectConnection();
			}
		});
	}

	protected void _selectConnection()
	{
		if (this.isConnectInProgress()) return;
		ProfileSelectionDialog dialog = null;
		try
		{
			WbSwingUtilities.showWaitCursor(this.parent);
			dialog = new ProfileSelectionDialog(this.parent, true, this.propertyKey);
			WbSwingUtilities.center(dialog, this.parent);
			WbSwingUtilities.showDefaultCursor(this.parent);
			dialog.setVisible(true);
			ConnectionProfile prof = dialog.getSelectedProfile();
			boolean cancelled = dialog.isCancelled();

			if (cancelled || prof == null)
			{
				this.client.connectCancelled();
			}
			else
			{
				this.connectTo(prof, false);
			}
		}
		catch (Throwable th)
		{
			LogMgr.logError("ConnectionSelector.selectConnection()", "Error during connect", th);
		}
		finally
		{
			if (dialog != null) dialog.dispose();
		}
	}

	public void connectTo(final ConnectionProfile aProfile, final boolean showDialogOnError)
	{
		if (this.isConnectInProgress()) return;

		Thread t = new WbThread("Connection thread")
		{
			@Override
			public void run()
			{
				doConnect(aProfile, showDialogOnError);
			}
		};
		t.start();
	}


	public void closeConnectingInfo()
	{
		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				if (connectingInfo != null)
				{
					connectingInfo.setVisible(false);
					connectingInfo.dispose();
					connectingInfo = null;
					WbSwingUtilities.repaintLater(parent);
				}
			}
		});
	}

	public void showDisconnectInfo()
	{
		showPopupMessagePanel(ResourceMgr.getString("MsgDisconnecting"));
	}

	public void showConnectingInfo()
	{
		showPopupMessagePanel(ResourceMgr.getString("MsgConnecting"));
	}

	protected void showPopupMessagePanel(final String msg)
	{
		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				if (connectingInfo != null)
				{
					connectingInfo.setMessage(msg);
					connectingInfo.pack();
					WbSwingUtilities.center(connectingInfo, parent);
				}
				else
				{
					connectingInfo = new FeedbackWindow(parent, msg);
					WbSwingUtilities.center(connectingInfo, parent);
					connectingInfo.setVisible(true);
				}
				connectingInfo.forceRepaint();
			}
		});
	}

	protected void doConnect(final ConnectionProfile aProfile, final boolean showSelectDialogOnError)
	{
		if (isConnectInProgress()) return;

		WbConnection conn = null;
		String error = null;

		if (!client.connectBegin(aProfile, this))
		{
			closeConnectingInfo();
			return;
		}
		setConnectIsInProgress();

		connectingInfo.repaint();

		showConnectingInfo();
		String id = this.client.getConnectionId(aProfile);
		try
		{
			ConnectionMgr mgr = ConnectionMgr.getInstance();

			WbSwingUtilities.showWaitCursor(this.parent);
			conn = mgr.getConnection(aProfile, id);
			if (this.propertyKey != null)
			{
				Settings.getInstance().setProperty(this.propertyKey, aProfile.getName());
			}
		}
		catch (UnsupportedClassVersionError ucv)
		{
			conn = null;
			error = ResourceMgr.getString("ErrDrvClassVersion");
		}
		catch (ClassNotFoundException cnf)
		{
			conn = null;
			error = ResourceMgr.getString("ErrDriverNotFound");
			error = StringUtil.replace(error, "%class%", aProfile.getDriverclass());
		}
		catch (SQLException se)
		{
			conn = null;
			StringBuilder logmsg = new StringBuilder(200);
			logmsg.append(ExceptionUtil.getDisplay(se));
			SQLException next = se.getNextException();
			while (next != null)
			{
				logmsg.append("\n");
				logmsg.append(ExceptionUtil.getDisplay(next));
				next = next.getNextException();
			}
			error = logmsg.toString();

			LogMgr.logError("ConnectionSelector.doConnect()", "SQL Exception when connecting", se);
		}
		catch (Throwable e)
		{
			conn = null;
			error = ExceptionUtil.getDisplay(e);
		}
		finally
		{
			WbSwingUtilities.showDefaultCursor(this.parent);
		}

		try
		{
			this.closeConnectingInfo();

			final WbConnection theConnection = conn;
			final String theError = error;

			WbSwingUtilities.invoke(new Runnable()
			{
				@Override
				public void run()
				{
					if (theConnection != null)
					{
						client.connected(theConnection);
					}
					else
					{
						client.connectFailed(theError);
					}
				}
			});

			if (conn == null && showSelectDialogOnError)
			{
				selectConnection();
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
		this.connectInProgress = true;
	}

	private void clearConnectIsInProgress()
	{
		this.connectInProgress = false;
	}

	@Override
	public void setStatusMessage(String message, int duration)
	{
		setStatusMessage(message);
	}

	@Override
	public void setStatusMessage(final String message)
	{
		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				showPopupMessagePanel(message);
			}
		});
	}

	@Override
	public void clearStatusMessage()
	{
		showPopupMessagePanel("");
	}

	@Override
	public void doRepaint()
	{
		if (this.connectingInfo != null) connectingInfo.forceRepaint();
	}

	@Override
	public String getText()
	{
		if (this.connectingInfo == null) return "";
		return connectingInfo.getMessage();
	}

}
