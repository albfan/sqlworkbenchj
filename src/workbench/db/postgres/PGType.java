/*
 * PGType.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.postgres;

/**
 *
 * @author Thomas Kellerer
 */
public class PGType
{
	String rawType;
	String formattedType;
	int oid;

	public PGType(String raw, String formatted, int id)
	{
		if (raw.equals("int2"))
		{
			rawType = "smallint";
		}
		else if (raw.equals("int4"))
		{
			rawType = "integer";
		}
		else if (raw.equals("int8"))
		{
			rawType = "bigint";
		}
		else if (raw.equals("bool"))
		{
			rawType = "boolean";
		}
		else
		{
			rawType = raw;
		}
		formattedType = formatted;
		oid = id;
	}
	
}
