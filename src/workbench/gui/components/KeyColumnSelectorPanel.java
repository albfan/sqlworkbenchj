/*
 * KeyColumnSelectorPanel.java
 *
 * Created on September 11, 2004, 6:08 PM
 */

package workbench.gui.components;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import workbench.db.ColumnIdentifier;
import workbench.resource.ResourceMgr;

/**
 *
 * @author  workbench@kellerer.org
 */
public class KeyColumnSelectorPanel
	extends JPanel
{
	private JTable selectTable;
	private KeySelectTableModel model;
	
	public KeyColumnSelectorPanel(ColumnIdentifier[] columns, String table)
	{
		this.setLayout(new BorderLayout());
		this.selectTable = new JTable();
		this.selectTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		this.selectTable.setRowSelectionAllowed(false);
		this.selectTable.setColumnSelectionAllowed(false);
		this.model = new KeySelectTableModel(columns);
		this.selectTable.setModel(this.model);
		TableColumnModel colMod = this.selectTable.getColumnModel();
		TableColumn col = colMod.getColumn(0);
		col.setPreferredWidth(150);
		col.setMinWidth(50);
		col = colMod.getColumn(1);
		col.setPreferredWidth(100);
		col.setMinWidth(50);
		WbScrollPane scroll = new WbScrollPane(this.selectTable);
		String msg = ResourceMgr.getString("MsgSelectKeyColumns").replaceAll("%tablename%", table);
		JLabel info = new JLabel(msg);
		this.add(info, BorderLayout.NORTH);
		this.add(scroll, BorderLayout.CENTER);
		Dimension d = new Dimension(280, 160);
		this.setPreferredSize(d);
	}

	public ColumnIdentifier[] getColumns()
	{
		return this.model.columns;
	}
	
}

class KeySelectTableModel
	implements TableModel
{
	ColumnIdentifier[] columns;
	private String colLabel = ResourceMgr.getString("LabelHeaderKeyColumnColName");
	private String pkLabel = ResourceMgr.getString("LabelHeaderKeyColumnPKFlag");
	private int rows;
	public KeySelectTableModel(ColumnIdentifier[] cols)
	{
		this.rows = cols.length;
		this.columns = new ColumnIdentifier[rows];
		for (int i=0; i < rows; i++)
		{
			this.columns[i] = cols[i].createCopy();
		}
	}
	
	public int getColumnCount()
	{
		return 2;
	}
	
	public Object getValueAt(int row, int column)
	{
		if (column == 0)
		{
			return this.columns[row].getColumnName();
		}
		else if (column == 1)
		{
			return new Boolean(this.columns[row].isPkColumn());
		}
		return "";
	}
	
	public int getRowCount()
	{
		return this.rows;
	}
	
	public void addTableModelListener(javax.swing.event.TableModelListener l)
	{
	}
	
	public Class getColumnClass(int columnIndex)
	{
		if (columnIndex == 0) return String.class;
		else return Boolean.class;
	}
	
	public String getColumnName(int columnIndex)
	{
		if (columnIndex == 0) return this.colLabel;
		return this.pkLabel;
	}
	
	public boolean isCellEditable(int rowIndex, int columnIndex)
	{
		return (columnIndex == 1);
	}
	
	public void removeTableModelListener(javax.swing.event.TableModelListener l)
	{
	}
	
	public void setValueAt(Object aValue, int rowIndex, int columnIndex)
	{
		if (columnIndex == 1 && aValue instanceof Boolean)
		{
			Boolean b = (Boolean)aValue;
			this.columns[rowIndex].setIsPkColumn(b.booleanValue());
		}
	}
	
}