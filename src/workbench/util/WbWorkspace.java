/*
 * WbWorkspace.java
 *
 * Created on March 29, 2003, 11:53 AM
 */

package workbench.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import workbench.gui.sql.SqlHistory;
import workbench.log.LogMgr;
import workbench.util.StringUtil;

/**
 *
 * @author  thomas
 */
public class WbWorkspace
{
	private String dir;
	private String basename;
	private ZipOutputStream zout;
	private ZipFile archive;
	private ArrayList entries;
	
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
			this.entries = new ArrayList(10);
			while (e.hasMoreElements())
			{
				ZipEntry entry = (ZipEntry)e.nextElement();
				String filename = entry.getName().toLowerCase();
				
				if (filename.endsWith("txt"))
				{
					this.entries.add(entry);
				}
				else if (filename.endsWith("properties"))
				{
					this.readTabInfo(entry);
				}
			}
		}
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
		return this.entries.size();
	}
	
	public void readHistoryData(int anIndex, SqlHistory history)
		throws IOException
	{
		if (!this.isReadOnly) throw new IllegalStateException("Workspace is opened for writing. Entry count is not available");
		if (anIndex > this.entries.size() - 1) throw new IndexOutOfBoundsException("Index " + anIndex + " is great then " + (this.entries.size() - 1));
		ZipEntry e = (ZipEntry)this.entries.get(anIndex);
		InputStream in = this.archive.getInputStream(e);
		history.readFromStream(in);
		return;
	}
	
	public Properties getSettings()
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
				catch (Exception e)
				{
					LogMgr.logError("WbWorkspace.close()", "Could not write tab info!", e);
				}
			}
			this.zout.close();
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

	public String getExternalFileName(int tabIndex)
	{
		if (this.tabInfo == null) return null;
		String key = "tab" + tabIndex + ".filename";
		String value = (String)this.tabInfo.get(key);
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
	
}
