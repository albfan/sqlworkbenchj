/*
 * PostgresSequenceReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.postgres;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import workbench.db.SequenceReader;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;

/**
 * @author  support@sql-workbench.net
 */
public class PostgresSequenceReader
	implements SequenceReader
{
	private Connection dbConnection;
	
	public PostgresSequenceReader(Connection conn)
	{
		this.dbConnection = conn;
	}
	
	/**
	 *	Return the source SQL for a PostgreSQL sequence definition.
	 *
	 *	@return The SQL to recreate the sequence if the current DBMS is Postgres. An empty String otherwise
	 */
	public String getSequenceSource(String owner, String aSequence)
	{
		if (aSequence == null) return "";

		int pos = aSequence.indexOf('.');
		if (pos > 0)
		{
			aSequence = aSequence.substring(pos);
		}
		
		String nl = Settings.getInstance().getInternalEditorLineEnding();
		
		Statement stmt = null;
		ResultSet rs = null;
		String result = "";
		try
		{
			String sql = "SELECT sequence_name, max_value, min_value, increment_by, cache_value, is_cycled FROM " + aSequence;
			stmt = this.dbConnection.createStatement();
			rs = stmt.executeQuery(sql);
			if (rs.next())
			{
				String name = rs.getString(1);;
				String max = rs.getString(2);
				long min = rs.getLong(3);
				long inc = rs.getLong(4);
				long cache = rs.getLong(5);
				String cycle = rs.getString(6);

				StringBuffer buf = new StringBuffer(250);
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
				final BigInteger bigMax = new BigInteger("9223372036854775807");
				BigInteger maxV = new BigInteger(max);
				if (!maxV.equals(bigMax))
				{
					buf.append(nl + "       MAXVALUE ");
					buf.append(max);
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
				buf.append(';');
				result = buf.toString();
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("PgSequenceReader.getSequenceSource()", "Error reading sequence definition", e);
			result = "";
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result;
	}

	public List getSequenceList(String owner)
	{
		// Already returned by JDBC driver
		return Collections.EMPTY_LIST;
	}
	
	public DataStore getSequenceDefinition(String owner, String sequence)
	{
		return null;
	}
	
}
