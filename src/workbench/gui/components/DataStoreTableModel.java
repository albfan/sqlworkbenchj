/*
 * DataStoreTableModel.java
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
package workbench.gui.components;

import java.awt.Toolkit;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.db.ConnectionProfile;
import workbench.db.WbConnection;

import workbench.gui.WbSwingUtilities;

import workbench.storage.DataStore;
import workbench.storage.InputValidator;
import workbench.storage.NamedSortDefinition;
import workbench.storage.ResultInfo;
import workbench.storage.SortDefinition;
import workbench.storage.filter.FilterExpression;

import workbench.util.ConverterException;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbThread;

/**
 * TableModel for displaying the contents of a {@link workbench.storage.DataStore }
 *
 * @author Thomas Kellerer
 */
public class DataStoreTableModel
	extends AbstractTableModel
{
	private DataStore dataCache;
	private boolean showStatusColumn;
	private int columnStartIndex;

	private List<Integer> noneditableColumns = new ArrayList<>();

	private SortDefinition sortDefinition = new SortDefinition();

	private boolean allowEditing = true;

	private boolean showConverterError = true;
	private final Object model_change_lock = new Object();
	private Set<Integer> readOnlyColumns;
	private InputValidator inputValidator;

	public DataStoreTableModel(DataStore sourceData)
		throws IllegalArgumentException
	{
		super();
		if (sourceData == null) throw new IllegalArgumentException("DataStore cannot be null");
		this.setDataStore(sourceData);
	}

	public DataStore getDataStore()
	{
		return this.dataCache;
	}

	public final void setDataStore(DataStore newData)
	{
		this.dispose();
		this.dataCache = newData;
		this.showStatusColumn = false;
		this.columnStartIndex = 0;
		if (newData.getLastSort() != null)
		{
			this.sortDefinition = newData.getLastSort();
		}
		this.fireTableStructureChanged();
	}

	public void setShowConverterError(boolean flag)
	{
		this.showConverterError = flag;
	}

	public void setSortIgnoreCase(boolean flag)
	{
		sortDefinition.setIgnoreCase(true);
	}

	public boolean isColumnModified(int row, int column)
	{
		if (this.showStatusColumn && column == 0)
		{
			return false;
		}
		try
		{
			return this.dataCache.isColumnModified(row, column - this.columnStartIndex);
		}
		catch (Exception e)
		{
			LogMgr.logError("DataStoreTableModel.isColumnModified()", "Error checking value at: " + row + "/" + column, e);
			return false;
		}
	}

	/**
	 *	Return the contents of the field at the given position
	 *	in the result set.
	 *	@param row - The row to get. Counting starts at zero.
	 *	@param col - The column to get. Counting starts at zero.
	 */
	@Override
	public Object getValueAt(int row, int col)
	{
		if (this.showStatusColumn && col == 0)
		{
			return Integer.valueOf(this.dataCache.getRowStatus(row));
		}

		try
		{
			Object result;
			result = this.dataCache.getValue(row, col - this.columnStartIndex);
			return result;
		}
		catch (Exception e)
		{
			LogMgr.logError("DataStoreTableModel.getValue()", "Error retrieving value at: " + row + "/" + col, e);
			return "Error";
		}
	}

	@Override
	public int findColumn(String aColname)
	{
		int index = this.dataCache.getColumnIndex(aColname);
		if (index == -1) return -1;
		return index + this.columnStartIndex;
	}

	public int getRealColumnStart()
	{
		return columnStartIndex;
	}

	/**
	 *	Shows or hides the status column.
	 *	The status column will display an indicator if the row has
	 *  been modified or was inserted
	 */
	public void setShowStatusColumn(boolean aFlag)
	{
		if (aFlag == this.showStatusColumn) return;
		synchronized(model_change_lock)
		{
			if (aFlag)
			{
				this.columnStartIndex = 1;
			}
			else
			{
				this.columnStartIndex = 0;
			}
			this.showStatusColumn = aFlag;
		}
		this.fireTableStructureChanged();
	}

	public void setReadOnlyColumns(Set<Integer> cols)
	{
		this.readOnlyColumns = cols;
	}

	public boolean getShowStatusColumn()
	{
		return this.showStatusColumn;
	}

	public boolean isUpdateable()
	{
		if (this.dataCache == null) return false;
		return this.dataCache.isUpdateable();
	}

	private boolean isNull(Object value, int column)
	{
		if (value == null) return true;
		String s = value.toString();
		int type = this.dataCache.getColumnType(column);
		if (SqlUtil.isCharacterType(type))
		{
			WbConnection con = this.dataCache.getOriginalConnection();
			ConnectionProfile profile = (con != null ? con.getProfile() : null);
			if (profile == null || profile.getEmptyStringIsNull())
			{
				return (s.length() == 0);
			}
			return false;
		}
		return StringUtil.isEmptyString(s);
	}

	public void setValidator(InputValidator validator)
	{
		inputValidator = validator;
	}

	@Override
	public void setValueAt(Object aValue, int row, int column)
	{
		// Updates to the status column shouldn't happen anyway ....
		if (this.showStatusColumn && column == 0) return;

		int realColumn = column - this.columnStartIndex;

		if (inputValidator != null)
		{
			if (!inputValidator.isValid(aValue, row, column, this)) return;
		}

		if (this.readOnlyColumns != null && readOnlyColumns.contains(realColumn)) return;

		if (isNull(aValue, realColumn))
		{
			this.dataCache.setValue(row, realColumn, null);
		}
		else
		{
			try
			{
				this.dataCache.setInputValue(row, realColumn, aValue);
			}
			catch (ConverterException ce)
			{
				int type = this.getColumnType(column);
				LogMgr.logError(this, "Error converting input >" + aValue + "< to column type " + SqlUtil.getTypeName(type) + " (" + type + ")", ce);
				Toolkit.getDefaultToolkit().beep();
				String msg = ResourceMgr.getString("MsgConvertError");
				msg = msg + "\r\n" + ce.getLocalizedMessage();
				if (showConverterError)
				{
					WbSwingUtilities.showErrorMessage(msg);
					return;
				}
				throw new IllegalArgumentException(msg);
			}
		}
		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				fireTableDataChanged();
			}
		});
	}

	/**
	 *	Return the number of columns in the model.
	 *	This will return the number of columns of the underlying DataStore (plus one
	 *  if the status column is enabled)
	 */
	@Override
	public int getColumnCount()
	{
		return this.dataCache.getColumnCount() + this.columnStartIndex;
	}

	/**
	 *	Returns the current width of the given column.
	 *	It returns the value of {@link workbench.storage.DataStore#getColumnDisplaySize(int)}
	 *  for every column which is not the status column.
	 *
	 * @param aColumn the column index
	 *
	 * @return the width of the column as defined by the DataStore or 0
	 * @see workbench.storage.DataStore#getColumnDisplaySize(int)
	 * @see #findColumn(String)
	 */
	public int getColumnWidth(int aColumn)
	{
		if (this.showStatusColumn && aColumn == 0) return 5;
		if (this.dataCache == null) return 0;
		try
		{
			return this.dataCache.getColumnSize(aColumn);
		}
		catch (Exception e)
		{
			LogMgr.logWarning("DataStoreTableModel.getColumnWidth()", "Error retrieving display size for column " + aColumn, e);
			return 100;
		}
	}

	/**
	 *	Returns the name of the datatype (according to java.sql.Types) of the
	 *  given column.
	 */
	public String getColumnTypeName(int col)
	{
		if (this.dataCache == null) return "";
		if (this.showStatusColumn && col == 0) return "";
		try
		{
			ResultInfo info = this.dataCache.getResultInfo();
			return info.getColumn(col - this.columnStartIndex).getColumnTypeName();
		}
		catch (Exception e)
		{
			return SqlUtil.getTypeName(this.getColumnType(col));
		}

	}

	public String getDbmsType(int col)
	{
		if (this.dataCache == null) return null;
		if (this.showStatusColumn && col == 0) return null;
		try
		{
			ResultInfo info = this.dataCache.getResultInfo();
			return info.getDbmsTypeName(col - this.columnStartIndex);
		}
		catch (Exception e)
		{
			return null;
		}

	}

	/**
	 *	Returns the type (java.sql.Types) of the given column.
	 */
	public int getColumnType(int aColumn)
	{
		if (this.dataCache == null) return Types.NULL;
		if (this.showStatusColumn && aColumn == 0) return 0;

		try
		{
			return this.dataCache.getColumnType(aColumn - this.columnStartIndex);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return Types.VARCHAR;
		}
	}

	public int getFilteredCount()
	{
		if (this.dataCache == null) return 0;
		return this.dataCache.getFilteredCount();
	}
	/**
	 *	Number of rows in the result set
	 */
	@Override
	public int getRowCount()
	{
		if (this.dataCache == null) return 0;
		return this.dataCache.getRowCount();
	}

	@Override
	public Class getColumnClass(int aColumn)
	{
		if (this.dataCache == null) return null;
		if (aColumn == 0 && this.showStatusColumn) return Integer.class;
		return this.dataCache.getColumnClass(aColumn - columnStartIndex);
	}

	public int insertRow(int afterRow)
	{
		int row = this.dataCache.insertRowAfter(afterRow);
		this.fireTableRowsInserted(row, row);
		return row;
	}

	public int addRow()
	{
		int row = this.dataCache.addRow();
		this.fireTableRowsInserted(row, row);
		return row;
	}

	public void deleteRow(int aRow, boolean withDependencies)
		throws SQLException
	{
		if (withDependencies)
		{
			this.dataCache.deleteRowWithDependencies(aRow);
		}
		else
		{
			this.dataCache.deleteRow(aRow);
		}
		this.fireTableRowsDeleted(aRow, aRow);
	}

	public int duplicateRow(int aRow)
	{
		int row = this.dataCache.duplicateRow(aRow);
		this.fireTableRowsInserted(row, row);
		return row;
	}

	public void fileImported()
	{
		int row = this.getRowCount();
		this.fireTableRowsInserted(0, row - 1);
	}

	/**
	 * Removes all rows from this table model.
	 */
	public void removeAll()
	{
		if (this.dataCache != null)
		{
			this.dataCache.reset();
			this.fireTableDataChanged();
		}
	}

	/**
	 *	Clears the EventListenerList and empties the DataStore
	 */
	public void dispose()
	{
		this.listenerList = new EventListenerList();
		if (this.dataCache != null)
		{
			this.dataCache.reset();
			this.dataCache = null;
		}
	}

	/** Return the name of the column as defined by the ResultSetData.
	 */
	@Override
	public String getColumnName(int aColumn)
	{
		if (this.showStatusColumn && aColumn == 0) return " ";

		try
		{
			String name = this.dataCache.getColumnDisplayName(aColumn - this.columnStartIndex);
			return name;
		}
		catch (Exception e)
		{
			return "(n/a)";
		}
	}

	@Override
	public boolean isCellEditable(int row, int column)
	{
		if (!this.noneditableColumns.isEmpty())
		{
			return (this.allowEditing && !noneditableColumns.contains(column));
		}
		else if (this.columnStartIndex > 0 && column < this.columnStartIndex)
		{
			return false;
		}
		else
		{
			// For BLOB columns the BlobHandler and BlobInfoDialog will
			// check if this model allows editing.
			// otherwise the blob dialog cannot be opened. Apparently
			// JTable does not even route the clicked event to the button
			// in the BlobColumnRenderer if the column is not editable.
			return this.allowEditing || SqlUtil.isBlobType(getColumnType(column));
		}
	}

	/**
	 * Clear the locked column. After a call to clearLockedColumn()
	 * all columns (except the status column) are editable
	 * when the table is in edit mode.
	 * @see #setLockedColumn(int)
	 */
	public void clearLockedColumn()
	{
		this.noneditableColumns.clear();
	}

	/**
	 * Define a column that may not be edited even if the
	 * table is in "Edit mode"
	 * @param column the column to be set as non-editable
	 * @see #clearLockedColumn()
	 */
	public void setLockedColumn(int column)
	{
		clearLockedColumn();
		this.noneditableColumns.add(column);
	}

	public void setNonEditableColums(int ... columns)
	{
		clearLockedColumn();
		if (columns == null) return;
		for (int col : columns)
		{
			if (col > -1) this.noneditableColumns.add(col);
		}
	}

	public void setAllowEditing(boolean aFlag)
	{
		this.allowEditing = aFlag;
	}

	public boolean getAllowEditing()
	{
		return allowEditing;
	}

	/**
	 * Clears the filter that is currently defined on the underlying
	 * DataStore. A tableDataChanged Event will be fired after this
	 */
	public void resetFilter()
	{
		if (isSortInProgress()) return;

		dataCache.clearFilter();
		// sort() will already fire a tableDataChanged()
		// if a sort column was defined
		if (!sort())
		{
			fireTableDataChanged();
		}
	}

	/**
	 * Applys the given filter to the underlying
	 * DataStore. A tableDataChanged Event will be fired after this
	 */
	public void applyFilter(FilterExpression filter)
	{
		if (isSortInProgress()) return;

		dataCache.applyFilter(filter);
		// sort() will already fire a tableDataChanged()
		// if a sort column was defined
		if (!sort())
		{
			this.fireTableDataChanged();
		}
	}

	private void setSortInProgress(final boolean flag)
	{
		this.sortingInProgress = flag;
	}

	private boolean isSortInProgress()
	{
		return this.sortingInProgress;
	}


	/**
	 * Return true if the data is sorted in ascending order.
	 * @return True if sorted in ascending order
	 */
	public boolean isSortAscending(int col)
	{
		return this.sortDefinition.isSortAscending(col - columnStartIndex);
	}


	public boolean isPrimarySortColumn(int col)
	{
		return this.sortDefinition.isPrimarySortColumn(col - columnStartIndex);
	}

	/**
	 * Check if the table is sorted by a column.
	 *
	 * @return true if the given column is a sort column
	 * @see #isSortAscending(int)
	 */
	public boolean isSortColumn(int col)
	{
		return this.sortDefinition.isSortColumn(col - columnStartIndex);
	}

	/**
	 * Returns a snapshot of the current sort columns identified
	 * by their names instead of their column index (as done by SortDefinition)
	 *
	 * @return the current sort definition with named columns or null if no sort is defined
	 * @see #getSortColumns()
	 */
	public NamedSortDefinition getSortDefinition()
	{
		if (this.sortDefinition == null) return null;
		if (this.sortDefinition.isEmpty()) return null;
		if (!this.sortDefinition.isValid()) return null;

		NamedSortDefinition newSort = new NamedSortDefinition(this.dataCache, this.sortDefinition);
		if (newSort.getColumnCount() > 0)
		{
			return newSort;
		}
		return null;
	}

	/**
	 * Returns a snapshot of the current sort columns identified by their position.
	 *
	 * @return the current sort definition with column indexes or null if no sort is defined
	 * @see #getSortDefinition()
	 */
	public SortDefinition getSortColumns()
	{
		if (this.sortDefinition == null) return null;
		return this.sortDefinition.createCopy();
	}

	public void setSortDefinition(SortDefinition newSort)
	{
		if (!newSort.equals(this.sortDefinition))
		{
			sortDefinition = newSort;
			applySortColumns();
		}
	}

	public void setSortDefinition(NamedSortDefinition definition)
	{
		if (definition == null) return;
		SortDefinition newSort = definition.getSortDefinition(dataCache);
		setSortDefinition(newSort);
	}

	/**
	 *	Re-apply the last sort order defined.
	 *  If no sort order was defined this method does nothing
	 */
	public boolean sort()
	{
		if (this.sortDefinition == null) return false;
		applySortColumns();
		return true;
	}

	public void removeSortColumn(int column)
	{
		if (sortDefinition == null) return;
		boolean isPrimaryColumn = sortDefinition.isPrimarySortColumn(column);
		sortDefinition.removeSortColumn(column);

		// if the primary (== first) column was removed
		// we have to re-apply the sort definition
		if (isPrimaryColumn)
		{
			applySortColumns();
		}
	}

	/**
	 * Sort the data by the given column in the defined order
	 */
	public void sortByColumn(int column, boolean ascending, boolean addSortColumn)
	{
		if (addSortColumn)
		{
			sortDefinition.addSortColumn(column - columnStartIndex, ascending);
		}
		else
		{
			sortDefinition.setSortColumn(column - columnStartIndex, ascending);
		}
		applySortColumns();
	}

	private void applySortColumns()
	{
		if (sortDefinition == null) return;
		if (dataCache == null) return;

		synchronized (model_change_lock)
		{
			try
			{
				setSortInProgress(true);
				dataCache.sort(this.sortDefinition);
			}
			catch (Throwable th)
			{
				LogMgr.logError("DataStoreTableModel.sortByColumn()", "Error when sorting data", th);
			}
			finally
			{
				setSortInProgress(false);
			}
		}
		final TableModelEvent event = new TableModelEvent(this);
		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				fireTableChanged(event);
			}
		});
	}

	private boolean sortingInProgress = false;

	public void startBackgroundSort(WbTable table, int aColumn, boolean addSortColumn, boolean descSortIfFirst)
	{
		if (sortingInProgress) return;

		if (aColumn < 0 && aColumn >= this.getColumnCount())
		{
			LogMgr.logWarning("DataStoreTableModel", "Wrong column index for sorting specified!");
			return;
		}
		int sortCols = this.sortDefinition.getColumnCount();
		boolean ascending = true;
		if (descSortIfFirst && (sortCols == 0 || !addSortColumn))
		{
			ascending = false;
		}
		else if (sortCols <= 1)
		{
			// toggle the ascending status only if there is a single sort column
			ascending = !this.isSortAscending(aColumn);
		}
		sortInBackground(table, aColumn, ascending, addSortColumn);
	}

	/**
	 *	Start a new thread to sort the data.
	 *	Any call to this method while the thread is running, will be ignored
	 */
	public void sortInBackground(final WbTable table, final int aColumn, final boolean ascending, final boolean addSortColumn)
	{
		if (isSortInProgress()) return;

		Thread t = new WbThread("Data Sort")
		{
			@Override
			public void run()
			{
				try
				{
					table.sortingStarted();
					sortByColumn(aColumn, ascending, addSortColumn);
				}
				finally
				{
					table.sortingFinished();
				}
			}
		};
		t.start();
	}

}
