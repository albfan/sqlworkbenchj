package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.KeyStroke;

import workbench.gui.WbSwingUtilities;
import workbench.gui.sql.SqlPanel;
import workbench.resource.ResourceMgr;
import workbench.sql.MacroManager;

/**
 *	@author  workbench@kellerer.org
 */
public class RunMacroAction extends WbAction
{
	private SqlPanel client;
	private String macroName;

	public RunMacroAction(SqlPanel aClient)
	{
		super();
		this.client = aClient;
		this.macroName = null;
		this.putValue(Action.NAME, ResourceMgr.getString("MnuTxtManageMacros"));
		this.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getDescription("MnuTxtManageMacros"));
		this.putValue(WbAction.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_SQL);
		this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_MASK));
		this.putValue(Action.SMALL_ICON, null);
	}
	
	public RunMacroAction(SqlPanel aClient, String aName)
	{
		super();
		this.client = aClient;
		this.macroName = aName;
		this.putValue(Action.NAME, aName);
		this.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getDescription("MnuTxtRunMacro") + " " + aName);
		this.putValue(WbAction.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_SQL);
		this.putValue(Action.SMALL_ICON, null);
	}

	public void actionPerformed(ActionEvent e)
	{
		if (this.client != null)
		{
			if (this.macroName == null)
			{
				MacroManager.getInstance().selectAndRun(this.client);
			}
			else
			{
				client.executeMacro(macroName);
			}
		}
	}

}
