/*
 * DbExplorerWindow.java
 *
 * Created on August 6, 2002, 1:11 PM
 */

package workbench.gui.dbobjects;

import java.awt.Image;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;
import workbench.WbManager;
import workbench.db.WbConnection;
import workbench.gui.WbSwingUtilities;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

/**
 *
 * @author  sql.workbench@freenet.de
 */
public class DbExplorerWindow
	extends JFrame
	implements WindowListener
{
	JTabbedPane tabPane;
	TableListPanel tables;
	
	/** Creates a new instance of DbExplorerWindow */
	public DbExplorerWindow(WbConnection aConnection, String aProfileName)
		throws Exception
	{
		super(ResourceMgr.getString("TxtDbExplorerTitel") + " - [" + aProfileName + "]");
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		tables = new TableListPanel(aConnection);
		tables.restoreSettings();
		tabPane = new JTabbedPane(JTabbedPane.TOP);
		tabPane.add(ResourceMgr.getString("TxtDbExplorerTables"), tables);
		this.addWindowListener(this);
		this.getContentPane().add(tabPane);
		this.setIconImage(ResourceMgr.getImage("Database").getImage());
		this.restorePosition();
	}
	
	public void setConnection(WbConnection aConnection, String aProfileName)
		throws Exception
	{
		this.setTitle(ResourceMgr.getString("TxtDbExplorerTitel") + " - [" + aProfileName + "]");
		this.tables.setConnection(aConnection);
	}
	
	public void restorePosition()
	{
		Settings s = WbManager.getSettings();

		if (!s.restoreWindowSize(this))
		{
			this.setSize(500,500);
		}

		if (!s.restoreWindowPosition(this))
		{
			WbSwingUtilities.center(this, null);
		}
	}
	
	public void windowActivated(WindowEvent e)
	{
	}	
	
	public void windowClosed(WindowEvent e)
	{
		WbManager.getSettings().storeWindowPosition(this);
		WbManager.getSettings().storeWindowSize(this);
		this.tables.saveSettings();
	}	
	
	public void windowClosing(WindowEvent e)
	{
	}
	
	public void windowDeactivated(WindowEvent e)
	{
	}

	public void windowDeiconified(WindowEvent e)
	{
	}
	
	public void windowIconified(WindowEvent e)
	{
	}
	
	public void windowOpened(WindowEvent e)
	{
	}
	
}
