/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import workbench.gui.sql.EditorPanel;
import workbench.interfaces.TextSelectionListener;
import workbench.resource.ResourceMgr;

/**
 *	Action to enable column selection for the next selection in the editor
 *	@author  workbench@kellerer.org
 */
public class ColumnSelectionAction extends WbAction
{
	private EditorPanel client;

	public ColumnSelectionAction(EditorPanel aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtColumnSelection", KeyStroke.getKeyStroke(KeyEvent.VK_Q,KeyEvent.ALT_MASK));
		this.setMenuItemName(ResourceMgr.MNU_TXT_EDIT);
		this.setEnabled(true);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.enableColumnSelection();
	}


}