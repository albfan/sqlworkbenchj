/*
 * WbWorkspace.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.util;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.gui.sql.PanelType;
import workbench.gui.sql.SqlHistory;

/**
 *
 * @author  Thomas Kellerer
 */
public class WbWorkspace
	implements Closeable
{
  private static final String VARIABLES_FILENAME = "variables.properties";
  private static final String TABINFO_FILENAME = "tabs.properties";
	private static final String TOOL_ENTRY_PREFIX = "toolprop_";

  private enum WorkspaceState
  {
    closed,
    reading,
    writing;
  }

	private ZipOutputStream zout;
	private ZipFile archive;

  private WorkspaceState state = WorkspaceState.closed;
	private int tabCount = -1;

	private WbProperties tabInfo = new WbProperties(0);
	private Map<String, WbProperties> toolProperties = new HashMap<>();
  private WbProperties variables = new WbProperties(0);

  private String filename;

	public WbWorkspace(String archiveName)
	{
    if (archiveName == null) throw new NullPointerException("Filename cannot be null");
    this.filename = archiveName;
	}

  /**
   * Opens the workspace for writing.
   *
   * Nothing will be saved. To actually save the workspace content the following methods need to be called:
   *
   * <ul>
   * <li>{@link #saveProperties() }</li>
   * <li>{@link #addHistoryEntry(int, workbench.gui.sql.SqlHistory) }
   * </ul>
   *
   * If the workspace was already open, it is closed.
   *
   * @throws IOException
   * @see #saveProperties()
   */
  public void openForWriting()
    throws IOException
  {
    close();

    File f = new File(filename);
    OutputStream out = new BufferedOutputStream(new FileOutputStream(f), 64*1024);
    zout = new ZipOutputStream(out);
    zout.setLevel(Settings.getInstance().getIntProperty("workbench.workspace.compression", 9));
    zout.setComment("SQL Workbench/J Workspace file");

    state = WorkspaceState.writing;
  }

  /**
   * Opens the workspace for reading.
   *
   * This will automatically load all properties stored in the workspace, but not the panel statements.
   *
   * If the workspace was already open, it is closed and all internal properties are discarded.
   *
   * @throws IOException
   * @see #readHistoryData(int, workbench.gui.sql.SqlHistory)
   */
  public void openForReading()
    throws IOException
  {
    close();
    clear();

    this.zout = null;
    this.archive = new ZipFile(filename);

    ZipEntry entry = archive.getEntry(TABINFO_FILENAME);
    long size = (entry != null ? entry.getSize() : 0);
    if (size <= 0)
    {
      // Old definition of tabs for builds before 103.2
      entry = archive.getEntry("tabinfo.properties");
    }

    readTabInfo(entry);
    readToolProperties();
    readVariables();

    tabCount = calculateTabCount();

    state = WorkspaceState.reading;
  }

  public void setFilename(String archiveName)
  {
    if (archiveName == null) throw new NullPointerException("Filename cannot be null");
    if (state != WorkspaceState.closed) throw new IllegalStateException("Cannot change filename if workspace is open");
    filename = archiveName;
  }

  public String getFilename()
  {
    return filename;
  }

	public Map<String, WbProperties> getToolProperties()
	{
		return toolProperties;
	}

	public void setEntryCount(int count)
	{
		tabInfo.setProperty("tab.total.count", count);
	}

	public void addHistoryEntry(int index, SqlHistory history)
		throws IOException
	{
    if (state != WorkspaceState.writing) throw new IllegalStateException("Workspace is not opened for writing. addHistoryEntry() may not be called");

		ZipEntry entry = new ZipEntry("WbStatements" + (index + 1) + ".txt");
		this.zout.putNextEntry(entry);
		if (history != null)
		{
			history.writeToStream(zout);
		}
		zout.closeEntry();
	}

  public WbProperties getVariables()
  {
    WbProperties props = new WbProperties(0);
    props.putAll(variables);
    return props;
  }

  public void setVariables(Properties newVars)
  {
    variables.clear();
    if (newVars != null)
    {
      variables.putAll(newVars);
    }
  }

  /**
   * Returns the number of available tab entries.
   *
   * Calling this method is only valid if {@link #openForReading() } has been called before.
   *
   * @return the number of tabs stored in this workspace
   * @see #openForReading()
   */
	public int getEntryCount()
	{
    if (state != WorkspaceState.reading) throw new IllegalStateException("Workspace is not open for reading. Entry count is not available");
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

  /**
   *
   * @param anIndex
   * @param history
   * @throws IOException
   */
	public void readHistoryData(int anIndex, SqlHistory history)
		throws IOException
	{
    if (state != WorkspaceState.reading) throw new IllegalStateException("Workspace is not open for reading. Entry count is not available");

		ZipEntry e = this.archive.getEntry("WbStatements" + (anIndex + 1) + ".txt");
		if (e != null)
		{
			InputStream in = this.archive.getInputStream(e);
			history.readFromStream(in);
		}
	}

  public void saveProperties()
  {
    if (this.zout != null)
    {
      saveTabInfo();
      saveToolProperties();
      saveVariables();
    }
  }

	@Override
	public void close()
		throws IOException
	{
		if (this.zout != null)
		{
      zout.close();
      zout = null;
		}
		else if (this.archive != null)
		{
			archive.close();
      archive = null;
		}
    state = WorkspaceState.closed;
	}

  public WbProperties getSettings()
	{
		return this.tabInfo;
	}

  private void clear()
  {
    toolProperties.clear();
    variables.clear();
    tabInfo.clear();
  }

  private void readVariables()
  {
    if (archive == null) return;

    variables.clear();

    try
    {
 			ZipEntry entry = archive.getEntry(VARIABLES_FILENAME);
      if (entry != null && entry.getSize() > 0)
      {
        InputStream in = this.archive.getInputStream(entry);
        variables.load(in);
      }
    }
    catch (Exception ex)
    {
      LogMgr.logError("WbWorkspace.readVariables()", "Could not read variables file", ex);
    }
  }

	private void readToolProperties()
	{
    toolProperties.clear();
		Enumeration<? extends ZipEntry> entries = archive.entries();
		while (entries.hasMoreElements())
		{
			ZipEntry entry = entries.nextElement();
			String name = entry.getName();
			if (name.startsWith(TOOL_ENTRY_PREFIX))
			{
				WbFile f = new WbFile(name.substring(TOOL_ENTRY_PREFIX.length()));
				String toolkey = f.getFileName();
				WbProperties props = readProperties(entry);
				toolProperties.put(toolkey, props);
			}
		}
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

  private void saveVariables()
  {
    if (CollectionUtil.isEmpty(variables)) return;
    try
    {
      ZipEntry entry = new ZipEntry(VARIABLES_FILENAME);
      this.zout.putNextEntry(entry);
      variables.save(this.zout);
    }
    catch (Exception ex)
    {
      LogMgr.logError("WbWorkspace.saveVariables()", "Could not write variables", ex);
    }
    finally
    {
      try { zout.closeEntry(); } catch (Throwable th) {}
    }
  }

	private void saveToolProperties()
	{
    if (CollectionUtil.isEmpty(this.toolProperties)) return;
    try
    {
      for (Map.Entry<String, WbProperties> propEntry : toolProperties.entrySet())
      {
        ZipEntry entry = new ZipEntry(TOOL_ENTRY_PREFIX + propEntry.getKey() + ".properties");
        zout.putNextEntry(entry);
        propEntry.getValue().save(zout);
      }
    }
    catch (Exception ex)
    {
      LogMgr.logError("WbWorkspace.saveToolProperties()", "Could not write variables", ex);
    }
    finally
    {
      try { zout.closeEntry(); } catch (Throwable th) {}
    }
	}

	private void saveTabInfo()
	{
    if (CollectionUtil.isEmpty(tabInfo)) return;
    try
    {
      ZipEntry entry = new ZipEntry(TABINFO_FILENAME);
      this.zout.putNextEntry(entry);
      tabInfo.save(this.zout);
    }
    catch (Exception ex)
    {
      LogMgr.logError("WbWorkspace.saveToolProperties()", "Could not write variables", ex);
    }
    finally
    {
      try { zout.closeEntry(); } catch (Throwable th) {}
    }
	}

	private void readTabInfo(ZipEntry entry)
	{
		this.tabInfo = readProperties(entry);
	}

	private WbProperties readProperties(ZipEntry entry)
	{
		WbProperties props = new WbProperties(null, 1);
		try
		{
			InputStream in = this.archive.getInputStream(entry);
			props.load(in);
		}
		catch (Exception e)
		{
			LogMgr.logError("WbWorkspace.readProperties()", "Could not read property file: " + entry.getName(), e);
		}
		return props;
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
		String encoded = StringUtil.escapeText(name, CharacterRange.RANGE_7BIT);
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
		String encoded = StringUtil.escapeText(filename, CharacterRange.RANGE_7BIT);
		this.tabInfo.setProperty(key, encoded);
	}

	public void setExternalFileEncoding(int tabIndex, String encoding)
	{
		if (encoding == null) return;
		String key = "tab" + tabIndex + ".encoding";
		this.tabInfo.setProperty(key, encoding);
	}

}
