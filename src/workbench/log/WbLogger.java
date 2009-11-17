/*
 * WbLogger
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.log;

import java.io.*;

import workbench.gui.components.*;

/**
 * 
 * @author Thomas Kellerer
 */
public interface WbLogger
{
	void setRootLevel(LogLevel level);

	LogLevel getRootLevel();

	void logMessage(LogLevel level, Object caller, String msg, Throwable th);

	void logSqlError(Object caller, String sql, Throwable th);

	void setMessageFormat(String newFormat);

	void logToSystemError(boolean flag);

	void setOutputFile(File logfile, int maxFilesize);

	File getCurrentFile();

	public void shutdownWbLog();

	void setLogViewer(LogFileViewer logViewer);

	boolean levelEnabled(LogLevel tolog);
}
