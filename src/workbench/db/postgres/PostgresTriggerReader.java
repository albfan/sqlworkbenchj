/*
 * PostgresTriggerReader
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2011, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.postgres;

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import workbench.db.DefaultTriggerReader;
import workbench.db.NoConfigException;
import workbench.db.ProcedureDefinition;
import workbench.db.ProcedureReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresTriggerReader
	extends DefaultTriggerReader
{
	public PostgresTriggerReader(WbConnection conn)
	{
		super(conn);

	}

	@Override
	public CharSequence getDependentSource(String triggerCatalog, String triggerSchema, String triggerName, TableIdentifier triggerTable)
		throws SQLException
	{
		PreparedStatement stmt = null;
		ResultSet rs = null;
		String funcName = null;
		String funcSchema = null;
		StringBuilder result = null;

		try
		{
			final String sql =
				"SELECT trgsch.nspname as function_schema, proc.proname as function_name \n" +
				"FROM pg_trigger trg  \n" +
				"  JOIN pg_class tbl ON tbl.oid = trg.tgrelid  \n" +
				"  JOIN pg_proc proc ON proc.oid = trg.tgfoid \n" +
				"  JOIN pg_namespace trgsch ON trgsch.oid = proc.pronamespace \n" +
				"  JOIN pg_namespace tblsch ON tblsch.oid = tbl.relnamespace \n";

			StringBuilder query = new StringBuilder(sql.length() + 50);
			query.append(sql);
			query.append("WHERE trg.tgname = ? \n");
			query.append("  AND tblsch.nspname = ? ");

			stmt = dbConnection.getSqlConnection().prepareStatement(query.toString());
			stmt.setString(1, triggerName);
			stmt.setString(2, triggerTable.getSchema());
			rs = stmt.executeQuery();
			if (rs.next())
			{
				funcSchema = rs.getString(1);
				funcName = rs.getString(2);
			}
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}

		if (funcName != null && funcSchema != null)
		{
			ProcedureReader reader = dbMeta.getProcedureReader();
			ProcedureDefinition def = new ProcedureDefinition(null, funcSchema, funcName, DatabaseMetaData.procedureResultUnknown);
			try
			{
				reader.readProcedureSource(def);
				CharSequence src = def.getSource();
				if (src != null)
				{
					result = new StringBuilder(src.length() + 50);
					result.append("---[ ");
					result.append(funcName);
					result.append(" ]---\n");
					result.append(src);
					result.append('\n');
				}
			}
			catch (NoConfigException cfg)
			{
				// nothing to do
			}
		}
		return result;
	}

}
