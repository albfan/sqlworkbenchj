/*
 * ConnectionSelector.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.sql.SQLException;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

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
 * @author  support@sql-workbench.net
 */
public class ConnectionSelector
	implements StatusBar
{
	protected Connectable client;
	private boolean connectInProgress;
	protected Frame parent;
	protected JDialog connectingInfo;
	protected JLabel connectLabel;
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
			public void run()
			{
				showPopupMessagePanel("");
				doConnect(aProfile, showDialogOnError);
			}
		};
		t.start();
	}


	public void closeConnectingInfo()
	{
		WbSwingUtilities.invoke(new Runnable()
		{
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
			public void run()
			{
				if (connectingInfo != null)
				{
					connectLabel.setText(msg);
					connectingInfo.pack();
					WbSwingUtilities.center(connectingInfo, parent);
				}
				else
				{
					JPanel p = new JPanel();
					p.setBorder(new CompoundBorder(WbSwingUtilities.getBevelBorderRaised(), new EmptyBorder(15, 20, 15, 20)));
					p.setLayout(new BorderLayout(0, 0));
					p.setMinimumSize(new Dimension(250, 50));
					connectLabel = new JLabel(msg);
					connectLabel.setMinimumSize(new Dimension(200, 50));
					connectLabel.setHorizontalAlignment(SwingConstants.CENTER);
					p.add(connectLabel, BorderLayout.CENTER);
					connectingInfo = new JDialog(parent, false);
					connectingInfo.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
					connectingInfo.getContentPane().setLayout(new BorderLayout());
					connectingInfo.getContentPane().add(p, BorderLayout.CENTER);
					connectingInfo.setUndecorated(true);
					connectingInfo.pack();
					WbSwingUtilities.center(connectingInfo, parent);
					connectingInfo.setVisible(true);
				}
				WbSwingUtilities.callRepaint(connectingInfo);
			}
		});
	}

	protected void doConnect(final ConnectionProfile aProfile, final boolean showSelectDialogOnError)
	{
		if (isConnectInProgress()) return;
		
		WbConnection conn = null;
		String error = null;
		
		setConnectIsInProgress();
		
		client.connectBegin(aProfile, this);
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

	public void setStatusMessage(final String message)
	{
		WbSwingUtilities.invoke(new Runnable()
		{
			public void run()
			{
				showPopupMessagePanel(message);
			}
		});
	}

	public void clearStatusMessage()
	{
		showPopupMessagePanel("");
	}

	public void repaint()
	{
		if (this.connectLabel != null) connectLabel.repaint();
	}

	public String getText()
	{
		if (this.connectLabel == null) return "";
		return connectLabel.getText();
	}

}
