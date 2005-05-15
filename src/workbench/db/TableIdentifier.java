/*
 * TableIdentifier.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import workbench.resource.ResourceMgr;
import workbench.util.SqlUtil;

/**
 *
 * @author  support@sql-workbench.net
 */
public class TableIdentifier
	implements Comparable
{
	private String tablename;
	private String schema;
	private String catalog;
	private String expression;
	private boolean isNewTable = false;
	private String pkName;
	private String type;
	
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

	public TableIdentifier createCopy()
	{
		TableIdentifier copy = new TableIdentifier();
		copy.isNewTable = this.isNewTable;
		copy.pkName = this.pkName;
		copy.schema = this.schema;
		copy.tablename = this.tablename;
		copy.catalog = this.catalog;
		copy.expression = null;
		return copy;
	}
	
	public String getTableExpression()
	{
		if (this.expression == null) this.initExpression();
		return this.expression;
	}
	
	public String getTableExpression(WbConnection conn)
	{
		return this.buildTableExpression(conn);
	}

	private void initExpression()
	{
		this.expression = this.buildTableExpression(null);
	}
	
	private String buildTableExpression(WbConnection conn)
	{
		if (this.isNewTable)
		{
			if (this.tablename == null)
			{
				return ResourceMgr.getString("TxtNewTableIdentifier");
			}
			else
			{
				return this.tablename;
			}
		}

		StringBuffer result = new StringBuffer(30);
		if (conn == null)
		{
			if (this.schema != null)
			{
				result.append(SqlUtil.quoteObjectname(this.schema));
				result.append('.');
			}
			result.append(SqlUtil.quoteObjectname(this.tablename));
		}
		else
		{
			DbMetadata meta = conn.getMetadata();
			if (this.schema != null)
			{
				result.append(meta.quoteObjectname(this.schema));
				result.append('.');
			}
			result.append(meta.quoteObjectname(this.tablename));
		}
		return result.toString();
	}

	public void adjustCase(WbConnection conn)
	{
		DbMetadata meta = conn.getMetadata();
		if (meta.storesUpperCaseIdentifiers())
		{
			if (this.tablename != null) this.tablename = this.tablename.toUpperCase();
			if (this.schema != null) this.schema = this.schema.toUpperCase();
			if (this.catalog != null) this.catalog = this.catalog.toUpperCase();
		}
		else if (meta.storesLowerCaseIdentifiers())
		{
			if (this.tablename != null) this.tablename = this.tablename.toLowerCase();
			if (this.schema != null) this.schema = this.schema.toLowerCase();
			if (this.catalog != null) this.catalog = this.catalog.toLowerCase();
		}
		this.expression = null;
	}
	
	public String getTable() { return this.tablename; }

	public void setTable(String aTable)
	{
		if (!this.isNewTable && (aTable == null || aTable.trim().length() == 0))
			throw new IllegalArgumentException("Table name may not be null");

		if (aTable == null)
		{
			this.tablename = null;
			this.schema = null;
			this.expression = null;
			return;
		}
		
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

	public int compareTo(Object other)
	{
		if (other instanceof TableIdentifier)
		{
			TableIdentifier t = (TableIdentifier)other;
			return this.getTableExpression().compareTo(t.getTableExpression());
		}
		return -1;
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

	public String getPrimaryKeyName()
	{
		return this.pkName;
	}
	
	public void setPrimaryKeyName(String name)
	{
		this.pkName = name;
	}

	public String getType()
	{
		return type;
	}

	public void setType(String type)
	{
		this.type = type;
	}
}
