/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.sql.commands;


import java.sql.SQLException;

import workbench.log.LogMgr;

import workbench.sql.SavepointStrategy;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

/**
 *
 * @author Thomas Kellerer
 */
public class TransactionStartCommand
  extends SqlCommand
{
  public static final String MANUAL_TRANSACTION_IN_PROGRESS = "manual_transaction";
  public static final String PREVIOUS_SP_STRATEGY = "savepoint_strategy";

  public static final TransactionStartCommand BEGIN = new TransactionStartCommand("BEGIN");
  public static final TransactionStartCommand BEGIN_WORK = new TransactionStartCommand("BEGIN WORK");
  public static final TransactionStartCommand BEGIN_TRANSACTION = new TransactionStartCommand("BEGIN TRANSACTION");
  public static final TransactionStartCommand BEGIN_TRAN = new TransactionStartCommand("BEGIN TRAN");
  public static final TransactionStartCommand START_TRANSACTION = new TransactionStartCommand("START TRANSACTION");

  private final String verb;

  private TransactionStartCommand(String sqlVerb)
  {
    this.verb = sqlVerb;
    this.isUpdatingCommand = true;
  }

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult(sql);

		try
		{
      this.currentStatement = currentConnection.createStatement();
      this.currentStatement.execute(sql);
			appendSuccessMessage(result);
			result.setSuccess();
      handleTransactionStart();
		}
		catch (Exception e)
		{
			addErrorInfo(result, sql, e);
			LogMgr.logUserSqlError("TransactionStartCommand.execute()", sql, e);
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

  private void handleTransactionStart()
  {
    if (currentConnection == null) return;
    if (!currentConnection.getAutoCommit()) return;

    try
    {
      LogMgr.logInfo("TransactionStartCommand.handleTransactionStart()", "Transaction start detected. Turning off auto commit");
      currentConnection.setAutoCommit(false);
      SavepointStrategy oldStrategy = runner.getSavepointStrategy();
      runner.setSavepointStrategy(SavepointStrategy.never);
      runner.setSessionProperty(MANUAL_TRANSACTION_IN_PROGRESS, "true");
      runner.setSessionProperty(PREVIOUS_SP_STRATEGY, oldStrategy.name());
    }
    catch (SQLException ex)
    {
      LogMgr.logError("handleTransactionStart.handleTransactionStart()", "Could disable auto commit!", ex);
    }
  }
}
