/*
 * RunScriptPanel.java
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
package workbench.gui.dbobjects;

import java.awt.Frame;

import javax.swing.SwingWorker;

import workbench.interfaces.Interruptable;
import workbench.interfaces.ResultLogger;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.db.WbConnection;

import workbench.gui.WbSwingUtilities;

import workbench.storage.RowActionMonitor;

import workbench.sql.BatchRunner;
import workbench.sql.ErrorDescriptor;
import workbench.sql.ErrorReportLevel;
import workbench.sql.ExecutionStatus;

import workbench.util.ExceptionUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class ScriptExecutionFeedback
  extends SwingWorker<ExecutionStatus, String>
  implements Interruptable
{
  private WbConnection dbConn;
  private BatchRunner runner;
  private String sqlScript;
  private ResultLogger resultLogger;
  private ProgressDialog dialog;

  public ScriptExecutionFeedback(WbConnection con, String script, ResultLogger errorDisplay)
  {
    dbConn = con;
    sqlScript = script;
    resultLogger = errorDisplay;
  }

  public void runScript(String object, Frame parentWindow)
  {
    dialog = new ProgressDialog(ResourceMgr.getString("TxtWindowTitleGeneratedScript"), parentWindow, this, false);
    dialog.getInfoPanel().setCurrentObject(object, -1, -1);
    WbSwingUtilities.center(dialog, parentWindow);
    execute();
    dialog.setVisible(true);
  }

  public ErrorDescriptor getLastError()
  {
    if (runner == null) return null;
    return runner.getLastError();
  }

  public int getLastErrorIndex()
  {
    if (runner == null) return -1;
    return runner.getLastErrorStatementIndex();
  }

  @Override
  public void cancelExecution()
  {
    if (runner != null)
    {
      runner.cancel();
    }
    this.cancel(true);
  }

  @Override
  public boolean confirmCancel()
  {
    return true;
  }

  @Override
  protected ExecutionStatus doInBackground()
    throws Exception
  {
    return startScript();
  }

  @Override
  protected void done()
  {
    super.done();
    if (dialog != null)
    {
      dialog.setVisible(false);
      dialog.dispose();
    }
  }

  protected ExecutionStatus startScript()
  {
    if (dbConn == null) return ExecutionStatus.Error;
    if (dbConn.isBusy()) return ExecutionStatus.Error;

    runner = new BatchRunner();
    if (dialog != null)
    {
      RowActionMonitor monitor = dialog.getMonitor();
      monitor.setMonitorType(RowActionMonitor.MONITOR_PLAIN);
      runner.setRowMonitor(monitor);
    }
    runner.setConnection(dbConn);
    runner.setAbortOnError(true);
    runner.setPrintStatements(false);
    runner.setShowProgress(false);
    runner.setShowTiming(false);
    runner.setStoreErrors(false);
    runner.setShowStatementWithResult(false);
    runner.setShowStatementSummary(false);
    runner.setErrorStatementLogging(ErrorReportLevel.none);
    runner.setAbortOnError(true);
    runner.setStoreErrors(true);
    runner.setResultLogger(resultLogger);

    ExecutionStatus status = ExecutionStatus.Error;

    try
    {
      status = runner.runScript(sqlScript);
    }
    catch (Exception e)
    {
      status = ExecutionStatus.Error;
      LogMgr.logError("RunScriptPanel.runScript()", "Error when running script", e);
      final String error = ExceptionUtil.getDisplay(e);
      resultLogger.appendToLog("\n" + error);
    }
    finally
    {
      runner.done();
    }

    return status;
  }

}
