/*
 * DefaultTextOptions.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.importer;

import workbench.gui.dialogs.dataimport.TextImportOptions;
import workbench.resource.Settings;

/**
 * @author support@sql-workbench.net
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
	public String getTextDelimiter() { return delimiter; }
	public boolean getContainsHeader() {  return true; }
	public String getTextQuoteChar() { return quoteChar;	}
	public boolean getDecode() { return false; }
	public String getDecimalChar() { return Settings.getInstance().getDecimalSymbol();	}

	public void setTextDelimiter(String delim) { 	}
	public void setContainsHeader(boolean flag) { }
	public void setTextQuoteChar(String quote) { }
	public void setDecode(boolean flag) { }
	public void setDecimalChar(String s) { }

}
