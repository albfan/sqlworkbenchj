/*
 * Created on 27. August 2002, 21:17
 */
package workbench.db.importer;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;

import workbench.db.WbConnection;
import workbench.exception.ExceptionUtil;
import workbench.interfaces.Interruptable;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.storage.RowActionMonitor;


/**
 *
 * @author  workbench@kellerer.org
 */
public class DataImporter
	implements Interruptable, RowDataReceiver
{
	private WbConnection dbConn;
	private String sql;

	private String tableName;

	private RowDataProducer source;
	private PreparedStatement updateStatement;

	private String targetTable = null;

	private int commitEvery = 0;

	private boolean keepRunning = true;

	private boolean success = false;
	private boolean hasWarning = false;
	private boolean deleteTarget = false;
	private boolean continueOnError = false;

	private long totalRows = 0;
	private int currentImportRow = 0;

	private int colCount;
	private ArrayList warnings = new ArrayList();
	private ArrayList errors = new ArrayList();
	private int[] columnTypes = null;

	private RowActionMonitor progressMonitor;
	private boolean isRunning = false;

	public DataImporter()
	{
	}

	public void setConnection(WbConnection aConn)
	{
		this.dbConn = aConn;
	}


	public void setRowActionMonitor(RowActionMonitor rowMonitor)
	{
		this.progressMonitor = rowMonitor;
		if (this.progressMonitor != null)
		{
			this.progressMonitor.setMonitorType(RowActionMonitor.MONITOR_INSERT);
		}
	}

	public void setProducer(RowDataProducer producer)
	{
		this.source = producer;
		this.source.setReceiver(this);
	}

	public void setCommitEvery(int aCount)
	{
		this.commitEvery = aCount;
	}
	public int getCommitEvery() { return this.commitEvery; }

	public void startBackgroundImport()
	{
		if (this.source == null) return;
		Thread t = new Thread()
		{
			public void run()
			{
				try { startImport(); } catch (Throwable th) {}
			}
		};
		t.setDaemon(true);
		t.setName("Wb-Import Thread");
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
	}

	public void startImport()
		throws IOException, SQLException, Exception
	{
		if (this.source == null) return;
		this.isRunning = true;
		this.source.start();
	}

	private void deleteTarget()
		throws SQLException
	{
		if (this.targetTable == null) return;
		String deleteSql = "DELETE FROM " + this.targetTable;
		Statement stmt = this.dbConn.createStatement();
		LogMgr.logDebug("DataImporter.deleteTarget()", "Executing: [" + deleteSql + "] to delete target table...");
		int rows = stmt.executeUpdate(deleteSql);
		this.warnings.add(rows + " " + ResourceMgr.getString("MsgImporterRowsDeleted") + " " + this.targetTable);
	}

	public boolean isRunning() { return this.isRunning; }
	public boolean isSuccess() { return this.errors.size() == 0; }
	public boolean hasWarning() { return this.warnings.size() > 0; }
	public long getAffectedRow() { return this.totalRows; }

	public String[] getErrors()
	{
		int count = this.errors.size();
		String[] result = new String[count];
		for (int i=0; i < count; i++)
		{
			result[i] = (String)this.errors.get(i);
		}
		return result;
	}

	public String[] getWarnings()
	{
		int count = this.warnings.size();
		String[] result = new String[count];
		for (int i=0; i < count; i++)
		{
			result[i] = (String)this.warnings.get(i);
		}
		return result;
	}

	public void cancelExecution()
	{
		this.isRunning = false;
		this.keepRunning = false;
		this.source.cancel();
		this.warnings.add(ResourceMgr.getString("MsgImportCancelled"));
		if (this.progressMonitor != null) this.progressMonitor.jobFinished();
	}

	/**
	 *	Callback function for RowDataProducer
	 */
	public void processRow(Object[] row) throws SQLException
	{
		if (row == null) return;
		if (row.length != this.colCount) return;
		StringBuffer values = new StringBuffer(row.length * 20);
		values.append("[");
		try
		{
			currentImportRow++;
			this.updateStatement.clearParameters();
			if (this.progressMonitor != null)
			{
				progressMonitor.setCurrentRow(currentImportRow, -1);
			}

			for (int i=0; i < row.length; i++)
			{
				if (i > 0) values.append(",");
				if (row[i] == null)
				{
					this.updateStatement.setNull(i + 1, this.columnTypes[i]);
					values.append("NULL");
				}
				else
				{
					this.updateStatement.setObject(i + 1, row[i]);
					values.append(row[i].toString());
				}
			}
			values.append("]");
			int rows = this.updateStatement.executeUpdate();
			this.totalRows += rows;
		}
		catch (SQLException e)
		{
			LogMgr.logError("DataImporter.processRow()", "Error importing row " + this.totalRows, e);
			this.errors.add(ResourceMgr.getString("ErrorImportingRow") + " " + currentImportRow);
			this.errors.add(ResourceMgr.getString("ErrorImportErrorMsg") + " " + e.getMessage());
			this.errors.add(ResourceMgr.getString("ErrorImportValues") + " " + values);
			this.errors.add("");
			if (!this.continueOnError) throw e;
		}
		if (this.commitEvery > 0 && ((this.totalRows % this.commitEvery) == 0) && !this.dbConn.getAutoCommit())
		{
			try
			{
				LogMgr.logDebug("DataImporter.processRow()", "Committing changes (commitEvery=" + this.commitEvery + ")");
				this.dbConn.commit();
			}
			catch (SQLException e)
			{
				String error = ExceptionUtil.getDisplay(e);
				this.errors.add(error);
				if (!continueOnError) throw e;
			}
		}
	}

	/**
	 *	Callback function from the RowDataProducer
	 */
	public void setTargetTable(String tableName, ColumnIdentifier[] columns)
		throws SQLException
	{
		this.targetTable = tableName;

		StringBuffer text = new StringBuffer(columns.length * 50);
		StringBuffer parms = new StringBuffer(columns.length * 20);

		text.append("INSERT INTO ");
		text.append(tableName);
		text.append(" (");
		this.colCount = columns.length;
		this.columnTypes = new int[this.colCount];
		for (int i=0; i < columns.length; i++)
		{
			this.columnTypes[i] = columns[i].getDataType();
			if (i > 0)
			{
				text.append(",");
				parms.append(",");
			}
			text.append(columns[i].getColumnName());
			parms.append('?');
		}
		text.append(") VALUES (");
		text.append(parms);
		text.append(")");
		try
		{
			this.sql = text.toString();
			this.updateStatement = this.dbConn.getSqlConnection().prepareStatement(this.sql);
		}
		catch (SQLException e)
		{
			LogMgr.logError("DataImporter.setTargetTable", "Error when creating SQL statement", e);
			this.errors.add(ResourceMgr.getString("ErrorImportInitTargetFailed"));
			this.errors.add(ExceptionUtil.getDisplay(e));
			this.updateStatement = null;
			throw e;
		}

		if (this.deleteTarget)
		{
			try
			{
				this.deleteTarget();
			}
			catch (Exception e)
			{
				LogMgr.logError("DataImporter.setTargetTable()", "Could not delete contents of table " + this.targetTable, e);
			}
		}

	}

	public void importFinished()
	{
		try
		{
			if (!this.dbConn.getAutoCommit())
			{
				LogMgr.logDebug("DataImporter.importFinished()", "Committing changes");
				this.dbConn.commit();
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("DataImporter.importFinished()", "Error commiting changes", e);
			this.errors.add(ExceptionUtil.getDisplay(e));
		}
		finally
		{
			this.isRunning = false;
			if (this.progressMonitor != null) this.progressMonitor.jobFinished();
		}
	}

	public void importCancelled()
	{
		try
		{
			if (!this.dbConn.getAutoCommit())
			{
				LogMgr.logDebug("DataImporter.importCancelled()", "Rollback changes");
				this.dbConn.rollback();
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("DataImporter.importCancelled()", "Error on rollback", e);
			this.errors.add(ExceptionUtil.getDisplay(e));
		}
		finally
		{
			this.isRunning = false;
			if (this.progressMonitor != null) this.progressMonitor.jobFinished();
		}
	}

	public boolean getContinueOnError() { return this.continueOnError; }
	public void setContinueOnError(boolean flag) { this.continueOnError = flag; }

	public boolean getDeleteTarget()
	{
		return deleteTarget;
	}

	public void setDeleteTarget(boolean deleteTarget)
	{
		this.deleteTarget = deleteTarget;
	}

}