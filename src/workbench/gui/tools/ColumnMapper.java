/*
 * ColumnMapper.java
 *
 * Created on December 21, 2003, 1:09 PM
 */

package workbench.gui.tools;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import workbench.WbManager;
import workbench.db.ColumnIdentifier;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.WbScrollPane;
import workbench.gui.components.WbTable;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.storage.NullValue;

/**
 *
 * @author  workbench@kellerer.org
 */
public class ColumnMapper
	extends JPanel
{
	private JTable columnDisplay;
	private JTextField targetEditor;
	private List sourceColumns;
	private List targetColumns;
	private ColumnMapRow[] mapping;
	private JComboBox sourceDropDown;
	private static final MapDataModel EMPTY_DATA_MODEL = new MapDataModel(new ColumnMapRow[0]);
	private MapDataModel data;
	
	private boolean allowTargetEditing = false;
	private boolean allowSourceEditing = false;
	
	public static final SkipColumnIndicator SKIP_COLUMN = new SkipColumnIndicator();
	
	public ColumnMapper()
	{
		this.setLayout(new BorderLayout());
		this.columnDisplay = this.createMappingTable();
		this.columnDisplay.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		this.columnDisplay.setRowSelectionAllowed(false);
		WbScrollPane scroll = new WbScrollPane(this.columnDisplay);
		this.add(scroll, BorderLayout.CENTER);
		this.columnDisplay.setModel(EMPTY_DATA_MODEL);
	}

	public void resetData()
	{
		if (this.columnDisplay.getModel() != EMPTY_DATA_MODEL)
		{
			this.columnDisplay.setModel(EMPTY_DATA_MODEL);
		}
	}
	
	private JTable createMappingTable()
	{
		// Create a specialized JTable which enables or 
		// disables the editing of the sourceDropDown based on the
		// current value of the column (if the value is set to "Skip column"
		// then it may not be edited even if source editing is allowed
		JTable t = new JTable()
		{
			public TableCellEditor getCellEditor(int row, int column)
			{
				TableCellEditor editor = super.getCellEditor(row, column);
				if (allowSourceEditing && column == 0)
				{
					Object current = getValueAt(row, column);
					if (current == null || current instanceof SkipColumnIndicator)
					{
						sourceDropDown.setEditable(false);
					}
					else
					{
						sourceDropDown.setEditable(allowSourceEditing);
					}
				}
				return editor;
			}
		};
		return t;
	}
	public void defineColumns(List source, List target)
	{
		if (source == null || target == null) throw new IllegalArgumentException("Both column lists have to be specified");
		this.sourceColumns = source;
		this.targetColumns = target;
		
		// we cannot have more mapping entries then the number of columns in the target
		int numTargetCols = this.targetColumns.size();
		int numSourceCols = this.sourceColumns.size();
		this.mapping = new ColumnMapRow[numTargetCols];
		for (int i=0; i < numTargetCols; i++)
		{
			ColumnMapRow row = new ColumnMapRow();
			ColumnIdentifier targetCol = (ColumnIdentifier)this.targetColumns.get(i);
			row.setTarget(targetCol);

			ColumnIdentifier sourceCol = this.findSourceColumnByName(targetCol.getColumnName());
			row.setSource(sourceCol);
			this.mapping[i] = row;
		}
		
		this.data = new MapDataModel(this.mapping);
		this.data.setAllowTargetEditing(this.allowTargetEditing);
		this.columnDisplay.setModel(this.data);
		TableColumnModel colMod = this.columnDisplay.getColumnModel();
		TableColumn col = colMod.getColumn(0);
		
		this.sourceDropDown = this.createDropDown(this.sourceColumns, true);
		Component c = this.sourceDropDown.getEditor().getEditorComponent();
		if (c != null && c instanceof JComponent)
		{
			JComponent ce = (JComponent)c;
			ce.setBorder(WbSwingUtilities.EMPTY_BORDER);
		}
		DefaultCellEditor edit = new DefaultCellEditor(this.sourceDropDown);
		col.setCellEditor(edit);
	
		this.targetEditor = new JTextField();
		this.targetEditor.setFont(WbManager.getSettings().getDataFont());
		this.targetEditor.setBorder(WbSwingUtilities.EMPTY_BORDER);
		edit = new DefaultCellEditor(this.targetEditor);
		col = colMod.getColumn(1);
		col.setCellEditor(edit);
		
		this.columnDisplay.setRowHeight(20);
	}
	
	public ColumnIdentifier findSourceColumnByName(String aName)
	{
		int count = this.sourceColumns.size();
		for (int i=0; i < count; i++)
		{
			ColumnIdentifier col = (ColumnIdentifier)this.sourceColumns.get(i);
			if (col.getColumnName().equalsIgnoreCase(aName)) return col;
		}
		return null;
	}
	
	public void setAllowSourceEditing(boolean aFlag)
	{
		this.allowSourceEditing = aFlag;
		this.sourceDropDown.setEditable(aFlag);
	}
	
	public void setAllowTargetEditing(boolean aFlag)
	{
		this.allowTargetEditing = true;
		if (this.data != null)
		{
			this.data.setAllowTargetEditing(aFlag);
		}
	}
	
	private JComboBox createDropDown(List cols, boolean allowEditing)
	{
		JComboBox result = new JComboBox();
		result.setFont(WbManager.getSettings().getDataFont());
		result.setEditable(allowEditing);
		int count = cols.size();
		if (allowEditing) result.addItem(SKIP_COLUMN);
		for (int i=0; i < count; i++)
		{
			result.addItem(cols.get(i));
		}
		return result;
	}
	
	public MappingDefinition getMapping()
	{
		int count = this.mapping.length;
		int realCount = 0;
		for (int i=0; i < count; i++)
		{
			ColumnMapRow row = this.mapping[i];
			String s = null;
			
			if (row.getSource() != null)
			{
				s = row.getSource().getColumnName();
				if (s == null || s.trim().length() == 0) continue;
				realCount ++;
			}
		}
		MappingDefinition def = new MappingDefinition();
		def.sourceColumns = new ColumnIdentifier[realCount];
		def.targetColumns = new ColumnIdentifier[realCount];

		int index = 0;
		for (int i=0; i < count; i++)
		{
			ColumnMapRow row = this.mapping[i];
			String s = null;
			
			if (row.getSource() != null)
			{
				s = row.getSource().getColumnName();
				if (s == null || s.trim().length() == 0) continue;
				def.sourceColumns[index] = row.getSource();
				def.targetColumns[index] = row.getTarget();
				index ++;
			}
		}
		return def;
	}
	
	class MappingDefinition
	{
		public ColumnIdentifier[] sourceColumns;
		public ColumnIdentifier[] targetColumns;
	}

}

class MapDataModel
	extends AbstractTableModel
{
	private boolean allowTargetEditing = false;
	private ColumnMapRow[] data;
	private final String sourceColName = ResourceMgr.getString("LabelSourceColumn");
	private final String targetColName = ResourceMgr.getString("LabelTargetColumn");
	
	public MapDataModel(ColumnMapRow[] data)
	{
		this.data = data;
	}
	
	public Class getColumnClass(int columnIndex)
	{
		return ColumnIdentifier.class;
	}
	
	public int getColumnCount()
	{
		return 2;
	}
	
	public String getColumnName(int columnIndex)
	{
		if (columnIndex == 0)
			return this.sourceColName;
		else
			return this.targetColName;
	}
	
	public int getRowCount()
	{
		return this.data.length;
	}
	
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		ColumnMapRow row = this.data[rowIndex];
		if (row == null) return "(error)";
		ColumnIdentifier col = null;
		
		if (columnIndex == 0)
		{
			col = row.getSource();
			if (col == null) return ColumnMapper.SKIP_COLUMN;
		}
		else
		{
			col = row.getTarget();
		}
		return col;
	}
	
	public void setAllowTargetEditing(boolean flag)
	{
		this.allowTargetEditing = flag;
	}
	
	public boolean isCellEditable(int rowIndex, int columnIndex)
	{
		if (rowIndex < 0 || rowIndex > this.getRowCount() -1) return false;
		if (columnIndex == 0) return true;
		if (!this.allowTargetEditing) return false;
		ColumnMapRow row = this.data[rowIndex];

		return (row.getSource() != null && allowTargetEditing);
	}
	
	public void setValueAt(Object aValue, int rowIndex, int columnIndex)
	{
		ColumnMapRow row = this.data[rowIndex];
		if (row == null) return;
		if (aValue == null) return;
		
		if (columnIndex == 0)
		{
			if (aValue instanceof ColumnIdentifier)
			{
				row.setSource((ColumnIdentifier)aValue);
			}
			else if (aValue instanceof String)
			{
				ColumnIdentifier col = row.getSource();
				String s = (String)aValue;
				if (s.trim().length() > 0)
				{
					if (col == null)
					{
						col = new ColumnIdentifier();
						col.setExpression(s);
					}
					else
					{
						col.setExpression(s);
					}
				}
			}
			else if (aValue instanceof SkipColumnIndicator)
			{
				row.setSource(null);
			}
			else
			{
				LogMgr.logWarning("ColumnMapper.setValueAt()", "Unsupported data type " + aValue.getClass().getName());
			}
		}
		else
		{
			if (aValue instanceof ColumnIdentifier)
			{
				row.setTarget((ColumnIdentifier)aValue);
			}
			else if (this.allowTargetEditing && aValue instanceof String)
			{
				ColumnIdentifier col = new ColumnIdentifier((String)aValue);
				row.setTarget(col);
			}
			else
			{
				LogMgr.logWarning("ColumnMapper.setValueAt()", "Unsupported data type " + aValue.getClass().getName());
			}
		}
	}
	
}
class ColumnMapRow
{
	private ColumnIdentifier source;
	private ColumnIdentifier target;
	
	public void setTarget(ColumnIdentifier id)
	{
		this.target = id;
	}
	
	public void setSource(ColumnIdentifier o)
	{
		this.source = o;
	}
	
	public ColumnIdentifier getSource() { return this.source; }
	public ColumnIdentifier getTarget() { return this.target; }

	public String toString()
	{
		return "Mapping " + source + " -> " + target;
	}
}

class SkipColumnIndicator
{
	private final String display = ResourceMgr.getString("LabelDPDoNotCopyColumns");
	
	public SkipColumnIndicator()
	{
	}
	public String toString()
	{
		return display;
	}
}