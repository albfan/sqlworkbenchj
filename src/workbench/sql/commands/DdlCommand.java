/*
 * DdlCommand.java
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
package workbench.sql.commands;

import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.db.ErrorInformationReader;
import workbench.db.ReaderFactory;
import workbench.db.TableIdentifier;

import workbench.sql.ErrorDescriptor;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

import workbench.util.CollectionUtil;
import workbench.util.DdlObjectInfo;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * Run a DDL (CREATE, DROP, ALTER, GRANT, REVOKE) command.
 *
 * @author Thomas Kellerer
 */
public class DdlCommand
	extends SqlCommand
{
	// Firebird RECREATE VIEW command
	public static DdlCommand getRecreateCommand()
	{
		return new DdlCommand("RECREATE");
	}

	public static DdlCommand getCreateCommand()
	{
		return new DdlCommand("CREATE");
	}

	public static List<DdlCommand> getDdlCommands()
	{
		return CollectionUtil.readOnlyList(
			new DdlCommand("DROP"),
			getCreateCommand(),
			new DdlCommand("ALTER"),
			new DdlCommand("GRANT"),
			new DdlCommand("REVOKE"));
	}

	private String verb;
	private Savepoint ddlSavepoint;
	private final Set<String> typesToRemember = CollectionUtil.caseInsensitiveSet("procedure", "function", "trigger", "package", "package body", "type");
  private Pattern alterDropPattern;

	private DdlCommand(String sqlVerb)
	{
		super();
		this.verb = sqlVerb;
		this.isUpdatingCommand = true;
    if ("ALTER".equals(verb))
    {
   		// an ALTER ... statement might also be a DROP (e.g. ALTER TABLE someTable DROP PRIMARY KEY)
      alterDropPattern = Pattern.compile("DROP\\s+(PRIMARY\\s+KEY|CONSTRAINT)\\s+", Pattern.CASE_INSENSITIVE);
    }
	}

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult(sql);

		boolean useSavepoint = runner.useSavepointForDDL();

		if (useSavepoint && !this.currentConnection.supportsSavepoints())
		{
			useSavepoint = false;
			LogMgr.logWarning("DdlCommand.execute()", "A savepoint should be used for this DDL command, but the driver does not support savepoints!");
		}

		DdlObjectInfo info = SqlUtil.getDDLObjectInfo(sql, currentConnection);
		if (info != null && verb.equals("ALTER") && info.getObjectType().equals("PACKAGE"))
		{
			// an "alter package .. compile"  will report errors for the package body, not the package
			info.setObjectType("PACKAGE BODY");
		}

		if (info != null && typesToRemember.contains(info.getObjectType()))
		{
			// This is only here to mimic SQL*Plus' behaviour for a "SHOW ERROR" without a parameter.
			// remember the last "object" in order to be able to show the errors but only for "PL/SQL" objects.
      // The last error is not overwritten by creating a table or a view
			currentConnection.setLastDDLObject(info);
      result.ignoreUpdateCounts(true);
		}

		boolean isDrop = false;
    boolean ignoreDropError = false;
		try
		{
			this.currentStatement = currentConnection.createStatement();

			if (currentConnection.getDbSettings().disableEscapesForDDL())
			{
				currentStatement.setEscapeProcessing(false);
			}

			sql = getSqlToExecute(sql);

			result.setSuccess();

			if (useSavepoint)
			{
				this.ddlSavepoint = currentConnection.setSavepoint();
			}

			isDrop = isDropCommand(sql);
      ignoreDropError = isDrop && this.runner.getIgnoreDropErrors();

      boolean hasResult = this.currentStatement.execute(sql);

      // Using a generic execute and result processing ensures that DBMS that
      // can process more than one statement with a single SQL are treated correctly.
      // e.g. when sending a SELECT and other statements as a "batch" with SQL Server
      processMoreResults(sql, result, hasResult);

      // processMoreResults() will have added any warnings and set the warning flag
      if (result.hasWarning())
      {
        // if the warning is actually an error, addExtendErrorInfo() will set the result to "failure"
        this.addExtendErrorInfo(sql, info, result);
      }

      if (result.isSuccess())
      {
        result.addMessage(getSuccessMessage(info, getVerb()));
      }

      this.currentConnection.releaseSavepoint(ddlSavepoint);

			if (isDrop && result.isSuccess())
			{
				removeFromCache(info);
			}
		}
		catch (Exception e)
		{
			this.currentConnection.rollback(ddlSavepoint);

      if (ignoreDropError)
      {
        addDropWarning(info, result);
        addErrorPosition(result, sql, e);
        result.setSuccess(); // must be done after addErrorPosition!
      }
			else
      {
        result.setFailure();
        addErrorStatement(result, sql);

        // if we have a warning and an exception we need to use the extended error info
        if (result.hasWarning())
        {
          // if addExtendedErrorInfo() added something, then there is no need to add the error possition
          // (assuming the more verbose error message already contains that information)
          if (!addExtendErrorInfo(sql, info, result))
          {
            addErrorPosition(result, sql, e);
          }
        }
        else
        {
          addErrorPosition(result, sql, e);
        }

        LogMgr.logUserSqlError("DdlCommand.execute()", sql, e);
      }
		}
		finally
		{
			// we know that we don't need the statement any longer, so to make
			// sure everything is cleaned up, we'll close it here
			done();
		}

		return result;
	}

  private void addDropWarning(DdlObjectInfo info, StatementRunnerResult result)
  {
    String msg;
    if (info != null)
    {
      msg = ResourceMgr.getFormattedString("MsgDropWarningNamed", info.getObjectName());
    }
    else
    {
      msg = ResourceMgr.getString("MsgDropWarning");
    }
    result.addMessage(msg);
  }

	private void removeFromCache(DdlObjectInfo info)
	{
		if (info == null) return;
		if (StringUtil.isEmptyString(info.getObjectName())) return;
    if (currentConnection == null) return;

		currentConnection.getObjectCache().removeTable(new TableIdentifier(info.getObjectName(), currentConnection));
	}

	@Override
	public void done()
	{
		super.done();
		this.ddlSavepoint = null;
	}

	public boolean isDropCommand(String sql)
	{
		if ("DROP".equals(this.verb))
		{
			return true;
		}
		if (!"ALTER".equals(this.verb))
		{
			return false;
		}
		Matcher m = alterDropPattern.matcher(sql);
		return m.find();
	}

	@Override
	protected String getSuccessMessage(DdlObjectInfo info, String verb)
	{
		String msg = super.getSuccessMessage(info, getVerb());
		if (msg == null)
		{
			return getDefaultSuccessMessage(null);
		}
		return msg;
	}

	/**
	 * Retrieve extended error information if the DBMS supports this.
	 *
	 * @return true if an error was added, false otherwise
	 *
	 * @see ErrorInformationReader#getErrorInfo(java.lang.String, java.lang.String, java.lang.String, boolean)
	 * @see ReaderFactory#getErrorInformationReader(workbench.db.WbConnection)
	 */
	private boolean addExtendErrorInfo(String sql, DdlObjectInfo info , StatementRunnerResult result)
	{
		if (info == null) return false;
		if (currentConnection == null) return false;

		ErrorInformationReader reader = ReaderFactory.getErrorInformationReader(currentConnection);
		if (reader == null) return false;

		ErrorDescriptor error = reader.getErrorInfo(sql, null, info.getObjectName(), info.getObjectType(), true);
		if (error == null) return false;

		if (error.getErrorPosition() == -1 && error.getErrorColumn() > -1 && error.getErrorLine() > -1)
		{
			int startOffset = 0;

			if (!currentConnection.getDbSettings().getErrorPosIncludesLeadingComments())
			{
				startOffset = SqlUtil.getRealStart(sql);
				sql = sql.substring(startOffset);
			}
			int offset = SqlUtil.getErrorOffset(sql, error);
			error.setErrorOffset(offset + startOffset);
		}
		result.addMessageNewLine();
		result.setFailure(error);
		result.addMessage(error.getErrorMessage());
		return true;
	}

	@Override
	public String getVerb()
	{
		return verb;
	}

}
