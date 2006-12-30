/*
 * NullValue.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage;

import java.util.HashMap;

import workbench.util.StringUtil;

public class NullValue
	implements Comparable
{
	private static HashMap valueCache = new HashMap();
	private int type;
	
	public static NullValue getInstance(int aType)
	{
		Integer key = new Integer(aType);
		NullValue val = (NullValue)valueCache.get(key);
		if (val == null)
		{
			val = new NullValue(aType);
			valueCache.put(key, val);
		}
		return val;
	}
	
	private NullValue(int aType)
	{
		this.type = aType;
	}
	public int getType() { return this.type; }
	public String toString() { return StringUtil.EMPTY_STRING; }
	
	public int compareTo(Object other)
	{
		if (other == null) return 0;
		if (other instanceof NullValue)
		{
			return 0;
		}
		else
		{
			return 1;
		}
	}
	
	public boolean equals(Object other)
	{
		if (other instanceof NullValue)
		{
			NullValue o = (NullValue)other;
			return o.getType() == this.getType();
		}
		else
		{
			return false;
		}
	}
}
	

