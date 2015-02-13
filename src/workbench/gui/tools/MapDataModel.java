/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015 Thomas Kellerer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.tools;

import java.util.Arrays;
import java.util.Comparator;

import javax.swing.table.AbstractTableModel;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.db.ColumnIdentifier;

import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
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
				if (c2 == null) return -1;
				return c1.getPosition() - c2.getPosition();
			}
		};
		Arrays.sort(data, comp);
	}
}
