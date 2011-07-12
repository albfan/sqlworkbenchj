/*
 * BaseAnalyzer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.completion;

import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;
import workbench.util.EncodingUtil;
import workbench.util.FileUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.TableAlias;

/**
 * Base class to analyze a SQL statement to find out what kind and which
 * objects should be included in the auto-completion window
 *
 * @author Thomas Kellerer
 */
public abstract class BaseAnalyzer
{
	public static final String QUALIFIER_DELIM = "\\?*=<>!/{}\\#%[]'\"(),:;";
	public static final String WORD_DELIM = QUALIFIER_DELIM + "@";
	public static final String SELECT_WORD_DELIM = WORD_DELIM + ".";

	protected static final int NO_CONTEXT = -1;

	/**
	 *  Context value to list the available tables
	 */
	protected static final int CONTEXT_TABLE_LIST = 1;

	/**
	 *  Context value to list the columns for a table
	 */
	protected static final int CONTEXT_COLUMN_LIST = 2;

	/**
	 * Context value to list the tables that are available in the FROM list
	 */
	protected static final int CONTEXT_FROM_LIST = 3;

	protected static final int CONTEXT_TABLE_OR_COLUMN_LIST = 4;

	/**
	 * Context value to list keywords available at this point
	 */
	protected static final int CONTEXT_KW_LIST = 5;

	/**
	 * Context value to list parameters for WB command parameters
	 */
	protected static final int CONTEXT_WB_PARAMS = 6;

	/**
	 * Context value to list all workbench commands
	 */
	protected static final int CONTEXT_WB_COMMANDS = 7;

	protected static final int CONTEXT_SYNTAX_COMPLETION = 8;
	protected static final int CONTEXT_STATEMENT_PARAMETER = 9;

	private final SelectAllMarker allColumnsMarker = new SelectAllMarker();
	private List<String> typeFilter;
	protected String keywordFile;
	protected WbConnection dbConnection;
	protected final String sql;
	protected final int cursorPos;
	protected int context;
	protected TableIdentifier tableForColumnList;
	protected String schemaForTableList;
	protected boolean addAllMarker;
	protected List elements;
	protected String title;
	private boolean overwriteCurrentWord;
	private boolean appendDot;
	private String columnPrefix;
	protected BaseAnalyzer parentAnalyzer;

	public BaseAnalyzer(WbConnection conn, String statement, int cursorPos)
	{
		this.dbConnection = conn;
		this.sql = statement;
		this.cursorPos = cursorPos;
	}

	public String getAnalyzedSql()
	{
		return this.sql;
	}

	public int getCursorPosition()
	{
		return this.cursorPos;
	}

	public char quoteCharForValue(String value)
	{
		return 0;
	}

	public boolean isWbParam()
	{
		return false;
	}

	/**
	 * checks if the value selected by the user should be changed according to the upper/lowercase settings.
	 *
	 * @return true - check the "paste case"
	 *         false - use the selected vale "as is"
	 */
	public boolean convertCase()
	{
		return true;
	}

	public String getSelectionPrefix()
	{
		return this.columnPrefix;
	}

	/**
	 * Set a prefix for columns that are added.
	 * If this value is set, any column that the user
	 * selects, will be prefixed with this string (plus a dot)
	 * This is used when the FROM list in a SELECT statement
	 * contains more than one column
	 */
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
		return this.context == CONTEXT_KW_LIST || this.context == CONTEXT_SYNTAX_COMPLETION;
	}

	public boolean getOverwriteCurrentWord()
	{
		return this.overwriteCurrentWord;
	}

	protected String getSchemaFromCurrentWord()
	{
		String word = getCurrentWord();
		String q = this.getQualifierLeftOfCursor();
		if (q != null)
		{
			return q;
		}

		TableIdentifier tbl = (StringUtil.isBlank(word) ? null : new TableIdentifier(word));

		if (tbl == null || StringUtil.isBlank(tbl.getSchema()))
		{
			if (this.dbConnection != null)
			{
				return this.dbConnection.getMetadata().getCurrentSchema();
			}
			return null;
		}

		return tbl.getSchema();
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

	public String getTitle()
	{
		return this.title;
	}

	public List getData()
	{
		return this.elements;
	}

	protected abstract void checkContext();

	// For use with hierarchical Analyzers (so that a child
	// analyzer can ask its parent directly for a table list
	// This should be overwritten by any Analyzer supporting
	// Sub-Selects
	protected List<TableAlias> getTables()
	{
		return Collections.emptyList();
	}

	public void setParent(BaseAnalyzer analyzer)
	{
		this.parentAnalyzer = analyzer;
	}

	protected boolean between(int toTest, int start, int end)
	{
		if (start == -1 || end == -1) return false;
		return (toTest > start && toTest < end);
	}

	public int getContext()
	{
		return context;
	}

	protected String contextToString()
	{
		switch (context)
		{
			case CONTEXT_COLUMN_LIST:
				return "CONTEXT_COLUMN_LIST";
			case CONTEXT_FROM_LIST:
				return "CONTEXT_FROM_LIST";
			case CONTEXT_KW_LIST:
				return "CONTEXT_KW_LIST";
			case CONTEXT_TABLE_LIST:
				return "CONTEXT_TABLE_LIST";
			case CONTEXT_TABLE_OR_COLUMN_LIST:
				return "CONTEXT_TABLE_LIST";
			case CONTEXT_WB_COMMANDS:
				return "CONTEXT_WB_COMMANDS";
			case CONTEXT_WB_PARAMS:
				return "CONTEXT_WB_PARAMS";
			case CONTEXT_SYNTAX_COMPLETION:
				return "CONTEXT_SYNTAX_COMPLETION";
			case CONTEXT_STATEMENT_PARAMETER:
				return "CONTEXT_STATEMENT_PARAMETER";
		}
		return Integer.toString(context);
	}

	@SuppressWarnings("unchecked")
	protected void buildResult()
	{

		if (context == CONTEXT_TABLE_OR_COLUMN_LIST && tableForColumnList != null)
		{
			if (!retrieveColumns())
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
			this.title = ResourceMgr.getString("LblCompletionListKws");
			this.elements = readKeywords();
		}
		else if (context == CONTEXT_WB_PARAMS)
		{
			// element list has already been filled
			if (isWbParam())
			{
				this.title = ResourceMgr.getString("LblCompletionListParams");
			}
			else
			{
				this.title = ResourceMgr.getString("LblCompletionListParmValues");
			}
		}
		else if (context == CONTEXT_WB_COMMANDS)
		{
			this.title = ResourceMgr.getString("LblCompletionListWbCmd");
		}
		else if (context == CONTEXT_SYNTAX_COMPLETION || context == CONTEXT_STATEMENT_PARAMETER)
		{
			this.title = SqlUtil.getSqlVerb(sql);
		}
		else
		{
			// no proper sql found
			this.elements = Collections.emptyList();
			this.title = null;
			Toolkit.getDefaultToolkit().beep();
		}

		if (this.addAllMarker && this.elements != null)
		{
			this.elements.add(0, this.allColumnsMarker);
		}
	}

	@SuppressWarnings("unchecked")
	private List<String> readKeywords()
	{
		if (this.keywordFile == null) return null;
		InputStream in = getClass().getResourceAsStream(keywordFile);
		try
		{
			BufferedReader reader = new BufferedReader(EncodingUtil.createReader(in, "ISO-8859-1"));
			return FileUtil.getLines(reader, true);
		}
		catch (IOException io)
		{
			return new ArrayList<String>();
		}
	}

	@SuppressWarnings("unchecked")
	private void retrieveTables()
	{
		Set<TableIdentifier> tables = this.dbConnection.getObjectCache().getTables(schemaForTableList, typeFilter);
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

	public String cleanupPasteValue(String toPaste)
	{
		if (schemaForTableList != null && this.context == CONTEXT_TABLE_LIST)
		{
			TableIdentifier tbl = new TableIdentifier(toPaste);
			if (schemaForTableList.equalsIgnoreCase(tbl.getSchema()))
			{
				return tbl.getTableName();
			}
		}
		return toPaste;
	}

	@SuppressWarnings("unchecked")
	private boolean retrieveColumns()
	{
		if (tableForColumnList == null) return false;

		String s = tableForColumnList.getSchema();
		if (s == null)
		{
			tableForColumnList.setSchema(this.dbConnection.getMetadata().getCurrentSchema());
		}
		TableIdentifier toCheck = this.dbConnection.getMetadata().resolveSynonym(tableForColumnList);
		List<ColumnIdentifier> cols = this.dbConnection.getObjectCache().getColumns(toCheck);
		if (cols != null && cols.size() > 0)
		{
			this.title = tableForColumnList.getTableName() + ".*";

			this.elements = new ArrayList(cols.size() + 1);
			this.elements.addAll(cols);
			if (GuiSettings.getSortCompletionColumns())
			{
				Collections.sort(elements);
			}
		}
		return (elements == null ? false : (elements.size() > 0));
	}

	protected void setTableTypeFilter(List<String> filter)
	{
		this.typeFilter = new ArrayList<String>(filter);
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

	public boolean isColumnList()
	{
		return this.context == CONTEXT_COLUMN_LIST;
	}

	protected String getCurrentWord()
	{
		return StringUtil.getWordLeftOfCursor(this.sql, cursorPos, WORD_DELIM);
	}

	protected void checkOverwrite()
	{
		String currentWord = getCurrentWord();
		if (StringUtil.isEmptyString(currentWord))
		{
			setOverwriteCurrentWord(false);
		}
		else
		{
			boolean keyWord = this.dbConnection.getMetadata().isKeyword(currentWord);
			setOverwriteCurrentWord(!keyWord);
		}
	}

	// Used by JUnit tests
	TableIdentifier getTableForColumnList()
	{
		return tableForColumnList;
	}
}
