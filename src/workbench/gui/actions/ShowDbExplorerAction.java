/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import java.awt.Cursor;
import java.awt.EventQueue;
import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import javax.swing.Action;
import workbench.resource.ResourceMgr;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.lang.Runnable;
import javax.swing.KeyStroke;
import workbench.WbManager;
import workbench.gui.MainWindow;

/**
 *	Action to paste the contents of the clipboard into the entry field
 *	@author  sql.workbench@freenet.de
 */
public class ShowDbExplorerAction extends WbAction
{
	private MainWindow mainWin;
	public ShowDbExplorerAction(MainWindow aWindow)
	{
		super();
		mainWin = aWindow;
		this.putValue(Action.NAME, ResourceMgr.getString("MnuTxtShowDbExplorer"));
		this.putValue(Action.SMALL_ICON, ResourceMgr.getImage("Database"));
		this.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getDescription("MnuTxtShowDbExplorer"));
		this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_MASK));
	}

	public void actionPerformed(ActionEvent e)
	{
		EventQueue.invokeLater(new Runnable() 
		{
			public void run()
			{
				mainWin.showDbExplorer();
			}
		});
	}
}
