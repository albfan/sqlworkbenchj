package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;
import workbench.gui.sql.SqlPanel;
import workbench.interfaces.TextFileContainer;
import workbench.resource.ResourceMgr;

/**
 *	@author  workbench@kellerer.org
 */
public class FileSaveAsAction extends WbAction
{
	private TextFileContainer client;

	public FileSaveAsAction(TextFileContainer aClient)
	{
		super();
		this.client = aClient;
		this.putValue(Action.NAME, ResourceMgr.getString("MnuTxtFileSaveAs"));
		this.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getDescription("MnuTxtFileSaveAs"));
		this.putValue(WbAction.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_FILE);
	}

	public void actionPerformed(ActionEvent e)
	{
		this.client.saveFile();
	}
}
