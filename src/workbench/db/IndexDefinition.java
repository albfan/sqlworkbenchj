/*
 * IndexDefinition.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A class to store the defintion of a database index.
 * @author  support@sql-workbench.net
 */
public class IndexDefinition
{
	private String expression;
	private boolean isPK = false;
	private boolean isUnique = false;
	private String indexName;
	private String indexType;
	private List<IndexColumn> columns = new ArrayList<IndexColumn>();
	
	public IndexDefinition(String name, String exp)
	{
		this.indexName = name;
		this.expression = exp;
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

	public List<IndexColumn> getColumns()
	{
		return Collections.unmodifiableList(columns);
	}
	
	public String getIndexType() { return this.indexType; }

	public void setExpression(String exp) { this.expression = exp; }
	public String getExpression() 
	{ 
		if (this.expression == null)
		{
			return buildExpression();
		}
		return this.expression; 
	}
	
	private String buildExpression()
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
}
