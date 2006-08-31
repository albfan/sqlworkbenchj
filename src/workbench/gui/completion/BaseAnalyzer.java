/*
 * BaseAnalyzer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.completion;

import java.awt.Toolkit;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.resource.ResourceMgr;
import workbench.util.StringUtil;
import workbench.util.TextlistReader;

/**
 * Base class to analyze a SQL statement to find out what kind and which 
 * objects should be included in the auto-completion window
 * @author support@sql-workbench.net
 */
public abstract class BaseAnalyzer
{
	public static final String QUALIFIER_DELIM = "<>!/{}\\#%[]'\"(),:;";
	public static final String WORD_DELIM = QUALIFIER_DELIM + "@";
	public static final String SELECT_WORD_DELIM = WORD_DELIM + ".";
	
	protected static final int NO_CONTEXT = -1;
	
	// List the available tables
	protected static final int CONTEXT_TABLE_LIST = 1;

	// List the columns for a table
	protected static final int CONTEXT_COLUMN_LIST = 2;

	// List the tables that are available in the FROM list
	protected static final int CONTEXT_FROM_LIST = 3;

	protected static final int CONTEXT_TABLE_OR_COLUMN_LIST = 4;
	
	// List keywords available at this point
	protected static final int CONTEXT_KW_LIST = 5;
	
	private final SelectAllMarker allColumnsMarker = new SelectAllMarker();
	private String typeFilter;
	protected String keywordFile;
	protected WbConnection dbConnection;
	protected final String sql;
	protected final int cursorPos;
	protected int context;
	protected TableIdentifier tableForColumnList;
	protected String schemaForTableList;
	protected boolean addAllMarker = false;
	protected List elements;
	protected String title;
	private boolean overwriteCurrentWord;
	private boolean appendDot; 
	private String columnPrefix;
	
	public BaseAnalyzer(WbConnection conn, String statement, int cursorPos)
	{
		this.dbConnection = conn;
		this.sql = statement;
		this.cursorPos = cursorPos;
	}

	public String getSelectionPrefix()
	{
		return this.columnPrefix;
	}
	
	protected void setColumnPrefix(String prefix)
	{
		this.columnPrefix = prefix;
	}
	
	public String getColumnPrefix() 
	{
		return this.columnPrefix;
	}
	
	protected void setOverwriteCurrentWord(boolean flag)
	{
		this.overwriteCurrentWord = flag;
	}
	
	protected void setAppendDot(boolean flag)
	{
		this.appendDot = flag;
	}
	
	public boolean appendDotToSelection()
	{
		return this.appendDot;
	}

	public boolean isKeywordList()
	{
		return this.context == CONTEXT_KW_LIST;
	}
	
	public boolean getOverwriteCurrentWord() 
	{ 
		return this.overwriteCurrentWord; 
	}
	
	public void retrieveObjects()
	{
		// reset current state
		this.elements = null;
		this.context = NO_CONTEXT;
		this.typeFilter = null;
		this.keywordFile = null;

		checkOverwrite();
		this.addAllMarker = false;

		// this should not be done in the constructor as the 
		// sub-classes might need to do important initializations durin initialization
		// and before checkContext is called
		this.checkContext();
		this.buildResult();
	}
	
	public String getTitle() { return this.title; }
	public List getData() { return this.elements; }
	
	protected abstract void checkContext();
	
	protected boolean between(int toTest, int start, int end)
	{
		if (start == -1 || end == -1) return false;
		return (toTest > start && toTest < end);
	}
	
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
			this.title = ResourceMgr.getString("LblCompletionListTables");
		}
		else if (context == CONTEXT_KW_LIST)
		{
			// element list has already been filled
			this.title = ResourceMgr.getString("LblCompletionListKws");
			this.elements = readKeywords();
		}
//		else if (context == CONTEXT_INDEX_LIST)
//		{
//			this.title = ResourceMgr.getString("LblCompletionListIndex");
//			IndexDefinition[] idx = this.dbConnection.getMetadata().getIndexList(this.schemaForTableList);
//			this.elements = new ArrayList();
//			for (int i = 0; i < idx.length; i++)
//			{
//				this.elements.add(idx[i].getName());
//			}
//		}
		else
		{
			// no proper sql found
			this.elements = Collections.EMPTY_LIST;
			this.title = null;
			Toolkit.getDefaultToolkit().beep();
		}
		if (this.addAllMarker && this.elements != null)
		{
			this.elements.add(0, this.allColumnsMarker);
		}
	}

	private List readKeywords()
	{
		if (this.keywordFile == null) return null;
		InputStream in = getClass().getResourceAsStream(keywordFile);
		TextlistReader reader = new TextlistReader(in);
		List result = new ArrayList(reader.getValues());
		return result;
	}
	
	private void retrieveTables()
	{
		Set tables = this.dbConnection.getObjectCache().getTables(schemaForTableList, typeFilter);
		if (schemaForTableList == null)
		{
			this.title = ResourceMgr.getString("LblCompletionListTables");
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
			this.title = tableForColumnList.getTableName() + ".*";
			
			this.elements = new ArrayList(cols.size() + 1);
			this.elements.addAll(cols);
		}
		return cols;
	}
	
	protected void setTableTypeFilter(String filter)
	{
		this.typeFilter = filter;
	}

	protected String getQualifierLeftOfCursor()
	{
		int len = this.sql.length();
		int start = this.cursorPos - 1;
		
		if (this.cursorPos > len) 
		{
			start = len - 1;
		}
			
		char c = this.sql.charAt(start);
		//if (Character.isWhitespace(c)) return null;
		
		// if no dot is present, then the current word is not a qualifier (e.g. a table name or alias)
		if (c != '.') return null;
		
		String word = StringUtil.getWordLeftOfCursor(this.sql, start, QUALIFIER_DELIM + ".");
		if (word == null) return null;
		int dotPos= word.indexOf('.');
		
		if (dotPos > -1)
		{
			return word.substring(0, dotPos);
		}
		return word;
	}

	protected String getCurrentWord()
	{
		return StringUtil.getWordLeftOfCursor(this.sql, cursorPos, WORD_DELIM);
	}
	
	protected void checkOverwrite()
	{
		String currentWord = getCurrentWord();
		if (!StringUtil.isEmptyString(currentWord))
		{
			boolean keyWord = this.dbConnection.getMetadata().isKeyword(currentWord);
			setOverwriteCurrentWord(!keyWord);
		}
		else
		{
			setOverwriteCurrentWord(false);
		}
	}
}
