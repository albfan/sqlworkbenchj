/*
 * EditorPanel.java
 *
 * Created on November 25, 2001, 1:54 PM
 */

package workbench.gui.sql;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.io.*;
import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import workbench.WbManager;
import workbench.gui.actions.WbAction;
import workbench.gui.components.ExtensionFileFilter;
import workbench.gui.editor.AnsiSQLTokenMarker;
import workbench.gui.editor.JEditTextArea;
import workbench.gui.editor.SyntaxStyle;
import workbench.gui.editor.Token;
import workbench.gui.menu.TextPopup;
import workbench.interfaces.ClipboardSupport;
import workbench.interfaces.FontChangedListener;
import workbench.interfaces.TextContainer;
import workbench.interfaces.TextFileContainer;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.util.LineTokenizer;


/**
 *
 * @author  workbench@kellerer.org
 * @version
 */
public class EditorPanel 
	extends JEditTextArea 
	implements ClipboardSupport, FontChangedListener, 
						 TextContainer, TextFileContainer
{
	private TextPopup popup = new TextPopup(this);
	private AnsiSQLTokenMarker tokenMarker;
	private File currentFile;
	
	/** Creates new EditorPanel */
	public EditorPanel()
	{
		super();
		this.setDoubleBuffered(true);
		this.setFont(WbManager.getSettings().getEditorFont());
		this.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		
		SyntaxStyle[] styles = new SyntaxStyle[Token.ID_COUNT];

		styles[Token.COMMENT1] = new SyntaxStyle(Color.GRAY,true,false);
		styles[Token.COMMENT2] = new SyntaxStyle(Color.GRAY,true,false);
		styles[Token.KEYWORD1] = new SyntaxStyle(Color.BLUE,false,false);
		styles[Token.KEYWORD2] = new SyntaxStyle(Color.MAGENTA,false,false);
		styles[Token.KEYWORD3] = new SyntaxStyle(new Color(0x009600),false,false);
		styles[Token.LITERAL1] = new SyntaxStyle(new Color(0x650099),false,false);
		styles[Token.LITERAL2] = new SyntaxStyle(new Color(0x650099),false,true);
		styles[Token.LABEL] = new SyntaxStyle(new Color(0x990033),false,true);
		styles[Token.OPERATOR] = new SyntaxStyle(Color.BLACK,false,false);
		styles[Token.INVALID] = new SyntaxStyle(Color.RED,false,true);
		
		this.getPainter().setStyles(styles);
		
		this.addKeyBinding("C+C", this.popup.getCopyAction());
		this.addKeyBinding("C+INSERT", this.popup.getCopyAction());
		
		this.addKeyBinding("C+V", this.popup.getPasteAction());
		this.addKeyBinding("SHIFT+INSERT", this.popup.getPasteAction());
		
		this.addKeyBinding("C+X", this.popup.getCutAction());
		this.addKeyBinding("SHIFT+DELETE", this.popup.getCutAction());
		
		this.addKeyBinding("C+a", this.popup.getSelectAllAction());
		
		this.setTabSize(WbManager.getSettings().getEditorTabWidth());
		this.tokenMarker = new AnsiSQLTokenMarker();
		this.setTokenMarker(tokenMarker);
		this.setCaretBlinkEnabled(true);
		
		this.setRightClickPopup(popup);
		this.setMaximumSize(null);
		this.setPreferredSize(null);
		WbManager.getSettings().addFontChangedListener(this);
	}

	public void fontChanged(String aKey, Font aFont)
	{
		if (aKey.equals(Settings.EDITOR_FONT_KEY))
		{
			this.setFont(aFont);
		}
	}
	
	/**
	 *	Make the current selection suitable for a SQL IN statement with
	 *	character datatype.
	 *	e.g. 
	 *<pre>
	 *1234
	 *5678
	 *</pre>
	 *will be converted to 
	 *<pre>  
	 *('1234',
	 *'4456')
	 *</pre>
	 */
	public void makeInListForChar()
	{
		int startline = this.getSelectionStartLine();
		int endline = this.getSelectionEndLine();
		StringBuffer newText = new StringBuffer((endline - startline + 1) * 80);
		for (int i=startline; i <= endline; i++)
		{
			String line = this.getLineText(i);
			StringBuffer newline = new StringBuffer(line.length() + 10);
			if (line != null && line.length() > 0) 
			{
				if (i > startline) 
				{
					if (i < endline) newText.append(',');
					newText.append('\n');
					newline.append(' ');
				}
				else
				{
					newline.append("(");
				}
				newline.append('\'');
				newline.append(line);
				newline.append('\'');
			}
			if (i == endline)
			{
				newline.append(')');
			}
			newText.append(newline);
		}
		this.setSelectedText(newText.toString());
	}
	
	public void makeInListForNonChar()
	{
		String selectedText = this.getSelectedText();
		if (selectedText == null || selectedText.length() == 0) return;
		LineTokenizer tok = new LineTokenizer(selectedText, " \n");
		int tokens = tok.countTokens();
		StringBuffer newText = new StringBuffer(selectedText.length() + tokens * 4);
		boolean afterFirst = false;
		while (tok.hasMoreTokens())
		{
			String line = tok.nextToken();
			StringBuffer newline = new StringBuffer(line.length() + 10);
			if (afterFirst) 
			{
				newline.append(' ');
			}
			else
			{
				newline.append("(");
				afterFirst = true;
			}
			newline.append(line);
			if (tok.hasMoreTokens()) 
			{
				newline.append(',');
			}
			else
			{
				newline.append(')');
			}
			newText.append(newline);
			if (newText.length() > 40) newText.append('\n');
		}
		this.setSelectedText(newText.toString());
	}
	
	public AnsiSQLTokenMarker getSqlTokenMarker()
	{
		return this.tokenMarker;
	}
	
	public void addPopupMenuItem(WbAction anAction, boolean withSeparator)
	{
		if (withSeparator)
		{
			this.popup.addSeparator();
		}
		this.popup.add(anAction.getMenuItem());
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
		String text = this.getSelectedText();
		if (text == null || text.length() == 0)
			return this.getText();
		else
			return text;
	}
	
	public void clear()
	{
		this.setSelectedText("");
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
	
	public boolean closeFile()
	{
    if (this.currentFile == null) return false;
		this.currentFile = null;
		this.setCaretPosition(0);
		this.setText("");
		this.clearUndoBuffer();
		this.resetModified();
    return true;
	}
	
	public boolean openFile()
	{
		boolean result = false;
		String lastDir = WbManager.getSettings().getLastSqlDir();
		JFileChooser fc = new JFileChooser(lastDir);
		fc.addChoosableFileFilter(ExtensionFileFilter.getSqlFileFilter());
		int answer = fc.showOpenDialog(SwingUtilities.getWindowAncestor(this));
		if (answer == JFileChooser.APPROVE_OPTION)
		{
			result = this.readFile(fc.getSelectedFile());
			lastDir = fc.getCurrentDirectory().getAbsolutePath();
			WbManager.getSettings().setLastSqlDir(lastDir);
		}
		return result;
	}
	
	public boolean readFile(File aFile)
	{
		if (aFile == null) return false;
		if (!aFile.exists()) return false;
		if (aFile.length() > Integer.MAX_VALUE) 
		{
			WbManager.getInstance().showErrorMessage(this, ResourceMgr.getString("MsgFileTooBig"));
			return false;
		}
		boolean result = false;
		try
		{
			String filename = aFile.getAbsolutePath();
			BufferedReader reader = new BufferedReader(new FileReader(filename));
			StringBuffer content = new StringBuffer((int)aFile.length() + 500);
			this.setText("");
			String line = reader.readLine();
			while (line != null)
			{
				content.append(line);
				content.append('\n');
				line = reader.readLine();
			}
			this.setText(content.toString());
			reader.close();
			this.currentFile = aFile;
			result = true;
			this.clearUndoBuffer();
			this.resetModified();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return result;
	}
	
	public boolean saveCurrentFile()
	{
		if (this.currentFile != null)
		{
			return this.saveFile(this.currentFile);
		}
		else
		{
			return this.saveFile();
		}
	}
	
	public boolean saveFile()
	{
		boolean result = false;
		String lastDir = WbManager.getSettings().getLastSqlDir();
		JFileChooser fc = new JFileChooser(lastDir);
		fc.setSelectedFile(this.currentFile);
		fc.addChoosableFileFilter(ExtensionFileFilter.getSqlFileFilter());
		int answer = fc.showSaveDialog(SwingUtilities.getWindowAncestor(this));
		if (answer == JFileChooser.APPROVE_OPTION)
		{
			result = this.saveFile(fc.getSelectedFile());
			lastDir = fc.getCurrentDirectory().getAbsolutePath();
			WbManager.getSettings().setLastSqlDir(lastDir);
		}
		return result;
	}
	
	public boolean saveFile(File aFile)
	{
		try
		{
			String filename = aFile.getAbsolutePath();
			PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(filename)));
			int count = this.getLineCount();
			String line;
			int trimLen;
			for (int i=0; i < count; i++)
			{
				line = this.getLineText(i);
				if (line.endsWith("\r\n") || line.endsWith("\n\r"))
					trimLen = 2;
				else if (line.endsWith("\n") || line.endsWith("\r"))
					trimLen = 1;
				else
					trimLen = 0;

				if (trimLen > 0)
					writer.println(line.substring(0, line.length() - trimLen));
				else
					writer.println(line);
			}
			writer.close();
			this.currentFile = aFile;
			this.resetModified();
			return true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return false;
	}
	
	public File getCurrentFile() { return this.currentFile; }
	public String getCurrentFileName() 
	{ 
		if (this.currentFile == null) return null;
		return this.currentFile.getAbsolutePath(); 
	}
	
}
