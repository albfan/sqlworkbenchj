/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import javax.swing.Action;
import workbench.resource.ResourceMgr;
import java.awt.event.ActionListener;
import workbench.WbManager;
import workbench.gui.display.MainWindow;

/**
 *	Action to paste the contents of the clipboard into the entry field
 *	@author  thomas.kellerer@web.de
 */
public class FileConnectAction extends AbstractAction
{
	private MainWindow window;
	
	public FileConnectAction(MainWindow aWindow)
	{
		this.window = aWindow;
		this.putValue(Action.NAME, ResourceMgr.getString(ResourceMgr.MNU_TXT_CONNECT));
	}
	
	public void actionPerformed(ActionEvent e)
	{
		this.window.selectConnection();
	}
}
