package workbench.gui.actions;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;

import javax.swing.Action;

import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.sql.EditorPanel;
import workbench.resource.ResourceMgr;
import workbench.sql.MacroManager;

/**
 *	@author  workbench@kellerer.org
 */
public class AddMacroAction extends WbAction
{
	private EditorPanel client;

	public AddMacroAction(EditorPanel aClient)
	{
		super();
		this.client = aClient;
		this.putValue(Action.NAME, ResourceMgr.getString("MnuTxtAddMacro"));
		this.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getDescription("MnuTxtAddMacro"));
		this.putValue(WbAction.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_SQL);
	}

	public void actionPerformed(ActionEvent e)
	{
		String text = client.getSelectedText();
		if (text == null || text.trim().length() == 0) 
		{
			Toolkit.getDefaultToolkit().beep();
			return;
		}
		
		String name = WbSwingUtilities.getUserInput(client, ResourceMgr.getString("TxtGetMacroNameWindowTitle"), ResourceMgr.getString("TxtEmptyMacroName"));
		if (name != null)
		{
			MacroManager.getInstance().setMacro(name, text);
		}
	}
}
