package workbench.gui.actions;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.Action;
import javax.swing.KeyStroke;
import workbench.interfaces.TextContainer;
import workbench.resource.ResourceMgr;
import workbench.util.StringUtil;

public class CleanJavaCodeAction extends WbAction
{
	private TextContainer client;

	public CleanJavaCodeAction(TextContainer aClient)
	{
		super();
		this.client = aClient;
		this.putValue(Action.NAME, ResourceMgr.getString("MnuTxtCleanJavaCode"));
		this.putValue(WbAction.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_SQL);
		this.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getDescription("MnuTxtCleanJavaCode"));
		this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.ALT_MASK));
	}

	public void actionPerformed(ActionEvent e)
	{
		boolean selected = true;
		String code = this.client.getSelectedText();
		if (code == null || code.length() == 0)
		{
			code = this.client.getText();
			selected = false;
		}
		String sql = StringUtil.cleanJavaString(code);
		if (sql != null && sql.length() > 0)
		{
			if (selected)
				client.setSelectedText(sql);
			else
				client.setText(sql);
		}
	}
}
