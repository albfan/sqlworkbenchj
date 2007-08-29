/*
 * HtmlViewer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.help;

import java.awt.Frame;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JDialog;

import workbench.gui.WbSwingUtilities;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;


public class HtmlViewer 
	extends JDialog
	implements WindowListener
{
	private HtmlPanel display;
	
	public HtmlViewer(Frame owner)
	{
		this(owner, "workbench-manual.html");
	}
	
	public HtmlViewer(Frame owner, String aStartFile)
	{
		super(owner, ResourceMgr.getString("TxtHelpWindowTitle"), false);
		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent evt)
			{
				saveSettings();
				setVisible(false);
				dispose();
			}
		});
		this.initHtml(aStartFile);
		this.restoreSettings(owner);
	}

	public HtmlViewer(JDialog owner)
	{
		super(owner, ResourceMgr.getString("TxtHelpWindowTitle"), false);
		this.initHtml(null);
		this.restoreSettings(owner);
	}
	
	private void restoreSettings(Window owner)
	{
		if (!Settings.getInstance().restoreWindowSize(this))
		{
			setSize(800,600);
		}
		
		if (!Settings.getInstance().restoreWindowPosition(this))
		{
			WbSwingUtilities.center(this, owner);
		}
	}
	
	private void initHtml(String aStartFile)
	{
		display = new HtmlPanel(aStartFile);
		this.getContentPane().add(display);
	}
	
	public void showDataPumperHelp()
	{
		this.display.showDataPumperHelp();
	}
	
	public void showOptionsHelp()
	{
		this.display.showOptionsHelp();
	}
	
	public void showProfileHelp()
	{
		this.display.showProfileHelp();
	}
	
	public void showIndex()
	{
		this.display.showIndex();
	}
	
	protected void saveSettings()
	{
		Settings.getInstance().storeWindowPosition(this);
		Settings.getInstance().storeWindowSize(this);
	}

	public void windowOpened(WindowEvent e)
	{
	}

	public void windowClosing(WindowEvent e)
	{
		setVisible(false);
		dispose();
	}

	public void windowClosed(WindowEvent e)
	{
	}

	public void windowIconified(WindowEvent e)
	{
	}

	public void windowDeiconified(WindowEvent e)
	{
	}

	public void windowActivated(WindowEvent e)
	{
	}

	public void windowDeactivated(WindowEvent e)
	{
	}

}
