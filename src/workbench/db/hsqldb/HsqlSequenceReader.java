/*
 * HsqlSequenceReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.hsqldb;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import workbench.db.SequenceReader;
import workbench.log.LogMgr;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;

/**
 * @author  support@sql-workbench.net
 */
public class HsqlSequenceReader
	implements SequenceReader
{
	private Connection dbConn;
	private final String sql;
	private boolean is18;
	
	public HsqlSequenceReader(Connection conn)
	{
		this.dbConn = conn;
		String version = null;
		try
		{
			version = conn.getMetaData().getDatabaseProductVersion();
		}
		catch (SQLException e)
		{
			version = "1.7.0";
		}
		
		if (version.startsWith("1.8"))
		{
			this.is18 = true;
			sql = "SELECT sequence_name, \n" + 
             "       dtd_identifier, \n" + 
             "       maximum_value, \n" + 
             "       minimum_value, \n" + 
             "       increment, \n" + 
             "       start_with " + 
             " FROM information_schema.system_sequences WHERE sequence_name = ?";		
		}
		else
		{
			sql = "SELECT sequence_name, \n" + 
             "       dtd_identifier, \n" + 
             "       maximum_value, \n" + 
             "       minimum_value, \n" + 
             "       increment, \n" + 
             "       start_with " + 
             " FROM system_sequences WHERE sequence_name = ?";		
		}
	}

	public DataStore getSequenceDefinition(String owner, String sequence)
	{
		PreparedStatement stmt = null;
		ResultSet rs = null;
		DataStore result = null;
		try
		{
			stmt = this.dbConn.prepareStatement(sql);
			stmt.setString(1, sequence.trim());
			rs = stmt.executeQuery();
			result = new DataStore(rs, true);
		}
		catch (Throwable e)
		{
			LogMgr.logError("HsqlSequenceReader.getSequenceDefinition()", "Error when retrieving sequence definition", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}

		return result;	
	}

	public List getSequenceList(String owner)
	{
		ResultSet rs = null;
		PreparedStatement stmt = null;
		ArrayList result = new ArrayList(100);

		StringBuffer sql = new StringBuffer(200);
		sql.append("SELECT sequence_name FROM ");
		if (is18) sql.append("information_schema.");
		sql.append("system_sequences");

		try
		{
			stmt = this.dbConn.prepareStatement(sql.toString());
			rs = stmt.executeQuery();
			while (rs.next())
			{
				String seq = rs.getString(1);
				if (seq != null) result.add(seq.trim());
			}
		}
		catch (Throwable e)
		{
			LogMgr.logError("HsqlSequenceReader.getSequenceList()", "Error when retrieving sequences",e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result;
	}

	public String getSequenceSource(String owner, String sequence)
	{
		ResultSet rs = null;
		PreparedStatement stmt = null;
		
		final String sql = "SELECT sequence_name, " +
			                 "       dtd_identifier, " +
			                 "       start_with, " +
			                 "       maximum_value, " +
			                 "       increment " +
			                 "FROM system_sequences WHERE sequence_name = ?";
		StringBuffer result = new StringBuffer(100);
		result.append("CREATE SEQUENCE ");
		try
		{
			stmt = this.dbConn.prepareStatement(sql);
			stmt.setString(1, sequence);
			rs = stmt.executeQuery();
			while (rs.next())
			{
				String seq = rs.getString(1);
				result.append(seq);
				String type = rs.getString(2);
				if (!"INTEGER".equals(type))
				{
					result.append(" AS " + type);
				}
				long start = rs.getLong(3);
				if (start > 0)
				{
					result.append("\n       START WITH ");
					result.append(start);
				}
				String max = rs.getString(4);
				final BigInteger bigMax = new BigInteger("9223372036854775807");
				final BigInteger intMax = new BigInteger(Integer.toString(Integer.MAX_VALUE));
				boolean isMax = false;
				try
				{
					BigInteger maxValue = new BigInteger(max);
					isMax = (maxValue.equals(intMax) || maxValue.equals(bigMax));
				}
				catch (Exception e)
				{
					isMax = false;
				}
				
				if (!isMax)
				{
					result.append("\n       START WITH ");
					result.append(start);
				}
				long inc = rs.getLong(5);
				if (inc != 1)
				{
					result.append("\n       INCREMENT BY ");
					result.append(inc);
				}
				result.append(";");
			}
		}
		catch (Throwable e)
		{
			LogMgr.logError("HsqlSequenceReader.getSequenceSource()", "Error when retrieving sequence source",e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result.toString();
	}
}
