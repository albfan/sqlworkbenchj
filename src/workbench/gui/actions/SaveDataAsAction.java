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
 *	@author  sql.workbench@freenet.de
 */
public class SaveDataAsAction extends WbAction
{
	private Exporter client;

	public SaveDataAsAction(Exporter aClient)
	{
		super();
		this.client = aClient;
		this.putValue(Action.NAME, ResourceMgr.getString("MnuTxtSaveDataAs"));
		this.putValue(Action.SMALL_ICON, ResourceMgr.getImage(ResourceMgr.IMG_SAVE_AS));
		this.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getDescription("MnuTxtSaveDataAs"));
		this.putValue(WbAction.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_DATA);
	}

	public void actionPerformed(ActionEvent e)
	{
		this.client.saveAsAscii();
	}
}
