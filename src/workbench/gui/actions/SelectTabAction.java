/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;

import workbench.gui.dbobjects.DbExplorerPanel;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	@author  workbench@kellerer.org
 */
public class SelectTabAction extends WbAction
{
	private JTabbedPane client;
	private int index;
	private String baseName;
	
	public SelectTabAction(JTabbedPane aPane, int anIndex)
	{
		super();
		this.client = aPane;
		this.index = anIndex;
		this.initName();
	}

	private void initName()
	{
		Object tab = this.client.getComponentAt(this.index);
		if (tab instanceof DbExplorerPanel)
		{
			this.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_MASK));
			this.putValue(Action.NAME, ResourceMgr.getString("MnuTxtShowDbExplorer"));
		}
		else
		{
			switch (this.index)
			{
				case 0:
					this.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, InputEvent.CTRL_MASK));
					break;
				case 1:
					this.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2, InputEvent.CTRL_MASK));
					break;
				case 2:
					this.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_3, InputEvent.CTRL_MASK));
					break;
				case 3:
					this.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_4, InputEvent.CTRL_MASK));
					break;
				case 4:
					this.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_5, InputEvent.CTRL_MASK));
					break;
				case 5:
					this.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_6, InputEvent.CTRL_MASK));
					break;
				case 6:
					this.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_7, InputEvent.CTRL_MASK));
					break;
				case 7:
					this.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_8, InputEvent.CTRL_MASK));
					break;
				case 8:
					this.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_9, InputEvent.CTRL_MASK));
					break;
				default:
					this.setAccelerator(null);
			}
			this.setActionName("SelectTab" + (this.index+1));
			this.putValue(Action.NAME, ResourceMgr.getString("LabelTabStatement") + " &" + Integer.toString(this.index+1));
		}
		this.setIcon(null);
	}
	public int getIndex() { return this.index; }
	
	public void setNewIndex(int anIndex)
	{
		this.index = anIndex;
		this.initName();
	}
	
	public void setName(String aName)
	{
		this.putValue(Action.NAME, aName + " &" + Integer.toString(this.index+1));
	}
	
	public void executeAction(ActionEvent e)
	{
		if (client != null)
		{
			try
			{
				this.client.setSelectedIndex(this.index);
			}
			catch (Exception ex)
			{
				LogMgr.logError("SelectTabAction.executeAction()", "Error when selecting tab " + this.index, ex);
			}
		}
	}
}
