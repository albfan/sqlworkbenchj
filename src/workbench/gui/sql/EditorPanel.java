/*
 * EditorPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.sql;

import java.awt.Component;
import java.awt.EventQueue;
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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.GapContent;
import workbench.WbManager;

import workbench.db.WbConnection;
import workbench.gui.actions.FileSaveAction;
import workbench.gui.editor.SyntaxUtilities;
import workbench.interfaces.EncodingSelector;
import workbench.sql.DelimiterDefinition;
import workbench.util.EncodingUtil;
import workbench.util.ExceptionUtil;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.ColumnSelectionAction;
import workbench.gui.actions.CommentAction;
import workbench.gui.actions.FileOpenAction;
import workbench.gui.actions.FileReloadAction;
import workbench.gui.actions.FileSaveAsAction;
import workbench.gui.actions.FindAction;
import workbench.gui.actions.FindAgainAction;
import workbench.gui.actions.FormatSqlAction;
import workbench.gui.actions.MatchBracketAction;
import workbench.gui.actions.ReplaceAction;
import workbench.gui.actions.UnCommentAction;
import workbench.gui.actions.WbAction;
import workbench.gui.components.ExtensionFileFilter;
import workbench.gui.components.ReplacePanel;
import workbench.gui.components.SearchCriteriaPanel;
import workbench.gui.components.WbMenuItem;
import workbench.gui.editor.AnsiSQLTokenMarker;
import workbench.gui.editor.JEditTextArea;
import workbench.gui.editor.SyntaxDocument;
import workbench.gui.editor.TokenMarker;
import workbench.interfaces.ClipboardSupport;
import workbench.interfaces.FilenameChangeListener;
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
import workbench.util.FileUtil;
import workbench.util.StringUtil;

/**
 * @author  support@sql-workbench.net
 */
public class EditorPanel
	extends JEditTextArea
	implements ClipboardSupport, FontChangedListener, PropertyChangeListener, DropTargetListener,
						 TextContainer, TextFileContainer, Replaceable, Searchable, FormattableSql
{
	private static final Border DEFAULT_BORDER = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
	private AnsiSQLTokenMarker sqlTokenMarker;
	private static final int SQL_EDITOR = 0;
	private static final int TEXT_EDITOR = 1;
	private int editorType;
	private String lastSearchCriteria;

	private FindAction findAction;
	private FindAgainAction findAgainAction;
	private ReplaceAction replaceAction;
	private FormatSqlAction formatSql;
	
	protected FileOpenAction fileOpen;
	protected FileSaveAsAction fileSaveAs;
	
	protected FileSaveAction fileSave;
	protected FileReloadAction fileReloadAction;
	
	private ColumnSelectionAction columnSelection;
	private MatchBracketAction matchBracket;
	private CommentAction commentAction;
	private UnCommentAction unCommentAction;

	private List<FilenameChangeListener> filenameChangeListeners;
	private File currentFile;
	private String fileEncoding;
	private Set dbFunctions = null;
	private ReplacePanel replacePanel = null;
	private boolean isMySQL = false;
	private DelimiterDefinition alternateDelimiter;
	
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

	public EditorPanel(TokenMarker aMarker)
	{
		super();
		this.setDoubleBuffered(true);
		this.setFont(Settings.getInstance().getEditorFont());
		this.setBorder(DEFAULT_BORDER);

		this.getPainter().setStyles(SyntaxUtilities.getDefaultSyntaxStyles());

		this.setTabSize(Settings.getInstance().getEditorTabWidth());
		this.setCaretBlinkEnabled(true);
		this.fileSave = new FileSaveAction(this);
		this.fileSaveAs = new FileSaveAsAction(this);
		this.addPopupMenuItem(fileSaveAs, true);
		this.fileOpen = new FileOpenAction(this);
		this.addPopupMenuItem(this.fileOpen, false);

		this.fileReloadAction = new FileReloadAction(this);
		this.fileReloadAction.setEnabled(false);
		
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
		this.setShowLineNumbers(Settings.getInstance().getShowLineNumbers());

		this.columnSelection = new ColumnSelectionAction(this);
		this.matchBracket = new MatchBracketAction(this);
		this.addKeyBinding(this.matchBracket);

		this.commentAction = new CommentAction(this);
		this.unCommentAction = new UnCommentAction(this);
		this.addKeyBinding(this.commentAction);
		this.addKeyBinding(this.unCommentAction);

		//this.setSelectionRectangular(true);
		Settings.getInstance().addFontChangedListener(this);
		Settings.getInstance().addPropertyChangeListener(this);
		this.setRightClickMovesCursor(Settings.getInstance().getRightClickMovesCursor());
		new DropTarget(this, DnDConstants.ACTION_COPY, this);
	}

	public void disableSqlHighlight()
	{
		this.sqlTokenMarker = null;
		this.setTokenMarker(null);
	}
	
	public void enableSqlHighlight()
	{
		if (this.sqlTokenMarker == null)
		{
			this.setTokenMarker(new AnsiSQLTokenMarker());
		}
		else
		{
			this.setTokenMarker(this.sqlTokenMarker);
		}
	}
	
	public void setDatabaseConnection(WbConnection aConnection)
	{
		if (aConnection == null) 
		{
			this.alternateDelimiter = Settings.getInstance().getAlternateDelimiter();
			return;
		}
		AnsiSQLTokenMarker token = this.getSqlTokenMarker();
		this.dbFunctions = aConnection.getMetadata().getDbFunctions();
		if (token != null) 
		{
			token.initKeywordMap(); // reset keywords, to get rid of old DBMS specific ones
			
			Collection keywords = aConnection.getMetadata().getSqlKeywords();
			token.setSqlKeyWords(keywords);
			token.setSqlFunctions(this.dbFunctions);
			
			String key = "workbench.db." + aConnection.getMetadata().getDbId() + ".syntax.";

			List addKeys = StringUtil.stringToList(Settings.getInstance().getProperty(key  + "keywords", ""), ",", true, true);
			token.setSqlKeyWords(addKeys);
			addKeys = StringUtil.stringToList(Settings.getInstance().getProperty(key  + "functions", ""), ",", true, true);
			token.setSqlFunctions(addKeys);			
			this.isMySQL = aConnection.getMetadata().isMySql();
			token.setIsMySQL(isMySQL);
		}
		
		if (aConnection.getMetadata().isMySql())
		{
			this.commentChar = "#";
		}
		else
		{
			this.commentChar = "--";
		}

		this.alternateDelimiter = Settings.getInstance().getAlternateDelimiter(aConnection);
	}

	public void fontChanged(String aKey, Font aFont)
	{
		if (aKey.equals(Settings.PROPERTY_EDITOR_FONT))
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

	public FileSaveAction getFileSaveAction() { return this.fileSave; }
	public FileSaveAsAction getFileSaveAsAction() { return this.fileSaveAs; }
	public FormatSqlAction getFormatSqlAction() { return this.formatSql; }
	public FileReloadAction getReloadAction() { return this.fileReloadAction; }
	public FileOpenAction getFileOpenAction() { return this.fileOpen; }

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
		this.fileOpen.setEnabled(editable);
		if (!editable)
		{
			Component[] c = this.popup.getComponents();
			for (int i = 0; i < c.length; i++)
			{
				if (c[i] instanceof WbMenuItem)
				{
					WbMenuItem menu = (WbMenuItem)c[i];
					if (menu.getAction() == fileOpen)
					{
						popup.remove(c[i]);
						return;
					}
				}
			}
		}
	}

	public void reformatSql()
	{
		String sql = this.getSelectedStatement();
		ScriptParser parser = new ScriptParser();
		parser.setAlternateDelimiter(this.alternateDelimiter);
		parser.setReturnStartingWhitespace(true);
		parser.setCheckHashComments(this.isMySQL);
		parser.setScript(sql);
		
		String delimit = parser.getDelimiterString();

		int count = parser.getSize();
		if (count < 1) return;

		StringBuilder newSql = new StringBuilder(sql.length() + 100);

		String end = Settings.getInstance().getInternalEditorLineEnding();
		
		for (int i=0; i < count; i++)
		{
			String command = parser.getCommand(i);

			// no need to format "empty" strings
			if (StringUtil.isWhitespace(command))
			{
				newSql.append(command);
				continue;
			}
			
			SqlFormatter f = new SqlFormatter(command, Settings.getInstance().getFormatterMaxSubselectLength());
			f.setDBFunctions(this.dbFunctions);
			int cols = Settings.getInstance().getFormatterMaxColumnsInSelect();
			f.setMaxColumnsPerSelect(cols);
			
			try
			{
				String formattedSql = f.getFormattedSql();
				newSql.append(formattedSql);
				if (!command.trim().endsWith(delimit))
				{
					newSql.append(delimit);
				}
			}
			catch (Exception e)
			{
				LogMgr.logError("EditorPanel.reformatSql()", "Error when formatting SQL", e);
			}
		}

		if (newSql.length() == 0) return;

		if (this.isTextSelected())
		{
			this.setSelectedText(newSql.toString());
		}
		else
		{
			this.setText(newSql.toString());
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
	 *	Change the currently selected text so that it can be used for a SQL IN statement with
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

	protected void makeInList(boolean quoteElements)
	{
		int startline = this.getSelectionStartLine();
		int endline = this.getSelectionEndLine();
		int count = (endline - startline + 1);
		StringBuilder newText = new StringBuilder(count * 80);
		String nl = Settings.getInstance().getInternalEditorLineEnding();
		
		try
		{
			// make sure at least one character from the last line is selected
			// if the selection does not extend into the line, then
			// the line is ignored
			int selectionLength = this.getSelectionEnd(endline) - this.getLineStartOffset(endline);
			if (selectionLength <= 0) endline--;
		}
		catch (Exception e)
		{
			// ignore it
		}
		
		int maxElementsPerLine = 5;
		if (quoteElements)
		{
			maxElementsPerLine = Settings.getInstance().getMaxCharInListElements();
		}
		else
		{
			maxElementsPerLine = Settings.getInstance().getMaxNumInListElements();
		}
		int elements = 0;
		
		boolean newLinePending = false;
		
		for (int i=startline; i <= endline; i++)
		{
			String line = this.getLineText(i);
			if (StringUtil.isEmptyString(line)) continue;
			
			if (i == startline)
			{
				newText.append('(');
			}
			else
			{
				newText.append(", ");
			}
			if (newLinePending)
			{
				newText.append(nl);
				newText.append(' ');
				newLinePending = false;
			}
			if (quoteElements) newText.append('\'');
			newText.append(line.trim());
			if (quoteElements) newText.append('\'');
			elements ++;
			if (i < endline)
			{
				if ((elements & maxElementsPerLine) == maxElementsPerLine) 
				{
					newLinePending = true;
					elements = 0;
				}
			}
		}
		newText.append(')');
		newText.append(nl);
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

	public void dispose()
	{
		this.clearUndoBuffer();
		this.popup.removeAll();
		this.popup = null;
		this.setDocument(new SyntaxDocument());
	}
	/**
	 * Return the selected statement of the editor. If no 
	 * text is selected, the whole text will be returned
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
			this.setDocument(new SyntaxDocument());
			this.clearUndoBuffer();
		}
		fireFilenameChanged(null);
		this.resetModified();
    return true;
	}

	protected void checkFileActions()
	{
		boolean hasFile = this.hasFileLoaded();
		this.fileSave.setEnabled(hasFile);
		this.fileReloadAction.setEnabled(hasFile);
	}
	
	public void fireFilenameChanged(String aNewName)
	{
		this.checkFileActions();
		if (this.filenameChangeListeners == null) return;
		Iterator<FilenameChangeListener> itr = filenameChangeListeners.iterator();
		while (itr.hasNext())
		{
			FilenameChangeListener l = itr.next();
			l.fileNameChanged(this, aNewName);
		}
	}

	public void addFilenameChangeListener(FilenameChangeListener aListener)
	{
		if (aListener == null) return;
		if (this.filenameChangeListeners == null) this.filenameChangeListeners = new LinkedList<FilenameChangeListener>();
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
		if (!this.canCloseFile())
		{
			this.requestFocusInWindow();
			return false;
		}

		String lastDir = Settings.getInstance().getLastSqlDir();
		JFileChooser fc = new JFileChooser(lastDir);
		JComponent p = EncodingUtil.createEncodingPanel();
		p.setBorder(new EmptyBorder(0, 5, 0, 0));
		EncodingSelector selector = (EncodingSelector)p;
		selector.setEncoding(Settings.getInstance().getDefaultFileEncoding());
		fc.setAccessory(p);
		
		fc.addChoosableFileFilter(ExtensionFileFilter.getSqlFileFilter());
		int answer = fc.showOpenDialog(SwingUtilities.getWindowAncestor(this));
		if (answer == JFileChooser.APPROVE_OPTION)
		{
			String encoding = selector.getEncoding();
			
			result = readFile(fc.getSelectedFile(), encoding);
			
			WbSwingUtilities.invoke(new Runnable()
			{
				public void run()
				{
					SwingUtilities.getWindowAncestor(EditorPanel.this).validate();
					SwingUtilities.getWindowAncestor(EditorPanel.this).repaint();
				}
			});
			
			lastDir = fc.getCurrentDirectory().getAbsolutePath();
			Settings.getInstance().setLastSqlDir(lastDir);
			Settings.getInstance().setDefaultFileEncoding(encoding);
		}
		return result;
	}

	public boolean reloadFile()
	{
		if (!this.hasFileLoaded()) return false;
		if (this.currentFile == null) return false;

		if (this.isModified())
		{
			String msg = ResourceMgr.getString("MsgConfirmUnsavedReload");
			msg = StringUtil.replace(msg, "%filename%", this.getCurrentFileName());
			boolean reload = WbSwingUtilities.getYesNo(this, msg);
			if (!reload) return false;
		}
		boolean result = false;
		int caret = this.getCaretPosition();
		result = this.readFile(currentFile, fileEncoding);
		if (result)
		{
			this.setCaretPosition(caret);
		}
		return result;
	}


	public boolean hasFileLoaded()
	{
		String file = this.getCurrentFileName();
		return (file != null) && (file.length() > 0);
	}

	public int checkAndSaveFile()
	{
		if (!this.hasFileLoaded()) return JOptionPane.YES_OPTION;
		int result = JOptionPane.YES_OPTION;

		if (this.isModified())
		{
			String msg = ResourceMgr.getString("MsgConfirmUnsavedEditorFile");
			msg = StringUtil.replace(msg, "%filename%", this.getCurrentFileName());
			result = WbSwingUtilities.getYesNoCancel(this, msg);
			if (result == JOptionPane.YES_OPTION)
			{
				this.saveCurrentFile();
			}
		}
		return result;
	}

	public boolean canCloseFile()
	{
		if (!this.hasFileLoaded()) return true;
		if (!this.isModified()) return true;
		int choice = this.checkAndSaveFile();
		if (choice == JOptionPane.YES_OPTION)
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	public boolean readFile(File aFile)
	{
		return this.readFile(aFile, null);
	}

	public boolean readFile(File aFile, String encoding)
	{
		if (aFile == null) return false;
		if (!aFile.exists()) return false;
		if (aFile.length() >= Integer.MAX_VALUE / 2)
		{
			WbSwingUtilities.showErrorMessageKey(this, "MsgFileTooBig");
			return false;
		}
		
		boolean result = false;
		
		BufferedReader reader = null;
		String lineEnding = Settings.getInstance().getInternalEditorLineEnding();
		SyntaxDocument doc = null;
		
		try
		{
			// try to free memory by releasing the current document
			if(this.document != null)
			{
				this.document.removeDocumentListener(documentHandler);
				this.document.dispose();
			}			
			System.gc();
			System.runFinalization();
			
			String filename = aFile.getAbsolutePath();
			File f = new File(filename);
			try
			{
				if (StringUtil.isEmptyString(encoding)) encoding = EncodingUtil.getDefaultEncoding();
				reader = EncodingUtil.createBufferedReader(f, encoding);
			}
			catch (UnsupportedEncodingException e)
			{
				LogMgr.logError("EditorPanel.readFile()", "Unsupported encoding: " + encoding + " requested. Using UTF-8", e);
				try
				{
					encoding = "UTF-8";
					FileInputStream in = new FileInputStream(filename);
					reader = new BufferedReader(new InputStreamReader(in, "UTF-8"), 8192);
				}
				catch (Throwable ignore) {}
			}

			// Creating a SyntaxDocument with a filled GapContent
			// does not seem to work, inserting the text has to
			// go through the SyntaxDocument
			// but we initiallize the GapContent in advance to avoid
			// too many re-allocations of the internal buffer
			GapContent  content = new GapContent((int)aFile.length() + 1500);
			doc = new SyntaxDocument(content);
			doc.suspendUndo();
			
			int pos = 0;
			
			final int numLines = 50;
			StringBuilder lineBuffer = new StringBuilder(numLines * 100);
			
			// Inserting the text in chunks is much faster than 
			// inserting it line by line. Optimal speed would probably
			// when reading everything into a buffer, and then call insertString()
			// only once, but that will cost too much memory (the memory footprint
			// of the editor is already too high...)
			int lines = FileUtil.readLines(reader, lineBuffer, numLines, lineEnding);
			while (lines > 0)
			{
				doc.insertString(pos, lineBuffer.toString(), null);
				pos += lineBuffer.length();
				lineBuffer.setLength(0);
				lines = FileUtil.readLines(reader, lineBuffer, numLines, lineEnding);
			}
			
			doc.resumeUndo();
			this.setDocument(doc);
			
			this.currentFile = aFile;
			this.fileEncoding = encoding;
			result = true;
			this.fireFilenameChanged(filename);
		}
		catch (BadLocationException bl)
		{
			LogMgr.logError("EditorPanel.readFile()", "Error reading file " + aFile.getAbsolutePath(), bl);
		}
		catch (IOException e)
		{
			LogMgr.logError("EditorPanel.readFile()", "Error reading file " + aFile.getAbsolutePath(), e);
		}
		catch (OutOfMemoryError mem)
		{
			doc.dispose();
			System.gc();
			WbManager.getInstance().showOutOfMemoryError();
			result = false;
		}
		finally
		{
			this.resetModified();
			try { reader.close(); } catch (Throwable th) {}
		}
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
				result = this.saveFile();
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
			lastDir = Settings.getInstance().getLastSqlDir();
			ff = ExtensionFileFilter.getSqlFileFilter();
		}
		else
		{
			lastDir = Settings.getInstance().getLastEditorDir();
			ff = ExtensionFileFilter.getTextFileFilter();
		}
		JFileChooser fc = new JFileChooser(lastDir);
		fc.setSelectedFile(this.currentFile);
		fc.addChoosableFileFilter(ff);
		JComponent p = EncodingUtil.createEncodingPanel();
		p.setBorder(new EmptyBorder(0,5,0,0));
		EncodingSelector selector = (EncodingSelector)p;
		selector.setEncoding(this.fileEncoding);
		fc.setAccessory(p);

		int answer = fc.showSaveDialog(SwingUtilities.getWindowAncestor(this));
		if (answer == JFileChooser.APPROVE_OPTION)
		{
			try
			{
				String encoding = selector.getEncoding();
				this.saveFile(fc.getSelectedFile(), encoding, Settings.getInstance().getExternalEditorLineEnding());
	      this.fireFilenameChanged(this.getCurrentFileName());
				lastDir = fc.getCurrentDirectory().getAbsolutePath();
				if (this.editorType == SQL_EDITOR)
				{
					Settings.getInstance().setLastSqlDir(lastDir);
				}
				else
				{
					Settings.getInstance().setLastEditorDir(lastDir);
				}
				result = true;
			}
			catch (IOException e)
			{
				WbSwingUtilities.showErrorMessage(this, ResourceMgr.getString("ErrSavingFile") + "\n" + ExceptionUtil.getDisplay(e));
				result = false;
			}
		}
		return result;
	}

	public void saveFile(File aFile)
		throws IOException
	{
		this.saveFile(aFile, this.fileEncoding, Settings.getInstance().getExternalEditorLineEnding());
	}

	public void saveFile(File aFile, String encoding)
		throws IOException
	{
		this.saveFile(aFile, encoding, Settings.getInstance().getExternalEditorLineEnding());
	}
	
	public void saveFile(File aFile, String encoding, String lineEnding)
		throws IOException
	{
		if (encoding == null)
		{
			encoding = Settings.getInstance().getDefaultFileEncoding();
		}

		try
		{
			String filename = aFile.getAbsolutePath();
			int pos = filename.indexOf('.');
			if (pos < 0)
			{
				filename = filename + ".sql";
				aFile = new File(filename);
			}
			
			Writer writer = EncodingUtil.createWriter(aFile, encoding, false);
			
			int count = this.getLineCount();
			
			for (int i=0; i < count; i++)
			{
				String line = this.getLineText(i);
				int len = StringUtil.getRealLineLength(line);
				if (len > 0) writer.write(line, 0, len);
				writer.write(lineEnding);
			}
			writer.close();
			this.currentFile = aFile;
			this.fileEncoding = encoding;
			this.resetModified();
		}
		catch (IOException e)
		{
			LogMgr.logError("EditorPanel.saveFile()", "Error saving file", e);
			throw e;
		}
	}

	public File getCurrentFile() { return this.currentFile; }

	public String getCurrentFileEncoding()
	{
		if (this.currentFile == null) return null;
		return this.fileEncoding;
	}
	public String getCurrentFileName()
	{
		if (this.currentFile == null) return null;
		return this.currentFile.getAbsolutePath();
	}

	public CommentAction getCommentAction() { return this.commentAction; }
	public UnCommentAction getUnCommentAction() { return this.unCommentAction; }

	public FindAction getFindAction() { return this.findAction; }
	public FindAgainAction getFindAgainAction() { return this.findAgainAction; }

	public ReplaceAction getReplaceAction() { return this.replaceAction; }

	public int find()
	{
		boolean showDialog = true;
		String crit = this.lastSearchCriteria;
		if (crit == null) crit = this.getSelectedText();
		SearchCriteriaPanel p = new SearchCriteriaPanel(crit);

		int pos = -1;
		while (showDialog)
		{
			boolean doFind = p.showFindDialog(this);
			if (!doFind) return -1;
			String criteria = p.getCriteria();
			boolean ignoreCase = p.getIgnoreCase();
			boolean wholeWord = p.getWholeWordOnly();
			boolean useRegex = p.getUseRegex();
			try
			{
				this.lastSearchCriteria = criteria;
				this.findAgainAction.setEnabled(false);
				pos = this.findText(criteria, ignoreCase, wholeWord, useRegex);
				showDialog = false;
				this.findAgainAction.setEnabled(pos > -1);
			}
			catch (Exception e)
			{
				pos = -1;
				WbSwingUtilities.showErrorMessage(this, ExceptionUtil.getDisplay(e));
				showDialog = true;
			}
		}
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
			String text = this.getSelectedText();
			Matcher m = this.lastSearchPattern.matcher(text);
			String newText = m.replaceAll(fixSpecialReplacementChars(aReplacement));
			this.setSelectedText(newText);
		}
		
		return (pos > -1);
	}

	public boolean isTextSelected()
	{
		int selStart = this.getSelectionStart();
		int selEnd = this.getSelectionEnd();
		return (selStart > -1 && selEnd > selStart);
	}
	
	private String fixSpecialReplacementChars(String input)
	{
		String fixed = input.replaceAll("\\\\n", "\n");
		fixed = fixed.replaceAll("\\\\r", "\r");
		fixed = fixed.replaceAll("\\\\t", "\t");
		return fixed;
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
		if (!useRegex)
		{
			replacement = StringUtil.quoteRegexMeta(replacement);
		}
		else
		{
			replacement = fixSpecialReplacementChars(replacement);
		}
		
		Pattern p = Pattern.compile(regex, (ignoreCase ? Pattern.CASE_INSENSITIVE : 0));
		Matcher m = p.matcher(old);
		String newText = m.replaceAll(replacement);
		
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
			Matcher m = this.lastSearchPattern.matcher(this.getSelectedText());
			String newText = m.replaceAll(fixSpecialReplacementChars(aReplacement));
			this.setSelectedText(newText);
			return true;
		}
		else
		{
			return replaceNext(aReplacement);
		}
	}

	/* (non-Javadoc)
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent evt)
	{
		if (Settings.PROPERTY_SHOW_LINE_NUMBERS.equals(evt.getPropertyName()))
		{
			this.setShowLineNumbers(Settings.getInstance().getShowLineNumbers());
			this.repaint();
		}
		else if (Settings.PROPERTY_EDITOR_TAB_WIDTH.equals(evt.getPropertyName()))
		{
			this.setTabSize(Settings.getInstance().getEditorTabWidth());
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
					EventQueue.invokeLater(new Runnable()
					{
						public void run()
						{
							w.toFront();
							w.requestFocus();
							WbSwingUtilities.showErrorMessageKey(w, "ErrNoMultipleDrop");
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
