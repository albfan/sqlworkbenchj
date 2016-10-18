/*
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
import java.util.List;
import java.util.Set;

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
	public static final String ARG_DEPENDENCE_TYPE = "dependency";

  private static final String TYPE_USES = "uses";
  private static final String TYPE_USED_BY = "using";

	public WbListDependencies()
	{
    super();
    this.isUpdatingCommand = false;
		cmdLine = new ArgumentParser();
		cmdLine.addArgument(CommonArgs.ARG_SCHEMA, ArgumentType.SchemaArgument);
		cmdLine.addArgument(CommonArgs.ARG_CATALOG, ArgumentType.CatalogArgument);
		cmdLine.addArgument(ARG_OBJECT_NAME, ArgumentType.TableArgument);
    cmdLine.addArgument(ARG_DEPENDENCE_TYPE, CollectionUtil.arrayList(TYPE_USES, TYPE_USED_BY));
    cmdLine.addArgument(ARG_OBJECT_TYPE);
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

    if (displayHelp(result))
    {
      return result;
    }

		String schema = null;
		String catalog = null;
    String objectName = null;
    String objectType = null;

    TableIdentifier base = null;
    Set<String> types = CollectionUtil.caseInsensitiveSet(TYPE_USES);
		if (cmdLine.hasArguments())
		{
			schema = cmdLine.getValue(CommonArgs.ARG_SCHEMA);
			catalog = cmdLine.getValue(CommonArgs.ARG_CATALOG);
      objectName = cmdLine.getValue(ARG_OBJECT_NAME);
      objectType = cmdLine.getValue(ARG_OBJECT_TYPE);
      types.addAll(cmdLine.getListValue(ARG_DEPENDENCE_TYPE));
      base = new TableIdentifier(catalog, schema, objectName);
		}
    else
    {
      // support an abbreviated version using "WbListDeps tablename"
      base = new TableIdentifier(options);
    }

    DbObject toUse = null;

    if ("procedure".equalsIgnoreCase(objectType))
    {
      toUse = currentConnection.getMetadata().getProcedureReader().findProcedureByName(base);
    }
    else
    {
      String[] typesToSearch = null;
      if (objectType != null)
      {
        typesToSearch = new String[] { objectType };
      }
      toUse = currentConnection.getMetadata().searchObjectOnPath(base, typesToSearch);
    }

    if (toUse == null)
    {
      result.addErrorMessageByKey("ErrTableOrViewNotFound", base.getObjectExpression(currentConnection));
      return result;
    }

    if (types.contains("*"))
    {
      types = CollectionUtil.caseInsensitiveSet(TYPE_USES, TYPE_USED_BY);
    }

    for (String depType : types)
    {
      String titleKey = null;
      List<DbObject> objects = null;

      if (depType.equalsIgnoreCase(TYPE_USED_BY))
      {
        objects = reader.getUsedBy(currentConnection, toUse);
        titleKey = "TxtDepsUsedByParm";
      }
      else if (depType.equalsIgnoreCase(TYPE_USES))
      {
        objects = reader.getUsedObjects(currentConnection, toUse);
        titleKey = "TxtDepsUsesParm";
      }

      if (CollectionUtil.isNonEmpty(objects))
      {
        DataStore ds = currentConnection.getMetadata().createTableListDataStore();
        ds.setResultName(ResourceMgr.getFormattedString(titleKey, toUse.getObjectExpression(currentConnection)));
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
