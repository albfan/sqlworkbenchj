/*
 * DataStoreReplacer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import workbench.gui.editor.SearchAndReplace;
import workbench.log.LogMgr;
import workbench.util.ConverterException;
import workbench.util.StringUtil;

/**
 * @author support@sql-workbench.net
 */
public class DataStoreReplacer
{
	private DataStore client;
	private Position lastFoundPosition;
	private Pattern lastPattern;
	private String currentReplacementValue;
	private int[] selectedRows;
	private int lastSelectedRowIndex;
	private String lastCriteria;
	private boolean isRegexSearch;
	
	public DataStoreReplacer()
	{
		
	}
	
	/**
	 * Create a new DataStoreReplacer for the given DataStore.
	 * The datastore is not checked if it is updateable!
	 * 
	 * @param store the DataStore to search and replace in
	 */ 
	public DataStoreReplacer(DataStore store)
	{
		setDataStore(store);
	}

	public void setDataStore(DataStore ds)
	{
		this.client = ds;
		this.reset();
	}
	/**
	 * Limit all search and replace actions to the selected rows.
	 * To reset search & replace in the selected rows, setSelecteRows()
	 * has to be called again with a null value
	 * @param the selected rows to be searched, null to reset row selection
	 */
	public void setSelectedRows(int[] rows)
	{
		this.selectedRows = rows;
		this.lastSelectedRowIndex = 0;
	}
	
	public Position getLastFoundPosition() 
	{
		return this.lastFoundPosition;
	}
	
	public String getLastCriteria()
	{
		return this.lastCriteria;
	}
	
	/**
	 * Find the given text in the datastore.
	 * 
	 * @param the text to search for
	 * @param ignoreCase if true, search is case-insesitive
	 * @param wholeWord if true, only text in word bounderies is found
	 * @param useRegex treat the text as a regular expression
	 * @return the position where the text was found
	 * 
	 * @see workbench.gui.editor.SeachAndReplace#getSearchExpression(String, boolean, boolean, boolean)
	 */
	public Position find(String text, boolean ignoreCase, boolean wholeWord, boolean useRegex)
		throws PatternSyntaxException
	{
		lastCriteria = text;
		lastFoundPosition = Position.NO_POSITION;
		lastSelectedRowIndex = 0;
		if (StringUtil.isEmptyString(text)) return Position.NO_POSITION;
		this.isRegexSearch = useRegex;
		String expression = SearchAndReplace.getSearchExpression(text, ignoreCase, wholeWord, useRegex);
		Pattern p = null;
		try
		{
			p = Pattern.compile(expression);
		}
		catch (PatternSyntaxException e)
		{
			LogMgr.logError("DataStoreReplacer.find()", "Error compiling search pattern", e);
			throw e;
		}
		return findPattern(p);
	}

	/**
	 * Find the next occurance of the search string. 
	 * This returns NO_POSITION if find(String, boolean) has not 
	 * been called before. 
	 * @return the position of the next occurance
	 * @see #find(String)
	 * @see #find(String, boolean)
	 */
	public Position findNext()
	{
		if (lastPattern == null)
		{
			reset();
			return lastFoundPosition;
		}
		return findPattern(lastPattern);
	}

	public void reset()
	{
		lastPattern = null;
		lastCriteria = null;
		currentReplacementValue = null;
		lastSelectedRowIndex = 0;
		lastFoundPosition = Position.NO_POSITION;
	}

	private Position findPattern(Pattern p)
	{
		int startRow = 0;
		int startCol = 0;

		int rowCount = this.client.getRowCount();
		int colCount = this.client.getColumnCount();

		if (this.lastFoundPosition.isValid())
		{
			startRow = this.lastFoundPosition.getRow();
			startCol = this.lastFoundPosition.getColumn() + 1;
			if (startCol >= colCount)
			{
				startCol = 0;
				startRow ++;
			}
		}

		this.lastPattern = p;

		int startIndex = startRow;
		
		if (this.selectedRows != null)
		{
			startIndex = this.lastSelectedRowIndex;
			rowCount = this.selectedRows.length;
		}
		
		if (startIndex < 0) startIndex = 0;
		
		for (int index = startIndex; index < rowCount; index++)
		{
			int row = index;
			if (selectedRows != null)
			{
				this.lastSelectedRowIndex = index;
				row = this.selectedRows[index];
			}
			
			for (int col=startCol; col < colCount; col++)
			{
				String colValue = client.getValueAsString(row, col);
				if (StringUtil.isEmptyString(colValue)) continue;
				Matcher m = p.matcher(colValue);
				if (m.find())
				{
					this.lastFoundPosition = new Position(row, col);
					return this.lastFoundPosition;
				}
			}
			startCol = 0;
		}
		
		return Position.NO_POSITION;
	}
	
	/**
	 * Replace all occurances of a value with the given replacement value.
	 * 
	 * @param expression the value to search for
	 * @param replacement the replacement value
	 * @param rows if not null search and replace is only done in these rows
	 * @param ignoreCase should the search pattern be applied case-insensitive
	 * @param wholeWord if true, only whole words are found
	 * @param useRegex if true, expression is treated as a regular expression
	 * 
	 * @return the number of occurances replaced
	 * @see workbench.gui.editor.SeachAndReplace#getSearchExpression(String, boolean, boolean, boolean)
	 * @see workbench.gui.editor.SeachAndReplace#fixSpecialreplacementChars(String)
	 */
	public int replaceAll(String text, String replacement, int[] rows, boolean ignoreCase, boolean wholeWord, boolean useRegex)
		throws ConverterException, PatternSyntaxException
	{
		reset();
		String expression = SearchAndReplace.getSearchExpression(text, ignoreCase, wholeWord, useRegex);
		
		this.isRegexSearch = useRegex;
		
		if (isRegexSearch)
		{
			currentReplacementValue = SearchAndReplace.fixSpecialReplacementChars(replacement);
		}
		else
		{
			currentReplacementValue = StringUtil.quoteRegexMeta(replacement);
		}

		int replaced = 0;
		Pattern p = Pattern.compile(expression);
		
		this.setSelectedRows(rows);
		
		Position pos = findPattern(p);
		
		while (pos.isValid())
		{
			replaceValueAt(pos, this.currentReplacementValue, this.lastPattern);
			replaced ++;
			pos = findNext();
		}
		return replaced;
	}

	public boolean replaceCurrent(String replacement)
		throws ConverterException
	{
		if (this.lastFoundPosition == null) return false;
		
		if (this.lastFoundPosition.isValid())
		{
			if (isRegexSearch)
			{
				currentReplacementValue = SearchAndReplace.fixSpecialReplacementChars(replacement);
			}
			else
			{
				currentReplacementValue = StringUtil.quoteRegexMeta(replacement);
			}
			replaceValueAt(lastFoundPosition, this.currentReplacementValue, this.lastPattern);
			return true;
		}
		return false;
	}
	
	private void replaceValueAt(Position pos, String replacement, Pattern p)
		throws ConverterException
	{
		String value = this.client.getValueAsString(pos.getRow(), pos.getColumn());
		if (!StringUtil.isEmptyString(value))
		{
			int type = this.client.getColumnType(pos.getColumn());
			Matcher m = p.matcher(value);
			String newValue = m.replaceAll(replacement);
			try
			{
				client.setInputValue(pos.getRow(), pos.getColumn(), newValue);
			}
			catch (ConverterException e)
			{
				LogMgr.logError("DataStoreReplacer.replaceAll()", "Could not convert the replacement data", e);
				throw e;
			}
		}
	}
}
