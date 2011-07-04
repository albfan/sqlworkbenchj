/*
 * DriverListModel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.profiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.ListModel;
import javax.swing.event.ListDataListener;

import workbench.db.DbDriver;

/**
 *
 * @author  Thomas Kellerer
 */
class DriverListModel
	implements ListModel
{
	private List<DbDriver> drivers;

	public DriverListModel(List<DbDriver> aDriverList)
	{
		this.drivers = new ArrayList<DbDriver>(aDriverList);
		Comparator<DbDriver> comp = new Comparator<DbDriver>()
		{
			@Override
			public int compare(DbDriver o1, DbDriver o2)
			{
				if (o1 == null && o2 == null) return 0;
				if (o1 == null) return -1;
				if (o2 == null) return 1;
				return o1.getName().compareTo(o2.getName());
			}
		};
		Collections.sort(this.drivers, comp);
	}

	/**
	 * Adds a listener to the list that's notified each time a change
	 * to the data model occurs.
	 *
	 * @param l the <code>ListDataListener</code> to be added
	 *
	 */
	@Override
	public void addListDataListener(ListDataListener l)
	{
	}

	/** Returns the value at the specified index.
	 * @param index the requested index
	 * @return the value at <code>index</code>
	 *
	 */
	@Override
	public Object getElementAt(int index)
	{
		return this.getDriver(index).getName();
	}

	public DbDriver getDriver(int index)
	{
		return this.drivers.get(index);
	}

	/**
	 * Returns the length of the list.
	 * @return the length of the list
	 *
	 */
	@Override
	public int getSize()
	{
		return this.drivers.size();
	}

	@Override
	public void removeListDataListener(ListDataListener l)
	{
	}

	public void addDriver(DbDriver aDriver)
	{
		this.drivers.add(this.drivers.size(), aDriver);
	}

	public void deleteDriver(int index)
	{
		this.drivers.remove(index);
	}

	public void putDriver(int index, DbDriver aDriver)
	{
		this.drivers.set(index, aDriver);
		index = 1;
	}

	public List<DbDriver> getValues()
	{
		return this.drivers;
	}
}

