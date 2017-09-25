/*
 * ConnectionPropertiesEditor.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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
package workbench.gui.components;


import java.awt.BorderLayout;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import workbench.interfaces.FileActions;

import workbench.gui.actions.DeleteListEntryAction;
import workbench.gui.actions.NewListEntryAction;
import workbench.gui.renderer.RendererSetup;

import workbench.storage.DataStore;

import workbench.sql.VariablesDataStore;

import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class MapEditor
	extends JPanel
	implements FileActions
{
	private DataStore propData;
	private WbTable propTable;

  private NewListEntryAction newItem;
  private DeleteListEntryAction deleteItem;

	public MapEditor(Properties source)
  {
    super();
    initUI();

    if (source != null)
    {
      Enumeration keys = source.propertyNames();
      while (keys.hasMoreElements())
      {
        String key = (String)keys.nextElement();
        String value = source.getProperty(key);
        int row = this.propData.addRow();
        this.propData.setValue(row, 0, key);
        this.propData.setValue(row, 1, value);
      }
    }
  }

	public MapEditor(Map<String, String> source)
	{
		super();
    initUI();

		if (source != null)
		{
      for (Map.Entry<String, String> entry : source.entrySet())
			{
				int row = this.propData.addRow();
				this.propData.setValue(row, 0, entry.getKey());
				this.propData.setValue(row, 1, entry.getValue());
			}
		}
	}

  public void setEditable(boolean flag)
  {
    newItem.setEnabled(flag);
    deleteItem.setEnabled(flag);
    propTable.setReadOnly(flag == false);
  }

  public boolean isModified()
  {
    return propData.isModified();
  }
  
  private void initUI()
  {
    this.propData = createDataStore();
    this.propData.setAllowUpdates(true);
    this.propTable = new WbTable();
    this.propTable.setRendererSetup(new RendererSetup(false));

    this.propTable.setModel(new DataStoreTableModel(this.propData));
    ColumnWidthOptimizer optimizer = new ColumnWidthOptimizer(this.propTable);
    optimizer.optimizeAllColWidth(75, 200, true);

    this.setLayout(new BorderLayout());
    JScrollPane scroll = new JScrollPane(this.propTable);

    WbToolbar toolbar = new WbToolbar();
    toolbar.addDefaultBorder();
    newItem = new NewListEntryAction(this);
    deleteItem = new DeleteListEntryAction(this);

    toolbar.add(newItem);
    toolbar.add(deleteItem);
    this.add(toolbar, BorderLayout.NORTH);
    this.add(scroll, BorderLayout.CENTER);
  }

  protected DataStore createDataStore()
  {
    return new VariablesDataStore();
  }

	public Properties getProperties()
  {
    Properties props = new Properties();
    props.putAll(getMap());
    return props;
  }

	public Map<String, String> getMap()
	{
		Map<String, String> props = new HashMap<>();
		this.propTable.stopEditing();
		int count = this.propData.getRowCount();
		for (int row=0; row < count; row++)
		{
			String key = this.propData.getValueAsString(row, 0);
			if (StringUtil.isEmptyString(key)) continue;
			String value = this.propData.getValueAsString(row, 1);
			props.put(key.trim(), (value == null ? "" : value.trim()));
		}
		return props;
	}

	@Override
	public void deleteItem()
		throws Exception
	{
		this.propTable.deleteRow();
	}

	@Override
	public void newItem(boolean copyCurrent)
		throws Exception
	{
		this.propTable.addRow();
		this.propTable.getSelectionModel().clearSelection();
	}

	@Override
	public void saveItem()
		throws Exception
	{
	}

  public void optimizeColumnWidths()
  {
    ColumnWidthOptimizer optimizer = new ColumnWidthOptimizer(propTable);
    optimizer.optimizeAllColWidth(true);
  }
}
