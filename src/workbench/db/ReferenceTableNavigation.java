/*
 * TableNavigation.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import workbench.db.TableIdentifier;
import workbench.log.LogMgr;
import workbench.storage.ColumnData;
import workbench.storage.SqlLiteralFormatter;

/**
 * A class to generate SQL SELECT statements that will retrieve the parent
 * or child records regarding the foreign key constraints for the given
 * table.
 * 
 * @author support@sql-workbench.net
 */
public class ReferenceTableNavigation
{
	private TableIdentifier baseTable;
	private WbConnection dbConn;
	private List<List<ColumnData>> pkValues;
	private SqlLiteralFormatter formatter;
	
	public ReferenceTableNavigation(TableIdentifier table, List<List<ColumnData>> values, WbConnection con)
	{
		this.baseTable = table;
		this.pkValues = values;
		this.dbConn = con;
		this.formatter = new SqlLiteralFormatter(dbConn);
	}

	public List<String> getSelectsForParents()
	{
		return generateSelects(false);
	}
	
	public List<String> getSelectsForChildren()
	{
		return generateSelects(true);
	}
	
	private List<String> generateSelects(boolean forChildren)
	{
		List<String> result = new LinkedList<String>();
		try
		{
			TableDependency dep = new TableDependency();
			dep.setMaxLevel(1);
			dep.setConnection(this.dbConn);
			dep.setTable(this.baseTable);
			dep.readDependencyTree(forChildren);
			List<DependencyNode> children = dep.getLeafs();
			Iterator<DependencyNode> itr = children.iterator();
			while (itr.hasNext())
			{
				DependencyNode node = itr.next();
				StringBuilder sql = new StringBuilder(100);
				sql.append("SELECT * \nFROM ");
				sql.append(node.getTable().getTableExpression(this.dbConn));
				sql.append("\nWHERE ");
				addWhere(sql, node);
				result.add(sql.toString());
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("TableNavigation.getSelectsForParents()", "Error retrieving parent tables", e);
			return Collections.EMPTY_LIST;
		}
		return result;
	}	
	
	private void addWhere(StringBuilder sql, DependencyNode node)
	{
		Map<String, String> colMapping = node.getColumns();
		
		Iterator<List<ColumnData>> rowItr = pkValues.iterator();
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
					sql.append("IS NULL");
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
