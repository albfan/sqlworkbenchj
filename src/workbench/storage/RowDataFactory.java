/*
 * RowDataFactory.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage;

import workbench.db.DbMetadata;
import workbench.db.WbConnection;
import workbench.db.mssql.SqlServerDataConverter;
import workbench.db.oracle.OracleDataConverter;
import workbench.resource.Settings;

/**
 * A factory to create instances of RowData.
 * <br>
 * When creating a new RowData instance a DataConverter is automatically registered
 * if one is available for the current connection.
 * <br/>
 * This is done by re-using singleton converters to minimize the memory used during data retrieval.
 *
 * @author Thomas Kellerer
 */
public class RowDataFactory
{

	public static RowData createRowData(int colCount, WbConnection conn)
	{
		RowData result = new RowData(colCount);
		result.setConverter(getConverterInstance(conn));
		return result;
	}

	public static RowData createRowData(ResultInfo info, WbConnection conn)
	{
		RowData result = new RowData(info);
		result.setConverter(getConverterInstance(conn));
		return result;
	}

	/**
	 * Creates instances of necessary DataConverters.
	 * <br/>
	 * The following datatypes are currently supported:
	 * <ul>
	 * <li>For SQL Server's timestamp type</li>
	 * <li>For Oracle: RAW and ROWID types</li>
	 * </ul>
	 *
	 * @param conn the connection for which to create the DataConverter
	 * @return a suitable converter or null if nothing should be converted
	 *
	 * @see workbench.resource.Settings#getFixSqlServerTimestampDisplay()
	 * @see workbench.resource.Settings#getConvertOracleTypes()
	 * @see workbench.db.oracle.OracleDataConverter
	 * @see workbench.db.mssql.SqlServerDataConverter
	 */
	public static DataConverter getConverterInstance(WbConnection conn)
	{
		if (conn == null) return null;

		DbMetadata meta = conn.getMetadata();
		if (meta == null) return null;

		if (meta.isOracle() && Settings.getInstance().getConvertOracleTypes())
		{
			return OracleDataConverter.getInstance();
		}
		if (meta.isSqlServer() && Settings.getInstance().getFixSqlServerTimestampDisplay())
		{
			return SqlServerDataConverter.getInstance();
		}
		return null;
	}


}
