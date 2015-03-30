/*
 * ClientSideTableSearchPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.gui.dbobjects;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyListener;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import workbench.db.search.ClientSideTableSearcher;
import workbench.db.search.TableDataSearcher;
import workbench.gui.components.TextComponentMouseListener;
import workbench.gui.filter.ComparatorListItem;
import workbench.gui.filter.ListComboBoxModel;
import workbench.interfaces.PropertyStorage;
import workbench.resource.ResourceMgr;
import workbench.storage.filter.ColumnComparator;
import workbench.storage.filter.ContainsComparator;
import workbench.storage.filter.RegExComparator;
import workbench.storage.filter.StartsWithComparator;
import workbench.storage.filter.StringEqualsComparator;
import workbench.util.CollectionUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class ClientSideTableSearchPanel
	extends JPanel
	implements TableSearchCriteriaGUI
{
	private JTextField searchText;
	private JCheckBox ignoreCase;
	private JComboBox comparatorDropDown;
	private ClientSideTableSearcher searcher;

	public ClientSideTableSearchPanel()
	{
		super(new GridBagLayout());
		initComponents();
		searcher = new ClientSideTableSearcher();
	}

	private void initComponents()
	{
    JLabel lbl = new JLabel();
    lbl.setText(ResourceMgr.getString("LblSearchTableTxtCriteria"));
		String tip = ResourceMgr.getDescription("LblSearchTableTxtCriteria");
    lbl.setToolTipText(tip);

		searchText = new JTextField();
		TextComponentMouseListener.addListener(searchText);
    searchText.setToolTipText(tip);

		GridBagConstraints constraints = new GridBagConstraints();
    constraints = new GridBagConstraints();
    constraints.gridx = 0;
    constraints.gridy = 0;
    constraints.insets = new Insets(0, 0, 0, 2);
    add(lbl, constraints);

		comparatorDropDown = new JComboBox();

		List<ComparatorListItem> items = CollectionUtil.arrayList();

		items.add(new ComparatorListItem(new ContainsComparator()));
		items.add(new ComparatorListItem(new StartsWithComparator()));
		items.add(new ComparatorListItem(new StringEqualsComparator()));
		items.add(new ComparatorListItem(new RegExComparator()));

		ListComboBoxModel model = new ListComboBoxModel(items);
		comparatorDropDown.setModel(model);
		comparatorDropDown.setSelectedIndex(0);

		//comparatorDropDown.setModel();
    constraints = new GridBagConstraints();
    constraints.gridx = 1;
    constraints.gridy = 0;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 0.0;
    constraints.insets = new Insets(0, 2, 0, 5);
		add(comparatorDropDown, constraints);

    constraints = new GridBagConstraints();
    constraints.gridx = 2;
    constraints.gridy = 0;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 1.0;
    constraints.insets = new Insets(0, 2, 0, 5);
    add(searchText, constraints);

		ignoreCase = new JCheckBox(ResourceMgr.getString("LblFilterIgnoreCase"));
    constraints = new GridBagConstraints();
    constraints.gridx = 3;
    constraints.gridy = 0;
    constraints.fill = GridBagConstraints.NONE;
    constraints.weightx = 0;
    constraints.insets = new Insets(0, 4, 0, 2);
    add(ignoreCase, constraints);

	}

	private ColumnComparator getComparator()
	{
		ComparatorListItem item = (ComparatorListItem)comparatorDropDown.getSelectedItem();
		return item.getComparator();
	}

	@Override
	public void disableControls()
	{
		searchText.setEnabled(false);
		comparatorDropDown.setEnabled(false);
	}

	@Override
	public void enableControls()
	{
		searchText.setEnabled(true);
		comparatorDropDown.setEnabled(true);
	}

	@Override
	public TableDataSearcher getSearcher()
	{
		// Comparator must be defined before setting the criteria!
		searcher.setComparator(getComparator());
		searcher.setCriteria(searchText.getText(), ignoreCase.isSelected());
		return searcher;
	}

	@Override
	public void saveSettings(String prefix, PropertyStorage props)
	{
		props.setProperty(prefix + ".clientsearch.criteria", this.searchText.getText());
		props.setProperty(prefix + ".clientsearch.comparator", getComparator().getClass().getName());
		props.setProperty(prefix + ".clientsearch.ignorecase", ignoreCase.isSelected());
	}

	@Override
	public void restoreSettings(String prefix, PropertyStorage props)
	{
		searchText.setText(props.getProperty(prefix + ".clientsearch.criteria", ""));
		ignoreCase.setSelected(props.getBoolProperty(prefix + ".clientsearch.ignorecase", true));
		String compClass = props.getProperty(prefix + ".clientsearch.comparator", null);
		if (StringUtil.isNonBlank(compClass))
		{
			int count = comparatorDropDown.getItemCount();
			for (int i=0; i < count; i++)
			{
				ComparatorListItem item = (ComparatorListItem)comparatorDropDown.getItemAt(i);
				ColumnComparator comp = item.getComparator();
				if (comp.getClass().getName().equals(compClass))
				{
					comparatorDropDown.setSelectedIndex(i);
				}
			}
		}
	}

	@Override
	public void addKeyListenerForCriteria(KeyListener listener)
	{
		searchText.addKeyListener(listener);
	}

}
