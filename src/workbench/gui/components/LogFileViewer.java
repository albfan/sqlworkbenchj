/*
 * LogFileViewer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
		display = new LogArea(this);
    display.setMaxLineCount(getMaxLines() + 5);
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

	public void setText(String text)
	{
		display.setText(text);
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
        display.addLine(msg.toString());
        scrollToEnd();
      }
    });
  }

	public void load()
	{
    if (sourceFile == null) return;

    final FixedSizeList<String> lines = readLines();
    if (lines == null) return;

    EventQueue.invokeLater(new Runnable()
    {
      @Override
      public void run()
      {
        display.setText("");
        while (lines.size() > 0)
        {
          display.addLine(lines.removeFirst());
        }
        scrollToEnd();
        LogMgr.addLogListener(LogFileViewer.this);
      }
    });
	}

  private void scrollToEnd()
  {
    try
    {
      int start = display.getLineStartOffset(display.getLineCount() - 1);
      display.setCaretPosition(start);
    }
    catch (BadLocationException ble)
    {
      JScrollBar b = scroll.getVerticalScrollBar();
      b.setValue(b.getMaximum());
      scroll.getHorizontalScrollBar().setValue(0);
    }
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

	private void saveSettings()
	{
		Settings.getInstance().storeWindowPosition(this);
		Settings.getInstance().storeWindowSize(this);
	}

	private FixedSizeList<String> readLines()
	{
    if (sourceFile == null) return null;

		int maxBuff = (int)(MemoryWatcher.getFreeMemory() * 0.1); // never use more than 10 percent of the free memory for the buffer
		int logfileSize = Settings.getInstance().getMaxLogfileSize();
		int buffSize = Settings.getInstance().getIntProperty("workbench.logviewer.readbuff", (int)(logfileSize * 0.15));
		if (buffSize > maxBuff)
		{
			buffSize = maxBuff;
		}

    // As there is no reliable way to read a text file "backwards", the only safe option
    // to read the last "n" lines from a text file, is to read through the entire file
    // and keep the last "n" lines.
    FixedSizeList<String> lines = new FixedSizeList<>(getMaxLines());
    lines.doAppend(true);
    lines.setAllowDuplicates(true);

    BufferedReader reader = null;

		try
		{
      reader = EncodingUtil.createBufferedReader(sourceFile, LogMgr.DEFAULT_ENCODING, buffSize);
			for (String line = reader.readLine(); line != null; line = reader.readLine())
			{
				lines.add(line);
			}
      return lines;
		}
    catch (Exception ex)
    {
      LogMgr.logError("LogFileViewer.load()", "Could not load logfile", ex);
      display.setText(ExceptionUtil.getDisplay(ex));
    }
		finally
		{
			FileUtil.closeQuietely(reader);
		}
    return null;
	}

  public static int getMaxLines()
  {
    return Settings.getInstance().getIntProperty("workbench.logviewer.numlines", 1000);
  }

}
