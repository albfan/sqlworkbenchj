/*
 * DeleteScriptGenerator.java
 *
 * Created on October 23, 2002, 2:59 PM
 */

package workbench.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;

/**
 *	Generates a SQL script to delete a record from the given table and 
 *	any dependent tables.
 * @author  workbench@kellerer.org
 */
public class DeleteScriptGenerator
{
	private WbConnection connection;
	private String tablename;
	private String schemaname;
	private String catalogname;
	private Map columnValues;
	private TableDependency dependency;
	private DbMetadata meta;
	
	public DeleteScriptGenerator(WbConnection aConnection)
		throws SQLException
	{
		this.connection = aConnection;
		this.meta = this.connection.getMetadata();
		this.dependency = new TableDependency();
		this.dependency.setConnection(this.connection);
	}
	
	public void setTable(String aCatalog, String aSchema, String aTable)
	{
		if (aTable == null || aTable.trim().length() == 0) throw new IllegalArgumentException("The table name may not be empty");
		this.tablename = aTable;
		this.catalogname = aCatalog;
		this.schemaname = aSchema;
		this.dependency.setTableName(aCatalog, aSchema, aTable);
	}
	
	public void setValues(Map colValues)
	{
		this.columnValues = colValues;
	}
	
	public String createScript()
	{
		ArrayList statements = new ArrayList();
		this.dependency.readDependencyTree();
		
		return "";
	}

	private String generateDelete(DependencyNode parent, DependencyNode current)
	{
	}
	
	/**
	 *	Read the FK key values for current from parent
	 */
	private Map getColumnValues(DependencyNode parent, DependencyNode current)
	{
	}
	
	private String getDeleteStatement(Map colValues, String aCatalog, String aSchema, String aTable)
	{
		DataStore tableDefinition;
		try
		{
			tableDefinition = this.meta.getTableDefinition(aCatalog, aSchema, aTable);
		}
		catch (Exception e)
		{
			return "";
		}
		StringBuffer sql = new StringBuffer(500);
		sql.append("DELETE FROM ");
		if (aSchema != null && aSchema.trim().length() > 0)
		{
			sql.append(SqlUtil.quoteObjectname(aSchema));
			sql.append('.');
		}
		sql.append(SqlUtil.quoteObjectname(aTable));
		sql.append(" WHERE ");
		Iterator itr = colValues.entrySet().iterator();
		boolean first = true;
		while (itr.hasNext())
		{
			Map.Entry entry = (Map.Entry)itr.next();
			String column = (String)entry.getKey();
			column = this.meta.adjustObjectname(column);
			String value = (String)entry.getValue();
			int type = this.getColumnType(tableDefinition, column);
			if (!first) 
			{
				sql.append(" AND ");
			}
			else
			{
				first = false;
			}
			sql.append(SqlUtil.quoteObjectname(column));
			sql.append(" = ");
			boolean charType = (type == Types.VARCHAR || type == Types.CHAR);
			if (charType)	sql.append('\'');
			sql.append(value);
			if (charType)	sql.append('\'');
		}
		return sql.toString();
	}
	
	private int getColumnType(DataStore tableDef, String aColname)
	{	
		for (int i=0; i < tableDef.getRowCount(); i ++)
		{
			String col = tableDef.getValueAsString(i, DbMetadata.COLUMN_IDX_TABLE_DEFINITION_COL_NAME);
			if (aColname.equals(col))
			{
				String t = tableDef.getValueAsString(i, DbMetadata.COLUMN_IDX_TABLE_DEFINITION_TYPE_ID);
				int type = 0;
				try { type = Integer.parseInt(t); } catch (Exception e) {}
				return type;
			}
		}
		return -1;
	}

	public static void main(String args[])
	{
		Connection con = null;
		try
		{
			Class.forName("com.inet.tds.TdsDriver");
			//Class.forName("oracle.jdbc.OracleDriver");
			con = DriverManager.getConnection("jdbc:inetdae:demsqlvisa02:1433?database=visa_cpl_test", "visa", "savivisa");
			//con = DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:oradb", "auto", "auto");
			WbConnection wb = new WbConnection(con);
			DeleteScriptGenerator gen = new DeleteScriptGenerator(wb);
			HashMap m = new HashMap();
			m.put("bidid", "1");
			m.put("configid", "2");
			String sql = gen.getDeleteStatement(m, "visa_cpl_test", "dbo", "visa_config");
			System.out.println(sql);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try { con.close(); } catch (Throwable th) {}
		}
	}
	
}
