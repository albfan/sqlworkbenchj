/*
 * OracleLongType.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2004, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.storage;

/**
 * Class to Wrap Oracle's LONG datatype
 * @author  info@sql-workbench.net
 */
class OracleLongType
{
	private final String value;
	
	public OracleLongType(String aValue)
	{
		this.value = aValue;
	}
	
	public String toString() { return this.value; }
	public String getValue() { return this.value; }
	
	public int getLength()
	{
		if (this.value == null) return 0;
		return this.value.length();
	}
}
