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
			buf.append("\n       INCREMENT BY ");
			buf.append(inc);
			buf.append("\n       MINVALUE ");
			buf.append(min);
			long maxMarker = 9223372036854775807L;
			if (max != maxMarker)
			{
				buf.append("\n       MAXVALUE ");
				buf.append(max.toString());
			}
			buf.append("\n       CACHE ");
			buf.append(cache);
			buf.append("\n       ");
			if ("false".equalsIgnoreCase(cycle))
			{
				buf.append("NO");
			}
			buf.append(" CYCLE");
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
		SequenceDefinition result = new SequenceDefinition(owner, sequence);
		DataStore ds = getRawSequenceDefinition(owner, sequence);
		if (ds == null) return result;
		if (ds.getRowCount() == 0) return result;
		
		long min = ds.getValueAsLong(0, 0, -1);
		long max = ds.getValueAsLong(0, 1, -1);
		long inc = ds.getValueAsLong(0, 2, 1);
		long cache = ds.getValueAsLong(0, 3, 1);
		String cycle = ds.getValueAsString(0, 4);

		result.setSequenceProperty("INCREMENT", Long.valueOf(inc));
		result.setSequenceProperty("MINVALUE", Long.valueOf(min));
		result.setSequenceProperty("CACHE", cache);
		result.setSequenceProperty("CYCLE", cycle);
		result.setSequenceProperty("MAXVALUE", Long.valueOf(max));
		readSequenceSource(result);
		return result;
	}

	public DataStore getRawSequenceDefinition(String owner, String sequence)
	{
		if (sequence == null) return null;

		int pos = sequence.indexOf('.');
		if (pos > 0)
		{
			sequence = sequence.substring(pos);
		}

		DataStore result = null;
		Statement stmt = null;
		ResultSet rs = null;
		Savepoint sp = null;
		try
		{
			String sql = "SELECT min_value, max_value, increment_by, cache_value, is_cycled FROM " + sequence;
			sp = this.dbConnection.setSavepoint();
			stmt = this.dbConnection.createStatement();
			rs = stmt.executeQuery(sql);
			result = new DataStore(rs, this.dbConnection, true);
			this.dbConnection.releaseSavepoint(sp);
		}
		catch (SQLException e)
		{
			this.dbConnection.rollback(sp);
			LogMgr.logDebug("PgSequenceReader.getSequenceDefinition()", "Error reading sequence definition", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result;
	}
}
