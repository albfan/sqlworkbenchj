/*
 * DbExplorerWindow.java
 *
 * Created on August 6, 2002, 1:11 PM
 */

package workbench.gui.dbobjects;

import java.awt.BorderLayout;
import java.awt.Dimension;
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
import workbench.db.WbConnection;
import workbench.gui.MainWindow;
import workbench.gui.actions.WbAction;
import workbench.gui.components.ConnectionInfo;
import workbench.gui.components.WbToolbar;
import workbench.interfaces.MainPanel;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

/**
 *
 * @author  workbench@kellerer.org
 */
public class DbExplorerPanel extends JPanel implements ActionListener, MainPanel
{
	private MainWindow parentWindow;
	private JTabbedPane tabPane;
	private TableListPanel tables;
	private TableSearchPanel searchPanel;
	private ProcedureListPanel procs;
	private JComboBox schemaSelector;
	private JComboBox catalogSelector;
	private JLabel schemaLabel;
	private JPanel selectorPanel;
	private JLabel catalogLabel;
	boolean connected;
	private WbConnection dbConnection;
	private DbExplorerWindow window;
	private WbToolbar toolbar;
	private ConnectionInfo connectionInfo;
	private boolean restoreWindow = false;

	public DbExplorerPanel(MainWindow aParent)
	{
		this.parentWindow = aParent;
		try
		{
			tables = new TableListPanel(aParent);
			procs = new ProcedureListPanel();
			searchPanel = new TableSearchPanel(tables);
			tabPane = new JTabbedPane(JTabbedPane.TOP);
			tabPane.add(ResourceMgr.getString("TxtDbExplorerTables"), tables);
			tabPane.add(ResourceMgr.getString("TxtDbExplorerProcs"), procs);
			tabPane.add(ResourceMgr.getString("TxtSearchTables"), searchPanel);
			tabPane.setFocusable(false);
		}
		catch (Exception e)
		{
			LogMgr.logError(this, "Could not initialize DbExplorerPane", e);
		}
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

		//this.catalogSelector = new JComboBox();
		//this.catalogSelector.setMaximumSize(d);
		//this.catalogLabel = new JLabel();
		//this.selectorPanel.add(this.catalogLabel);
		//this.selectorPanel.add(this.catalogSelector);

		this.add(this.selectorPanel, BorderLayout.NORTH);
		this.add(tabPane, BorderLayout.CENTER);

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

	public void setConnection(WbConnection aConnection)
	{
		this.setConnection(aConnection, null);
	}

	public void readSchemas()
	{
		try
		{
			this.schemaSelector.removeActionListener(this);

			StringBuffer s = new StringBuffer(this.dbConnection.getMetadata().getSchemaTerm());
			s.setCharAt(0, Character.toUpperCase(s.charAt(0)));
			this.schemaLabel.setText(s.toString());
			List schemas = this.dbConnection.getMetadata().getSchemas();
			this.schemaSelector.removeAllItems();
			this.schemaSelector.addItem("*");
			for (int i=0; i < schemas.size(); i++)
			{
				this.schemaSelector.addItem(schemas.get(i));
			}
		}
		catch (Exception e)
		{
			LogMgr.logError(this, "Could not retrieve list of schemas", e);
		}
		this.schemaSelector.setSelectedItem(null);
		this.schemaSelector.addActionListener(this);
	}

	/*
	public void readCatalogs()
	{
		this.catalogSelector.removeActionListener(this);
		this.catalogSelector.removeAllItems();
		List catalogs = this.dbConnection.getMetadata().getCatalogs();
		if (catalogs.size() > 0)
		{
			StringBuffer s = new StringBuffer(this.dbConnection.getMetadata().getCatalogTerm());
			s.setCharAt(0, Character.toUpperCase(s.charAt(0)));
			this.catalogLabel.setText(s.toString());
			this.catalogSelector.addItem("*");
			for (int i=0; i < catalogs.size(); i++)
			{
				this.catalogSelector.addItem(catalogs.get(i));
			}
			this.catalogSelector.setSelectedItem(null);
			this.catalogSelector.addActionListener(this);
		}
		else
		{
			this.catalogLabel.setVisible(false);
			this.catalogSelector.setVisible(false);
		}
	}
	*/

	public void setConnection(WbConnection aConnection, String aProfilename)
	{
		if (aConnection == null) return;
		this.dbConnection = aConnection;
		this.tables.setConnection(aConnection);
		this.procs.setConnection(aConnection);
		this.searchPanel.setConnection(aConnection);
		this.tables.addTableListDisplayClient(this.searchPanel.getTableList());
		this.readSchemas();

		if (this.window != null && aProfilename != null)
		{
			this.window.setProfileName(aProfilename);
		}
		this.connectionInfo.setConnection(aConnection);
	}

	public void disconnect()
	{
		this.dbConnection = null;
		tables.disconnect();
		procs.disconnect();
		this.searchPanel.disconnect();
	}

	public boolean isConnected()
	{
		return this.dbConnection != null;
	}

	public void saveSettings()
	{
		this.tables.saveSettings();
		this.procs.saveSettings();
		this.searchPanel.saveSettings();
	}
	public void restoreSettings()
	{
		tables.restoreSettings();
		procs.restoreSettings();
		this.searchPanel.restoreSettings();
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == this.schemaSelector)
		{
			try
			{
				String schema = (String)schemaSelector.getSelectedItem();
				String cat = null;
				if (catalogSelector != null)
				{
					cat = (String)catalogSelector.getSelectedItem();
				}
				tables.setCatalogAndSchema(cat, schema);
				procs.setCatalogAndSchema(cat, schema);
			}
			catch (Exception ex)
			{
				LogMgr.logError(this, "Could not set schema", ex);
			}
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
	public void mainWindowDeiconified()
	{
		if (this.window != null && this.restoreWindow) this.window.show();
	}

	public void mainWindowIconified()
	{
		if (this.window != null)
		{
			this.restoreWindow = this.window.isVisible();
			this.window.hide();
		}
	}
	public void updateUI()
	{
		super.updateUI();
		if (this.toolbar != null)
		{
			this.toolbar.updateUI();
			this.toolbar.repaint();
		}
	}
}
