/*
 * ReferenceTableNavigation.java
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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import workbench.log.LogMgr;
import workbench.storage.ColumnData;
import workbench.storage.SqlLiteralFormatter;

/**
 * A class to generate SQL SELECT statements that will retrieve the parent
 * or child records regarding the foreign key constraints for the given
 * table.
 * 
 * @author Thomas Kellerer
 */
public class ReferenceTableNavigation
{
	private TableIdentifier baseTable;
	private WbConnection dbConn;
	private SqlLiteralFormatter formatter;
	private TableDependency dependencyTree;
	
	public ReferenceTableNavigation(TableIdentifier table, WbConnection con)
	{
		this.baseTable = table;
		this.dbConn = con;
		this.formatter = new SqlLiteralFormatter(dbConn);
	}

	public void readTreeForChildren()
	{
		readDependencyTree(true);
	}
	
	public void readTreeForParents()
	{
		readDependencyTree(false);
	}
	
	protected void readDependencyTree(boolean forChildren)
	{
		dependencyTree = new TableDependency(this.dbConn, this.baseTable);
		dependencyTree.setRetrieveDirectChildrenOnly(true);
		if (forChildren)
		{
			dependencyTree.readTreeForChildren();
		}
		else
		{
			dependencyTree.readTreeForParents();
		}
	}

	public DependencyNode getNodeForTable(TableIdentifier tbl, String fkName)
	{
		if (this.dependencyTree == null) return null;
		if (tbl == null) return null;
		
		TableIdentifier table = tbl.createCopy();
		if (table.getSchema() == null)
		{
			table.setSchema(this.dbConn.getMetadata().getSchemaToUse());
		}
		if (table.getCatalog() == null)
		{
			table.setCatalog(this.dbConn.getMetadata().getCurrentCatalog());
		}
		table.adjustCase(dbConn);
		return dependencyTree.findLeafNodeForTable(table, fkName);
	}

	public TableDependency getTree()
	{
		return this.dependencyTree;
	}
	
	/**
	 * Return a SELECT statement to retrieve the child rows referenced by the given
	 * table column values
	 *
	 * @param tbl the table for which the parent rows should be retrieved
	 * @param fkName the name of the foreign key that links tbl
	 * @param values the values for which the SQL statement should be created
	 */
	public String getSelectForChild(TableIdentifier tbl, String fkName, List<List<ColumnData>> values)
	{
		return generateSelect(tbl, fkName, true, values);
	}

	/**
	 * Return a SELECT statement to retrieve the parent rows referencing by the given
	 * table column values
	 * 
	 * @param tbl the table for which the parent rows should be retrieved
	 * @param fkName the name of the foreign key that links tbl
	 * @param values the values for which the SQL statement should be created
	 */
	public String getSelectForParent(TableIdentifier tbl, String fkName, List<List<ColumnData>> values)
	{
		return generateSelect(tbl, fkName, false, values);
	}
	
	private String generateSelect(TableIdentifier tbl, String fkName, boolean forChildren, List<List<ColumnData>> values)
	{
		String result = null;
		try
		{
			if (this.dependencyTree == null) this.readDependencyTree(forChildren);
			
			DependencyNode node = getNodeForTable(tbl, fkName);
			
			StringBuilder sql = new StringBuilder(100);
			sql.append("SELECT * \nFROM ");
			sql.append(node.getTable().getTableExpression(this.dbConn));
			sql.append("\nWHERE ");
			addWhere(sql, node, values);
			result = sql.toString();
		}
		catch (Exception e)
		{
			LogMgr.logError("TableNavigation.getSelectsForParents()", "Error retrieving parent tables", e);
		}
		return result;
	}	
	
	private void addWhere(StringBuilder sql, DependencyNode node, List<List<ColumnData>> values)
	{
		Map<String, String> colMapping = node.getColumns();
		
		Iterator<List<ColumnData>> rowItr = values.iterator();
		while (rowItr.hasNext())
		{
			List<ColumnData> row = rowItr.next();
			sql.append('(');
			Iterator<Map.Entry<String, String>> colItr = colMapping.entrySet().iterator();
			while (colItr.hasNext())
			{
				Map.Entry<String, String> entry = colItr.next();
				String childColumn = entry.getKey();
				String parentColumn = entry.getValue();
				ColumnData data = getPkValue(row, parentColumn);
				if (data == null) continue;
				sql.append(childColumn);
				if (data.isNull())
				{
					sql.append(" IS NULL");
				}
				else
				{
					sql.append(" = ");
					sql.append(formatter.getDefaultLiteral(data));
				}
				if (colItr.hasNext())
				{
					sql.append(" AND ");
				}
			}
			sql.append(')');
			if (rowItr.hasNext())
			{
				sql.append("\n   OR ");
			}
		}
	}
	
	private ColumnData getPkValue(List<ColumnData> colData, String column)
	{
		for (ColumnData data : colData)
		{
			if (data.getIdentifier().getColumnName().equalsIgnoreCase(column)) return data;
		}
		return null;
	}
}
