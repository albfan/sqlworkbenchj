package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.KeyStroke;

import workbench.interfaces.Commitable;
import workbench.resource.ResourceMgr;

/**
 *	Action to clear the contents of a entry field
 *	@author  workbench@kellerer.org
 */
public class CommitAction extends WbAction
{
	private Commitable client;
	
	public CommitAction(Commitable aClient)
	{
		super();
		this.client = aClient;
		this.setMenuTextByKey("MnuTxtCommit");
		this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
		this.setIcon(ResourceMgr.getImage("Commit"));
		this.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.ALT_MASK));
	}

	public void executeAction(ActionEvent e)
	{
		if (this.client != null) this.client.commit();
	}
	
}
