/*
 * StatementRunner.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.interfaces;

import java.sql.SQLException;
import workbench.db.WbConnection;
import workbench.sql.StatementRunnerResult;
import workbench.storage.RowActionMonitor;

/**
 * @author support@sql-workbench.net
 */
public interface StatementRunner
{
	void setConnection(WbConnection aConn);
	void setExecutionController(ExecutionController control);
	StatementRunnerResult getResult();
	void setResultLogger(ResultLogger logger);
	void setParameterPrompter(ParameterPrompter filter);
	void runStatement(String aSql, int maxRows, int queryTimeout)
		throws SQLException, Exception;
	void setVerboseLogging(boolean flag);
	boolean getVerboseLogging();
	void cancel();
	void done();
	void setRowMonitor(RowActionMonitor monitor);
	void statementDone();
	void setBaseDir(String dir);
	String getBaseDir();
	void setFullErrorReporting(boolean flag);
	boolean getIgnoreDropErrors();
	void setIgnoreDropErrors(boolean flag);
}
