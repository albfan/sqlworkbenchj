/*
 * ConnectionPropertiesEditor.java
 *
 * Created on July 6, 2003, 1:10 PM
 */

package workbench.gui.profiles;

import java.awt.BorderLayout;
import java.sql.Types;
import java.util.Enumeration;
import java.util.Properties;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import workbench.gui.actions.DeleteListEntryAction;
import workbench.gui.actions.NewListEntryAction;
import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.WbTable;
import workbench.gui.components.WbToolbar;
import workbench.interfaces.FileActions;
import workbench.resource.ResourceMgr;
import workbench.storage.DataStore;

/**
 *
 * @author  thomas
 */
public class ConnectionPropertiesEditor
	extends JPanel
	implements FileActions
{
	private DataStore propData;
	private WbTable propTable;

	private NewListEntryAction newItem;
	private DeleteListEntryAction deleteItem;

	public ConnectionPropertiesEditor(Properties source)
	{
		String[] cols = new String[] { ResourceMgr.getString("TxtConnDataPropName"), ResourceMgr.getString("TxtConnDataPropValue") };
		int[] types = new int[] { Types.VARCHAR, Types.VARCHAR };
		int[] sizes = new int[] { 15, 5 };

		this.propData = new DataStore(cols, types, sizes);
		this.propData.setAllowUpdates(true);
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

		this.propTable.setModel(new DataStoreTableModel(this.propData));
		this.propTable.optimizeAllColWidth(100, true);

		this.setLayout(new BorderLayout());
		JScrollPane scroll = new JScrollPane(this.propTable);

		WbToolbar toolbar = new WbToolbar();
		toolbar.addDefaultBorder();
		this.newItem = new NewListEntryAction(this);
		this.deleteItem = new DeleteListEntryAction(this);

		toolbar.add(this.newItem);
		toolbar.add(this.deleteItem);
		this.add(toolbar, BorderLayout.NORTH);
		this.add(scroll, BorderLayout.CENTER);
	}

	public Properties getProperties()
	{
		Properties props = new Properties();
		this.propTable.stopEditing();
		int count = this.propData.getRowCount();
		for (int row=0; row < count; row++)
		{
			String key = this.propData.getValueAsString(row, 0);
			String value = this.propData.getValueAsString(row, 1);
			props.setProperty(key, value);
		}
		return props;
	}

	public void deleteItem()
		throws Exception
	{
		this.propTable.deleteRow();
	}

	public void newItem(boolean copyCurrent)
		throws Exception
	{
		this.propTable.addRow();
		this.propTable.getSelectionModel().clearSelection();
	}

	public void saveItem()
		throws Exception
	{
	}

}