package workbench.gui.actions;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import workbench.WbManager;
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
		this.initMenuDefinition("MnuTxtCreateSnippet", KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.ALT_MASK));
		this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
	}

	public void executeAction(ActionEvent e)
	{
		String sql = this.client.getSelectedText();
		if (sql == null || sql.length() == 0)
		{
			sql = this.client.getText();
		}
		String code = StringUtil.makeJavaString(sql, WbManager.getSettings().getIncludeNewLineInCodeSnippet());
		Clipboard clp = Toolkit.getDefaultToolkit().getSystemClipboard();
		StringSelection sel = new StringSelection(code);
		clp.setContents(sel, sel);
	}
}
