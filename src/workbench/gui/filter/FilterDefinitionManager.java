/*
 * FilterDefinitionManager.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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
package workbench.gui.filter;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import workbench.interfaces.PropertyStorage;
import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.storage.filter.FilterExpression;

import workbench.util.FixedSizeList;
import workbench.util.WbFile;
import workbench.util.WbPersistence;

/**
 * Provide a MRU lookup for filter definitions saved to a file.
 *
 * @author Thomas Kellerer
 */
public class FilterDefinitionManager
{
	private List<PropertyChangeListener> listeners;
	private FixedSizeList<WbFile> filterFiles;
	private static final int DEFAULT_MAX_SIZE = 15;
  private final String propertyPrefix;

  private static FilterDefinitionManager DEFAULT_INSTANCE;

	public synchronized static FilterDefinitionManager getDefaultInstance()
	{
		if (DEFAULT_INSTANCE == null)
		{
			DEFAULT_INSTANCE = new FilterDefinitionManager();
      DEFAULT_INSTANCE.load(Settings.getInstance());
		}
		return DEFAULT_INSTANCE;
	}

	public FilterDefinitionManager()
  {
    this("workbench.gui");
  }

	public FilterDefinitionManager(String prefix)
	{
    this.propertyPrefix = prefix;
		int size = Settings.getInstance().getIntProperty("workbench.gui.filter.mru.maxsize", DEFAULT_MAX_SIZE);
		this.filterFiles = new FixedSizeList<>(size);
	}

	public void load(PropertyStorage settings)
  {
    load(settings, propertyPrefix);
  }

	public void load(PropertyStorage settings, String prefix)
	{
    if (!prefix.endsWith("."))
    {
      prefix += ".";
    }

		int size = settings.getIntProperty(prefix + "filter.mru.size", 0);
		for (int i=0; i < size; i++)
		{
			String filename = settings.getProperty(prefix + "filter.mru.entry." + i, null);
			if (filename != null)
			{
				WbFile f = new WbFile(filename);
				if (f.exists())
				{
					filterFiles.append(f);
				}
				else
				{
					LogMgr.logInfo("FilterDefinitionManager.loadMRUList()", "Removed filter file '" + f.getFullPath() + "' from list because it does not longer exist");
				}
			}
		}
	}

	private void removeOldSettings(PropertyStorage s, String prefix)
	{
		int size = s.getIntProperty(prefix + "filter.mru.maxsize", DEFAULT_MAX_SIZE);
		for (int i=0; i < size; i++)
		{
			s.removeProperty(prefix + "filter.mru.entry." + i);
		}
	}

	public void saveMRUList(PropertyStorage s)
  {
    saveMRUList(s, "workbench.gui");
  }

	public void saveMRUList(PropertyStorage s, String prefix)
	{
    if (!prefix.endsWith("."))
    {
      prefix += ".";
    }

		removeOldSettings(s, prefix);
		int index = 0;
		for (WbFile f : filterFiles.getEntries())
		{
			s.setProperty(prefix + "filter.mru.entry." + index, f.getFullPath());
			index ++;
		}
		s.setProperty(prefix + "filter.mru.size", index);
	}

  public synchronized void removePropertyChangeListener(PropertyChangeListener l)
  {
    if (listeners != null)
    {
      listeners.remove(l);
    }
  }

	public synchronized void addPropertyChangeListener(PropertyChangeListener l)
	{
		if (this.listeners == null) this.listeners = new ArrayList<>(2);
		this.listeners.add(l);
	}

	public synchronized void filterUsed(WbFile filename)
	{
		filterFiles.addEntry(filename);
		firePropertyChanged();
	}

	private synchronized void firePropertyChanged()
	{
		if (this.listeners == null) return;
		PropertyChangeEvent evt = new PropertyChangeEvent(this, "mruList", null, Integer.valueOf(filterFiles.size()));
		for(PropertyChangeListener l : listeners)
		{
			if (l == null) continue;
			l.propertyChange(evt);
		}
	}

	public List<WbFile> getEntries()
	{
		List<WbFile> result = new ArrayList<>(filterFiles.getEntries());
    result.removeIf(f -> !f.exists());
    return result;
	}

  public String getLastFilterDir()
  {
    if (this == DEFAULT_INSTANCE)
    {
      return Settings.getInstance().getLastFilterDir();
    }
    return Settings.getInstance().getProperty(propertyPrefix + ".lastdir", Settings.getInstance().getLastFilterDir());
  }

  public void setLastFilterDir(String dirName)
  {
    if (this == DEFAULT_INSTANCE)
    {
      Settings.getInstance().setLastFilterDir(dirName);
    }
    else
    {
      Settings.getInstance().setProperty(propertyPrefix + ".lastdir", dirName);
    }
  }

	public void saveFilter(FilterExpression filter, WbFile file)
		throws IOException
	{
		WbPersistence p = new WbPersistence(file.getFullPath());
		p.writeObject(filter);
		filterUsed(file);
	}

	public FilterExpression loadFilter(String filename)
		throws Exception
	{
		WbPersistence p = new WbPersistence(filename);
		FilterExpression result = null;
		Object o = p.readObject();
		if (o instanceof FilterExpression)
		{
			result = (FilterExpression)o;
			filterUsed(new WbFile(filename));
		}
		return result;
	}
}
