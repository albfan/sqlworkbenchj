/*
 * DwTable.java
 *
 * Created on December 1, 2001, 11:41 PM
 */
package workbench.gui.components;

import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import workbench.gui.components.TableSorter;

public class DwTable 
	extends javax.swing.JTable
{
	private TableSorter sortModel;
	
	public DwTable()
	{
	}
	
	public void setModel(TableModel aModel)
	{
		super.setModel(aModel);
		this.sortModel = null;
	}
	
	public void setSortModel(TableModel aModel)
	{
		this.sortModel = new TableSorter(aModel);
		super.setModel(this.sortModel);
    JTableHeader header = this.getTableHeader();
		header.setDefaultRenderer(new SortHeaderRenderer());
		this.sortModel.addMouseListenerToHeaderInTable(this);
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
}

