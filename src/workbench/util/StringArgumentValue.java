/*
 * StringArgumentValue
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.util;

/**
 *
 * @author Thomas Kellerer
 */
public class StringArgumentValue
	implements ArgumentValue
{
	private String value;

	public StringArgumentValue(String argValue)
	{
		value = argValue;
	}

	@Override
	public String getDisplay()
	{
		return value;
	}

	@Override
	public String getValue()
	{
		return value;
	}

	@Override
	public String toString()
	{
		return value;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == null)
		{
			return false;
		}
		if (getClass() != obj.getClass())
		{
			return false;
		}
		final StringArgumentValue other = (StringArgumentValue) obj;
		if ((this.value == null) ? (other.value != null) : !this.value.equals(other.value))
		{
			return false;
		}
		return true;
	}

	@Override
	public int hashCode()
	{
		int hash = 5;
		hash = 89 * hash + (this.value != null ? this.value.hashCode() : 0);
		return hash;
	}

	
}
