/*
 * DbExplorerWindow.java
 *
 * Created on August 6, 2002, 1:11 PM
 */

package workbench.gui.dbobjects;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.List;

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

import workbench.WbManager;
import workbench.db.WbConnection;
import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.WbAction;
import workbench.gui.components.ConnectionInfo;
import workbench.gui.components.TabbedPaneUIFactory;
import workbench.gui.components.WbToolbar;
import workbench.interfaces.MainPanel;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.util.WbThread;


/**
 *
 * @author  workbench@kellerer.org
 */
public class DbExplorerPanel
	extends JPanel
	implements ActionListener, MainPanel, ChangeListener
{
	private MainWindow parentWindow;
	private JTabbedPane tabPane;
	private TableListPanel tables;
	private TableSearchPanel searchPanel;
	private ProcedureListPanel procs;
	//private PersistenceGeneratorPanel generator;
	private JComboBox schemaSelector;
	//private JComboBox catalogSelector;
	private JLabel schemaLabel;
	private JPanel selectorPanel;
	private JLabel catalogLabel;
	boolean connected;
	private WbConnection dbConnection;
	private DbExplorerWindow window;
	private WbToolbar toolbar;
	private ConnectionInfo connectionInfo;
	private boolean restoreWindow = false;
	private boolean retrievePending = false;
	private int internalId = 0;

	public DbExplorerPanel(MainWindow aParent, int index)
	{
		this.parentWindow = aParent;
		this.internalId = index;
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

			//tabPane.add(ResourceMgr.getString("TxtPersistenceGenerator"), new JPanel());
			tabPane.setFocusable(false);

			this.setBorder(WbSwingUtilities.EMPTY_BORDER);
			this.setLayout(new BorderLayout());
			Dimension d = new Dimension(32768, 20);
			this.selectorPanel = new JPanel();
			this.selectorPanel.setMaximumSize(d);
			this.selectorPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));

			this.schemaLabel = new JLabel();

			this.selectorPanel.add(schemaLabel);
			this.schemaSelector = new JComboBox();
			d = new Dimension(150, 20);
			this.schemaSelector.setMaximumSize(d);

			this.selectorPanel.add(this.schemaSelector);

			this.add(this.selectorPanel, BorderLayout.NORTH);
			this.add(tabPane, BorderLayout.CENTER);
			this.searchPanel.restoreSettings();

			this.toolbar = new WbToolbar();
			Border b = new CompoundBorder(new EmptyBorder(1,0,1,0), new EtchedBorder());
			this.toolbar.setBorder(b);
			this.toolbar.setBorderPainted(true);
			d = new Dimension(30, 30);
			this.toolbar.setMinimumSize(d);
			this.toolbar.setPreferredSize(new Dimension(100, 30));
			this.connectionInfo = new ConnectionInfo(this.toolbar.getBackground());
			this.connectionInfo.setMinimumSize(d);
			this.toolbar.add(this.connectionInfo);
		}
		catch (Throwable e)
		{
			LogMgr.logError(this, "Could not initialize DbExplorerPanel", e);
		}

		//this.tabPane.addChangeListener(this);
	}

	public boolean isBusy()
	{
		return false;
	}

	public String getId()
	{
		return "WbExp-" + Integer.toString(this.internalId);
	}

	private void initSearchPanel()
	{
		if (this.searchPanel != null) return;

		this.searchPanel = new TableSearchPanel(tables);

		if (this.dbConnection != null)
		{
			this.searchPanel.setConnection(this.dbConnection);
		}
		this.searchPanel.restoreSettings();
		int index = this.tabPane.getTabCount() - 2;
		this.tabPane.setComponentAt(index, this.searchPanel);
	}

	/*
	private void initGenerator()
	{
		this.generator = new PersistenceGeneratorPanel(this.tables);
		if (this.dbConnection != null)
		{
			this.generator.setConnection(this.dbConnection);
		}
		this.generator.restoreSettings();
		int index = this.tabPane.getTabCount() - 1;
		this.tabPane.setComponentAt(index, this.generator);
	}
	*/

	public void readSchemas()
	{
		String currentSchema = null;
		try
		{
			//LogMgr.logDebug("DbExplorerPanel.readSchemas()", "Reading schemas...");
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
				this.schemaSelector.addItem(schema);
				if (user.equalsIgnoreCase(schema)) currentSchema = schema;
			}
			schemaSelector.setSelectedItem(currentSchema);
			tables.setCatalogAndSchema(null, currentSchema, false);
      procs.setCatalogAndSchema(null, currentSchema, false);
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

	public void setConnection(WbConnection aConnection)
	{
		this.setConnection(aConnection, null);
	}

	public void setConnection(WbConnection aConnection, String aProfilename)
	{
		if (aConnection == null) return;
		this.dbConnection = aConnection;
		this.tables.setConnection(aConnection);
		this.procs.setConnection(aConnection);
		if (this.searchPanel != null) this.searchPanel.setConnection(aConnection);
		//if (this.generator != null) this.generator.setConnection(aConnection);
		this.schemaLabel.setText(aConnection.getMetadata().getSchemaTerm());
		this.schemaSelector.doLayout();
		this.readSchemas();

		if (this.window != null && aProfilename != null)
		{
			this.window.setProfileName(aProfilename);
		}
		this.connectionInfo.setConnection(aConnection);

		if (WbManager.getSettings().getRetrieveDbExplorer())
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

	public void startRetrieve()
	{
		this.fireSchemaChanged();
	}

	public WbConnection getConnection()
	{
		return this.dbConnection;
	}

	public void disconnect()
	{
		this.dbConnection = null;
		this.tables.disconnect();
		this.procs.disconnect();
		this.searchPanel.disconnect();

		int count = this.tabPane.getTabCount();
		this.tabPane.setSelectedIndex(0);
		this.closeWindow();
	}

	public void saveSettings()
	{
		this.tables.saveSettings();
		this.procs.saveSettings();
		if (this.searchPanel != null) this.searchPanel.saveSettings();
		//if (this.generator != null) this.generator.saveSettings();
	}

	public void restoreSettings()
	{
		tables.restoreSettings();
		procs.restoreSettings();
		if (this.searchPanel != null) searchPanel.restoreSettings();
		//if (this.generator != null) this.generator.restoreSettings();
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == this.schemaSelector)
		{
			fireSchemaChanged();
		}
	}

	private void fireSchemaChanged()
	{
		this.fireSchemaChanged(true);
	}

	private void fireSchemaChanged(final boolean retrieve)
	{
		final String schema = (String)schemaSelector.getSelectedItem();

		Thread t = new WbThread("Schema Change")
		{
			public void run()
			{
				try
				{
					tables.setCatalogAndSchema(null, schema, retrieve);
					procs.setCatalogAndSchema(null, schema, retrieve);
				}
				catch (Exception ex)
				{
					LogMgr.logError(this, "Could not set schema", ex);
				}
			}
		};
		t.start();
	}

	public void setTabTitle(JTabbedPane tab, int index)
	{
		tab.setTitleAt(index, ResourceMgr.getString("LabelDbExplorer"));
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
		this.window.show();
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

	public void explorerWindowClosed()
	{
		this.window = null;
		if (Settings.getInstance().getDbExplorerClearDataOnClose() && this.tables != null) this.tables.clearTableData();
		if (this.dbConnection != null &&
		    Settings.getInstance().disconnectDbExplorerOnClose() &&
		    this.dbConnection.getProfile().getUseSeperateConnectionPerTab())
		{
			try
			{
				this.dbConnection.disconnect();
			}
			catch (Throwable th)
			{
				LogMgr.logWarning("DbExplorerPanel.dispose()", "Error when closing connection", th);
			}

		}
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
		this.tables.reset();
	}

}