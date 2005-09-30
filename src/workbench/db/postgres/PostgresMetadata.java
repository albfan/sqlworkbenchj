/*
 * PostgresMetadata.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.postgres;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import workbench.db.DbMetadata;
import workbench.db.JdbcProcedureReader;
import workbench.db.ProcedureReader;
import workbench.storage.DataStore;
import workbench.util.StrBuffer;

/**
 * @author  support@sql-workbench.net
 */
public class PostgresMetadata
	implements ProcedureReader
{
	private DbMetadata metaData;
	
	public PostgresMetadata(DbMetadata meta)
	{
		this.metaData = meta;
	}
	
	public DataStore getProcedureColumns(String aCatalog, String aSchema, String aProcname)
		throws SQLException
	{
		JdbcProcedureReader reader = new JdbcProcedureReader(this.metaData);
		return reader.getProcedureColumns(aCatalog, aSchema, aProcname);
	}
	
	public DataStore getProcedures(String catalog, String schema)
		throws SQLException
	{
		JdbcProcedureReader procReader = new JdbcProcedureReader(this.metaData);
		return procReader.getProcedures(catalog, schema);
	}
	
	public StrBuffer getProcedureHeader(String aCatalog, String aSchema, String aProcname, int procType)
	{
		StrBuffer source = new StrBuffer();
		try
		{
			DataStore ds = this.metaData.getProcedureColumns(aCatalog, aSchema, aProcname);
			source.append("CREATE OR REPLACE ");
			
			if (procType == DatabaseMetaData.procedureReturnsResult) source.append("FUNCTION ");
			else source.append("PROCEDURE ");
			
			source.append(aProcname);
			source.append(" (");
			String retType = null;
			int count = ds.getRowCount();
			int added = 0;
			for (int i=0; i < count; i++)
			{
				String vartype = ds.getValueAsString(i,ProcedureReader.COLUMN_IDX_PROC_COLUMNS_DATA_TYPE);
				String ret = ds.getValueAsString(i,ProcedureReader.COLUMN_IDX_PROC_COLUMNS_RESULT_TYPE);
				if ("RETURN".equals(ret))
				{
					retType = vartype;
				}
				else
				{
					if (added > 0) source.append(",");
					source.append(vartype);
					added ++;
				}
			}
			source.append(")");
			source.append("\nRETURNS ");
			source.append(retType);
			source.append("\nAS\n");
		}
		catch (Exception e)
		{
			source = StrBuffer.EMPTY_BUFFER;
		}
		return source;
	}

}
