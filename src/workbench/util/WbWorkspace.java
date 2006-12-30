/*
 * WbWorkspace.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import workbench.gui.sql.SqlHistory;
import workbench.log.LogMgr;

/**
 *
 * @author  support@sql-workbench.net
 */
public class WbWorkspace
{
	private ZipOutputStream zout;
	private ZipFile archive;
	private ZipEntry[] entries;

	private boolean isReadOnly;
	private WbProperties tabInfo = new WbProperties(1);

	public WbWorkspace(String archiveName, boolean createNew)
		throws IOException
	{
		if (createNew)
		{
			this.isReadOnly = false;
			File f = new File(archiveName);
			OutputStream out = new FileOutputStream(f);
			this.zout = new ZipOutputStream(out);
			this.zout.setLevel(9);
			this.zout.setComment("SQL Workbench/J Workspace file");
		}
		else
		{
			this.isReadOnly = true;
			this.zout = null;
			this.archive = new ZipFile(archiveName);
			Enumeration e = this.archive.entries();
			ArrayList tempEntries = new ArrayList(10);
			while (e.hasMoreElements())
			{
				ZipEntry entry = (ZipEntry)e.nextElement();
				String filename = entry.getName().toLowerCase();

				if (filename.endsWith("txt"))
				{
					tempEntries.add(entry);
				}
				else if (filename.endsWith("properties"))
				{
					this.readTabInfo(entry);
				}
			}

			int count = tempEntries.size();
			this.entries = new ZipEntry[count];

			for (int i=0; i < count; i++)
			{
				int ind = -1;
				int realIndex = -1;
				try
				{
					ZipEntry entry = (ZipEntry)tempEntries.get(i);
					String filename = entry.getName().toLowerCase();
					int pos = filename.indexOf('.');
					try { ind = Integer.parseInt(filename.substring(12,pos)); } catch (Throwable th) {ind = -1;}
					realIndex = ind - 1;
					if (realIndex >= 0 && realIndex < count)
					{
						entries[realIndex] = entry;
					}
					else
					{
						LogMgr.logError("WbWorkspace.<init>", "Wrong index " + realIndex + " retrieved from workspace file (" + count + " entries)", null);
					}
				}
				catch (Throwable ex)
				{
						LogMgr.logError("WbWorkspace.<init>", "Error reading history data for index " + realIndex + " (of " + count + " entries)", ex);
				}
			}
		}
	}

	/**
	 * Increase the counter for visible DbExplorer panels
	 */
	public void dDbExplorerVisible()
	{
		int count = this.tabInfo.getIntProperty("dbexplorer.visible", 0);
		count ++;
		this.tabInfo.setProperty("dbexplorer.visible", count);
	}
	
	public int getDbExplorerVisibleCount()
	{
		return this.tabInfo.getIntProperty("dbexplorer.visible", 0);
	}
	
	public void addHistoryEntry(String aFilename, SqlHistory history)
		throws IOException
	{
		if (this.isReadOnly) throw new IllegalStateException("Workspace is opened for reading. addHistoryEntry() may not be called");

		File f = new File(aFilename);
		String filename = f.getName();
		ZipEntry entry = new ZipEntry(filename);
		this.zout.putNextEntry(entry);
		history.writeToStream(zout);
		zout.closeEntry();
	}

	public int getEntryCount()
	{
		if (!this.isReadOnly) throw new IllegalStateException("Workspace is opened for writing. Entry count is not available");
		if (this.entries == null) return 0;
		return this.entries.length;
	}

	public void readHistoryData(int anIndex, SqlHistory history)
		throws IOException
	{
		if (!this.isReadOnly) throw new IllegalStateException("Workspace is opened for writing. Entry count is not available");
		if (anIndex > this.entries.length - 1) throw new IndexOutOfBoundsException("Index " + anIndex + " is great then " + (this.entries.length - 1));
		ZipEntry e = this.entries[anIndex];
		if (e != null)
		{
			InputStream in = this.archive.getInputStream(e);
			history.readFromStream(in);
		}
		else
		{
			LogMgr.logError("WbWorkspace.readHistoryData()", "Requested ZipEntry for index " + anIndex + " was null!", null);
		}
		return;
	}

	public WbProperties getSettings()
	{
		return this.tabInfo;
	}

	public void close()
		throws IOException
	{
		if (this.zout != null)
		{
			if (this.tabInfo != null && this.tabInfo.size() > 0)
			{
				try
				{
					ZipEntry entry = new ZipEntry("tabinfo.properties");
					this.zout.putNextEntry(entry);
					this.tabInfo.save(this.zout);
					zout.closeEntry();
				}
				catch (Throwable e)
				{
					LogMgr.logError("WbWorkspace.close()", "Could not write tab info!", e);
				}
				finally
				{
					this.zout.close();
				}
			}
		}
		else if (this.archive != null)
		{
			this.archive.close();
		}
	}

	private void readTabInfo(ZipEntry entry)
	{
		try
		{
			InputStream in = this.archive.getInputStream(entry);
			this.tabInfo = new WbProperties(1);
			this.tabInfo.load(in);
		}
		catch (Exception e)
		{
			LogMgr.logError("WbWorkspace", "Could not read tab info!", e);
			this.tabInfo = new WbProperties();
		}
	}

	public void setSelectedTab(int anIndex)
	{
		this.tabInfo.setProperty("tab.selected", Integer.toString(anIndex));
	}

	public int getSelectedTab()
	{
		return StringUtil.getIntValue(this.tabInfo.getProperty("tab.selected", "0"));
	}

	public void setTabTitle(int index, String name)
	{
		String key = "tab" + index + ".title";
		this.tabInfo.setProperty(key, name);
	}

	public String getTabTitle(int index)
	{
		if (this.tabInfo == null) return null;
		String key = "tab" + index + ".title";
		String value = (String)this.tabInfo.get(key);
		return value;
	}

	public int getExternalFileCursorPos(int tabIndex)
	{
		if (this.tabInfo == null) return -1;
		String key = "tab" + tabIndex + ".file.cursorpos";
		String value = (String)this.tabInfo.get(key);
		if (value == null) return -1;
		int result = -1;
		try
		{
			result = Integer.parseInt(value);
		}
		catch (Exception e)
		{
			result = -1;
		}

		return result;
	}

	public void setQueryTimeout(int index, int timeout)
	{
		String key = "tab" + index + ".timeout";
		this.tabInfo.setProperty(key, Integer.toString(timeout));
	}

	public int getQueryTimeout(int index)
	{
		if (this.tabInfo == null) return 0;
		String key = "tab" + index + ".timeout";
		String value = (String)this.tabInfo.get(key);
		if (value == null) return 0;
		int result = 0;
		try
		{
			result = Integer.parseInt(value);
		}
		catch (Exception e)
		{
			result = 0;
		}
		return result;
	}

	public void setMaxRows(int index, int numRows)
	{
		String key = "tab" + index + ".maxrows";
		this.tabInfo.setProperty(key, Integer.toString(numRows));
	}

	public int getMaxRows(int tabIndex)
	{
		if (this.tabInfo == null) return 0;
		String key = "tab" + tabIndex + ".maxrows";
		String value = (String)this.tabInfo.get(key);
		if (value == null) return 0;
		int result = 0;
		try
		{
			result = Integer.parseInt(value);
		}
		catch (Exception e)
		{
			result = 0;
		}
		return result;
	}

	public String getExternalFileName(int tabIndex)
	{
		if (this.tabInfo == null) return null;
		String key = "tab" + tabIndex + ".filename";
		String value = (String)this.tabInfo.get(key);
		return value;
	}
	public String getExternalFileEncoding(int tabIndex)
	{
		if (this.tabInfo == null) return null;
		String key = "tab" + tabIndex + ".encoding";
		String value = (String)this.tabInfo.get(key);
		if (StringUtil.isEmptyString(value)) return EncodingUtil.getDefaultEncoding();
		return value;
	}

	public void setExternalFileCursorPos(int tabIndex, int cursor)
	{
		String key = "tab" + tabIndex + ".file.cursorpos";
		this.tabInfo.setProperty(key, Integer.toString(cursor));
	}

	public void setExternalFileName(int tabIndex, String filename)
	{
		String key = "tab" + tabIndex + ".filename";
		this.tabInfo.setProperty(key, filename);
	}

	public void setExternalFileEncoding(int tabIndex, String encoding)
	{
		if (encoding == null) return;
		String key = "tab" + tabIndex + ".encoding";
		this.tabInfo.setProperty(key, encoding);
	}
}
