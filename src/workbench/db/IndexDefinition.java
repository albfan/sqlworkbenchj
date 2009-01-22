/*
 * IndexDefinition.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.util.ArrayList;
import java.util.List;
import workbench.util.SqlUtil;

/**
 * A class to store the defintion of a database index.
 * @author  support@sql-workbench.net
 */
public class IndexDefinition
	implements DbObject
{
	private boolean isPK = false;
	private boolean isUnique = false;
	private String indexName;
	private String indexType;
	private TableIdentifier baseTable;
	private List<IndexColumn> columns = new ArrayList<IndexColumn>();
	private String comment;

	public IndexDefinition(TableIdentifier table, String name)
	{
		this.indexName = name;
		this.baseTable = table;
	}

	public String getComment()
	{
		return comment;
	}

	public void setComment(String c)
	{
		comment = c;
	}

	public String getSchema()
	{
		return baseTable.getSchema();
	}

	public String getCatalog()
	{
		return null;
	}

	public void addColumn(String column, String direction)
	{
		this.columns.add(new IndexColumn(column, direction));
	}

	public void setIndexType(String type)
	{
		if (type == null)
		{
			this.indexType = "NORMAL";
		}
		else
		{
			this.indexType = type;
		}
	}

	public void setBaseTable(TableIdentifier table)
	{
		this.baseTable = table;
	}
	
	public String getObjectExpression(WbConnection conn)
	{
		return SqlUtil.buildExpression(conn, null, getSchema(), indexName);
	}

	public String getObjectNameForDrop(WbConnection con)
	{
		return getObjectName(con);
	}

	public String getObjectName(WbConnection conn)
	{
		return conn.getMetadata().quoteObjectname(indexName);
	}

	public String getObjectType()
	{
		return "INDEX";
	}
	
	public String getObjectName()
	{
		return getName();
	}
	
	public List<IndexColumn> getColumns()
	{
		return columns;
	}
	
	public String getIndexType() { return this.indexType; }

	public String toString()
	{
		return getExpression();
	}
	
	public String getExpression()
	{
		StringBuilder result = new StringBuilder(this.columns.size() * 10);
		for (int i=0; i < this.columns.size(); i++)
		{
			if (i > 0) result.append(", ");
			result.append(columns.get(i).getExpression());
		}
		return result.toString();
	}
	
	public String getName() { return this.indexName; }
	public void setPrimaryKeyIndex(boolean flag) { this.isPK = flag; }
	public boolean isPrimaryKeyIndex() { return this.isPK; }
	
	public void setUnique(boolean flag) { this.isUnique = flag; }
	public boolean isUnique() { return this.isUnique; }

  public int hashCode()
  {
    int hash = 71 * 7 + (this.indexName != null ? this.indexName.hashCode() : 0);
    return hash;
  }
	
	public boolean equals(Object o)
	{
		if (o instanceof IndexDefinition)
		{
			IndexDefinition other = (IndexDefinition)o;
			boolean equal = this.getExpression().equals(other.getExpression());
			if (equal)
			{
				equal = (this.isPK == other.isPK) && (this.isUnique == other.isUnique);
			}
			return equal;
		}
		else if (o instanceof String)
		{
			return this.getExpression().equals((String)o);
		}
		return false;
	}
	
	public CharSequence getSource(WbConnection con)
	{
		if (con == null) return null;
		return con.getMetadata().buildIndexSource(baseTable, indexName, isUnique, this.columns);
	}
}
