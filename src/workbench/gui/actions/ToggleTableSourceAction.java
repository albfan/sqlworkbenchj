package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import workbench.gui.sql.SqlPanel;
import workbench.resource.ResourceMgr;
import workbench.gui.dbobjects.TableListPanel;

/**
 *	@author  workbench@kellerer.org
 */
public class ToggleTableSourceAction extends WbAction
{
	private TableListPanel client;

	public ToggleTableSourceAction(TableListPanel aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtToggleTableSource", KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
		//this.setMenuItemName(ResourceMgr.MNU_TXT_VIEW);
		this.setIcon(null);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.toggleExpandSource();
	}
}