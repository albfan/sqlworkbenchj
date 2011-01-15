/*
 * TextRowDataConverter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.exporter;

import java.io.File;
import workbench.log.LogMgr;
import workbench.storage.RowData;
import workbench.util.CharacterRange;
import workbench.util.QuoteEscapeType;
import workbench.util.SqlUtil;
import workbench.util.StrBuffer;
import workbench.util.StringUtil;

/**
 * Converts data from the database into text files.
 *
 * The format of the output can be modified for different needs.
 *
 * The following things can be configured
 * <ul>
 *	<li>Timestamp and Date formats</li>
 *  <li>Decimal characters</li>
 *  <li>Escaping of non-ASCI characters</li>
 *  <li>Line ending uses</li>
 *  <li>Column delimiter</li>
 * </ul>
 * @author  Thomas Kellerer
 */
public class TextRowDataConverter
	extends RowDataConverter
{
	private String delimiter = "\t";
	private String quoteCharacter;
	private boolean quoteAlways;
	private CharacterRange escapeRange;
	private String delimiterAndQuote;
	private String lineEnding = StringUtil.LINE_TERMINATOR;
	private boolean writeBlobFiles = true;
	private boolean writeClobFiles = false;
	private QuoteEscapeType quoteEscape = QuoteEscapeType.none;
	private String rowIndexColumnName;
	private char escapeHexType = 'u';
	
	public void setWriteClobToFile(boolean flag)
	{
		this.writeClobFiles = flag;
	}

	public void setWriteBlobToFile(boolean flag)
	{
		writeBlobFiles = flag;
	}

	/**
	 * Define a column name to include the rowindex in the output
	 * If the name is null, the rowindex column will not be written.
	 *
	 * @param colname
	 */
	public void setRowIndexColName(String colname)
	{
		if (StringUtil.isEmptyString(colname))
		{
			this.rowIndexColumnName = null;
		}
		else
		{
			this.rowIndexColumnName = colname;
		}
	}

	public StrBuffer getEnd(long totalRows)
	{
		return null;
	}

	public void setQuoteEscaping(QuoteEscapeType type)
	{
		this.quoteEscape = type;
	}

	public QuoteEscapeType getQuoteEscaping()
	{
		return this.quoteEscape;
	}

	public StrBuffer convertRowData(RowData row, long rowIndex)
	{
		return convertRowData(row, rowIndex, null);
	}

	public StrBuffer convertRowData(RowData row, long rowIndex, int[] colMap)
	{
		int count = this.metaData.getColumnCount();
		StrBuffer result = new StrBuffer(count * 30);
		boolean canQuote = this.quoteCharacter != null;
		int currentColIndex = 0;

		if (rowIndexColumnName != null)
		{
			result.append(Long.toString(rowIndex + 1));
			result.append(this.delimiter);
		}

		for (int c=0; c < count; c++)
		{
			int colIndex = getRealIndex(c, colMap);
			if (!this.includeColumnInExport(colIndex)) continue;

			if (currentColIndex > 0)
			{
				result.append(this.delimiter);
			}
			int colType = this.metaData.getColumnType(colIndex);
			String dbmsType = this.metaData.getDbmsTypeName(colIndex);
			
			String value = null;

			boolean addQuote = quoteAlways;
			boolean isConverted = row.typeIsConverted(colType, dbmsType);

			if (!isConverted && writeBlobFiles && SqlUtil.isBlobType(colType))
			{
				try
				{
					File blobFile = createBlobFile(row, colIndex, rowIndex);
					value = getBlobFileValue(blobFile);
					long blobSize = writeBlobFile(row.getValue(colIndex), blobFile);
					if (blobSize <= 0)
					{
						value = null;
					}
				}
				catch (Exception e)
				{
					LogMgr.logError("TextRowDataConverter.convertRowData", "Error writing BLOB file", e);
					throw new RuntimeException("Error writing BLOB file", e);
				}
			}
			else if (!isConverted && writeClobFiles && SqlUtil.isClobType(colType, dbmsType, this.originalConnection.getDbSettings()))
			{
				Object clobData = row.getValue(colIndex);
				if (clobData != null)
				{
					try
					{
						File clobFile = createBlobFile(row, colIndex, rowIndex);
						value = getBlobFileValue(clobFile);
						String s = clobData.toString();
						writeClobFile(s, clobFile, this.encoding);
					}
					catch (Exception e)
					{
						throw new RuntimeException("Error writing CLOB file", e);
					}
				}
			}
			else
			{
				value = this.getValueAsFormattedString(row, colIndex);
			}

			boolean isNull = (value == null);
			if (value == null) 
			{
				value = "";
				// Never quote null values
				addQuote = false;
			}

			if (SqlUtil.isCharacterType(colType) && !isNull)
			{
				boolean containsDelimiter = value.indexOf(this.delimiter) > -1;
				addQuote = (this.quoteAlways || (canQuote && containsDelimiter));
				
				if (this.escapeRange != null && this.escapeRange != CharacterRange.RANGE_NONE)
				{
					if (addQuote)
					{
						value = StringUtil.escapeText(value, escapeHexType, this.escapeRange, this.quoteCharacter);
					}
					else
					{
						value = StringUtil.escapeText(value, escapeHexType, this.escapeRange, this.delimiterAndQuote);
					}
				}
				if (this.quoteCharacter != null && this.quoteEscape != QuoteEscapeType.none && value.indexOf(this.quoteCharacter) > -1)
				{
					if (this.quoteEscape == QuoteEscapeType.escape)
					{
						value = StringUtil.replace(value, this.quoteCharacter, "\\" + this.quoteCharacter);
					}
					else
					{
						value = StringUtil.replace(value, this.quoteCharacter, this.quoteCharacter + this.quoteCharacter);
					}
				}
			}
			
			if (addQuote) result.append(this.quoteCharacter);
			result.append(value);
			if (addQuote) result.append(this.quoteCharacter);

			currentColIndex ++;
		}
		result.append(lineEnding);
		return result;
	}

	public void setLineEnding(String ending)
	{
		if (ending != null) this.lineEnding = ending;
	}

	public StrBuffer getStart()
	{
		return getStart(null);
	}

	public StrBuffer getStart(int[] colMap)
	{
		this.setAdditionalEncodeCharacters();
		
		if (!this.writeHeader) return null;

		StrBuffer result = new StrBuffer();
		int colCount = this.metaData.getColumnCount();

		boolean first = true;
		if (rowIndexColumnName != null)
		{
			result.append(rowIndexColumnName);
			first = false;
		}

		for (int c=0; c < colCount; c ++)
		{
			int colIndex = getRealIndex(c, colMap);
			if (!this.includeColumnInExport(colIndex)) continue;
			String name = this.metaData.getColumnName(colIndex);

			if (first)
			{
				first = false;
			}
			else
			{
				result.append(delimiter);
			}

			result.append(name);
		}
		result.append(lineEnding);
		return result;
	}

	private int getRealIndex(int colIndex, int[] colMap)
	{
		if (colMap == null) return colIndex;
		if (colIndex >= colMap.length) return -1;
		return colMap[colIndex];
	}
	
	public void setDelimiter(String delimit)
	{
		if (StringUtil.isBlank(delimit)) return;

		if (delimit.equals("\\t"))
		{
			this.delimiter = "\t";
		}
		else
		{
			this.delimiter = delimit;
		}
		setAdditionalEncodeCharacters();
	}

	private void setAdditionalEncodeCharacters()
	{
		if (this.escapeRange == null) return;
		if (this.quoteCharacter == null && this.delimiter == null) return;

		this.delimiterAndQuote = this.delimiter;

		// Make sure we have a quote character if quoteAlways was requested
		if (quoteAlways && this.quoteCharacter == null) quoteCharacter="\"";

		// If values should always be quoted, then we need to
		// escape the quote character in values
		if (this.quoteCharacter != null)
		{
			this.delimiterAndQuote += this.quoteCharacter;
		}
	}

	public void setQuoteCharacter(String quote)
	{
		if (StringUtil.isNonBlank(quote))
		{
			this.quoteCharacter = quote;
			setAdditionalEncodeCharacters();
		}
	}

	public void setQuoteAlways(boolean flag)
	{
		this.quoteAlways = flag;
	}

	/**
	 *	Define the range of characters to be escaped
	 *  @see workbench.util.StringUtil
	 */
	public void setEscapeRange(CharacterRange range)
	{
		this.escapeRange = range;
	}

}
