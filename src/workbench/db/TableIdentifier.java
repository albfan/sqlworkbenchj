/*
 * TableIdentifier.java
 *
 * Created on 31. Oktober 2002, 21:37
 */

package workbench.db;

import workbench.util.SqlUtil;

/**
 *
 * @author  workbench@kellerer.org
 */
public class TableIdentifier
{
	
	private String tablename;
	private String schema;
	private String catalog;
	private String expression;
	
	/** Creates a new instance of TableIdentifier */
	public TableIdentifier(String aCatalog, String aSchema, String aTable)
	{
		this.setTable(aTable);
		this.setCatalog(aCatalog);
		this.setSchema(aSchema);
	}

	public String getTableExpression()
	{
		if (this.expression == null) this.initExpression();
		return this.expression;
	}
	
	private void initExpression()
	{
		StringBuffer result = new StringBuffer(30);
		if (this.schema != null)
		{
			result.append(SqlUtil.quoteObjectname(this.schema));
			result.append('.');
		}
		if (this.catalog != null)
		{
			result.append(SqlUtil.quoteObjectname(this.catalog));
			result.append('.');
		}
		result.append(SqlUtil.quoteObjectname(this.tablename));
		this.expression = result.toString();
	}
	
	public String getTable() { return this.tablename; }
	
	public void setTable(String aTable)
	{
		if (aTable == null || aTable.trim().length() == 0) 
			throw new IllegalArgumentException("Table name may not be null");
		this.tablename = aTable;
		this.expression = null;
	}
	
	public String getSchema() { return this.schema; }
	public void setSchema(String aSchema)
	{
		
		if (aSchema != null && aSchema.trim().length() == 0)
		{
			this.schema = null;
		}
		else
		{
			this.schema = aSchema;
		}
		this.expression = null;
	}

	public String getCatalog() { return this.catalog; }
	public void setCatalog(String aCatalog)
	{
		if (aCatalog != null && aCatalog.trim().length() == 0)
		{
			this.catalog = null;
		}
		else
		{
			this.catalog = aCatalog;
		}
		this.expression = null;
	}
	
	public String toString() { return this.getTableExpression(); }
	
	public boolean equals(Object other)
	{
		if (other instanceof TableIdentifier)
		{
			TableIdentifier t = (TableIdentifier)other;
			return this.getTableExpression().equals(t.getTableExpression());
		}
		return false;
	}
}	