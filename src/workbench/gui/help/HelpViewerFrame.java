/*
 * HelpViewerFrame.java
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

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import workbench.gui.WbSwingUtilities;
import workbench.interfaces.ToolWindow;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;


public class HelpViewerFrame 
	extends JFrame
	implements ToolWindow
{
	private HtmlPanel display;
	
	public HelpViewerFrame()
	{
		super(ResourceMgr.getString("TxtHelpWindowTitle"));
		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent evt)
			{
				saveSettings();
				setVisible(false);
				dispose();
			}
		});
		this.display = new HtmlPanel("workbench-manual.html");
		this.getContentPane().add(this.display);
		this.setIconImage(ResourceMgr.getImage("help").getImage());
		this.restoreSettings();
	}

	private void restoreSettings()
	{
		if (!Settings.getInstance().restoreWindowSize(this))
		{
			setSize(800,600);
		}
		
		if (!Settings.getInstance().restoreWindowPosition(this))
		{
			WbSwingUtilities.center(this, null);
		}
	}
	
	protected void saveSettings()
	{
		Settings.getInstance().storeWindowPosition(this);
		Settings.getInstance().storeWindowSize(this);
	}

  public void closeWindow()
  {
		saveSettings();
    this.setVisible(false);
		this.dispose();
  }

  public void disconnect()
  {
		// not needed
  }

}
