package workbench.gui.actions;

import java.awt.event.ActionEvent;

import workbench.gui.MainWindow;

public class FileDisconnectAction extends WbAction
{
	private MainWindow window;

	public FileDisconnectAction(MainWindow aWindow)
	{
		super();
		this.window = aWindow;
		this.initMenuDefinition("MnuTxtDisconnect");
	}

	public void executeAction(ActionEvent e)
	{
		window.disconnect(true, true);
	}
}
