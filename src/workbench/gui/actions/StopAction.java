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
import workbench.resource.ResourceMgr;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	@author  thomas.kellerer@web.de
 */
public class StopAction extends AbstractAction
{
	public StopAction()
	{
		this.putValue(Action.NAME, ResourceMgr.getString(ResourceMgr.TXT_STOP_STMT));
		this.putValue(Action.SMALL_ICON, ResourceMgr.getImage(ResourceMgr.IMG_STOP));
	}

	public void actionPerformed(ActionEvent e)
	{
	}
}
