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
	
	public IndexDefinition(String name, String exp)
	{
		this.indexName = name;
		this.expression = exp;
	}

	public void addColumn(String colExpression)
	{
		if (this.expression != null && this.expression.trim().length() > 0)
		{
			this.expression = this.expression + "," + colExpression;
		}
		else
		{
			this.expression = colExpression;
		}
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
	
	public String getIndexType() { return this.indexType; }

	public void setExpression(String exp) { this.expression = exp; }
	public String getExpression() { return this.expression; }
	public String getName() { return this.indexName; }
	public void setPrimaryKeyIndex(boolean flag) { this.isPK = flag; }
	public boolean isPrimaryKeyIndex() { return this.isPK; }
	
	public void setUnique(boolean flag) { this.isUnique = flag; }
	public boolean isUnique() { return this.isUnique; }
	
	public boolean equals(Object o)
	{
		if (o instanceof IndexDefinition)
		{
			IndexDefinition other = (IndexDefinition)o;
			boolean equal = this.expression.equals(other.expression);
			if (equal)
			{
				equal = (this.isPK == other.isPK) && (this.isUnique == other.isUnique);
			}
			return equal;
		}
		else if (o instanceof String)
		{
			return this.expression.equals((String)o);
		}
		return false;
	}
}
