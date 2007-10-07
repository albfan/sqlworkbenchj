/*
 * DbExplorerPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.dbobjects;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;

import workbench.db.WbConnection;
import workbench.interfaces.DbExecutionListener;
import workbench.util.ExceptionUtil;
import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.WbAction;
import workbench.gui.components.ConnectionInfo;
import workbench.gui.components.ConnectionSelector;
import workbench.gui.components.WbTabbedPane;
import workbench.gui.components.WbToolbar;
import workbench.interfaces.MainPanel;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.util.NumberStringCache;
import workbench.util.StringUtil;
import workbench.util.WbProperties;
import workbench.util.WbThread;
import workbench.util.WbWorkspace;


/**
 * The main container panel for the DbExplorer. 
 * 
 * This panel incorporates the panels to 
 * <ul>
 *	<li>Display a list of tables, views, etc {@link workbench.gui.dbobjects.TableListPanel}</li>
 *  <li>Display a list of procedures: {@link workbench.gui.dbobjects.ProcedureListPanel}</li>
 *	<li>Allow search across several table and columns: {@link workbench.gui.dbobjects.TableSearchPanel}</li>
 * </ul>
 * This panel can either be displayed inside the MainWindow as a tab, or as 
 * a separate Window (@link workbench.gui.dbobjects.DbExplorerWindow}
 * @author  support@sql-workbench.net
 */
public class DbExplorerPanel
	extends JPanel
	implements ActionListener, MainPanel, ChangeListener, DbExecutionListener
{
	private JTabbedPane tabPane;
	protected TableListPanel tables;
	protected TableSearchPanel searchPanel;
	protected ProcedureListPanel procs;
	protected TriggerListPanel triggers;
	protected JComboBox schemaSelector;
	private JComboBox catalogSelector;
	private JLabel schemaLabel;
	private JLabel catalogLabel;
	private JPanel selectorPanel;
	boolean connected;
	private WbConnection dbConnection;
	private DbExplorerWindow window;
	private WbToolbar toolbar;
	private ConnectionInfo connectionInfo;
	protected boolean retrievePending;
	private boolean schemaRetrievePending = true;
	private boolean connectionInitPending = true;
	private int internalId = 0;
	private ConnectionSelector connectionSelector;
	private JButton selectConnectionButton;
	private String tabTitle;
	private static int instanceCount = 0;
	private MainWindow mainWindow;
	private boolean busy;
	private String schemaFromWorkspace;
	private String catalogFromWorkspace;
	private boolean switchCatalog = false;
	
	public DbExplorerPanel()
	{
		this(null);
	}

	public DbExplorerPanel(MainWindow aParent)
	{
		synchronized (DbExplorerPanel.class)
		{
			instanceCount++;
			this.internalId = instanceCount;
		}
		
		this.mainWindow = aParent;
		try
		{
			tables = new TableListPanel(aParent);
			setDbExecutionListener(aParent);
			procs = new ProcedureListPanel(aParent);
			this.searchPanel = new TableSearchPanel(tables);
			tabPane = new WbTabbedPane(JTabbedPane.TOP);
			tabPane.add(ResourceMgr.getString("TxtDbExplorerTables"), tables);
			tabPane.setToolTipTextAt(0, ResourceMgr.getDescription("TxtDbExplorerTables"));
			
			String tabLocation = Settings.getInstance().getProperty("workbench.gui.dbobjects.maintabs", "top");
			int location = JTabbedPane.TOP;
			if (tabLocation.equalsIgnoreCase("bottom"))
			{
				location = JTabbedPane.BOTTOM;
			}
			else if (tabLocation.equalsIgnoreCase("left"))
			{
				location = JTabbedPane.LEFT;
			}
			else if (tabLocation.equalsIgnoreCase("right"))
			{
				location = JTabbedPane.RIGHT;
			}
			tabPane.setTabPlacement(location);
			
			int index = 1;
			
			tabPane.add(ResourceMgr.getString("TxtDbExplorerProcs"), procs);
			tabPane.setToolTipTextAt(index, ResourceMgr.getDescription("TxtDbExplorerProcs"));
			
			if (Settings.getInstance().getShowTriggerPanel())
			{
				triggers = new TriggerListPanel(aParent);
				tabPane.add(ResourceMgr.getString("TxtDbExplorerTriggers"), triggers);
				tabPane.setToolTipTextAt(index ++, ResourceMgr.getDescription("TxtDbExplorerTriggers"));
			}			
			tabPane.add(ResourceMgr.getString("TxtSearchTables"), this.searchPanel);
			tabPane.setToolTipTextAt(index ++, ResourceMgr.getDescription("TxtSearchTables"));
			tabPane.setFocusable(false);

			this.setBorder(WbSwingUtilities.EMPTY_BORDER);
			this.setLayout(new BorderLayout());
			
			this.selectorPanel = new JPanel();
			this.selectorPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));

			this.schemaLabel = new JLabel(ResourceMgr.getString("LblSchema"));

			this.selectorPanel.add(schemaLabel);
			this.schemaSelector = new JComboBox();
			Dimension d = new Dimension(80, 20);
			this.schemaSelector.setMinimumSize(d);
			this.selectorPanel.add(this.schemaSelector);

			this.catalogSelector  = new JComboBox();
			this.catalogLabel = new JLabel("Catalog");
			this.catalogSelector.setVisible(false);
			this.catalogSelector.setEnabled(false);
			this.catalogLabel.setVisible(false);
			this.selectorPanel.add(catalogLabel);
			this.selectorPanel.add(catalogSelector);
			
			this.add(this.selectorPanel, BorderLayout.NORTH);
			this.add(tabPane, BorderLayout.CENTER);
			this.searchPanel.restoreSettings();

			this.toolbar = new WbToolbar();
			this.toolbar.addDefaultBorder();
			d = new Dimension(30, 29);
			this.toolbar.setMinimumSize(d);
			this.toolbar.setPreferredSize(new Dimension(100, 29));
			this.connectionInfo = new ConnectionInfo(this.toolbar.getBackground());
			this.connectionInfo.setMinimumSize(d);
			this.toolbar.add(this.connectionInfo);
			if (mainWindow != null) mainWindow.addExecutionListener(this);
		}
		catch (Throwable e)
		{
			LogMgr.logError(this, "Could not initialize DbExplorerPanel", e);
		}
	}

	public void setDbExecutionListener(DbExecutionListener l)
	{
		if (this.tables != null)
		{
			tables.setDbExecutionListener(l);
		}
	}
	
	public void setSwitchCatalog(boolean flag)
	{
		this.switchCatalog = flag && Settings.getInstance().getSwitchCatalogInExplorer();
	}
	
	public void showConnectButton(ConnectionSelector selector)
	{
		this.connectionSelector = selector;
		this.selectConnectionButton = new JButton(ResourceMgr.getString("LblSelectConnection"));
		Border b = new CompoundBorder(new EtchedBorder(EtchedBorder.LOWERED), new EmptyBorder(1, 10, 1, 10));
		this.selectConnectionButton.setBorder(b);
		this.selectConnectionButton.addActionListener(this);
		this.selectorPanel.add(Box.createHorizontalStrut(15));
		this.selectorPanel.add(this.selectConnectionButton);
		
		
	}

	private final Object busyLock = new Object();
	private void setBusy(boolean flag)
	{
		synchronized (busyLock)
		{
			busy = flag;
		}
	}
	
	public boolean isBusy()
	{
		synchronized (busyLock)
		{
			return this.busy;
		}
	}

	public String getId()
	{
		return "WbExp-" + NumberStringCache.getNumberString(this.internalId);
	}

	private void readSchemas()
	{
		if (this.isBusy() || isConnectionBusy() || this.dbConnection == null || this.dbConnection.getMetadata() == null) 
		{
			this.schemaRetrievePending = true;
			return;
		}
		
		String schemaToSelect = null;
		try
		{
			this.schemaSelector.removeActionListener(this);

			setBusy(true);
			
			List schemas = this.dbConnection.getMetadata().getSchemas();
			String currentSchema = null;
			boolean workspaceSchema = false;
			if (this.dbConnection.getProfile().getStoreExplorerSchema())
			{
				currentSchema = this.schemaFromWorkspace;
				workspaceSchema = true;
			}
			
			if (currentSchema == null) currentSchema = this.dbConnection.getCurrentSchema();
			if (currentSchema == null) currentSchema = this.dbConnection.getCurrentUser();
			
			if (schemas.size() > 0)
			{
				this.schemaSelector.setEnabled(true);
				this.schemaSelector.setVisible(true);
				this.schemaLabel.setVisible(true);

				this.schemaSelector.removeAllItems();
				this.schemaSelector.addItem("*");
				for (int i=0; i < schemas.size(); i++)
				{
					String schema = (String)schemas.get(i);
					if (schema != null) 
					{
						this.schemaSelector.addItem(schema.trim());
						if (schema.equalsIgnoreCase(currentSchema)) schemaToSelect = schema;
					}
				}
				
				if (workspaceSchema && schemaToSelect == null)
				{
					// when using the workspace for multiple connections
					// it can happen that the stored schema does not exist 
					// for the current connection, in this case we revert
					// to "current" schema
					schemaToSelect = this.dbConnection.getMetadata().getCurrentSchema();
				}
				//LogMgr.logDebug("DbExplorerPanel.readSchemas()", "Selected schema entry: " + schemaToSelect);
				if (schemaToSelect != null) 
				{
					schemaSelector.setSelectedItem(schemaToSelect);
				}
				else 
				{
					schemaSelector.setSelectedIndex(0);
				}
				currentSchema = (String)schemaSelector.getSelectedItem();
			}
			else
			{
				this.schemaSelector.setEnabled(false);
				this.schemaSelector.setVisible(false);
				this.schemaLabel.setVisible(false);
				currentSchema = null;
			}
			readCatalogs();
			
			tables.setCatalogAndSchema(getSelectedCatalog(), currentSchema, false);
      procs.setCatalogAndSchema(getSelectedCatalog(), currentSchema, false);
		}
		catch (Throwable e)
		{
			LogMgr.logError("DbExplorer.readSchemas()", "Could not retrieve list of schemas", e);
		}
		finally
		{
			this.schemaRetrievePending = false;
			setBusy(false);
		}
		this.schemaSelector.addActionListener(this);
	}

	public boolean isConnected()
	{
		return (this.dbConnection != null);
	}

	private void doConnect(ConnectionProfile profile)
	{
		String id = this.getId();
		ConnectionMgr mgr = ConnectionMgr.getInstance();
		WbConnection conn = null;
		try
		{
			WbSwingUtilities.showWaitCursor(this);
			conn = mgr.getConnection(profile, id, true);
			this.setConnection(conn);
			if (Settings.getInstance().getRetrieveDbExplorer())
			{
				this.retrieve();
			}
		}
		catch (Exception e)
		{
			String error = ExceptionUtil.getDisplay(e);
			String msg = ResourceMgr.getString("ErrExplorerConnectFailed").replaceAll("%msg%", error.trim());
			WbSwingUtilities.showErrorMessage(this, msg);
			LogMgr.logError("MainWindow.showDbExplorer()", "Error getting new connection for DbExplorer tab. Using connection from current panel", e);
		}
		finally
		{
			WbSwingUtilities.showDefaultCursor(this);
		}
	}
	
	public void connect(final ConnectionProfile profile)
	{
		// connecting can be pretty time consuming on a slow system
		// so move it into its own thread...
		if (!this.isConnected())
		{
			Thread t = new WbThread("DbExplorer connection")
			{
				public void run()
				{
					doConnect(profile);
				}
			};
			t.start();
		}
	}
	
	private boolean isConnectionBusy()
	{
		if (this.dbConnection == null) return false;
		//if (!this.mainWindow.isBusy()) return false;
		if (this.dbConnection.getProfile().getUseSeparateConnectionPerTab()) return this.isBusy();
		return dbConnection.isBusy();
	}
	
	private void initConnection()
	{
		if (this.dbConnection == null) return;
		try
		{
			this.tables.setConnection(this.dbConnection);
			this.procs.setConnection(dbConnection);
			if (this.triggers != null) this.triggers.setConnection(dbConnection);
			if (this.searchPanel != null) this.searchPanel.setConnection(dbConnection);
			readSchemaLabel();
			this.connectionInitPending = false;
		}
		catch (Exception e)
		{
			LogMgr.logError("DbExplorerPanel.initConnection()", "Error during init",e);
		}
	}

	private void readSchemaLabel()
	{
		StringBuilder s = new StringBuilder(this.dbConnection.getMetadata().getSchemaTerm());
		s.setCharAt(0, Character.toUpperCase(s.charAt(0)));
		this.schemaLabel.setText(s.toString());
	}
	
	public void setConnection(WbConnection aConnection)
	{
		if (this.isBusy()) return;
		
		this.dbConnection = aConnection;
		setSwitchCatalog(false);
		
		if (aConnection == null)
		{
			this.reset();
			this.connectionInitPending = false;
			return;
		}
		
		WbSwingUtilities.showWaitCursorOnWindow(this);
		
		try
		{
			if (this.connectionSelector != null)
			{
				// always switch database/catalog if in stand-alone mode
				setSwitchCatalog(true);
			}
			else if (aConnection.getProfile() != null)
			{
				boolean separateConnection = aConnection.getProfile().getUseSeparateConnectionPerTab();
				setSwitchCatalog(separateConnection);
				// when dealing with tables that have LONG or LONG RAW columns
				// and DBMS_OUTPUT was enabled, then retrieval of those columns
				// does not work. If we have separate connections for each tab
				// we can safely disable the DBMS_OUTPUT on this connection 
				// as there won't be a way to view the output anyway
				if (separateConnection) aConnection.getMetadata().disableOutput();
			}
			
			this.connectionInitPending = true;
			this.schemaRetrievePending = true;
			this.retrievePending = Settings.getInstance().getRetrieveDbExplorer();
			
			if (this.window != null)
			{
				String name = null;
				ConnectionProfile prof = aConnection.getProfile();
				if (prof != null) name = prof.getName();
				if (name != null) this.window.setProfileName(name);
			}
			
			this.connectionInfo.setConnection(aConnection);
			
			// Try to avoid concurrent execution on the 
			// same connection object
			if (!this.isConnectionBusy())
			{
				initConnection();
				
				if (this.isVisible())
				{
					readSchemas();
					
					if (this.retrievePending)
					{
						// if we are visible start the retrieve immediately
						retrieve();
					}
				}
			}
		}
		catch (Throwable th)
		{
			this.retrievePending = true;
			this.schemaRetrievePending = true;
			LogMgr.logError("DbExplorerPanel.setConnection()", "Error during connection init", th);
		}
		finally
		{
			WbSwingUtilities.showDefaultCursorOnWindow(this);
		}
		
	}

	private void readCatalogs()
	{
		DataStore ds = this.dbConnection.getMetadata().getCatalogInformation();
		this.catalogSelector.removeActionListener(this);
		if (ds.getRowCount() == 0) 
		{
			this.catalogSelector.setVisible(false);
			this.catalogSelector.setEnabled(false);
			this.catalogLabel.setVisible(false);
			this.catalogFromWorkspace = null;
		}
		else
		{
			String cat = StringUtil.capitalize(this.dbConnection.getMetadata().getCatalogTerm());

			this.catalogSelector.removeAllItems();
			this.catalogLabel.setText(cat);

			for (int i = 0; i < ds.getRowCount(); i++)
			{
				String db = ds.getValueAsString(i, 0);
				catalogSelector.addItem(db);
			}
			String db = this.dbConnection.getMetadata().getCurrentCatalog();
			catalogSelector.setSelectedItem(db);
			if (this.dbConnection.getProfile().getStoreExplorerSchema() && this.catalogFromWorkspace != null)
			{
				this.catalogSelector.setSelectedItem(this.catalogFromWorkspace);
			}
			this.catalogSelector.addActionListener(this);
			this.catalogSelector.setVisible(true);
			this.catalogSelector.setEnabled(true);
			this.catalogLabel.setVisible(true);
		}
		this.selectorPanel.validate();
	}
	
	public void setVisible(boolean flag)
	{
		boolean wasVisible = this.isVisible();
		super.setVisible(flag);
		if (!wasVisible && flag)
		{
			if (schemaRetrievePending)
			{
				this.readSchemas();
			}
			if (retrievePending)
			{
				// retrievePending will be true, if the connection has
				// been set already, the DbExplorer should be retrieved automatically
				// and the panel was not visible when the connection was provided
				EventQueue.invokeLater(new Runnable()
				{
					public void run()
					{
						retrieve();
					}
				});
			}
		}
	}

	public void panelSelected()
	{
		Component panel = this.tabPane.getSelectedComponent();
		if (panel == null) return;
		if (panel == this.tables)
		{
			this.tables.panelSelected();
		}
		else if (panel == this.procs)
		{
			this.procs.panelSelected();
		}
		else if (panel == this.triggers)
		{
			this.triggers.panelSelected();
		}
	}
	
	public WbConnection getConnection()
	{
		return this.dbConnection;
	}

	public void reset()
	{
		if (this.tables != null) this.tables.reset();
		if (this.procs != null) this.procs.reset();
		if (this.searchPanel != null) this.searchPanel.reset();
		if (this.tabPane != null && this.tabPane.getTabCount() > 0) this.tabPane.setSelectedIndex(0);
	}

	public void disconnect()
	{
		this.reset();
		this.tables.disconnect();
		this.procs.disconnect();
		if (this.triggers != null) this.triggers.disconnect();
		this.searchPanel.disconnect();
		this.dbConnection = null;
	}

	public void saveSettings()
	{
		this.tables.saveSettings();
		this.procs.saveSettings();
		if (this.triggers != null) this.triggers.saveSettings();
		if (this.searchPanel != null) this.searchPanel.saveSettings();
	}

	public void restoreSettings()
	{
		if (tables != null) tables.restoreSettings();
		if (procs != null) procs.restoreSettings();
		if (this.triggers != null) triggers.restoreSettings();
		if (this.searchPanel != null) searchPanel.restoreSettings();
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == this.schemaSelector)
		{
			retrieve();
		}
		else if (e.getSource() == this.selectConnectionButton)
		{
			this.connectionSelector.selectConnection();
		}
		else if (e.getSource() == this.catalogSelector)
		{
			if (this.switchCatalog)
			{
				try
				{
					this.dbConnection.getMetadata().setCurrentCatalog(getSelectedCatalog());
				}
				catch (SQLException ex)
				{
					WbSwingUtilities.showErrorMessage(this, ExceptionUtil.getDisplay(ex));
				}
			}
			retrieve();
		}
	}

	protected String getSelectedCatalog()
	{
		if (this.catalogSelector == null) return null;
		return (String)catalogSelector.getSelectedItem();
	}
	
	protected void retrieve()
	{
		if (this.dbConnection == null || this.dbConnection.isClosed()) return;
		
		if (this.isBusy() || isConnectionBusy()) 
		{
			this.retrievePending = true;
			return;
		}
		
		if (this.connectionInitPending)
		{
			this.initConnection();
		}
		
		if (this.schemaRetrievePending)
		{
			this.readSchemas();
		}
		
		final String schema = (String)schemaSelector.getSelectedItem();
		final Component c = this;
		
		Thread t = new WbThread("SchemaChange")
		{
			public void run()
			{
				try
				{
					setBusy(true);
					WbSwingUtilities.showWaitCursorOnWindow(c);
					tables.setCatalogAndSchema(getSelectedCatalog(), schema, true);
					procs.setCatalogAndSchema(getSelectedCatalog(), schema, true);
					if (triggers != null) triggers.setCatalogAndSchema(getSelectedCatalog(), schema, true);
				}
				catch (Exception ex)
				{
					LogMgr.logError(this, "Could not set schema", ex);
				}
				finally
				{
					retrievePending = false;
					setBusy(false);
					WbSwingUtilities.showDefaultCursorOnWindow(c);
				}
			}
		};
		t.start();
	}

	public String getName() { return getTabTitle(); }
	
	public void setTabName(String name)
	{
		this.tabTitle = name;
	}
	
	public String getTabTitle()
	{
		return ResourceMgr.getString("LblDbExplorer");
	}
	
	public void setTabTitle(JTabbedPane tab, int index)
	{
		String title = (this.tabTitle == null ? getTabTitle() : this.tabTitle);
		
		String realTitle = title + " " + Integer.toString(index+1);
		tab.setTitleAt(index, realTitle);
		if (index < 9)
		{
			char c = Integer.toString(index+1).charAt(0);
			int pos = title.length() + 1;
			tab.setMnemonicAt(index, c);
			// The Mnemonic index has to be set explicitely otherwise
			// the display would be wrong if the tab title contains
			// the mnemonic character
			tab.setDisplayedMnemonicIndexAt(index, pos);
		}
	}

	public void closeWindow()
	{
		if (this.window != null)
		{
			this.window.setVisible(false);
			this.window.dispose();
			this.window = null;
		}

	}

	public void openWindow(String aProfileName)
	{
		if (this.window == null)
		{
			this.window = new DbExplorerWindow(this, aProfileName);
		}
		this.window.setVisible(true);
	}

	public DbExplorerWindow getWindow()
	{
		return this.window;
	}

	public void addToActionMap(WbAction anAction)
	{
	}

	public List getActions()
	{
		return Collections.EMPTY_LIST;
	}

	public WbToolbar getToolbar()
	{
		return this.toolbar;
	}

	public void showLogMessage(String aMsg) {}
	public void clearStatusMessage() {} 
	public void showStatusMessage(String aMsg) {}
	public void clearLog() {}

	public void showLogPanel() {}
	public void showResultPanel() {}

	public void addToToolbar(WbAction anAction, boolean aFlag)
	{
	}

	public void activateWindow()
	{
		if (this.window != null)
		{
			this.window.setVisible(true);
			this.window.toFront();
		}
	}
	
	public void explorerWindowClosed()
	{
		this.window = null;
		
		if (this.dbConnection != null)
		{
			if (this.dbConnection.getProfile().getUseSeparateConnectionPerTab())
			{
				try { this.dbConnection.disconnect(); } catch (Throwable th) {}
			}
		}
		this.dispose();
		this.disconnect();
		
		this.mainWindow.explorerWindowClosed(this);
	}

	public void updateUI()
	{
		super.updateUI();
		if (this.toolbar != null)
		{
			this.toolbar.updateUI();
			this.toolbar.repaint();
		}
		if (this.procs != null)
		{
			this.procs.updateUI();
			this.procs.repaint();
		}
	}

	public void stateChanged(ChangeEvent e)
	{
		if (e.getSource() == this.tabPane)
		{
			if (this.tabPane.getSelectedIndex() == 1)
			{
				this.procs.retrieveIfNeeded();
			}
		}
	}

	public void dispose()
	{
		this.reset();
		if (mainWindow != null)
		{
			mainWindow.removeExecutionListener(this);
		}
		
		synchronized (DbExplorerPanel.class)
		{
			instanceCount--;
		}
	}

	public void saveToWorkspace(WbWorkspace w, int index) 
		throws IOException
	{
		// this will increase the visible count for DbExplorer Panels in the workspace
		w.dDbExplorerVisible();
		Object s = this.schemaSelector.getSelectedItem();
		WbProperties p = w.getSettings();
		String key = "dbexplorer" + index + ".currentschema";
		if (s != null)
		{
			p.setProperty(key, s.toString());
		}
		else if (this.schemaFromWorkspace != null)
		{
			// if the DbExplorer was never "really" displayed we have to 
			// save the schema that we retrieved initially from the workspace
			p.setProperty(key, this.schemaFromWorkspace);
		}
		
		key = "dbexplorer" + index + ".currentcatalog";
		s = this.getSelectedCatalog();
		if (s != null)
		{
			p.setProperty(key, s.toString());
		}
		else if (this.catalogFromWorkspace != null)
		{
			p.setProperty(key, this.catalogFromWorkspace);
		}

		tables.saveToWorkspace(w, index);
		searchPanel.saveToWorkspace(w, index);
		procs.saveToWorkspace(w, index);
		if (triggers != null) triggers.saveToWorkspace(w, index);
	}

	public boolean canCloseTab()
	{
		return true;
	}

	public void readFromWorkspace(WbWorkspace w, int index) 
		throws IOException
	{
		this.schemaFromWorkspace = null;
		this.catalogFromWorkspace = null;
		this.reset();
		try
		{
			WbProperties p = w.getSettings();
			this.schemaFromWorkspace = p.getProperty("dbexplorer" + index + ".currentschema", null);
			this.catalogFromWorkspace = p.getProperty("dbexplorer" + index + ".currentcatalog", null);
			tables.readFromWorkspace(w, index);
			searchPanel.readFromWorkspace(w, index);
			procs.readFromWorkspace(w, index);
			if (triggers != null) triggers.readFromWorkspace(w, index);
		}
		catch (Exception e)
		{
			LogMgr.logError("DbExplorerPanel.readFromWorkspace()", "Error loading workspace", e);
		}
	}

	public void executionStart(WbConnection conn, Object source)
	{
	}

	/*
	 *	Fired by the SqlPanel if DB access finished
	 */
	public void executionEnd(WbConnection conn, Object source)
	{
		if (this.connectionInitPending)
		{
			this.initConnection();
		}
		if (this.isVisible() && this.schemaRetrievePending)
		{
			this.readSchemas();
		}
		if (this.isVisible() && this.retrievePending)
		{
			this.retrieve();
		}
	}

}
