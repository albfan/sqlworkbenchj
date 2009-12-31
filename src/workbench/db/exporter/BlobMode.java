/*
 * BlobMode.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.exporter;

import java.util.List;
import workbench.util.CollectionUtil;

/**
 * Define codes for the different ways how BLOBs can be handled by the export classes.
 *
 * @author Thomas Kellerer
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

	/**
	 * Encode the blob using a Base64 encoding (e.g. for Postgres COPY format)
	 */
	Base64,

	None;

	/**
	 * Convert a user-supplied mode keyword to the matching BlobMode
	 * Valid input strings are:
	 * <ul>
	 * <li><tt>none</tt> - maps to {@link #None}</li>
	 * <li><tt>ansi</tt> - maps to {@link #AnsiLiteral}</li>
	 * <li><tt>dbms</tt> - maps to {@link #DbmsLiteral}</li>
	 * <li><tt>file</tt> - maps to {@link #SaveToFile}</li>
	 * <li><tt>base64</tt> - maps  to {@link #Base64}</li>
	 * </ul>
	 * @param type the type as entered by the user
	 * @return null if the type was invalid, the corresponding BlobMode otherwise
	 */
	public static BlobMode getMode(String type)
	{
		if (type == null) return BlobMode.None;
		if ("none".equalsIgnoreCase(type.trim())) return BlobMode.None;
		if ("ansi".equalsIgnoreCase(type.trim())) return BlobMode.AnsiLiteral;
		if ("dbms".equalsIgnoreCase(type.trim())) return BlobMode.DbmsLiteral;
		if ("file".equalsIgnoreCase(type.trim())) return BlobMode.SaveToFile;
		if ("base64".equalsIgnoreCase(type.trim())) return BlobMode.Base64;
		return null;
	}

	public static List<String> getTypes()
	{
		return CollectionUtil.arrayList("file", "ansi", "dbms", "base64");
	}

}
