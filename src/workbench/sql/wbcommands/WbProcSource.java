/*
 * WbProcSource.java
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

import workbench.resource.ResourceMgr;

import workbench.db.ProcedureDefinition;
import workbench.db.ProcedureReader;
import workbench.db.TableIdentifier;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

/**
 * Display the source code for a procedure.
 *
 * @see workbench.db.ProcedureDefinition#getSource(workbench.db.WbConnection)
 * @author Thomas Kellerer
 */
public class WbProcSource
	extends SqlCommand
{
	public static final String VERB = "WbProcSource";

	public WbProcSource()
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
		object.adjustCase(currentConnection);

		ProcedureReader reader = currentConnection.getMetadata().getProcedureReader();
		ProcedureDefinition def = new ProcedureDefinition(object.getCatalog(), object.getSchema(), object.getObjectName());

		if (reader.procedureExists(def))
		{
			CharSequence source = def.getSource(currentConnection);
			result.addMessage(source);
			result.setSuccess();
		}
		else
		{
			result.addMessage(ResourceMgr.getFormattedString("ErrProcNotFound", args));
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
