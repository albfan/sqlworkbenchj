/*
 * ObjectCompilerUI.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.gui.dbobjects;

import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.WindowListener;
import java.sql.SQLException;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

import workbench.db.WbConnection;
import workbench.db.oracle.OracleObjectCompiler;
import workbench.gui.WbSwingUtilities;
import workbench.gui.sql.EditorPanel;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.util.WbThread;

/**
 *
 * @author  info@sql-workbench.net
 */
public class ObjectCompilerUI
	extends JPanel
	implements Runnable, WindowListener
{
	private Thread worker;
	private EditorPanel log;
	private JFrame window;
	private List types;
	private List names;
	private OracleObjectCompiler compiler;

	public ObjectCompilerUI(List names, List types, WbConnection conn)
		throws SQLException
	{
		super();
		this.names = names;
		this.types = types;
		this.compiler = new OracleObjectCompiler(conn);
		this.setLayout(new BorderLayout());
		this.log = EditorPanel.createTextEditor();
		this.add(this.log, BorderLayout.CENTER);
	}

	private void startCompile()
	{
		this.worker = new WbThread(this, "ObjectCompiler Thread");
		this.worker.start();
	}

	public void run()
	{
		String msg = ResourceMgr.getString("TxtCompilingObject");
		this.log.setText("");
		int count = this.names.size();


		for (int i=0; i < count; i++)
		{
			String name = (String)this.names.get(i);
			String type = (String)this.types.get(i);
			if (i > 0) this.log.appendLine("\n");
			this.log.appendLine(msg + " " + name + " ... ");
			if (this.compiler.compileObject(name, type))
			{
				this.log.appendLine(ResourceMgr.getString("TxtOK"));
			}
			else
			{
				this.log.appendLine(ResourceMgr.getString("TxtError"));
				this.log.appendLine(this.compiler.getLastError());
			}
		}
		this.log.setCaretPosition(0);
	}

	public void show(Window aParent)
	{
		if (this.window == null)
		{
			this.window = new JFrame(ResourceMgr.getString("TxtWindowTitleCompileStatus"));
			this.window.getContentPane().setLayout(new BorderLayout());
			this.window.getContentPane().add(this, BorderLayout.CENTER);
			this.window.setIconImage(ResourceMgr.getImage("workbench").getImage());
			if (!Settings.getInstance().restoreWindowSize(this.window, ObjectCompilerUI.class.getName()))
			{
				this.window.setSize(500,400);
			}

			if (!Settings.getInstance().restoreWindowPosition(this.window, ObjectCompilerUI.class.getName()))
			{
				WbSwingUtilities.center(this.window, aParent);
			}
			this.window.addWindowListener(this);
		}
		this.window.show();
		this.startCompile();
	}

	public void windowActivated(java.awt.event.WindowEvent e)
	{
	}

	public void windowClosed(java.awt.event.WindowEvent e)
	{
	}

	public void windowClosing(java.awt.event.WindowEvent e)
	{
		if (this.worker != null)
		{
			this.worker.interrupt();
			this.compiler.close();
			this.compiler = null;
			this.worker = null;
		}
		Settings.getInstance().storeWindowPosition(this.window, ObjectCompilerUI.class.getName());
		Settings.getInstance().storeWindowSize(this.window, ObjectCompilerUI.class.getName());
		this.window.hide();
		this.window.dispose();
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
