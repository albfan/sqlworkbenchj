/*
 * TableHistoryModel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
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
package workbench.gui.dbobjects;

import java.util.Arrays;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.event.ListDataListener;

import workbench.resource.DbExplorerSettings;

import workbench.db.TableIdentifier;

import workbench.util.FixedSizeList;

/**
 *
 * @author Thomas Kellerer
 */
public class TableHistoryModel
	extends DefaultComboBoxModel
{
	private final FixedSizeList<TableIdentifier> history;

	public TableHistoryModel()
	{
		history = new FixedSizeList<>(DbExplorerSettings.getDbExplorerTableHistorySize());
	}

	public TableIdentifier getSelectedTable()
	{
		return (TableIdentifier)getSelectedItem();
	}

	@Override
	public int getSize()
	{
		return history.size();
	}

	@Override
	public Object getElementAt(int index)
	{
		return history.get(index);
	}

	@Override
	public void addElement(Object obj)
	{
		addTable((TableIdentifier)obj);
	}

	public void addTable(TableIdentifier tbl)
	{
		this.insertElementAt(tbl, 0);
		fireIntervalAdded(this,0, 0);
		setSelectedItem(tbl);
	}

	@Override
	public void removeElement(Object obj)
	{
		int index = history.indexOf((TableIdentifier)obj);
		if (index != -1)
		{
			removeElementAt(index);
		}
	}

	@Override
	public void insertElementAt(Object obj, int index)
	{
		history.add(index, (TableIdentifier)obj);
		fireIntervalAdded(this, index, index);
	}

	@Override
	public void removeElementAt(int index)
	{
		if (getElementAt(index) == getSelectedItem())
		{
			if (index == 0)
			{
				setSelectedItem(getSize() == 1 ? null : getElementAt(index + 1));
			}
			else
			{
				setSelectedItem(getElementAt(index - 1));
			}
		}
		history.remove(index);
		fireIntervalRemoved(this, index, index);
	}

	@Override
	public void removeAllElements()
	{
		super.removeAllElements(); // this will reset the selectecObject
		int size = history.size();
		if (size > 0)
		{
			history.clear();
			fireIntervalRemoved(this, 0, size - 1);
		}
	}

	public void clearListeners()
	{
		List<ListDataListener> listeners = Arrays.asList(listenerList.getListeners(ListDataListener.class));
		for (ListDataListener l : listeners)
		{
			removeListDataListener(l);
		}
	}
}
