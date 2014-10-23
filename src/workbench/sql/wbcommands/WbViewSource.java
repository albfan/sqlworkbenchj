/*
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

import workbench.db.DbMetadata;
import workbench.resource.ResourceMgr;

import workbench.db.TableIdentifier;
import workbench.db.ViewReader;
import workbench.db.ViewReaderFactory;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.StringUtil;

/**
 * Display the source code of a view.
 *
 * @author Thomas Kellerer
 */
public class WbViewSource
	extends SqlCommand
{

	public static final String VERB = "WbViewSource";

	public WbViewSource()
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
		object.setType(currentConnection.getMetadata().getViewTypeName());

		ViewReader reader = ViewReaderFactory.createViewReader(currentConnection);
		CharSequence source = reader.getExtendedViewSource(object, false);
		if (StringUtil.isEmptyString(source))
		{
			object.setType(DbMetadata.MVIEW_NAME);
			TableIdentifier mview = currentConnection.getMetadata().findObject(object);
			source = reader.getExtendedViewSource(mview, false);
		}
		if (source != null)
		{
			result.addMessage(source);
			result.setSuccess();
		}
		else
		{
			result.addMessage(ResourceMgr.getFormattedString("ErrViewNotFound", object.getObjectExpression(currentConnection)));
			result.setFailure();
		}

		return result;
	}

	@Override
	public boolean isWbCommand()
	{
		return true;
	}
}
