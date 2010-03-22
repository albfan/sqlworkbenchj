/*
 * LogFileViewer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
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
import java.io.File;

import java.io.Reader;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JFrame;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;

import workbench.gui.WbSwingUtilities;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.util.EncodingUtil;
import workbench.util.FileUtil;
import workbench.util.WbFile;

/**
 * @author Thomas Kellerer
 */
public class LogFileViewer
	extends JFrame
{
	protected SearchableTextPane display;
	protected JScrollPane scroll;
	private WbFile sourceFile;
	private long lastFileTime;
	private long lastSize;
	private Timer watcher;

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
				if (watcher != null) watcher.cancel();
				saveSettings();
				setVisible(false);
				dispose();
			}
		});
	}


	private void initWatcher()
	{
		watcher = new Timer(true);
		TimerTask task = new TimerTask()
		{
			@Override
			public void run()
			{
				long currentTime = sourceFile.lastModified();
				long currentSize = sourceFile.length();
				if (currentTime != lastFileTime || currentSize != lastSize)
				{
					load();
				}
			}
		};
		int refreshTime = Settings.getInstance().getIntProperty("workbench.logviewer.refresh", 1000);
		watcher.schedule(task, refreshTime, refreshTime);
	}

	public void append(String msg)
	{
		this.display.append(msg);
		scrollToEnd();
	}

	public synchronized void load()
	{
		Reader in = null;
		try
		{
			lastFileTime = sourceFile.lastModified();
			lastSize = sourceFile.length();
			in = EncodingUtil.createReader(sourceFile, Settings.getInstance().getDefaultEncoding());
			display.read(in, null);
			scrollToEnd();
		}
		catch (Exception e)
		{
			display.setText(e.toString());
		}
		finally
		{
			FileUtil.closeQuietely(in);
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
		initWatcher();
		// load() is not necessary because setVisible() will do that
	}

	protected void saveSettings()
	{
		Settings.getInstance().storeWindowPosition(this);
		Settings.getInstance().storeWindowSize(this);
	}

}
