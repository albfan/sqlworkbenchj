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
import javax.swing.table.TableModel;
import workbench.gui.sql.DwTableModel;
import workbench.gui.renderer.DateColumnRenderer;

public class WbTable extends javax.swing.JTable
{
	private WbTableSorter sortModel;
	private DwTableModel dwModel;
	private int lastFoundRow = -1;
	private String lastSearchText = null;
	private TableModelListener changeListener = null;
	
	public WbTable()
	{
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
		}
		else
		{
			this.setModelForSorting(aModel);
		}
		if (aModel instanceof DwTableModel)
		{
			this.dwModel = (DwTableModel)aModel;
			this.dwModel.addTableModelListener(this.changeListener);
		}
	}
	
	public void setModelForSorting(TableModel aModel)
	{
		this.sortModel = new WbTableSorter(aModel);
		super.setModel(this.sortModel);
    JTableHeader header = this.getTableHeader();
		if (header != null)
		{
			header.setDefaultRenderer(new SortHeaderRenderer());
			this.sortModel.addMouseListenerToHeaderInTable(this);
		}
	}

	public int getSortedColumnIndex()
	{
		if (this.sortModel == null) return -1;
		return this.sortModel.getColumn();
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
		int count = this.sortModel.getRowCount();
		StringBuffer result = new StringBuffer(count * 250);
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
	}

	public void removeTableModelListener(TableModelListener aListener)
	{
		this.dwModel.removeTableModelListener(aListener);
	}
	
	public void scrollToRow(int aRow)
	{
		Rectangle rect = this.getCellRect(aRow, 1, true);
		this.scrollRectToVisible(rect);
	}
	
}

