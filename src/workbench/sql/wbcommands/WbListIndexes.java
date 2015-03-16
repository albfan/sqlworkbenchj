/*
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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
import java.util.ArrayList;
import java.util.List;

import workbench.resource.ResourceMgr;

import workbench.db.DbMetadata;
import workbench.db.IndexDefinition;
import workbench.db.IndexReader;
import workbench.db.TableIdentifier;

import workbench.storage.DataStore;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.StringUtil;

/**
 * List all indexes available to the current user.
 * <br>
 *
 * @see workbench.db.DbMetadata#getObjects(String, String, String, String[])
 * @author Thomas Kellerer
 */
public class WbListIndexes
	extends SqlCommand
{
	public static final String VERB = "WbListIndexes";
	public static final String ARG_TABLE_NAME = "tableName";
	public static final String ARG_INDEX_NAME = "indexName";

	public WbListIndexes()
	{
		cmdLine = new ArgumentParser();
		cmdLine.addArgument(CommonArgs.ARG_SCHEMA, ArgumentType.SchemaArgument);
		cmdLine.addArgument(CommonArgs.ARG_CATALOG, ArgumentType.CatalogArgument);
		cmdLine.addArgument(ARG_TABLE_NAME, ArgumentType.TableArgument);
		cmdLine.addArgument(ARG_INDEX_NAME);
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

	@Override
	public StatementRunnerResult execute(String aSql)
		throws SQLException
	{
		String options = getCommandLine(aSql);

		DbMetadata meta = currentConnection.getMetadata();

		IndexReader reader = meta.getIndexReader();

		StatementRunnerResult result = new StatementRunnerResult();

		cmdLine.parse(options);

		String schema = null;
		String catalog = null;

		if (cmdLine.hasUnknownArguments())
		{
			result.addMessage(ResourceMgr.getString("ErrListIdxWrongArgs"));
			result.setFailure();
			return result;
		}

		if (cmdLine.hasArguments())
		{
			schema = cmdLine.getValue(CommonArgs.ARG_SCHEMA);
			catalog = cmdLine.getValue(CommonArgs.ARG_CATALOG);
		}

		String currentSchema = currentConnection.getMetadata().getCurrentSchema();
		String currentCatalog = currentConnection.getMetadata().getCurrentCatalog();

		String indexPattern = cmdLine.getValue(ARG_INDEX_NAME);

		List<IndexDefinition> indexes = null;

		if (cmdLine.isArgPresent(ARG_TABLE_NAME))
		{
			SourceTableArgument tableArg = new SourceTableArgument(cmdLine.getValue(ARG_TABLE_NAME), null, StringUtil.coalesce(schema, currentSchema), currentConnection);

			List<TableIdentifier> tables = tableArg.getTables();
			indexes = new ArrayList<>();
			for (TableIdentifier tbl : tables)
			{
        List<IndexDefinition> indexList = reader.getTableIndexList(tbl);
				indexes.addAll(indexList);
			}
		}
		else
		{
      if (!reader.supportsIndexList())
      {
        result.addMessage(ResourceMgr.getFormattedString("ErrIdxListNotSupported", meta.getProductName()));
        result.setFailure();
        return result;
      }
			indexes = reader.getIndexes(StringUtil.coalesce(catalog, currentCatalog), StringUtil.coalesce(schema, currentSchema), null, indexPattern);
		}

		DataStore ds = reader.fillDataStore(indexes, true);
		ds.setResultName(ResourceMgr.getString("TxtDbExplorerIndexes"));
		result.addDataStore(ds);

		return result;
	}

	@Override
	public boolean isWbCommand()
	{
		return true;
	}
}
