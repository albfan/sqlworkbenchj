/*
 * TableDeleter.java
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
package workbench.db;

import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import workbench.db.importer.TableDependencySorter;
import workbench.interfaces.JobErrorHandler;
import workbench.interfaces.StatusBar;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.util.ExceptionUtil;
import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class TableDeleter
{

	private WbConnection connection;
	private boolean cancelExecution;
	private Statement currentStatement;
	private JobErrorHandler errorHandler;
	private StatusBar statusDisplay;

	public TableDeleter(WbConnection con)
	{
		this.connection = con;
	}

	public void cancel()
	{
		this.cancelExecution = true;
	}

	/**
	 * Define a status bar where the progress can be displayed.
	 *
	 * @param status
	 */
	public void setStatusBar(StatusBar status)
	{
		this.statusDisplay = status;
	}

	/**
	 * Define an error handler to ask the user for input if anyhting goes wrong.
	 * If this is not set deleteTableData() will simply stop at the first exception
	 * while deleting the data.
	 *
	 * @param handler
	 */
	public void setErrorHandler(JobErrorHandler handler)
	{
		this.errorHandler = handler;
	}

	/**
	 * Delete the data from the list of tables.
	 * No dependency checking is done, the tables are deleted in the order
	 * in which they appear in the list.
	 * To delete the tables with respect to possible FK constraints, use
	 * {@link TableDependencySorter#sortForDelete(java.util.List, boolean) } before
	 * calling this method
	 *
	 * If an error handler is defined and an error occurs, the user will be prompted
	 * what to do. If the user aborts, deleteTableData() will re-throw the exception
	 * otherwise the exception will only be logged
	 *
	 * @param objectNames the list of tables to be deleted
	 * @param commitEach true = commit each table, false = one single commit at the end
	 * @param useTruncate true = use TRUNCATE instead of DELETE (implies commitEach = false)
	 *
	 * @return the tables were the data was actually deleted. This can be different to the input list,
	 * if one of the DELETEs caused an error but the user chose to continue anyway.
	 *
	 * @throws java.sql.SQLException if anything goes wrong and no error handler was defined or the user
	 * chose to abort due to an error.
	 *
	 * @see TableDependencySorter#sortForDelete(java.util.List, boolean)
	 * @see #setErrorHandler(workbench.interfaces.JobErrorHandler)
	 */
	public List<TableIdentifier> deleteTableData(List<TableIdentifier> objectNames, boolean commitEach, boolean useTruncate, boolean cascadedTruncate)
			throws SQLException
	{
		this.cancelExecution = false;
		boolean ignoreAll = false;

		if (useTruncate && !connection.getDbSettings().supportsTruncate())
		{
			useTruncate = false;
			LogMgr.logWarning("TableDeleterUI.deleteTables()", "Use of TRUNCATE requested, but DBMS does not support truncate. Using DELETE instead.");
		}

		boolean hasError = false;

		List<TableIdentifier> deletedTables = new ArrayList<>();

		try
		{
			this.currentStatement = this.connection.createStatement();
		}
		catch (SQLException e)
		{
			LogMgr.logError("TableDeleterUI.deleteTables()", "Error creating statement", e);
			throw e;
		}

		try
		{
			this.connection.setBusy(true);

			for (TableIdentifier table : objectNames)
			{
				if (this.cancelExecution)
				{
					break;
				}
				if (this.statusDisplay != null)
				{
					this.statusDisplay.setStatusMessage(ResourceMgr.getFormattedString("TxtDeletingTable", table.getTableName()));
				}
				try
				{
					deleteTable(table, useTruncate, commitEach, cascadedTruncate);
					deletedTables.add(table);
				}
				catch (SQLException ex)
				{
					String error = ExceptionUtil.getDisplay(ex);
					LogMgr.logError("TableDeleter.deleteTableData()", "Error deleting table " + table, ex);

					if (errorHandler == null)
					{
						this.connection.rollback();
						throw ex;
					}
					else if (!ignoreAll)
					{
						String question = ResourceMgr.getString("ErrDeleteTableData");
						question = question.replace("%table%", table.toString());
						question = question.replace("%error%", error);
						question = question + "\n" + ResourceMgr.getString("MsgContinueQ");
						int choice = errorHandler.getActionOnError(-1, null, null, question);
						if (choice == JobErrorHandler.JOB_ABORT)
						{
							// the hasError flag will cause a rollback at the end.
							hasError = true;
							break;
						}
						else if (choice == JobErrorHandler.JOB_CONTINUE)
						{
							// only ignore this error
							hasError = false;
						}
						else if (choice == JobErrorHandler.JOB_IGNORE_ALL)
						{
							// if we ignore all errors we should do a commit at the
							// end in order to ensure that the delete's which were
							// successful are committed.
							hasError = false;
							ignoreAll = true;
						}
					}
				}
			}

			boolean commitNeeded = !connection.getAutoCommit();

			if (commitNeeded && useTruncate)
			{
				commitNeeded = connection.getDbSettings().truncateNeedsCommit();
			}

			boolean commitDone = false;

			try
			{
				if (commitNeeded)
				{
					if (hasError || cancelExecution)
					{
						commitDone = false;
						connection.rollback();
					}
					else if (commitNeeded)
					{
						commitDone = true;
						connection.commit();
					}
				}
			}
			catch (SQLException e)
			{
				LogMgr.logError("TableDeleter.deleteTableData()", "Error on commit/rollback", e);
				String error = ExceptionUtil.getDisplay(e);
				String msg = null;

				if (commitDone)
				{
					msg = ResourceMgr.getFormattedString("ErrCommit", error);
				}
				else if (commitNeeded)
				{
					msg = ResourceMgr.getFormattedString("ErrRollbackTableData", error);
				}
				else
				{
					msg = error;
				}

				if (this.errorHandler != null)
				{
					errorHandler.fatalError(msg);
				}
			}
		}
		finally
		{
			SqlUtil.closeStatement(currentStatement);
			connection.setBusy(false);
		}
		if (statusDisplay != null)
		{
			statusDisplay.clearStatusMessage();
		}

		return deletedTables;
	}

	private void deleteTable(final TableIdentifier table, final boolean useTruncate, final boolean doCommit, final boolean cascadedTruncate)
			throws SQLException
	{
		Savepoint sp = null;
		try
		{
			if (connection.getDbSettings().useSavePointForDML())
			{
				sp = connection.setSavepoint();
			}
			String deleteSql = getDeleteStatement(table, useTruncate, cascadedTruncate);
			LogMgr.logInfo("TableDeleterUI.deleteTable()", "Executing: [" + deleteSql + "] to delete target table...");
			currentStatement.executeUpdate(deleteSql);
			if (doCommit && !this.connection.getAutoCommit())
			{
				this.connection.commit();
			}
			else
			{
				connection.releaseSavepoint(sp);
			}
		}
		catch (SQLException e)
		{
			connection.rollback(sp);
			if (doCommit && !this.connection.getAutoCommit())
			{
				this.connection.rollback();
			}
			throw e;
		}
	}

	private String getDeleteStatement(final TableIdentifier table, final boolean useTruncate, boolean cascade)
	{
		String deleteSql = null;
		String tableName = table.createCopy().getTableExpression(this.connection);
		if (useTruncate)
		{
			String sql = connection.getDbSettings().getTruncateCommand(cascade);
			if (sql != null)
			{
				deleteSql = sql.replace(MetaDataSqlManager.TABLE_NAME_PLACEHOLDER, tableName);
			}
		}
		if (deleteSql == null)
		{
			deleteSql = "DELETE FROM " + tableName;
		}
		return deleteSql;
	}

	public CharSequence generateScript(List<TableIdentifier> objectNames, boolean commitEach, boolean useTruncate, boolean cascadedTruncate)
	{
		boolean commitTruncate = connection.getDbSettings().truncateNeedsCommit();
		if (commitEach && useTruncate)
		{
			commitEach = commitTruncate;
		}
		StringBuilder script = new StringBuilder(objectNames.size() * 30);
		for (TableIdentifier table : objectNames)
		{
			String sql = this.getDeleteStatement(table, useTruncate, cascadedTruncate);
			script.append(sql);
			script.append(";\n");
			if (commitEach)
			{
				script.append("COMMIT;\n\n");
			}
		}

		boolean commitNeeded = !commitEach;
		if (commitNeeded && useTruncate)
		{
			commitNeeded = commitTruncate;
		}

		if (commitNeeded)
		{
			script.append("\nCOMMIT;\n");
		}
		return script;
	}
}
