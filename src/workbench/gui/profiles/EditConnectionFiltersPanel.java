/*
 * EditConnectScriptsPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.profiles;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JLabel;
import javax.swing.JPanel;
import workbench.db.ConnectionProfile;
import workbench.db.ObjectNameFilter;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.ValidatingDialog;
import workbench.gui.components.WbTraversalPolicy;
import workbench.gui.sql.EditorPanel;
import workbench.interfaces.ValidatingComponent;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

/**
 * @author support@sql-workbench.net
 */
public class EditConnectionFiltersPanel
	extends JPanel
	implements ValidatingComponent
{
	private EditorPanel schemaFilterEditor;
	private EditorPanel catalogFilterEditor;
	private ObjectNameFilter schemaFilter;
	private ObjectNameFilter catalogFilter;

	public EditConnectionFiltersPanel(ConnectionProfile profile)
	{
		super();
		if (profile == null) throw new NullPointerException("Null profile specified!");

		this.setLayout(new GridBagLayout());
		schemaFilter = profile.getSchemaFilter();
		catalogFilter = profile.getCatalogFilter();

		JPanel p1 = new JPanel(new BorderLayout(0,5));
		JLabel l1 = new JLabel(ResourceMgr.getString("LblSchemaFilter"));
		schemaFilterEditor = EditorPanel.createTextEditor();
		showFilter(schemaFilter, schemaFilterEditor);
		Dimension d = new Dimension(200,200);
		schemaFilterEditor.setPreferredSize(d);
		p1.add(l1, BorderLayout.NORTH);
		p1.add(schemaFilterEditor, BorderLayout.CENTER);

		JPanel p2 = new JPanel(new BorderLayout(0,5));
		JLabel l2 = new JLabel(ResourceMgr.getString("LblCatalogFilter"));
		catalogFilterEditor = EditorPanel.createTextEditor();
		showFilter(catalogFilter, catalogFilterEditor);
		catalogFilterEditor.setPreferredSize(d);
		catalogFilterEditor.setCaretVisible(false);
		p2.add(catalogFilterEditor, BorderLayout.CENTER);
		p2.add(l2, BorderLayout.NORTH);

		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.fill = GridBagConstraints.BOTH;
		this.add(p1, c);

		c.gridx ++;
		c.insets = new Insets(0,20,0,0);
		this.add(p2, c);

		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 2;
		c.insets = new Insets(10, 0, 5, 0);
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.NORTH;
		JLabel l = new JLabel(ResourceMgr.getString("TxtSchemCatFilterHelp"));
		add(l, c);
		
		WbTraversalPolicy pol = new WbTraversalPolicy();
		pol.addComponent(this.schemaFilterEditor);
		pol.addComponent(this.catalogFilterEditor);
		pol.setDefaultComponent(this.schemaFilterEditor);
		this.setFocusTraversalPolicy(pol);
		this.setFocusCycleRoot(false);
	}

	protected void showFilter(ObjectNameFilter filter, EditorPanel editor)
	{
		editor.setText("");
		if (filter == null) return;
		if (filter.getFilterExpressions() == null) return;
		for (String s : filter.getFilterExpressions())
		{
			editor.appendLine(s);
		}
	}

	protected ObjectNameFilter getCatalogFilter()
	{
		int lines = catalogFilterEditor.getLineCount();
		String text = catalogFilterEditor.getText().trim();
		if (lines <= 0 || text.length() == 0)
		{
			if (catalogFilter != null) catalogFilter.clear();
			return catalogFilter;
		}
		if (catalogFilter == null)
		{
			catalogFilter = new ObjectNameFilter();
		}

		for (int i=0; i < lines; i++)
		{
			String l = catalogFilterEditor.getLineText(i);
			catalogFilter.addExpression(l);
		}
		return catalogFilter;
	}

	protected ObjectNameFilter getSchemaFilter()
	{
		int lines = schemaFilterEditor.getLineCount();
		String text = schemaFilterEditor.getText().trim();
		if (lines <= 0 || text.length() == 0)
		{
			if (schemaFilter != null) schemaFilter.clear();
			return schemaFilter;
		}

		if (schemaFilter == null)
		{
			schemaFilter = new ObjectNameFilter();
		}

		for (int i=0; i < lines; i++)
		{
			String l = schemaFilterEditor.getLineText(i);
			schemaFilter.addExpression(l);
		}
		return schemaFilter;
	}

	public static boolean editFilter(Dialog owner, ConnectionProfile profile)
	{
		EditConnectionFiltersPanel p = new EditConnectionFiltersPanel(profile);
		ValidatingDialog d = new ValidatingDialog(owner, ResourceMgr.getString("LblSchemaFilterBtn"), p);
		boolean hasSize = Settings.getInstance().restoreWindowSize(d,"workbench.gui.connectionfilter.window");
		if (!hasSize)
		{
				d.setSize(400,350);
		}
		WbSwingUtilities.center(d, owner);
		d.setVisible(true);
		Settings.getInstance().storeWindowSize(d, "workbench.gui.connectionfilter.window");
		if (d.isCancelled()) return false;

		profile.setCatalogFilter(p.getCatalogFilter());
		profile.setSchemaFilter(p.getSchemaFilter());
		return true;
	}

	public boolean validateInput()
	{
		return true;
	}

	public void componentDisplayed()
	{
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				schemaFilterEditor.requestFocusInWindow();
			}
		});
	}
}
