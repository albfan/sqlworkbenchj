package workbench.gui.components;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;


public class EmptyTableModel implements TableModel
{
	
	public EmptyTableModel() 
	{
	}
	
	public Object getValueAt(int row, int col)
	{
		return "";
	}	

	public void setValueAt(Object aValue, int row, int column)
	{
		return;
	}
	
	public int getColumnCount()
	{
		return 0;
	}

	public int getRowCount() 
	{ 
		return 0;
	}

	public boolean isCellEditable(int row, int column)
	{
		return true;
	}

	public void addTableModelListener(TableModelListener l)
	{
	}	

	public Class getColumnClass(int columnIndex)
	{
		return String.class;
	}
	
	public String getColumnName(int columnIndex)
	{
		return "";
	}
	
	public void removeTableModelListener(TableModelListener l)
	{
	}
	
}
