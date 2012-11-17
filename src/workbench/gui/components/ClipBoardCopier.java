/*
 * ClipBoardCopier.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumnModel;
import workbench.WbManager;
import workbench.console.DataStorePrinter;
import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.db.exporter.BlobMode;
import workbench.db.exporter.ExportType;
import workbench.db.exporter.SqlRowDataConverter;
import workbench.gui.WbSwingUtilities;
import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.storage.DataPrinter;
import workbench.storage.DataStore;
import workbench.storage.RowData;
import workbench.util.ExceptionUtil;
import workbench.util.StrBuffer;
import workbench.util.StringUtil;
import workbench.util.WbThread;

/**
 * A class to copy the data of a {@link workbench.gui.components.WbTable} to
 * the clipboard. Either as tab-separated text or SQL Statements.
 *
 * @author Thomas Kellerer
 */
public class ClipBoardCopier
{
	private DataStore data;
	private WbTable client;

	/**
	 * Create a new ClipBoardCopier to copy the contents of the given table.
	 *
	 * @param table  the table for which the data should be copied.
	 */
	public ClipBoardCopier(WbTable table)
	{
		this.client = table;
		this.data = client.getDataStore();
	}

	/**
	 * For testing purposes only.
	 *
	 * @param ds the datastore containing the data to copy
	 */
	ClipBoardCopier(DataStore ds)
	{
		this.data = ds;
	}

	/**
	 *	Copy data from the table as tab-delimited into the clipboard
	 *
	 *	@param includeHeaders if true, then a header line with the column names is copied as well
	 *  @param selectedOnly if true, then only selected rows are copied, else all rows
	 *  @param showSelectColumns if true, a dialog will be presented to the user to select the columns to be included
	 */
	public void copyDataToClipboard(boolean includeHeaders, boolean selectedOnly, final boolean showSelectColumns)
	{
		if (this.data == null)
		{
			WbSwingUtilities.showErrorMessage(client, "No DataStore available!");
			LogMgr.logError("ClipBoardCopier.copyDataToClipboard()", "Cannot copy without a DataStore!", null);
			return;
		}

		if (this.data.getRowCount() <= 0) return;

		List<ColumnIdentifier> columnsToCopy = null;
		if (selectedOnly  && !showSelectColumns && client != null && this.client.getColumnSelectionAllowed())
		{
			columnsToCopy = getColumnsFromSelection();
		}

		boolean doTextFormat = false;
		if (showSelectColumns && client != null)
		{
			// Display column selection dialog
			ColumnSelectionResult result = this.selectColumns(includeHeaders, selectedOnly, true, client.getSelectedRowCount() > 0, true);
			if (result == null) return;

			columnsToCopy = result.columns;
			includeHeaders = result.includeHeaders;
			selectedOnly = result.selectedOnly;
			doTextFormat = result.formatText;
		}

		try
		{
			int count = this.data.getRowCount();
			int[] rows = null;
			if (selectedOnly)
			{
				rows = client == null ? null : this.client.getSelectedRows();
				count = rows == null ? 0 : rows.length;
			}

			StringWriter out = new StringWriter(count * 250);
			if (doTextFormat)
			{
				DataStorePrinter printer = new DataStorePrinter(this.data);
				printer.setFormatColumns(true);
				printer.setPrintRowCount(false);
				if (columnsToCopy != null)
				{
					List<String> colNames =new ArrayList<String>(columnsToCopy.size());
					for (ColumnIdentifier id : columnsToCopy)
					{
						colNames.add(id.getColumnName());
					}
					printer.setColumnsToPrint(colNames);
				}
				PrintWriter pw = new PrintWriter(out);
				printer.printTo(pw);
			}
			else
			{
				// Do not use StringUtil.LINE_TERMINATOR for the line terminator
				// because for some reason this creates additional empty lines
				// under Windows
				DataPrinter printer = new DataPrinter(this.data, "\t", "\n", columnsToCopy, includeHeaders);
				printer.setNullString(GuiSettings.getDisplayNullString());
				printer.setColumnMapping(getColumnOrder());

				printer.writeDataString(out, rows);
			}

			Clipboard clp = Toolkit.getDefaultToolkit().getSystemClipboard();
			WbSwingUtilities.showWaitCursorOnWindow(this.client);
			StringSelection sel = new StringSelection(out.toString());
			clp.setContents(sel, sel);
		}
		catch (Throwable ex)
		{
			if (ex instanceof OutOfMemoryError)
			{
				WbManager.getInstance().showOutOfMemoryError();
			}
			else
			{
				String msg = ResourceMgr.getString("ErrClipCopy");
				msg = StringUtil.replace(msg, "%errmsg%", ExceptionUtil.getDisplay(ex));
				WbSwingUtilities.showErrorMessage(client, msg);
			}
			LogMgr.logError(this, "Could not copy text data to clipboard", ex);
		}
		WbSwingUtilities.showDefaultCursorOnWindow(this.client);
	}

	private int[] getColumnOrder()
	{
		if (client == null) return null;
		if (!client.isColumnOrderChanged()) return null;

		TableColumnModel model = client.getColumnModel();
		int colCount = model.getColumnCount();
		int[] result = new int[colCount];

		for (int i=0; i < colCount; i++)
		{
			int modelIndex = model.getColumn(i).getModelIndex();
			result[i] = modelIndex;
		}
		return result;
	}

	public void copyAsSqlInsert(boolean selectedOnly, boolean showSelectColumns)
	{
		this.copyAsSql(ExportType.SQL_INSERT, selectedOnly, showSelectColumns);
	}

	public void copyAsSqlDeleteInsert(boolean selectedOnly, boolean showSelectColumns)
	{
		this.copyAsSql(ExportType.SQL_DELETE_INSERT, selectedOnly, showSelectColumns);
	}

	/**
	 * Copy the data of the client table as SQL UPDATE statements to the clipboard.
	 * Before copying, the primary key columns of the underlying {@link workbench.storage.DataStore}
	 * are checked. If none are present, the user is prompted to select the key columns
	 *
	 * @see workbench.storage.DataStore#hasPkColumns()
	 * @see workbench.gui.components.WbTable#detectDefinedPkColumns()
	 * @see #copyAsSql(boolean, boolean, boolean, boolean)
	 */
	public void copyAsSqlUpdate(boolean selectedOnly, boolean showSelectColumns)
	{
		copyAsSql(ExportType.SQL_UPDATE, selectedOnly, showSelectColumns);
	}


	/**
	 * 	Copy the data of the client table into the clipboard using SQL statements
	 */
	public void copyAsSql(final ExportType type, final boolean selectedOnly, final boolean showSelectColumns)
	{
		if (this.data == null)
		{
			// Should not happen.
			WbSwingUtilities.showErrorMessage(client, "No DataStore available!");
			LogMgr.logError("ClipBoardCopier.copyAsSql()", "Cannot copy without a DataStore!", null);
			return;
		}

		// For some reason the statusbar will not be updated if
		// this is run in the AWT thread, so we have to
		// create a new thread to run the actual copy
		WbThread t = new WbThread("CopyThread")
		{
			@Override
			public void run()
			{
				doCopyAsSql(type, selectedOnly, showSelectColumns);
			}
		};
		t.start();
	}

	private boolean needsPK(ExportType type)
	{
		return type == ExportType.SQL_UPDATE || type == ExportType.SQL_MERGE || type == ExportType.SQL_DELETE_INSERT;
	}

	public void doCopyAsSql(final ExportType type, boolean selectedOnly, final boolean showSelectColumns)
	{
		if (this.data.getRowCount() <= 0) return;

		if (needsPK(type))
		{
			boolean pkOK = this.data.hasPkColumns();
			if (!pkOK && this.client != null)
			{
				this.client.checkPkColumns(true);
			}

			// re-check in case the user simply clicked OK during the PK prompt
			pkOK = this.data.hasPkColumns();

			// Can't do anything if we don't have PK
			if (!pkOK)
			{
				LogMgr.logError("ClipBoardCopier._copyAsSql()", "Cannot create UPDATE or DELETE statements without a primary key!", null);
				if (!WbManager.isTest()) WbSwingUtilities.showErrorMessageKey(client, "ErrCopyNotAvailable");
				return;
			}
		}

		TableIdentifier updateTable = data.getUpdateTable();
		if (updateTable == null && client != null)
		{
			UpdateTableSelector selector = new UpdateTableSelector(client);
			updateTable = selector.selectUpdateTable();
			if (updateTable != null)
			{
				client.getDataStore().setUpdateTable(updateTable);
			}
		}

		List<ColumnIdentifier> columnsToInclude = null;
		if (selectedOnly  && !showSelectColumns && this.client.getColumnSelectionAllowed())
		{
			columnsToInclude = getColumnsFromSelection();
		}

		if (showSelectColumns && !WbManager.isTest())
		{
      ColumnSelectionResult result = this.selectColumns(false, selectedOnly, false, client.getSelectedRowCount() > 0, false);
			if (result == null) return;
			columnsToInclude = result.columns;
      selectedOnly = result.selectedOnly;
		}

		// Now check if the selected columns are different to the key columns.
		// If only key columns are selected then creating an UPDATE statement does not make sense.
		if (type == ExportType.SQL_UPDATE)
		{
			List<ColumnIdentifier> keyColumns = new ArrayList<ColumnIdentifier>();
			for (ColumnIdentifier col : data.getResultInfo().getColumns())
			{
				if (col.isPkColumn())
				{
					keyColumns.add(col);
				}
			}

			if (columnsToInclude != null && columnsToInclude.size() == keyColumns.size() && columnsToInclude.containsAll(keyColumns))
			{
				LogMgr.logError("ClipBoardCopier._copyAsSql()", "Cannot create UPDATE statement with only key columns!", null);
				if (!WbManager.isTest()) WbSwingUtilities.showErrorMessageKey(client, "ErrCopyNoNonKeyCols");
				return;
			}
		}

		try
		{
			if (!WbManager.isTest()) WbSwingUtilities.showWaitCursorOnWindow(this.client);

			int[] rows = null;
			if (selectedOnly) rows = this.client.getSelectedRows();

			SqlRowDataConverter converter = new SqlRowDataConverter(data.getOriginalConnection());
			converter.setIncludeTableOwner(Settings.getInstance().getIncludeOwnerInSqlExport());
			converter.setResultInfo(data.getResultInfo());
			converter.setDateLiteralType(Settings.getInstance().getDefaultCopyDateLiteralType());
			converter.setType(type);

			if (type == ExportType.SQL_INSERT || type == ExportType.SQL_DELETE_INSERT)
			{
				if (data.getResultInfo().getUpdateTable() == null)
				{
					String tbl = data.getInsertTable();
					TableIdentifier table = new TableIdentifier(tbl, data.getOriginalConnection());
					converter.setAlternateUpdateTable(table);
				}
			}

			converter.setColumnsToExport(columnsToInclude);
			converter.setBlobMode(BlobMode.DbmsLiteral);

			int count;
			if (rows != null)
			{
				count = rows.length;
			}
			else
			{
				count = data.getRowCount();
			}

			StringBuilder result = new StringBuilder(count * 100);
			RowData rowdata = null;

			for (int row = 0; row < count; row ++)
			{
				if (rows == null)
				{
					rowdata = this.data.getRow(row);
				}
				else
				{
					rowdata = data.getRow(rows[row]);
				}
				StrBuffer sql = converter.convertRowData(rowdata, row);
				sql.appendTo(result);
				if (!sql.endsWith('\n'))
				{
					result.append('\n');
				}
			}

			Clipboard clp = Toolkit.getDefaultToolkit().getSystemClipboard();
			StringSelection sel = new StringSelection(result.toString());
			clp.setContents(sel, sel);
		}
		catch (Throwable e)
		{
			if (e instanceof OutOfMemoryError)
			{
				WbManager.getInstance().showOutOfMemoryError();
			}
			else
			{
				String msg = ResourceMgr.getString("ErrClipCopy");
				msg = StringUtil.replace(msg, "%errmsg%", ExceptionUtil.getDisplay(e));
				if (!WbManager.isTest()) WbSwingUtilities.showErrorMessage(client, msg);
			}
			LogMgr.logError("ClipboardCopier.doCopyAsSql()", "Error when copying as SQL", e);
		}
		finally
		{
			if (!WbManager.isTest()) WbSwingUtilities.showDefaultCursorOnWindow(this.client);
		}

	}


	/**
	 *	A general purpose method to select specific columns from the result set
	 *  this is e.g. used for copying data to the clipboard
	 *
	 */
	public ColumnSelectionResult selectColumns(boolean includeHeader, boolean selectedOnly, boolean showHeaderSelection, boolean showSelectedRowsSelection, boolean showTextFormat)
	{
		if (this.data == null) return null;

		ColumnSelectionResult result = new ColumnSelectionResult();
		result.includeHeaders = includeHeader;
		result.selectedOnly = selectedOnly;

		ColumnIdentifier[] originalCols = this.data.getColumns();
		ColumnSelectorPanel panel = new ColumnSelectorPanel(originalCols, includeHeader, selectedOnly, showHeaderSelection, showSelectedRowsSelection, showTextFormat);
		panel.selectAll();
		int choice = JOptionPane.showConfirmDialog(SwingUtilities.getWindowAncestor(this.client), panel, ResourceMgr.getString("MsgSelectColumnsWindowTitle"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

		if (choice == JOptionPane.OK_OPTION)
		{
			result.columns = panel.getSelectedColumns();
			result.includeHeaders = panel.includeHeader();
			result.selectedOnly = panel.selectedOnly();
			result.formatText = panel.formatTextOutput();
		}
		else
		{
			result = null;
		}
		return result;
	}

	private List<ColumnIdentifier> getColumnsFromSelection()
	{
		int[] cols = this.client.getSelectedColumns();
		DataStore ds = this.client.getDataStore();
		if (ds == null) return Collections.emptyList();
		List<ColumnIdentifier> result = new ArrayList<ColumnIdentifier>(cols.length);
		TableColumnModel model = client.getColumnModel();
		for (int i=0; i < cols.length; i++)
		{
			int realIndex = model.getColumn(cols[i]).getModelIndex();
			result.add(ds.getResultInfo().getColumn(realIndex));
		}
		return result;
	}
}

