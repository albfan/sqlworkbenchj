/*
 * PgSequenceReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2004, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.db.postgres;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import workbench.db.SequenceReader;
import workbench.log.LogMgr;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;
import workbench.util.StrBuffer;

/**
 * @author  info@sql-workbench.net
 */
public class PgSequenceReader
	implements SequenceReader
{
	private Connection dbConnection;
	
	public PgSequenceReader(Connection conn)
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
				long maxValue = rs.getLong(2);
				long minValue = rs.getLong(3);
				String max = Long.toString(maxValue);
				String min = Long.toString(minValue);
				String inc = rs.getString(4);
				String cache = rs.getString(5);
				String cycle = rs.getString(6);

				StrBuffer buf = new StrBuffer(250);
				buf.append("CREATE SEQUENCE ");
				buf.append(name);
				buf.append(" INCREMENT ");
				buf.append(inc);
				buf.append(" MINVALUE ");
				buf.append(min);
				buf.append(" MAXVALUE ");
				buf.append(max);
				buf.append(" CACHE ");
				buf.append(cache);
				if ("true".equalsIgnoreCase(cycle))
				{
					buf.append(" CYCLE");
				}
				buf.append(";");
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
		return Collections.EMPTY_LIST;
	}
	
	public DataStore getSequenceDefinition(String owner, String sequence)
	{
		return null;
	}
	
}
