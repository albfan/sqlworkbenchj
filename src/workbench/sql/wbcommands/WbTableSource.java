/*
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

import workbench.resource.ResourceMgr;

import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.TableSourceBuilder;
import workbench.db.TableSourceBuilderFactory;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

/**
 * Display the source code of a table.
 *
 * @author Thomas Kellerer
 */
public class WbTableSource
	extends SqlCommand
{
	public static final String VERB = "WbTableSource";

	public WbTableSource()
	{
		super();
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
		StatementRunnerResult result = new StatementRunnerResult();
		String args = getCommandLine(sql);

		TableIdentifier object = new TableIdentifier(args, currentConnection);

		TableDefinition tableDef = currentConnection.getMetadata().getTableDefinition(object);
		if (tableDef == null || tableDef.getColumnCount() == 0)
		{
			result.addMessage(ResourceMgr.getFormattedString("ErrTableNotFound", object.getObjectExpression(currentConnection)));
			result.setFailure();
			return result;
		}

		TableSourceBuilder reader = TableSourceBuilderFactory.getBuilder(currentConnection);
		String source = reader.getTableSource(tableDef.getTable(), tableDef.getColumns());
		if (source != null)
		{
			result.addMessage(source);
			result.setSuccess();
		}
		return result;
	}

	@Override
	public boolean isWbCommand()
	{
		return true;
	}
}
