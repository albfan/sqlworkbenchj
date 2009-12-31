/*
 * ExportType.java
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

/**
 *
 * @author Thomas Kellerer
 */
public enum ExportType
{
	SQL_INSERT("SQL"),
	SQL_UPDATE("SQL Update"),
	SQL_DELETE_INSERT("SQL Delete/Insert"),
	HTML("HTML"),
	TEXT("Text"),
	XML("XML"),
	ODS("OpenDocument Spreadsheet"),
	XLS("XLS"),
	XLSX("XLSX");

	private String display;
	
	private ExportType(String disp)
	{
		display = disp;
	}

	public String toString()
	{
		return display;
	}
	
	public static ExportType getExportType(String type)
	{
		if (type.equalsIgnoreCase("txt")) return TEXT;
		if (type.equalsIgnoreCase("sql")) return SQL_INSERT;
		if (type.equalsIgnoreCase("sqlinsert")) return SQL_INSERT;
		if (type.equalsIgnoreCase("sqlupdate")) return SQL_UPDATE;
		if (type.equalsIgnoreCase("sqldeleteinsert")) return SQL_DELETE_INSERT;

		try
		{
			return valueOf(type.toUpperCase());
		}
		catch (Exception ex)
		{
			return null;
		}
	}

	public static ExportType getTypeFromCode(String code)
	{
		if (code == null) return null;
		if (code.equals("1")) return SQL_INSERT;
		if (code.equals("2")) return TEXT;
		if (code.equals("3")) return XML;
		if (code.equals("4")) return HTML;
		if (code.equals("5")) return ODS;
		if (code.equals("6")) return XLSX;
		if (code.equals("7")) return XLS;
		return null;
	}

	public boolean isSqlType()
	{
		return this == SQL_INSERT || this == SQL_UPDATE || this == SQL_DELETE_INSERT;
	}
	
	public String getDefaultFileExtension()
	{
		switch (this)
		{
			case SQL_INSERT:
			case SQL_UPDATE:
			case SQL_DELETE_INSERT:
				return ".sql";

			case TEXT:
				return ".txt";

			case XML:
				return ".xml";

			case HTML:
				return ".html";

			case ODS:
				return ".ods";

			case XLSX:
				return ".xlsx";

			case XLS:
				return ".xls";
		}
		return null;
	}
	
	public String getCode()
	{
		switch (this)
		{
			case SQL_INSERT:
			case SQL_UPDATE:
			case SQL_DELETE_INSERT:
				return "1";

			case TEXT:
				return "2";

			case XML:
				return "3";

			case HTML:
				return "4";

			case ODS:
				return "5";

			case XLSX:
				return "7";

			case XLS:
				return "6";
		}
		return null;
	}
	
}
