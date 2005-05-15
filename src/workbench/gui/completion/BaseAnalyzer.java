/*
 * BaseAnalyzer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.completion;

import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import workbench.db.DbMetadata;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.resource.ResourceMgr;
import workbench.util.StringUtil;

/**
 * Base class to analyze a SQL statement to find out what kind and which 
 * objects should be included in the auto-completion window
 * @author support@sql-workbench.net
 */
public abstract class BaseAnalyzer
{
	protected static final int NO_CONTEXT = -1;
	
	// List the available tables
	protected static final int CONTEXT_TABLE_LIST = 1;

	// List the columns for a table
	protected static final int CONTEXT_COLUMN_LIST = 2;

	// List the tables that are available in the FROM list
	protected static final int CONTEXT_FROM_LIST = 3;
	
	protected static final int CONTEXT_TABLE_OR_COLUMN_LIST = 4;
	private String typeFilter;
	protected WbConnection dbConnection;
	protected final String sql;
	protected final int pos;
	protected int context;
	protected TableIdentifier tableForColumnList;
	protected String schemaForTableList;

	protected List elements;
	protected String title;
	
	public BaseAnalyzer(WbConnection conn, String statement, int cursorPos)
	{
		this.dbConnection = conn;
		this.sql = statement;
		this.pos = cursorPos;
	}

	public void retrieveObjects()
	{
		// reset current state
		this.elements = null;
		this.context = NO_CONTEXT;
		this.typeFilter = null;
		
		// this should not be done in the constructor as the 
		// sub-classes might to import initializations there
		this.checkContext();
		this.buildResult();
	}
	
	public String getTitle() { return this.title; }
	public List getData() { return this.elements; }
	
	protected abstract void checkContext();
	
	private void buildResult()
	{
		if (context == CONTEXT_TABLE_OR_COLUMN_LIST && tableForColumnList != null)
		{
			List cols = retrieveColumns();
			if (cols == null || cols.size() == 0)
			{
				retrieveTables();
			}
		}
		else if (context == CONTEXT_TABLE_LIST)
		{
			if (this.elements == null) retrieveTables();
		}
		else if (context == CONTEXT_COLUMN_LIST)
		{
			retrieveColumns();
		}
		else if (context == CONTEXT_FROM_LIST)
		{
			// element list has already been filled
			this.title = ResourceMgr.getString("LabelCompletionListTables");
		}
		else
		{
			// no proper sql found
			this.elements = Collections.EMPTY_LIST;
			this.title = null;
			Toolkit.getDefaultToolkit().beep();
		}
	}

	private void retrieveTables()
	{
		Set tables = this.dbConnection.getObjectCache().getTables(schemaForTableList, typeFilter);
		if (schemaForTableList == null)
		{
			this.title = ResourceMgr.getString("LabelCompletionListTables");
		}
		else
		{
			this.title = schemaForTableList + ".*";
		}
		this.elements = new ArrayList(tables.size());
		this.elements.addAll(tables);
	}
	
	private List retrieveColumns()
	{
		if (tableForColumnList == null) return null;
		String s = tableForColumnList.getSchema();
		if (s == null)
		{
			tableForColumnList.setSchema(this.dbConnection.getMetadata().getCurrentSchema());
		}
		tableForColumnList.adjustCase(this.dbConnection);
		List cols = this.dbConnection.getObjectCache().getColumns(tableForColumnList);
		if (cols != null && cols.size() > 0)
		{
			this.title = tableForColumnList.getTable() + ".*";
			this.elements = cols;
		}
		return cols;
	}
	
	protected void setTableTypeFilter(String filter)
	{
		this.typeFilter = filter;
	}
	
	protected String getWordLeftOfCursor(String sql, int pos)
	{
		String word = null;
		int len = sql.length();
		int start = pos - 1;
		int end = pos;
		if (pos > len) 
		{
			start = len - 1;
			end = len;
		}

		// find the first non-whitespace character left of the cursor
		char c = 0;
		int p = pos;
		while ( p > 1 && Character.isWhitespace(sql.charAt(p)))
		{
			p--;
		}

		if (p > 1)
		{
			int startOfQualifier = StringUtil.findPreviousWhitespace(sql, p);
			if (startOfQualifier > 0)
			{
				word = sql.substring(startOfQualifier, p).trim();
			}
		}
		return word;
	}
	
	protected String getQualifierLeftOfCursor(String sql, int pos)
	{
		String qualifier = null;
		int len = sql.length();
		int start = pos - 1;
		if (pos > len) 
		{
			start = len - 1;
		}
			
		char c = sql.charAt(start);
		if (c == '.')
		{
			// find qualifier
			int startOfQualifier = StringUtil.findPreviousWhitespace(sql, start);
			if (startOfQualifier > 0)
			{
				qualifier = sql.substring(startOfQualifier, start).trim();
			}
		}
		return qualifier;
	}
	
}
