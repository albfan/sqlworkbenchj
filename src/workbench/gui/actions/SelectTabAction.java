/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import workbench.interfaces.ClipboardSupport;
import workbench.interfaces.Exporter;
import workbench.resource.ResourceMgr;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	@author  sql.workbench@freenet.de
 */
public class SelectTabAction extends WbAction
{
	private JTabbedPane client;
	private int index;
	
	public SelectTabAction(JTabbedPane aPane, int anIndex)
	{
		super();
		this.client = aPane;
		this.index = anIndex;
		switch (anIndex)
		{
			case 0:
				this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_1, InputEvent.CTRL_MASK));
				break;
			case 1:
				this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_2, InputEvent.CTRL_MASK));
				break;
			case 2:
				this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_3, InputEvent.CTRL_MASK));
				break;
			case 3:
				this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_4, InputEvent.CTRL_MASK));
				break;
			case 4:
				this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_5, InputEvent.CTRL_MASK));
				break;
			case 5:
				this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_6, InputEvent.CTRL_MASK));
				break;
			case 6:
				this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_7, InputEvent.CTRL_MASK));
				break;
			case 7:
				this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_8, InputEvent.CTRL_MASK));
				break;
			case 8:
				this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_9, InputEvent.CTRL_MASK));
				break;
		}
		this.setActionName("SelectTab" + (anIndex+1));
		this.putValue(Action.NAME, ResourceMgr.getString("LabelTabStatement") + " " + Integer.toString(anIndex+1));
		this.putValue(Action.SMALL_ICON, null);
	}

	public void actionPerformed(ActionEvent e)
	{
		if (client != null)
		{
			try
			{
				this.client.setSelectedIndex(this.index);
			}
			catch (Exception ex)
			{
			}
		}
	}
}
