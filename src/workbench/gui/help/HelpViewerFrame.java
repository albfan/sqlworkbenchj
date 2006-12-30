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

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;

import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;

import workbench.gui.WbSwingUtilities;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;


public class HelpViewerFrame 
	extends JFrame
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

}
