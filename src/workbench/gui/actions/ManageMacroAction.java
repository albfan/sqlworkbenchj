package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.KeyStroke;

import workbench.gui.MainWindow;
import workbench.gui.sql.SqlPanel;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.sql.MacroManager;

/**
 *	@author  workbench@kellerer.org
 */
public class ManageMacroAction extends WbAction
{
	private MainWindow client;

	public ManageMacroAction(MainWindow aClient)
	{
		super();
		this.client = aClient;
		this.putValue(Action.NAME, ResourceMgr.getString("MnuTxtManageMacros"));
		this.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getDescription("MnuTxtManageMacros"));
		this.putValue(WbAction.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_SQL);
		this.putValue(WbAction.SMALL_ICON, null);
		this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_MASK));
	}
	
	public void executeAction(ActionEvent e)
	{
		SqlPanel sql = this.client.getCurrentSqlPanel();
		if (sql != null)
		{	
			MacroManager.getInstance().selectAndRun(sql);
		}
		else
		{
			LogMgr.logWarning("ManageMacroAction.actionPerformed()", "Don't have a current SqlPanel!");
		}
	}

}
