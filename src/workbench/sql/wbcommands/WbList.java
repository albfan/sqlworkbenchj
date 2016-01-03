/*
 * WbListTables.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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

import workbench.console.ConsoleSettings;
import workbench.console.RowDisplay;
import workbench.resource.ResourceMgr;

import workbench.db.DbMetadata;

import workbench.storage.DataStore;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;

/**
 * List tables and other database objects available to the current user.
 * <br>
 * This is the same information as displayed in the DbExplorer's "Objects" tab.
 *
 * @see workbench.db.DbMetadata#getObjects(String, String, String, String[])
 * @author Thomas Kellerer
 */
public class WbList
	extends SqlCommand
{
	public static final String VERB = "WbList";

	public WbList()
	{
		cmdLine = new ArgumentParser();
		cmdLine.addArgument(CommonArgs.ARG_OBJECTS);
		cmdLine.addArgument(CommonArgs.ARG_TYPES, ArgumentType.ObjectTypeArgument);
		cmdLine.addArgument(CommonArgs.ARG_SCHEMA, ArgumentType.SchemaArgument);
		cmdLine.addArgument(CommonArgs.ARG_CATALOG, ArgumentType.CatalogArgument);
		cmdLine.addArgument(CommonArgs.ARG_HELP, ArgumentType.BoolSwitch);
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException
	{
		String options = getCommandLine(sql);

		StatementRunnerResult result = new StatementRunnerResult();
		ConsoleSettings.getInstance().setNextRowDisplay(RowDisplay.SingleLine);

		cmdLine.parse(options);
		if (cmdLine.isArgPresent(CommonArgs.ARG_HELP))
		{
			result.addMessage(ResourceMgr.getString("ErrListWrongArgs"));
			result.setSuccess();
			return result;
		}

		ObjectLister lister = new ObjectLister();
		DataStore resultList = lister.getObjects(cmdLine, options, currentConnection);

		if (resultList != null)
		{
			resultList.setResultName(ResourceMgr.getString("TxtObjList"));
			resultList.setGeneratingSql(sql);
			resultList.sort(DbMetadata.getTableListSort());
			result.addDataStore(resultList);

      if (currentConnection.getDbSettings().supportsSchemas() && lister.getSchemaUsed() != null)
      {
        currentConnection.getObjectCache().addTableList(resultList, lister.getSchemaUsed());
      }
      else if (currentConnection.getDbSettings().supportsCatalogs()&& lister.getCatalogUsed() != null)
      {
        currentConnection.getObjectCache().addTableList(resultList, lister.getCatalogUsed());
      }
		}
		return result;
	}

	@Override
	public boolean isWbCommand()
	{
		return true;
	}
}
