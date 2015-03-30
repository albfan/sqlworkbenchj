/*
 * TransactionEndCommand.java
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
package workbench.sql.commands;

import java.sql.SQLException;

import workbench.log.LogMgr;

import workbench.sql.SavepointStrategy;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.sql.lexer.SQLLexer;
import workbench.sql.lexer.SQLLexerFactory;
import workbench.sql.lexer.SQLToken;

import workbench.util.StringUtil;

/**
 * Handles COMMIT and ROLLBACK
 *
 * @author Thomas Kellerer
 */
public class TransactionEndCommand
  extends SqlCommand
{
	public static final String COMMIT_VERB = "COMMIT";
	public static final String ROLLBACK_VERB = "ROLLBACK";

	public static SqlCommand getCommit()
	{
		return new TransactionEndCommand(COMMIT_VERB);
	}

	public static SqlCommand getRollback()
	{
		return new TransactionEndCommand(ROLLBACK_VERB);
	}

	private String verb;

	private TransactionEndCommand(String sqlVerb)
	{
		super();
		this.verb = sqlVerb;
		this.isUpdatingCommand = COMMIT_VERB.equalsIgnoreCase(this.verb);
	}

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult(sql);

		try
		{
			if (currentConnection.useJdbcCommit())
			{
				if (COMMIT_VERB.equals(this.verb))
				{
					currentConnection.getSqlConnection().commit();
				}
				else if (ROLLBACK_VERB.equals(this.verb))
				{
					currentConnection.getSqlConnection().rollback();
				}
			}
			else
			{
				this.currentStatement = currentConnection.createStatement();

        // use the complete statement from the user, because this could be a "rollback to savepoint"
        // which is not the same as calling currentConnection.rollback()
				this.currentStatement.execute(sql);
			}

      if (shouldHandleTransactionEnd(sql))
      {
        handleTransactionEnd();
      }
      runner.removeSessionProperty(TransactionStartCommand.MANUAL_TRANSACTION_IN_PROGRESS);

			appendSuccessMessage(result);
			result.setSuccess();
			processMoreResults(sql, result, false);
		}
		catch (Exception e)
		{
			addErrorInfo(result, sql, e);
			LogMgr.logUserSqlError("SingleVerbCommand.execute()", sql, e);
		}
		finally
		{
			done();
		}

		return result;
	}

	@Override
	public String getVerb()
	{
		return verb;
	}

  private boolean isManualTransaction()
  {
    return StringUtil.stringToBool(runner.getSessionAttribute(TransactionStartCommand.MANUAL_TRANSACTION_IN_PROGRESS));
  }

  private boolean shouldHandleTransactionEnd(String sql)
  {
    if (!isManualTransaction()) return false;

    // no special treatment for commit
    if (getVerb().equals("COMMIT")) return true;

    // not a commit, so we need to check if this is a rollback to a savepoint
    SQLLexer lexer = SQLLexerFactory.createLexer(currentConnection, sql);

    SQLToken token = lexer.getNextToken(false, false); // this is the ROLLBACK

    // second token
    token = lexer.getNextToken(false, false);

    // no second token, only a single "rollback" used
    if (token == null) return true;

    if (token.getText().equalsIgnoreCase("TO"))
    {
      // "rollback to savepoint" does not end the transaction
      return false;
    }

    // this is a "rollback work" and that can still roll back to a savepoint
    // using "rollback work to xxx"
    token = lexer.getNextToken(false, false);

    if (token != null && token.getText().equalsIgnoreCase("TO"))
    {
      // "rollback work to savepoint" does not end the transaction
      return false;
    }

    // this is a real rollback
    return true;
  }

  private void handleTransactionEnd()
  {
    try
    {
      LogMgr.logInfo("TransactionEndCommand.handleTransactionEnd()", "Transaction end detected. Turning auto commit back on");
      runner.getConnection().setAutoCommit(true);
    }
    catch (Exception ex)
    {
      LogMgr.logError("TransactionEndCommand.handleTransactionEnd()", "Could disable auto commit!", ex);
    }

    String strategy = runner.getSessionAttribute(TransactionStartCommand.PREVIOUS_SP_STRATEGY);
    if (strategy != null)
    {
      runner.setSavepointStrategy(SavepointStrategy.valueOf(strategy));
      runner.removeSessionProperty(TransactionStartCommand.PREVIOUS_SP_STRATEGY);
    }
  }

}
