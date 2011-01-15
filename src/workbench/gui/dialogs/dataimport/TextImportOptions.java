/*
 * TextImportOptions.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.dialogs.dataimport;

import workbench.util.QuoteEscapeType;

/**
 *
 * @author Thomas Kellerer
 */
public interface TextImportOptions
{
	String getTextDelimiter();
	void setTextDelimiter(String delim);
	boolean getContainsHeader();
	void setContainsHeader(boolean flag);
	String getTextQuoteChar();
	void setTextQuoteChar(String quote);
	boolean getDecode();
	void setDecode(boolean flag);
	String getDecimalChar();
	void setDecimalChar(String s);
	QuoteEscapeType getQuoteEscaping();
	boolean getQuoteAlways();
}
