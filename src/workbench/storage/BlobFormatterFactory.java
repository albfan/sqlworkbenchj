/*
 * BlobFormatterFactory.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage;

import workbench.db.DbMetadata;
import workbench.db.DbSettings;
import workbench.util.StringUtil;

/**
 * @author Thomas Kellerer
 */
public class BlobFormatterFactory
{

	public static BlobLiteralFormatter createAnsiFormatter()
	{
		// ANSI Syntax is 0xABCDEF...
		// which is e.g. used by SQL Server
		DefaultBlobFormatter f = new DefaultBlobFormatter();
		f.setPrefix("0x");
		
		return f;
	}

	public static BlobLiteralFormatter createInstance(BlobLiteralType type)
	{
		DefaultBlobFormatter f = new DefaultBlobFormatter();
		f.setLiteralType(type);
		return f;
	}
	
	public static BlobLiteralFormatter createInstance(DbMetadata meta)
	{
		// Check for a user-defined formatter definition
		// for the current DBMS
		DbSettings s = meta.getDbSettings();
		String prefix = s.getBlobLiteralPrefix();
		String suffix = s.getBlobLiteralSuffix();
		if (StringUtil.isNonBlank(prefix) && StringUtil.isNonBlank(suffix))
		{
			DefaultBlobFormatter f = new DefaultBlobFormatter();
			String type = s.getBlobLiteralType();
			
			BlobLiteralType literalType = null;
			try
			{
				literalType = BlobLiteralType.valueOf(type);
			}
			catch (Throwable e)
			{
				literalType = BlobLiteralType.hex;
			}
			
			BlobLiteralType.valueOf(type);
			f.setUseUpperCase(s.getBlobLiteralUpperCase());
			f.setLiteralType(literalType);
			f.setPrefix(prefix);
			f.setSuffix(suffix);
			return f;
		}		
		
		// No user-defined formatter definition found, use the built-in settings
		if (meta.isPostgres())
		{
			return new PostgresBlobFormatter();
		}
		else if (meta.isOracle())
		{
			// this might only work with Oracle 10g...
			// and will probably fail on BLOBs > 4KB
			DefaultBlobFormatter f = new DefaultBlobFormatter();
			f.setUseUpperCase(true);
			f.setPrefix("to_blob(utl_raw.cast_to_raw('0x");
			f.setSuffix("'))");
			return f;
		}
		else if (meta.getDbId().startsWith("db2") || meta.isH2())
		{
			// Although the DB2 Manuals says it supports
			// binary string constants, it is very likely
			// that this will be rejected by DB2 due to the 
			// max.length of 32K for binary strings.
			DefaultBlobFormatter f = new DefaultBlobFormatter();
			f.setUseUpperCase(true);
			f.setPrefix("X'");
			f.setSuffix("'");
			return f;
		}
		else if (meta.isHsql())
		{
			DefaultBlobFormatter f = new DefaultBlobFormatter();
			f.setUseUpperCase(false);
			f.setPrefix("'");
			f.setSuffix("'");
			return f;
		}

		// Still no luck, use the ANSI format.
		return createAnsiFormatter();
	}
	
}
