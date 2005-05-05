/*
 * RowDataListSorter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
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
 * @author  info@sql-workbench.net
 */
public class RowDataListSorter
	implements Comparator
{
	private int sortColumn;
	private int colType;
	private boolean sortAscending;
	private Collator defaultCollator;
	private CollationKey[] keys;
	
	public RowDataListSorter(int column, boolean ascending)
	{
		this.sortColumn = column;
		this.sortAscending = ascending;
		
		Locale l = null;
		String lang = null;
		String country = null;
		try
		{
			lang = System.getProperty("org.kellerer.sort.language", System.getProperty("user.language", "en"));
			country = System.getProperty("org.kellerer.sort.country", System.getProperty("user.country", null));
		}
		catch (Exception e)
		{
			l = Locale.ENGLISH;
		}

		if (lang != null && country != null)
		{
			l = new Locale(lang, country);
		}
		else if (lang != null && country == null)
		{
			l = new Locale(lang);
		}
		defaultCollator = Collator.getInstance(l);
	}	
	
	public void sort(RowDataList data)
	{
		data.sort(this);
	}
	
	private int compareRows(RowData row1, RowData row2)
	{
		Object o1 = row1.getValue(sortColumn);
		Object o2 = row2.getValue(sortColumn);

		if (o1 == null && o2 == null)
		{
			return 0;
		}
		else if (o1 == null)
		{
			return 1;
		}
		else if (o2 == null)
		{
			return -1;
		}

		if (o1 instanceof String && o2 instanceof String)
		{
			return defaultCollator.compare(o1, o2);
		}

		try
		{
			int result = ((Comparable)o1).compareTo(o2);
			return result;
		}
		catch (Throwable e)
		{
		}

		String v1 = o1.toString();
		String v2 = o2.toString();
		return v1.compareTo(v2);
	}

	public int compare(Object o1, Object o2)
	{
		try
		{
			RowData row1 = (RowData)o1;
			RowData row2 = (RowData)o2;
			int result = compareRows(row1, row2);
			return this.sortAscending ? result : -result;
		}
		catch (ClassCastException e)
		{
			// cannot happen
		}
		return 0;
	}
	
}
