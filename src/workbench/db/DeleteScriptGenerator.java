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
import java.util.List;
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
	private DataStore tableDefinition;
	
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
		this.tablename = this.meta.adjustObjectname(aTable);
		if (this.tablename.indexOf('.') > 0)
		{
			this.tablename = this.tablename.substring(this.tablename.lastIndexOf('.') + 1);
		}
		
		this.catalogname = this.meta.adjustObjectname(aCatalog);
		this.schemaname = this.meta.adjustObjectname(aSchema);
		
		if (this.schemaname == null)
		{
			try
			{
				this.schemaname = this.meta.getSchemaForTable(this.tablename);
			}
			catch (Exception e)
			{
				this.schemaname = null;
			}
		}
		
		this.dependency.setTableName(this.catalogname, this.schemaname, this.tablename);
		try
		{
			this.tableDefinition = this.meta.getTableDefinition(this.catalogname, this.schemaname, this.tablename);
		}
		catch (Exception e)
		{
			//
		}
	}
	
	public void setValues(Map colValues)
	{
		this.columnValues = colValues;
	}
	
	public String createScript()
	{
		ArrayList parents = new ArrayList();
		this.dependency.readDependencyTree(true);
		List leafs = this.dependency.getLeafs();
		StringBuffer sql = new StringBuffer(2000);
		DependencyNode p, node; 
		for (int i=0; i < leafs.size(); i++)
		{
			node = (DependencyNode)leafs.get(i);
			this.addDeleteStatement(sql, node);
			p = node.getParent();
			while (p != null)
			{
				if (!isMasterTable(p) && !parents.contains(p))
				{
					parents.add(p);
				}
				p = p.getParent();
			}
			sql.append("\n\n");
		}
		for (int i=0; i < parents.size(); i++)
		{
			p = (DependencyNode)parents.get(i);
			this.addDeleteStatement(sql, p);
			sql.append("\n\n");
		}
		DependencyNode root = this.dependency.getRootNode();
		sql.append("DELETE FROM ");
		sql.append(this.createTableExpression(root.getCatalog(), root.getSchema(), root.getTable()));
		sql.append(" WHERE ");
		this.addRootTableWhere(sql);
		sql.append(';');
		return sql.toString();
	}

	private void addDeleteStatement(StringBuffer sql, DependencyNode node)
	{
		if (node == null) return;
		String catalog = node.getCatalog();
		String schema = node.getSchema();
		String table = node.getTable();
		
		sql.append("DELETE FROM ");
		sql.append(this.createTableExpression(catalog, schema, table));
		sql.append("\n   WHERE ");
		
		this.addParentWhere(sql, node);
		sql.append(';');
	}

	private void addParentWhere(StringBuffer sql, DependencyNode node)
	{
		this.addParentWhere(sql, node, null);
	}
	private void addParentWhere(StringBuffer sql, DependencyNode node, String nodeColumn)
	{
		try
		{
			DependencyNode parent = node.getParent();
			if (parent != null && !isMasterTable(parent))
			{
				sql.append(" (");
				String parentTable = parent.getTable();
				String parentSchema = parent.getSchema();
				String parentCatalog = parent.getCatalog();

				Map columns = node.getColumns();
				Iterator itr = columns.entrySet().iterator();
				int count = 0;
				while (itr.hasNext())
				{
					Map.Entry entry = (Map.Entry)itr.next();
					String column = (String)entry.getKey();
					column = this.meta.adjustObjectname(column);
					String parentColumn = (String)entry.getValue();
					if (nodeColumn != null && !nodeColumn.equals(column)) continue;
					if (count > 0) sql.append("\n          AND ");
					sql.append("(");
					sql.append(column);
					sql.append(" IN ( SELECT ");
					sql.append(parentColumn);
					sql.append(" FROM ");
					sql.append(this.createTableExpression(parentCatalog, parentSchema, parentTable));
					sql.append(" WHERE ");
					this.addParentWhere(sql, parent, column);
					sql.append(")) ");
					count ++;
				}
				sql.append(" )");
			}
			else
			{
				this.addRootTableWhere(sql);
			}
		}
		catch (Throwable th)
		{
			th.printStackTrace();
		}
	}
	
	private boolean isMasterTable(DependencyNode node)
	{
		String table = node.getTable();
		String schema = node.getSchema();
		String catalog = node.getCatalog();
		
		if (schema == null) schema = "";
		if (table == null) table = "";
		return (schema.equals(this.schemaname) && table.equals(this.tablename));
	}
	private void addRootTableWhere(StringBuffer sql)
	{
		Iterator itr = this.columnValues.entrySet().iterator();
		boolean first = true;
		while (itr.hasNext())
		{
			Map.Entry entry = (Map.Entry)itr.next();
			String column = (String)entry.getKey();
			column = this.meta.adjustObjectname(column);
			Object data = entry.getValue();
      String value = data.toString();
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
	}
	private StringBuffer createTableExpression(String aCatalog, String aSchema, String aTable)
	{
		StringBuffer buff = new StringBuffer(100);
		if (aSchema != null && aSchema.length() > 0)
		{
			buff.append(SqlUtil.quoteObjectname(this.meta.adjustObjectname(aSchema)));
			buff.append('.');
		}
		buff.append(SqlUtil.quoteObjectname(this.meta.adjustObjectname(aTable)));
		return buff;
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
			//con = DriverManager.getConnection("jdbc:inetdae:demsqlvisa02:1433?database=visa_cpl_test", "visa", "savivisa");
			con = DriverManager.getConnection("jdbc:inetdae:reosqlpro08:1433?database=visa", "visa", "savivisa");
			//con = DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:oradb", "auto", "auto");
			WbConnection wb = new WbConnection(con);
			DeleteScriptGenerator gen = new DeleteScriptGenerator(wb);
			HashMap m = new HashMap();
			//m.put("custid", "100");
			m.put("bidid", "345");
			//m.put("configid", "2");
			//gen.setTable("visa_cpl_test", "dbo", "visa_bid");
			gen.setTable("visa", "dbo", "visa_bid");
			//gen.setTable(null, "auto", "expense_type");
			gen.setValues(m);
			String sql = gen.createScript();

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
