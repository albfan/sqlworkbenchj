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
import javax.swing.KeyStroke;
import workbench.interfaces.ClipboardSupport;
import workbench.interfaces.Exporter;
import workbench.resource.ResourceMgr;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	@author  workbench@kellerer.org
 */
public class ChangeDatabaseAction extends WbAction
{
	public ChangeDatabaseAction(String aCatalogName)
	{
		super();
		String name = ResourceMgr.getString("MnuTxtChangeDatabase");
		this.putValue(Action.NAME, name);
		this.putValue(WbAction.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_SQL);
		this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_MASK));
	}

	public void actionPerformed(ActionEvent e)
	{
	}
}
