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
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.swing.ComboBoxEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import workbench.resource.ResourceMgr;

import workbench.util.CollectionUtil;

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
public class MultiSelectComboBox<T extends Object>
	extends WbComboBox
	implements PopupMenuListener
{
	private static final String PROP_KEY = "userObject";
	private static final String ALL_ITEMS_SELECTED_DISPLAY = "*";
	private static final EmptyBorder emptyBorder = new EmptyBorder(1,0,1,0);
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
	private final List<JCheckBox> values = Collections.synchronizedList(new ArrayList<JCheckBox>());

	private String nothingSelectedText;
	private String selectAllLabel;
	private String selectNoneLabel;

	private ActionListener listener;
	private boolean selectionRequired;
	private boolean closing;
	private boolean closePopupOnSelect;
	private List<T> lastSelected;

	private int maxElementWidth;

	private final MultiSelectRenderer myRenderer = new MultiSelectRenderer();
	private final DividerBorder topDivider = new DividerBorder(DividerBorder.TOP);

	public MultiSelectComboBox()
	{
		super();
		addPopupMenuListener(this);
		setRenderer(myRenderer);
		nothingSelectedText = ResourceMgr.getString("LblNone");
		selectAllLabel = ResourceMgr.getString("LblSelectAll");
		selectNoneLabel = ResourceMgr.getString("LblSelectNone");
	}

	public void setItems(List<T> items, Collection<T> selectedItems)
	{
		super.removeActionListener(this);

		removeAllItems();

		this.addItem(nothingSelectedText);
		this.addItem(selectAllLabel);
		this.addItem(selectNoneLabel);

		setupItemIndexes();

		maxElementWidth = Integer.MIN_VALUE;

		for (T item : items)
		{
			boolean selected = selectedItems == null ? false : selectedItems.contains(item);
			JCheckBox cb = new JCheckBox(item.toString());
			cb.setBorder(emptyBorder);

			int cwidth = cb.getPreferredSize().width;
			if (cwidth > maxElementWidth)
			{
				maxElementWidth = cwidth;
			}
			cb.putClientProperty(PROP_KEY, item);
			cb.setSelected(selected);
			values.add(cb);
			addItem(cb);
		}
		int scrollWidth = UIManager.getInt("ScrollBar.width");
		setPopupWidth(maxElementWidth + scrollWidth + 5);
		this.setToolTipText(getSelectedItemsDisplay());
		super.addActionListener(this);
	}

	public void setCloseOnSelect(boolean flag)
	{
		this.closePopupOnSelect = flag;
	}

	@Override
	public void removeAllItems()
	{
		super.removeAllItems();
		values.clear();
	}

	@Override
	public void removeActionListener(ActionListener l)
	{
		if (l == this.listener)
		{
			this.listener = null;
		}
	}

	@Override
	public void addActionListener(ActionListener l)
	{
		this.listener = l;
	}

	public int getValueCount()
	{
		if (values == null) return 0;
		return values.size();
	}

	private void setupItemIndexes()
	{
		int count = getItemCount();
		lastCustomIndex = count - 1;
		valueIndexOffset = count;

		for (int i=0; i < count; i++)
		{
			Object o = getItemAt(i);
			if (o == nothingSelectedText)
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
	 * If nothing is selected, null is returned.
	 *
	 * As this combobox can select multiple items,
	 * {@link #getSelectedItems()} should be used instead.
	 *
	 * @return the first selected item, or null item if nothing was selected
	 *
	 * @see #getSelectedItems()
	 */
	@Override
	public T getSelectedItem()
	{
		List<T> items = getSelectedItems();
		if (CollectionUtil.isEmpty(items))
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
		if (values.isEmpty()) return 0;

		synchronized (values)
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
	}

	/**
	 * Returns all items that are selected.
	 *
	 * @return the selected items.
	 * @see #getSelectedCount()
	 */
	public List<T> getSelectedItems()
	{
		List<T> ret = new ArrayList<>();
		if (values.isEmpty()) return ret;

		synchronized (values)
		{
			// Avoid the iterator to prevent a ConcurrentModificationException
			for (JCheckBox cbx : values)
			{
				if (cbx.isSelected())
				{
					ret.add(getUserObject(cbx));
				}
			}
			return ret;
		}
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

	private void fireActionPerformed(ActionEvent e)
	{
		if (this.listener != null)
		{
			listener.actionPerformed(e);
		}
	}

	@Override
	public void removeNotify()
	{
		super.removeNotify();
		if (myRenderer != null)
		{
			myRenderer.dispose();
		}
	}

	private boolean isChanged()
	{
		List<T> selected = getSelectedItems();
		return !Objects.equals(selected, lastSelected);
	}

	private void closeAndFire()
	{
		boolean changed = isChanged();

		boolean allowClose = !selectionRequired || getSelectedCount() > 0;
		if (!allowClose) return;

		try
		{
			closing = true;
			ActionEvent e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, getActionCommand(), 0);
			super.setPopupVisible(false);
			this.setToolTipText(getSelectedItemsDisplay());
			if (changed)
			{
				fireActionPerformed(e);
			}
		}
		finally
		{
			closing = false;
		}
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		int index = getSelectedIndex();
		if (index == summaryIndex)
		{
			closeAndFire();
		}
		else if (index == selectAllIndex)
		{
			selectAll();
		}
		else if (index == selectNoneIndex)
		{
			selectNone();
		}
		else if (index >= valueIndexOffset)
		{
			JCheckBox cb = values.get(index - valueIndexOffset);
			cb.setSelected(!cb.isSelected());
		}
		// clear the selection of the underlying listbox so that all items are "unselected"
		// as the selection itself is represented by the checkbox
		this.setSelectedIndex(-1);
		if (closePopupOnSelect && listener != null)
		{
			closeAndFire();
		}
	}

	public String getSelectedItemsDisplay()
	{
		List<T> items = getSelectedItems();
		if (items.size() == getValueCount())
		{
			return ALL_ITEMS_SELECTED_DISPLAY;
		}
		else if (items.isEmpty())
		{
			return nothingSelectedText;
		}

		StringBuilder display = new StringBuilder(items.size() * 10);
		for (int i = 0; i < items.size(); i++)
		{
			if (i > 0)
			{
				display.append(',');
			}
			display.append(items.get(i).toString());
		}
		return display.toString();
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

	@Override
	public Object getPrototypeDisplayValue()
	{
		return getSelectedItemsDisplay();
	}

	@Override
	public void popupMenuWillBecomeVisible(PopupMenuEvent e)
	{
		lastSelected = getSelectedItems();
	}

	@Override
	public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
	{
		if (closing) return;

		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				closeAndFire();
			}
		});
	}

	@Override
	public void popupMenuCanceled(PopupMenuEvent e)
	{
	}

	private class MultiSelectRenderer
		extends DefaultListCellRenderer
	{
		private ImageIcon icon;
		private Dimension iconSize;
		MultiSelectRenderer()
		{
			icon = ResourceMgr.getImageByName("filter_go16.gif");
			int width = icon.getIconWidth();
			int height = icon.getIconHeight();
			iconSize = new Dimension((int)(width * 1.5), (int)(height * 1.5));
		}

		void dispose()
		{
			if (icon != null && icon.getImage() != null)
			{
				icon.getImage().flush();
			}
		}

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
				renderer.setToolTipText(null);
				return renderer;
			}
			else if (index == selectNoneIndex)
			{
				JComponent renderer = (JComponent) super.getListCellRendererComponent(list, selectNoneLabel, index, isSelected, cellHasFocus);
				renderer.setBorder(DividerBorder.BOTTOM_DIVIDER);
				renderer.setToolTipText(null);
				return renderer;
			}
			else if (index == summaryIndex)
			{
				JComponent renderer = (JComponent) super.getListCellRendererComponent(list, getSelectedItemsDisplay(), index, isSelected, cellHasFocus);
				renderer.setPreferredSize(iconSize);
				renderer.setToolTipText(ResourceMgr.getDescription("LblMultiClose"));
				try
				{
					JLabel label = (JLabel)renderer;
					label.setIcon(icon);
				}
				catch (Throwable th)
				{
					// ignore, should not happen
				}
				return renderer;
			}

			// index == -1 means the currently selected item should be displayed
			// which is only requested when the ComboBox is closed, so we need
			// to adjust the ComboBox editor

			int selectedCount = getSelectedCount();
			ComboBoxEditor editor = MultiSelectComboBox.this.getEditor();
			if (selectedCount == 0)
			{
				// nothing selected --> return the default label
				editor.setItem(nothingSelectedText);
			}
			else if (selectedCount == getValueCount())
			{
				// all items selected --> show a * to indicate "everything"
				editor.setItem(ALL_ITEMS_SELECTED_DISPLAY);
			}
			else
			{
				// only some items are selected --> display a comma separated list of those items
				editor.setItem(getSelectedItemsDisplay());
			}
			return editor.getEditorComponent();
		}
	}

}
