/*
 * BaseAnalyzer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.completion;

import java.awt.Toolkit;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;

import workbench.db.ColumnIdentifier;
import workbench.db.DbSearchPath;
import workbench.db.IndexDefinition;
import workbench.db.IndexReader;
import workbench.db.QuoteHandler;
import workbench.db.SequenceDefinition;
import workbench.db.SequenceReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.objectcache.DbObjectCache;

import workbench.sql.formatter.SQLToken;
import workbench.sql.syntax.SqlKeywordHelper;

import workbench.util.SelectColumn;
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
	public static final String QUALIFIER_DELIM = "\\?*=<>!/{}\\%'(),:;";
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
	 * Context value to list parameters for WB commands
	 */
	protected static final int CONTEXT_WB_PARAMS = 6;

	/**
	 * Context value to list all workbench commands
	 */
	protected static final int CONTEXT_WB_COMMANDS = 7;

	/**
	 * Context value to list values for a specific command parameter
	 */
	protected static final int CONTEXT_WB_PARAMVALUES = 8;

	protected static final int CONTEXT_SYNTAX_COMPLETION = 9;
	protected static final int CONTEXT_STATEMENT_PARAMETER = 10;
	protected static final int CONTEXT_SCHEMA_LIST = 11;
	protected static final int CONTEXT_CATALOG_LIST = 12;
	protected static final int CONTEXT_SEQUENCE_LIST = 13;
	protected static final int CONTEXT_INDEX_LIST = 14;

	private final SelectAllMarker allColumnsMarker = new SelectAllMarker();
	private List<String> typeFilter;
	protected String keywordFile;
	protected WbConnection dbConnection;
	protected final String sql;
	protected final String verb;
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
	protected char catalogSeparator;

	protected SelectFKValueMarker fkMarker;

	public BaseAnalyzer(WbConnection conn, String statement, int cursorPos)
	{
		this.dbConnection = conn;
		this.sql = statement;
		this.verb = SqlUtil.getSqlVerb(sql);
		this.cursorPos = cursorPos;
		this.catalogSeparator = SqlUtil.getCatalogSeparator(this.dbConnection);
	}

	/**
	 * To be implemented by specialized analyzers that might need more complex logic
	 * when the user selects an entry from the popup list.
	 *
	 * @param selectedObject
	 * @return the value to paste or null if the standard behaviour should be used
	 */
	public String getPasteValue(Object selectedObject)
	{
		return null;
	}

	public QuoteHandler getQuoteHandler()
	{
		if (dbConnection == null) return QuoteHandler.STANDARD_HANDLER;
		QuoteHandler handler = dbConnection.getMetadata();
		if (handler == null) return QuoteHandler.STANDARD_HANDLER;
		return handler;
	}

	public WbConnection getConnection()
	{
		return dbConnection;
	}

	public String getWordDelimiters()
	{
		return SELECT_WORD_DELIM;
	}

	public boolean needsCommaForMultipleSelection()
	{
		return true;
	}

	/**
	 * For testing purposes only!
	 * @param newSeparator
	 */
	void setCatalogSeparator(char newSeparator)
	{
		this.catalogSeparator = newSeparator;
	}

	public String getSqlVerb()
	{
		return this.verb;
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

	public String getSchemaForTableList()
	{
		return schemaForTableList;
	}

	protected String getCurrentSchemaToUse()
	{
		if (dbConnection == null) return null;

		if (!dbConnection.getDbSettings().supportsSchemas())
		{
			// No schemas supported (e.g. MySQL) pretend a catalog is the same thing
			return this.dbConnection.getMetadata().getCurrentCatalog();
		}

		if (dbConnection.getDbSettings().useFullSearchPathForCompletion())
		{
			return null;
		}

		String schema = this.dbConnection.getCurrentSchema();
		List<String> schemas = DbSearchPath.Factory.getSearchPathHandler(dbConnection).getSearchPath(dbConnection, schema);
		if (schemas.isEmpty())
		{
			// DBMS does not have a search path, so use the current schema
			return schema;
		}
		else if (schemas.size() == 1)
		{
			return schemas.get(0);
		}
		return null;
	}

	protected String getSchemaFromCurrentWord()
	{
		String word = getCurrentWord();
		String q = getQualifierLeftOfCursor();
		if (q != null)
		{
			return q;
		}

		TableIdentifier tbl = (StringUtil.isBlank(word) ? null : new TableIdentifier(word, dbConnection));

		if (tbl == null || StringUtil.isBlank(tbl.getSchema()))
		{
			return getCurrentSchemaToUse();
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
		this.fkMarker =  null;

		checkOverwrite();
		this.addAllMarker = false;

		// this should not be done in the constructor as the
		// sub-classes might need to do important initializations during initialization
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
			case CONTEXT_WB_PARAMVALUES:
				return "CONTEXT_WB_PARAMVALUES";
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
		else if (context == CONTEXT_SCHEMA_LIST)
		{
			this.title = StringUtil.capitalize(dbConnection.getMetadata().getSchemaTerm());
			retrieveSchemas();
		}
		else if (context == CONTEXT_CATALOG_LIST)
		{
			this.title = StringUtil.capitalize(dbConnection.getMetadata().getCatalogTerm());
			retrieveCatalogs();
		}
		else if (context == CONTEXT_SEQUENCE_LIST)
		{
			retrieveSequences();
		}
		else if (context == CONTEXT_INDEX_LIST)
		{
			retrieveIndexes();
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

	private List<String> readKeywords()
	{
		if (this.keywordFile == null) return null;
		SqlKeywordHelper helper = new SqlKeywordHelper(dbConnection.getDbId());
		Set<String> kwlist = helper.loadKeywordsFromFile(keywordFile);
		return new ArrayList<>(kwlist);
	}

	@SuppressWarnings("unchecked")
	protected void retrieveSchemas()
	{
		List<String> schemas = dbConnection.getMetadata().getSchemas();
		this.elements = new ArrayList(schemas.size());
		this.elements.addAll(schemas);
	}

	@SuppressWarnings("unchecked")
	protected void retrieveCatalogs()
	{
		List<String> catalogs = dbConnection.getMetadata().getCatalogs();
		this.elements = new ArrayList(catalogs.size());
		this.elements.addAll(catalogs);
	}

	@SuppressWarnings("unchecked")
	protected void retrieveIndexes()
	{
		IndexReader reader = dbConnection.getMetadata().getIndexReader();
		title = "Index";
		String schema = dbConnection.getMetadata().adjustSchemaNameCase(schemaForTableList);
		String catalog = null;
		if (!dbConnection.getDbSettings().supportsSchemas())
		{
			// no schemas supported (e.g. MySQL) --> use the "schema" for the catalog
			catalog = schema;
			schema = null;
		}
		List<IndexDefinition> indexes = reader.getIndexes(catalog, schema, null, null);
		this.elements = new ArrayList(indexes.size());
		for (IndexDefinition idx : indexes)
		{
			if (!idx.isPrimaryKeyIndex())
			{
				idx.setDisplayName(idx.getObjectExpression(dbConnection) + " (" + idx.getBaseTable().getTableExpression(dbConnection) + ")");
				elements.add(idx);
			}
		}
	}

	@SuppressWarnings("unchecked")
	protected void retrieveSequences()
	{
		SequenceReader reader = dbConnection.getMetadata().getSequenceReader();
		if (reader == null) return;
		try
		{
			title = StringUtil.capitalize(reader.getSequenceTypeName());
			String schema = dbConnection.getMetadata().adjustSchemaNameCase(schemaForTableList);
			List<SequenceDefinition> sequences = reader.getSequences(null, schema, null);
			elements = new ArrayList(sequences);
		}
		catch (SQLException se)
		{
			LogMgr.logError("BaseAnalyzer.retrieveSequences()", "Could not retrieve sequences", se);
		}
	}


	@SuppressWarnings("unchecked")
	protected void retrieveTables()
	{
		DbObjectCache cache = this.dbConnection.getObjectCache();
		Set<TableIdentifier> tables = cache.getTables(schemaForTableList, typeFilter);
		if (schemaForTableList == null || cache.getSearchPath(schemaForTableList).size() > 1)
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

	@SuppressWarnings("unchecked")
	protected boolean retrieveColumns()
	{
		if (tableForColumnList == null) return false;
		if (this.dbConnection == null) return false;
		DbObjectCache cache = this.dbConnection.getObjectCache();
		TableIdentifier toCheck = cache.getSynonymTable(tableForColumnList);
		List<ColumnIdentifier> cols = cache.getColumns(toCheck);
		if (cols != null && cols.size() > 0)
		{
			if (cache.supportsSearchPath())
			{
				TableIdentifier tbl = cache.getTable(toCheck);
				this.title = (tbl == null ? toCheck.getTableName() : tbl.getTableExpression()) + ".*";
			}
			else
			{
				this.title = tableForColumnList.getTableName() + ".*";
			}
			this.elements = new ArrayList(cols.size() + 1);
			this.elements.addAll(cols);
			if (GuiSettings.getSortCompletionColumns())
			{
				Collections.sort(elements);
			}
			if (fkMarker != null)
			{
//				TableIdentifier tbl = (tableForFkSelect != null ? tableForFkSelect : tableForColumnList);
//				SelectFKValueMarker fk = new SelectFKValueMarker(columnForFKSelect, tbl, multiValueFkSelect);
				if (GuiSettings.showSelectFkValueAtTop())
				{
					elements.add(0, fkMarker);
				}
				else
				{
					elements.add(fkMarker);
				}
			}
		}
		return (elements == null ? false : (elements.size() > 0));
	}

	protected void setTableTypeFilter(List<String> filter)
	{
		this.typeFilter = new ArrayList<>(filter);
	}

	public String getQualifierLeftOfCursor()
	{
		int len = this.sql.length();
		int start = this.cursorPos - 1;

		if (this.cursorPos > len)
		{
			start = len - 1;
		}

		char separator = SqlUtil.getSchemaSeparator(this.dbConnection);
		char c = this.sql.charAt(start);
		//if (Character.isWhitespace(c)) return null;

		// if no dot is present, then the current word is not a qualifier (e.g. a table name or alias)
		if (c != separator) return null;

		String word = StringUtil.getWordLeftOfCursor(this.sql, start, QUALIFIER_DELIM + separator);
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
			boolean keyWord = this.dbConnection != null && this.dbConnection.getMetadata().isKeyword(currentWord);
			char separator = SqlUtil.getCatalogSeparator(this.dbConnection);
			setOverwriteCurrentWord(!keyWord && currentWord.charAt(currentWord.length() - 1) != separator);
		}
	}

	protected SelectFKValueMarker checkFkLookup()
	{
		SQLToken prev = SqlUtil.getOperatorBeforeCursor(sql, cursorPos);
		if (prev == null) return null;
		int pos = prev.getCharBegin() - 1;
		String col = StringUtil.getWordLeftOfCursor(sql, pos, " ");

		if (col != null)
		{
			SelectColumn scol = new SelectColumn(col);
			String column = scol.getObjectName();

			// getOperatorBeforeCursor() only returns operators and IN, ANY, ALL
			// if the token is not an operator it's an IN, ANY, ALL condition
			// which would allow multiple values to be selected.
			boolean multiValueFkSelect = !prev.isOperator();
			TableIdentifier fkTable = this.tableForColumnList;
			String tbl =  scol.getColumnTable();
			if (tbl != null)
			{
				List<TableAlias> tblList = getTables();
				for (TableAlias alias : tblList)
				{
					if (tbl.equalsIgnoreCase(alias.getNameToUse()))
					{
						fkTable = alias.getTable();
					}
				}
			}
			return new SelectFKValueMarker(column, fkTable, multiValueFkSelect);
		}
		return null;
	}
	// Used by JUnit tests
	TableIdentifier getTableForColumnList()
	{
		return tableForColumnList;
	}


}
