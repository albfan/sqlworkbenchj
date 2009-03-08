/*
 * 
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * Copyright 2002-2008, Thomas Kellerer
 * 
 * No part of this code maybe reused without the permission of the author
 * 
 * To contact the author please send an email to: support@sql-workbench.net
 * 
 */

package workbench.console;

/**
 * A singleton to control and manage the current display style in the console
 * It stores
 * <ul>
 *	<li>The current choice between row and page format</li>
 *  <li>The current page size if paging is enabled</li>
 * </ul>
 * @author support@sql-workbench.net
 */
public class ConsoleSettings
{
	private boolean pageResults;
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

	/**
	 * Returns the pager that should be used to display results.
	 *
	 * @return the Pager to be used. Never null
	 */
	public Pager getPager()
	{
		if (pageResults)
		{
			return new ConsolePager();
		}

		// Dummy pager
		return new Pager()
		{
			public boolean canPrintLine(int lineNumber)
			{
				return true;
			}
		};
	}

	public void setEnablePager(boolean flag)
	{
		pageResults = flag;
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
