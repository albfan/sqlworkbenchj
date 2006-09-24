/*
 * ListComboBoxModel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.event.ListDataListener;

/**
 * A ComboBoxModel backed by a List 
 * 
 * @author support@sql-workbench.net
 */
public class ListComboBoxModel
	extends AbstractListModel
	implements ComboBoxModel
{
	private List data = Collections.EMPTY_LIST;
	private List listener = new ArrayList();
	private Object selectedItem;
	
	public ListComboBoxModel()
	{
		data = new ArrayList();
	}
	
	public ListComboBoxModel(List l)
	{
		data = l;
	}

	public void addListDataListener(ListDataListener l)
	{
		listener.add(l);
	}

	public void setData(List l)
	{
		this.data = l;
		fireContentsChanged(this, -1, -1);
	}
	
	public Object getElementAt(int index)
	{
		if (data == null) return null;
		return data.get(index);
	}

	public Object getSelectedItem()
	{
		return selectedItem;
	}

	public int getSize()
	{
		if (data == null) return 0;
		return data.size();
	}

	public int findItemIgnoreCase(String item)
	{
		if (item == null) return -1;
		int size = data.size();
		for (int i=0; i < size; i++)
		{
			Object o = data.get(i);
			if (o == null) continue;
			if (item.equalsIgnoreCase(o.toString())) return i;
		}
		return -1;
	}
	
	public void removeListDataListener(ListDataListener l)
	{
		listener.remove(l);
	}

	public void setSelectedItem(Object anItem)
	{
		this.selectedItem = anItem;
		if (selectedItem != null && !selectedItem.equals(anItem))
		{
			selectedItem = anItem;
			fireContentsChanged(this, -1, -1);
		}
	}

}
