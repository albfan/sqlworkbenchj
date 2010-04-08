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
import workbench.db.JdbcProcedureReader;
import workbench.db.NoConfigException;
import workbench.db.ProcedureDefinition;
import workbench.db.WbConnection;
import workbench.resource.Settings;
import workbench.sql.DelimiterDefinition;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;

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

	@Override
	public DataStore fillProcedureListDataStore(ResultSet rs)
		throws SQLException
	{
		DataStore result = super.fillProcedureListDataStore(rs);
		int count = result.getRowCount();
		for (int i=count-1; i >= 0; i --)
		{
			String type = result.getValueAsString(i, COLUMN_IDX_PROC_COLUMNS_REMARKS);
			if (type == null) continue;
			// Remove object types from the list as they are completely handled in the TableListPanel 
//			if (type.equals("Packaged function"))
//			{
//				result.deleteRow(i);
//			}
		}
		return result;
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
				result.append(nl);
				result.append(delimiter.getDelimiter());
				result.append(nl);
				result.append(nl);
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
	
	
	public void readProcedureSource(ProcedureDefinition def)
		throws NoConfigException
	{
		if (def.isOraclePackage())
		{
			CharSequence source = getPackageSource(def.getSchema(), def.getProcedureName());
			def.setSource(source);
		}
		else if (def.isOracleObjectType())
		{
			OracleObjectType type = new OracleObjectType(def.getSchema(), def.getOracleObjectTypeName());
			CharSequence source = typeReader.getObjectSource(connection, type);
			def.setSource(source);
		}
		else if (def.getCatalog() != null)
		{
			// Fallback in case the definition was not initialized correctly.
			CharSequence source = getPackageSource(def.getSchema(), def.getCatalog());
			def.setSource(source);
			def.setOraclePackage(true);
		}
		else
		{
			super.readProcedureSource(def);
		}
	}		
}
