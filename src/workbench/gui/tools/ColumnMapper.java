/*
 * ColumnMapper.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.tools;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

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

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.ColumnIdentifier;
import workbench.db.importer.RowDataProducer;

import workbench.gui.WbSwingUtilities;
import workbench.gui.components.WbScrollPane;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A panel to map columns from one table definition to another.
 * Source and target are populated with a list of ColumnIdentifiers.
 * Identifiers that have the same name are automatically "mapped".
 *
 * @author  Thomas Kellerer
 */
public class ColumnMapper
	extends JPanel
{
	private JTable columnDisplay;
	private List<ColumnIdentifier> sourceColumns;
	private List<ColumnIdentifier> targetColumns;
	private ColumnMapRow[] mapping;
	protected JComboBox sourceDropDown;
	private final MapDataModel EMPTY_DATA_MODEL = new MapDataModel(new ColumnMapRow[0]);
	private MapDataModel data;

	private boolean allowTargetEditing = false;
	protected boolean allowSourceEditing = false;

	static final SkipColumnIndicator SKIP_COLUMN = new SkipColumnIndicator();

	public ColumnMapper()
	{
		super();
		this.setLayout(new BorderLayout());
		this.columnDisplay = this.createMappingTable();
		this.columnDisplay.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		this.columnDisplay.setRowSelectionAllowed(false);
		WbScrollPane scroll = new WbScrollPane(this.columnDisplay);
		this.add(scroll, BorderLayout.CENTER);
		this.columnDisplay.setModel(EMPTY_DATA_MODEL);
		this.adjustKeyColumn();
	}

	private void adjustKeyColumn()
	{
		TableColumnModel colMod = this.columnDisplay.getColumnModel();
		TableColumn col = colMod.getColumn(2);
		Font f = this.columnDisplay.getTableHeader().getFont();
		FontMetrics fm = this.columnDisplay.getTableHeader().getFontMetrics(f);
		String label = colMod.getColumn(2).getHeaderValue().toString();
		int width = fm.stringWidth(label);
		int addWidth = fm.stringWidth("WWWW");
		col.setMinWidth(width + addWidth);
		col.setMaxWidth(width + addWidth);
		//col.setPreferredWidth(width);
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
			@Override
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

	public void defineColumns(List<ColumnIdentifier> source, List<ColumnIdentifier> target, boolean syncDataTypes, boolean keepSourceOrder)
	{
		if (source == null || target == null) throw new IllegalArgumentException("Both column lists have to be specified");
		this.sourceColumns = source;
		this.targetColumns = target;

		// we cannot have more mapping entries then the number of columns in the target
		int numTargetCols = this.targetColumns.size();
		//int numSourceCols = this.sourceColumns.size();
		this.mapping = new ColumnMapRow[numTargetCols];
		for (int i=0; i < numTargetCols; i++)
		{
			ColumnMapRow row = new ColumnMapRow();
			ColumnIdentifier targetCol = this.targetColumns.get(i);
			row.setTarget(targetCol);

			ColumnIdentifier sourceCol = this.findSourceColumnByName(SqlUtil.removeObjectQuotes(targetCol.getColumnName()));
			if (syncDataTypes && sourceCol != null)
			{
				sourceCol.setDataType(targetCol.getDataType());
				sourceCol.setColumnTypeName(targetCol.getColumnTypeName());
				sourceCol.setDbmsType(targetCol.getDbmsType());
				sourceCol.setDecimalDigits(targetCol.getDecimalDigits());
			}
			row.setSource(sourceCol);
			this.mapping[i] = row;
		}

		this.data = new MapDataModel(this.mapping);
		if (keepSourceOrder)
		{
			data.sortBySourcePosition();
		}
		this.data.setAllowTargetEditing(this.allowTargetEditing);
		this.columnDisplay.setModel(this.data);
		TableColumnModel colMod = this.columnDisplay.getColumnModel();
		TableColumn col = colMod.getColumn(0);

		this.sourceDropDown = this.createDropDown(this.sourceColumns, true);
		Component c = this.sourceDropDown.getEditor().getEditorComponent();
		if (c instanceof JComponent)
		{
			JComponent ce = (JComponent)c;
			ce.setBorder(WbSwingUtilities.EMPTY_BORDER);
		}
		DefaultCellEditor edit = new DefaultCellEditor(this.sourceDropDown);
		col.setCellEditor(edit);

		JTextField targetEditor = new JTextField();
		Font f = Settings.getInstance().getDataFont();
		if (f != null) targetEditor.setFont(f);
		targetEditor.setBorder(WbSwingUtilities.EMPTY_BORDER);
		edit = new DefaultCellEditor(targetEditor);
		col = colMod.getColumn(1);
		col.setCellEditor(edit);

		this.adjustKeyColumn();
		this.columnDisplay.setRowHeight(20);
	}

	public ColumnIdentifier findSourceColumnByName(String aName)
	{
		for (ColumnIdentifier col : this.sourceColumns)
		{
			if (SqlUtil.removeObjectQuotes(col.getColumnName()).equalsIgnoreCase(aName)) return col;
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
		Font f = Settings.getInstance().getDataFont();
		if (f != null) result.setFont(f);
		result.setEditable(allowEditing);
		int count = cols.size();
		if (allowEditing) result.addItem(SKIP_COLUMN);
		for (int i=0; i < count; i++)
		{
			result.addItem(cols.get(i));
		}
		return result;
	}

	private ColumnIdentifier getTargetColumn(ColumnIdentifier source)
	{
		int count = this.mapping.length;
		for (int i=0; i < count; i++)
		{
			ColumnMapRow row = this.mapping[i];
			ColumnIdentifier sourceCol = row.getSource();
			if (sourceCol == null) continue;
			if (sourceCol.getColumnName().equals(source.getColumnName()))
			{
				return row.getTarget();
			}
		}
		return null;
	}

	/**
	 * Return the columns from the input file as they should
	 * be specified for the WbImport command
	 */
	public List<ColumnIdentifier> getMappingForImport()
	{
		int count = this.sourceColumns.size();
		ArrayList<ColumnIdentifier> result = new ArrayList<>(count);
		ColumnIdentifier skipId = new ColumnIdentifier(RowDataProducer.SKIP_INDICATOR);
		for (int i=0; i < count; i++)
		{
			ColumnIdentifier col = this.sourceColumns.get(i);

			ColumnIdentifier target = getTargetColumn(col);

			if (target == null)
			{
				result.add(skipId);
			}
			else
			{
				result.add(target);
			}
		}
		return result;
	}

	protected MappingDefinition getMapping()
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
				if (StringUtil.isBlank(s)) continue;
				realCount ++;
			}
		}
		MappingDefinition def = new MappingDefinition();
		def.sourceColumns = new ColumnIdentifier[realCount];
		def.targetColumns = new ColumnIdentifier[realCount];
		def.hasSkippedColumns = (realCount != count);

		int index = 0;
		for (int i=0; i < count; i++)
		{
			ColumnMapRow row = this.mapping[i];
			String s = null;

			if (row.getSource() != null)
			{
				s = row.getSource().getColumnName();
				if (StringUtil.isBlank(s)) continue;
				def.sourceColumns[index] = row.getSource();
				def.targetColumns[index] = row.getTarget();
				index ++;
			}
		}
		return def;
	}

	public static class MappingDefinition
	{
		public ColumnIdentifier[] sourceColumns;
		public ColumnIdentifier[] targetColumns;
		public boolean hasSkippedColumns;
	}

}

class MapDataModel
	extends AbstractTableModel
{
	private boolean allowTargetEditing = false;
	private ColumnMapRow[] data;
	private final String sourceColName = ResourceMgr.getString("LblSourceColumn");
	private final String targetColName = ResourceMgr.getString("LblTargetColumn");

	MapDataModel(ColumnMapRow[] data)
	{
		super();
		this.data = data;
	}

	@Override
	public Class getColumnClass(int columnIndex)
	{
		if (columnIndex == 2)
			return Boolean.class;
		return ColumnIdentifier.class;
	}

	@Override
	public int getColumnCount()
	{
		return 3;
	}

	@Override
	public String getColumnName(int columnIndex)
	{
		switch (columnIndex)
		{
			case 0:
				return this.sourceColName;
			case 1:
				return this.targetColName;
			case 2:
				return ResourceMgr.getString("LblDPKeyColumnTitle");
			default:
				return "";
		}
	}

	@Override
	public int getRowCount()
	{
		return this.data.length;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		ColumnMapRow row = this.data[rowIndex];
		if (row == null) return "(error)";

		Object value = null;

		switch (columnIndex)
		{
			case 0:
				value = row.getSource();
				if (value == null) value = ColumnMapper.SKIP_COLUMN;
				break;
			case 1:
				value = row.getTarget();
				break;
			case 2:
				ColumnIdentifier col = row.getTarget();
				if (col == null)
				{
					value = Boolean.FALSE;
				}
				else
				{
					boolean pk = col.isPkColumn();
					if (pk) value = Boolean.TRUE;
					else value = Boolean.FALSE;
				}
				break;
		}
		return value;
	}

	public void setAllowTargetEditing(boolean flag)
	{
		this.allowTargetEditing = flag;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex)
	{
		if (rowIndex < 0 || rowIndex > this.getRowCount() -1) return false;
		if (columnIndex == 0) return true;
		if (columnIndex == 1)
		{
			if (!this.allowTargetEditing) return false;
			ColumnMapRow row = this.data[rowIndex];

			return (row.getSource() != null && allowTargetEditing);
		}
		return true;
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex)
	{
		ColumnMapRow row = this.data[rowIndex];
		if (row == null) return;
		if (aValue == null) return;

		switch (columnIndex)
		{
			case 0:
				if (aValue instanceof ColumnIdentifier)
				{
					row.setSource((ColumnIdentifier)aValue);
				}
				else if (aValue instanceof String)
				{
					ColumnIdentifier col = row.getSource();
					String s = (String)aValue;
					if (!StringUtil.isBlank(s))
					{
						if (col == null)
						{
							col = new ColumnIdentifier();
						}
						col.setExpression(s);
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
				break;

			case 1:
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
				break;

			case 2:
				if (aValue instanceof Boolean)
				{
					boolean key = ((Boolean)aValue).booleanValue();
					row.getTarget().setIsPkColumn(key);
				}
		}
	}

	public void sortBySourcePosition()
	{
		Comparator<ColumnMapRow> comp = new Comparator<ColumnMapRow>()
		{
			@Override
			public int compare(ColumnMapRow o1, ColumnMapRow o2)
			{
				if (o1 == null) return 1;
				if (o2 == null) return -1;

				ColumnIdentifier c1 = o1.getSource();
				ColumnIdentifier c2 = o2.getSource();
				if (c1 == null) return 1;
				if (c1 == null) return -1;
				return c1.getPosition() - c2.getPosition();
			}
		};
		Arrays.sort(data, comp);
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

	public ColumnIdentifier getSource()
	{
		return this.source;
	}

	public ColumnIdentifier getTarget()
	{
		return this.target;
	}

	@Override
	public String toString()
	{
		return "Mapping " + source + " -> " + target;
	}
}

class SkipColumnIndicator
{
	private final String display = ResourceMgr.getString("LblDPDoNotCopyColumns");

	@Override
	public String toString()
	{
		return display;
	}

}
