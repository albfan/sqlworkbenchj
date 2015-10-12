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
import java.util.Iterator;

import javax.swing.JFrame;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.text.BadLocationException;

import workbench.log.LogListener;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.gui.WbSwingUtilities;
import workbench.gui.sql.LogArea;

import workbench.util.EncodingUtil;
import workbench.util.ExceptionUtil;
import workbench.util.FileUtil;
import workbench.util.FixedSizeList;
import workbench.util.MemoryWatcher;
import workbench.util.WbFile;
import workbench.util.WbThread;

/**
 * @author Thomas Kellerer
 */
public class LogFileViewer
	extends JFrame
  implements LogListener
{
	protected LogArea display;
	protected JScrollPane scroll;
	private WbFile sourceFile;

	public LogFileViewer(Frame owner)
	{
		super();
		ResourceMgr.setWindowIcons(this, "logfile");
		display = new LogArea(owner);
    display.setMaxLineCount(getMaxLines());
		display.setFont(Settings.getInstance().getEditorFont());
		display.setEditable(false);
		display.setBackground(Color.WHITE);
		display.setWrapStyleWord(false);
    display.setLineWrap(false);
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
        LogMgr.removeLogListener(LogFileViewer.this);
				saveSettings();
				setVisible(false);
				dispose();
			}
		});
	}

  private int getMaxLines()
  {
    return Settings.getInstance().getIntProperty("workbench.logviewer.numlines", 10000);
  }

	public void setText(String text)
	{
		display.setText(text);
	}

	public void append(String msg)
	{
    display.addLine(msg);
	}

	public void load()
	{
		final String text = readLastLines(sourceFile, getMaxLines());

    EventQueue.invokeLater(new Runnable()
    {
      @Override
      public void run()
      {
        display.setText(text);
        scrollToEnd();
        LogMgr.addLogListener(LogFileViewer.this);
      }
    });
	}

	protected Runnable _scroller = new Runnable()
	{
		@Override
		public void run()
		{
      try
      {
        int start = display.getLineStartOffset(display.getLineCount() - 1);
        display.setCaretPosition(start);
      }
      catch (BadLocationException ble)
      {
        JScrollBar b = scroll.getVerticalScrollBar();
        int max = b.getMaximum();
        b.setValue(max);
      }
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
    WbThread loader = new WbThread("LogFileLoader")
    {
      @Override
      public void run()
      {
        load();
      }
    };
    loader.start();
	}

	protected void saveSettings()
	{
		Settings.getInstance().storeWindowPosition(this);
		Settings.getInstance().storeWindowSize(this);
	}

	private String readLastLines(File src, int maxLines)
	{
		final int maxBuff = (int)(MemoryWatcher.getFreeMemory() * 0.1); // never use more than 10 percent of the free memory for the buffer
		int logfileSize = Settings.getInstance().getMaxLogfileSize();
		int buffSize = Settings.getInstance().getIntProperty("workbench.logviewer.readbuff", (int)(logfileSize * 0.15));
		if (buffSize > maxBuff)
		{
			buffSize = maxBuff;
		}

    BufferedReader reader = null;
		try
		{
      reader = EncodingUtil.createBufferedReader(src, LogMgr.DEFAULT_ENCODING, buffSize);
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
    catch (Exception io)
    {
      return ExceptionUtil.getDisplay(io);
    }
		finally
		{
			FileUtil.closeQuietely(reader);
		}
	}

  @Override
  public void messageLogged(final CharSequence msg)
  {
    if (msg == null) return;

    EventQueue.invokeLater(new Runnable()
    {
      @Override
      public void run()
      {
        append(msg.toString());
      }
    });
  }

}
