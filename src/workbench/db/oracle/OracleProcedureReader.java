/*
 * OracleProcedureReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.oracle;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import workbench.db.DbMetadata;
import workbench.db.JdbcProcedureReader;
import workbench.db.NoConfigException;
import workbench.db.ProcedureDefinition;
import workbench.db.WbConnection;
import workbench.resource.Settings;
import workbench.sql.DelimiterDefinition;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A ProcedureReader to read the source of an Oracle procedure.
 * Packages are handled properly. The Oracle JDBC driver
 * reports the package name in the catalog column
 * of the getProcedures() ResultSet.
 * The method {@link #readProcedureSource(ProcedureDefinition)} the
 * catalog definition of the ProcedureDefinition is checked. If it's not
 * null it is assumed that this the definition is actually a package.
 *
 * @see workbench.db.JdbcProcedureReader
 * @author Thomas Kellerer
 */
public class OracleProcedureReader
	extends JdbcProcedureReader
{
	private OracleTypeReader typeReader = new OracleTypeReader();

	public OracleProcedureReader(WbConnection conn)
	{
		super(conn);
	}

	private final StringBuilder PROC_HEADER = new StringBuilder("CREATE OR REPLACE ");

	public StringBuilder getProcedureHeader(String catalog, String schema, String procname, int procType)
	{
		return PROC_HEADER;
	}

	public CharSequence getPackageSource(String owner, String packageName)
	{
		final String sql = "SELECT text \n" +
			"FROM all_source \n" +
			"WHERE name = ? \n" +
			"AND   owner = ? \n" +
			"AND   type = ? \n" +
			"ORDER BY line";

		StringBuilder result = new StringBuilder(1000);
		PreparedStatement stmt = null;
		ResultSet rs = null;

		String nl = Settings.getInstance().getInternalEditorLineEnding();
		DelimiterDefinition delimiter = Settings.getInstance().getAlternateDelimiter(connection);

		try
		{
			int lineCount = 0;

			synchronized (connection)
			{
				stmt = this.connection.getSqlConnection().prepareStatement(sql);
				stmt.setString(1, packageName);
				stmt.setString(2, owner);
				stmt.setString(3, "PACKAGE");
				rs = stmt.executeQuery();
				while (rs.next())
				{
					String line = rs.getString(1);
					if (line != null)
					{
						lineCount ++;
						if (lineCount == 1)
						{
							result.append("CREATE OR REPLACE ");
						}
						result.append(line);
					}
				}
				if (lineCount > 0)
				{
					result.append(nl);
					result.append(delimiter.getDelimiter());
					result.append(nl);
					result.append(nl);
				}
				lineCount = 0;

				stmt.clearParameters();
				stmt.setString(1, packageName);
				stmt.setString(2, owner);
				stmt.setString(3, "PACKAGE BODY");
				rs = stmt.executeQuery();
				while (rs.next())
				{
					String line = rs.getString(1);
					if (line != null)
					{
						lineCount ++;
						if (lineCount == 1)
						{
							result.append("CREATE OR REPLACE ");
						}
						result.append(line);
					}
				}
			}
			result.append(nl);
			if (lineCount > 0)
			{
				result.append(delimiter.getDelimiter());
				result.append(nl);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result;
	}

	@Override
	public DataStore buildProcedureListDataStore(DbMetadata meta, boolean addSpecificName)
	{
		DataStore ds = super.buildProcedureListDataStore(meta, addSpecificName);
		ds.getResultInfo().getColumn(COLUMN_IDX_PROC_LIST_CATALOG).setColumnName("PACKAGE");
		return ds;
	}

	@Override
	public DataStore getProcedureColumns(String aCatalog, String aSchema, String aProcname)
		throws SQLException
	{
		DataStore result = super.getProcedureColumns(aCatalog, aSchema, aProcname);

		// Remove the implicit parameter for Object type functions that passes
		// the instance of that object to the function
		for (int row = result.getRowCount() - 1; row >= 0; row --)
		{
			String colname = result.getValueAsString(row, COLUMN_IDX_PROC_COLUMNS_COL_NAME);
			int type = result.getValueAsInt(row, COLUMN_IDX_PROC_COLUMNS_JDBC_DATA_TYPE, Types.OTHER);
			if ("SELF".equals(colname) && type == Types.OTHER)
			{
				result.deleteRow(row);
			}
		}
		return result;
	}


	@Override
	public DataStore getProcedures(String catalog, String schema, String name)
		throws SQLException
	{
		if (!Settings.getInstance().getBoolProperty("workbench.db.oracle.procedures.custom_sql", true))
		{
			return super.getProcedures(catalog, schema, name);
		}

		String standardProcs = "select null as procedure_cat,  \n" +
             "       ap.owner as procedure_schem,  \n" +
             "       ap.object_name as procedure_name, \n" +
						 "       null, \n" +
						 "       null, \n" +
						 "       null, \n" +
             "       null as remarks, \n" +
             "       decode(ao.object_type, 'PROCEDURE', 1, 'FUNCTION', 2, 0) as PROCEDURE_TYPE \n" +
             "from all_procedures ap \n" +
             "  join all_objects ao on ao.object_name = ap.object_name and ao.owner = ap.owner  \n" +
             "where ao.object_type in ('PROCEDURE', 'FUNCTION') ";

		if (StringUtil.isNonBlank(schema))
		{
			standardProcs += " AND ao.owner = '" + schema + "' ";
		}

		if (StringUtil.isBlank(name))
		{
			name = "%";
		}

		standardProcs += " AND ao.object_name LIKE '" + name + "' ";

		String pkgProcs = "select package_name as procedure_cat, \n" +
             "       ao.owner as procedure_schem, \n" +
             "       aa.object_name as procedure_name, \n" +
             "       null, \n" +
             "       null, \n" +
             "       null, \n" +
             "       decode(ao.object_type, 'TYPE', 'OBJECT TYPE', ao.object_type) as remarks, \n" +
             "       decode(aa.in_out, 'IN', 1, 'OUT', 2, 0) as PROCEDURE_TYPE \n" +
             "from all_arguments aa \n" +
             "  join all_objects ao on aa.package_name = ao.object_name and aa.owner = ao.owner and ao.object_type IN ('PACKAGE', 'TYPE') \n" +
             "where aa.owner = user \n" +
             "and aa.package_name IS NOT NULL \n" +
             "and (    (aa.position = 0 and aa.sequence = 1 AND aa.IN_OUT = 'OUT') \n" +
             "      OR (aa.position = 1 and aa.sequence = 1) \n" +
             "      OR (aa.position = 1 and aa.sequence = 0) \n" +
             "    )";

		if (StringUtil.isNonBlank(schema))
		{
			pkgProcs += " AND ao.owner = '" + schema + "' ";
		}

		pkgProcs += " AND aa.object_name LIKE '" + name + "' ";

		String sql = standardProcs + " UNION ALL " + pkgProcs + " ORDER BY 2,3";
		Statement stmt = null;
		try
		{
			stmt = this.connection.createStatementForQuery();
			ResultSet rs = stmt.executeQuery(sql);

			// the result set will be closed by fillProcedureListDataStore()
			DataStore ds = fillProcedureListDataStore(rs);
			ds.resetStatus();
			return ds;
		}
		finally
		{
			SqlUtil.closeStatement(stmt);
		}
	}

	public void readProcedureSource(ProcedureDefinition def)
		throws NoConfigException
	{
		if (def.getPackageName() == null)
		{
			super.readProcedureSource(def);
			return;
		}

		if (def.isOracleObjectType())
		{
			OracleObjectType type = new OracleObjectType(def.getSchema(), def.getPackageName());
			CharSequence source = typeReader.getObjectSource(connection, type);
			def.setSource(source);
		}
		else
		{
			CharSequence source = getPackageSource(def.getSchema(), def.getPackageName());
			if (StringUtil.isBlank(source))
			{
				// Fallback if the ProcedureDefinition was not initialized correctly.
				// This will happen if our custom SQL was not used.
				OracleObjectType type = new OracleObjectType(def.getSchema(), def.getPackageName());
				source = typeReader.getObjectSource(connection, type);
			}
			def.setSource(source);
		}
	}
}
