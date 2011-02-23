/*
 * IndexDefinition.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
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
 * @author  Thomas Kellerer
 */
public class IndexDefinition
	implements DbObject
{
	private boolean isPK;
	private boolean isUnique;
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

	@Override
	public String getComment()
	{
		return comment;
	}

	@Override
	public void setComment(String c)
	{
		comment = c;
	}

	@Override
	public String getSchema()
	{
		return baseTable.getSchema();
	}

	@Override
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

	@Override
	public String getFullyQualifiedName(WbConnection conn)
	{
		return SqlUtil.buildExpression(conn, null, getSchema(), indexName);
	}

	@Override
	public String getObjectExpression(WbConnection conn)
	{
		return SqlUtil.buildExpression(conn, null, getSchema(), indexName);
	}

	@Override
	public String getDropStatement(WbConnection con, boolean cascade)
	{
		return null;
	}

	@Override
	public String getObjectNameForDrop(WbConnection con)
	{
		return getFullyQualifiedName(con);
	}

	@Override
	public String getObjectName(WbConnection conn)
	{
		return conn.getMetadata().quoteObjectname(indexName);
	}

	@Override
	public String getObjectType()
	{
		return "INDEX";
	}

	@Override
	public String getObjectName()
	{
		return getName();
	}

	public List<IndexColumn> getColumns()
	{
		return columns;
	}

	public String getIndexType() { return this.indexType; }

	@Override
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

	@Override
  public int hashCode()
  {
    int hash = 71 * 7 + (this.indexName != null ? this.indexName.hashCode() : 0);
    return hash;
  }

	@Override
	public boolean equals(Object o)
	{
		if (o instanceof IndexDefinition)
		{
			IndexDefinition other = (IndexDefinition)o;
			boolean equals = false;
			if (this.isPK && other.isPK || this.isUnique && other.isUnique)
			{
				// for PK indexes the order of the columns in the index does not matter
				// so we consider the same list of columns equals even if they have different order
				for (IndexColumn col : columns)
				{
					if (!other.columns.contains(col))
					{
						equals = false;
						break;
					}
				}
				equals = true;
			}
			else
			{
				equals = this.columns.equals(other.columns);
			}

			if (equals)
			{
				equals = (this.isPK == other.isPK) && (this.isUnique == other.isUnique);
			}
			return equals;
		}
		else if (o instanceof String)
		{
			return this.getExpression().equals((String)o);
		}
		return false;
	}

	@Override
	public CharSequence getSource(WbConnection con)
	{
		if (con == null) return null;
		IndexReader reader = con.getMetadata().getIndexReader();
		return reader.getIndexSource(baseTable, this, null);
	}
}
