/*
 * DefaultTextImportOptions.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.importer;

import workbench.gui.dialogs.dataimport.TextImportOptions;
import workbench.resource.Settings;
import workbench.util.QuoteEscapeType;

/**
 * @author Thomas Kellerer
 */
public class DefaultTextImportOptions
	implements TextImportOptions 
{
	private String delimiter;
	private String quoteChar;
	
	public DefaultTextImportOptions(String delim, String quote)
	{
		this.delimiter = delim;
		this.quoteChar = quote;
	}
	@Override
	public String getTextDelimiter() { return delimiter; }

	@Override
	public boolean getContainsHeader() {  return true; }

	@Override
	public boolean getQuoteAlways() { return false; }

	@Override
	public String getTextQuoteChar() { return quoteChar;	}

	@Override
	public QuoteEscapeType getQuoteEscaping() { return QuoteEscapeType.none; }

	@Override
	public boolean getDecode() { return false; }

	@Override
	public String getDecimalChar() { return Settings.getInstance().getDecimalSymbol();	}

	@Override
	public void setTextDelimiter(String delim) { 	}

	@Override
	public void setContainsHeader(boolean flag) { }

	@Override
	public void setTextQuoteChar(String quote) { }

	@Override
	public void setDecode(boolean flag) { }
	
	@Override
	public void setDecimalChar(String s) { }

}
