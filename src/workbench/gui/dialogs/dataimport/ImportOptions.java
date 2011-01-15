/*
 * ImportOptions.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.dialogs.dataimport;

/**
 *
 * @author Thomas Kellerer
 */
public interface ImportOptions
{
	void setDateFormat(String format);
	String getDateFormat();
	void setTimestampFormat(String format);
	String getTimestampFormat();
	void setEncoding(String enc);
	String getEncoding();
	void setMode(String mode);
	String getMode();
}
