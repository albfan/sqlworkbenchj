/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.lang.Runnable;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;
import workbench.gui.components.WbTable;
import workbench.interfaces.ClipboardSupport;
import workbench.interfaces.Exporter;
import workbench.resource.ResourceMgr;
import workbench.util.StringUtil;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	@author  workbench@kellerer.org
 */
public class CopyAsSqlInsertAction extends WbAction
{
	private WbTable client;

	public CopyAsSqlInsertAction(WbTable aClient)
	{
		super();
		this.client = aClient;
		this.putValue(Action.NAME, ResourceMgr.getString("MnuTxtCopyAsSqlInsert"));
		this.putValue(WbAction.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_DATA);
		String desc = ResourceMgr.getDescription("MnuTxtCopyAsSqlInsert");
		this.putValue(Action.SHORT_DESCRIPTION, desc);
		this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_MASK));
	}

	public void actionPerformed(ActionEvent e)
	{
		client.copyAsSqlInsert();
	}

}
