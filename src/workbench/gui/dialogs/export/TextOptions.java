/*
 * TextOptions.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.dialogs.export;

import workbench.util.CharacterRange;
import workbench.util.QuoteEscapeType;

/**
 *
 * @author Thomas Kellerer
 */
public interface TextOptions
{
	String getTextDelimiter();
	void setTextDelimiter(String delim);
	boolean getExportHeaders();
	void setExportHeaders(boolean flag);
	String getTextQuoteChar();
	void setTextQuoteChar(String quote);
	void setQuoteAlways(boolean flag);
	boolean getQuoteAlways();
	void setEscapeRange(CharacterRange range);
	CharacterRange getEscapeRange();
	void setLineEnding(String ending);
	String getLineEnding();
	void setDecimalSymbol(String decimal);
	String getDecimalSymbol();
	QuoteEscapeType getQuoteEscaping();
}
