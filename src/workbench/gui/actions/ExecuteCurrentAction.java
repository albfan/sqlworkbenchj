package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;
import workbench.gui.sql.SqlPanel;

import workbench.resource.ResourceMgr;

/**
 *	@author  workbench@kellerer.org
 */
public class ExecuteCurrentAction extends WbAction
{
	private SqlPanel target;
	
	public ExecuteCurrentAction(SqlPanel aPanel)
	{
		super();
		this.target = aPanel;
		this.initMenuDefinition("MnuTxtExecuteCurrent", KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_MASK));
		this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
		this.putValue(WbAction.ADD_TO_TOOLBAR, "false");
	}

	public void executeAction(ActionEvent e)
	{
		this.target.runCurrentStatement();
	}
	
}
