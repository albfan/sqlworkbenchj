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
import workbench.interfaces.ClipboardSupport;
import workbench.interfaces.Exporter;
import workbench.resource.ResourceMgr;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	@author  thomas.kellerer@web.de
 */
public class SaveDataAsAction extends AbstractAction
{
	private Exporter client;
	
	public SaveDataAsAction(Exporter aClient)
	{
		this.client = aClient;
		this.putValue(Action.NAME, ResourceMgr.getString("SaveDataAs"));
		this.putValue(Action.SMALL_ICON, ResourceMgr.getImage(ResourceMgr.IMG_SAVE_AS));
		this.putValue(WbActionConstants.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_DATA);
	}

	public void actionPerformed(ActionEvent e)
	{
		this.client.saveAsAscii();
	}
}
