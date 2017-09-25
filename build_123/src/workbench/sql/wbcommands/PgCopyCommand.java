/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer.
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
package workbench.sql.wbcommands;

import java.io.StringReader;
import java.sql.SQLException;

import workbench.log.LogMgr;

import workbench.db.postgres.PgCopyManager;

import workbench.storage.DataStore;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.sql.lexer.SQLLexer;
import workbench.sql.lexer.SQLLexerFactory;
import workbench.sql.lexer.SQLToken;
import workbench.sql.parser.ParserType;

import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class PgCopyCommand
  extends SqlCommand
{
  public static final String VERB = "COPY";

  @Override
  public StatementRunnerResult execute(String sql)
    throws SQLException, Exception
  {

    // contains only the copy statement
    String copy = "";

    boolean fromStdin = false;
    boolean toStdout = false;
    int dataStart = -1;

    SQLLexer lexer = SQLLexerFactory.createLexer(ParserType.Postgres, sql);
    SQLToken token = lexer.getNextToken(true, true);
    SQLToken lastToken = null;

    while (token != null)
    {
      if (!token.isComment() && !token.isWhiteSpace())
      {
        if (fromStdin && token.getText().equals(";"))
        {
          dataStart = token.getCharEnd();
          dataStart = StringUtil.findNextLineStart(sql, dataStart);
          break;
        }
        if (token.getText().equalsIgnoreCase("stdin") && lastToken != null && lastToken.getText().equalsIgnoreCase("FROM"))
        {
          fromStdin = true;
        }

        if (token.getText().equalsIgnoreCase("stdout") && lastToken != null && lastToken.getText().equalsIgnoreCase("TO"))
        {
          toStdout = true;
          break;
        }
        lastToken = token;
      }
      copy += token.getText();
      token = lexer.getNextToken(true, true);
    }

    if (fromStdin)
    {
      return processCopyIn(sql, copy, dataStart);
    }
    else if (toStdout)
    {
      return processCopyOut(sql);
    }
    return super.execute(sql);
  }

  private StatementRunnerResult processCopyOut(String sql)
  {
    StatementRunnerResult result = new StatementRunnerResult(sql);
    try
    {
      runner.setSavepoint();
      PgCopyManager pgCopy = new PgCopyManager(currentConnection);
      DataStore ds = pgCopy.copyToStdOut(sql);
      if (ds != null)
      {
        result.addDataStore(ds);
        result.setSuccess();
        appendSuccessMessage(result);
      }
      runner.releaseSavepoint();
    }
    catch (Exception ex)
    {
      runner.rollbackSavepoint();
      addErrorInfo(result, sql, ex);
      LogMgr.logUserSqlError("PgCopyCommand.processCopyOut()", sql, ex);
    }
    return result;
  }

  private StatementRunnerResult processCopyIn(String sql, String copy, int dataStart)
  {
    StatementRunnerResult result = new StatementRunnerResult(copy);
    try
    {
      runner.setSavepoint();
      StringReader reader = new StringReader(sql);
      reader.skip(dataStart);

      PgCopyManager pgCopy = new PgCopyManager(currentConnection);
      pgCopy.copyFromStdin(copy, reader);
      long rows = pgCopy.processStreamData();
      result.addUpdateCountMsg((int)rows);
      result.setSuccess();
      appendSuccessMessage(result);
      runner.releaseSavepoint();
    }
    catch (Exception ex)
    {
      runner.rollbackSavepoint();
      addErrorInfo(result, copy, ex);
      LogMgr.logUserSqlError("PgCopyCommand.processCopyIn()", copy, ex);
    }
    return result;
  }

  @Override
  public String getVerb()
  {
    return VERB;
  }

}
