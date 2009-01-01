/*
 * WbMediaSizeName.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.print;

import javax.print.attribute.EnumSyntax;
import javax.print.attribute.standard.MediaSizeName;

/**
 *
 * @author support@sql-workbench.net
 */
public class WbMediaSizeName 
	extends MediaSizeName
{
	
	public WbMediaSizeName(int aValue)
	{
		super(aValue);
	}
	
	public static String getName(MediaSizeName aMedia)
	{
		int value = aMedia.getValue();
		WbMediaSizeName dummy = new WbMediaSizeName(0);
		String[] names = dummy.getStringTable();
		if (value >= 0 && value < names.length)
		{
			return names[value];
		}
		return null;
	}
	
	public static MediaSizeName getMediaSize(String aName)
	{
		WbMediaSizeName dummy = new WbMediaSizeName(0);
		//String[] names = dummy.getStringTable();
		EnumSyntax[] values = dummy.getEnumValueTable();
		
		for (int i=0; i < values.length; i++)
		{
			MediaSizeName entry = (MediaSizeName)values[i];
			if (entry.getName().equals(aName))
			{
				return entry;
			}
		}
		return null;
	}
}
