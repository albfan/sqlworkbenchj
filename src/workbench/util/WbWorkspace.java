/*
 * WbWorkspace.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import workbench.gui.sql.PanelType;
import workbench.gui.sql.SqlHistory;
import workbench.log.LogMgr;
import workbench.resource.Settings;

/**
 *
 * @author  Thomas Kellerer
 */
public class WbWorkspace
	implements Closeable
{
	private ZipOutputStream zout;
	private ZipFile archive;

	private boolean isReadOnly;
	private WbProperties tabInfo = new WbProperties(null, 0);
	private int tabCount = -1;

	public WbWorkspace(String archiveName, boolean createNew)
		throws IOException
	{
		if (createNew)
		{
			this.isReadOnly = false;
			File f = new File(archiveName);
			OutputStream out = new BufferedOutputStream(new FileOutputStream(f), 128*1024);
			zout = new ZipOutputStream(out);
			zout.setLevel(Settings.getInstance().getIntProperty("workbench.workspace.compression", 3));
			zout.setComment("SQL Workbench/J Workspace file");
		}
		else
		{
			this.isReadOnly = true;
			this.zout = null;
			this.archive = new ZipFile(archiveName);

			ZipEntry entry = archive.getEntry("tabs.properties");
			long size = (entry != null ? entry.getSize() : 0);
			if (size <= 0)
			{
				// Old definition of tabs for builds before 103.2
				entry = archive.getEntry("tabinfo.properties");
			}
			this.readTabInfo(entry);

			tabCount = calculateTabCount();
		}
	}

	/**
	 * For testing purposes only
	 * @param props
	 */
	WbWorkspace(WbProperties props)
	{
		this.tabInfo = props;
		tabCount = calculateTabCount();
		this.isReadOnly = true;
	}

	private int calculateTabCount()
	{
		// new property that stores the total count of tabs
		int count = tabInfo.getIntProperty("tab.total.count", -1);
		if (count > 0) return count;

		// Old tabinfo.properties format
		boolean found = true;
		int index = 0;
		while (found)
		{
			if (tabInfo.containsKey("tab" + index + ".maxrows") ||
					tabInfo.containsKey("tab" + index + ".title") ||
					tabInfo.containsKey("tab" + index + ".append.results"))
			{
				tabInfo.setProperty("tab" + index + ".type", PanelType.sqlPanel.toString());
				index ++;
			}
			else if (tabInfo.containsKey("tab" + index + ".type"))
			{
				index ++;
			}
			else
			{
				found = false;
			}
		}

		int dbExplorer = this.tabInfo.getIntProperty("dbexplorer.visible", 0);

		// now add the missing .type entries for the DbExplorer panels
		for (int i=0; i < dbExplorer; i++)
		{
			tabInfo.setProperty("tab" + index + ".type", PanelType.dbExplorer.toString());
			index ++;
		}
		return index;
	}

	public void setEntryCount(int count)
	{
		tabInfo.setProperty("tab.total.count", count);
	}

	public void addHistoryEntry(int index, SqlHistory history)
		throws IOException
	{
		if (this.isReadOnly) throw new IllegalStateException("Workspace is opened for reading. addHistoryEntry() may not be called");

		ZipEntry entry = new ZipEntry("WbStatements" + index + ".txt");
		this.zout.putNextEntry(entry);
		if (history != null)
		{
			history.writeToStream(zout);
		}
		zout.closeEntry();
	}

	public int getEntryCount()
	{
		if (!this.isReadOnly) throw new IllegalStateException("Workspace is opened for writing. Entry count is not available");
		return tabCount;
	}

	public PanelType getPanelType(int index)
	{
		String type = tabInfo.getProperty("tab" + index + ".type", "sqlPanel");
		try
		{
			return PanelType.valueOf(type);
		}
		catch (Exception e)
		{
			return PanelType.sqlPanel;
		}
	}

	public void readHistoryData(int anIndex, SqlHistory history)
		throws IOException
	{
		if (!this.isReadOnly) throw new IllegalStateException("Workspace is opened for writing. Entry count is not available");
		//if (anIndex > this.entries.length - 1) throw new IndexOutOfBoundsException("Index " + anIndex + " is great then " + (this.entries.length - 1));

		ZipEntry e = this.archive.getEntry("WbStatements" + (anIndex + 1) + ".txt");
		if (e != null)
		{
			InputStream in = this.archive.getInputStream(e);
			history.readFromStream(in);
		}
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
					ZipEntry entry = new ZipEntry("tabs.properties");
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
			this.tabInfo = new WbProperties(this);
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

	public boolean isSelectedTabExplorer()
	{
		int index = getSelectedTab();
		return PanelType.dbExplorer == this.getPanelType(index);
	}

	public void setTabTitle(int index, String name)
	{
		String key = "tab" + index + ".title";
		String encoded = StringUtil.escapeUnicode(name, CharacterRange.RANGE_7BIT);
		this.tabInfo.setProperty(key, encoded);
	}

	public String getTabTitle(int index)
	{
		if (this.tabInfo == null) return null;
		String key = "tab" + index + ".title";
		String value = (String)this.tabInfo.get(key);
		return StringUtil.decodeUnicode(value);
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
		return StringUtil.decodeUnicode(value);
	}

	public String getExternalFileEncoding(int tabIndex)
	{
		if (this.tabInfo == null) return null;
		String key = "tab" + tabIndex + ".encoding";
		String value = (String)this.tabInfo.get(key);
		if (StringUtil.isEmptyString(value)) return Settings.getInstance().getDefaultEncoding();
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
		String encoded = StringUtil.escapeUnicode(filename, CharacterRange.RANGE_7BIT);
		this.tabInfo.setProperty(key, encoded);
	}

	public void setExternalFileEncoding(int tabIndex, String encoding)
	{
		if (encoding == null) return;
		String key = "tab" + tabIndex + ".encoding";
		this.tabInfo.setProperty(key, encoding);
	}

}
