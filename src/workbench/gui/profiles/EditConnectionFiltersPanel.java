/*
 * EditConnectionFiltersPanel.java
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
package workbench.gui.profiles;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import workbench.interfaces.ValidatingComponent;
import workbench.resource.IconMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.ConnectionProfile;
import workbench.db.ObjectNameFilter;

import workbench.gui.WbSwingUtilities;
import workbench.gui.components.ValidatingDialog;
import workbench.gui.components.WbTraversalPolicy;
import workbench.gui.sql.EditorPanel;

import workbench.util.StringUtil;

/**
 * @author Thomas Kellerer
 */
public class EditConnectionFiltersPanel
	extends JPanel
  implements ValidatingComponent, ActionListener
{
	private EditorPanel schemaFilterEditor;
	private EditorPanel catalogFilterEditor;
	private JCheckBox catalogInclusionFlag;
	private JCheckBox schemaInclusionFlag;
	private ObjectNameFilter schemaFilter;
	private ObjectNameFilter catalogFilter;
  private JComboBox catalogTemplates;
	private JComboBox schemaTemplates;
  private JButton addSchemaTemplate;
  private JButton addCatalogTemplate;
  private JButton editSchemaTemplates;
  private JButton editCatalogTemplates;

	public EditConnectionFiltersPanel(ConnectionProfile profile)
	{
		super();
		if (profile == null) throw new NullPointerException("Null profile specified!");

		this.setLayout(new GridBagLayout());

		JPanel p1 = new JPanel(new BorderLayout(0,5));
		JLabel l1 = new JLabel(ResourceMgr.getString("LblSchemaFilter"));
    JPanel schemaHeader = new JPanel(new BorderLayout());
    schemaHeader.add(l1, BorderLayout.PAGE_START);
    schemaTemplates = new JComboBox();
    ObjectFilterTemplateStorage schemas = new ObjectFilterTemplateStorage(TemplateType.schema);
    schemaTemplates.setModel(schemas);
    schemaTemplates.addActionListener(this);
    schemaHeader.add(schemaTemplates, BorderLayout.CENTER);

    JPanel buttonPanel1 = new JPanel(new BorderLayout());
    addSchemaTemplate = new JButton();
    addSchemaTemplate.setIcon(IconMgr.getInstance().getLabelIcon("add"));
    addSchemaTemplate.addActionListener(this);
    int iconSize = IconMgr.getInstance().getSizeForLabel();
    WbSwingUtilities.adjustButtonWidth(addSchemaTemplate, iconSize + 6, iconSize + 6);
    buttonPanel1.add(addSchemaTemplate, BorderLayout.LINE_START);
    editSchemaTemplates = new JButton();
    editSchemaTemplates.setIcon(IconMgr.getInstance().getLabelIcon("edit"));
    editSchemaTemplates.addActionListener(this);
    WbSwingUtilities.adjustButtonWidth(editSchemaTemplates, iconSize + 6, iconSize + 6);
    buttonPanel1.add(editSchemaTemplates, BorderLayout.LINE_END);
    schemaHeader.add(buttonPanel1, BorderLayout.LINE_END);

		schemaFilterEditor = EditorPanel.createTextEditor();
		schemaInclusionFlag = new JCheckBox(ResourceMgr.getString("LblInclFilter"));
		schemaInclusionFlag.setToolTipText(ResourceMgr.getDescription("LblInclFilter"));
		p1.add(schemaInclusionFlag, BorderLayout.SOUTH);
		p1.add(schemaHeader, BorderLayout.NORTH);
		p1.add(schemaFilterEditor, BorderLayout.CENTER);

		showSchemaFilter(profile.getSchemaFilter());

		JPanel p2 = new JPanel(new BorderLayout(0,5));
		JLabel l2 = new JLabel(ResourceMgr.getString("LblCatalogFilter"));
    JPanel catalogHeader = new JPanel(new BorderLayout());
    catalogHeader.add(l2, BorderLayout.PAGE_START);
    catalogTemplates = new JComboBox();
    ObjectFilterTemplateStorage catalogs = new ObjectFilterTemplateStorage(TemplateType.catalog);
    catalogTemplates.setModel(catalogs);

    JPanel buttonPanel2 = new JPanel(new BorderLayout());
    addCatalogTemplate = new JButton();
    addCatalogTemplate.setIcon(IconMgr.getInstance().getLabelIcon("add"));
    addCatalogTemplate.addActionListener(this);
    WbSwingUtilities.adjustButtonWidth(addCatalogTemplate, iconSize + 6, iconSize + 6);
    buttonPanel2.add(addCatalogTemplate, BorderLayout.LINE_START);

    editCatalogTemplates = new JButton();
    editCatalogTemplates.setIcon(IconMgr.getInstance().getLabelIcon("edit"));
    WbSwingUtilities.adjustButtonWidth(editCatalogTemplates, iconSize + 6, iconSize + 6);
    buttonPanel2.add(editCatalogTemplates, BorderLayout.LINE_END);

    catalogHeader.add(catalogTemplates, BorderLayout.CENTER);
    catalogHeader.add(buttonPanel2, BorderLayout.LINE_END);

		catalogFilterEditor = EditorPanel.createTextEditor();
		catalogFilterEditor.setCaretVisible(false);
		catalogInclusionFlag = new JCheckBox(ResourceMgr.getString("LblInclFilter"));
		catalogInclusionFlag.setToolTipText(ResourceMgr.getDescription("LblInclFilter"));
		catalogInclusionFlag.setSelected(catalogFilter != null ? catalogFilter.isInclusionFilter() : false);

		p2.add(catalogHeader, BorderLayout.NORTH);
		p2.add(catalogFilterEditor, BorderLayout.CENTER);
		p2.add(catalogInclusionFlag, BorderLayout.SOUTH);

		showCatalogFilter(profile.getCatalogFilter());

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
		pol.addComponent(this.schemaInclusionFlag);
		pol.addComponent(this.catalogInclusionFlag);
		pol.setDefaultComponent(this.schemaFilterEditor);
		this.setFocusTraversalPolicy(pol);
		this.setFocusCycleRoot(false);
	}

  private void showSchemaFilter(ObjectNameFilter filter)
  {
    showFilter(filter, schemaFilterEditor);
    schemaInclusionFlag.setSelected(filter != null ? filter.isInclusionFilter() : false);
		schemaFilter = filter;
  }

  private void showCatalogFilter(ObjectNameFilter filter)
  {
    showFilter(filter, catalogFilterEditor);
    catalogInclusionFlag.setSelected(filter != null ? filter.isInclusionFilter() : false);
    catalogFilter = filter;
  }

	private void showFilter(ObjectNameFilter filter, EditorPanel editor)
	{
		editor.setText("");
		if (filter == null) return;
		if (filter.getFilterExpressions() == null) return;
		for (String s : filter.getFilterExpressions())
		{
			editor.appendLine(s + "\n");
		}
	}

	protected ObjectNameFilter getCatalogFilter()
	{
		int lines = catalogFilterEditor.getLineCount();
		String text = catalogFilterEditor.getText().trim();

		if (lines <= 0 || text.isEmpty())
		{
			return null;
		}

		if (catalogFilter == null)
		{
			catalogFilter = new ObjectNameFilter();
		}
		else
		{
			catalogFilter.removeExpressions();
		}

		catalogFilter.setInclusionFilter(catalogInclusionFlag.isSelected());

		for (int i=0; i < lines; i++)
		{
			String line = catalogFilterEditor.getLineText(i);
			catalogFilter.addExpression(convertSQLExpression(line));
		}

		return catalogFilter;
	}

	protected ObjectNameFilter getSchemaFilter()
	{
		int lines = schemaFilterEditor.getLineCount();
		String text = schemaFilterEditor.getText().trim();

		if (lines <= 0 || text.isEmpty())
		{
			return null;
		}

		if (schemaFilter == null)
		{
			schemaFilter = new ObjectNameFilter();
		}
		else
		{
			schemaFilter.removeExpressions();
		}
		schemaFilter.setInclusionFilter(schemaInclusionFlag.isSelected());

		for (int i=0; i < lines; i++)
		{
			String line = schemaFilterEditor.getLineText(i);
			schemaFilter.addExpression(convertSQLExpression(line));
		}
		return schemaFilter;
	}

	private String convertSQLExpression(String input)
	{
		if (input.endsWith("%"))
		{
			input = input.substring(0, input.length() - 1) + ".*";
			if (!input.startsWith("^"))
			{
				input = "^" + input;
			}
		}
		return input;
  }

  @Override
  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == schemaTemplates)
    {
      applySchemaTemplate();
    }
    if (e.getSource() == catalogTemplates)
    {
      applyCatalogTemplate();
    }
    if (e.getSource() == addSchemaTemplate)
    {
      addSchemaTemplate();
    }
    if (e.getSource() == addCatalogTemplate)
    {
      addCatalogTemplate();
    }
    if (e.getSource() == editSchemaTemplates)
    {
      editTemplates((ObjectFilterTemplateStorage)schemaTemplates.getModel());
    }
    if (e.getSource() == editCatalogTemplates)
    {
      editTemplates((ObjectFilterTemplateStorage)catalogTemplates.getModel());
    }
  }

  private void editTemplates(ObjectFilterTemplateStorage model)
  {
    List<ObjectFilterTemplate> templates = model.getTemplates();
    TemplateListEditor editor = new TemplateListEditor();
    editor.setTemplates(templates);
    boolean ok = WbSwingUtilities.getOKCancel("Manage templates", this, editor);
    if (ok)
    {
      model.setTemplates(editor.getTemplates());
    }
  }

  private void applySchemaTemplate()
  {
    ObjectFilterTemplateStorage model = (ObjectFilterTemplateStorage)schemaTemplates.getModel();
    ObjectFilterTemplate template = model.getSelectedItem();
    if (template == null) return;

    showSchemaFilter(template.getFilter().createCopy());
  }

  private void applyCatalogTemplate()
  {
    ObjectFilterTemplateStorage model = (ObjectFilterTemplateStorage)catalogTemplates.getModel();
    ObjectFilterTemplate template = model.getSelectedItem();
    if (template == null) return;

    showCatalogFilter(template.getFilter().createCopy());
  }

  private void addSchemaTemplate()
  {
    ObjectNameFilter filter = getSchemaFilter();
    if (filter == null) return;

    ObjectFilterTemplateStorage model = (ObjectFilterTemplateStorage)schemaTemplates.getModel();
    String name = WbSwingUtilities.getUserInput(this, "Enter a name", "Schema Filter Template");
    if (StringUtil.isNonBlank(name))
    {
      model.addTemplate(name, filter.createCopy());
    }
  }

  private void addCatalogTemplate()
  {
    ObjectNameFilter filter = getCatalogFilter();
    if (filter == null) return;

    ObjectFilterTemplateStorage model = (ObjectFilterTemplateStorage)schemaTemplates.getModel();
    String name = WbSwingUtilities.getUserInput(this, "Enter a name", "Catalog Filter Template");
    if (StringUtil.isNonBlank(name))
    {
      model.addTemplate(name, filter.createCopy());
    }
  }

  private void saveTemplates()
  {
    ObjectFilterTemplateStorage model = (ObjectFilterTemplateStorage)schemaTemplates.getModel();
    model.saveTemplates();
    model = (ObjectFilterTemplateStorage)catalogTemplates.getModel();
    model.saveTemplates();
  }

	public static boolean editFilter(Dialog owner, ConnectionProfile profile)
	{
		final EditConnectionFiltersPanel p = new EditConnectionFiltersPanel(profile);
		ValidatingDialog d = new ValidatingDialog(owner, ResourceMgr.getString("LblSchemaFilterBtn"), p);
    p.catalogFilterEditor.addKeyBinding(d.getESCAction());
    p.schemaFilterEditor.addKeyBinding(d.getESCAction());
		boolean hasSize = Settings.getInstance().restoreWindowSize(d, "workbench.gui.connectionfilter.window");
		if (!hasSize)
		{
			d.setSize(400, 450);
		}
		WbSwingUtilities.center(d, owner);
		d.setVisible(true);
		Settings.getInstance().storeWindowSize(d, "workbench.gui.connectionfilter.window");
    p.saveTemplates();

		if (d.isCancelled())
		{
			return false;
		}

		profile.setCatalogFilter(p.getCatalogFilter());
		profile.setSchemaFilter(p.getSchemaFilter());
		return true;
	}

	@Override
	public boolean validateInput()
	{
		return true;
	}

  @Override
  public void componentWillBeClosed()
  {
		// nothing to do
  }

	@Override
	public void componentDisplayed()
	{
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				schemaFilterEditor.requestFocusInWindow();
			}
		});
	}
}
