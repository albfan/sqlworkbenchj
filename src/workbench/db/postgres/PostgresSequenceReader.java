/*
 * PostgresSequenceReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.postgres;

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import workbench.db.SequenceDefinition;
import workbench.db.SequenceReader;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.SqlUtil;
import workbench.storage.DataStore;

/**
 * @author  support@sql-workbench.net
 */
public class PostgresSequenceReader
	implements SequenceReader
{
	private WbConnection dbConnection;

	public PostgresSequenceReader(WbConnection conn)
	{
		this.dbConnection = conn;
	}

	public void readSequenceSource(SequenceDefinition def)
	{
		if (def == null) return;
		String nl = Settings.getInstance().getInternalEditorLineEnding();

		StringBuilder buf = new StringBuilder(250);

		try
		{
			String name = def.getSequenceName();
			Long max = (Long) def.getSequenceProperty("MAXVALUE");
			Long min = (Long) def.getSequenceProperty("MINVALUE");
			Long inc = (Long) def.getSequenceProperty("INCREMENT");
			Long cache = (Long) def.getSequenceProperty("CACHE");
			String cycle = (String) def.getSequenceProperty("CYCLE");

			buf.append("CREATE SEQUENCE ");
			buf.append(name);
			if (inc != 1)
			{
				buf.append("\n       INCREMENT ");
				buf.append(inc);
			}
			if (min != 1)
			{
				buf.append(nl + "       MINVALUE ");
				buf.append(min);
			}
			long maxMarker = 9223372036854775807L;
			if (max != maxMarker)
			{
				buf.append(nl + "       MAXVALUE ");
				buf.append(max.toString());
			}
			if (cache != 1)
			{
				buf.append(nl + "        CACHE ");
				buf.append(cache);
			}
			if ("true".equalsIgnoreCase(cycle))
			{
				buf.append(nl + "        CYCLE");
			}
			buf.append(";\n");
		}
		catch (Exception e)
		{
			LogMgr.logError("PgSequenceReader.getSequenceSource()", "Error reading sequence definition", e);
		}

		def.setSource(buf);
	}

	/**
	 *	Return the source SQL for a PostgreSQL sequence definition.
	 *
	 *	@return The SQL to recreate the given sequence
	 */
	public CharSequence getSequenceSource(String owner, String aSequence)
	{
		SequenceDefinition def = getSequenceDefinition(owner, aSequence);
		return def.getSource();
	}

	public List<String> getSequenceList(String owner)
	{
		// Already returned by JDBC driver
		return Collections.emptyList();
	}

	/**
	 * Retrieve the list of full SequenceDefinitions from the database.
	 */
	public List<SequenceDefinition> getSequences(String owner)
	{
		List<SequenceDefinition> result = new ArrayList<SequenceDefinition>();

		ResultSet rs = null;
		PreparedStatement stmt = null;
		Savepoint sp = null;
		try
		{
			sp = this.dbConnection.setSavepoint();
			DatabaseMetaData meta = this.dbConnection.getSqlConnection().getMetaData();
			rs = meta.getTables(null, owner, "%", new String[] { "SEQUENCE"} );
			while (rs.next())
			{
				String seq_name = rs.getString("TABLE_NAME");
				String schema = rs.getString("TABLE_SCHEM");
				SequenceDefinition def = getSequenceDefinition(schema, seq_name);
				result.add(def);
			}
			this.dbConnection.releaseSavepoint(sp);
		}
		catch (SQLException e)
		{
			this.dbConnection.rollback(sp);
			LogMgr.logError("PostgresSequenceReader.getSequences()", "Error retrieving sequences", e);
			return null;
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result;
	}

	public SequenceDefinition getSequenceDefinition(String owner, String sequence)
	{
		if (sequence == null) return null;

		int pos = sequence.indexOf('.');
		if (pos > 0)
		{
			sequence = sequence.substring(pos);
		}

		Statement stmt = null;
		ResultSet rs = null;
		SequenceDefinition result = new SequenceDefinition(owner, sequence);
		Savepoint sp = null;
		try
		{
			String sql = "SELECT max_value, min_value, increment_by, cache_value, is_cycled FROM " + sequence;
			sp = this.dbConnection.setSavepoint();
			stmt = this.dbConnection.createStatement();
			rs = stmt.executeQuery(sql);
			if (rs.next())
			{
  			long max = rs.getLong(1);
				long min = rs.getLong(2);
				long inc = rs.getLong(3);
				long cache = rs.getLong(4);
				String cycle = rs.getString(5);

				result.setSequenceProperty("INCREMENT", Long.valueOf(inc));
				result.setSequenceProperty("MINVALUE", Long.valueOf(min));
				result.setSequenceProperty("CACHE", cache);
				result.setSequenceProperty("CYCLE", cycle);
				result.setSequenceProperty("MAXVALUE", Long.valueOf(max));
				readSequenceSource(result);
			}
			this.dbConnection.releaseSavepoint(sp);
		}
		catch (SQLException e)
		{
			this.dbConnection.rollback(sp);
			LogMgr.logDebug("PgSequenceReader.getSequenceDefinition()", "Error reading sequence definition", e);
			result = null;
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result;
	}

	public DataStore getRawSequenceDefinition(String owner, String sequence)
	{
		// The definition can be displayed by doing a SELECT * FROM sequence
		// so we don't need to build up the datastore here.
		return null;
	}
}
