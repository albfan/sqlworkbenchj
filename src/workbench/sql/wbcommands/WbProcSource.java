/*
 * WbProcSource.java
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

import workbench.resource.ResourceMgr;

import workbench.db.ProcedureDefinition;
import workbench.db.ProcedureReader;
import workbench.db.TableIdentifier;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;

import workbench.util.StringUtil;

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
		this.isUpdatingCommand = false;

		cmdLine = new ArgumentParser();
		cmdLine.addArgument(CommonArgs.ARG_OBJECTS, ArgumentType.TableArgument);
		cmdLine.addArgument(CommonArgs.ARG_FILE, ArgumentType.Filename);
		CommonArgs.addEncodingParameter(cmdLine);
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
		ProcedureDefinition def = reader.findProcedureByName(object);

		if (def != null)
		{
			CharSequence source = def.getSource(currentConnection);
			if (def.isPackageProcedure())
			{
        CharSequence procSrc = reader.getPackageProcedureSource(def);
				if (procSrc != null)
				{
					String msg = "Package: " + def.getPackageName();
					result.addMessage(msg);
					result.addMessage(StringUtil.padRight("-", msg.length(), '-') + "\n");
					result.addMessage(procSrc);
					result.addMessageNewLine();
				}
				else
				{
					result.addMessage(source);
				}
			}
			else
			{
				result.addMessage(source);
			}
			result.setSuccess();
		}
		else
		{
      // maybe this is just the package name
      String user = currentConnection.getMetadata().adjustObjectnameCase(currentConnection.getCurrentUser());
      CharSequence source = reader.getPackageSource(null, user, object.getObjectName());
      if (source != null)
      {
        result.addMessage(source);
        result.addMessageNewLine();
        return result;
      }
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
