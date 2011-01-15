/*
 * StatementParameterTableModel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.preparedstatement;

import javax.swing.table.TableModel;
import workbench.resource.ResourceMgr;
import workbench.sql.preparedstatement.StatementParameters;
import workbench.util.SqlUtil;

/**
 * @author  Thomas Kellerer
 */
public class StatementParameterTableModel
	implements TableModel
{
	private final String[] columns;
	private Integer[] parameterIndex;
	private String[] types;
	private String[] values;

	private StatementParameters parms;

	public StatementParameterTableModel(StatementParameters parm, boolean showParameterNames)
	{
		if (showParameterNames)
		{
			columns = new String[] { ResourceMgr.getString("TxtPSParameterIndex"), ResourceMgr.getString("TxtPSParameterName"), ResourceMgr.getString("TxtPSParameterType"), ResourceMgr.getString("TxtPSParameterValue") };
		}
		else
		{
			columns = new String[] { ResourceMgr.getString("TxtPSParameterIndex"), ResourceMgr.getString("TxtPSParameterType"), ResourceMgr.getString("TxtPSParameterValue") };
		}
		this.parms = parm;
		int count = parm.getParameterCount();
		this.parameterIndex = new Integer[count];
		this.types = new String[count];
		this.values = new String[count];
		for (int i=0; i < count; i++)
		{
			this.parameterIndex[i] = Integer.valueOf(i+1);
			this.types[i] = SqlUtil.getTypeName(parm.getParameterType(i));
			Object v = parm.getParameterValue(i);
			this.values[i] = (v == null ? "" : v.toString());
		}
	}

	@Override
	public Class getColumnClass(int columnIndex)
	{
		if (columnIndex == 0)
		{
			return Integer.class;
		}
		else
		{
			return String.class;
		}
	}

	@Override
	public int getColumnCount()
	{
		return columns.length;
	}

	@Override
	public String getColumnName(int columnIndex)
	{
		return columns[columnIndex];
	}

	@Override
	public int getRowCount()
	{
		return this.parms.getParameterCount();
	}

	public String getParameterValue(int index)
	{
		return this.values[index];
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		if (columnIndex == 0)
		{
			return parameterIndex[rowIndex];
		}

		if (columns.length == 4)
		{
			switch (columnIndex)
			{
				case 1:
					return parms.getParameterName(rowIndex);
				case 2:
					return types[rowIndex];
				case 3:
					return values[rowIndex];
			}
		}
		else
		{
			switch (columnIndex)
			{
				case 1:
					return types[rowIndex];
				case 2:
					return values[rowIndex];
			}
		}
		return null;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex)
	{
		return (columnIndex == (columns.length - 1));
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex)
	{
		if (columnIndex == (columns.length -1))
		{
			this.values[rowIndex] = (aValue == null ? "" : aValue.toString());
		}
	}

	@Override
	public void removeTableModelListener(javax.swing.event.TableModelListener l)
	{
	}

	@Override
	public void addTableModelListener(javax.swing.event.TableModelListener l)
	{
	}


}
