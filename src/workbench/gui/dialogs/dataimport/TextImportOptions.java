/*
 * TextImportOptions.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.gui.dialogs.dataimport;

/**
 *
 * @author info@sql-workbench.net
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
}
