/*
 * ObjectScripterUI.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.dbobjects;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Window;
import java.awt.event.WindowListener;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.CreateSnippetAction;
import workbench.gui.sql.EditorPanel;
import workbench.interfaces.ScriptGenerationMonitor;
import workbench.interfaces.Scripter;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.util.StringUtil;
import workbench.util.WbThread;

/**
 *
 * @author  support@sql-workbench.net
 */
public class ObjectScripterUI
	extends JPanel
	implements WindowListener, ScriptGenerationMonitor
{
	protected Scripter scripter;
	protected JLabel statusMessage;
	protected EditorPanel editor;
	protected JFrame window;
	private boolean isRunning;
	private Object runMonitor = new Object();

	public ObjectScripterUI(Scripter scripter)
	{
		super();
		this.scripter = scripter;
		this.scripter.setProgressMonitor(this);

		this.statusMessage = new JLabel("");
		this.statusMessage.setBorder(new EtchedBorder());
		this.statusMessage.setMaximumSize(new Dimension(32768, 22));
		this.statusMessage.setMinimumSize(new Dimension(10, 22));
		this.statusMessage.setPreferredSize(new Dimension(60, 22));
		this.setLayout(new BorderLayout());
		this.add(this.statusMessage, BorderLayout.SOUTH);
		this.editor = EditorPanel.createSqlEditor();
		CreateSnippetAction create = new CreateSnippetAction(this.editor);
		this.editor.addPopupMenuItem(create, true);
		this.add(this.editor, BorderLayout.CENTER);
	}

	private void setRunning(boolean flag) 
	{ 
		synchronized (runMonitor) 
		{
			this.isRunning = flag; 
		}
	}
	
	private boolean isRunning()
	{
		synchronized (runMonitor)
		{
			return this.isRunning;
		}
	}
	
	private void startScripting()
	{
		if (this.isRunning()) return;
		WbThread t = new WbThread("ObjectScripter Thread")
		{
			public void run()
			{
				try
				{
					setRunning(true);
					scripter.generateScript();
					if (!scripter.isCancelled())
					{
						editor.setText(scripter.getScript().toString());
						editor.setCaretPosition(0);
					}
				}
				finally
				{
					setRunning(false);
					EventQueue.invokeLater(new Runnable()
					{
						public void run() { statusMessage.setText(StringUtil.EMPTY_STRING); }
					});
				}
			}
		};
		t.start();
	}

	public void setCurrentObject(String aTableName)
	{
		this.statusMessage.setText(aTableName);
		this.statusMessage.repaint();
	}

	public void show(Window aParent)
	{
		if (this.window == null)
		{
			this.window = new JFrame(ResourceMgr.getString("TxtWindowTitleGeneratedScript"));
			this.window.getContentPane().setLayout(new BorderLayout());
			this.window.getContentPane().add(this, BorderLayout.CENTER);
			this.window.setIconImage(ResourceMgr.getImage("script").getImage());
			if (!Settings.getInstance().restoreWindowSize(this.window, ObjectScripterUI.class.getName()))
			{
				this.window.setSize(500,400);
			}

			if (!Settings.getInstance().restoreWindowPosition(this.window, ObjectScripterUI.class.getName()))
			{
				WbSwingUtilities.center(this.window, aParent);
			}
			this.window.addWindowListener(this);
		}
		this.window.setVisible(true);
		this.startScripting();
	}

	public void windowActivated(java.awt.event.WindowEvent e)
	{
	}

	public void windowClosed(java.awt.event.WindowEvent e)
	{
	}

	private void cancel()
	{
		WbThread t = new WbThread("Scripter Cancel")
		{
			public void run()
			{
				try
				{
					WbSwingUtilities.showWaitCursor(window);
					statusMessage.setText(ResourceMgr.getString("MsgCancelling"));
					Thread.yield();
					scripter.cancel();
				}
				catch (Throwable ex)
				{
					ex.printStackTrace();
				}
				finally
				{
					WbSwingUtilities.showDefaultCursor(window);
				}
				scripter = null;
				setRunning(false);
				closeWindow();
			}
		};
		t.start();
	}
	
	protected void closeWindow()
	{
		if (isRunning()) return;
		Settings.getInstance().storeWindowPosition(this.window, ObjectScripterUI.class.getName());
		Settings.getInstance().storeWindowSize(this.window, ObjectScripterUI.class.getName());
		this.window.setVisible(false);
		this.window.dispose();
	}
	
	public void windowClosing(java.awt.event.WindowEvent e)
	{
		if (this.isRunning())
		{
			cancel();
			return;
		}
		closeWindow();
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
