/*
 * TextRowDataConverter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.exporter;

import java.text.SimpleDateFormat;

import workbench.storage.ResultInfo;
import workbench.storage.RowData;
import workbench.util.CharacterRange;
import workbench.util.SqlUtil;
import workbench.util.StrBuffer;
import workbench.util.StringUtil;

/**
 *
 * @author  support@sql-workbench.net
 */
public class TextRowDataConverter
	extends RowDataConverter
{
	private SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
	private SimpleDateFormat timestampFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SS");
	private String delimiter = "\t";
	private String quoteCharacter = null;
	private boolean writeHeader = true;
	private boolean cleanCR = false;
	private boolean quoteAlways = false;
	private CharacterRange escapeRange = null;
	private String encodingUsed = null;
	private String additionalEncodeCharacters = null;
	private String lineEnding = StringUtil.LINE_TERMINATOR;

	public TextRowDataConverter(ResultInfo info)
	{
		super(info);
	}

	public void setCleanNonPrintable(boolean flag)
	{
		this.cleanCR = flag;
	}

	public StrBuffer convertData()
	{
		return null;
	}

	public StrBuffer getEnd(long totalRows)
	{
		return null;
	}

	public String getFormatName()
	{
		return "Text";
	}

	public StrBuffer convertRowData(RowData row, long rowIndex)
	{
		int count = this.metaData.getColumnCount();
		StrBuffer result = new StrBuffer(count * 30);
		boolean shouldQuote = this.quoteCharacter != null;
		for (int c=0; c < count; c ++)
		{
			if (!this.includeColumnInExport(c)) continue;
			String value = this.getValueAsFormattedString(row, c);
			if (value == null) value = "";
			boolean needQuote = false;

			if (SqlUtil.isCharacterType(this.metaData.getColumnType(c)))
			{
				if (this.cleanCR)
				{
					value = StringUtil.cleanNonPrintable(value);
				}
				if (this.escapeRange != null)
				{
					value = StringUtil.escapeUnicode(value, this.additionalEncodeCharacters, this.escapeRange);
				}

				needQuote = (this.quoteAlways || (shouldQuote && value.indexOf(this.delimiter) > -1));
				if (needQuote) result.append(this.quoteCharacter);
			}

			result.append(value);

			if (needQuote) result.append(this.quoteCharacter);

			if (c < count - 1) result.append(this.delimiter);
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
		this.setAdditionalEncodeCharacters();

		if (!this.isWriteHeader()) return null;

		StrBuffer result = new StrBuffer();
		int colCount = this.metaData.getColumnCount();
		boolean first = true;
		for (int c=0; c < colCount; c ++)
		{
			if (!this.includeColumnInExport(c)) continue;
			String name = this.metaData.getColumnName(c);
			if (first) 
			{
				first = false;
			}
			else
			{
				result.append(this.getDelimiter());
			}
			result.append(name);
		}
		result.append(lineEnding);
		return result;
	}

	public String getDelimiter()
	{
		return delimiter;
	}

	public void setDelimiter(String delimit)
	{
		if (delimit == null || delimit.trim().length() == 0) return;

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

		// If values should always be quoted, then we need to
		// escape the quote character in values
		if (this.quoteAlways)
		{
			this.additionalEncodeCharacters = (this.quoteCharacter != null ? this.quoteCharacter : "");
		}
		else
		{

			this.additionalEncodeCharacters =
					(this.quoteCharacter != null ? this.quoteCharacter : "") +
					(this.delimiter != null ? this.delimiter : "" );
		}
	}

	public String getQuoteCharacter()
	{
		return quoteCharacter;
	}

	public void setQuoteCharacter(String quote)
	{
		if (quote != null && quote.trim().length() > 0)
		{
			this.quoteCharacter = quote;
			setAdditionalEncodeCharacters();
		}
	}

	public boolean isWriteHeader()
	{
		return writeHeader;
	}

	public void setWriteHeader(boolean writeHeader)
	{
		this.writeHeader = writeHeader;
	}

	public boolean getQuoteAlways()
	{
		return quoteAlways;
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

	public CharacterRange getEscapeRange()
	{
		return this.escapeRange;
	}

	public void setEncodingUsed(String enc)
	{
		this.encodingUsed = enc;
	}
}
