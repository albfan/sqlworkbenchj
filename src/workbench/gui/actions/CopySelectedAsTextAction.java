/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;

import workbench.interfaces.Exporter;
import workbench.resource.ResourceMgr;
import workbench.util.StringUtil;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	@author  workbench@kellerer.org
 */
public class CopySelectedAsTextAction extends WbAction
{
	private Exporter client;
	
	public CopySelectedAsTextAction(Exporter aClient)
	{
		super();
		this.client = aClient;
			
		this.setMenuItemName(ResourceMgr.MNU_TXT_COPY_SELECTED);
		String desc = ResourceMgr.getDescription("MnuTxtCopySelectedAsText");
		String shift = KeyEvent.getKeyModifiersText(KeyEvent.SHIFT_MASK);
		desc = StringUtil.replace(desc, "%shift%", shift);
		
		this.initMenuDefinition(ResourceMgr.getString("MnuTxtCopySelectedAsText"), desc, null);
		this.setIcon(null);
		this.setEnabled(false);
	}

	public void executeAction(ActionEvent e)
	{
		boolean shiftPressed = ((e.getModifiers() & ActionEvent.SHIFT_MASK) == ActionEvent.SHIFT_MASK);
		client.copyDataToClipboard(!shiftPressed, true); 
	}
}
