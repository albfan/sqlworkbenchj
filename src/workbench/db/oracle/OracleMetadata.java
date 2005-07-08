/*
 * OracleMetaData.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.oracle;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import workbench.db.ConnectionProfile;
import workbench.db.DbMetadata;
import workbench.db.ErrorInformationReader;
import workbench.db.JdbcProcedureReader;
import workbench.db.ProcedureReader;
import workbench.db.SchemaInformationReader;
import workbench.db.SequenceReader;
import workbench.db.WbConnection;
import workbench.resource.Settings;
import workbench.util.ExceptionUtil;

import workbench.log.LogMgr;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;
import workbench.util.StrBuffer;

/**
 *
 * @author  support@sql-workbench.net
 */
public class OracleMetadata
	implements SequenceReader, ProcedureReader, ErrorInformationReader, SchemaInformationReader
{
	private WbConnection connection;
	private Statement statement;
	private PreparedStatement columnStatement;
	private int version = 8;
	private JdbcProcedureReader procReader;
	
	/** Creates a new instance of OracleMetaData */
	public OracleMetadata(WbConnection conn)
	{
		this.connection = conn;
		try
		{
			String version = this.connection.getSqlConnection().getMetaData().getDatabaseProductVersion();
			if (version.indexOf("Release 9.") > -1)
			{
				this.version = 9;
			}
			else if (version.indexOf("Release 10.") > -1)
			{
				this.version = 10;
			}
		}
		catch (Throwable th)
		{
			this.version = 8;
		}
	}
	
	public boolean isOracle8()
	{
		return this.version == 8;
	}
	
	public DataStore getSequenceDefinition(String owner, String sequence)
	{
		StringBuffer sql = new StringBuffer(100);
		sql.append("SELECT * FROM all_sequences ");
		sql.append(" WHERE sequence_owner = ? ");
		sql.append("  AND sequence_name = ? ");
		PreparedStatement stmt = null;
		ResultSet rs = null;
		DataStore result = null;
		try
		{
			stmt = this.connection.getSqlConnection().prepareStatement(sql.toString());
			stmt.setString(1, owner);
			stmt.setString(2, sequence);
			rs = stmt.executeQuery();
			result = new DataStore(rs, true);
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
	public List getSequenceList(String owner)
	{
		ResultSet rs = null;
		PreparedStatement stmt = null;
		ArrayList result = new ArrayList(100);
		
		StringBuffer sql = new StringBuffer(200);
		sql.append("SELECT sequence_name FROM all_sequences ");
		if (owner != null)
		{
			sql.append(" WHERE sequence_owner = ?");
		}
		
		try
		{
			stmt = this.connection.getSqlConnection().prepareStatement(sql.toString());
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
			LogMgr.logError("OracleMetaData.getSequenceList()", "Error when retrieving sequences",e);
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
		StringBuffer result = new StringBuffer(100);
		
		String sql ="SELECT SEQUENCE_NAME, \n" +
			"       MIN_VALUE, \n" +
			"       MAX_VALUE, \n" +
			"       INCREMENT_BY, \n" +
			"       decode(CYCLE_FLAG,'Y','CYCLE','NOCYCLE'), \n" +
			"       decode(ORDER_FLAG,'Y','ORDER','NOORDER'), \n" +
			"       CACHE_SIZE \n" +
			"FROM   ALL_SEQUENCES \n" +
			"WHERE sequence_owner = ?" +
			"  AND sequence_name = ?";
		
		try
		{
			stmt = this.connection.getSqlConnection().prepareStatement(sql);
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
				String cycle = rs.getString(5);
				String order = rs.getString(6);
				long cache = rs.getLong(7);
				
				result.append("\n      INCREMENT BY ");
				result.append(increment);
				
				BigInteger one = new BigInteger("1");
				BigInteger max = new BigInteger(Integer.toString(Integer.MAX_VALUE));
				
				if (minvalue.compareTo(one) == 0)
				{
					result.append("\n      NOMINVALUE");
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
					result.append("\n      NOMAXVALUE");
				}
				if (cache > 0)
				{
					result.append("\n      CACHE ");
					result.append(cache);
				}
				else
				{
					result.append("\n      NOCACHE");
				}
				result.append("\n      ");
				result.append(cycle);
				
				result.append("\n      ");
				result.append(order);
				
				result.append("\n;");
			}
		}
		catch (Exception e)
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
	
	/**
	 * 	Replacement for the DatabaseMetaData.getIndexInfo() method.
	 * 	Oracle's JDBC driver does an ANALYZE INDEX each time an indexInfo is
	 * 	requested which slows down the retrieval of index information.
	 *  (and is not necessary at all for the Workbench, as we don't use the
	 *  cardinality field anyway)
	 */
	public ResultSet getIndexInfo(String catalog, String schema, String table, boolean unique, boolean flag1)
	throws SQLException
	{
		this.closeStatement();
		
		StringBuffer sql = new StringBuffer(200);
		sql.append("SELECT null as table_cat, " +
			"       i.owner as table_schem, " +
			"       i.table_name, "+
			"       decode (i.uniqueness, 'UNIQUE', 0, 1) as non_unique, " +
			"       null as index_qualifier, " +
			"       i.index_name, "+
			"       1 as type, " +
			"       c.column_position as ordinal_position, " +
			"       c.column_name, " +
			"       null as asc_or_desc, " +
			"       i.distinct_keys as cardinality, " +
			"       i.leaf_blocks as pages, " +
			"       null as filter_condition, " +
			"       i.index_type " +
			"FROM all_indexes i, all_ind_columns c " +
			"WHERE i.table_name = '" + table + "' \n");
		String ownerWhere2 = "";
		if (schema != null && schema.length() > 0)
		{
			sql.append("  AND i.owner = '" + schema + "'\n");
		}
		if (unique)
		{
			sql.append("  and i.uniqueness = 'UNIQUE'\n");
		}
		sql.append("  and i.index_name = c.index_name " +
			"  and i.table_owner = c.table_owner " +
			"  and i.table_name = c.table_name " +
			"  and i.owner = c.index_owner ");
		sql.append("ORDER BY non_unique, type, index_name, ordinal_position ");
		if (this.statement != null) this.closeStatement();
		this.statement = this.connection.createStatementForQuery();
		ResultSet rs = this.statement.executeQuery(sql.toString());
		return rs;
	}
	
	/**
	 * 	Read the definition for function based indexes into the Map provided.
	 * 	The map should contain the names of the indexes as keys, and an List
	 * 	as elements. Each Element of the list is one part (=function call to a column)
	 * 	of the index definition.
	 */
	public void readFunctionIndexDefinition(String schema, String table, Map indexNames)
	{
		if (indexNames.size() == 0) return;
		
		String base="SELECT i.index_name, e.column_expression, e.column_position \n" +
			"FROM all_indexes i, all_ind_expressions e  \n" +
			" WHERE i.index_name = e.index_name   \n" +
			"    and i.owner = e.index_owner   \n" +
			"    and i.table_name = e.table_name   \n" +
			"    and e.index_owner = i.owner \n " +
			"    and i.index_type like 'FUNCTION-BASED%' ";
		StringBuffer sql = new StringBuffer(300);
		sql.append(base);
		if (schema != null && schema.length() > 0)
		{
			sql.append(" AND i.owner = '" + schema + "' ");
		}
		Iterator keys = indexNames.keySet().iterator();
		boolean first = true;
		sql.append(" AND i.index_name IN (");
		while (keys.hasNext())
		{
			if (first)
			{
				first = false;
			}
			else
			{
				sql.append(",");
			}
			sql.append('\'');
			sql.append((String)keys.next());
			sql.append('\'');
		}
		sql.append(") ");
		
		ResultSet rs = null;
		Statement stmt = null;
		try
		{
			stmt = this.connection.createStatementForQuery();
			rs = stmt.executeQuery(sql.toString());
			while (rs.next())
			{
				String name = rs.getString(1);
				String exp = rs.getString(2);
				List cols = (List)indexNames.get(name);
				if (cols != null)
				{
					if (!cols.contains(exp))
					{
						cols.add(exp);
					}
				}
			}
		}
		catch (Exception e)
		{
			LogMgr.logWarning("OracleMetaData.readFunctionIndexDefinition()", "Error reading function-based index definition", e);
		}
		finally
		{
			try
			{ rs.close(); }
			catch (Throwable th)
			{}
			try
			{ stmt.close(); }
			catch (Throwable th)
			{}
		}
	}
	
	public void closeStatement()
	{
		if (this.statement != null)
		{
			try
			{
				this.statement.close();
			}
			catch (Throwable e)
			{
			}
		}
		
		this.statement = null;
	}
	
	public ResultSet getColumns(String catalog, String schema, String table, String cols)
		throws SQLException
	{
		// Oracle 9 and above reports a wrong length if NLS_LENGTH_SEMANTICS is set to char
		// this statement fixes this problem
		final String sql1 ="SELECT NULL AS table_cat,  \n" +
			"       t.owner AS table_schem,  \n" +
			"       t.table_name AS table_name,  \n" +
			"       t.column_name AS column_name,  \n" +
			"       DECODE (t.data_type, 'CHAR', 1, 'VARCHAR2', 12, 'NVARCHAR2', 12, 'NUMBER', 3, 'LONG', -1, 'DATE', 93, 'RAW', -3, 'LONG RAW', -4, 1111)  AS data_type,  \n" +
			"       t.data_type AS type_name,  \n" +
			"       DECODE (t.data_precision, null, decode(t.data_type, 'VARCHAR2', t.char_length, 'NVARCHAR2', t.char_length, t.data_length), t.data_precision) AS column_size,  \n" +
			"       0 AS buffer_length,  \n" +
			"       t.data_scale AS decimal_digits,  \n" +
			"       10 AS num_prec_radix,  \n" +
			"       DECODE (t.nullable, 'N', 0, 1) AS nullable,  \n";
		final String sql2 = "       t.data_default AS column_def,  \n" +
			"       0 AS sql_data_type,  \n" +
			"       0 AS sql_datetime_sub,  \n" +
			"       t.data_length AS char_octet_length,  \n" +
			"       t.column_id AS ordinal_position,   \n" +
			"	     DECODE (t.nullable, 'N', 'NO', 'YES') AS is_nullable  \n" +
			"FROM all_tab_columns t";
		final String where = " WHERE t.owner LIKE ? ESCAPE '/'  AND t.table_name LIKE ? ESCAPE '/'  AND t.column_name LIKE ? ESCAPE '/'  \n";
		
		final String comment_join = "   AND t.owner = c.owner (+)  AND t.table_name = c.table_name (+)  AND t.column_name = c.column_name (+)  \n";
		final String order = "ORDER BY table_schem, table_name, ordinal_position";
		final String sql_comment = sql1 +  "       c.comments AS remarks, \n" + sql2 + where + ", all_col_comments c  \n" + comment_join + order;
		final String sql_no_comment = sql1 +  "       null AS remarks, \n" + sql2 + where + order;
		String sql;
		
		boolean returnComments = false;
		ConnectionProfile prof = this.connection.getProfile();
		if (prof != null)
		{
			Properties prop = prof.getConnectionProperties();
			if (prop != null)	returnComments = "true".equals(prop.getProperty("remarksReporting", "false"));
		}
		
		if (returnComments)
		{
			sql = sql_comment;
		}
		else
		{
			sql = sql_no_comment;
		}
		
		ResultSet rs = null;
		
		synchronized (connection)
		{
			// The above statement does not work with Oracle 8
			// so in that case we revert back to Oracle's implementation of getColumns()
			if (version > 8 && Settings.getInstance().useOracleCharSemanticsFix())
			{
				if (this.columnStatement == null)
				{
					this.columnStatement = this.connection.getSqlConnection().prepareStatement(sql);
				}
				this.columnStatement.setString(1, schema != null ? schema : "%");
				this.columnStatement.setString(2, table != null ? table : "%");
				this.columnStatement.setString(3, cols != null ? cols : "%");
				rs = this.columnStatement.executeQuery();
			}
			else
			{
				rs = this.connection.getSqlConnection().getMetaData().getColumns(catalog, schema, table, cols);
			}
		}
		return rs;
	}
	
	private final StrBuffer PROC_HEADER = new StrBuffer("CREATE OR REPLACE ");
	
	public StrBuffer getProcedureHeader(String catalog, String schema, String procname)
	{
		return PROC_HEADER;
	}
	
	public DataStore getProcedureColumns(String catalog, String schema, String procname)
		throws SQLException
	{
		initProcReader();
		return this.procReader.getProcedureColumns(catalog, schema, procname);
	}
	
	public DataStore getProcedures(String catalog, String schema)
		throws SQLException
	{
		initProcReader();
		return this.procReader.getProcedures(catalog, schema);
	}

	private void initProcReader()
	{
		if (this.procReader != null) return;
		this.procReader = new JdbcProcedureReader(this.connection.getMetadata());
	}
	
	public StrBuffer getPackageSource(String owner, String packageName)
	{
		final String sql = "SELECT text \n" +
			"FROM all_source \n" +
			"WHERE name = ? \n" +
			"AND   owner = ? \n" +
			"AND   type = ? \n" +
			"ORDER BY line";
		
		StrBuffer result = new StrBuffer(1000);
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try
		{
			int lineCount = 0;
			stmt = this.connection.getSqlConnection().prepareStatement(sql);
			stmt.setString(1, packageName);
			stmt.setString(2, owner);
			stmt.setString(3, "PACKAGE");
			rs = stmt.executeQuery();
			while (rs.next())
			{
				String line = rs.getString(1);
				if (line != null)
				{
					lineCount ++;
					if (lineCount == 1)
					{
						result.append("CREATE OR REPLACE ");
					}
					result.append(line);
				}
			}
			if (!(result.endsWith('\n') || result.endsWith('\r'))) result.append('\n');
			result.append("/\n\n");
			lineCount = 0;
			
			stmt.clearParameters();
			stmt.setString(1, packageName);
			stmt.setString(2, owner);
			stmt.setString(3, "PACKAGE BODY");
			rs = stmt.executeQuery();
			while (rs.next())
			{
				String line = rs.getString(1);
				if (line != null)
				{
					lineCount ++;
					if (lineCount == 1)
					{
						result.append("CREATE OR REPLACE ");
					}
					result.append(line);
				}
			}
			if (!(result.endsWith('\n') || result.endsWith('\r'))) result.append('\n');
			result.append("/\n");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result;
	}
	
	private String CURRENT_SCHEMA_SQL = "select schemaname from v$session where audsid = userenv('sessionid')";
	
	public String getCurrentSchema(WbConnection con)
	{
		String schema = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try
		{
			stmt = con.getSqlConnection().prepareStatement(CURRENT_SCHEMA_SQL);
			rs = stmt.executeQuery();
			if (rs.next())
			{
				schema = rs.getString(1);
			}
		}
		catch (Exception e)
		{
			schema = con.getCurrentUser();
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return schema;
	}
	
	private String ERROR_QUERY = "SELECT line, position, text " +
		"  FROM all_errors  " +
		" WHERE owner = ?  " +
		"   AND type = ? " +
		"   AND name = ? ";
	
	/**
	 *	Return the errors reported in the all_errors table for Oracle.
	 *	This method can be used to obtain error information after a CREATE PROCEDURE
	 *	or CREATE TRIGGER statement has been executed.
	 *
	 *	@return extended error information if the current DBMS is Oracle. An empty string otherwise.
	 */
	public String getErrorInfo(String schema, String objectName, String objectType)
	{
		if (objectType == null || objectName == null) return "";
		
		PreparedStatement stmt = null;
		ResultSet rs = null;
		
		StrBuffer result = new StrBuffer(250);
		try
		{
			if (objectName.indexOf('.') > -1)
			{
				schema = objectName.substring(0, objectName.indexOf('.'));
			}
			else if (schema == null)
			{
				schema = this.connection.getSqlConnection().getMetaData().getUserName();
			}
			stmt = this.connection.getSqlConnection().prepareStatement(ERROR_QUERY);
			stmt.setString(1, schema.toUpperCase());
			stmt.setString(2, objectType.toUpperCase());
			stmt.setString(3, objectName.toUpperCase());
			
			rs = stmt.executeQuery();
			int count = 0;
			while (rs.next())
			{
				if (count > 0) result.append("\r\n");
				int line = rs.getInt(1);
				int pos = rs.getInt(2);
				String msg = rs.getString(3);
				result.append("Error at line ");
				result.append(line);
				result.append(", position ");
				result.append(pos);
				result.append(": ");
				result.append(msg);
				count ++;
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("OracleMetadata.getExtendedErrorInfo()", "Error retrieving error information",e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result.toString();
	}
	
	public void done()
	{
		this.closeStatement();
		if (this.columnStatement != null)
		{
			try
			{ this.columnStatement.close(); }
			catch (Throwable th)
			{}
		}
	}
}
