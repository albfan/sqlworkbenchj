package workbench.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.Action;

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
		window.disconnect(true, true);
	}
}
