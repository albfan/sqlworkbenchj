package workbench.gui.components;

import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import workbench.WbManager;
import workbench.exception.WbException;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.storage.DataStore;
import workbench.storage.NullValue;
import workbench.util.SqlUtil;



/** 
 * TableModel for displaying the contents of a DataStore or 
 * a ResultSet. If it's a result set, this will be cached
 * in a {@link workbench.storage.DataStore }
 */
public class DataStoreTableModel 
	extends AbstractTableModel
{
	private DataStore dataCache;
	private WbTable parentTable;
	private boolean showStatusColumn = false;
	private int statusOffset = 0;
	public static final String NOT_AVAILABLE = "(n/a)";

	// used for sorting the model
	private boolean sortAscending = true;
	private int sortColumn = -1;
	
	public DataStoreTableModel(ResultSet aResultSet) throws SQLException, WbException
	{
		this(aResultSet, null);
	}
	
	public DataStoreTableModel(DataStore aDataStore) throws NullPointerException
	{
		if (aDataStore == null) throw new NullPointerException("DataStore cannot be null");
		this.setDataStore(aDataStore);
	}
	
	public DataStoreTableModel(ResultSet aResultSet, List aColumnList) throws SQLException, WbException
	{
		DataStore ds = new DataStore(aResultSet, aColumnList);
		this.setDataStore(ds);
	}
	
	public DataStore getDataStore()
	{
		return this.dataCache;
	}
	
	public synchronized void setDataStore(DataStore newData)
	{
		this.dataCache = newData;
		this.showStatusColumn = false;
		this.statusOffset = 0;
		this.sortColumn = -1;
		this.fireTableStructureChanged();
	}
	/**
	 *	Return the contents of the field at the given position
	 *	in the result set.
	 *	@parm row - The row to get. Counting starts at zero.
	 *	@parm col - The column to get. Counting starts at zero.
	 */
	public Object getValueAt(int row, int col)
	{
		if (this.showStatusColumn && col == 0)
		{
			return this.dataCache.getRowStatus(row);
		}
		
		try
		{
			Object result;
			result = this.dataCache.getValue(row, col - this.statusOffset);
			return result;
		}
		catch (Exception e)
		{
			StringBuffer msg = new StringBuffer("Error @");
			msg.append(row);
			msg.append('/');
			msg.append(col);
			msg.append(" - ");
			if (e.getMessage() == null)
			{
				msg.append(e.getClass().getName());
			}
			else
			{
				msg.append(e.getMessage());
			}
			return msg.toString();
		}
	}	

	public int findColumn(String aColname)
	{
		int index = -1;
		try
		{
			index = this.dataCache.getColumnIndex(aColname) + this.statusOffset;
		}
		catch (SQLException e)
		{
			index = -1;
		}
		return index;
	}
	
	public void setUpdateTable(String aTable)
	{
		this.dataCache.setUpdateTable(aTable);
	}
	
	public void setShowStatusColumn(boolean aFlag)
	{
		if (aFlag == this.showStatusColumn) return;
		this.showStatusColumn = aFlag;
		if (this.showStatusColumn)
			this.statusOffset = 1;
		else 
			this.statusOffset = 0;
		this.fireTableStructureChanged();
	}
	
	public boolean getShowStatusColumn() { return this.showStatusColumn; }
	
	public boolean isUpdateable() { return this.dataCache.isUpdateable(); }
	
	public void setValueAt(Object aValue, int row, int column)
	{
		if (this.showStatusColumn && column == 0) return;
		
		try
		{
			if (this.isUpdateable())
			{
				if (aValue == null || aValue.toString().length() == 0) 
				{
					this.dataCache.setNull(row, column - this.statusOffset);
				}
				else 
				{
					try
					{
						Object realValue = this.dataCache.convertCellValue(aValue, column - this.statusOffset);
						this.dataCache.setValue(row, column - this.statusOffset, realValue);
					}
					catch (Exception ce)
					{
						LogMgr.logError(this, "Error converting input >" + aValue + "< to column type (" + this.getColumnType(column) + ") ", ce);
						Toolkit.getDefaultToolkit().beep();
						String msg = ResourceMgr.getString("MsgConvertError");
						msg = msg + "\r\n" + ce.getLocalizedMessage();
						WbManager.getInstance().showErrorMessage(msg);
						return;
					}
				}
				fireTableDataChanged();
			}
		}
		catch (Exception e)
		{
			System.err.println("Error in setValueAt");
			e.printStackTrace(System.err);
		}
	}
	
	/**
	 *	Return the number of columns in the model.
	 */
	public int getColumnCount()
	{
		return this.dataCache.getColumnCount() + this.statusOffset;
	}

	public int getColumnWidth(int aColumn)
	{
		if (this.showStatusColumn && aColumn == 0) return 5;
		try
		{
			return this.dataCache.getColumnDisplaySize(aColumn - this.statusOffset);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return 100;
		}
	}

	public String getColumnTypeName(int aColumn)
	{
		if (aColumn == 0) return "";
		return SqlUtil.getTypeName(this.getColumnType(aColumn));
	}
	
	public int getColumnType(int aColumn)
	{
		if (this.dataCache == null) return Types.NULL;
		if (this.showStatusColumn && aColumn == 0) return 0;
		
		try
		{
			return this.dataCache.getColumnType(aColumn - this.statusOffset);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return Types.VARCHAR;
		}
	}		

	/**
	 *	Number of rows in the result set
	 */
	public int getRowCount() 
	{ 
		if (this.dataCache == null) return 0;
		return this.dataCache.getRowCount();
	}

	public Class getColumnClass(int aColumn)
	{
		if (this.showStatusColumn && aColumn == 0) return Integer.class;
		return this.dataCache.getColumnClass(aColumn - this.statusOffset);
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
	public void deleteRow(int aRow)
	{
		this.dataCache.deleteRow(aRow);
		this.fireTableRowsDeleted(aRow, aRow);
	}
	
	public void dispose()
	{
		this.dataCache = null;
	}
	
	public void finalize()
	{
		this.dispose();
	}
	
	/** Return the name of the column as defined by the ResultSetData.
	 */	
	public String getColumnName(int aColumn)
	{
		if (this.showStatusColumn && aColumn == 0) return " ";
		
		try
		{
			String name = this.dataCache.getColumnName(aColumn - this.statusOffset);
			if (name == null || name.length() == 0)
			{
				name = "Col" + aColumn;
			}
			return name;
		}
		catch (Exception e)
		{
			return NOT_AVAILABLE;
		}
	}
	
	public StringBuffer getRowData(int aRow)
	{
		return this.dataCache.getRowDataAsString(aRow);
	}
	
	public boolean isCellEditable(int row, int column)
	{
		if (this.showStatusColumn)
			return (column != 0);
		else
			return true;
	}

	/**    Return true if the data is sorted in ascending order.
	 *
	 * @return True if sorted in ascending order
	 */
	public boolean isSortAscending()
	{
		return sortAscending;
	}
	
	public int getSortColumn() { return this.sortColumn; }
	
	public synchronized void sortByColumn(int column)
	{
		boolean ascending = true;
		if (this.sortColumn == column)
			ascending = !this.sortAscending;
		sortByColumn(column, ascending);
	}
	
	public void sortByColumn(int aColumn, boolean ascending)
	{
		this.sortAscending = ascending;
		this.sortColumn = aColumn;
		try
		{
			this.dataCache.sortByColumn(aColumn, ascending);
		}
		catch (Throwable th)
		{
			th.printStackTrace();
		}
		fireTableChanged(new TableModelEvent(this));
	}
	
	public void sortInBackground(WbTable table, int aColumn)
	{
		if (aColumn < 0) 
		{
			System.err.println("Wrong column index specified!");
			return;
		}
		
		boolean ascending = true;
		if (this.sortColumn == aColumn)
			ascending = !this.sortAscending;
		sortInBackground(table, aColumn, ascending);
	}
	
	public void sortInBackground(final WbTable table, final int aColumn, final boolean ascending)
	{
		final DataStoreTableModel sorter = this;
		new Thread(new Runnable()
		{
			public void run()
			{
				table.getParent().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				table.getTableHeader().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				table.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				sorter.sortByColumn(aColumn, ascending);
				table.getTableHeader().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				table.getParent().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				table.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				// repaint the header so that the icon is displayed...
				table.getTableHeader().repaint(); 
			}
		}).start();
	}


}
