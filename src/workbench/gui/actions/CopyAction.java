/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;
import workbench.gui.ClipboardSupport;
import workbench.resource.ResourceMgr;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	@author  thomas.kellerer@web.de
 */
public class CopyAction extends AbstractAction
{
	private ClipboardSupport client;
	
	public CopyAction(ClipboardSupport aClient)
	{
		this.client = aClient;
		this.putValue(Action.NAME, ResourceMgr.getString(ResourceMgr.TXT_COPY));
		this.putValue(Action.SMALL_ICON, ResourceMgr.getImage(ResourceMgr.IMG_COPY_16));
		//this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("C+C"));
	}
	
	public void actionPerformed(ActionEvent e)
	{
		this.client.copy();
	}
}
