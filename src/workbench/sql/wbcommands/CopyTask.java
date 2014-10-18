/*
 * CopyTask.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.sql.SQLException;

import workbench.db.WbConnection;

import workbench.storage.RowActionMonitor;

import workbench.sql.StatementRunnerResult;

import workbench.util.ArgumentParser;

/**
 * An interface to define a single copy task.
 *
 * @author Thomas Kellerer
 */
public interface CopyTask
{
	void setAdjustSequences(boolean flag);

	void setTargetSchemaAndCatalog(String schema, String catalog);

	boolean init(WbConnection source, WbConnection target, StatementRunnerResult result, ArgumentParser cmdLine, RowActionMonitor monitor)
		throws SQLException;


	long copyData()
		throws SQLException, Exception;

	boolean isSuccess();

	boolean hasWarnings();
	
	CharSequence getMessages();

	void cancel();
}
