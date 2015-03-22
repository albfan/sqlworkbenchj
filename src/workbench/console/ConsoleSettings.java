/*
 * ConsoleSettings.java
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
package workbench.console;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import workbench.resource.GuiSettings;
import workbench.resource.Settings;

import workbench.util.StringUtil;

/**
 * A singleton to control and manage the current display style in the console.
 * <br>
 * It stores
 * <ul>
 *	<li>The current choice between row and page format</li>
 *  <li>The current page size if paging is enabled</li>
 * </ul>
 * @author Thomas Kellerer
 */
public class ConsoleSettings
{
	private RowDisplay rowDisplay = RowDisplay.SingleLine;
	private RowDisplay nextRowDisplay;
	private List<PropertyChangeListener> listener = new ArrayList<>();

	protected static class LazyInstanceHolder
	{
		protected static ConsoleSettings instance = new ConsoleSettings();
	}

	private ConsoleSettings()
	{
	}

	public static ConsoleSettings getInstance()
	{
		return LazyInstanceHolder.instance;
	}

	public RowDisplay getRowDisplay()
	{
		return rowDisplay;
	}

	public void addChangeListener(PropertyChangeListener l)
	{
		listener.add(l);
	}

	public void removeChangeListener(PropertyChangeListener l)
	{
		listener.remove(l);
	}

	protected void firePropertyChange(RowDisplay oldDisplay, RowDisplay newDisplay)
	{
		if (listener.isEmpty()) return;
		PropertyChangeEvent evt = new PropertyChangeEvent(this, "display", oldDisplay, newDisplay);
		for (PropertyChangeListener l : listener)
		{
			l.propertyChange(evt);
		}
	}

	/**
	 * Change the row display permanently.
	 * @param display the new display style. Ignored when null
	 */
	public synchronized void setRowDisplay(RowDisplay display)
	{
		RowDisplay old = rowDisplay;
		if (display != null) rowDisplay = display;
		firePropertyChange(old, rowDisplay);
	}

	public synchronized void setNextRowDisplay(RowDisplay display)
	{
		this.nextRowDisplay = display;
	}

	public synchronized RowDisplay getNextRowDisplay()
	{
		if (nextRowDisplay != null)
		{
			RowDisplay d = nextRowDisplay;
			nextRowDisplay = null;
			return d;
		}
		return rowDisplay;
	}

	/**
	 * Convenience method to retrieve the NULL string to be used in the console
	 *
	 * This gets the value for the property <tt>workbench.console.nullstring</tt>
	 */
	public static String getNullString()
	{
		String display = Settings.getInstance().getProperty("workbench.console.nullstring", GuiSettings.getDisplayNullString());
		if (display == null) return StringUtil.EMPTY_STRING;
		return display;
	}

	public static boolean showProfileInPrompt()
	{
		return Settings.getInstance().getBoolProperty("workbench.console.prompt.useprofilename", false);
	}

	public static boolean showScriptFinishTime()
	{
		return Settings.getInstance().getBoolProperty("workbench.console.script.showtime", false);
	}

	public static boolean changeTerminalTitle()
	{
		return Settings.getInstance().getBoolProperty("workbench.console.terminal.title.change", true);
	}

	public static boolean termTitleAppNameAtEnd()
	{
		return Settings.getInstance().getBoolProperty("workbench.console.terminal.title.appname.end", true);
	}

	public static boolean termTitleIncludeUrl()
	{
		return Settings.getInstance().getBoolProperty("workbench.console.terminal.title.show.url", false);
	}

	public static boolean useHistoryPerProfile()
	{
		return Settings.getInstance().getBoolProperty("workbench.console.history.per_profile", true);
	}

	public static String getDefaultRefreshInterval()
	{
		return Settings.getInstance().getProperty("workbench.console.refresh.interval.default", "5s");
	}
}
