/*
 * DbExplorerWindow.java
 *
 * Created on August 6, 2002, 1:11 PM
 */

package workbench.gui.dbobjects;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import workbench.WbManager;
import workbench.gui.WbSwingUtilities;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

/**
 *
 * @author  workbench@kellerer.org
 */
public class DbExplorerWindow
	extends JFrame
	implements WindowListener
{
	DbExplorerPanel panel;
	boolean connected;

	/** Creates a new instance of DbExplorerWindow */
	public DbExplorerWindow(DbExplorerPanel aPanel, String aProfileName)
	{
		super(ResourceMgr.getString("TxtDbExplorerTitel") + " - [" + aProfileName + "]");
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.panel = aPanel;
		this.addWindowListener(this);
		this.getContentPane().add(this.panel);
		this.setIconImage(ResourceMgr.getImage("Database").getImage());
		this.setProfileName(aProfileName);
		this.restorePosition();
	}

	public void setProfileName(String aProfileName)
	{
		this.setTitle(ResourceMgr.getString("TxtDbExplorerTitel") + " - [" + aProfileName + "]");
	}
  
  public void saveSettings()
  {
		WbManager.getSettings().storeWindowPosition(this);
		WbManager.getSettings().storeWindowSize(this);
		this.panel.saveSettings();
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
		if (this.panel != null)
		{
			panel.explorerWindowClosed();
		}
	}

	public void windowClosing(WindowEvent e)
	{
    this.saveSettings();
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
