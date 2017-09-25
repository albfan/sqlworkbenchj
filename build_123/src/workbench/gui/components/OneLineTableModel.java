/*
 * OneLineTableModel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.gui.components;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;


/**
 *
 * @author Thomas Kellerer
 */
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

	@Override
	public Object getValueAt(int row, int col)
	{
		return message;
	}

	@Override
	public void setValueAt(Object aValue, int row, int column)
	{
	}

	@Override
	public int getColumnCount()
	{
		return 1;
	}

	@Override
	public int getRowCount()
	{
		return 1;
	}

	@Override
	public boolean isCellEditable(int row, int column)
	{
		return false;
	}

	@Override
	public void addTableModelListener(TableModelListener l)
	{
	}

	@Override
	public Class getColumnClass(int columnIndex)
	{
		return String.class;
	}

	@Override
	public String getColumnName(int columnIndex)
	{
		return this.columnTitle;
	}

	@Override
	public void removeTableModelListener(TableModelListener l)
	{
	}

}
