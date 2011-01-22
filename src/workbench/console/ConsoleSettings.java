/*
 * ConsoleSettings.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.console;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

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
	private List<PropertyChangeListener> listener = new ArrayList<PropertyChangeListener>();

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
		if (listener.size() == 0) return;
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
}
