/*
 * LogFileViewer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFrame;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.gui.WbSwingUtilities;

import workbench.util.EncodingUtil;
import workbench.util.FileUtil;
import workbench.util.FixedSizeList;
import workbench.util.MemoryWatcher;
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
		ResourceMgr.setWindowIcons(this, "logfile");
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
			@Override
			public void windowClosing(WindowEvent evt)
			{
				if (watcher != null)
				{
					watcher.cancel();
				}
				saveSettings();
				setVisible(false);
				dispose();
			}

		});
	}

	public void setText(String text)
	{
		this.display.setText(text);
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
		watcher.schedule(task, (long)(refreshTime * 1.5), refreshTime);
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
			WbSwingUtilities.showWaitCursor(this);
			lastFileTime = sourceFile.lastModified();
			lastSize = sourceFile.length();
			int maxLines = Settings.getInstance().getIntProperty("workbench.logviewer.numlines", 5000);
			String lines = readLastLines(sourceFile, maxLines);
			display.setText(lines);
			scrollToEnd();
		}
		catch (Exception e)
		{
			display.setText(e.toString());
		}
		finally
		{
			FileUtil.closeQuietely(in);
			WbSwingUtilities.showDefaultCursor(this);
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
		@Override
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
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				load();
			}
		});
	}

	protected void saveSettings()
	{
		Settings.getInstance().storeWindowPosition(this);
		Settings.getInstance().storeWindowSize(this);
	}

	public String readLastLines(File src, int maxLines)
		throws IOException
	{
		final int maxBuff = (int)(MemoryWatcher.getFreeMemory() * 0.1); // never use more than 10 percent of the free memory for the buffer
		int logfileSize = Settings.getInstance().getMaxLogfileSize();
		int buffSize = Settings.getInstance().getIntProperty("workbench.logviewer.readbuff", (int)(logfileSize * 0.15));
		if (buffSize > maxBuff)
		{
			buffSize = maxBuff;
		}
		BufferedReader reader = EncodingUtil.createBufferedReader(src, LogMgr.DEFAULT_ENCODING, buffSize);
		try
		{
			FixedSizeList<String> lines = new FixedSizeList<>(maxLines);
			lines.doAppend(true);
			lines.setAllowDuplicates(true);
			for (String line = reader.readLine(); line != null; line = reader.readLine())
			{
				lines.add(line);
			}
			StringBuilder result = new StringBuilder(lines.size() * 100);
			Iterator<String> itr = lines.iterator();
			while (itr.hasNext())
			{
				String line = itr.next();
				result.append(line);
				result.append('\n');
			}
			return result.toString();
		}
		finally
		{
			FileUtil.closeQuietely(reader);
		}
	}

}
