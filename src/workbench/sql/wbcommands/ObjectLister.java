/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015 Thomas Kellerer.
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

package workbench.sql.wbcommands;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.util.ArgumentParser;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class ObjectLister
{

	public DataStore getSelectableObjects(ArgumentParser cmdLine, String userInput, WbConnection connection)
		throws SQLException
	{
		String[] types = connection.getMetadata().getSelectableTypes();
		return getObjects(cmdLine, userInput, connection, types);
	}

	public DataStore getObjects(ArgumentParser cmdLine, String userInput, WbConnection connection)
		throws SQLException
	{
		String[] types = connection.getMetadata().getTableTypesArray();
		return getObjects(cmdLine, userInput, connection, types);
	}

	private DataStore getObjects(ArgumentParser cmdLine, String userInput, WbConnection connection, String[] types)
		throws SQLException
	{
		String objects = userInput;
		String schema = null;
		String catalog = null;

		cmdLine.parse(userInput);

		if (cmdLine.hasArguments())
		{
			objects = cmdLine.getValue(CommonArgs.ARG_OBJECTS);

			List<String> typeList = cmdLine.getListValue(CommonArgs.ARG_TYPES);
			if (typeList.size() == 1 && (typeList.get(0).equals("*") || typeList.get(0).equals("%")))
			{
				Collection<String> allTypes = connection.getMetadata().getObjectTypes();
				types = StringUtil.toArray(allTypes, true, true);
			}
			else if (typeList.size() > 0)
			{
				types = StringUtil.toArray(typeList, true, true);
			}
			schema = cmdLine.getValue(CommonArgs.ARG_SCHEMA);
			catalog = cmdLine.getValue(CommonArgs.ARG_CATALOG);
		}
		else
		{
			objects = userInput;
		}

		if (StringUtil.isBlank(schema))
		{
			schema = connection.getMetadata().getCurrentSchema();
		}

		if (StringUtil.isBlank(catalog))
		{
			catalog = connection.getMetadata().getCurrentCatalog();
		}

		DataStore resultList = null;

		if (StringUtil.isBlank(objects))
		{
			objects = "%";
		}

		List<String> objectFilters = StringUtil.stringToList(objects, ",", true, true, false, true);

		for (String filter : objectFilters)
		{
			// Create a tableidentifier for parsing e.g. parameters
			// like -tables=public.*
			TableIdentifier tbl = new TableIdentifier(connection.getMetadata().adjustObjectnameCase(filter), connection);
			String tschema = tbl.getSchema();
			if (StringUtil.isBlank(tschema))
			{
				tschema = schema;
			}
			tschema = connection.getMetadata().adjustSchemaNameCase(tschema);
			String tcatalog = tbl.getCatalog();
			if (StringUtil.isBlank(tcatalog))
			{
				tcatalog = catalog;
			}
			tcatalog = connection.getMetadata().adjustObjectnameCase(tcatalog);

			String tname = tbl.getTableName();

			DataStore ds = connection.getMetadata().getObjects(tcatalog, tschema, tname, types);
			if (resultList == null)
			{
				// first result retrieved
				resultList = ds;
			}
			else
			{
				// additional results retrieved, add them to the current result
				resultList.copyFrom(ds);
			}
		}
		return resultList;
	}
}
