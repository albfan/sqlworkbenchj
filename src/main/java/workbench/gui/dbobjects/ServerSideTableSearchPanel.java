/*
 * ServerSideTableSearchPanel.java
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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import workbench.db.search.ServerSideTableSearcher;
import workbench.db.search.TableDataSearcher;
import workbench.gui.components.TextComponentMouseListener;
import workbench.interfaces.PropertyStorage;
import workbench.resource.ResourceMgr;

/**
 *
 * @author Thomas Kellerer
 */
public class ServerSideTableSearchPanel
	extends JPanel
	implements TableSearchCriteriaGUI
{
	private JTextField columnFunction;
	private JTextField searchText;
	private ServerSideTableSearcher searcher;

	public ServerSideTableSearchPanel()
	{
		super(new GridBagLayout());
		searcher = new ServerSideTableSearcher();
		initComponents();
	}

	private void initComponents()
	{
    searchText = new JTextField();
    searchText.setMinimumSize(new java.awt.Dimension(100, 20));
		searchText.addMouseListener(new TextComponentMouseListener());
		String tip = ResourceMgr.getDescription("LblSearchTableSqlCriteria");
		searchText.setToolTipText(tip);

		columnFunction = new JTextField();
		columnFunction.addMouseListener(new TextComponentMouseListener());
    JLabel likeLabel = new JLabel();

    columnFunction.setColumns(8);
    GridBagConstraints gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.EAST;
    gridBagConstraints.insets = new Insets(0, 0, 0, 2);
    add(columnFunction, gridBagConstraints);

    likeLabel.setText("LIKE");
    likeLabel.setToolTipText(tip);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new Insets(0, 2, 0, 2);
    add(likeLabel, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1.0;
		gridBagConstraints.insets = new Insets(0, 2, 0, 5);
    add(searchText, gridBagConstraints);
	}

	@Override
	public void disableControls()
	{
		columnFunction.setEnabled(false);
		searchText.setEnabled(false);
	}

	@Override
	public void enableControls()
	{
		columnFunction.setEnabled(true);
		searchText.setEnabled(true);
	}

	@Override
	public TableDataSearcher getSearcher()
	{
		searcher.setCriteria(searchText.getText(), false);
		searcher.setColumnFunction(columnFunction.getText());
		return searcher;
	}

	@Override
	public void saveSettings(String prefix, PropertyStorage props)
	{
		props.setProperty(prefix + ".criteria", this.searchText.getText());
		props.setProperty(prefix + ".column-function", this.columnFunction.getText());
	}

	@Override
	public void restoreSettings(String prefix, PropertyStorage props)
	{
		this.searchText.setText(props.getProperty(prefix + ".criteria", ""));
		this.columnFunction.setText(props.getProperty(prefix + ".column-function", "$col$"));
	}

	@Override
	public void addKeyListenerForCriteria(KeyListener listener)
	{
		searchText.addKeyListener(listener);
	}

}
