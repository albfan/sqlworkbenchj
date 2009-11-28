/*
 * CopyTask.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.sql.SQLException;
import workbench.db.WbConnection;
import workbench.sql.StatementRunnerResult;
import workbench.storage.RowActionMonitor;
import workbench.util.ArgumentParser;

/**
 * An interface to define a single copy task.
 * 
 * @author Thomas Kellerer
 */
public interface CopyTask
{
	boolean init(WbConnection source, WbConnection target, StatementRunnerResult result, ArgumentParser cmdLine, RowActionMonitor monitor)
		throws SQLException;
	
	void copyData()
		throws SQLException, Exception;
	
	boolean isSuccess();
	
	CharSequence getMessages();
	
	void cancel();
}
