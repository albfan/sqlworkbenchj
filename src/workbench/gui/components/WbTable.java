/*
 * DwTable.java
 *
 * Created on December 1, 2001, 11:41 PM
 */
package workbench.gui.components;

import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import workbench.gui.components.TableSorter;

public class WbTable extends javax.swing.JTable
{
	private TableSorter sortModel;
	
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
	}
	
	public void setModelForSorting(TableModel aModel)
	{
		this.sortModel = new TableSorter(aModel);
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
}

