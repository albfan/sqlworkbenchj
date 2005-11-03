/*
 * DbExplorerPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
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
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
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
import workbench.util.ExceptionUtil;
import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.WbAction;
import workbench.gui.components.ConnectionInfo;
import workbench.gui.components.ConnectionSelector;
import workbench.gui.components.TabbedPaneUIFactory;
import workbench.gui.components.WbToolbar;
import workbench.interfaces.MainPanel;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.util.WbThread;
import workbench.util.WbWorkspace;


/**
 *
 * @author  support@sql-workbench.net
 */
public class DbExplorerPanel
	extends JPanel
	implements ActionListener, MainPanel, ChangeListener
{
	private JTabbedPane tabPane;
	private TableListPanel tables;
	private TableSearchPanel searchPanel;
	private ProcedureListPanel procs;
	private JComboBox schemaSelector;
	private JLabel schemaLabel;
	private JPanel selectorPanel;
	boolean connected;
	private WbConnection dbConnection;
	private DbExplorerWindow window;
	private WbToolbar toolbar;
	private ConnectionInfo connectionInfo;
	private boolean retrievePending = false;
	private int internalId = 0;
	private ConnectionSelector connectionSelector;
	private JButton selectConnectionButton;
	private String tabTitle;
	private static int instanceCount = 0;
	private MainWindow mainWindow;
	
	public DbExplorerPanel()
	{
		this(null);
	}

	public DbExplorerPanel(MainWindow aParent)
	{
		synchronized (DbExplorerPanel.class)
		{
			instanceCount++;
		}
		this.internalId = instanceCount;
		this.mainWindow = aParent;
		try
		{
			tables = new TableListPanel(aParent);
			procs = new ProcedureListPanel();
			this.searchPanel = new TableSearchPanel(tables);
			tabPane = new JTabbedPane(JTabbedPane.TOP);
			tabPane.setUI(TabbedPaneUIFactory.getBorderLessUI());
			tabPane.add(ResourceMgr.getString("TxtDbExplorerTables"), tables);
			tabPane.setToolTipTextAt(0, ResourceMgr.getDescription("TxtDbExplorerTables"));

			tabPane.add(ResourceMgr.getString("TxtDbExplorerProcs"), procs);
			tabPane.setToolTipTextAt(1, ResourceMgr.getDescription("TxtDbExplorerProcs"));
			tabPane.add(ResourceMgr.getString("TxtSearchTables"), this.searchPanel);
			tabPane.setToolTipTextAt(2, ResourceMgr.getDescription("TxtSearchTables"));
			tabPane.setFocusable(false);

			this.setBorder(WbSwingUtilities.EMPTY_BORDER);
			this.setLayout(new BorderLayout());
			Dimension d = new Dimension(800, 20);
			this.selectorPanel = new JPanel();
			this.selectorPanel.setMaximumSize(d);
			d = new Dimension(250, 20);
			this.selectorPanel.setPreferredSize(d);

			this.selectorPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));

			this.schemaLabel = new JLabel();

			this.selectorPanel.add(schemaLabel);
			this.schemaSelector = new JComboBox();

			this.selectorPanel.add(this.schemaSelector);

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
		}
		catch (Throwable e)
		{
			LogMgr.logError(this, "Could not initialize DbExplorerPanel", e);
		}
	}

	public void showConnectButton(ConnectionSelector selector)
	{
		this.connectionSelector = selector;
		this.selectConnectionButton = new JButton(ResourceMgr.getString("LabelSelectConnection"));
		Border b = new CompoundBorder(new EtchedBorder(EtchedBorder.LOWERED), new EmptyBorder(1, 10, 1, 10));
		this.selectConnectionButton.setBorder(b);
		this.selectConnectionButton.addActionListener(this);
		this.selectorPanel.add(Box.createHorizontalStrut(15));
		this.selectorPanel.add(this.selectConnectionButton);
	}

	public boolean isBusy()
	{
		return false;
	}

	public String getId()
	{
		return "WbExp-" + Integer.toString(this.internalId);
	}

	public void readSchemas()
	{
		String currentSchema = null;
		try
		{
			this.schemaSelector.removeActionListener(this);

			StringBuffer s = new StringBuffer(this.dbConnection.getMetadata().getSchemaTerm());
			s.setCharAt(0, Character.toUpperCase(s.charAt(0)));
			this.schemaLabel.setText(s.toString());
			List schemas = this.dbConnection.getMetadata().getSchemas();
			String user = this.dbConnection.getMetadata().getUserName();
			this.schemaSelector.removeAllItems();
			this.schemaSelector.addItem("*");
			for (int i=0; i < schemas.size(); i++)
			{
				String schema = (String)schemas.get(i);
				if (schema != null) 
				{
					this.schemaSelector.addItem(schema.trim());
					if (user.equalsIgnoreCase(schema)) currentSchema = schema;
				}
			}
			schemaSelector.setSelectedItem(currentSchema);
			tables.setCatalogAndSchema(null, currentSchema, false);
      procs.setCatalogAndSchema(null, currentSchema, false);

			Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
			int maxWidth = (int)(d.getWidth() / 2);
			d = this.schemaSelector.getPreferredSize();
			if (d.getWidth() > maxWidth)
			{
				d = new Dimension(maxWidth, 20);
				this.schemaSelector.setMaximumSize(d);
			}
		}
		catch (Throwable e)
		{
			LogMgr.logError(this, "Could not retrieve list of schemas", e);
		}
		this.schemaSelector.addActionListener(this);
	}

	public boolean isConnected()
	{
		if (this.dbConnection == null) return false;
		try
		{
			return !this.dbConnection.isClosed();
		}
		catch (Throwable e)
		{
			return false;
		}
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
				this.startRetrieve();
			}
		}
		catch (Exception e)
		{
			String error = ExceptionUtil.getDisplay(e);
			String msg = ResourceMgr.getString("ErrorExplorerConnectFailed").replaceAll("%msg%", error.trim());
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
	
	public void setConnection(WbConnection aConnection)
	{
		if (aConnection == null)
		{
			this.reset();
			return;
		}
		
		WbSwingUtilities.showWaitCursorOnWindow(this);
		try
		{
			this.dbConnection = aConnection;
			this.tables.setConnection(aConnection);
			this.procs.setConnection(aConnection);
			if (this.searchPanel != null) this.searchPanel.setConnection(aConnection);
			//if (this.generator != null) this.generator.setConnection(aConnection);
			this.schemaLabel.setText(aConnection.getMetadata().getSchemaTerm());
			this.schemaSelector.doLayout();
			this.readSchemas();

			if (this.window != null)
			{
				String name = null;
				ConnectionProfile prof = aConnection.getProfile();
				if (prof != null) name = prof.getName();
				if (name != null) this.window.setProfileName(name);
			}
			
			this.connectionInfo.setConnection(aConnection);

			if (Settings.getInstance().getRetrieveDbExplorer())
			{
				if (this.isVisible())
				{
					// if we are visible start the retrieve immediately
					this.retrievePending = false;
					EventQueue.invokeLater(new Runnable()
					{
						public void run()
						{
							fireSchemaChanged();
						}
					});
				}
				else
				{
					// if we are not visible just store the information
					// that we need to retrieve the table list.
					// this will be evaluated by the (overwritten) setVisible() method
					// There is no need in retrieving the information if we are not visible
					this.retrievePending = true;
				}
			}
		}
		finally
		{
			WbSwingUtilities.showDefaultCursorOnWindow(this);
		}
	}

	public void setVisible(boolean flag)
	{
		boolean wasVisible = this.isVisible();
		super.setVisible(flag);
		if (!wasVisible && flag && retrievePending)
		{
			// retrievePending will be true, if the connection has
			// been set already, the DbExplorer should be retrieved automatically
			// and the panel was not visible when the connection was provided
			retrievePending = false;
			EventQueue.invokeLater(new Runnable()
			{
				public void run()
				{
					fireSchemaChanged();
				}
			});
		}
	}

	public void panelSelected()
	{
		// nothing to do
	}
	
	public void startRetrieve()
	{
		this.fireSchemaChanged();
	}

	public WbConnection getConnection()
	{
		return this.dbConnection;
	}

	private void reset()
	{
		if (this.dbConnection != null)
		{
			try { this.dbConnection.rollback(); } catch (Throwable th) {}
		}
		this.tables.reset();
		this.procs.reset();
		this.searchPanel.reset();
		this.tabPane.setSelectedIndex(0);
	}

	public void disconnect()
	{
		this.reset();
		this.tables.disconnect();
		this.procs.disconnect();
		this.searchPanel.disconnect();
		this.dbConnection = null;
	}

	public void saveSettings()
	{
		this.tables.saveSettings();
		this.procs.saveSettings();
		if (this.searchPanel != null) this.searchPanel.saveSettings();
	}

	public void restoreSettings()
	{
		tables.restoreSettings();
		procs.restoreSettings();
		if (this.searchPanel != null) searchPanel.restoreSettings();
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == this.schemaSelector)
		{
			fireSchemaChanged();
		}
		else if (e.getSource() == this.selectConnectionButton)
		{
			this.connectionSelector.selectConnection();
		}
	}

	private void fireSchemaChanged()
	{
		this.fireSchemaChanged(true);
	}

	private void fireSchemaChanged(final boolean retrieve)
	{
		final String schema = (String)schemaSelector.getSelectedItem();
		final Component c = this;
		
		Thread t = new WbThread("Schema Change")
		{
			public void run()
			{
				try
				{
					WbSwingUtilities.showWaitCursorOnWindow(c);
					tables.setCatalogAndSchema(null, schema, retrieve);
					procs.setCatalogAndSchema(null, schema, retrieve);
				}
				catch (Exception ex)
				{
					LogMgr.logError(this, "Could not set schema", ex);
				}
				finally
				{
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
		return ResourceMgr.getString("LabelDbExplorer");
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

	public void showLogMessage(String aMsg)
	{
	}

	public void showStatusMessage(String aMsg)
	{
	}
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

	public void mainWindowDeiconified()
	{
	}

	public void mainWindowIconified()
	{
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
		tables.saveToWorkspace(w, index);
		searchPanel.saveToWorkspace(w, index);
	}

	public boolean prepareWorkspaceSaving()
	{
		return true;
	}

	public void readFromWorkspace(WbWorkspace w, int index) 
		throws IOException
	{
		tables.readFromWorkspace(w, index);
		searchPanel.readFromWorkspace(w, index);
	}

}
