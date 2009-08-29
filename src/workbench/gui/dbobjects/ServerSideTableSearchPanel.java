/*
 * ServerSideTableSearchPanel
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
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

	public void disableControls()
	{
		columnFunction.setEnabled(false);
		searchText.setEnabled(false);
	}

	public void enableControls()
	{
		columnFunction.setEnabled(true);
		searchText.setEnabled(true);
	}

	public TableDataSearcher getSearcher()
	{
		searcher.setCriteria(searchText.getText(), false);
		searcher.setColumnFunction(columnFunction.getText());
		return searcher;
	}

	public void saveSettings(String prefix, PropertyStorage props)
	{
		props.setProperty(prefix + ".criteria", this.searchText.getText());
		props.setProperty(prefix + ".column-function", this.columnFunction.getText());
	}

	public void restoreSettings(String prefix, PropertyStorage props)
	{
		this.searchText.setText(props.getProperty(prefix + ".criteria", ""));
		this.columnFunction.setText(props.getProperty(prefix + ".column-function", "$col$"));
	}

	public void addKeyListenerForCriteria(KeyListener listener)
	{
		searchText.addKeyListener(listener);
	}

}
