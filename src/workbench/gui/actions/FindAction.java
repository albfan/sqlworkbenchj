package workbench.gui.actions;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.Action;
import javax.swing.KeyStroke;
import workbench.interfaces.Searchable;
import workbench.resource.ResourceMgr;

/**
 *	Action to clear the contents of a entry field
 *	@author  workbench@kellerer.org
 */
public class FindAction extends WbAction
{
	private Searchable client;

	public FindAction(Searchable aClient)
	{
		super();
		this.client = aClient;
		this.putValue(Action.NAME, ResourceMgr.getString("MnuTxtFind"));
		this.putValue(Action.SMALL_ICON, ResourceMgr.getImage(ResourceMgr.IMG_FIND));
		this.putValue(WbAction.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_DATA);
		this.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getDescription("MnuTxtFind"));
		this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK));
		this.setCreateToolbarSeparator(true);
	}

	public void actionPerformed(ActionEvent e)
	{
		this.client.findData();
	}
}
