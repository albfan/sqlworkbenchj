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
import java.util.List;

import workbench.resource.ResourceMgr;

import workbench.db.DbMetadata;
import workbench.db.DbObject;
import workbench.db.TableIdentifier;
import workbench.db.dependency.DependencyReader;
import workbench.db.dependency.DependencyReaderFactory;

import workbench.storage.DataStore;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.CollectionUtil;

/**
 * List dependent or depending objects in the database
 * <br>
 *
 * @see DependencyReader
 * @author Thomas Kellerer
 */
public class WbListDependencies
	extends SqlCommand
{
	public static final String VERB = "WbListDependencies";
	public static final String VERB_SHORT = "WbListDeps";
	public static final String ARG_OBJECT_NAME = "name";
	public static final String ARG_OBJECT_TYPE = "objectType";
	public static final String ARG_DEPENDENCE_TYPE = "type";

  private static final String TYPE_USES = "uses";
  private static final String TYPE_USED_BY = "using";
	public WbListDependencies()
	{
		cmdLine = new ArgumentParser();
		cmdLine.addArgument(CommonArgs.ARG_SCHEMA, ArgumentType.SchemaArgument);
		cmdLine.addArgument(CommonArgs.ARG_CATALOG, ArgumentType.CatalogArgument);
		cmdLine.addArgument(ARG_OBJECT_NAME, ArgumentType.TableArgument);
    cmdLine.addArgument(ARG_DEPENDENCE_TYPE, CollectionUtil.arrayList(TYPE_USES, TYPE_USED_BY));
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

	@Override
	public String getAlternateVerb()
	{
		return VERB_SHORT;
	}

	@Override
	public StatementRunnerResult execute(String userSql)
		throws SQLException
	{
		String options = getCommandLine(userSql);

		StatementRunnerResult result = new StatementRunnerResult();
    DependencyReader reader = DependencyReaderFactory.getReader(currentConnection);
    if (reader == null)
    {
      result.addErrorMessage("Not supported");
      return result;
    }

		cmdLine.parse(options);

		String schema = null;
		String catalog = null;
    String objectName = null;

    TableIdentifier base = null;
    String type = TYPE_USES;
		if (cmdLine.hasArguments())
		{
			schema = cmdLine.getValue(CommonArgs.ARG_SCHEMA);
			catalog = cmdLine.getValue(CommonArgs.ARG_CATALOG);
      objectName = cmdLine.getValue(ARG_OBJECT_NAME);
      type = cmdLine.getValue(ARG_DEPENDENCE_TYPE, TYPE_USES);
      base = new TableIdentifier(catalog, schema, objectName);
		}
    else
    {
      // support an abbreviated version using "WbListDeps tablename"
      base = new TableIdentifier(options);
    }

    base = currentConnection.getMetadata().findObject(base);

    String titleKey = null;
    List<DbObject> objects = null;
    if (type.equalsIgnoreCase(TYPE_USED_BY))
    {
      objects = reader.getUsedBy(currentConnection, base);
      titleKey = "TxtDepsUsedByParm";
    }
    else
    {
      objects = reader.getUsedObjects(currentConnection, base);
      titleKey = "TxtDepsUsesParm";
    }
    DataStore ds = currentConnection.getMetadata().createTableListDataStore();
    ds.setResultName(ResourceMgr.getFormattedString(titleKey, base.getTableExpression(currentConnection)));
    for (DbObject dbo : objects)
    {
      int row = ds.addRow();
      ds.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_CATALOG, dbo.getCatalog());
      ds.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA, dbo.getSchema());
      ds.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME, dbo.getObjectName());
      ds.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE, dbo.getObjectType());
      ds.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_REMARKS, dbo.getComment());
    }
    ds.resetStatus();
    result.addDataStore(ds);
		return result;
	}

	@Override
	public boolean isWbCommand()
	{
		return true;
	}
}
