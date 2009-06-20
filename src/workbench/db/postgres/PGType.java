/*
 * PGType.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.postgres;

/**
 *
 * @author support@sql-workbench.net
 */
public class PGType
{
	String rawType;
	String formattedType;
	int oid;

	public PGType(String raw, String formatted, int id)
	{
		rawType = raw;
		formattedType = formatted;
		oid = id;
	}
	
}
