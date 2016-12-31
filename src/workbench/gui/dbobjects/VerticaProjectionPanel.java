/*
 * VerticaProjectionPanel.java
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
package workbench.gui.dbobjects;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import workbench.interfaces.PropertyStorage;
import workbench.interfaces.Reloadable;
import workbench.interfaces.Resettable;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.vertica.VerticaProjectionReader;

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.ReloadAction;
import workbench.gui.actions.WbAction;
import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.WbScrollPane;
import workbench.gui.components.WbSplitPane;
import workbench.gui.components.WbTabbedPane;
import workbench.gui.components.WbTable;
import workbench.gui.components.WbToolbar;
import workbench.gui.renderer.RendererSetup;
import workbench.gui.settings.PlacementChooser;

import workbench.storage.DataStore;

import workbench.util.FilteredProperties;
import workbench.util.WbThread;
import workbench.util.WbWorkspace;

/**
 * A panel with projection information for Vertica
 *
 * @author Tatiana Saltykova
 */
public class VerticaProjectionPanel
	extends JPanel
	implements Resettable, Reloadable, ActionListener, ListSelectionListener, ChangeListener
{
	protected WbTable projectionList;
	protected WbTable projectionBuddies;
	protected WbTable projectionColumns;
	private WbSplitPane splitPanel;
	private JTabbedPane displayTab;

	private ReloadAction reloadProjections;

	private boolean isRetrieving;
	private boolean isDetailsRetrieving;
	private boolean initialized;

	private TableIdentifier currentTable;
	private VerticaProjectionReader projectionReader;
	private FilteredProperties workspaceProperties;

	public VerticaProjectionPanel()
	{
		super(new BorderLayout());
		projectionReader = new VerticaProjectionReader();
	}

	private void initGui()
	{
		if (initialized) return;

		WbSwingUtilities.invoke(this::_initGui);
	}

	private void _initGui()
	{
		this.projectionList = new WbTable();
		this.projectionList.setAdjustToColumnLabel(false);
		this.projectionList.setRendererSetup(RendererSetup.getBaseSetup());
		this.projectionList.getSelectionModel().addListSelectionListener(this);
		this.projectionList.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		WbScrollPane scrollList = new WbScrollPane(this.projectionList);

		this.projectionBuddies = new WbTable();
		this.projectionBuddies.setAdjustToColumnLabel(false);
		this.projectionBuddies.setRendererSetup(RendererSetup.getBaseSetup());
		this.projectionBuddies.getSelectionModel().addListSelectionListener(this);
		this.projectionBuddies.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		WbScrollPane scrollDetail = new WbScrollPane(this.projectionBuddies);

		this.projectionColumns = new WbTable();
		this.projectionColumns.setAdjustToColumnLabel(false);
		this.projectionColumns.setRendererSetup(RendererSetup.getBaseSetup());
		WbScrollPane scrollColumns = new WbScrollPane(this.projectionColumns);

		int location = PlacementChooser.getPlacementLocation();
		displayTab = new WbTabbedPane(location);
		displayTab.setBorder(WbSwingUtilities.EMPTY_BORDER);
		displayTab.setName("displaytab");
		displayTab.add("Buddies", scrollDetail);
		displayTab.add(ResourceMgr.getString("TxtDbExplorerTableDefinition"), scrollColumns);
		displayTab.addChangeListener(this);

		this.splitPanel = new WbSplitPane(JSplitPane.VERTICAL_SPLIT);
		this.splitPanel.setDividerSize(8);
		this.splitPanel.setTopComponent(scrollList);
		this.splitPanel.setBottomComponent(displayTab);

		WbToolbar toolbar = new WbToolbar();
		toolbar.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
		reloadProjections = new ReloadAction(this);
		toolbar.add(reloadProjections);
		toolbar.addSeparator();

		this.add(toolbar, BorderLayout.NORTH);
		this.add(splitPanel, BorderLayout.CENTER);

		this.splitPanel.setDividerLocation(0.5d);

		initialized = true;

		restoreSettings();
		if (workspaceProperties != null)
		{
			readSettings(workspaceProperties, workspaceProperties.getFilterPrefix());
			workspaceProperties = null;
		}
	}

	public void setConnection(WbConnection conn)
	{
		reset();
		projectionReader.setConnection(conn);
	}

	@Override
	public void reset()
	{
		if (initialized)
		{
			projectionList.reset();
			projectionBuddies.reset();
			projectionColumns.reset();
		}
	}

	public boolean isRetrieving()
	{
		return isRetrieving || isDetailsRetrieving;
	}

	public void reloadTable()
	{
		try
		{
			reset();
			retrieve(currentTable);
		}
		catch (SQLException sql)
		{
			LogMgr.logError("VerticaProjectionPanel.reloadTable()", "Could not reload projections", sql);
		}
	}

	protected void retrieve(TableIdentifier table)
		throws SQLException
	{
		initGui();
		try
		{
			currentTable = table;
			isRetrieving = true;

			final DataStoreTableModel model = new DataStoreTableModel(getProjectionDataStore(table));
			WbSwingUtilities.invoke(() ->
      {
        projectionList.setModel(model, true);
        projectionList.adjustRowsAndColumns();
        projectionBuddies.reset();
        projectionColumns.reset();
      });
		}
		finally
		{
			isRetrieving = false;
		}
	}

	private DataStore getProjectionDataStore(TableIdentifier table)
		throws SQLException
	{
		return projectionReader.getProjectionList(table);
	}

	private DataStore getProjectionCopiesDataStore(String basename)
		throws SQLException
	{
		return projectionReader.getProjectionCopies(basename);
	}

	private DataStore getProjectionColumnsDataStore(String projectionName)
		throws SQLException
	{
		return projectionReader.getProjectionColumns(projectionName);
	}

	public void dispose()
	{
		reset();
		WbAction.dispose(reloadProjections);
	}

	protected void retrieveProjectionCopies()
	{
		try
		{
			int selected = Math.max(0, projectionList.getSelectedRow());
			String basename = projectionList.getValueAsString(selected, 0);

			final DataStoreTableModel model = new DataStoreTableModel(getProjectionCopiesDataStore(basename));
			WbSwingUtilities.invoke(() ->
      {
        projectionBuddies.setModel(model, true);
        projectionBuddies.adjustRowsAndColumns();
      });
		}
		catch (SQLException se)
		{
			LogMgr.logError("VerticaProjectionPanel.retrieveProjectionCopies()", "Could not retrieve projection copies", se);
		}
	}

	protected void retrieveProjectionColumns()
	{
		try
		{
			int selected = Math.max(0, projectionList.getSelectedRow());
			String basename = projectionList.getValueAsString(selected, 0);

			final DataStoreTableModel model = new DataStoreTableModel(getProjectionColumnsDataStore(basename));
			WbSwingUtilities.invoke(() ->
      {
        projectionColumns.setModel(model, true);
        projectionColumns.adjustRowsAndColumns();
      });
		}
		catch (SQLException se)
		{
			LogMgr.logError("VerticaProjectionPanel.retrieveProjectionColumns()", "Could not retrieve projection columns", se);
		}
	}

	protected void retrieveCurrentPanel()
	{
		if (projectionList.getSelectedRowCount() < 1)
		{
			return;
		}

		switch (displayTab.getSelectedIndex())
		{
			case 0:
				if (projectionBuddies.getRowCount() < 1)
				{
					retrieveProjectionCopies();
				}
				break;
			case 1:
				if (projectionColumns.getRowCount() < 1)
				{
					retrieveProjectionColumns();
				}
				break;
		}
	}

	@Override
	public void reload()
	{
		reloadTable();
	}

	public void reloadDetails()
	{
		WbThread t = new WbThread("ProjectionDetailsRetriever")
		{
			@Override
			public void run()
			{
				try
				{
					isDetailsRetrieving = true;
					retrieveCurrentPanel();
				}
				finally
				{
					isDetailsRetrieving = false;
					reloadProjections.setEnabled(true);
					WbSwingUtilities.showDefaultCursor(VerticaProjectionPanel.this);
				}
			}

		};
		reloadProjections.setEnabled(false);
		WbSwingUtilities.showWaitCursor(this);
		t.start();
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (!initialized) return;
		if (e.getSource() != this.reloadProjections) return;

		LogMgr.logDebug("VerticaProjectionPanel.actionPerformed()", "Trying to select projection copies");
		WbSwingUtilities.invokeLater(this::reload);
	}

	/**
	 * Invoked when a projection is selected in the projection list
	 */
	@Override
	public void valueChanged(ListSelectionEvent e)
	{
		if (!initialized) return;

		if (e.getValueIsAdjusting()) return;
		if (e.getSource() == this.projectionList.getSelectionModel())
		{
			projectionBuddies.reset();
			projectionColumns.reset();
			LogMgr.logDebug("VerticaProjectionPanel.actionPerformed()", "Trying to select projection copies");
			WbSwingUtilities.invokeLater(this::reloadDetails);
		}
	}

	/** Invoked when the displayed tab has changed.
	 * Retrieve table detail information here.
	 */
	@Override
	public void stateChanged(ChangeEvent e)
	{
		if (!initialized) return;

		if (e.getSource() == this.displayTab)
		{
			EventQueue.invokeLater(this::reloadDetails);
		}
	}

	private String getWorkspacePrefix(int index)
	{
		return "dbexplorer" + index + ".projections.";
	}

	public void saveSettings()
	{
		if (initialized)
		{
			storeSettings(Settings.getInstance(), this.getClass().getName() + ".");
		}
	}

	public void saveToWorkspace(WbWorkspace w, int index)
	{
		String prefix = getWorkspacePrefix(index);
		if (initialized)
		{
			storeSettings(w.getSettings(), prefix);
		}
		else if (workspaceProperties != null)
		{
			workspaceProperties.copyTo(w.getSettings(), prefix);
		}
	}

	private void storeSettings(PropertyStorage props, String prefix)
	{
		props.setProperty(prefix + "divider", splitPanel.getDividerLocation());
	}

	public void restoreSettings()
	{
		if (initialized)
		{
			readSettings(Settings.getInstance(), this.getClass().getName() + ".");
		}
	}

	public void readFromWorkspace(WbWorkspace w, int index)
	{
		String prefix = getWorkspacePrefix(index);
		if (initialized)
		{
			readSettings(w.getSettings(), prefix);
		}
		else
		{
			workspaceProperties = new FilteredProperties(w.getSettings(), prefix);
		}
	}

	private void readSettings(PropertyStorage props, String prefix)
	{
		if (initialized)
		{
			int loc = props.getIntProperty(prefix + "divider", 200);
			splitPanel.setDividerLocation(loc);
		}
	}
}
