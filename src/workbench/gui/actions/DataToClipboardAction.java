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
import workbench.interfaces.ClipboardSupport;
import workbench.interfaces.Exporter;
import workbench.resource.ResourceMgr;
import workbench.util.StringUtil;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	@author  sql.workbench@freenet.de
 */
public class DataToClipboardAction extends WbAction
{
	private Exporter client;

	public DataToClipboardAction(Exporter aClient)
	{
		super();
		this.client = aClient;
		this.putValue(Action.NAME, ResourceMgr.getString("MnuTxtDataToClipboard"));
		this.putValue(WbAction.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_DATA);
		String desc = ResourceMgr.getDescription("MnuTxtDataToClipboard");
		String shift = KeyEvent.getKeyModifiersText(KeyEvent.SHIFT_MASK);
		desc = StringUtil.replace(desc, "%s", shift);
		this.putValue(Action.SHORT_DESCRIPTION, desc);
		this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_MASK));
	}

	public void actionPerformed(ActionEvent e)
	{
		final boolean shiftPressed = ((e.getModifiers() & e.SHIFT_MASK) == e.SHIFT_MASK);
		
		EventQueue.invokeLater(new Runnable()
		{ 
			public void run()
			{ 
				client.copyDataToClipboard(!shiftPressed); 
			} 
		});
	}
	
	public void addToInputMap(InputMap im, ActionMap am)
	{
		super.addToInputMap(im, am);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK), this.getActionName());
	}
}
