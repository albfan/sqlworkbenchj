package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import workbench.interfaces.Undoable;
import workbench.resource.ResourceMgr;

/**
 *	@author  workbench@kellerer.org
 */
public class UndoAction extends WbAction
{
	private Undoable client;

	public UndoAction(Undoable aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtUndo",KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_MASK));
		this.setIcon(ResourceMgr.getImage("Undo"));
		this.setMenuItemName(ResourceMgr.MNU_TXT_EDIT);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.undo();
	}
}
