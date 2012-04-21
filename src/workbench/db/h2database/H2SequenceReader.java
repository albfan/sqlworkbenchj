/*
 * H2SequenceReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.h2database;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import workbench.db.SequenceDefinition;
import workbench.db.SequenceReader;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * SequenceReader for <a href="http://www.h2database.com">H2 Database</a>
 *
 * @author  Thomas Kellerer
 */
public class H2SequenceReader
	implements SequenceReader
{
	private WbConnection dbConnection;

	public H2SequenceReader(WbConnection conn)
	{
		this.dbConnection = conn;
	}

	/**
	 *	Return the source SQL for a H2 sequence definition.
	 *
	 *	@return The SQL to recreate the given sequence
	 */
	@Override
	public CharSequence getSequenceSource(String catalog, String owner, String aSequence)
	{
		SequenceDefinition def = getSequenceDefinition(catalog, owner, aSequence);
		if (def == null) return "";
		return def.getSource();
	}

	@Override
	public List<SequenceDefinition> getSequences(String catalog, String owner, String namePattern)
	{
		DataStore ds = getRawSequenceDefinition(catalog, owner, namePattern);
		if (ds == null) return Collections.emptyList();
		List<SequenceDefinition> result = new ArrayList<SequenceDefinition>();

		for (int row=0; row < ds.getRowCount(); row++)
		{
			result.add(createSequenceDefinition(ds, row));
		}
		return result;
	}

	@Override
	public SequenceDefinition getSequenceDefinition(String catalog, String owner, String sequence)
	{
    DataStore ds = getRawSequenceDefinition(catalog, owner, sequence);
    if (ds == null || ds.getRowCount() == 0) return null;

		return createSequenceDefinition(ds, 0);
	}

	private SequenceDefinition createSequenceDefinition(DataStore ds, int row)
	{
		SequenceDefinition result = null;

    if (ds == null || ds.getRowCount() == 0) return null;

		String name = ds.getValueAsString(row, "SEQUENCE_NAME");
		String schema = ds.getValueAsString(row, "SEQUENCE_SCHEMA");
		result = new SequenceDefinition(schema, name);

		result.setSequenceProperty("CURRENT_VALUE", ds.getValue(row, "CURRENT_VALUE"));
		result.setSequenceProperty("INCREMENT", ds.getValue(row, "INCREMENT"));
		result.setSequenceProperty("IS_GENERATED", ds.getValue(row, "IS_GENERATED"));
		String comment = ds.getValueAsString(row, "REMARKS");
		result.setComment(comment);
		result.setSequenceProperty("CACHE", ds.getValue(row, "CACHE"));
		readSequenceSource(result);

		return result;
	}

	@Override
	public void readSequenceSource(SequenceDefinition def)
	{
		if (def == null) return;

		StringBuilder result = new StringBuilder(100);
		String nl = Settings.getInstance().getInternalEditorLineEnding();

    result.append("CREATE SEQUENCE ");
    result.append(def.getSequenceName());

		Long inc = (Long)def.getSequenceProperty("INCREMENT");
    if (inc != null && inc != 1)
    {
      result.append("\n       INCREMENT BY ");
      result.append(inc);
    }

    result.append("\n       CACHE ");
		result.append(def.getSequenceProperty("CACHE").toString());

		result.append(';');
		result.append(nl);

		String comments = def.getComment();
		if (StringUtil.isNonBlank(comments))
		{
			result.append("\nCOMMENT ON SEQUENCE ");
			result.append(def.getSequenceName());
			result.append(" IS '");
			result.append(comments.replace("'", "''"));
			result.append("';");
		}

		def.setSource(result);
	}

	@Override
	public DataStore getRawSequenceDefinition(String catalog, String owner, String sequence)
	{
		Statement stmt = null;
		ResultSet rs = null;
		DataStore ds = null;
		try
		{
			StringBuilder sql = new StringBuilder(100);
			sql.append("SELECT SEQUENCE_CATALOG, " +
				"SEQUENCE_SCHEMA, " +
				"SEQUENCE_NAME, " +
				"CURRENT_VALUE, " +
				"INCREMENT, " +
				"IS_GENERATED, " +
				"REMARKS, " +
				"ID," +
				"CACHE " +
				"FROM information_schema.sequences ");

			boolean whereAdded = false;

			if (StringUtil.isNonBlank(owner))
			{
				if (!whereAdded)
				{
					sql.append(" WHERE ");
					whereAdded = true;
				}
				else
				{
					sql.append(" AND ");
				}
				sql.append(" sequence_schema = '" + owner + "'");
			}

			if (StringUtil.isNonBlank(sequence))
			{
				if (!whereAdded)
				{
					sql.append(" WHERE ");
					whereAdded = true;
				}
				else
				{
					sql.append(" AND ");
				}
				SqlUtil.appendExpression(sql, "sequence_name", sequence, dbConnection);
			}

			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logInfo("H2SequenceReader.getRawSequenceDefinition()", "Using query=\n" + sql);
			}
			stmt = this.dbConnection.createStatement();
			rs = stmt.executeQuery(sql.toString());
			ds = new DataStore(rs, true);
		}
		catch (Exception e)
		{
			LogMgr.logError("H2SequenceReader.getSequenceDefinition()", "Error reading sequence definition", e);
			ds = null;
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return ds;
	}
}
