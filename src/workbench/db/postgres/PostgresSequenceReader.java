/*
 * PostgresSequenceReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
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
import workbench.util.StringUtil;

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

			if (StringUtil.isNonBlank(def.getComment()))
			{
				buf.append("\n");
				buf.append("COMMENT ON SEQUENCE " + def.getSequenceName() + " IS '" + def.getComment().replace("'", "''") + "';");
			}
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
				result.add(createDefinition(rs));
			}
			this.dbConnection.releaseSavepoint(sp);

			for (SequenceDefinition def : result)
			{
				updateProperties(def);
			}
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

	private SequenceDefinition createDefinition(ResultSet rs)
		throws SQLException
	{
		String seq_name = rs.getString("TABLE_NAME");
		String schema = rs.getString("TABLE_SCHEM");
		String comment = rs.getString("REMARKS");
		SequenceDefinition def = new SequenceDefinition(schema, seq_name);
		def.setSequenceProperty("SEQUENCE_NAME", seq_name);
		def.setSequenceProperty("SEQUENCE_SCHEMA", schema);
		def.setSequenceProperty("INCREMENT", rs.getObject("increment_by"));
		def.setSequenceProperty("MAXVALUE", rs.getObject("max_value"));
		def.setSequenceProperty("MINVALUE", rs.getObject("min_value"));
		def.setSequenceProperty("CACHE", rs.getObject("cache_value"));
		def.setSequenceProperty("CYCLE", rs.getObject("is_cycle"));
		def.setSequenceProperty("REMARKS", comment);

		def.setComment(comment);
		return def;
	}

	private SequenceDefinition getSequence(String owner, String sequence)
	{
		SequenceDefinition result = null;

		ResultSet rs = null;
		PreparedStatement stmt = null;
		Savepoint sp = null;
		try
		{
			sp = this.dbConnection.setSavepoint();
			DatabaseMetaData meta = this.dbConnection.getSqlConnection().getMetaData();
			rs = meta.getTables(null, owner, sequence, new String[] { "SEQUENCE"} );
			while (rs.next())
			{
				result = createDefinition(rs);
			}
			this.dbConnection.releaseSavepoint(sp);

			updateProperties(result);
		}
		catch (SQLException e)
		{
			this.dbConnection.rollback(sp);
			LogMgr.logError("PostgresSequenceReader.getSequence()", "Error retrieving sequences", e);
			return null;
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result;
	}

	private void updateProperties(SequenceDefinition def)
	{
		DataStore ds = getRawSequenceDefinition(def.getSequenceOwner(), def.getSequenceName());
		if (ds == null) return;
		if (ds.getRowCount() == 0) return;

		long min = ds.getValueAsLong(0, 0, -1);
		long max = ds.getValueAsLong(0, 1, -1);
		long inc = ds.getValueAsLong(0, 2, 1);
		long cache = ds.getValueAsLong(0, 3, 1);
		String cycle = ds.getValueAsString(0, 4);

		def.setSequenceProperty("INCREMENT", Long.valueOf(inc));
		def.setSequenceProperty("MINVALUE", Long.valueOf(min));
		def.setSequenceProperty("CACHE", cache);
		def.setSequenceProperty("CYCLE", cycle);
		def.setSequenceProperty("MAXVALUE", Long.valueOf(max));
		readSequenceSource(def);
	}

	public SequenceDefinition getSequenceDefinition(String owner, String sequence)
	{
		SequenceDefinition result = getSequence(owner, sequence);
		updateProperties(result);
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
