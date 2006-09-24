/*
 * BlobFormatterFactory.java
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

import workbench.db.DbMetadata;

/**
 * @author support@sql-workbench.net
 */
public class BlobFormatterFactory
{

	public static BlobLiteralFormatter createAnsiFormatter()
	{
		// SQL Server, MySQL support the ANSI Syntax
		// using 0xABCDEF...
		// we use that for all others as well.
		HexBlobFormatter f = new HexBlobFormatter();
		f.setPrefix("0x");
		
		return f;
	}
	
	public static BlobLiteralFormatter createInstance(DbMetadata meta)
	{
		if (meta.isPostgres())
		{
			return new PostgresBlobFormatter();
		}
		else if ("db2_nt".equalsIgnoreCase(meta.getDbId()))
		{
			// Although the DB2 Manuals says it supports
			// binary string constants, it is very likely
			// that this will be rejected by DB2 due to the 
			// max.length of 32K for binary strings.
			HexBlobFormatter f = new HexBlobFormatter();
			f.setUseUpperCase(true);
			f.setPrefix("X'");
			f.setSuffix("'");
			return f;
		}
		else if (meta.isHsql())
		{
			HexBlobFormatter f = new HexBlobFormatter();
			f.setUseUpperCase(false);
			f.setPrefix("'");
			f.setSuffix("'");
			return f;
		}

		return createAnsiFormatter();
	}
	
}
