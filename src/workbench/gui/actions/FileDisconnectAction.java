package workbench.gui.actions;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.lang.Runnable;
import javax.swing.Action;
import javax.swing.KeyStroke;
import workbench.gui.MainWindow;
import workbench.resource.ResourceMgr;

public class FileDisconnectAction extends WbAction
{
	private MainWindow window;

	public FileDisconnectAction(MainWindow aWindow)
	{
		super();
		this.window = aWindow;
		this.putValue(Action.NAME, ResourceMgr.getString("MnuTxtDisconnect"));
	}

	public void actionPerformed(ActionEvent e)
	{
		window.disconnect();
	}
}
