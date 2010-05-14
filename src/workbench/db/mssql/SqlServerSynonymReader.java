/*
 * SqlServerSynonymReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.mssql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import workbench.db.DbMetadata;
import workbench.db.JdbcUtils;
import workbench.db.SynonymReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * Retrieve synonyms and their definition from SQL Server
 *
 * @author Thomas Kellerer
 */
public class SqlServerSynonymReader
	implements SynonymReader
{
	private DbMetadata meta;

	private final String baseSql =
		"SELECT syn.name, syn.base_object_name \n" +
    " FROM sys.synonyms syn JOIN sys.schemas sc ON syn.schema_id = sc.schema_id  ";

	public SqlServerSynonymReader(DbMetadata dbMeta)
	{
		this.meta = dbMeta;
	}

	public static boolean supportsSynonyms(WbConnection con)
	{
		return JdbcUtils.hasMinimumServerVersion(con, "9.0");
	}

	@Override
	public List<String> getSynonymList(WbConnection con, String owner, String namePattern)
		throws SQLException
	{
		List<String> result = new LinkedList<String>();
		String sql = baseSql;

		int schemaIndex = -1;
		int nameIndex = -1;

		boolean whereAdded = false;

		if (StringUtil.isNonBlank(owner))
		{
			sql += " where sc.name = ?";
			whereAdded = true;
			schemaIndex = 1;
		}

		if (StringUtil.isNonBlank(namePattern))
		{
			if (whereAdded)
			{
				sql += " AND ";
			}
			else
			{
				sql += " WHERE ";
			}
			if (namePattern.indexOf('%') > -1)
			{
				sql += " syn.name LIKE ?";
			}
			else
			{
				sql += " syn.name = ? ";
			}
			if (schemaIndex == 1) nameIndex = 2;
			else nameIndex = 1;
		}

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo(getClass().getName() + ".getSynonymList()", "Using SQL: " + sql);
		}

		PreparedStatement stmt = null;
		ResultSet rs = null;
		try
		{
			stmt = con.getSqlConnection().prepareStatement(sql);
			if (schemaIndex != -1) stmt.setString(schemaIndex, owner);
			if (nameIndex != -1) stmt.setString(nameIndex, namePattern);

			rs = stmt.executeQuery();
			while (rs.next())
			{
				String syn = rs.getString(1);
				if (!rs.wasNull())
				{
					result.add(syn);
				}
			}
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}

		return result;
	}

	public TableIdentifier getSynonymTable(WbConnection con, String schema, String synonymName)
		throws SQLException
	{
		String sql = baseSql + " where syn.name = ? and sc.name = ?";

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo(getClass().getName() + ".getSynonymTable()", "Using SQL: " + sql);
		}

		PreparedStatement stmt = con.getSqlConnection().prepareStatement(sql);
		stmt.setString(1, synonymName);
		stmt.setString(2, schema);
		ResultSet rs = stmt.executeQuery();
		String table = null;
		TableIdentifier result = null;
		try
		{
			if (rs.next())
			{
				table = rs.getString(2);
				if (table != null)
				{
					result = new TableIdentifier(meta.removeQuotes(table));
				}
			}
		}
		finally
		{
			SqlUtil.closeAll(rs,stmt);
		}
		if (result == null) return null;

		result.setSchema(schema);
		TableIdentifier tbl = meta.findObject(result);
		return tbl;
	}

	public String getSynonymSource(WbConnection con, String anOwner, String aSynonym)
		throws SQLException
	{
		TableIdentifier id = getSynonymTable(con, anOwner, aSynonym);
		if (id == null) return "";

		StringBuilder result = new StringBuilder(200);
		String nl = Settings.getInstance().getInternalEditorLineEnding();
		result.append("CREATE SYNONYM ");
		result.append(aSynonym);
		result.append(nl + "       FOR ");
		result.append(id.getTableExpression());
		result.append(';');
		result.append(nl);

		return result.toString();
	}

}
