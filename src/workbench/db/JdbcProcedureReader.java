/*
 * JdbcProcedureReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import workbench.log.LogMgr;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;
import workbench.util.StrBuffer;

/**
 * @author  info@sql-workbench.net
 */
public class JdbcProcedureReader
	implements ProcedureReader
{
	private DbMetadata dbMeta;
	public JdbcProcedureReader(DbMetadata meta)
	{
		this.dbMeta = meta;
	}
	
	public StrBuffer getProcedureHeader(String catalog, String schema, String procName)
	{
		return StrBuffer.EMPTY_BUFFER;
	}
	
	public DataStore getProcedures(String aCatalog, String aSchema)
		throws SQLException
	{
		if ("*".equals(aSchema) || "%".equals(aSchema))
		{
			aSchema = null;
		}
		ResultSet rs = this.dbMeta.metaData.getProcedures(aCatalog, aSchema, "%");
		return buildProcedureListDataStore(rs);
	}
	
	public DataStore buildProcedureListDataStore(ResultSet rs)
		throws SQLException
	{
		String[] cols = new String[] {"PROCEDURE_NAME", "TYPE", this.dbMeta.getCatalogTerm().toUpperCase(), this.dbMeta.getSchemaTerm().toUpperCase(), "REMARKS"};
		final int types[] = {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR};
		final int sizes[] = {30,12,10,10,20};

		DataStore ds = new DataStore(cols, types, sizes);

		try
		{
			String sType;
			while (rs.next())
			{
				String cat = rs.getString("PROCEDURE_CAT");
				String schema = rs.getString("PROCEDURE_SCHEM");
				String name = rs.getString("PROCEDURE_NAME");
				String remark = rs.getString("REMARKS");
				short type = rs.getShort("PROCEDURE_TYPE");
				if (rs.wasNull())
				{
					sType = "N/A";
				}
				else
				{
					if (type == DatabaseMetaData.procedureNoResult)
						sType = ProcedureReader.PROC_RESULT_NO;
					else if (type == DatabaseMetaData.procedureReturnsResult)
						sType = ProcedureReader.PROC_RESULT_YES;
					else
						sType = ProcedureReader.PROC_RESULT_UNKNOWN;
				}
				int row = ds.addRow();


				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_CATALOG, cat);
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_SCHEMA, schema);
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_NAME, name);
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_TYPE, sType);
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_REMARKS, remark);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("DbMetadata.getProcedures()", "Error while retrieving procedures", e);
		}
		finally
		{
			SqlUtil.closeResult(rs);
		}
		return ds;
	}
	
}
