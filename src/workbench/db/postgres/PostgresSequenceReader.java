/*
 * PostgresSequenceReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
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
import java.util.List;
import workbench.db.SequenceDefinition;
import workbench.db.SequenceReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.util.SqlUtil;
import workbench.storage.DataStore;
import workbench.util.StringUtil;

/**
 * @author  Thomas Kellerer
 */
public class PostgresSequenceReader
	implements SequenceReader
{
	private WbConnection dbConnection;
	private final String baseSql = "SELECT min_value, max_value, increment_by, cache_value, is_cycled FROM ";

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
			Boolean cycle = (Boolean) def.getSequenceProperty("CYCLE");
			if (cycle == null) cycle = Boolean.FALSE;

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
			if (!cycle.booleanValue())
			{
				buf.append("NO");
			}
			buf.append(" CYCLE");
			buf.append(";\n");

			if (StringUtil.isNonBlank(def.getComment()))
			{
				buf.append('\n');
				buf.append("COMMENT ON SEQUENCE " + def.getSequenceName() + " IS '" + def.getComment().replace("'", "''") + "';");
			}

			String col = def.getRelatedColumn();
			TableIdentifier tbl = def.getRelatedTable();
			if (tbl != null && StringUtil.isNonBlank(col))
			{
				buf.append('\n');
				buf.append("ALTER SEQUENCE ");
				buf.append(def.getSequenceName());
				buf.append(" OWNER TO ");
				buf.append(tbl.getTableName());
				buf.append('.');
				buf.append(col);
				buf.append(';');
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("PgSequenceReader.getSequenceSource()", "Error reading sequence definition", e);
		}

		def.setSource(buf);
	}

	private void readRelatedTable(SequenceDefinition def)
	{
		if (def == null) return;
		String basesql = "SELECT s.relname as sequence_name,  \n" +
             "       n.nspname as sequence_schema,  \n" +
             "       t.relname as related_table, " +
						 "       a.attname as related_column \n" +
             "  FROM pg_class s, pg_depend d, pg_class t, pg_attribute a, pg_namespace n \n" +
             "  WHERE s.relkind     = 'S' \n" +
             "    AND n.oid         = s.relnamespace \n" +
             "    AND d.objid       = s.oid \n" +
             "    AND d.refobjid    = t.oid \n" +
             "    AND (d.refobjid, d.refobjsubid) = (a.attrelid, a.attnum)";
	  String sql = "SELECT * FROM ( " + basesql + ") t \n" +
			"WHERE sequence_name = ?" ;

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		Savepoint sp = null;

		try
		{
			if (def.getSchema() != null)
			{
				sql += " AND sequence_schema = ?";
			}
			pstmt = dbConnection.getSqlConnection().prepareStatement(sql);
			pstmt.setString(1, def.getObjectName());
			if (def.getSchema() != null)
			{
				pstmt.setString(2, def.getSchema());
			}
			rs = pstmt.executeQuery();
			if (rs.next())
			{
				String tbl = rs.getString(3);
				String col = rs.getString(4);
				if (StringUtil.isNonBlank(tbl) && StringUtil.isNonBlank(col))
				{
					def.setRelatedTable(new TableIdentifier(def.getSchema(), tbl), col);
				}
			}
			dbConnection.releaseSavepoint(sp);
		}
		catch (SQLException e)
		{
			dbConnection.rollback(sp);
			LogMgr.logError("PostgresSequenceReader.getOwnedByClause()", "Error retrieving sequence column", e);
		}
		return;
	}
	/**
	 *	Return the source SQL for a PostgreSQL sequence definition.
	 *
	 *	@return The SQL to recreate the given sequence
	 */
	public CharSequence getSequenceSource(String catalog, String owner, String aSequence)
	{
		SequenceDefinition def = getSequenceDefinition(catalog, owner, aSequence);
		return def.getSource();
	}

	/**
	 * Retrieve the list of full SequenceDefinitions from the database.
	 */
	public List<SequenceDefinition> getSequences(String catalog, String owner, String namePattern)
	{
		List<SequenceDefinition> result = new ArrayList<SequenceDefinition>();

		ResultSet rs = null;
		PreparedStatement stmt = null;
		Savepoint sp = null;
		if (namePattern == null) namePattern = "%";

		try
		{
			sp = this.dbConnection.setSavepoint();
			DatabaseMetaData meta = this.dbConnection.getSqlConnection().getMetaData();
			rs = meta.getTables(null, owner, namePattern, new String[] { "SEQUENCE"} );
			while (rs.next())
			{
				String name = rs.getString("TABLE_NAME");
				String schema = rs.getString("TABLE_SCHEM");
				result.add(getSequenceDefinition(catalog, schema, name));
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

	private SequenceDefinition createDefinition(String name, String schema, String comment, DataStore ds)
		throws SQLException
	{
		SequenceDefinition def = new SequenceDefinition(schema, name);
		def.setSequenceProperty("INCREMENT", ds.getValue(0, "increment_by"));
		def.setSequenceProperty("MAXVALUE", ds.getValue(0, "max_value"));
		def.setSequenceProperty("MINVALUE", ds.getValue(0, "min_value"));
		def.setSequenceProperty("CACHE", ds.getValue(0, "cache_value"));
		def.setSequenceProperty("CYCLE", ds.getValue(0, "is_cycled"));
		def.setSequenceProperty("REMARKS", comment);
		def.setComment(comment);
		return def;
	}

	private SequenceDefinition getSequence(String owner, String sequence)
	{
		SequenceDefinition result = null;

		ResultSet rs = null;
		Savepoint sp = null;

		try
		{
			sp = this.dbConnection.setSavepoint();
			DatabaseMetaData meta = this.dbConnection.getSqlConnection().getMetaData();
			rs = meta.getTables(null, owner, sequence, new String[] { "SEQUENCE"} );
			boolean exists = false;
			String comment = null;
			if (rs.next())
			{
				comment = rs.getString("REMARKS");
				exists = true;
			}
			this.dbConnection.releaseSavepoint(sp);
			if (exists)
			{
				DataStore ds = getRawSequenceDefinition(null, owner, sequence);
				result = createDefinition(sequence, owner, comment, ds);
				readRelatedTable(result);
				readSequenceSource(result); // should be called after readRelatedTable() !!
			}
		}
		catch (SQLException e)
		{
			this.dbConnection.rollback(sp);
			LogMgr.logError("PostgresSequenceReader.getSequence()", "Error retrieving sequences", e);
			return null;
		}
		finally
		{
			SqlUtil.closeResult(rs);
		}

		return result;
	}

	private void updateProperties(SequenceDefinition def)
	{
		if (def == null) return;

		DataStore ds = getRawSequenceDefinition(null, def.getSequenceOwner(), def.getSequenceName());
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

	public SequenceDefinition getSequenceDefinition(String catalog, String owner, String sequence)
	{
		return getSequence(owner, sequence);
	}

	public DataStore getRawSequenceDefinition(String catalog, String owner, String sequence)
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
			String sql = baseSql + sequence;
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
