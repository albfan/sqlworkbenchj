/*
 * ProfileEditor.java
 *
 * Created on 1. Juli 2002, 18:34
 */

package workbench.gui.profiles;

import java.awt.BorderLayout;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JToolBar;
import javax.swing.ListModel;
import javax.swing.border.Border;
import workbench.WbManager;
import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.DbDriver;
import workbench.exception.WbException;
import workbench.gui.components.WbSplitPane;
import workbench.gui.components.WbToolbar;
import workbench.gui.components.WbToolbarButton;
import workbench.interfaces.FileActions;
import workbench.resource.ResourceMgr;

public class DriverlistEditorPanel extends javax.swing.JPanel implements FileActions
{
	private DriverListModel model;
	private JToolBar toolbar;
	private int lastIndex = -1;

	private workbench.gui.profiles.DriverEditorPanel driverEditor;
	private javax.swing.JScrollPane jScrollPane1;
	private javax.swing.JSplitPane jSplitPane1;
	private javax.swing.JList driverList;
	private javax.swing.JPanel listPanel;


	/** Creates new form ProfileEditor */
	public DriverlistEditorPanel()
	{
		initComponents();
		this.fillDriverList();
		driverList.setNextFocusableComponent(driverEditor);
		this.driverEditor.setNextFocusableComponent(driverList);
		this.toolbar = new WbToolbar();
		this.toolbar.add(new NewProfileAction(this));
		this.toolbar.add(new DeleteProfileAction(this));
		this.listPanel.add(this.toolbar, BorderLayout.NORTH);
	}

	private void fillDriverList()
	{
		this.model = new DriverListModel(WbManager.getInstance().getConnectionMgr().getDrivers());
		this.driverList.setModel(this.model);
		if (this.model.getSize() > 0)
		{
			this.driverList.setSelectedIndex(0);
		}
	}

	private void initComponents()
	{
		jSplitPane1 = new WbSplitPane();

		listPanel = new javax.swing.JPanel();
		jScrollPane1 = new javax.swing.JScrollPane();
		driverList = new javax.swing.JList();
		driverEditor = new workbench.gui.profiles.DriverEditorPanel();

		setLayout(new java.awt.BorderLayout());

		jSplitPane1.setDividerLocation(100);
		listPanel.setLayout(new java.awt.BorderLayout());

		driverList.addListSelectionListener(new javax.swing.event.ListSelectionListener()
		{
			public void valueChanged(javax.swing.event.ListSelectionEvent evt)
			{
				driverListValueChanged(evt);
			}
		});

		jScrollPane1.setViewportView(driverList);

		listPanel.add(jScrollPane1, java.awt.BorderLayout.CENTER);

		jSplitPane1.setLeftComponent(listPanel);
		jSplitPane1.setRightComponent(driverEditor);

		add(jSplitPane1, java.awt.BorderLayout.CENTER);

	}

	private void driverListValueChanged(javax.swing.event.ListSelectionEvent evt)
	{
		if (evt.getSource() == this.driverList)
		{
			if (lastIndex > -1)
			{
				DbDriver current = this.driverEditor.getDriver();
				this.model.putDriver(lastIndex, current);
			}
			int index = this.driverList.getSelectedIndex();
			DbDriver newDriver = this.model.getDriver(index);
			this.driverEditor.setDriver(newDriver);
			lastIndex = index;
		}
	}

	public DbDriver getSelectedDriver()
	{
		this.updateUI();
		this.driverEditor.updateDriver();
		int index = driverList.getSelectedIndex();
		DbDriver drv = this.model.getDriver(index);
		return drv;
	}

	private void selectDriver(String aDriverName)
	{
		if (aDriverName == null) return;

		try
		{
			int count = this.model.getSize();

			for (int i=0; i < count; i++)
			{
				DbDriver drv = (DbDriver)this.model.getDriver(i);
				if (drv.getName().equals(aDriverName))
				{
					this.driverList.setSelectedIndex(i);
					break;
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			driverList.setSelectedIndex(0);
		}
	}

	/**
	 *	Remove an item from the listmodel
	 */
	public void deleteItem() throws WbException
	{
		int index = this.driverList.getSelectedIndex();
		if (index > 0) this.driverList.setSelectedIndex(index - 1);
		this.model.deleteDriver(index);
		this.driverList.updateUI();
	}

	/**
	 *	Create a new profile. This will only be
	 *	created in the ListModel.
	 */
	public void newItem() throws WbException
	{
		DbDriver drv = new DbDriver();
		drv.setName(ResourceMgr.getString("TxtEmptyDriverName"));
		this.model.addDriver(drv);
		this.selectDriver(drv.getName());
		this.driverList.updateUI();
	}

	public void saveItem() throws WbException
	{
		ConnectionMgr conn = WbManager.getInstance().getConnectionMgr();
		conn.setDrivers(this.model.getValues());
		conn.saveDrivers();
	}

}
