/*
 * RowDataFactory.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
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
 * A factory to create instances of RowData.<br/>
 * <br/>
 * When creating a new instance a possible converter is automatically registered
 * with the created instance.
 * 
 * @author support@sql-workbench.net
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

	public static DataConverter getConverterInstance(WbConnection conn)
	{
		if (conn == null) return null;

		DbMetadata meta = conn.getMetadata();
		if (meta == null) return null;

		if (meta.isSqlServer() && Settings.getInstance().getFixSqlServerTimestampDisplay())
		{
			return SqlServerDataConverter.getInstance();
		}
		if (meta.isOracle() && Settings.getInstance().getConvertOracleRawData())
		{
			return OracleDataConverter.getInstance();
		}
		return null;
	}


}
