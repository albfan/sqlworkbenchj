/*
 * MultiSelectComboBox.java
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
package workbench.gui.components;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;

import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import workbench.resource.ResourceMgr;

/**
 * A JComboBox containing checkboxes to allow multiple items to be selected.
 *
 * In addition to the passed items, a summary (comma-separated) of the selected items
 * is displayed if the ComboBox is collapsed. If all items are selected a <tt>*</tt>
 * will be displayed.
 *
 * The actual item will be stored as a client property of the created checkboxes in order to be able to
 * return original values from the getSelectedItems() method.
 *
 * If the ComboBox is expanded, two additional items are displayed to enable selection of all or no items
 *
 * @author Thomas Kellerer
 */
public class MultiSelectComboBox<T>
	extends JComboBox
{
	private static final String PROP_KEY = "userObject";
	private static final String ALL_ITEMS_SELECTED_DISPLAY = "*";

	/** holds the index inside this combobox's items which represents the summary display. */
	private int summaryIndex;

	/** holds the index inside this combobox's items which the "select all" item. */
	private int selectAllIndex;

	/** holds the index inside this combobox's items which the "select none" item. */
	private int selectNoneIndex;

	/** the value of the last index containing "custom" items (i.e. no real values). */
	private int lastCustomIndex;

	/** the offset that needs to be subtracted from this combobox items index to get the index inside our own list of checkboxes. */
	private int valueIndexOffset;

	/**
	 * The checkboxes that are displayed.
	 * the actual item value is stored as a client properpty of the checkbox
	 */
	private List<JCheckBox> values;

	private String pleaseSelectLabel;
	private String selectAllLabel;
	private String selectNoneLabel;

	private boolean selectionRequired;

	private final DividerBorder topDivider = new DividerBorder(DividerBorder.TOP);


	/**
	 * Initialize a new MultiSelectComboBox.
	 *
	 * @param items  the items to display. May not contain null values
	 */
	public MultiSelectComboBox(List<T> items)
	{
		super();
		initialize(items, null);
	}

	/**
	 * Initialize a new MultiSelectComboBox.
	 *
	 * @param items          the items to display. May not contain null values
	 * @param selectedItems  the items that should be pre-selected
	 */
	public MultiSelectComboBox(List<T> items, Collection<T> selectedItems)
	{
		super();
		initialize(items, selectedItems);
	}

	private void initialize(List<T> items, Collection<T> selectedItems)
	{
		pleaseSelectLabel = ResourceMgr.getString("LblNone");
		selectAllLabel = ResourceMgr.getString("LblSelectAll");
		selectNoneLabel = ResourceMgr.getString("LblSelectNone");

		values = new ArrayList<>(items.size());

		this.addItem(pleaseSelectLabel);
		this.addItem(selectAllLabel);
		this.addItem(selectNoneLabel);

		setupItemIndexes();

		for (T item : items)
		{
			boolean selected = selectedItems == null ? false : selectedItems.contains(item);
			JCheckBox cb = new JCheckBox(item.toString());
			cb.putClientProperty(PROP_KEY, item);
			cb.setSelected(selected);
			values.add(cb);
			this.addItem(cb);
		}

		addActionListener(this);
		setRenderer(createRenderer());
	}

	private void setupItemIndexes()
	{
		lastCustomIndex = getItemCount() - 1;
		valueIndexOffset = getItemCount();
		for (int i=0; i < getItemCount(); i++)
		{
			Object o = getItemAt(i);
			if (o == pleaseSelectLabel)
			{
				summaryIndex = i;
			}
			else if (o == selectAllLabel)
			{
				selectAllIndex = i;
			}
			else if (o == selectNoneLabel)
			{
				selectNoneIndex = i;
			}
		}
	}

	/**
	 * Controls if the dropdown can be closed even if nothing is selected.
	 *
	 * @param flag  true, the dropdown cannot be closed without a selection.<br/>
	 *              false, the dropdown can be closed without a selection
	 */
	public void setSelectionRequired(boolean flag)
	{
		this.selectionRequired = flag;
	}

	/**
	 * Set the items that are selected.
	 *
	 * All items in the collection are "checked", all other items
	 * in the supplied values are unchecked.
	 *
	 * @param selectedItems the items to select
	 */
	public void setSelectedItems(Collection<T> selectedItems)
	{
		for (JCheckBox cbx : values)
		{
			T item = getUserObject(cbx);
			cbx.setSelected(item != null && selectedItems.contains(item));
		}
	}

	/**
	 * Returns the first selected item.
	 *
	 * If nothing is selected, the default item is returned.
	 *
	 * As this combobox can select multiple items,
	 * {@link #getSelectedItems()} should be used instead.
	 *
	 * @return the first selected item, or the defaul item if nothing was selected
	 *
	 * @see #getSelectedItems()
	 */
	@Override
	public T getSelectedItem()
	{
		List<T> items = getSelectedItems();
		if (items.isEmpty())
		{
			return null;
		}
		return items.get(0);
	}

	/**
	 * Returns the number of selected items.
	 *
	 * This is faster than calling getSelectedItems().size() because no intermediate
	 * List is created to hold the selected items.
	 *
	 * @return the number of selected items.
	 * @see #getSelectedItems()
	 */
	public int getSelectedCount()
	{
		int count = 0;

		for (JCheckBox cbx : values)
		{
			if (cbx.isSelected())
			{
				count ++;
			}
		}
		return count;
	}

	/**
	 * Returns all items that are selected.
	 *
	 * @return the selected items.
	 * @see #getSelectedCount()
	 */
	public List<T> getSelectedItems()
	{
		List<T> ret = new ArrayList<>(values.size());
		for (JCheckBox cbx : values)
		{
			if (cbx.isSelected())
			{
				ret.add(getUserObject(cbx));
			}
		}
		return ret;
	}

	private T getUserObject(JCheckBox cbx)
	{
		return (T)cbx.getClientProperty(PROP_KEY);
	}

	/**
	 * Mark all items as selected.
	 */
	public void selectAll()
	{
		setSelectedForAll(true);
	}

	/**
	 * Mark all items as unselected.
	 * <br/><br/>
	 * After a call to selectNone() {@link #getSelectedCount() } returns 0 (zero).
	 */
	public void selectNone()
	{
		setSelectedForAll(false);
	}

	private void setSelectedForAll(boolean selected)
	{
		for (JCheckBox cb : values)
		{
			cb.setSelected(selected);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		int index = getSelectedIndex();
		if (index == summaryIndex)
		{
			boolean allowClose = !selectionRequired || getSelectedCount() > 0;
			if (allowClose)
			{
				super.setPopupVisible(false);
			}
		}
		else if (index == selectAllIndex)
		{
			selectAll();
		}
		else if (index == selectNoneIndex)
		{
			selectNone();
		}
		else
		{
			JCheckBox cb = values.get(index - valueIndexOffset);
			cb.setSelected(!cb.isSelected());
		}
		// clear the selection of the underlying listbox so that all items are "unselected"
		// as the selection itself is represented by the checkbox
		this.setSelectedIndex(-1);
	}

	public CharSequence getSelectedItemsDisplay()
	{
		List<T> items = getSelectedItems();
		StringBuilder display = new StringBuilder(items.size() * 10);
		for (int i = 0; i < items.size(); i++)
		{
			if (i > 0)
			{
				display.append(',');
			}
			display.append(items.get(i).toString());
		}
		return display;
	}

	/**
	 * Ignores the request to hide the popup.
	 *
	 */
	@Override
	public void hidePopup()
	{
		// ignore the calls to hide the popup.
		// This will prevent the dropdown from being closed when clicking on an item in the list
		// note that this will  not prevent the popup from being closed when clicking on the "down arrow"
	}

	/**
	 * Ignores the request to hide the popup.
	 * @param flag only true is processed. A request to hide the popup is ignored because this is controlled in the actionPerformed event.
	 */
	@Override
	public void setPopupVisible(boolean flag)
	{
		// ignoring the call to hide the popup prevents the popup from closing when an item is selected
		// we only close the checkbox if the "Summary item" is displayed (and that is done "directly" in the actionPerformed method)
		if (flag)
		{
			// do pass the request to show the popup to the actual implementation
			super.setPopupVisible(flag);
		}
	}

	private ListCellRenderer createRenderer()
	{
		return new DefaultListCellRenderer()
		{
			@Override
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
			{
				if (index > lastCustomIndex)
				{
					// this is a CheckBox in our DropDown, so we need to return the CheckBox as the item renderer
					JCheckBox cb = values.get(index - valueIndexOffset);

					// A checkbox does not have the same UI preferences for background foreground colors as the JList does.
					cb.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
					cb.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
					return cb;
				}
				else if (index == selectAllIndex)
				{
					JComponent renderer = (JComponent) super.getListCellRendererComponent(list, selectAllLabel, index, isSelected, cellHasFocus);
					renderer.setBorder(topDivider);
					return renderer;
				}
				else if (index == selectNoneIndex)
				{
					JComponent renderer = (JComponent) super.getListCellRendererComponent(list, selectNoneLabel, index, isSelected, cellHasFocus);
					renderer.setBorder(DividerBorder.BOTTOM_DIVIDER);
					return renderer;
				}

				// the only index left is the index for the "summary display"
				int selectedCount = getSelectedCount();

				if (selectedCount == 0)
				{
					// nothing selected --> return the default label
					return super.getListCellRendererComponent(list, pleaseSelectLabel, index, isSelected, cellHasFocus);
				}

				if (selectedCount == values.size())
				{
					// all items selected --> show a * to indicate "everything"
					return super.getListCellRendererComponent(list, ALL_ITEMS_SELECTED_DISPLAY, index, isSelected, cellHasFocus);
				}

				// only some items are selected --> display a comma separated list of those items
				return super.getListCellRendererComponent(list, getSelectedItemsDisplay(), index, isSelected, cellHasFocus);
			}
		};
	}

}
