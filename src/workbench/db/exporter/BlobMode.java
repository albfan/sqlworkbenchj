/*
 * BlobMode.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.exporter;

import java.util.List;
import workbench.util.CollectionBuilder;

/**
 * Define codes for the different ways how BLOBs can be handled by the export classes.
 *
 * @author support@sql-workbench.net
 */
public enum BlobMode
{
	/**
	 * Use a DBMS specific literals for BLOBs in SQL statements.
	 * @see workbench.storage.BlobFormatterFactory#createInstance(workbench.db.DbMetadata meta)
	 * @see workbench.db.exporter.DataExporter#setBlobMode(String)
	 */
	DbmsLiteral,

	/**
	 * Use ANSI literals for BLOBs in SQL statements.
	 * @see workbench.storage.BlobFormatterFactory#createAnsiFormatter()
	 * @see workbench.db.exporter.DataExporter#setBlobMode(String)
	 */
	AnsiLiteral,

	/**
	 * Generate WB Specific {$blobfile=...} statements
	 * @see workbench.db.exporter.DataExporter#setBlobMode(String)
	 */
	SaveToFile,

	None;

	/**
	 * Convert a user-supplied mode keyword to the matching BlobMode
	 * Valid input strings are:
	 * <ul>
	 * <li><tt>none</tt> - maps to {@link #None}</li>
	 * <li><tt>ansi</tt> - maps to {@link #AnsiLiteral}</li>
	 * <li><tt>dbms</tt> - maps to {@link #DbmsLiteral}</li>
	 * <li><tt>file</tt> - maps to {@link #SaveToFile}</li>
	 * </ul>
	 * @param type the type as entered by the user
	 * @return null if the type was invalid, the corresponding BlobMode otherwise
	 */
	public static BlobMode getMode(String type)
	{
		if (type == null) return BlobMode.None;
		if ("none".equalsIgnoreCase(type)) return BlobMode.None;
		if ("ansi".equalsIgnoreCase(type)) return BlobMode.AnsiLiteral;
		if ("dbms".equalsIgnoreCase(type)) return BlobMode.DbmsLiteral;
		if ("file".equalsIgnoreCase(type)) return BlobMode.SaveToFile;
		return null;
	}

	public static List<String> getTypes()
	{
		return CollectionBuilder.arrayList("file", "ansi", "dbms");
	}

}
