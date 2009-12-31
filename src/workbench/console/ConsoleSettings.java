/*
 * ConsoleSettings.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.console;

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
	
	protected static class LazyInstanceHolder
	{
		protected static ConsoleSettings instance = new ConsoleSettings();
	}

	private ConsoleSettings()
	{
	}

	public static final ConsoleSettings getInstance()
	{
		return LazyInstanceHolder.instance;
	}

	public RowDisplay getRowDisplay()
	{
		return rowDisplay;
	}

	/**
	 * Change the row display permanently.
	 * @param display the new display style. Ignored when null
	 */
	public synchronized void setRowDisplay(RowDisplay display)
	{
		if (display != null) rowDisplay = display;
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
}
