/*
 * OracleLongType.java
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

/**
 * Class to Wrap Oracle's LONG datatype. This is merely used to
 * identify the LONG datatype when storing the content into the database
 * as it requires a special treatment.
 * @see DmlStatement#executePrepared(Connection)
 * @author  support@sql-workbench.net
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
