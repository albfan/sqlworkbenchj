/*
 * CharacterRange.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import workbench.resource.ResourceMgr;

/**
 * @author  support@sql-workbench.net
 */
public class CharacterRange
{
	public static final CharacterRange RANGE_NONE = new CharacterRange(0);
	public static final CharacterRange RANGE_CONTROL = new CharacterRange(1);
	public static final CharacterRange RANGE_7BIT = new CharacterRange(2);
	public static final CharacterRange RANGE_8BIT = new CharacterRange(3);
	public static final CharacterRange RANGE_8BIT_EXTENDED = new CharacterRange(4);
	
	private int typeIndex = 0;
	
	private CharacterRange(int index)
	{
		typeIndex = index;
	}
	
	public static boolean isValidId(int id)
	{
		return id >= 0 && id <= 4;
	}
	
	public static CharacterRange[] getRanges()
	{
		return new CharacterRange[] { RANGE_NONE, RANGE_CONTROL, RANGE_7BIT, RANGE_8BIT, RANGE_8BIT_EXTENDED };
	}
	
	public static CharacterRange getRangeById(int index)
	{
		switch (index)
		{
			case 0:
				return RANGE_NONE;
			case 1:
				return RANGE_CONTROL;
			case 2:
				return RANGE_7BIT;
			case 3:
				return RANGE_8BIT;
			case 4: 
				return RANGE_8BIT_EXTENDED;
		}
		return null;
	}
	
	public int getId()
	{
		return typeIndex;
	}
	
	public boolean isOutsideRange(char c)
	{
		switch (typeIndex)
		{
			case 0:
				return false;
			case 1:
				return (c < 32);
			case 2:
				return (c < 32 || c > 126);
			case 3:
				return (c < 32 || c > 255);
			case 4: 
				return (c < 32 || (c > 126 && c < 161) || c > 255);
		}
		return false;
	}

	public String getFilterDescription()
	{
		switch (typeIndex)
		{
			case 0:
				return ResourceMgr.getString("LblNone");
			case 1:
				return ResourceMgr.getString("LblExportRangeCtrl");
			case 2:
				return ResourceMgr.getString("LblExportRange7Bit");
			case 3:
				return ResourceMgr.getString("LblExportRange8Bit");
			case 4: 
				return ResourceMgr.getString("LblExportRange8BitExtended");
		}
		return "";
	}
	
	public String toString()
	{
		return getFilterDescription();
	}
}
