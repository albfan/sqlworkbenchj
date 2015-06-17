/*
 * ConnectionSelector.java
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
package workbench.gui.components;

import java.awt.Frame;
import java.io.File;
import java.sql.SQLException;
import java.util.List;

import workbench.interfaces.Connectable;
import workbench.interfaces.StatusBar;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.NoConnectionException;
import workbench.db.WbConnection;

import workbench.gui.WbSwingUtilities;
import workbench.gui.profiles.ProfileSelectionDialog;

import workbench.util.ExceptionUtil;
import workbench.util.ImageUtil;
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
		WbSwingUtilities.invoke(new Runnable()
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
				this.connectTo(prof, false, true);
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

	public void connectTo(final ConnectionProfile aProfile, final boolean showDialogOnError, final boolean loadWorkspace)
	{
		if (this.isConnectInProgress())
    {
      LogMgr.logWarning("ConnectionSelector.connectTo()", "connectTo() called while a connect is still in progress");
      return;
    }

		Thread t = new WbThread("Connection thread")
		{
			@Override
			public void run()
			{
				doConnect(aProfile, showDialogOnError, loadWorkspace);
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


	protected void doConnect(final ConnectionProfile aProfile, final boolean showSelectDialogOnError, final boolean loadWorkspace)
	{
		if (this.isConnectInProgress())
    {
      LogMgr.logWarning("ConnectionSelector.doConnect()", "doConnect() called while a connect is still in progress");
      return;
    }

		List<File> iconFiles = ImageUtil.getIcons(aProfile.getIcon());
		if (iconFiles.size() > 0)
		{
			try
			{
				ResourceMgr.setWindowIcons(parent, iconFiles);
			}
			catch (Throwable th)
			{
				LogMgr.logError("ConnectionSelector.doConnect()", "Could not set window icon", th);
				ResourceMgr.setWindowIcons(this.parent, "workbench");
			}
		}
		else
		{
			// make sure to clear any previously assigned customized icon
			ResourceMgr.setWindowIcons(this.parent, client.getDefaultIconName());
		}

		WbConnection conn = null;
		String error = null;

		if (!client.connectBegin(aProfile, this, loadWorkspace))
		{
			closeConnectingInfo();
			return;
		}
		setConnectIsInProgress();

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
		catch (NoConnectionException noConn)
		{
			conn = null;
			LogMgr.logError("ConnectionSelector.doConnect()", "No connection returned for profile "+ aProfile.getKey(), noConn);
			error = ResourceMgr.getString("ErrNoConnReturned");
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
