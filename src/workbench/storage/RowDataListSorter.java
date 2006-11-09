/*
 * RowDataListSorter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage;

import java.text.CollationKey;
import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;
import workbench.resource.Settings;

/**
 * A class to sort a RowDataList
 * @author  support@sql-workbench.net
 */
public class RowDataListSorter
	implements Comparator
{
	private int[] sortColumns;
	private boolean[] sortAscending;
	private Collator defaultCollator;
	
	public RowDataListSorter(int column, boolean ascending)
	{
		this.sortColumns = new int[] { column };
		this.sortAscending = new boolean[] { ascending };
		initCollator();
	}
	
	public RowDataListSorter(int[] columns, boolean[] order)
	{
		if (columns.length != order.length) throw new IllegalArgumentException("Size of arrays must match");
		this.sortColumns = new int[columns.length];
		System.arraycopy(columns, 0, sortColumns, 0, columns.length);
		
		this.sortAscending = new boolean[columns.length];
		System.arraycopy(order, 0, sortAscending, 0, columns.length);
		initCollator();
	}
	
	private void initCollator()
	{
		// Using a Collator to compare Strings is much slower then 
		// using String.compareTo() so by default this is disabled
		boolean useCollator = Settings.getInstance().getUseCollator();
		if (useCollator)
		{
			Locale l = null;
			String lang = Settings.getInstance().getSortLanguage();
			String country = Settings.getInstance().getSortCountry();
			try
			{
				if (lang != null && country != null)
				{
					l = new Locale(lang, country);
				}
				else if (lang != null && country == null)
				{
					l = new Locale(lang);
				}
			}
			catch (Exception e)
			{
				l = Locale.getDefault();
			}
			defaultCollator = Collator.getInstance(l);
		}
	}	
	
	public void sort(RowDataList data)
	{
		data.sort(this);
	}
	
	/**
	 * Compares the defined sort column 
	 */
	private int compareColumn(int column, RowData row1, RowData row2)
	{
		Object o1 = row1.getValue(column);
		Object o2 = row2.getValue(column);

		// Special handling for NULL values 
		// Even though NullValue implements the Comparable
		// interface String.compareTo(NullValue) does not 
		// work correctly, so we'll handle the situation where
		// one value is null before calling compareTo()
		if ( (o1 == null && o2 == null) || (o1 instanceof NullValue && o2 instanceof NullValue) )
		{
			return 0;
		}
		else if (o1 == null || o1 instanceof NullValue)
		{
			return 1;
		}
		else if (o2 == null || o2 instanceof NullValue)
		{
			return -1;
		}

		// Special handling for String columns 
		if (defaultCollator != null)
		{
			if (o1 instanceof String && o2 instanceof String)
			{
				return defaultCollator.compare(o1, o2);
			}
		}
		
		int result = 0;
		try
		{
			result = ((Comparable)o1).compareTo(o2);
		}
		catch (Throwable e)
		{
			// If one of the objects did not implement 
			// the comparable interface, we'll use the 
			// toString() values to compare them
			String v1 = o1.toString();
			String v2 = o2.toString();
			result = v1.compareTo(v2);
		}
		return result;
	}

	public int compare(Object o1, Object o2)
	{
		try
		{
			RowData row1 = (RowData)o1;
			RowData row2 = (RowData)o2;
			
			int colIndex = 0;
			int result = compareColumn(sortColumns[colIndex], row1, row2);
			result = this.sortAscending[colIndex] ? result : -result;
			while (result == 0 && colIndex < this.sortColumns.length - 1)
			{
				colIndex ++;
				result = compareColumn(sortColumns[colIndex], row1, row2);
				result = this.sortAscending[colIndex] ? result : -result;
			}
			return result;
		}
		catch (ClassCastException e)
		{
			// should not happen
		}
		return 0;
	}
	
}
