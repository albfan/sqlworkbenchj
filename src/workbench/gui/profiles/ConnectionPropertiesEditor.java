/*
 * ConnectionPropertiesEditor.java
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
import java.awt.Dimension;
import java.awt.Window;
import java.sql.Types;
import java.util.Enumeration;
import java.util.Properties;
import javax.swing.JCheckBox;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import workbench.db.ConnectionProfile;
import workbench.gui.actions.DeleteListEntryAction;
import workbench.gui.actions.NewListEntryAction;
import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.ColumnWidthOptimizer;
import workbench.gui.components.ValidatingDialog;
import workbench.gui.components.WbTable;
import workbench.gui.components.WbToolbar;
import workbench.gui.renderer.RendererSetup;
import workbench.interfaces.FileActions;
import workbench.resource.ResourceMgr;
import workbench.storage.DataStore;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class ConnectionPropertiesEditor
	extends JPanel
	implements FileActions
{
	private DataStore propData;
	private WbTable propTable;

	private JCheckBox copyProps;

	public ConnectionPropertiesEditor(ConnectionProfile profile)
	{
		super();
		String[] cols = new String[] { ResourceMgr.getString("TxtConnDataPropName"), ResourceMgr.getString("TxtConnDataPropValue") };
		int[] types = new int[] { Types.VARCHAR, Types.VARCHAR };
		int[] sizes = new int[] { 15, 5 };

		this.propData = new DataStore(cols, types, sizes);
		this.propData.setAllowUpdates(true);
		Properties source = profile.getConnectionProperties();

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
		this.propTable = new WbTable();
		this.propTable.setRendererSetup(new RendererSetup(false));

		this.propTable.setModel(new DataStoreTableModel(this.propData));
		ColumnWidthOptimizer optimizer = new ColumnWidthOptimizer(this.propTable);
		optimizer.optimizeAllColWidth(75, 200, true);

		this.setLayout(new BorderLayout());
		JScrollPane scroll = new JScrollPane(this.propTable);

		WbToolbar toolbar = new WbToolbar();
		toolbar.addDefaultBorder();
		NewListEntryAction newItem = new NewListEntryAction(this);
		DeleteListEntryAction deleteItem = new DeleteListEntryAction(this);

		toolbar.add(newItem);
		toolbar.add(deleteItem);
		this.add(toolbar, BorderLayout.NORTH);
		this.add(scroll, BorderLayout.CENTER);
		copyProps = new JCheckBox(ResourceMgr.getString("LblCpProps2System"));
		copyProps.setToolTipText(ResourceMgr.getDescription("LblCpProps2System"));
		this.add(copyProps, BorderLayout.SOUTH);
		this.copyProps.setSelected(profile.getCopyExtendedPropsToSystem());
	}

	public boolean getCopyToSystem()
	{
		return this.copyProps.isSelected();
	}

	public Properties getProperties()
	{
		Properties props = new Properties();
		this.propTable.stopEditing();
		int count = this.propData.getRowCount();
		for (int row=0; row < count; row++)
		{
			String key = this.propData.getValueAsString(row, 0);
			if (StringUtil.isEmptyString(key)) continue;
			String value = this.propData.getValueAsString(row, 1);
			props.setProperty(key.trim(), (value == null ? "" : value.trim()));
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

	public static void editProperties(Window parent, ConnectionProfile profile)
	{
		ConnectionPropertiesEditor editor = new ConnectionPropertiesEditor(profile);
		Dimension d = new Dimension(300, 250);
		editor.setMinimumSize(d);
		editor.setPreferredSize(d);

		boolean ok = ValidatingDialog.showConfirmDialog(parent, editor, ResourceMgr.getString("TxtEditConnPropsWindowTitle"));
		if (ok)
		{
			profile.setConnectionProperties(editor.getProperties());
			profile.setCopyExtendedPropsToSystem(editor.getCopyToSystem());
		}
	}
}
