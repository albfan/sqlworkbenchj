/*
 * LogFileViewer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.Reader;
import javax.swing.JFrame;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;

import workbench.gui.WbSwingUtilities;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.util.EncodingUtil;
import workbench.util.FileUtil;
import workbench.util.WbFile;

/**
 * @author support@sql-workbench.net  
 */
public class LogFileViewer
	extends JFrame
{
	protected SearchableTextPane display;
	protected JScrollPane scroll;
	private WbFile sourceFile;
	
	public LogFileViewer(Frame owner)
	{
		super();
		setIconImage(ResourceMgr.getImage("script").getImage());
		display = new SearchableTextPane(this);
		display.setFont(Settings.getInstance().getEditorFont());
		display.setEditable(false);
		display.setBackground(Color.WHITE);
		display.setFont(Settings.getInstance().getEditorFont());
		display.setWrapStyleWord(false);
		scroll = new JScrollPane(display);

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(scroll, BorderLayout.CENTER);

		if (!Settings.getInstance().restoreWindowSize(this))
		{
			setSize(800, 600);
		}

		if (!Settings.getInstance().restoreWindowPosition(this))
		{
			WbSwingUtilities.center(this, owner);
		}

		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent evt)
			{
				LogMgr.removeViewer();
				saveSettings();
				setVisible(false);
				dispose();
			}
		});
	}

	public void append(String msg)
	{
		this.display.append(msg);
		scrollToEnd();
	}
	
	public void load()
	{
		Reader in = null;
		try
		{
			in = EncodingUtil.createReader(sourceFile, System.getProperty("file.encoding"));
			display.read(in, null);
			scrollToEnd();
			LogMgr.registerViewer(this);
		}
		catch (Exception e)
		{
			display.setText(e.toString());
		}
		finally
		{
			FileUtil.closeQuitely(in);
		}
	}

	@Override
	public void setVisible(boolean b)
	{
		super.setVisible(b);
		if (b && sourceFile != null)
		{
			load();
		}
	}

	protected Runnable _scroller = new Runnable()
		{
			public void run()
			{
				JScrollBar b = scroll.getVerticalScrollBar();
				int max = b.getMaximum();
				b.setValue(max);
			}
		};

	protected void scrollToEnd()
	{
		EventQueue.invokeLater(_scroller);
	}

	public void showFile(WbFile f)
	{
		sourceFile = new WbFile(f);
		setTitle(sourceFile.getFullPath());
		// load() is not necessary because setVisible() will do that
	}

	protected void saveSettings()
	{
		Settings.getInstance().storeWindowPosition(this);
		Settings.getInstance().storeWindowSize(this);
	}
}
