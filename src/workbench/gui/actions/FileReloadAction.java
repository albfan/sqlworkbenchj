package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;

import workbench.gui.sql.SqlPanel;
import workbench.resource.ResourceMgr;
import workbench.util.StringUtil;

public class FileReloadAction extends WbAction
{
	private SqlPanel client;

	public FileReloadAction(SqlPanel aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtFileReload", KeyStroke.getKeyStroke(KeyEvent.VK_F5,KeyEvent.SHIFT_MASK));
		this.setMenuItemName(ResourceMgr.MNU_TXT_FILE);
		this.setEnabled(aClient.hasFileLoaded());
	}

	public void executeAction(ActionEvent e)
	{
		this.client.reloadFile();
	}
}