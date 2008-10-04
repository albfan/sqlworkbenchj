/*
 * TableDeleter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.SQLException;
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
 * @author support@sql-workbench.net
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
	public List<TableIdentifier> deleteTableData(List<TableIdentifier> objectNames, boolean commitEach, boolean useTruncate)
		throws SQLException
	{
		this.cancelExecution = false;
		boolean ignoreAll = false;

		if (useTruncate && !connection.getDbSettings().supportsTruncate())
		{
			useTruncate = false;
			LogMgr.logWarning("TableDeleterUI.deleteTables()", "Use of TRUNCATE requested, but DBMS does not support truncate. Using DELETE instead.");
		}
		
		if (useTruncate)
		{
			commitEach = false;
		}
		boolean hasError = false;

		List<TableIdentifier> deletedTables = new ArrayList<TableIdentifier>();

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
					this.deleteTable(table, useTruncate, commitEach);
					deletedTables.add(table);
				}
				catch (SQLException ex)
				{
					String error = ExceptionUtil.getDisplay(ex);
					LogMgr.logError("TableDeleter.deleteTableData()", "Error deleting table " + table, ex);

					if (errorHandler == null)
					{
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
						else if (choice == JobErrorHandler.JOB_IGNORE_ALL)
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

			boolean doCommit = true;
			try
			{
				if (hasError || cancelExecution)
				{
					doCommit = false;
					this.connection.rollback();
				}
				else
				{
					this.connection.commit();
				}
			}
			catch (SQLException e)
			{
				LogMgr.logError("TableDeleter.deleteTableData()", "Error on commit/rollback", e);
				String error = ExceptionUtil.getDisplay(e);
				String msg = null;

				if (doCommit)
				{
					msg = ResourceMgr.getFormattedString("ErrCommit", error);
				}
				else
				{
					msg = ResourceMgr.getFormattedString("ErrRollbackTableData", error);
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
			this.connection.setBusy(false);
		}
		if (this.statusDisplay != null) this.statusDisplay.clearStatusMessage();
		
		return deletedTables;
	}

	private void deleteTable(final TableIdentifier table, final boolean useTruncate, final boolean doCommit)
		throws SQLException
	{
		try
		{
			String deleteSql = getDeleteStatement(table, useTruncate);
			LogMgr.logInfo("TableDeleterUI.deleteTable()", "Executing: [" + deleteSql + "] to delete target table...");
			currentStatement.executeUpdate(deleteSql);
			if (doCommit && !this.connection.getAutoCommit())
			{
				this.connection.commit();
			}
		}
		catch (SQLException e)
		{
			if (doCommit && !this.connection.getAutoCommit())
			{
				this.connection.rollback();
			}
			LogMgr.logError("TableDeleterUI.deleteTable()", "Error when deleting table!", e);
			throw e;
		}
	}

	private String getDeleteStatement(final TableIdentifier table, final boolean useTruncate)
	{
		String deleteSql = null;
		String tableName = table.getTableExpression(this.connection);
		if (useTruncate)
		{
			deleteSql = "TRUNCATE TABLE " + tableName;
		}
		else
		{
			deleteSql = "DELETE FROM " + tableName;
		}
		return deleteSql;
	}

	public CharSequence generateScript(List<TableIdentifier> objectNames, boolean commitEach, boolean useTruncate)
	{
		if (useTruncate)
		{
			commitEach = false;
		}
		StringBuilder script = new StringBuilder(objectNames.size() * 30);
		for (TableIdentifier table : objectNames)
		{
			String sql = this.getDeleteStatement(table, useTruncate);
			script.append(sql);
			script.append(";\n");
			if (commitEach)
			{
				script.append("COMMIT;\n\n");
			}
		}

		if (!commitEach && !useTruncate)
		{
			script.append("\nCOMMIT;\n");
		}
		return script;
	}
	
}
