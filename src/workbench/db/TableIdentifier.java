/*
 * TableIdentifier.java
 *
 * Created on 31. Oktober 2002, 21:37
 */

package workbench.db;

import workbench.resource.ResourceMgr;
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
	private boolean isNewTable = false;

	public TableIdentifier(String aName)
	{
		this.expression = null;
		this.isNewTable = false;
		this.setTable(aName);
	}

	/**
	 *	Initialize a TableIdentifier for a new (to be defined) table
	 */
	public TableIdentifier()
	{
		this.expression = null;
		this.schema = null;
		this.catalog = null;
		this.tablename = null;
		this.isNewTable = true;
	}

	public TableIdentifier(String aSchema, String aTable)
	{
		this.setCatalog(null);
		this.setTable(aTable);
		this.setSchema(aSchema);
	}

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
		if (this.isNewTable)
		{
			if (this.tablename == null)
			{
				this.expression = ResourceMgr.getString("TxtNewTableIdentifier");
			}
			else
			{
				this.expression = this.tablename;
			}
			return;
		}

		StringBuffer result = new StringBuffer(30);
		if (this.schema != null)
		{
			result.append(SqlUtil.quoteObjectname(this.schema));
			result.append('.');
		}
		result.append(SqlUtil.quoteObjectname(this.tablename));
		this.expression = result.toString();
	}

	public String getTable() { return this.tablename; }

	public void setTable(String aTable)
	{
		if (!this.isNewTable && (aTable == null || aTable.trim().length() == 0))
			throw new IllegalArgumentException("Table name may not be null");
		
		int pos = aTable.indexOf('.');
		if (pos > -1)
		{
			this.schema = aTable.substring(0, pos).trim();
			this.tablename = aTable.substring(pos + 1).trim();
		}
		else
		{
			this.tablename = aTable.trim();
		}
		this.expression = null;
	}

	public String getSchema() { return this.schema; }
	public void setSchema(String aSchema)
	{
		if (this.isNewTable) return;

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
		if (this.isNewTable) return;

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

	public String toString()
	{
		if (this.isNewTable)
		{
			if (this.tablename == null)
			{
				return this.getTableExpression();
			}
			else
			{
				return "(+) " + this.tablename;
			}
		}
		else
		{
			return this.getTableExpression();
		}
	}
	public boolean isNewTable() { return this.isNewTable; }

	public void setNewTable(boolean flag)
	{
		this.expression = null;
		this.isNewTable = flag;
	}

	public boolean equals(Object other)
	{
		if (other instanceof TableIdentifier)
		{
			boolean result = false;
			TableIdentifier t = (TableIdentifier)other;
			if (this.isNewTable && t.isNewTable)
			{
				result = true;
			}
			else if (this.isNewTable || t.isNewTable)
			{
				result = false;
			}
			else
			{
				result = this.getTableExpression().equals(t.getTableExpression());
			}
			return result;
		}
		return false;
	}
}