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
import workbench.gui.sql.EditorPanel;
import workbench.gui.sql.SqlPanel;
import workbench.interfaces.ClipboardSupport;
import workbench.interfaces.Exporter;
import workbench.interfaces.TextSelectionListener;
import workbench.resource.ResourceMgr;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	@author  workbench@kellerer.org
 */
public class MakeLowerCaseAction extends WbAction
	implements TextSelectionListener
{
	private EditorPanel client;

	public MakeLowerCaseAction(EditorPanel aClient)
	{
		super();
		this.client = aClient;
		this.client.addSelectionListener(this);
		this.putValue(Action.NAME, ResourceMgr.getString("MnuTxtMakeLowerCase"));
		this.putValue(WbAction.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_EDIT);
		this.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getDescription("MnuTxtMakeLowerCase"));
		this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_L,KeyEvent.CTRL_MASK));
		this.setEnabled(false);
	}

	public void actionPerformed(ActionEvent e)
	{
		this.client.toLowerCase();
	}

	public void selectionChanged(int newStart, int newEnd)
	{
		this.setEnabled(newEnd > newStart);
	}

}
