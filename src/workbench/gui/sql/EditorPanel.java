/*
 * EditorPanel.java
 *
 * Created on November 25, 2001, 1:54 PM
 */

package workbench.gui.sql;

import javax.swing.border.Border;
import javax.swing.border.BevelBorder;
import javax.swing.BorderFactory;
import workbench.gui.editor.AnsiSQLTokenMarker;
import workbench.gui.editor.JEditTextArea;
import java.awt.Color;
import java.awt.Dimension;
import workbench.WbManager;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import workbench.interfaces.ClipboardSupport;
import workbench.gui.menu.TextPopup;
import javax.swing.Action;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.border.EtchedBorder;
import workbench.gui.actions.WbAction;
import workbench.gui.editor.SyntaxStyle;
import workbench.gui.editor.Token;

/**
 *
 * @author  thomas
 * @version
 */
public class EditorPanel 
	extends JEditTextArea
	implements ClipboardSupport
{
	private TextPopup popup = new TextPopup(this);
	/** Creates new EditorPanel */
	public EditorPanel()
	{
		super();
		this.setDoubleBuffered(true);
		this.setFont(WbManager.getSettings().getEditorFont());
		this.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		
		SyntaxStyle[] styles = new SyntaxStyle[Token.ID_COUNT];

		styles[Token.COMMENT1] = new SyntaxStyle(Color.gray,true,false);
		styles[Token.COMMENT2] = new SyntaxStyle(Color.gray,true,false);
		styles[Token.KEYWORD1] = new SyntaxStyle(Color.blue,false,false);
		styles[Token.KEYWORD2] = new SyntaxStyle(Color.magenta,false,false);
		styles[Token.KEYWORD3] = new SyntaxStyle(new Color(0x009600),false,false);
		styles[Token.LITERAL1] = new SyntaxStyle(new Color(0x650099),false,false);
		styles[Token.LITERAL2] = new SyntaxStyle(new Color(0x650099),false,true);
		styles[Token.LABEL] = new SyntaxStyle(new Color(0x990033),false,true);
		styles[Token.OPERATOR] = new SyntaxStyle(Color.black,false,false);
		styles[Token.INVALID] = new SyntaxStyle(Color.red,false,true);
		
		this.getPainter().setStyles(styles);
		
		this.addKeyBinding("C+C", this.popup.getCopyAction());
		this.addKeyBinding("C+INSERT", this.popup.getCopyAction());
		
		this.addKeyBinding("C+V", this.popup.getPasteAction());
		this.addKeyBinding("SHIFT+INSERT", this.popup.getPasteAction());
		
		this.addKeyBinding("C+X", this.popup.getCutAction());
		this.addKeyBinding("SHIFT+DELETE", this.popup.getCutAction());
		
		this.addKeyBinding("C+a", this.popup.getSelectAllAction());
		
		this.setTabSize(WbManager.getSettings().getEditorTabWidth());
		this.setTokenMarker(new AnsiSQLTokenMarker());
		this.setCaretBlinkEnabled(true);
		
		this.setRightClickPopup(popup);
		this.setMaximumSize(null);
		this.setPreferredSize(null);
	}
	
	public void addPopupMenuItem(Action anAction, boolean withSeparator)
	{
		if (withSeparator)
		{
			this.popup.addSeparator();
		}
		this.popup.add(anAction);
	}
	
	/**
	 *	Return the contents of the EditorPanel
	 */
	public String getStatement()
	{
		return this.getText();
	}
	
	/**
	 *	Return the selected text of the editor
	 */
	public String getSelectedStatement()
	{
		return this.getSelectedText();
	}
	
	public void clear()
	{
		this.setText("");
	}
	
	public void addKeyBinding(String aBinding, ActionListener aListener)
	{
		this.getInputHandler().addKeyBinding(aBinding, aListener);
	}
	
	public void addKeyBinding(WbAction anAction)
	{
		KeyStroke key = anAction.getAccelerator();
		if (key != null)
		{
			this.getInputHandler().addKeyBinding(key, anAction);
		}
	}

}
