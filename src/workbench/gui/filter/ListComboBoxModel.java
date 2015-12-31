/*
 * ListComboBoxModel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.gui.filter;

import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;

/**
 * A ComboBoxModel backed by a List
 *
 * @author Thomas Kellerer
 */
public class ListComboBoxModel
	extends AbstractListModel
	implements ComboBoxModel
{
	private List<? extends Object> data = new ArrayList<Object>();
	private Object selectedItem;

	public ListComboBoxModel()
	{
		this(new ArrayList<Object>());
	}

	public ListComboBoxModel(List<? extends Object> l)
	{
		super();
		data = l;
	}

	public void setData(List<? extends Object> l)
	{
		this.data = l;
		this.selectedItem = null;
		fireContentsChanged(this, -1, -1);
	}

	@Override
	public Object getElementAt(int index)
	{
		if (data == null) return null;
		return data.get(index);
	}

	@Override
	public Object getSelectedItem()
	{
		return selectedItem;
	}

	@Override
	public int getSize()
	{
		if (data == null) return 0;
		return data.size();
	}

	public int findItemIgnoreCase(String item)
	{
		if (item == null) return -1;
		int size = data.size();
		for (int i = 0; i < size; i++)
		{
			Object o = data.get(i);
			if (o == null) continue;
			if (item.equalsIgnoreCase(o.toString())) return i;
		}
		return -1;
	}

	@Override
	public void setSelectedItem(Object anItem)
	{
		selectedItem = anItem;
		fireContentsChanged(this, -1, -1);
	}

}
