/*
 * DwTable.java
 *
 * Created on December 1, 2001, 11:41 PM
 */
package workbench.gui.components;

import java.awt.Cursor;
import java.awt.Rectangle;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import javax.swing.JScrollPane;
import javax.swing.event.TableModelListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import workbench.gui.renderer.DateColumnRenderer;
import workbench.gui.renderer.RowStatusRenderer;

public class WbTable extends javax.swing.JTable
{
	private WbTableSorter sortModel;
	private ResultSetTableModel dwModel;
	private TableModel originalModel;
	private int lastFoundRow = -1;
	private String lastSearchText = null;
	private TableModelListener changeListener = null;
	
	public WbTable()
	{
		this.setMinimumSize(null);
		this.setMaximumSize(null);
		this.setPreferredSize(null);
	}
	
	public void setModel(TableModel aModel)
	{
		this.setModel(aModel, false);
	}
	
	public void setModel(TableModel aModel, boolean sortIt)
	{
		if (!sortIt)
		{
			super.setModel(aModel);
			this.sortModel = null;
			this.originalModel = aModel;
			this.checkModel();
		}
		else
		{
			this.setModelForSorting(aModel);
		}
	}
	
	public void setModelForSorting(TableModel aModel)
	{
		this.originalModel = aModel;
		this.sortModel = new WbTableSorter(aModel);
		super.setModel(this.sortModel);
    JTableHeader header = this.getTableHeader();
		if (header != null)
		{
			header.setDefaultRenderer(new SortHeaderRenderer());
			this.sortModel.addMouseListenerToHeaderInTable(this);
		}
		this.checkModel();
	}

	private void checkModel()
	{
		if (this.originalModel == null) return;
		if (this.originalModel instanceof ResultSetTableModel)
		{
			this.dwModel = (ResultSetTableModel)this.originalModel;
			if (this.changeListener != null) this.dwModel.addTableModelListener(this.changeListener);
			TableColumn col = this.getColumnModel().getColumn(0);
			col.setCellRenderer(new RowStatusRenderer());
			col.setMaxWidth(20);
			col.setMinWidth(20);
			col.setPreferredWidth(20);
		}
	}
	
	public int getSortedViewColumnIndex()
	{
		if (this.sortModel == null) return -1;
		int modelIndex = this.sortModel.getColumn();
		int viewIndex = this.convertColumnIndexToView(modelIndex);
		return viewIndex;
	}
	
	public boolean isSortedColumnAscending()
	{
		if (this.sortModel == null) return true;
		return this.sortModel.isAscending();
	}
	
	public synchronized void sortingStarted()
	{
		this.setIgnoreRepaint(true);
	}

	public synchronized void sortingFinished()
	{
		this.setIgnoreRepaint(false);
	}
	
	public String getDataString(String aLineTerminator)
	{
		int colCount = this.sortModel.getColumnCount();
		int count = this.sortModel.getRowCount();
		StringBuffer result = new StringBuffer(count * 250);
		// Start loop at 1 --> ignore status column
		for (int i=1; i < colCount; i++)
		{
			result.append(this.sortModel.getColumnName(i));
			if (i < colCount - 1) result.append('\t');
		}
		result.append(aLineTerminator);
		for (int i=0; i < count; i++)
		{
			int row = this.sortModel.getRealIndex(i);
			result.append(this.dwModel.getRowData(row));
			result.append(aLineTerminator);
		}
		return result.toString();
	}
	
	public void saveAsAscii(String aFilename)
		throws IOException
	{
		if (this.sortModel == null || this.dwModel == null) return;
		
		PrintWriter out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(aFilename)));
		this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		this.getDataString("\r\n");
		out.close();
		this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
	
	public boolean canSearchAgain()
	{
		return this.lastFoundRow >= 0;
	}
	
	public int search(String aText)
	{
		return this.search(aText, false);
	}
	
	public int searchNext()
	{
		return this.search(this.lastSearchText, true);
	}
	
	public int search(String aText, boolean doContinue)
	{
		if (aText == null) return -1;
		aText = aText.toLowerCase();
		this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		int foundRow = -1;
		int start = 0;
		if (doContinue && this.lastFoundRow >= 0)
		{
			start = this.lastFoundRow  + 1;
		}
	
		for (int i=start; i < this.sortModel.getRowCount(); i++)
		{
			int row = sortModel.getRealIndex(i);
			String rowString = this.dwModel.getRowData(row).toString();
			if (rowString == null) continue;
			
			if (rowString.toLowerCase().indexOf(aText) > -1)
			{
				//this.infoTable.scrollToRow(i);
				this.getSelectionModel().setSelectionInterval(i,i);
				foundRow = i;
				this.lastSearchText = aText;
				break;
			}
		}
		this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		this.lastFoundRow = foundRow;
		if (foundRow >= 0)
		{
			this.scrollToRow(foundRow);
		}
			
		return foundRow;
	}
	
	public void addTableModelListener(TableModelListener aListener)
	{
		this.changeListener = aListener;
		if (this.dwModel != null) this.dwModel.addTableModelListener(aListener);
	}

	public void removeTableModelListener(TableModelListener aListener)
	{
		this.changeListener = null;
		if (this.dwModel != null) this.dwModel.removeTableModelListener(aListener);
	}
	
	public void scrollToRow(int aRow)
	{
		Rectangle rect = this.getCellRect(aRow, 1, true);
		this.scrollRectToVisible(rect);
	}
	
}

