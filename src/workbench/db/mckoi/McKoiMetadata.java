/*
 * McKoiMetadata.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2004, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.db.mckoi;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import workbench.db.SequenceReader;
import workbench.exception.ExceptionUtil;
import workbench.log.LogMgr;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;


/**
 * @author  info@sql-workbench.net
 */
public class McKoiMetadata
	implements SequenceReader
{
	private Connection connection;
	
	public McKoiMetadata(Connection con)
	{
		this.connection = con;
	}
	
	// This is a dirty hack, as McKoi does not store the real SQL
	// but some kind of Object in the database. But for now this seems
	// to work.
	public String getViewSourceFromBlob(byte[] content)
	{
		String s = new String(content, 2, content.length - 6);
		return s;
	}
	
	public void done()
	{
		this.connection = null;
	}
	
	public workbench.storage.DataStore getSequenceDefinition(String owner, String sequence)
	{
		StringBuffer sql = new StringBuffer(100);
		sql.append("select sd.* from SYS_INFO.sUSRSequence sd, SYS_INFO.sUSRSequenceInfo si ");
		sql.append(" where sd.seq_id = si.id AND \"si.schema\"='" + owner + "' and si.name = '" + sequence + "'");
		PreparedStatement stmt = null;
		ResultSet rs = null;
		DataStore result = null;
		try
		{
			stmt = this.connection.prepareStatement(sql.toString());
			stmt.setString(1, owner);
			stmt.setString(2, sequence);
			rs = stmt.executeQuery();
			result = new DataStore(rs, true);
		}
		catch (Exception e)
		{
			LogMgr.logError("McKoiMetadata.getSequenceDefinition()", "Error when retrieving sequence definition", e);
		}
		finally
		{
			try { rs.close(); } catch (Throwable th) {}
			try { stmt.close(); } catch (Throwable th) {}
		}
		
		return result;
	}
	
	public List getSequenceList(String owner)
	{
		ResultSet rs = null;
		PreparedStatement stmt = null;
		ArrayList result = new ArrayList(100);

		StringBuffer sql = new StringBuffer(200);
		sql.append("select name from SYS_INFO.sUSRSequence ");
		if (owner != null)
		{
			sql.append(" WHERE \"schema\" = ?");
		}

		try
		{
			stmt = this.connection.prepareStatement(sql.toString());
			if (owner != null) stmt.setString(1, owner);
			rs = stmt.executeQuery();
			while (rs.next())
			{
				String seq = rs.getString(1);
				result.add(seq);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("McKoiMetadata.getSequenceList()", "Error when retrieving sequences",e);
		}
		finally
		{
			try { rs.close(); } catch (Throwable th) {}
			try { stmt.close(); } catch (Throwable th) {}
		}
		return result;
	}
	
	public String getSequenceSource(String owner, String sequence)
	{
		String sql = "SELECT si.name, \n" + 
								 "       sd.MINVALUE, \n" + 
								 "       sd.MAXVALUE, \n" + 
								 "       sd.INCREMENT, \n" + 
								 "       sd.CYCLE, \n" + 
								 "       sd.START, \n" + 
								 "       sd.CACHE \n" + 
								 "FROM SYS_INFO.sUSRSequence sd, \n" + 
								 "     SYS_INFO.sUSRSequenceInfo si \n" + 
								 "WHERE sd.seq_id = si.id \n" + 
								 "AND   \"si.schema\" = ? \n" + 
								 "AND   si.name = ? ";		

		ResultSet rs = null;
		PreparedStatement stmt = null;
		StringBuffer result = new StringBuffer(100);
		
		try
		{
			stmt = this.connection.prepareStatement(sql);
			stmt.setString(1, owner);
			stmt.setString(2, sequence);

			rs = stmt.executeQuery();
			if (rs.next())
			{
				result.append("CREATE SEQUENCE ");
				result.append(rs.getString(1));

				BigInteger minvalue = rs.getBigDecimal(2).toBigInteger();
				BigInteger maxvalue = rs.getBigDecimal(3).toBigInteger();
				long increment = rs.getLong(4);
				boolean cycle = rs.getBoolean(5);
				long start = rs.getLong(6);
				long cache = rs.getLong(7);

				if (start > 0)
				{
					result.append("\n      START ");
					result.append(start);
				}
				
				if (increment != 1)
				{
					result.append("\n      INCREMENT ");
					result.append(increment);
				}

				BigInteger zero = new BigInteger("0");
				BigInteger max = new BigInteger(Long.toString(Long.MAX_VALUE));

				if (minvalue.compareTo(zero) == -1)
				{
					result.append("\n      MINVALUE ");
					result.append(minvalue);
				}

				if (maxvalue.compareTo(max) == -1)
				{
					result.append("\n      MAXVALUE ");
					result.append(maxvalue);
				}
				
				if (cache > 0)
				{
					result.append("\n      CACHE ");
					result.append(cache);
				}
				if (cycle)
				{
					result.append("\n      CYCLE");
				}

				result.append(";");
			}
		}
		catch (Throwable e)
		{
			LogMgr.logError("OracleMetaData.getSequenceList()", "Error when retrieving sequences",e);
			result = new StringBuffer(ExceptionUtil.getDisplay(e));
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		
		return result.toString();
	}
	
}
