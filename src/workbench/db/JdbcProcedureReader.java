/*
 * JdbcProcedureReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
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
 * @author  support@sql-workbench.net
 */
public class JdbcProcedureReader
	implements ProcedureReader
{
	protected DbMetadata dbMeta;
	
	public JdbcProcedureReader(DbMetadata meta)
	{
		this.dbMeta = meta;
	}
	
	public StrBuffer getProcedureHeader(String catalog, String schema, String procName, int procType)
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
		aSchema = this.dbMeta.adjustObjectnameCase(aSchema);
		aCatalog = this.dbMeta.adjustObjectnameCase(aCatalog);
		
		ResultSet rs = this.dbMeta.getSqlConnection().getMetaData().getProcedures(aCatalog, aSchema, "%");
		return fillProcedureListDataStore(rs);
	}

	public static DataStore buildProcedureListDataStore(DbMetadata meta)
	{
		String[] cols = new String[] {"PROCEDURE_NAME", "TYPE", meta.getCatalogTerm().toUpperCase(), meta.getSchemaTerm().toUpperCase(), "REMARKS"};
		final int types[] = {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR};
		final int sizes[] = {30,12,10,10,20};

		DataStore ds = new DataStore(cols, types, sizes);
		return ds;
	}
	
	public DataStore fillProcedureListDataStore(ResultSet rs)
		throws SQLException
	{
		DataStore ds = buildProcedureListDataStore(this.dbMeta);
		
		String versionDelimiter = dbMeta.getProcVersionDelimiter();
		boolean stripVersion = dbMeta.getStripProcedureVersion();
		try
		{
			while (rs.next())
			{
				String cat = rs.getString("PROCEDURE_CAT");
				String schema = rs.getString("PROCEDURE_SCHEM");
				String name = rs.getString("PROCEDURE_NAME");
				String remark = rs.getString("REMARKS");
				short type = rs.getShort("PROCEDURE_TYPE");
				Integer iType = null;
				if (rs.wasNull())
				{
					//sType = "N/A";
					iType = new Integer(DatabaseMetaData.procedureResultUnknown);
				}
				else
				{
					iType = new Integer(type);
				}
				int row = ds.addRow();


				if (stripVersion)
				{
					int pos = name.lastIndexOf(versionDelimiter);
					if (pos > 1)
					{
						name = name.substring(0,pos);
					}
				}
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_CATALOG, cat);
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_SCHEMA, schema);
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_NAME, name);
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_TYPE, iType);
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
	
	public static String convertProcType(int type)
	{
		if (type == DatabaseMetaData.procedureNoResult)
			return ProcedureReader.PROC_RESULT_NO;
		else if (type == DatabaseMetaData.procedureReturnsResult)
			return ProcedureReader.PROC_RESULT_YES;
		else
			return ProcedureReader.PROC_RESULT_UNKNOWN;
	}
	
	public DataStore getProcedureColumns(String aCatalog, String aSchema, String aProcname)
		throws SQLException
	{
		final String cols[] = {"COLUMN_NAME", "TYPE", "TYPE_NAME", "REMARKS"};
		final int types[] =   {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR};
		final int sizes[] =   {20, 10, 18, 30};
		DataStore ds = new DataStore(cols, types, sizes);

		ResultSet rs = null;
		try
		{
			rs = this.dbMeta.getSqlConnection().getMetaData().getProcedureColumns(aCatalog, aSchema, dbMeta.adjustObjectnameCase(aProcname), "%");
			while (rs.next())
			{
				int row = ds.addRow();
				String colName = rs.getString("COLUMN_NAME");
				ds.setValue(row, 0, colName);
				int colType = rs.getInt("COLUMN_TYPE");
				String stype;

				if (colType == DatabaseMetaData.procedureColumnIn)
					stype = "IN";
				else if (colType == DatabaseMetaData.procedureColumnInOut)
					stype = "INOUT";
				else if (colType == DatabaseMetaData.procedureColumnOut)
					stype = "OUT";
				else if (colType == DatabaseMetaData.procedureColumnResult)
					stype = "RESULTSET";
				else if (colType == DatabaseMetaData.procedureColumnReturn)
					stype = "RETURN";
				else
					stype = "";
				ds.setValue(row, 1, stype);

				int sqlType = rs.getInt("DATA_TYPE");
				String typeName = rs.getString("TYPE_NAME");
				int digits = rs.getInt("PRECISION");
				int size = rs.getInt("LENGTH");
				String rem = rs.getString("REMARKS");

				String display = DbMetadata.getSqlTypeDisplay(typeName, sqlType, size, digits);
				ds.setValue(row, 2, display);
				ds.setValue(row, 3, rem);
			}
		}
		finally
		{
			SqlUtil.closeResult(rs);
		}

		return ds;
	}

}
