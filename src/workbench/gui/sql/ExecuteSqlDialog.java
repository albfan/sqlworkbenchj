/*
 * ExecuteSqlDialog.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.sql;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionListener;
import java.awt.event.WindowListener;
import java.sql.Statement;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;

import workbench.db.WbConnection;
import workbench.util.ExceptionUtil;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.WbButton;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.util.WbThread;

/**
 *
 * @author  support@sql-workbench.net
 */
public class ExecuteSqlDialog
	extends JDialog
	implements ActionListener, WindowListener
{
	protected WbConnection dbConn;
	protected EditorPanel sqlEditor;
	protected WbButton startButton = new WbButton(ResourceMgr.getString("LblStartSql"));
	protected WbButton closeButton = new WbButton(ResourceMgr.getString("LblClose"));
	protected JLabel statusMessage;
	private Thread worker;

	/** Creates a new instance of ExecuteSqlDialog */
	public ExecuteSqlDialog(Frame owner, String aTitle, String sql, final String highlight, WbConnection aConnection)
	{
		super(owner, aTitle, true);
		this.dbConn = aConnection;
		this.getContentPane().setLayout(new BorderLayout());
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		this.startButton.addActionListener(this);
		this.closeButton.addActionListener(this);
		buttonPanel.add(startButton);
		buttonPanel.add(closeButton);
		JPanel mainPanel = new JPanel();
		sqlEditor = EditorPanel.createSqlEditor();
		sqlEditor.showFormatSql();
		mainPanel.setLayout(new BorderLayout());
		mainPanel.add(sqlEditor, BorderLayout.CENTER);
		sqlEditor.setDatabaseConnection(this.dbConn);
		sqlEditor.setText(sql);

		this.statusMessage = new JLabel("");
		this.statusMessage.setBorder(new EtchedBorder());
		this.statusMessage.setMaximumSize(new Dimension(32768, 22));
		this.statusMessage.setMinimumSize(new Dimension(10, 22));
		this.statusMessage.setPreferredSize(new Dimension(60, 22));
		mainPanel.add(this.statusMessage, BorderLayout.SOUTH);
		this.getContentPane().add(mainPanel, BorderLayout.CENTER);
		this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		if (!Settings.getInstance().restoreWindowSize(this, this.getClass().getName()))
		{
			this.setSize(500,400);
		}
		this.addWindowListener(this);

		WbSwingUtilities.center(this, owner);
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				highlightText(highlight);
			}
		});
	}

	public void setStartButtonText(String aText)
	{
		this.startButton.setText(aText);
	}

	private void highlightText(String aText)
	{
		if (aText == null || aText.trim().length() == 0) return;
		sqlEditor.reformatSql();
		sqlEditor.setCaretPosition(0);
		int start = sqlEditor.getReplacer().findFirst(aText, true, true, false);
		int end = start + aText.length();
		if (start > -1)
		{
			sqlEditor.select(start, end);
		}
		sqlEditor.requestFocusInWindow();
	}

	public void closeWindow()
	{
		if (this.worker != null)
		{
			this.worker.interrupt();
			this.worker = null;
		}
		Settings.getInstance().storeWindowSize(this, this.getClass().getName());
		this.setVisible(false);
		this.dispose();
	}

	private void startCreate()
	{
		if (this.dbConn == null) return;
		
		if (this.dbConn.isBusy()) 
		{
			WbSwingUtilities.showMessageKey(this, "ErrConnectionBusy");
			return;
		}
		this.statusMessage.setText(ResourceMgr.getString("MsgCreatingIndex"));

		this.worker = new WbThread("Create index thread")
		{
			public void run()
			{
				Statement stmt = null;
				try
				{
					String sql = sqlEditor.getText().trim().replaceAll(";", "");
					dbConn.setBusy(true);
					stmt = dbConn.createStatement();
					stmt.executeUpdate(sql);
					statusMessage.setText(ResourceMgr.getString("MsgIndexCreated"));
					if (dbConn.shouldCommitDDL())
					{
						dbConn.commit();
					}
					EventQueue.invokeLater(new Runnable()
					{
						public void run()
						{
							createSuccess();
						}
					});
				}
				catch (Exception e)
				{
					if (dbConn.shouldCommitDDL())
					{
						try { dbConn.rollback(); } catch (Throwable th) {}
					}
					createFailure(e);
				}
				finally
				{
					dbConn.setBusy(false);
					try { stmt.close(); } catch (Throwable th) {}
				}
			}
		};
		WbSwingUtilities.showWaitCursor(this);
		this.startButton.setEnabled(false);
		this.closeButton.setEnabled(false);
		this.sqlEditor.setEnabled(false);
		this.worker.start();
	}

	protected void createSuccess()
	{
		createFinished();
		WbSwingUtilities.showMessage(this, ResourceMgr.getString("MsgIndexCreated"));
		this.closeWindow();
	}
	
	protected void createFinished()
	{
		WbSwingUtilities.showDefaultCursor(this);
		this.startButton.setEnabled(true);
		this.closeButton.setEnabled(true);
		this.sqlEditor.setEnabled(true);
		this.worker = null;
	}

	protected void createFailure(Exception e)
	{
		createFinished();
		String error = ExceptionUtil.getDisplay(e);
		statusMessage.setText(error);
		String msg = ResourceMgr.getString("MsgCreateIndexError") + "\n" + error;
		WbSwingUtilities.showErrorMessage(this, msg);
	}

	public void actionPerformed(java.awt.event.ActionEvent e)
	{
		if (this.worker != null) return;
		if (e.getSource() == this.closeButton)
		{
			this.closeWindow();
		}
		else if (e.getSource() == this.startButton)
		{
			this.startCreate();
		}
	}

	public void windowActivated(java.awt.event.WindowEvent e)
	{
	}

	public void windowClosed(java.awt.event.WindowEvent e)
	{
	}

	public void windowClosing(java.awt.event.WindowEvent e)
	{
		if (this.worker == null) this.closeWindow();
	}

	public void windowDeactivated(java.awt.event.WindowEvent e)
	{
	}

	public void windowDeiconified(java.awt.event.WindowEvent e)
	{
	}

	public void windowIconified(java.awt.event.WindowEvent e)
	{
	}

	public void windowOpened(java.awt.event.WindowEvent e)
	{
	}

}
