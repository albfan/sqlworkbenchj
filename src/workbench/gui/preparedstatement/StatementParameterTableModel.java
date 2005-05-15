/*
 * StatementParameterTableModel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
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
 * @author  support@sql-workbench.net
 */
public class StatementParameterTableModel
	implements TableModel
{
	private String[] columns = { ResourceMgr.getString("TxtPSParameterIndex"), ResourceMgr.getString("TxtPSParameterType"), ResourceMgr.getString("TxtPSParameterValue") };
	private Integer[] parameterIndex;
	private String[] types;
	private String[] values;
	private StatementParameters parms;
	
	public StatementParameterTableModel(StatementParameters parm)
	{
		this.parms = parm;
		int count = parm.getParameterCount();
		this.parameterIndex = new Integer[count];
		this.types = new String[count];
		this.values = new String[count];
		for (int i=0; i < count; i++)
		{
			this.parameterIndex[i] = new Integer(i+1);
			this.types[i] = SqlUtil.getTypeName(parm.getParameterType(i));
			Object v = parm.getParameterValue(i);
			this.values[i] = (v == null ? "" : v.toString());
		}
	}

	public void addTableModelListener(javax.swing.event.TableModelListener l)
	{
	}

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

	public int getColumnCount()
	{
		return 3;
	}

	public String getColumnName(int columnIndex)
	{
		return columns[columnIndex];
	}

	public int getRowCount()
	{
		return this.parms.getParameterCount();
	}

	public String getParameterValue(int index)
	{
		return this.values[index];
	}
	
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		if (columnIndex == 0)
		{
			return parameterIndex[rowIndex];
		}
		else if (columnIndex == 1)
		{
			return types[rowIndex];
		}
		else 
		{
			return values[rowIndex];
		}
	}

	public boolean isCellEditable(int rowIndex, int columnIndex)
	{
		return (columnIndex == 2);
	}

	public void removeTableModelListener(javax.swing.event.TableModelListener l)
	{
	}

	public void setValueAt(Object aValue, int rowIndex, int columnIndex)
	{
		if (columnIndex == 2)
		{
			this.values[rowIndex] = (aValue == null ? "" : aValue.toString());
		}
	}
	
}
