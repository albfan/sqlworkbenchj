/*
 * ClipBoardCopier.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumnModel;
import workbench.WbManager;
import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.db.exporter.BlobMode;
import workbench.db.exporter.SqlRowDataConverter;
import workbench.gui.WbSwingUtilities;
import workbench.log.LogMgr;
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

	public ClipBoardCopier(WbTable t)
	{
		this.client = t;
		this.data = client.getDataStore();
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
		if (selectedOnly  && !showSelectColumns && this.client.getColumnSelectionAllowed())
		{
			columnsToCopy = getColumnsFromSelection();
		}

		if (showSelectColumns)
		{
			// Display column selection dialog
			ColumnSelectionResult result = this.selectColumns(includeHeaders, selectedOnly, true, client.getSelectedRowCount() > 0);
			if (result == null) return;
			columnsToCopy = result.columns;
			includeHeaders = result.includeHeaders;
			selectedOnly = result.selectedOnly;
		}

		try
		{
			StringWriter out = null;
			int count = this.data.getRowCount();
			int[] rows = null;
			if (selectedOnly)
			{
				rows = this.client.getSelectedRows();
				count = rows.length;
			}

			// Do not use StringUtil.LINE_TERMINATOR for the line terminator
			// because for some reason this creates additional empty lines
			// under Windows
			DataPrinter printer = new DataPrinter(this.data, "\t", "\n", columnsToCopy, includeHeaders);
			printer.setColumnMapping(getColumnOrder());
			
			out = new StringWriter(count * 250);
			printer.writeDataString(out, rows);

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
		this.copyAsSql(false, selectedOnly, showSelectColumns, false);
	}

	public void copyAsSqlDeleteInsert(boolean selectedOnly, boolean showSelectColumns)
	{
		this.copyAsSql(false, selectedOnly, showSelectColumns, true);
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
		copyAsSql(true, selectedOnly, showSelectColumns, false);
	}


	/**
	 * 	Copy the data of the client table into the clipboard using SQL statements
	 */
	public void copyAsSql(final boolean useUpdate, final boolean selectedOnly, final boolean showSelectColumns, final boolean includeDelete)
	{
		if (this.data == null)
		{
			WbSwingUtilities.showErrorMessage(client, "No DataStore available!");
			LogMgr.logError("ClipBoardCopier.copyAsSql()", "Cannot copy without a DataStore!", null);
			return;
		}

		// For some reason the statusbar will not be updated if
		// this is run in the AWT thread, so we have to
		// create a new thread to run the actual copy
		WbThread t = new WbThread("CopyThread")
		{
			public void run()
			{
				_copyAsSql(useUpdate, selectedOnly, showSelectColumns, includeDelete);
			}
		};
		t.start();
	}

	protected void _copyAsSql(final boolean useUpdate, boolean selectedOnly, final boolean showSelectColumns, final boolean includeDelete)
	{
		if (this.data.getRowCount() <= 0) return;

		if (useUpdate || includeDelete)
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
				String msg = ResourceMgr.getString("ErrCopyNotAvailable");
				WbSwingUtilities.showErrorMessage(client, msg);
				return;
			}
		}

		TableIdentifier updateTable = data.getUpdateTable();
		if (updateTable == null)
		{
			updateTable = client.selectUpdateTable();
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

		if (showSelectColumns)
		{
      ColumnSelectionResult result = this.selectColumns(false, selectedOnly, false, client.getSelectedRowCount() > 0);
			if (result == null) return;
			columnsToInclude = result.columns;
      selectedOnly = result.selectedOnly;
		}

		try
		{
			WbSwingUtilities.showWaitCursorOnWindow(this.client);
			int[] rows = null;
			if (selectedOnly) rows = this.client.getSelectedRows();

			SqlRowDataConverter converter = new SqlRowDataConverter(data.getOriginalConnection());
			converter.setIncludeTableOwner(Settings.getInstance().getIncludeOwnerInSqlExport());
			converter.setResultInfo(data.getResultInfo());
			converter.setDateLiteralType(Settings.getInstance().getDefaultCopyDateLiteralType());
			if (useUpdate)
			{
				converter.setCreateUpdate();
			}
			else if (includeDelete)
			{
				converter.setCreateInsertDelete();
			}
			else
			{
				converter.setCreateInsert();
				if (data.getResultInfo().getUpdateTable() == null)
				{
					String tbl = data.getInsertTable();
					TableIdentifier table = new TableIdentifier(tbl);
					converter.setAlternateUpdateTable(table);
				}
			}
			converter.setColumnsToExport(columnsToInclude);
			converter.setBlobMode(BlobMode.DbmsLiteral);

			int count = 0;
			if (rows != null) count = rows.length;
			else count = data.getRowCount();

			StringBuilder result = new StringBuilder(count * 100);
			RowData rowdata = null;

			for (int row = 0; row < count; row ++)
			{
				if (rows == null) rowdata = this.data.getRow(row);
				else rowdata = data.getRow(rows[row]);

				StrBuffer sql = converter.convertRowData(rowdata, row);
				sql.appendTo(result);
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
				WbSwingUtilities.showErrorMessage(client, msg);
			}
			LogMgr.logError(this, "Error when copying as SQL", e);
		}
		WbSwingUtilities.showDefaultCursorOnWindow(this.client);
	}


	/**
	 *	A general purpose method to select specific columns from the result set
	 *  this is e.g. used for copying data to the clipboard
	 *
	 */
	public ColumnSelectionResult selectColumns(boolean includeHeader, boolean selectedOnly, boolean showHeaderSelection, boolean showSelectedRowsSelection)
	{
		if (this.data == null) return null;

		ColumnSelectionResult result = new ColumnSelectionResult();
		result.includeHeaders = includeHeader;
		result.selectedOnly = selectedOnly;

		ColumnIdentifier[] originalCols = this.data.getColumns();
		ColumnSelectorPanel panel = new ColumnSelectorPanel(originalCols, includeHeader, selectedOnly, showHeaderSelection, showSelectedRowsSelection);
		panel.selectAll();
		int choice = JOptionPane.showConfirmDialog(SwingUtilities.getWindowAncestor(this.client), panel, ResourceMgr.getString("MsgSelectColumnsWindowTitle"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

		if (choice == JOptionPane.OK_OPTION)
		{
			result.columns = panel.getSelectedColumns();
			result.includeHeaders = panel.includeHeader();
			result.selectedOnly = panel.selectedOnly();
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
		for (int i=0; i < cols.length; i++)
		{
			result.add(ds.getResultInfo().getColumn(cols[i]));
		}
		return result;
	}
}

