/*
 * ClipBoardCopier.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
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
import workbench.WbManager;
import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.db.exporter.SqlRowDataConverter;
import workbench.gui.WbSwingUtilities;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.storage.DataPrinter;
import workbench.storage.DataStore;
import workbench.storage.RowData;
import workbench.util.StrBuffer;
import workbench.util.WbThread;

/**
 * A class to copy the data of a {@link workbench.gui.components.WbTable} to 
 * the clipboard. Either as tab-separated text or SQL Statements.
 *
 * @author support@sql-workbench.net
 */
public class ClipBoardCopier
{
	private WbTable client;
	
	public ClipBoardCopier(WbTable t)
	{
		this.client = t;
	}

	/**
	 *	Copy data from the table as tab-delimited into the clipboard
	 *	@param includeHeaders if true, then a header line with the column names is copied as well
	 *  @param selectedOnly if true, then only selected rows are copied, else all rows
	 *  @param showSelectColumns if true, a dialog will be presented to the user to select the columns to be included
	 */
	public void copyDataToClipboard(boolean includeHeaders, boolean selectedOnly, final boolean showSelectColumns)
	{
		if (this.client.getRowCount() <= 0) return;
		
		List columnsToCopy = null;
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
			DataStore ds = this.client.getDataStore();
			StringWriter out = null;
			int count = this.client.getRowCount();
			int[] rows = null;
			if (selectedOnly)
			{
				rows = this.client.getSelectedRows();
				count = rows.length;
			}
			
			DataPrinter printer = new DataPrinter(ds, "\t", "\n", columnsToCopy, includeHeaders);
			out = new StringWriter(count * 250);
			// Do not use StringUtil.LINE_TERMINATOR for the line terminator
			// because for some reason this creates additional empty lines
			// under Windows
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
			LogMgr.logError(this, "Could not copy text data to clipboard", ex);
		}
		WbSwingUtilities.showDefaultCursorOnWindow(this.client);
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
	
	protected void _copyAsSql(boolean useUpdate, boolean selectedOnly, boolean showSelectColumns, boolean includeDelete)
	{
		if (this.client.getRowCount() <= 0) return;
		
		DataStore ds = this.client.getDataStore();
		if (ds == null) return;

		if (useUpdate || includeDelete)
		{
			boolean pkOK = this.client.checkPkColumns(true);
			
			// checkPkColumns will return false, if the user cancelled the prompting
			if (!pkOK) return;
		}

		List columnsToInclude = null;
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
			int rows[] = null;
			if (selectedOnly) rows = this.client.getSelectedRows();
			
			SqlRowDataConverter converter = new SqlRowDataConverter(ds.getOriginalConnection());
			converter.setIncludeTableOwner(Settings.getInstance().getIncludeOwnerInSqlExport());
			converter.setResultInfo(ds.getResultInfo());
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
				if (ds.getResultInfo().getUpdateTable() == null)
				{
					String tbl = ds.getInsertTable();
					TableIdentifier table = new TableIdentifier(tbl);
					converter.setAlternateUpdateTable(table);
				}
			}
			converter.setColumnsToExport(columnsToInclude);
			converter.setBlobTypeDbmsLiteral();
			
			int count = 0;
			if (rows != null) count = rows.length;
			else count = ds.getRowCount();
			
			StringBuffer data = new StringBuffer(count * 100);
			RowData rowdata = null;
			
			for (int row = 0; row < count; row ++)
			{
				if (rows == null) rowdata = ds.getRow(row);
				else rowdata = ds.getRow(rows[row]);
				
				StrBuffer sql = converter.convertRowData(rowdata, row);
				sql.appendTo(data);
			}
			
			Clipboard clp = Toolkit.getDefaultToolkit().getSystemClipboard();
			StringSelection sel = new StringSelection(data.toString());
			clp.setContents(sel, sel);
		}
		catch (Throwable e)
		{
			if (e instanceof OutOfMemoryError)
			{
				WbManager.getInstance().showOutOfMemoryError();
			}
			LogMgr.logError(this, "Error when copying SQL inserts", e);
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
		DataStore ds = this.client.getDataStore();
		if (ds == null) return null;

    ColumnSelectionResult result = new ColumnSelectionResult();
    result.includeHeaders = includeHeader;
    result.selectedOnly = selectedOnly;

		ColumnIdentifier[] originalCols = ds.getColumns();
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

	private List getColumnsFromSelection()
	{
		int[] cols = this.client.getSelectedColumns();
		DataStore ds = this.client.getDataStore();
		if (ds == null) return Collections.EMPTY_LIST;
		List result = new ArrayList(cols.length);
		for (int i=0; i < cols.length; i++)
		{
			result.add(ds.getResultInfo().getColumn(cols[i]));
		}
		return result;
	}
}

class ColumnSelectionResult
{
    public boolean includeHeaders;
    public boolean selectedOnly;
    public List columns;
}
