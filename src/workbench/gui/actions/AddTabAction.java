package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import workbench.gui.MainWindow;
import workbench.resource.ResourceMgr;

/**
 *	@author  workbench@kellerer.org
 */
public class AddTabAction extends WbAction
{
	private MainWindow client;

	public AddTabAction(MainWindow aClient)
	{
		super();
		this.client = aClient;
		this.setMenuItemName(ResourceMgr.MNU_TXT_VIEW);
		this.initMenuDefinition("MnuTxtAddTab", KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_MASK));
		this.setIcon(null);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.addTab();
	}
}
