package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import workbench.gui.MainWindow;
import workbench.resource.ResourceMgr;

public class FileConnectAction extends WbAction
{
	private MainWindow window;

	public FileConnectAction(MainWindow aWindow)
	{
		super();
		this.window = aWindow;
		this.initMenuDefinition(ResourceMgr.MNU_TXT_CONNECT, KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.ALT_MASK));
	}

	public void executeAction(ActionEvent e)
	{
		this.window.selectConnection();
	}
}
