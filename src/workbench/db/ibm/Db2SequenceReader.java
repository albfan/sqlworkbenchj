/*
 * Db2SequenceReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.ibm;

import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import workbench.db.SequenceReader;
import workbench.db.WbConnection;
import workbench.util.ExceptionUtil;

import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;

/**
 * A class to read sequence definitions from a DB2 database.
 * 
 * @author  support@sql-workbench.net
 */
public class Db2SequenceReader
	implements SequenceReader
{
	private WbConnection connection;
	
	public Db2SequenceReader(WbConnection conn)
	{
		this.connection = conn;
	}
	
	public DataStore getSequenceDefinition(String schema, String sequence)
	{
		StringBuilder sql = new StringBuilder(100);
		sql.append("SELECT * FROM syscat.sequences ");
		sql.append(" WHERE seqschema  = ? ");
		sql.append("  AND seqname  = ? ");
		
		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("Db2SequenceReader.getSequenceDefinition()", "Using query=\n" + sql);
		}
		
		PreparedStatement stmt = null;
		ResultSet rs = null;
		DataStore result = null;
		try
		{
			stmt = this.connection.getSqlConnection().prepareStatement(sql.toString());
			stmt.setString(1, schema);
			stmt.setString(2, sequence);
			rs = stmt.executeQuery();
			result = new DataStore(rs, this.connection, true);
		}
		catch (Exception e)
		{
			LogMgr.logError("OracleMetaData.getSequenceDefinition()", "Error when retrieving sequence definition", e);
		}
		finally
		{
			SqlUtil.closeAll(rs,stmt);
		}
		
		return result;
	}
	
	/**
	 * 	Get a list of sequences for the given owner
	 */
	public List getSequenceList(String schema)
	{
		ResultSet rs = null;
		PreparedStatement stmt = null;
		List result = new ArrayList(100);
		
		StringBuilder sql = new StringBuilder(200);
		sql.append("SELECT seqname FROM syscat.sequences WHERE seqtype = 'S' ");
		
		if (schema != null)
		{
			sql.append(" AND seqschema = ?");
		}
		
		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("Db2SequenceReader.getSequenceList()", "Using query=\n" + sql);
		}
		
		try
		{
			stmt = this.connection.getSqlConnection().prepareStatement(sql.toString());
			if (schema != null) stmt.setString(1, schema);
			rs = stmt.executeQuery();
			while (rs.next())
			{
				String seq = rs.getString(1);
				result.add(seq);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("OracleMetaData.getSequenceList()", "Error when retrieving sequences",e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result;
	}
	
	public String getSequenceSource(String schema, String sequence)
	{
		ResultSet rs = null;
		PreparedStatement stmt = null;
		StringBuilder result = new StringBuilder(100);
		
		String sql ="SELECT SEQNAME, \n" +
			"       START, \n" +
			"       MINVALUE, \n" +
			"       MAXVALUE, \n" +
			"       INCREMENT, \n" +
			"       CYCLE, \n" +
			"       ORDER, \n" +
			"       CACHE, \n" +
			"       DATATYPEID  \n" +
			"FROM   syscat.sequences \n" +
			"WHERE seqschema = ?" +
			"  AND seqname = ?";
		
		try
		{
			stmt = this.connection.getSqlConnection().prepareStatement(sql);
			stmt.setString(1, schema);
			stmt.setString(2, sequence);
			
			String nl = Settings.getInstance().getInternalEditorLineEnding();
			
			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logInfo("Db2SequenceReader.getSequenceSource()", "Using query=\n" + sql);
			}
			rs = stmt.executeQuery();
			if (rs.next())
			{
				result.append("CREATE SEQUENCE ");
				result.append(rs.getString("SEQNAME"));
				
				BigInteger start = rs.getBigDecimal("START").toBigInteger();
				BigInteger minvalue = rs.getBigDecimal("MINVALUE").toBigInteger();
				BigInteger maxvalue = rs.getBigDecimal("MAXVALUE").toBigInteger();
				long increment = rs.getLong("INCREMENT");
				String cycle = rs.getString("CYCLE");
				String order = rs.getString("ORDER");
				long cache = rs.getLong("CACHE");
				int typeid = rs.getInt("DATATYPEID");
				
				result.append(" AS " + typeIdToName(typeid));
				result.append(nl + "      INCREMENT BY ");
				result.append(increment);
				
				BigInteger one = new BigInteger("1");
				BigInteger max = new BigInteger(Integer.toString(Integer.MAX_VALUE));

				if (start.compareTo(one) > 0)
				{
					result.append(nl + "      START WITH ");
					result.append(start);
				}
				
				if (minvalue.compareTo(one) == 0)
				{
					result.append(nl + "      NO MINVALUE");
				}
				else
				{
					result.append(nl + "      MINVALUE ");
					result.append(minvalue);
				}
				
				if (maxvalue.compareTo(max) == -1)
				{
					result.append(nl + "      MAXVALUE ");
					result.append(maxvalue);
				}
				else
				{
					result.append(nl + "      NO MAXVALUE");
				}
				if (cache > 0)
				{
					result.append(nl + "      CACHE ");
					result.append(cache);
				}
				else
				{
					result.append(nl + "      NO CACHE");
				}
				result.append(nl + "      ");
				if (cycle.equals("Y"))
				{
					result.append("CYCLE");
				}
				else
				{
					result.append("NO CYCLE");
				}
				
				result.append(nl + "      ");
				if (order.equals("Y"))
				{
					result.append("ORDER");
				}
				else
				{
					result.append("NO ORDER");
				}
				
				result.append(nl);
				result.append(';');
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("OracleMetaData.getSequenceList()", "Error when retrieving sequences",e);
			result = new StringBuilder(ExceptionUtil.getDisplay(e));
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result.toString();
	}

	private String typeIdToName(int id)
	{
		switch (id)
		{
			case 20:
				return "BIGINT";
			case 28:
				return "SMALLINT";
			case 16:
				return "DECIMAL";
		}
		return "INTEGER";
	}
}
