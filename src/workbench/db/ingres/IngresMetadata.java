/*
 * IngresMetadata.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.ingres;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import workbench.db.SequenceDefinition;
import workbench.db.SequenceReader;
import workbench.db.SynonymReader;
import workbench.db.TableIdentifier;
import workbench.util.ExceptionUtil;
import workbench.log.LogMgr;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;

/**
 * Ingres related metadata information.
 * 
 * @author  support@sql-workbench.net
 */
public class IngresMetadata
	implements SynonymReader, SequenceReader
{
	private Connection dbConn;
	
	public IngresMetadata(Connection conn)
	{
		dbConn = conn;
	}

	/**
	 * 	Get a list of synonyms for the given owner
	 */
	public List<String> getSynonymList(Connection conn, String owner)
	{
		ResultSet rs = null;
		PreparedStatement stmt = null;
		List<String> result = new LinkedList<String>();

		StringBuilder sql = new StringBuilder(200);
		sql.append("SELECT synonym_name FROM iisynonyms ");
		if (owner != null)
		{
			sql.append(" WHERE synonym_owner = ?");
		}

		try
		{
			stmt = conn.prepareStatement(sql.toString());
			if (owner != null) stmt.setString(1, owner);
			rs = stmt.executeQuery();
			while (rs.next())
			{
				String seq = rs.getString(1);
				if (seq != null) result.add(seq.trim());
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("OracleMetaData.getSynonymList()", "Error when retrieving synonyms",e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result;
	}

	public TableIdentifier getSynonymTable(Connection con, String anOwner, String aSynonym)
		throws SQLException
	{
		StringBuilder sql = new StringBuilder(200);
		sql.append("SELECT synonym_name, table_owner, table_name FROM iisynonyms ");
		sql.append(" WHERE synonym_name = ? AND synonym_owner = ?");

		PreparedStatement stmt = con.prepareStatement(sql.toString());
		stmt.setString(1, aSynonym);
		stmt.setString(2, anOwner);

		ResultSet rs = stmt.executeQuery();
		String table = null;
		String owner = null;
		TableIdentifier result = null;
		try
		{
			if (rs.next())
			{
				owner = rs.getString(2);
				table = rs.getString(3);
				if (table != null)
				{
					result = new TableIdentifier(null, owner, table);
				}
			}
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}

		return result;
	}

	public String getSynonymSource(Connection con, String anOwner, String aSynonym)
		throws SQLException
	{
		TableIdentifier id = getSynonymTable(con, anOwner, aSynonym);
		StringBuilder result = new StringBuilder(200);
		result.append("CREATE SYNONYM ");
		result.append(aSynonym);
		result.append("\n       FOR ");
		result.append(id.getTableExpression());
		result.append(";\n");
		return result.toString();
	}

	
	public List<SequenceDefinition> getSequences(String owner)
	{
		return null;
	}
	
	public SequenceDefinition getSequenceDefinition(String owner, String sequence)
	{
		return null;
	}
		
	public DataStore getRawSequenceDefinition(String owner, String sequence)
	{
		String sql = "SELECT * FROM iisequences WHERE seq_owner = ? AND seq_name = ? ";
		PreparedStatement stmt = null;
		ResultSet rs = null;
		DataStore result = null;
		try
		{
			stmt = this.dbConn.prepareStatement(sql);
			stmt.setString(1, owner.trim());
			stmt.setString(2, sequence.trim());
			rs = stmt.executeQuery();
			result = new DataStore(rs, true);
		}
		catch (Throwable e)
		{
			LogMgr.logError("IngresMetaData.getRawSequenceDefinition()", "Error when retrieving sequence definition", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}

		return result;
	}

	public List<String> getSequenceList(String owner)
	{
		ResultSet rs = null;
		PreparedStatement stmt = null;
		List<String> result = new LinkedList<String>();

		StringBuilder sql = new StringBuilder(200);
		sql.append("SELECT seq_name FROM iisequences ");
		if (owner != null)
		{
			sql.append(" WHERE seq_owner = ?");
		}

		try
		{
			stmt = this.dbConn.prepareStatement(sql.toString());
			if (owner != null) stmt.setString(1, owner.trim());
			rs = stmt.executeQuery();
			while (rs.next())
			{
				String seq = rs.getString(1);
				if (seq != null) result.add(seq.trim());
			}
		}
		catch (Throwable e)
		{
			LogMgr.logError("IngresMetaData.getSequenceList()", "Error when retrieving sequences",e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result;
	}

	public void readSequenceSource(SequenceDefinition def)
	{
		if (def == null) return;
		CharSequence s = getSequenceSource(def.getSequenceOwner(), def.getSequenceName());
		def.setSource(s);
	}
	
	public String getSequenceSource(String owner, String sequence)
	{
		ResultSet rs = null;
		PreparedStatement stmt = null;
		StringBuilder result = new StringBuilder(100);

		String sql = "SELECT trim(SEQ_NAME),  \n" + 
             "       MIN_VALUE,  \n" + 
             "       MAX_VALUE,  \n" + 
             "       INCREMENT_value,  \n" + 
             "       case CYCLE_FLAG when 'Y' then 'CYCLE' else 'NO CYCLE' end,  \n" + 
             "       case ORDER_FLAG when 'Y' then 'ORDER' else 'NO ORDER' end,  \n" + 
             "       CACHE_SIZE  \n" + 
             "FROM   iisequences \n" +
						 " WHERE seq_owner = ?" +
		         "  AND seq_name = ?";
		try
		{
			stmt = this.dbConn.prepareStatement(sql);
			stmt.setString(1, owner.trim());
			stmt.setString(2, sequence.trim());

			rs = stmt.executeQuery();
			if (rs.next())
			{
				result.append("CREATE SEQUENCE ");
				result.append(rs.getString(1));

				BigInteger minvalue = rs.getBigDecimal(2).toBigInteger();
				BigInteger maxvalue = rs.getBigDecimal(3).toBigInteger();
				long increment = rs.getLong(4);
				String cycle = rs.getString(5);
				String order = rs.getString(6);
				long cache = rs.getLong(7);

				result.append("\n      INCREMENT BY ");
				result.append(increment);

				BigInteger one = new BigInteger("1");
				BigInteger max = new BigInteger(Integer.toString(Integer.MAX_VALUE));

				if (minvalue.compareTo(one) == 0)
				{
					result.append("\n      NO MINVALUE");
				}
				else
				{
					result.append("\n      MINVALUE ");
					result.append(minvalue);
				}

				if (maxvalue.compareTo(max) == -1)
				{
					result.append("\n      MAXVALUE ");
					result.append(maxvalue);
				}
				else
				{
					result.append("\n      NO MAXVALUE");
				}
				if (cache > 0)
				{
					result.append("\n      CACHE ");
					result.append(cache);
				}
				else
				{
					result.append("\n      NO CACHE");
				}
				result.append("\n      ");
				result.append(cycle);

				result.append("\n      ");
				result.append(order);

				result.append("\n;");
			}
		}
		catch (Throwable e)
		{
			LogMgr.logError("IngresMetaData.getSequenceSource()", "Error when retrieving sequence source",e);
			result = new StringBuilder(ExceptionUtil.getDisplay(e));
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result.toString();
	}
	
}

