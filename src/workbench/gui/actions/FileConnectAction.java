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
import javax.swing.KeyStroke;
import workbench.gui.MainWindow;
import workbench.resource.ResourceMgr;

/**
 *	Action to paste the contents of the clipboard into the entry field
 *	@author  workbench@kellerer.org
 */
public class FileConnectAction extends WbAction
{
	private MainWindow window;

	public FileConnectAction(MainWindow aWindow)
	{
		super();
		this.window = aWindow;
		this.putValue(Action.NAME, ResourceMgr.getString(ResourceMgr.MNU_TXT_CONNECT));
		this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.ALT_MASK));
		this.putValue(Action.SMALL_ICON, ResourceMgr.getImage("Connect"));
	}

	public void actionPerformed(ActionEvent e)
	{
		this.window.selectConnection();
	}
}
