package workbench.gui.components;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;


public class OneLineTableModel 
	implements TableModel
{
	private String columnTitle;
	private String message;
	
	public OneLineTableModel(String colTitle, String msg) 
	{
		this.columnTitle = colTitle;
		this.message = msg;
	}

	public void setMessage(String aMessage)
	{
		this.message = aMessage;
	}
	
	public Object getValueAt(int row, int col)
	{
		return message;
	}	

	public void setValueAt(Object aValue, int row, int column)
	{
		return;
	}
	
	public int getColumnCount()
	{
		return 1;
	}

	public int getRowCount() 
	{ 
		return 1;
	}

	public boolean isCellEditable(int row, int column)
	{
		return false;
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
		return this.columnTitle;
	}
	
	public void removeTableModelListener(TableModelListener l)
	{
	}
	
}
