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

/**
 *	Action to clear the contents of a entry field
 *	@author  workbench@kellerer.org
 */
public class CreateSnippetAction extends WbAction
{
	private TextContainer client;

	public CreateSnippetAction(TextContainer aClient)
	{
		super();
		this.client = aClient;
		this.putValue(Action.NAME, ResourceMgr.getString("MnuTxtCreateSnippet"));
		this.putValue(WbAction.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_SQL);
		this.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getDescription("MnuTxtCreateSnippet"));
		this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.ALT_MASK));
	}

	public void actionPerformed(ActionEvent e)
	{
		String sql = this.client.getText();
		String code = StringUtil.makeJavaString(sql);
		Clipboard clp = Toolkit.getDefaultToolkit().getSystemClipboard();
		StringSelection sel = new StringSelection(code);
		clp.setContents(sel, sel);
	}
}
