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
package workbench.db.h2database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import workbench.db.SequenceReader;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;

/**
 * @author  support@sql-workbench.net
 */
public class H2SequenceReader
	implements SequenceReader
{
	private Connection dbConnection;
	
	public H2SequenceReader(Connection conn)
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
		String result = null;
		String nl = Settings.getInstance().getInternalEditorLineEnding();
		try
		{
			
			String sql = "SELECT name,increment FROM information_schema.sequences WHERE name = '" + aSequence + "'";
			stmt = this.dbConnection.createStatement();
			rs = stmt.executeQuery(sql);
			if (rs.next())
			{
				String name = rs.getString(1);
				long inc = rs.getLong(2);
				
				StringBuffer buf = new StringBuffer(250);
				buf.append("DROP SEQUENCE " + name + " IF EXISTS;");
				buf.append(nl);
				buf.append("CREATE SEQUENCE ");
				buf.append(name);
				if (inc != 1)
				{
					buf.append(" INCREMENT BY ");
					buf.append(inc);
				}
				buf.append(';');
				result = buf.toString();
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("H2SequenceReader.getSequenceSource()", "Error reading sequence definition", e);
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
		List result = new LinkedList();
		ResultSet rs = null;
		Statement stmt = null;
		try
		{
			stmt = this.dbConnection.createStatement();
			rs = stmt.executeQuery("SELECT name FROM information_schema.sequences ORDER BY name");
			while (rs.next())
			{
				result.add(rs.getString(1));
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("H2SequenceReader.getSequenceList()", "Error reading sequences", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result;
	}
	
	public DataStore getSequenceDefinition(String owner, String sequence)
	{
		Statement stmt = null;
		ResultSet rs = null;
		DataStore ds = null;
		try
		{
			
			String sql = "SELECT * FROM information_schema.sequences WHERE name = '" + sequence + "'";
			stmt = this.dbConnection.createStatement();
			rs = stmt.executeQuery(sql);
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
