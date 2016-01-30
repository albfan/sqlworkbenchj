/*
 * EditorPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.FocusEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.GapContent;

import workbench.WbManager;
import workbench.interfaces.ClipboardSupport;
import workbench.interfaces.EncodingSelector;
import workbench.interfaces.FilenameChangeListener;
import workbench.interfaces.FontChangedListener;
import workbench.interfaces.FormattableSql;
import workbench.interfaces.SqlTextContainer;
import workbench.interfaces.TextFileContainer;
import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.QuoteHandler;
import workbench.db.WbConnection;

import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.ColumnSelectionAction;
import workbench.gui.actions.CommentAction;
import workbench.gui.actions.FileReloadAction;
import workbench.gui.actions.FileSaveAction;
import workbench.gui.actions.FileSaveAsAction;
import workbench.gui.actions.FindAction;
import workbench.gui.actions.FindNextAction;
import workbench.gui.actions.FindPreviousAction;
import workbench.gui.actions.FormatSqlAction;
import workbench.gui.actions.JumpToLineAction;
import workbench.gui.actions.MatchBracketAction;
import workbench.gui.actions.OpenFileAction;
import workbench.gui.actions.RedoAction;
import workbench.gui.actions.ReplaceAction;
import workbench.gui.actions.ToggleCommentAction;
import workbench.gui.actions.UnCommentAction;
import workbench.gui.actions.UndoAction;
import workbench.gui.actions.WbAction;
import workbench.gui.components.ExtensionFileFilter;
import workbench.gui.components.WbFileChooser;
import workbench.gui.components.WbMenuItem;
import workbench.gui.dbobjects.objecttree.EditorDropHandler;
import workbench.gui.dbobjects.objecttree.ObjectTreeTransferable;
import workbench.gui.editor.AnsiSQLTokenMarker;
import workbench.gui.editor.JEditTextArea;
import workbench.gui.editor.SearchAndReplace;
import workbench.gui.editor.SyntaxDocument;
import workbench.gui.editor.TextFormatter;
import workbench.gui.editor.TokenMarker;

import workbench.sql.DelimiterDefinition;
import workbench.sql.syntax.SqlKeywordHelper;

import workbench.util.CollectionUtil;
import workbench.util.EncodingUtil;
import workbench.util.ExceptionUtil;
import workbench.util.FileUtil;
import workbench.util.MemoryWatcher;
import workbench.util.StringUtil;
import workbench.util.WbFile;

/**
 * An extension to {@link workbench.gui.editor.JEditTextArea}. This class
 * implements Workbench (SQL) specific extensions to the original jEdit class.
 *
 * @see workbench.gui.editor.JEditTextArea
 *
 * @author Thomas Kellerer
 */
public class EditorPanel
  extends JEditTextArea
  implements ClipboardSupport, FontChangedListener, PropertyChangeListener, DropTargetListener,
             SqlTextContainer, TextFileContainer, FormattableSql
{
  private static final Border DEFAULT_BORDER = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
  private AnsiSQLTokenMarker sqlTokenMarker;
  private static final int SQL_EDITOR = 0;
  private static final int TEXT_EDITOR = 1;
  private int editorType;

  private FormatSqlAction formatSql;
  private final SearchAndReplace replacer;

  protected UndoAction undo;
  protected RedoAction redo;
  protected OpenFileAction fileOpen;
  protected FileSaveAsAction fileSaveAs;

  private boolean allowReformatOnReadonly;

  protected FileSaveAction fileSave;
  protected FileReloadAction fileReloadAction;

  private final ColumnSelectionAction columnSelection;
  private final MatchBracketAction matchBracket;
  private final ToggleCommentAction toggleCommentAction;
  private final CommentAction commentAction;
  private final UnCommentAction unCommentAction;
  private final JumpToLineAction jumpToLineAction;

  private final List<FilenameChangeListener> filenameChangeListeners = new LinkedList<>();
  private WbFile currentFile;
  private boolean saveInProgress;
  private boolean loadInProgress;
  private long fileModifiedTime;
  private String fileEncoding;
  private Set<String> dbFunctions;
  private Set<String> dbDatatypes;
  private DelimiterDefinition alternateDelimiter;
  private String dbId;
  private QuoteHandler quoteHandler = QuoteHandler.STANDARD_HANDLER;
  private boolean allowFileLoading = true;

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
    this.setBorder(DEFAULT_BORDER);

    this.setTabSize(Settings.getInstance().getEditorTabWidth());
    this.setCaretBlinkEnabled(true);
    this.fileSave = new FileSaveAction(this);
    this.fileSave.setEnabled(false);
    this.fileSaveAs = new FileSaveAsAction(this);
    this.addPopupMenuItem(fileSave, true);
    this.addPopupMenuItem(fileSaveAs, false);
    this.fileOpen = new OpenFileAction(this);
    this.addPopupMenuItem(this.fileOpen, false);
    this.jumpToLineAction = new JumpToLineAction(this);

    this.fileReloadAction = new FileReloadAction(this);
    this.fileReloadAction.setEnabled(false);

    this.replacer = new SearchAndReplace(this, this);
    this.addKeyBinding(this.getFindAction());
    this.addKeyBinding(this.getFindPreviousAction());
    this.addKeyBinding(this.getFindNextAction());
    this.addKeyBinding(this.getReplaceAction());
    this.addKeyBinding(getJumpToLineAction());

    if (aMarker != null) this.setTokenMarker(aMarker);

    this.setMaximumSize(null);
    this.setPreferredSize(null);

    this.columnSelection = new ColumnSelectionAction(this);
    this.matchBracket = new MatchBracketAction(this);
    this.addKeyBinding(this.matchBracket);

    this.toggleCommentAction = new ToggleCommentAction(this);
    this.commentAction = new CommentAction(this);
    this.unCommentAction = new UnCommentAction(this);
    this.addKeyBinding(this.toggleCommentAction);
    this.addKeyBinding(this.commentAction);
    this.addKeyBinding(this.unCommentAction);

    this.undo = new UndoAction(this);
    this.redo = new RedoAction(this);
    this.addKeyBinding(undo);
    this.addKeyBinding(redo);

    Settings.getInstance().addFontChangedListener(this);
    Settings.getInstance().addPropertyChangeListener(this, Settings.PROPERTY_EDITOR_TAB_WIDTH, Settings.PROPERTY_EDITOR_ELECTRIC_SCROLL);

    this.setRightClickMovesCursor(Settings.getInstance().getRightClickMovesCursor());

    new DropTarget(this, DnDConstants.ACTION_COPY, this);
  }

  public void setAllowFileLoading(boolean flag)
  {
    allowFileLoading = flag;
  }

  @Override
  public void invalidate()
  {
    super.invalidate();
    if (this.painter != null)
    {
      this.painter.invalidate();
    }
  }

  public void disableSqlHighlight()
  {
    this.setTokenMarker(null);
  }

  public void enableSqlHighlight()
  {
    if (this.sqlTokenMarker == null)
    {
      this.sqlTokenMarker = new AnsiSQLTokenMarker();
    }
    this.setTokenMarker(this.sqlTokenMarker);
  }

  public void setDatabaseConnection(WbConnection aConnection)
  {
    dbFunctions = CollectionUtil.caseInsensitiveSet();
    dbDatatypes = CollectionUtil.caseInsensitiveSet();

    this.alternateDelimiter = Settings.getInstance().getAlternateDelimiter(aConnection, null);

    AnsiSQLTokenMarker token = this.getSqlTokenMarker();

    // can happen for plain (non SQL) editor
    if (token == null) return;

    if (aConnection != null)
    {
      quoteHandler = aConnection.getMetadata();
      // Support MySQL's non-standard line comment
      boolean isMySQL = aConnection.getMetadata().isMySql();
      token.setIsMySQL(isMySQL);
      if (isMySQL && Settings.getInstance().getBoolProperty("workbench.editor.mysql.usehashcomment", false))
      {
        commentChar = "#";
      }

      // Support Microsoft's broken object quoting using square brackets (e.g. [wrong_table])
      token.setIsMicrosoft(aConnection.getMetadata().isSqlServer());
      this.dbId = aConnection.getDbSettings().getDbId();
    }
    else
    {
      this.dbId = null;
      this.commentChar = "--";
      quoteHandler = QuoteHandler.STANDARD_HANDLER;
    }

    SqlKeywordHelper helper = new SqlKeywordHelper(this.dbId);
    dbFunctions.addAll(helper.getSqlFunctions());
    dbDatatypes.addAll(helper.getDataTypes());

    token.initKeywordMap(helper.getKeywords(), dbDatatypes, helper.getOperators(), dbFunctions);
  }

  @Override
  protected QuoteHandler getQuoteHandler()
  {
    return quoteHandler;
  }
  @Override
  public void fontChanged(String aKey, Font aFont)
  {
    if (aKey.equals(Settings.PROPERTY_EDITOR_FONT))
    {
      this.setFont(aFont);
    }
  }

  public AnsiSQLTokenMarker getSqlTokenMarker()
  {
    TokenMarker marker = this.getTokenMarker();
    if (marker instanceof AnsiSQLTokenMarker)
    {
      return (AnsiSQLTokenMarker)marker;
    }
    return null;
  }

  @Override
  public int getStartInLine(int offset)
  {
    int line = getLineOfOffset(offset);
    int start = getLineStartOffset(line);
    return offset - start;
  }

  @Override
  public void focusGained(FocusEvent e)
  {
    super.focusGained(e);
    WbSwingUtilities.invokeLater(this::checkFileChange);
  }

  protected void checkFileChange()
  {
    if (this.currentFile == null) return;
    if (this.saveInProgress) return;
    if (this.loadInProgress) return;

    try
    {
      loadInProgress = true;

      long currentFileTime = currentFile.lastModified();
      int graceTime = Settings.getInstance().getIntProperty("workbench.gui.editor.reload.check.mindiff", 2000);

      if (Math.abs(currentFileTime - fileModifiedTime) > graceTime)
      {
        String fname = getCurrentFileName();
        FileReloadType reloadType = GuiSettings.getReloadType();

        if (reloadType != FileReloadType.none)
        {
          LogMgr.logDebug("EditorPanel", "File " + fname + " has been modified externally. currentFileTime=" + currentFileTime + ", saved lastModifiedTime=" + fileModifiedTime);
        }

        if (reloadType == FileReloadType.automatic)
        {
          reloadCurrentFile();
          statusBar.setStatusMessage(ResourceMgr.getFormattedString("MsgFileReloaded", fname), 5000);
        }
        else if (reloadType == FileReloadType.prompt)
        {
          boolean doReload = WbSwingUtilities.getYesNo(this, ResourceMgr.getFormattedString("MsgReloadFile", fname));
          if (doReload)
          {
            reloadCurrentFile();
          }
          else
          {
            // don't check again until the file changes another time
            fileModifiedTime = currentFileTime;
          }
        }
      }
    }
    finally
    {
      loadInProgress = false;
    }
  }

  @Override
  public boolean isModifiedAfter(long timeInMillis)
  {
    boolean isModified = super.isModifiedAfter(timeInMillis);
    return isModified || (this.hasFileLoaded() && fileModifiedTime > timeInMillis);
  }

  public void showFindOnPopupMenu()
  {
    this.addPopupMenuItem(this.getFindAction(), true);
    this.addPopupMenuItem(this.getFindNextAction(), false);
    this.addPopupMenuItem(this.getReplaceAction(), false);
  }

  public MatchBracketAction getMatchBracketAction()
  {
    return this.matchBracket;
  }

  public ColumnSelectionAction getColumnSelection()
  {
    return this.columnSelection;
  }

  public UndoAction getUndoAction() { return this.undo; }
  public RedoAction getRedoAction() { return this.redo; }

  public final JumpToLineAction getJumpToLineAction()
  {
    return this.jumpToLineAction;
  }

  protected final FindPreviousAction getFindPreviousAction() { return this.replacer.getFindPreviousAction(); }
  protected final FindAction getFindAction() { return this.replacer.getFindAction(); }
  protected final FindNextAction getFindNextAction() { return this.replacer.getFindNextAction(); }
  protected final ReplaceAction getReplaceAction() { return this.replacer.getReplaceAction(); }

  public SearchAndReplace getReplacer() { return this.replacer; }

  public FileSaveAction getFileSaveAction() { return this.fileSave; }
  public FileSaveAsAction getFileSaveAsAction() { return this.fileSaveAs; }
  public FormatSqlAction getFormatSqlAction() { return this.formatSql; }
  public FileReloadAction getReloadAction() { return this.fileReloadAction; }
  public OpenFileAction getOpenFileAction() { return this.fileOpen; }

  public void showFormatSql()
  {
    if (this.formatSql != null) return;
    this.formatSql = new FormatSqlAction(this);
    this.addKeyBinding(this.formatSql);
    this.addPopupMenuItem(this.formatSql, true);
  }

  @Override
  public void setEditable(boolean editable)
  {
    super.setEditable(editable);
    this.getReplaceAction().setEnabled(editable);
    this.fileOpen.setEnabled(editable);
    if (!editable)
    {
      Component[] c = this.popup.getComponents();
      for (Component c1 : c)
      {
        if (c1 instanceof WbMenuItem)
        {
          WbMenuItem menu = (WbMenuItem) c1;
          if (menu.getAction() == fileOpen)
          {
            popup.remove(c1);
            return;
          }
        }
      }
    }
  }

  /**
   * Controls if reformatting of the SQL is allowed even if the editor is not editable.
   */
  public void setAllowReformatOnReadonly(boolean flag)
  {
    allowReformatOnReadonly = flag;
  }

  @Override
  public void reformatSql()
  {
    if (!allowReformatOnReadonly && !this.isEditable()) return;

    try
    {
      WbSwingUtilities.showWaitCursor(this);
      TextFormatter f = new TextFormatter(this.dbId);
      f.formatSql(this, alternateDelimiter);
    }
    finally
    {
      WbSwingUtilities.showDefaultCursor(this);
    }
  }

  public final void removePopupMenuItem(WbAction anAction)
  {
    if (popup == null) return;
    if (anAction == null) return;

    int count = popup.getComponentCount();
    int removeIndex = -1;

    for (int i=0; i < count; i++)
    {
      Component comp = popup.getComponent(i);
      if (comp instanceof JMenuItem)
      {
        JMenuItem item = (JMenuItem)comp;
        if (item.getAction() == anAction)
        {
          removeIndex = i;
          break;
        }
      }
    }

    if (removeIndex > -1)
    {
      popup.remove(removeIndex);
    }
    removeKeyBinding(anAction);
  }

  public final void addPopupMenuItem(WbAction anAction, boolean withSeparator)
  {
    if (popup == null) return;
    popup.addAction(anAction, withSeparator);
    addKeyBinding(anAction);
  }

  @Override
  public void dispose()
  {
    super.dispose();
    Settings.getInstance().removeFontChangedListener(this);
    this.clearUndoBuffer();
    if (this.popup != null)
    {
      this.popup.dispose();
    }
    this.popup = null;
    this.stopBlinkTimer();
    if (painter != null)
    {
      this.painter.dispose();
    }
    if (inputHandler != null)
    {
      inputHandler.dispose();
    }

    WbAction.dispose(columnSelection, toggleCommentAction, commentAction, fileOpen, fileReloadAction, fileSave,
      fileSaveAs, formatSql, jumpToLineAction, matchBracket, redo, unCommentAction, undo
    );
    replacer.dispose();
    quoteHandler = null;
    this.setDocument(new SyntaxDocument());
  }

  /**
   * Return the selected statement of the editor. If no
   * text is selected, the whole text will be returned
   */
  @Override
  public String getSelectedStatement()
  {
    String text = this.getSelectedText();
    if (text == null || text.length() == 0)
    {
      return this.getText();
    }
    else
    {
      return text;
    }
  }

  @Override
  public boolean closeFile(boolean clearText)
  {
    if (this.currentFile == null) return false;
    this.currentFile = null;
    if (clearText)
    {
      this.setCaretPosition(0);
      this.reset();
    }
    fireFilenameChanged(null);
    checkFileActions();

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
    for (FilenameChangeListener listener : filenameChangeListeners)
    {
      listener.fileNameChanged(this, aNewName);
    }
  }

  public void addFilenameChangeListener(FilenameChangeListener aListener)
  {
    if (aListener == null) return;
    this.filenameChangeListeners.add(aListener);
  }

  public void removeFilenameChangeListener(FilenameChangeListener aListener)
  {
    if (aListener == null) return;
    this.filenameChangeListeners.remove(aListener);
  }

  @Override
  public MainWindow getMainWindow()
  {
    Window w = SwingUtilities.getWindowAncestor(this);
    if (w instanceof MainWindow)
    {
      return (MainWindow)w;
    }
    return null;
  }

  public boolean reloadFile()
  {
    if (this.saveInProgress) return false;
    if (this.loadInProgress) return false;

    try
    {
      loadInProgress = true;
      return reloadCurrentFile();
    }
    finally
    {
      loadInProgress = false;
    }
  }

  private boolean reloadCurrentFile()
  {
    if (!this.hasFileLoaded()) return false;

    if (this.isModified())
    {
      String msg = ResourceMgr.getString("MsgConfirmUnsavedReload");
      msg = StringUtil.replace(msg, "%filename%", this.getCurrentFileName());
      boolean reload = WbSwingUtilities.getYesNo(this, msg);
      if (!reload) return false;
    }
    boolean result;
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
    String fileName = this.getCurrentFileName();
    return StringUtil.isNonEmpty(fileName);
  }

  public int checkAndSaveFile()
  {
    return checkAndSaveFile(true);
  }

  public int checkAndSaveFile(boolean allowCancel)
  {
    if (!this.hasFileLoaded()) return JOptionPane.YES_OPTION;
    int result = JOptionPane.YES_OPTION;

    if (this.isModified())
    {
      String msg = ResourceMgr.getString("MsgConfirmUnsavedEditorFile");
      msg = StringUtil.replace(msg, "%filename%", this.getCurrentFileName());
      if (allowCancel)
      {
        result = WbSwingUtilities.getYesNoCancel(this, msg);
      }
      else
      {
        boolean yesNo = WbSwingUtilities.getYesNo(this, msg);
        if (yesNo)
        {
          result = JOptionPane.YES_OPTION;
        }
        else
        {
          result = JOptionPane.NO_OPTION;
        }
      }

      if (result == JOptionPane.YES_OPTION)
      {
        this.saveCurrentFile();
      }

      if (result == JOptionPane.CLOSED_OPTION)
      {
        result = JOptionPane.CANCEL_OPTION;
      }
    }
    return result;
  }

  public YesNoCancelResult canCloseFile()
  {
    int choice = this.checkAndSaveFile();
    if (choice == JOptionPane.YES_OPTION)
    {
      return YesNoCancelResult.yes;
    }
    else if (choice == JOptionPane.NO_OPTION)
    {
      return YesNoCancelResult.no;
    }
    else
    {
      return YesNoCancelResult.cancel;
    }
  }

  public boolean readFile(File aFile)
  {
    String encoding = null;
    if (Settings.getInstance().getEditorDetectEncoding())
    {
      encoding = FileUtil.detectFileEncoding(aFile);
      if (encoding == null)
      {
        encoding = Settings.getInstance().getSystemFileEncoding();
      }
    }
    return this.readFile(aFile, encoding);
  }

  public boolean readFile(File toLoad, String encoding)
  {
    if (toLoad == null) return false;
    if (!toLoad.exists()) return false;
    if (toLoad.length() >= Integer.MAX_VALUE / 3)
    {
      WbSwingUtilities.showErrorMessageKey(this, "MsgFileTooBig");
      return false;
    }

    boolean result = false;

    this.selectNone();

    BufferedReader reader = null;
    SyntaxDocument doc = null;

    try
    {
      setCaretPosition(0);
      // try to free memory by releasing the current document
      // these is also done later when calling setDocument()
      // but that would mean, that the old and the new document would
      // be in memory at the same time.
      clearCurrentDocument();

      try
      {
        if (StringUtil.isEmptyString(encoding))
        {
          encoding = Settings.getInstance().getDefaultFileEncoding();
        }
        reader = EncodingUtil.createBufferedReader(toLoad, encoding);
      }
      catch (UnsupportedEncodingException e)
      {
        LogMgr.logError("EditorPanel.readFile()", "Unsupported encoding: " + encoding + " requested. Using UTF-8", e);
        WbSwingUtilities.showErrorMessage(this, ResourceMgr.getFormattedString("ErrWrongEncoding", encoding));
        return false;
      }

      // Creating a SyntaxDocument with a filled GapContent
      // does not seem to work, inserting the text has to
      // go through the SyntaxDocument
      // but we initialize the GapContent in advance to avoid
      // too many re-allocations of the internal buffer
      GapContent  content = new GapContent((int)toLoad.length() + 1500);
      doc = new SyntaxDocument(content);
      doc.suspendUndo();

      int pos = 0;

      final int numLines = 50;
      StringBuilder lineBuffer = new StringBuilder(numLines * 100);
      boolean lowMemory = false;

      // Inserting the text in chunks is much faster than
      // inserting it line by line. Best performance would probably be
      // to read everything into one single buffer, and then call insertString()
      // once, but that will double the memory usage during loading
      int lines = FileUtil.readLines(reader, lineBuffer, numLines, "\n");

      while (lines > 0)
      {
        doc.insertString(pos, lineBuffer.toString(), null);
        pos += lineBuffer.length();
        lineBuffer.setLength(0);
        lines = FileUtil.readLines(reader, lineBuffer, numLines, "\n");
        if (MemoryWatcher.isMemoryLow(true))
        {
          lowMemory = true;
          break;
        }
      }

      if (lowMemory)
      {
        doc.reset();
        result = false;
        WbManager.getInstance().showLowMemoryError();
      }
      else
      {
        doc.resumeUndo();
        setDocument(doc);
        currentFile = new WbFile(toLoad);
        fileEncoding = encoding;
        result = true;
        fireFilenameChanged(toLoad.getAbsolutePath());
      }
      checkFileActions();
    }
    catch (BadLocationException | IOException bl)
    {
      LogMgr.logError("EditorPanel.readFile()", "Error reading file " + toLoad.getAbsolutePath(), bl);
    }
    catch (OutOfMemoryError mem)
    {
      if (doc != null) doc.reset();
      System.gc();
      WbManager.getInstance().showOutOfMemoryError();
      result = false;
    }
    finally
    {
      resetModified();
      FileUtil.closeQuietely(reader);
      fileModifiedTime = currentFile.lastModified();
    }

    return result;
  }

  @Override
  public boolean saveCurrentFile()
  {
    boolean result;
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

  @Override
  public boolean saveFile()
  {
    boolean result = false;
    String lastDir;
    FileFilter ff;
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
    JFileChooser fc = new WbFileChooser(lastDir);
    fc.setSelectedFile(this.currentFile);
    fc.addChoosableFileFilter(ff);
    JComponent p = EncodingUtil.createEncodingPanel();
    p.setBorder(new EmptyBorder(0,5,0,0));
    EncodingSelector selector = (EncodingSelector)p;
    selector.setEncoding(fileEncoding != null ? fileEncoding : Settings.getInstance().getDefaultFileEncoding());
    fc.setAccessory(p);

    int answer = fc.showSaveDialog(SwingUtilities.getWindowAncestor(this));
    if (answer == JFileChooser.APPROVE_OPTION)
    {
      try
      {
        String encoding = selector.getEncoding();
        if (StringUtil.isEmptyString(encoding))
        {
          encoding = FileUtil.detectFileEncoding(fc.getSelectedFile());
        }
        saveFile(fc.getSelectedFile(), encoding, Settings.getInstance().getExternalEditorLineEnding());
        if (allowFileLoading)
        {
          this.fireFilenameChanged(this.getCurrentFileName());
        }
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

    boolean trimTrailing = Settings.getInstance().getTrimTrailingSpaces();

    Writer writer = null;
    try
    {
      saveInProgress = true;
      String filename = aFile.getAbsolutePath();
      int pos = filename.indexOf('.');
      if (pos < 0)
      {
        filename += ".sql";
        aFile = new File(filename);
      }

      writer = EncodingUtil.createWriter(aFile, encoding, false);

      int count = this.getLineCount();

      for (int i=0; i < count; i++)
      {
        String line = this.getLineText(i);
        int len = 0;
        if (trimTrailing)
        {
          line = StringUtil.rtrim(line);
          len = line.length();
        }
        else
        {
          len = StringUtil.getRealLineLength(line);
        }
        if (len > 0)
        {
          writer.write(line, 0, len);
          writer.write(lineEnding);
        }
        else if (i < count - 1)
        {
          // do not append an empty line at the end
          writer.write(lineEnding);
        }
      }

      if (allowFileLoading)
      {
        currentFile = new WbFile(aFile);
        fileEncoding = encoding;
        fileModifiedTime = currentFile.lastModified();
        resetModified();
      }
    }
    catch (IOException e)
    {
      LogMgr.logError("EditorPanel.saveFile()", "Error saving file", e);
      throw e;
    }
    finally
    {
      FileUtil.closeQuietely(writer);
      saveInProgress = false;
    }
  }

  @Override
  public File getCurrentFile()
  {
    return this.currentFile;
  }

  public String getCurrentFileEncoding()
  {
    if (this.currentFile == null) return null;
    return this.fileEncoding;
  }

  public String getCurrentFileName()
  {
    if (this.currentFile == null) return null;
    return this.currentFile.getFullPath();
  }

  public ToggleCommentAction getToggleCommentAction()
  {
    return toggleCommentAction;
  }

  public CommentAction getCommentAction()
  {
    return commentAction;
  }

  public UnCommentAction getUnCommentAction()
  {
    return this.unCommentAction;
  }

  /**
   * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
   */
  @Override
  public void propertyChange(PropertyChangeEvent evt)
  {
    super.propertyChange(evt);
    if (Settings.PROPERTY_EDITOR_TAB_WIDTH.equals(evt.getPropertyName()))
    {
      this.setTabSize(Settings.getInstance().getEditorTabWidth());
    }
    else if (Settings.PROPERTY_EDITOR_ELECTRIC_SCROLL.equals(evt.getPropertyName()))
    {
      this.setElectricScroll(Settings.getInstance().getElectricScroll());
    }
    if (this.isReallyVisible())
    {
      WbSwingUtilities.repaintNow(this);
    }
  }

  @Override
  public void dragEnter(DropTargetDragEvent dropTargetDragEvent)
  {
    if (isEditable())
    {
      dropTargetDragEvent.acceptDrag(DnDConstants.ACTION_COPY);
    }
    else
    {
      dropTargetDragEvent.rejectDrag();
    }
  }

  @Override
  public void dragExit(java.awt.dnd.DropTargetEvent dropTargetEvent)
  {
  }

  @Override
  public void dragOver(DropTargetDragEvent evt)
  {
  }

  @Override
  public void drop(DropTargetDropEvent evt)
  {
    try
    {
      Transferable tr = evt.getTransferable();
      if (tr.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
      {
        evt.acceptDrop(DnDConstants.ACTION_COPY);
        java.util.List fileList = (java.util.List)tr.getTransferData(DataFlavor.javaFileListFlavor);
        if (fileList != null && fileList.size() == 1)
        {
          File file = (File)fileList.get(0);
          if (this.canCloseFile() != YesNoCancelResult.cancel)
          {
            this.readFile(file);
            evt.getDropTargetContext().dropComplete(true);
          }
          else
          {
            evt.getDropTargetContext().dropComplete(false);
          }
        }
        else
        {
          evt.getDropTargetContext().dropComplete(false);
          final Window w = SwingUtilities.getWindowAncestor(this);
          EventQueue.invokeLater(() ->
          {
            w.toFront();
            w.requestFocus();
            WbSwingUtilities.showErrorMessageKey(w, "ErrNoMultipleDrop");
          });
        }
      }
      else if (tr.isDataFlavorSupported(ObjectTreeTransferable.DATA_FLAVOR))
      {
        ObjectTreeTransferable selection = (ObjectTreeTransferable)tr.getTransferData(ObjectTreeTransferable.DATA_FLAVOR);
        EditorDropHandler handler = new EditorDropHandler(this);
        handler.handleDrop(selection, evt.getLocation());
        evt.getDropTargetContext().dropComplete(true);
        WbSwingUtilities.requestFocus(this);
      }
      else
      {
        evt.rejectDrop();
      }
    }
    catch (IOException | UnsupportedFlavorException ex)
    {
      LogMgr.logDebug("EditorPanel.drop()", "Error when processing drop event", ex);
      evt.rejectDrop();
    }
  }

  @Override
  public void dropActionChanged(java.awt.dnd.DropTargetDragEvent dropTargetDragEvent)
  {
  }

}
