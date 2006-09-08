/*
 * HtmlViewer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
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


public class HtmlViewer 
	extends JDialog 
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

}
