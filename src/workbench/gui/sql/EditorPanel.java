/*
 * EditorPanel.java
 *
 * Created on November 25, 2001, 1:54 PM
 */

package workbench.gui.sql;

import java.awt.Color;
import java.awt.Font;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.filechooser.FileFilter;

import workbench.WbManager;
import workbench.exception.ExceptionUtil;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.ColumnSelectionAction;
import workbench.gui.actions.FileOpenAction;
import workbench.gui.actions.FileSaveAsAction;
import workbench.gui.actions.FindAction;
import workbench.gui.actions.FindAgainAction;
import workbench.gui.actions.FormatSqlAction;
import workbench.gui.actions.ReplaceAction;
import workbench.gui.actions.WbAction;
import workbench.gui.components.ExtensionFileFilter;
import workbench.gui.components.ReplacePanel;
import workbench.gui.components.SearchCriteriaPanel;
import workbench.gui.editor.AnsiSQLTokenMarker;
import workbench.gui.editor.JEditTextArea;
import workbench.gui.editor.SyntaxStyle;
import workbench.gui.editor.SyntaxDocument;
import workbench.gui.editor.Token;
import workbench.gui.editor.TokenMarker;
import workbench.interfaces.ClipboardSupport;
import workbench.interfaces.FontChangedListener;
import workbench.interfaces.FormattableSql;
import workbench.interfaces.Replaceable;
import workbench.interfaces.Searchable;
import workbench.interfaces.TextContainer;
import workbench.interfaces.TextFileContainer;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.sql.ScriptParser;
import workbench.sql.formatter.SqlFormatter;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.gui.actions.MatchBracketAction;
import workbench.gui.editor.SyntaxDocument;
import workbench.interfaces.FilenameChangeListener;


/**
 *
 * @author  workbench@kellerer.org
 * @version
 */
public class EditorPanel
	extends JEditTextArea
	implements ClipboardSupport, FontChangedListener, PropertyChangeListener, DropTargetListener,
						 TextContainer, TextFileContainer, Replaceable, Searchable, FormattableSql
{
	private static final Border DEFAULT_BORDER = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
	private AnsiSQLTokenMarker sqlTokenMarker;
	private File currentFile;
	private static final int SQL_EDITOR = 0;
	private static final int JAVA_EDITOR = 1;
	private static final int TEXT_EDITOR = 2;
	private int editorType;
	private String lastSearchCriteria;

	private FindAction findAction;
	private FindAgainAction findAgainAction;
	private ReplaceAction replaceAction;
	private FormatSqlAction formatSql;
	private FileOpenAction fileOpen;
	private ColumnSelectionAction columnSelection;
	private MatchBracketAction matchBracket;
	
	private List filenameChangeListeners;

  private static final SyntaxStyle[] SYNTAX_COLORS;
  static
  {
		SYNTAX_COLORS = new SyntaxStyle[Token.ID_COUNT];

		SYNTAX_COLORS[Token.COMMENT1] = new SyntaxStyle(Color.GRAY,true,false);
		SYNTAX_COLORS[Token.COMMENT2] = new SyntaxStyle(Color.GRAY,true,false);
		SYNTAX_COLORS[Token.KEYWORD1] = new SyntaxStyle(Color.BLUE,false,false);
		SYNTAX_COLORS[Token.KEYWORD2] = new SyntaxStyle(Color.MAGENTA,false,false);
		SYNTAX_COLORS[Token.KEYWORD3] = new SyntaxStyle(new Color(0x009600),false,false);
		SYNTAX_COLORS[Token.LITERAL1] = new SyntaxStyle(new Color(0x650099),false,false);
		SYNTAX_COLORS[Token.LITERAL2] = new SyntaxStyle(new Color(0x650099),false,true);
		SYNTAX_COLORS[Token.LABEL] = new SyntaxStyle(new Color(0x990033),false,true);
		SYNTAX_COLORS[Token.OPERATOR] = new SyntaxStyle(Color.BLACK,false,false);
		SYNTAX_COLORS[Token.INVALID] = new SyntaxStyle(Color.RED,false,true);
  }

	public static EditorPanel createSqlEditor()
	{
		AnsiSQLTokenMarker sql = new AnsiSQLTokenMarker();
		EditorPanel p = new EditorPanel(sql);
		p.editorType = SQL_EDITOR;
		p.sqlTokenMarker = sql;
		return p;
	}

	public static EditorPanel createTextEditor()
	{
		EditorPanel p = new EditorPanel(null);
		p.editorType = TEXT_EDITOR;
		return p;
	}

	private EditorPanel()
	{
		this(null);
	}

	public EditorPanel(TokenMarker aMarker)
	{
		super();
		this.setDoubleBuffered(true);
		this.setFont(WbManager.getSettings().getEditorFont());
		this.setBorder(DEFAULT_BORDER);

		this.getPainter().setStyles(SYNTAX_COLORS);

		this.setTabSize(WbManager.getSettings().getEditorTabWidth());
		this.setCaretBlinkEnabled(true);
		this.addPopupMenuItem(new FileSaveAsAction(this), true);
		this.fileOpen = new FileOpenAction(this);
		this.addPopupMenuItem(this.fileOpen, false);

		this.findAction = new FindAction(this);
		this.findAction.setEnabled(true);
		this.addKeyBinding(this.findAction);

		this.findAgainAction = new FindAgainAction(this);
		this.findAgainAction.setEnabled(false);
		this.addKeyBinding(this.findAgainAction);

		this.replaceAction = new ReplaceAction(this);
		this.addKeyBinding(this.replaceAction);

		if (aMarker != null) this.setTokenMarker(aMarker);

		this.setMaximumSize(null);
		this.setPreferredSize(null);
		this.setShowLineNumbers(WbManager.getSettings().getShowLineNumbers());

		this.columnSelection = new ColumnSelectionAction(this);
		this.matchBracket = new MatchBracketAction(this);
		this.addKeyBinding(this.matchBracket);

		//this.setSelectionRectangular(true);
		WbManager.getSettings().addFontChangedListener(this);
		WbManager.getSettings().addChangeListener(this);
		
		new DropTarget(this, DnDConstants.ACTION_COPY, this);
	}

	public void setFileOpenAction(FileOpenAction anAction)
	{
		this.fileOpen = anAction;
	}
	
	public void fontChanged(String aKey, Font aFont)
	{
		if (aKey.equals(Settings.EDITOR_FONT_KEY))
		{
			this.setFont(aFont);
		}
	}
	public AnsiSQLTokenMarker getSqlTokenMarker()
	{
		return this.sqlTokenMarker;
	}

	public void showFindOnPopupMenu()
	{
		this.addPopupMenuItem(this.findAction, true);
		this.addPopupMenuItem(this.findAgainAction, false);
		this.addPopupMenuItem(this.replaceAction, false);
	}

	public MatchBracketAction getMatchBracketAction()
	{
		return this.matchBracket;
	}

	public ColumnSelectionAction getColumnSelection()
	{
		return this.columnSelection;
	}

	public FormatSqlAction getFormatSqlAction()
	{
		return this.formatSql;
	}

	public void showFormatSql()
	{
		if (this.formatSql != null) return;
		this.formatSql = new FormatSqlAction(this);
		this.addKeyBinding(this.formatSql);
		this.addPopupMenuItem(this.formatSql, true);
	}

	public void setEditable(boolean editable)
	{
		super.setEditable(editable);
		this.replaceAction.setEnabled(editable);
		this.fileOpen.setEnabled(false);
	}

	public void reformatSql()
	{
		String sql = this.getSelectedStatement();
		ScriptParser parser = new ScriptParser();
		parser.setAlternateDelimiter(WbManager.getSettings().getAlternateDelimiter());
		parser.setScript(sql);
		List commands = parser.getCommands();
		String delimit = parser.getDelimiter();

		int count = commands.size();
		StringBuffer newSql = new StringBuffer(sql.length() + 100);
		String formattedDelimit = "";

		if (count > 1)
		{
			formattedDelimit = "\n" + delimit + "\n\n";
		}
		else if (sql.endsWith("\n") || sql.endsWith("\r"))
		{
			formattedDelimit = delimit + "\n";
		}
		else
		{
			formattedDelimit = delimit;
		}

		for (int i=0; i < count; i++)
		{
			String command = (String)commands.get(i);
			SqlFormatter f = new SqlFormatter(command, WbManager.getSettings().getMaxSubselectLength());
			try
			{
				String formattedSql = f.format().trim();
				newSql.append(formattedSql);
				if (command.trim().endsWith(delimit))
				{
					newSql.append(formattedDelimit);
				}
				else
				{
					newSql.append("\n");
				}
			}
			catch (Exception e)
			{
			}
		}

		if (newSql.length() == 0) return;
		int caret = -1;

		if (this.isTextSelected())
		{
			caret = this.getSelectionStart();
			this.setSelectedText(newSql.toString());
			this.select(caret, caret + newSql.length());
		}
		else
		{
			caret = this.getCaretPosition();
			this.setText(newSql.toString());
			if (caret > 0 && caret < this.getText().length()) this.setCaretPosition(caret);
		}

	}

	/**
	 * 	Enable column selection for the next selection.
	 */
	public void enableColumnSelection()
	{
		this.setSelectionRectangular(true);
	}


	/**
	 *	Change the currently selected so that it can be used for a SQL IN statement with
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
		this.makeInList(true);
	}


	public void makeInListForNonChar()
	{
		this.makeInList(false);
	}

	private void makeInList(boolean quoteElements)
	{
		int startline = this.getSelectionStartLine();
		int endline = this.getSelectionEndLine();
		int count = (endline - startline + 1);
		StringBuffer newText = new StringBuffer(count * 80);
		for (int i=startline; i <= endline; i++)
		{
			String line = this.getLineText(i);

			// make sure at least one character from the line is selected
			// if the selection does not extend into the line, then
			// the line is ignored. This can happen with the last line
			int pos = this.getSelectionEnd(i) - this.getLineStartOffset(i);

			StringBuffer newline = new StringBuffer(line.length() + 10);
			if (pos > 0 && line != null && line.length() > 0)
			{
				if (i > startline)
				{
					newText.append(',');
					if ( (quoteElements && count > 5) || (!quoteElements && count > 15)) newText.append('\n');
					newline.append(' ');
				}
				else
				{
					newline.append("(");
				}
				if (quoteElements) newline.append('\'');
				newline.append(line);
				if (quoteElements) newline.append('\'');
			}
			if (i == endline)
			{
				newline.append(')');
			}
			newText.append(newline);
		}
		int pos = this.getSelectionEnd(endline) - this.getLineStartOffset(endline);
		if (pos == 0) newText.append("\n");
		this.setSelectedText(newText.toString());
	}

	public void addPopupMenuItem(WbAction anAction, boolean withSeparator)
	{
		if (withSeparator)
		{
			this.popup.addSeparator();
		}
		this.popup.add(anAction.getMenuItem());
		this.addKeyBinding(anAction);
	}

	/**
	 *	Return the contents of the EditorPanel
	 */
	public String getStatement()
	{
		return this.getText();
	}

	public void dispose()
	{
		this.clearUndoBuffer();
		this.popup.removeAll();
		this.popup = null;
		this.setDocument(new SyntaxDocument());
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

	public boolean closeFile(boolean clearText)
	{
    if (this.currentFile == null) return false;
		this.currentFile = null;
		if (clearText)
		{
			this.setCaretPosition(0);
			this.setText("");
			this.clearUndoBuffer();
		}
		this.resetModified();
    return true;
	}

	public void fireFilenameChanged(String aNewName)
	{
		if (this.filenameChangeListeners == null) return;
		for (int i=0; i < this.filenameChangeListeners.size(); i++)
		{
			FilenameChangeListener l = (FilenameChangeListener)this.filenameChangeListeners.get(i);
			l.fileNameChanged(this, aNewName);
		}
	}

	public void addFilenameChangeListener(FilenameChangeListener aListener)
	{
		if (aListener == null) return;
		if (this.filenameChangeListeners == null) this.filenameChangeListeners = new ArrayList();
		this.filenameChangeListeners.add(aListener);
	}

	public void removeFilenameChangeListener(FilenameChangeListener aListener)
	{
		if (aListener == null) return;
		if (this.filenameChangeListeners == null) return;
		this.filenameChangeListeners.remove(aListener);
	}
	
	public boolean openFile()
	{
		boolean result = false;
		String oldFile = this.getCurrentFileName();
		if (!this.canCloseFile())
		{
			this.requestFocusInWindow();
			return false;
		}
		
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

	public boolean hasFileLoaded()
	{
		String file = this.getCurrentFileName();
		return (file != null) && (file.length() > 0);
	}
	
	public boolean canCloseFile()
	{
		if (!this.hasFileLoaded()) return true;
		boolean result = true;
		if (this.isModified())
		{
			String filename = this.getCurrentFileName().replaceAll("\\\\", "\\\\\\\\");
			String msg = ResourceMgr.getString("MsgConfirmUnsavedEditorFile").replaceAll("%filename%", filename);
			int choice = WbSwingUtilities.getYesNoCancel(this, msg);
			if (choice == JOptionPane.YES_OPTION)
			{
				this.saveCurrentFile();
			}
			result = (choice != JOptionPane.CANCEL_OPTION);
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
			this.setText(""); // clear memory!
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
			this.fireFilenameChanged(filename);
		}
		catch (IOException e)
		{
			LogMgr.logError("EditorPanel.readFile()", "Error reading file " + aFile.getAbsolutePath(), e);
		}
		catch (OutOfMemoryError mem)
		{
			mem.printStackTrace();
			WbManager.getInstance().showErrorMessage(this, ResourceMgr.getString("MsgOutOfMemoryError"));
		}
		this.setCaretPosition(0);
		return result;
	}

	public boolean saveCurrentFile()
	{
		boolean result = false;
		try
		{
			if (this.currentFile != null)
			{
				this.saveFile(this.currentFile);
				result = true;
			}
			else
			{
				this.saveFile();
			}
		}
		catch (IOException e)
		{
			result = false;
		}
		return result;
	}

	public boolean saveFile()
	{
		boolean result = false;
		String lastDir;
		FileFilter ff = null;
		if (this.editorType == SQL_EDITOR)
		{
			lastDir = WbManager.getSettings().getLastSqlDir();
			ff = ExtensionFileFilter.getSqlFileFilter();
		}
		else if (this.editorType == JAVA_EDITOR)
		{
			lastDir = WbManager.getSettings().getLastJavaDir();
			ff = ExtensionFileFilter.getJavaFileFilter();
		}
		else
		{
			lastDir = WbManager.getSettings().getLastEditorDir();
			ff = ExtensionFileFilter.getTextFileFilter();
		}
		JFileChooser fc = new JFileChooser(lastDir);
		fc.setSelectedFile(this.currentFile);
		fc.addChoosableFileFilter(ff);
		int answer = fc.showSaveDialog(SwingUtilities.getWindowAncestor(this));
		if (answer == JFileChooser.APPROVE_OPTION)
		{
			try
			{
				this.saveFile(fc.getSelectedFile());
	      this.fireFilenameChanged(this.getCurrentFileName());
				lastDir = fc.getCurrentDirectory().getAbsolutePath();
				if (this.editorType == SQL_EDITOR)
				{
					WbManager.getSettings().setLastSqlDir(lastDir);
				}
				else if (this.editorType == JAVA_EDITOR)
				{
					WbManager.getSettings().setLastJavaDir(lastDir);
				}
				else
				{
					WbManager.getSettings().setLastEditorDir(lastDir);
				}
			}
			catch (IOException e)
			{
				WbSwingUtilities.showErrorMessage(this, ResourceMgr.getString("ErrorSavingFile") + "\n" + ExceptionUtil.getDisplay(e));
				result = false;
			}
		}
		return result;
	}

	public void saveFile(File aFile)
		throws IOException
	{
		try
		{
			String filename = aFile.getAbsolutePath();
			int pos = filename.indexOf('.');
			if (pos < 0)
			{
				filename = filename + ".sql";
				aFile = new File(filename);
			}
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
		}
		catch (IOException e)
		{
			LogMgr.logError("EditorPanel.saveFile()", "Error saving file", e);
			throw e;
		}
	}

	public File getCurrentFile() { return this.currentFile; }
	public String getCurrentFileName()
	{
		if (this.currentFile == null) return null;
		return this.currentFile.getAbsolutePath();
	}

	public FindAction getFindAction() { return this.findAction; }
	public FindAgainAction getFindAgainAction() { return this.findAgainAction; }

	public ReplaceAction getReplaceAction() { return this.replaceAction; }

	public int find()
	{
		String crit = this.lastSearchCriteria;
		if (crit == null) crit = this.getSelectedText();
		SearchCriteriaPanel p = new SearchCriteriaPanel(crit);
		boolean doFind = p.showFindDialog(this);
		if (!doFind) return -1;
		String criteria = p.getCriteria();
		boolean ignoreCase = p.getIgnoreCase();
		boolean wholeWord = p.getWholeWordOnly();
		boolean useRegex = p.getUseRegex();
		int pos = this.findText(criteria, ignoreCase, wholeWord, useRegex);
		this.lastSearchCriteria = criteria;
		this.findAgainAction.setEnabled(pos > -1);
		return pos;
	}

	public int findNext()
	{
		return this.findNextText();
	}

	public int findFirst(String aValue, boolean ignoreCase, boolean wholeWord, boolean useRegex)
	{
		int pos = this.findText(aValue, ignoreCase, wholeWord, useRegex);
		return pos;
	}

	public int find(String aValue, boolean ignoreCase, boolean wholeWord, boolean useRegex)
	{
		if (this.isCurrentSearchCriteria(aValue, ignoreCase, wholeWord, useRegex))
		{
			return this.findNext();
		}
		else
		{
			return this.findFirst(aValue, ignoreCase, wholeWord, useRegex);
		}
	}
	private ReplacePanel replacePanel = null;

	public void replace()
	{
		if (this.replacePanel == null)
		{

			this.replacePanel = new ReplacePanel(this);
		}
		this.replacePanel.showReplaceDialog(this, this.getSelectedText());
	}

	/**
	 *	Find and replace the next occurance of the current search string
	 */
	public boolean replaceNext(String aReplacement)
	{
		int pos = this.findNext();
		if (pos > -1)
		{
			this.setSelectedText(aReplacement);
		}
		return (pos > -1);
	}

	public boolean isTextSelected()
	{
		int selStart = this.getSelectionStart();
		int selEnd = this.getSelectionEnd();
		return (selStart > -1 && selEnd > selStart);
	}
	public int replaceAll(String value, String replacement, boolean selectedText, boolean ignoreCase, boolean wholeWord, boolean useRegex)
	{
		String old = null;
		if (selectedText)
		{
			old = this.getSelectedText();
		}
		else
		{
			old = this.getText();
		}
		int cursor = this.getCaretPosition();
		int selStart = this.getSelectionStart();
		int selEnd = this.getSelectionEnd();
		int newLen = -1;
		String regex = this.getSearchExpression(value, ignoreCase, wholeWord, useRegex);
		String newText = old.replaceAll(regex, replacement);
		if (selectedText)
		{
			this.setSelectedText(newText);
			newLen = this.getText().length();
		}
		else
		{
			this.setText(newText);
			newLen = this.getText().length();
			selStart = -1;
			selEnd = -1;
		}
		if (cursor < newLen)
		{
			this.setCaretPosition(cursor);
		}
		if (selStart > -1 && selEnd > selStart && selStart < newLen && selEnd < newLen)
		{
			this.select(selStart, selEnd);
		}
		return 0;
	}

	public boolean replaceCurrent(String aReplacement)
	{
		if (this.searchPatternMatchesSelectedText())
		{
			this.setSelectedText(aReplacement);
			return true;
		}
		else
		{
			if (this.findNext() > -1)
			{
				this.setSelectedText(aReplacement);
				return true;
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent evt)
	{
		if (Settings.PROPERTY_SHOW_LINE_NUMBERS.equals(evt.getPropertyName()))
		{
			this.setShowLineNumbers(WbManager.getSettings().getShowLineNumbers());
			this.repaint();
		}
	}

	public void dragEnter(java.awt.dnd.DropTargetDragEvent dropTargetDragEvent)
	{
		if (this.isEditable())
		{
			dropTargetDragEvent.acceptDrag (DnDConstants.ACTION_COPY);
		}
		else
		{
			dropTargetDragEvent.rejectDrag();
		}
	}
	
	public void dragExit(java.awt.dnd.DropTargetEvent dropTargetEvent)
	{
	}
	
	public void dragOver(java.awt.dnd.DropTargetDragEvent dropTargetDragEvent)
	{
	}
	
	public void drop(java.awt.dnd.DropTargetDropEvent dropTargetDropEvent)
	{
		try
		{
			Transferable tr = dropTargetDropEvent.getTransferable();
			if (tr.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
			{
				dropTargetDropEvent.acceptDrop(DnDConstants.ACTION_COPY);
				java.util.List fileList = (java.util.List)tr.getTransferData(DataFlavor.javaFileListFlavor);
				if (fileList != null && fileList.size() == 1)
				{
					File file = (File)fileList.get(0);
					if (this.canCloseFile())
					{
						this.readFile(file);
						dropTargetDropEvent.getDropTargetContext().dropComplete(true);
					}
					else
					{
						dropTargetDropEvent.getDropTargetContext().dropComplete(false);
					}
				}
				else
				{
					dropTargetDropEvent.getDropTargetContext().dropComplete(false);
					final Window w = SwingUtilities.getWindowAncestor(this);
					SwingUtilities.invokeLater(new Runnable()
					{
						public void run()
						{
							w.toFront();
							w.requestFocus();
							WbSwingUtilities.showErrorMessage(w, ResourceMgr.getString("ErrorNoMultipleDrop"));
						}
					});
				}
			} 
			else
			{
				dropTargetDropEvent.rejectDrop();
			}
		} 
		catch (IOException io)
		{
			io.printStackTrace();
			dropTargetDropEvent.rejectDrop();
		} 
		catch (UnsupportedFlavorException ufe)
		{
			ufe.printStackTrace();
			dropTargetDropEvent.rejectDrop();
		}
	}
	
	
	public void dropActionChanged(java.awt.dnd.DropTargetDragEvent dropTargetDragEvent)
	{
	}
	
}