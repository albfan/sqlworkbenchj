/*
 * TextOptions.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.gui.dialogs.export;

import workbench.util.CharacterRange;

/**
 *
 * @author info@sql-workbench.net
 */
public interface TextOptions
{
	String getTextDelimiter();
	void setTextDelimiter(String delim);
	boolean getExportHeaders();
	void setExportHeaders(boolean flag);
	String getTextQuoteChar();
	void setTextQuoteChar(String quote);
	boolean getCleanupCarriageReturns();
	void setCleanupCarriageReturns(boolean flag);
	void setQuoteAlways(boolean flag);
	boolean getQuoteAlways();
	void setEscapeRange(CharacterRange range);
	CharacterRange getEscapeRange();
	void setLineEnding(String ending);
	String getLineEnding();
}
