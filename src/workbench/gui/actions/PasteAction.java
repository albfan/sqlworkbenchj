/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import workbench.gui.ClipboardSupport;
import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.KeyStroke;
import workbench.resource.ResourceMgr;

/**
 *	Action to paste the contents of the clipboard into the entry field
 *	@author  thomas.kellerer@web.de
 */
public class PasteAction extends AbstractAction
{
	private ClipboardSupport client;
	
	public PasteAction(ClipboardSupport aClient)
	{
		this.client = aClient;
		this.putValue(Action.NAME, ResourceMgr.getString(ResourceMgr.TXT_PASTE));
		this.putValue(Action.SMALL_ICON, ResourceMgr.getImage(ResourceMgr.IMG_PASTE_16));
		this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("C+V"));
	}
	
	public void actionPerformed(ActionEvent e)
	{
		this.client.paste();
	}
}
