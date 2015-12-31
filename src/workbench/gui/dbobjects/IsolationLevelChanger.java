/*
 * IsolationLevelChanger.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.dbobjects;

import java.sql.Connection;

import workbench.db.WbConnection;

/**
 *
 * @author Thomas Kellerer
 */
public class IsolationLevelChanger
{
	private int oldLevel = Connection.TRANSACTION_NONE;

	public void restoreIsolationLevel(WbConnection dbConnection)
	{
		if (oldLevel == Connection.TRANSACTION_READ_COMMITTED ||
			  oldLevel == Connection.TRANSACTION_REPEATABLE_READ ||
			  oldLevel == Connection.TRANSACTION_SERIALIZABLE)
		{
			dbConnection.setIsolationLevel(oldLevel);
		}
	}

	public void changeIsolationLevel(WbConnection dbConnection)
	{
		oldLevel = Connection.TRANSACTION_NONE;
		if (dbConnection == null) return;

		if (dbConnection.getProfile().getUseSeparateConnectionPerTab() && dbConnection.getDbSettings().useReadUncommittedForDbExplorer())
		{
			oldLevel = dbConnection.getIsolationLevel();
			dbConnection.setIsolationLevel(Connection.TRANSACTION_READ_UNCOMMITTED);
		}
	}

}
