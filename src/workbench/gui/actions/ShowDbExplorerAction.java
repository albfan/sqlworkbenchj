/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import workbench.gui.MainWindow;
import workbench.resource.ResourceMgr;
import javax.swing.SwingUtilities;

/**
 *	Action to paste the contents of the clipboard into the entry field
 *	@author  workbench@kellerer.org
 */
public class ShowDbExplorerAction
	extends WbAction
{
	private MainWindow mainWin;
	public ShowDbExplorerAction(MainWindow aWindow)
	{
		super();
		mainWin = aWindow;
		this.initMenuDefinition("MnuTxtShowDbExplorer",KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_MASK));
		this.setIcon(ResourceMgr.getImage("Database"));
	}

	public void executeAction(ActionEvent e)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				mainWin.showDbExplorer();
			}
		});
	}
}