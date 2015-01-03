/*
 * ColumnMapper.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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
import java.util.List;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

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

	MappingDefinition getMapping()
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

	static class MappingDefinition
	{
		public ColumnIdentifier[] sourceColumns;
		public ColumnIdentifier[] targetColumns;
		public boolean hasSkippedColumns;
	}

}

