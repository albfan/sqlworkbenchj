/*
 * IndexDefinition.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.db;

/**
 * A class to store the defintion of a database index.
 * @author  info@sql-workbench.net
 */
public class IndexDefinition
{
	private String expression;
	private boolean isPK = false;
	private boolean isUnique = false;
	private String indexName;
	
	public IndexDefinition(String exp)
	{
		this(null, exp);
	}
	
	public IndexDefinition(String name, String exp)
	{
		if (exp == null) throw new NullPointerException("Expression may not be null");
		this.indexName = name;
		this.expression = exp;
	}

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
