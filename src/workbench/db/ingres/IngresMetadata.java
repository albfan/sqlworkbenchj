/*
 * IngresMetadata.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import workbench.db.SequenceDefinition;
import workbench.db.SequenceReader;
import workbench.db.SynonymReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.util.ExceptionUtil;
import workbench.log.LogMgr;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * Ingres related metadata information.
 * 
 * @author  support@sql-workbench.net
 */
public class IngresMetadata
	implements SynonymReader, SequenceReader
{
	private Connection dbConn;
	private final String SELECT_SEQUENCE_DEF = 
			       "SELECT SEQ_NAME,  \n" +
			       "       SEQ_OWNER, \n" +
             "       MIN_VALUE,  \n" +
             "       MAX_VALUE,  \n" +
             "       INCREMENT_VALUE,  \n" +
             "       CYCLE_FLAG,  \n" +
             "       ORDER_FLAG,  \n" +
             "       CACHE_SIZE  \n" +
             "FROM   iisequences \n";

	public IngresMetadata(Connection conn)
	{
		dbConn = conn;
	}

	/**
	 * 	Get a list of synonyms for the given owner
	 */
	public List<String> getSynonymList(WbConnection conn, String owner)
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
			stmt = conn.getSqlConnection().prepareStatement(sql.toString());
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

	public TableIdentifier getSynonymTable(WbConnection con, String anOwner, String aSynonym)
		throws SQLException
	{
		StringBuilder sql = new StringBuilder(200);
		sql.append("SELECT synonym_name, table_owner, table_name FROM iisynonyms ");
		sql.append(" WHERE synonym_name = ? AND synonym_owner = ?");

		PreparedStatement stmt = con.getSqlConnection().prepareStatement(sql.toString());
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

	public String getSynonymSource(WbConnection con, String anOwner, String aSynonym)
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

	
	public List<SequenceDefinition> getSequences(String owner, String namePattern)
	{
		StringBuilder sql = new StringBuilder(SELECT_SEQUENCE_DEF);

		boolean whereAdded = false;
		int ownerIndex = -1;
		int nameIndex = -1;
		
		if (StringUtil.isNonBlank(owner))
		{
			whereAdded = true;
			ownerIndex = 1;
			sql.append(" WHERE seq_owner = ?");

		}
		if (StringUtil.isNonBlank(namePattern))
		{
			if (whereAdded)
			{
				sql.append(" AND ");
				nameIndex = 2;
			}
			else
			{
				sql.append(" WHERE ");
				nameIndex = 1;
			}
			sql.append(" seq_name LIKE ? ");
		}

		ResultSet rs = null;
		PreparedStatement stmt = null;
		List<SequenceDefinition> result = new ArrayList<SequenceDefinition>();

		try
		{
			stmt = this.dbConn.prepareStatement(sql.toString());
			if (ownerIndex != -1) stmt.setString(ownerIndex, owner.trim());
			if (nameIndex != -1) stmt.setString(nameIndex, namePattern.trim());
			rs = stmt.executeQuery();
			DataStore ds = new DataStore(rs, true);
			for (int i=0; i < ds.getRowCount(); i++)
			{
				result.add(getDefinition(ds, i, ds.getValueAsString(i, 1), ds.getValueAsString(i, 0)));
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
	
	public SequenceDefinition getSequenceDefinition(String owner, String sequence)
	{
		DataStore ds = getRawSequenceDefinition(owner, sequence);
		if (ds == null || ds.getRowCount() == 0) return null;
		return getDefinition(ds, 0, owner, sequence);
	}

	private SequenceDefinition getDefinition(DataStore ds, int row, String owner, String sequence)
	{
		SequenceDefinition def = new SequenceDefinition(owner, sequence);
		def.setSequenceProperty("MIN_VALUE", ds.getValue(row, "MIN_VALUE"));
		def.setSequenceProperty("MAX_VALUE", ds.getValue(row, "MAX_VALUE"));
		def.setSequenceProperty("INCREMENT_VALUE", ds.getValue(row, "INCREMENT_VALUE"));
		def.setSequenceProperty("CYCLE_FLAG", ds.getValue(row, "CYCLE_FLAG"));
		def.setSequenceProperty("ORDER_FLAG", ds.getValue(row, "ORDER_FLAG"));
		def.setSequenceProperty("CACHE_SIZE", ds.getValue(row, "CACHE_SIZE"));
		def.setSource(buildSource(def));
		return def;
	}
		
	public DataStore getRawSequenceDefinition(String owner, String sequence)
	{
		PreparedStatement stmt = null;
		ResultSet rs = null;
		DataStore result = null;
		String sql = SELECT_SEQUENCE_DEF + " WHERE seq_owner = ? AND seq_name = ?";
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

	public void readSequenceSource(SequenceDefinition def)
	{
		if (def == null) return;
		CharSequence s = getSequenceSource(def.getSequenceOwner(), def.getSequenceName());
		def.setSource(s);
	}

	private String buildSource(SequenceDefinition def)
	{
		StringBuilder result = new StringBuilder(100);
		Number minvalue = (Number)def.getSequenceProperty("MIN_VALUE");
		Number maxvalue = (Number)def.getSequenceProperty("MAX_VALUE");
		String increment = def.getSequenceProperty("INCREMENT_VALUE").toString();
		String cycle = def.getSequenceProperty("CYCLE_FLAG").toString();
		String order = def.getSequenceProperty("ORDER_FLAG").toString();
		Number cache = (Number)def.getSequenceProperty("CACHE_SIZE");

		result.append("CREATE SEQUENCE ");
		result.append(def.getSequenceName());

		result.append("\n       INCREMENT BY ");
		result.append(increment);

		if (minvalue.intValue() == 0)
		{
			result.append("\n       NO MINVALUE");
		}
		else
		{
			result.append("\n       MINVALUE ");
			result.append(minvalue);
		}

		if (maxvalue.intValue() < Integer.MAX_VALUE)
		{
			result.append("\n       MAXVALUE ");
			result.append(maxvalue);
		}
		else
		{
			result.append("\n       NO MAXVALUE");
		}
		if (cache.intValue() > 0)
		{
			result.append("\n       CACHE ");
			result.append(cache);
		}
		else
		{
			result.append("\n       NO CACHE");
		}
		result.append("\n       ");
		result.append(cycle.equals("Y") ? "CYCLE" : "NO CYCLE");

		result.append("\n       ");
		result.append(order.equals("Y") ? "ORDER" : "NO ORDER");

		result.append(";\n");
		return result.toString();
	}

	public String getSequenceSource(String owner, String sequence)
	{
		ResultSet rs = null;
		PreparedStatement stmt = null;
		StringBuilder result = new StringBuilder(100);

		try
		{
			stmt = this.dbConn.prepareStatement(SELECT_SEQUENCE_DEF);
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

				BigInteger max = new BigInteger(Integer.toString(Integer.MAX_VALUE));

				if (minvalue.compareTo(BigInteger.ONE) == 0)
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
				result.append(cycle.equals("Y") ? "CYCLE" : "NO CYCLE");

				result.append("\n      ");
				result.append(order.equals("Y") ? "ORDER" : "NO ORDER");

				result.append(";\n");
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

