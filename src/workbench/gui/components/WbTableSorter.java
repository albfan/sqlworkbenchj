package workbench.gui.components;

import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Vector;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import workbench.storage.NullValue;


public class WbTableSorter 
	extends AbstractTableModel 
	implements TableModelListener
{
	protected TableModel model;
	RowIndex[]   indexes;
	boolean ascending = true;
	int     column = -1;
	int     compares;
	ColumnComparator comparator;
	Class currentSortColumnClass;
	
	public WbTableSorter(TableModel model)
	{
		setModel(model);
		this.comparator = new ColumnComparator();
	}
	
	public TableModel getModel()
	{
		return model;
	}
	
	public void setModel(TableModel model)
	{
		this.model = model;
		model.addTableModelListener(this);
		reallocateIndexes();
	}
	
	/**    Get the value at the given row and column.
	 *
	 * @param aRow The row
	 * @param aColumn The column
	 * @return The value
	 */
	public Object getValueAt(int aRow, int aColumn)
	{
		checkModel();
		return model.getValueAt(indexes[aRow].rowIndex, aColumn);
	}
	
	/**    Set the value at the given row and column.
	 *
	 * @param aValue The value
	 * @param aRow The row
	 * @param aColumn The column
	 */
	public void setValueAt(Object aValue, int aRow, int aColumn)
	{
		checkModel();
		model.setValueAt(aValue, indexes[aRow].rowIndex, aColumn);
	}
	
	/**    Get the row count.
	 *
	 * @return The row count
	 */
	public int getRowCount()
	{
		return (model == null) ? 0 : model.getRowCount();
	}
	
	/**    Get the column count.
	 *
	 * @return The column count
	 */
	public int getColumnCount()
	{
		return (model == null) ? 0 : model.getColumnCount();
	}
	
	/**    Get the column name for the given index.
	 *
	 * @param aColumn The column
	 * @return The column name
	 */
	public String getColumnName(int aColumn)
	{
		return model.getColumnName(aColumn);
	}
	
	/**    Get the column class for the given column index.
	 *
	 * @param aColumn The column
	 * @return The column class
	 */
	public Class getColumnClass(int aColumn)
	{
		return model.getColumnClass(aColumn);
	}
	
	/**    Return true if the cell at the given row and column is editable.
	 *
	 * @param row The row
	 * @param column The column
	 * @return True if the cell is editable
	 */
	public boolean isCellEditable(int row, int column)
	{
		return model.isCellEditable(row, column);
	}
	
	/**    Fire an event signaling that the table has changed.
	 *
	 * @param e The TableModelEvent
	 */
	public void tableChanged(TableModelEvent e)
	{
		reallocateIndexes();
		if (column >= 0)
		{
			sortByColumn(column, ascending);
		}
		fireTableChanged(e);
	}
	
	public int compareRowsByColumn(int row1, int row2, int column)
	{
		TableModel data = model;
		Object     o1 = data.getValueAt(row1, column);
		Object     o2 = data.getValueAt(row2, column);
		// If both values are null, return 0.
		if (o1 == null && o2 == null)
		{
			return 0;
		}
		else if (o1 == null)
		{// Define null less than everything.
			return -1;
		}
		else if (o2 == null)
		{
			return 1;
		}
		
		try
		{
			int result = ((Comparable)o1).compareTo(o2);
			return result;
		}
		catch (Exception e)
		{
		}

		if (o1 instanceof NullValue && o2 instanceof NullValue) return 0;
		if (o1 instanceof NullValue) return -1;
		if (o2 instanceof NullValue) return 2;

		if (this.currentSortColumnClass.getSuperclass() == java.lang.Number.class)
		{
			Number n1 = (Number)data.getValueAt(row1, column);
			double d1 = n1.doubleValue();
			Number n2 = (Number)data.getValueAt(row2, column);
			double d2 = n2.doubleValue();
			if (d1 < d2)
			{
				return -1;
			}
			else if (d1 > d2)
			{
				return 1;
			}
			else
			{
				return 0;
			}
		}
		else if (this.currentSortColumnClass == Date.class)
		{
			Date d1 = (Date)data.getValueAt(row1, column);
			long n1 = d1.getTime();
			Date d2 = (Date)data.getValueAt(row2, column);
			long n2 = d2.getTime();
			if (n1 < n2)
			{
				return -1;
			}
			else if (n1 > n2)
			{
				return 1;
			}
			else
			{
				return 0;
			}
		}
		else if (this.currentSortColumnClass == String.class)
		{
			String s1 = (String)data.getValueAt(row1, column);
			String s2 = (String)data.getValueAt(row2, column);
			int    result = s1.compareTo(s2);
			if (result < 0)
			{
				return -1;
			}
			else if (result > 0)
			{
				return 1;
			}
			else
			{
				return 0;
			}
		}
		else if (this.currentSortColumnClass == Boolean.class)
		{
			Boolean bool1 = (Boolean)data.getValueAt(row1, column);
			boolean b1 = bool1.booleanValue();
			Boolean bool2 = (Boolean)data.getValueAt(row2, column);
			boolean b2 = bool2.booleanValue();
			if (b1 == b2)
			{
				return 0;
			}
			else if (b1)
			{// Define false < true
				return 1;
			}
			else
			{
				return -1;
			}
		}
		else
		{
			Object v1 = data.getValueAt(row1, column);
			Comparable comp = null;
			if (v1 instanceof Comparable)
			{
				comp = (Comparable)v1;
			}
			else
			{
				comp = v1.toString();
			}
			int result = comp.compareTo(o2);
			if (result < 0)
			{
				return -1;
			}
			else if (result > 0)
			{
				return 1;
			}
			else
			{
				return 0;
			}
		}
	}
	
	/**    Compare two rows.  All sorting columns will be sorted.
	 *
	 * @param row1 Row 1
	 * @param row2 Row 2
	 * @return 1, 0, or -1
	 */
	public int compare(int row1, int row2)
	{
		int result = compareRowsByColumn(row1, row2, this.column);
		if (result != 0)
		{
			return ascending ? result : -result;
		}
		return 0;
	}
	
	/**    Reallocate the array which holds sorted indexes. */
	public void reallocateIndexes()
	{
		int rowCount = model.getRowCount();
		indexes = new RowIndex[rowCount];
		for (int row = 0; row < rowCount; row++)
		{
			indexes[row] = new RowIndex(row);
		}
	}
	
	/**    Check to see if the index length matches the model row count.  If
	 * not then the sorter was never informed of a change in the model.
	 */
	public void checkModel()
	{
		if (indexes.length != model.getRowCount())
		{
			// System.err.println("Sorter not informed of a change in model.");
		}
	}
	
	/**    Sort the table data.
	 *
	 * @param sender The object which invoked the sort
	 */
	public synchronized void sort(Object sender)
	{
		checkModel();
		compares = 0;
		//dumpIndexes();
		Arrays.sort(indexes, this.comparator);
		//dumpIndexes();
	}

	public void dumpIndexes()
	{
		for (int i=0; i < this.indexes.length; i++)
		{
			System.out.println("Row " + i + " --> " + indexes[i].rowIndex);
		}
	}
	/**    Sort by the given column in ascending order.
	 *
	 * @param column The column
	 */
	public synchronized void sortByColumn(int column)
	{
		sortByColumn(column, true);
	}
	
	/**    Sort by the given column and order.
	 *
	 * @param column The column
	 * @param ascending True to sort in ascending order
	 */
	public synchronized void sortByColumn(int aColumn, boolean ascending)
	{
		this.ascending = ascending;
		this.column = aColumn;
		//sortingColumns.removeAllElements();
		//sortingColumns.addElement(new Integer(column));
		this.currentSortColumnClass = model.getColumnClass(aColumn);
		this.sort(this);
		fireTableChanged(new TableModelEvent(this));
		this.currentSortColumnClass = null;
	}

	public void startSorting(final WbTable table, final int aColumn, final boolean ascending)
	{
		final WbTableSorter sorter = this;
		EventQueue.invokeLater( new Runnable()
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
		});
	}
	/**    Get the column which is being sorted.
	 *
	 * @return The sorted column
	 */
	public int getColumn()
	{
		return column;
	}
	
	/**    Get the real index of the given row.
	 *
	 * @param row The row index
	 * @return The index
	 */
	public int getRealIndex(int row)
	{
		return indexes[row].rowIndex;
	}
	
	/**    Return true if the data is sorted in ascending order.
	 *
	 * @return True if sorted in ascending order
	 */
	public boolean isAscending()
	{
		return ascending;
	}


	/**    Add a MouseListener to the given table which will cause the table
	 * to be sorted when the header is clicked.  The table will be sorted
	 * in ascending order initially.  If the table was already sorted in
	 * ascending order and the same column is clicked then the order will
	 * be reversed.
	 *
	 * @param table The JTable
	 */
	public void addMouseListenerToHeaderInTable(WbTable table)
	{
		final WbTableSorter sorter = this;
		final WbTable tableView = table;
		tableView.setColumnSelectionAllowed(false);
		MouseAdapter listMouseListener = new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				if (e.getButton() != e.BUTTON1) return;
				if (e.getClickCount() != 1) return;
				
				TableColumnModel columnModel = tableView.getColumnModel();
				final int viewColumn = columnModel.getColumnIndexAtX(e.getX());
				final int column = tableView.convertColumnIndexToModel(viewColumn);

				if (column != -1)
				{
					if (WbTableSorter.this.column == column)
					{
						ascending = !ascending;
					}
					else
					{
						ascending = true;
					}
					// start sorting in background...
					sorter.startSorting(tableView, column, ascending);
				}
			}
		};
		JTableHeader th = tableView.getTableHeader();
		th.addMouseListener(listMouseListener);
	}
	
	class RowIndex
	{
		public int rowIndex;
		public RowIndex(int i)
		{
			this.rowIndex = i;
		}
	}
	
	class ColumnComparator implements Comparator
	{
		public ColumnComparator()
		{
		}
		
		public int compare(Object o1, Object o2)
		{
			try
			{
				RowIndex row1 = (RowIndex)o1;
				RowIndex row2 = (RowIndex)o2;
				return WbTableSorter.this.compare(row1.rowIndex, row2.rowIndex);
			}
			catch (ClassCastException e)
			{
				// cannot happen
			}
			return 0;
		}
	}
}