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

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import workbench.db.DbMetadata;
import workbench.db.JdbcProcedureReader;
import workbench.db.JdbcUtils;
import workbench.db.NoConfigException;
import workbench.db.ProcedureDefinition;
import workbench.db.ProcedureReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
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

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("OracleProcedureReader.getPackageSource()", "Using SQL to retrieve package source:\n" + sql.toString());
		}

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

	public ProcedureDefinition resolveSynonym(String catalog, String schema, String procname)
		throws SQLException
	{
		TableIdentifier tbl = connection.getMetadata().getSynonymTable(new TableIdentifier(procname));
		if (tbl == null && catalog != null)
		{
			// maybe a public synonym on the package?
			tbl = connection.getMetadata().getSynonymTable(new TableIdentifier(catalog));
		}
		if (tbl != null)
		{
			schema = tbl.getSchema();
			if (catalog != null)
			{
				// This is a synonym for a package, in this case the "tablename" is the actual package name
				catalog = tbl.getTableName();
			}
			return ProcedureDefinition.createOracleDefinition(schema, procname, catalog, 0, null);
		}
		return null;
	}

	@Override
	public DataStore getProcedureColumns(ProcedureDefinition def)
		throws SQLException
	{
		String overload = def.getOracleOverloadIndex();
		DataStore result = createProcColsDataStore();
		ResultSet rs = null;

		try
		{
			rs = this.connection.getSqlConnection().getMetaData().getProcedureColumns(def.getCatalog(), def.getSchema(), def.getProcedureName(), "%");
			int overloadIndex = JdbcUtils.getColumnIndex(rs, "OVERLOAD");

			while (rs.next())
			{
				if (overload != null && overloadIndex > 0)
				{
					String toTest = rs.getString(overloadIndex);
					if (!StringUtil.equalString(toTest, overload)) continue;
				}
				processProcedureColumnResultRow(result, rs);
			}
		}
		finally
		{
			SqlUtil.closeResult(rs);
		}

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

	private boolean useCustomSql()
	{
		if (connection == null) return false;
		return JdbcUtils.hasMinimumServerVersion(connection, "10.0") && Settings.getInstance().getBoolProperty("workbench.db.oracle.procedures.custom_sql", true);
	}

	@Override
	public DataStore getProcedures(String catalog, String schema, String name)
		throws SQLException
	{
		if (!useCustomSql())
		{
			return super.getProcedures(catalog, schema, name);
		}

		String standardProcs = "select null as package_name,  \n" +
             "       ap.owner as procedure_owner,  \n" +
             "       ap.object_name as procedure_name, \n" +
						 "       null as overload_index, \n" +
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

		String pkgProcs = "select aa.package_name, \n" +
             "       ao.owner as procedure_owner, \n" +
             "       aa.object_name as procedure_name, \n" +
             "       aa.overload as overload_index, \n" +
             "       decode(ao.object_type, 'TYPE', 'OBJECT TYPE', ao.object_type) as remarks, \n" +
             "       decode(aa.in_out, 'IN', 1, 'OUT', 2, 0) as PROCEDURE_TYPE \n" +
             "from all_arguments aa \n" +
             "  join all_objects ao on aa.package_name = ao.object_name and aa.owner = ao.owner and ao.object_type IN ('PACKAGE', 'TYPE') \n" +
             "where aa.owner = ao.owner \n" +
             "and aa.package_name IS NOT NULL \n" +
             "and (    (aa.position = 0 and aa.sequence = 1 AND aa.IN_OUT = 'OUT') \n" +
             "      OR (aa.position = 1 and aa.sequence = 1) \n" +
             "      OR (aa.position = 1 and aa.sequence = 0) \n" +
             "    )";

		if (StringUtil.isNonBlank(schema))
		{
			pkgProcs += "\n AND ao.owner = '" + schema + "' ";
		}

		if (StringUtil.isNonBlank(catalog))
		{
			pkgProcs += "\n AND aa.package_name = '" + catalog + "' ";
		}

		pkgProcs += "\n AND aa.object_name LIKE '" + name + "' ";

		String sql = standardProcs + "\n UNION ALL \n" + pkgProcs + "\n ORDER BY 2,3";

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("OracleProcedureReader.getProcedures()", "Using SQL to retrieve procedures:\n" + sql.toString());
		}

		Statement stmt = null;
		ResultSet rs = null;
		DataStore ds = buildProcedureListDataStore(this.connection.getMetadata(), false);
		try
		{
			stmt = this.connection.createStatementForQuery();
			rs = stmt.executeQuery(sql);

			while (rs.next())
			{
				String packageName = rs.getString("PACKAGE_NAME");
				String owner = rs.getString("PROCEDURE_OWNER");
				String procedureName = rs.getString("PROCEDURE_NAME");
				String remark = rs.getString("REMARKS");
				String overloadIndicator = rs.getString("OVERLOAD_INDEX");
				int type = rs.getInt("PROCEDURE_TYPE");

				Integer iType;
				if (rs.wasNull())
				{
					iType = Integer.valueOf(DatabaseMetaData.procedureResultUnknown);
				}
				else
				{
					iType = Integer.valueOf(type);
				}
				ProcedureDefinition def = ProcedureDefinition.createOracleDefinition(owner, procedureName, packageName, type, remark);
				def.setOracleOverloadIndex(overloadIndicator);
				int row = ds.addRow();
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_CATALOG, packageName);
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_SCHEMA, schema);
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_NAME, procedureName);
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_TYPE, iType);
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_REMARKS, remark);
				ds.getRow(row).setUserObject(def);
			}
			ds.resetStatus();
		}
		catch (Exception e)
		{
			LogMgr.logError("JdbcProcedureReader.getProcedures()", "Error while retrieving procedures", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return ds;
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
