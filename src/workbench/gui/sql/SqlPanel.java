/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Window;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.ComponentInputMap;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import workbench.WbManager;
import workbench.interfaces.Commitable;
import workbench.interfaces.Connectable;
import workbench.interfaces.DbExecutionListener;
import workbench.interfaces.DbExecutionNotifier;
import workbench.interfaces.DbUpdater;
import workbench.interfaces.ExecutionController;
import workbench.interfaces.Exporter;
import workbench.interfaces.FilenameChangeListener;
import workbench.interfaces.FontChangedListener;
import workbench.interfaces.Interruptable;
import workbench.interfaces.JobErrorHandler;
import workbench.interfaces.MainPanel;
import workbench.interfaces.Moveable;
import workbench.interfaces.ParameterPrompter;
import workbench.interfaces.ResultLogger;
import workbench.interfaces.ResultReceiver;
import workbench.interfaces.ScriptErrorHandler;
import workbench.interfaces.StatusBar;
import workbench.interfaces.ToolWindow;
import workbench.interfaces.ToolWindowManager;
import workbench.log.LogMgr;
import workbench.resource.AutoFileSaveType;
import workbench.resource.ErrorPromptType;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.ColumnIdentifier;
import workbench.db.DbSettings;
import workbench.db.TableIdentifier;
import workbench.db.TableSelectBuilder;
import workbench.db.TransactionChecker;
import workbench.db.WbConnection;
import workbench.db.exporter.DataExporter;
import workbench.db.importer.DataStoreImporter;
import workbench.db.importer.DefaultImportOptions;
import workbench.db.importer.DefaultTextImportOptions;
import workbench.db.importer.ImportOptions;
import workbench.db.importer.TextImportOptions;

import workbench.gui.ErrorContinueDialog;
import workbench.gui.MainWindow;
import workbench.gui.PanelReloader;
import workbench.gui.WbSwingUtilities;
import workbench.gui.WindowTitleBuilder;
import workbench.gui.actions.AppendResultsAction;
import workbench.gui.actions.AutoCompletionAction;
import workbench.gui.actions.AutoJumpNextStatement;
import workbench.gui.actions.CheckPreparedStatementsAction;
import workbench.gui.actions.CleanJavaCodeAction;
import workbench.gui.actions.ClearCompletionCacheAction;
import workbench.gui.actions.ClearMessagesAction;
import workbench.gui.actions.CloseAllResultsAction;
import workbench.gui.actions.CloseResultTabAction;
import workbench.gui.actions.CommitAction;
import workbench.gui.actions.ConsolidateLogAction;
import workbench.gui.actions.CopyAsDbUnitXMLAction;
import workbench.gui.actions.CopyAsSqlDeleteAction;
import workbench.gui.actions.CopyAsSqlDeleteInsertAction;
import workbench.gui.actions.CopyAsSqlInsertAction;
import workbench.gui.actions.CopyAsSqlMergeAction;
import workbench.gui.actions.CopyAsSqlUpdateAction;
import workbench.gui.actions.CopyAsTextAction;
import workbench.gui.actions.CopyCurrentStatementAction;
import workbench.gui.actions.CopyRowAction;
import workbench.gui.actions.CreateDeleteScriptAction;
import workbench.gui.actions.CreateSnippetAction;
import workbench.gui.actions.DeleteDependentRowsAction;
import workbench.gui.actions.DeleteRowAction;
import workbench.gui.actions.DisplayDataFormAction;
import workbench.gui.actions.ExecuteAllAction;
import workbench.gui.actions.ExecuteCurrentAction;
import workbench.gui.actions.ExecuteFromCursorAction;
import workbench.gui.actions.ExecuteSelAction;
import workbench.gui.actions.ExecuteUpToCursorAction;
import workbench.gui.actions.ExpandEditorAction;
import workbench.gui.actions.ExpandResultAction;
import workbench.gui.actions.FileDiscardAction;
import workbench.gui.actions.FilterDataAction;
import workbench.gui.actions.FilterPickerAction;
import workbench.gui.actions.FindAction;
import workbench.gui.actions.FindDataAction;
import workbench.gui.actions.FindDataAgainAction;
import workbench.gui.actions.FormatSqlAction;
import workbench.gui.actions.HighlightCurrentStatement;
import workbench.gui.actions.HighlightErrorLineAction;
import workbench.gui.actions.IgnoreErrorsAction;
import workbench.gui.actions.ImportClipboardAction;
import workbench.gui.actions.ImportFileAction;
import workbench.gui.actions.InsertRowAction;
import workbench.gui.actions.JoinCompletionAction;
import workbench.gui.actions.JumpToStatement;
import workbench.gui.actions.MakeInListAction;
import workbench.gui.actions.MakeLowerCaseAction;
import workbench.gui.actions.MakeNonCharInListAction;
import workbench.gui.actions.MakeUpperCaseAction;
import workbench.gui.actions.OptimizeAllColumnsAction;
import workbench.gui.actions.OptimizeRowHeightAction;
import workbench.gui.actions.PrintAction;
import workbench.gui.actions.PrintPreviewAction;
import workbench.gui.actions.ReplaceDataAction;
import workbench.gui.actions.ResetFilterAction;
import workbench.gui.actions.ResetHighlightAction;
import workbench.gui.actions.RollbackAction;
import workbench.gui.actions.SaveDataAsAction;
import workbench.gui.actions.SelectEditorAction;
import workbench.gui.actions.SelectKeyColumnsAction;
import workbench.gui.actions.SelectMaxRowsAction;
import workbench.gui.actions.SelectResultAction;
import workbench.gui.actions.SelectionFilterAction;
import workbench.gui.actions.ShowObjectInfoAction;
import workbench.gui.actions.SpoolDataAction;
import workbench.gui.actions.SqlPanelReloadAction;
import workbench.gui.actions.StopAction;
import workbench.gui.actions.ToggleAutoCommitAction;
import workbench.gui.actions.ToggleSelectionHighlightAction;
import workbench.gui.actions.UndoExpandAction;
import workbench.gui.actions.UpdateDatabaseAction;
import workbench.gui.actions.ViewMessageLogAction;
import workbench.gui.actions.WbAction;
import workbench.gui.bookmarks.BookmarkAnnotation;
import workbench.gui.bookmarks.NamedScriptLocation;
import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.DbUnitHelper;
import workbench.gui.components.EtchedBorderTop;
import workbench.gui.components.GenericRowMonitor;
import workbench.gui.components.TabCloser;
import workbench.gui.components.WbMenu;
import workbench.gui.components.WbScrollPane;
import workbench.gui.components.WbSplitPane;
import workbench.gui.components.WbTabbedPane;
import workbench.gui.components.WbTable;
import workbench.gui.components.WbToolbar;
import workbench.gui.dbobjects.objecttree.DbTreeSettings;
import workbench.gui.dbobjects.objecttree.FindObjectAction;
import workbench.gui.dbobjects.objecttree.ObjectFinder;
import workbench.gui.dbobjects.objecttree.ResultTabDropHandler;
import workbench.gui.dialogs.dataimport.ImportFileDialog;
import workbench.gui.dialogs.export.ExportFileDialog;
import workbench.gui.editor.InsertTipProvider;
import workbench.gui.editor.actions.IndentSelection;
import workbench.gui.editor.actions.ShowTipAction;
import workbench.gui.editor.actions.UnIndentSelection;
import workbench.gui.macros.MacroClient;
import workbench.gui.menu.TextPopup;
import workbench.gui.preparedstatement.ParameterEditor;
import workbench.gui.toolbar.MainToolbar;
import workbench.gui.toolbar.ToolbarBuilder;

import workbench.storage.DataStore;

import workbench.sql.AppendResultAnnotation;
import workbench.sql.ErrorDescriptor;
import workbench.sql.OutputPrinter;
import workbench.sql.StatementHistory;
import workbench.sql.StatementRunner;
import workbench.sql.StatementRunnerResult;
import workbench.sql.UseTabAnnotation;
import workbench.sql.VariablePool;
import workbench.sql.commands.TransactionEndCommand;
import workbench.sql.macros.MacroManager;
import workbench.sql.parser.ParserType;
import workbench.sql.parser.ScriptParser;
import workbench.sql.preparedstatement.PreparedStatementPool;
import workbench.sql.preparedstatement.StatementParameters;

import workbench.util.CollectionUtil;
import workbench.util.DurationFormat;
import workbench.util.DurationFormatter;
import workbench.util.ExceptionUtil;
import workbench.util.HtmlUtil;
import workbench.util.LowMemoryException;
import workbench.util.MemoryWatcher;
import workbench.util.MessageBuffer;
import workbench.util.NumberStringCache;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;
import workbench.util.WbThread;
import workbench.util.WbWorkspace;


/**
 * A panel with an SQL editor (EditorPanel), a log panel and
 * a panel for displaying SQL results (DwPanel)
 *
 * @author Thomas Kellerer
 */
public class SqlPanel
	extends JPanel
	implements FontChangedListener, PropertyChangeListener, ChangeListener,
		MainPanel, Exporter, DbUpdater, Interruptable, Commitable,
		JobErrorHandler, ExecutionController, ResultLogger, ParameterPrompter, DbExecutionNotifier,
		FilenameChangeListener, ResultReceiver, MacroClient, Moveable, TabCloser, StatusBar, ToolWindowManager, OutputPrinter, PanelReloader,
    ScriptErrorHandler
{
	//<editor-fold defaultstate="collapsed" desc=" Variables ">
	protected EditorPanel editor;
	protected DwPanel currentData;
	protected SqlHistory sqlHistory;
	protected StatementHistory historyStatements;

	protected LogArea log;
	protected WbTabbedPane resultTab;
	protected WbSplitPane contentPanel;
	protected boolean threadBusy;
	protected volatile boolean cancelExecution;

	private final List actions = new ArrayList(50);
	private final List<FilenameChangeListener> filenameChangeListeners = new ArrayList<>();

	protected StopAction stopAction;
	protected ExecuteAllAction executeAll;
	protected ExecuteCurrentAction executeCurrent;
  protected ExecuteFromCursorAction executeFromCurrent;
  protected ExecuteUpToCursorAction executeToCursor;
	protected ExecuteSelAction executeSelected;

	private static int instanceCount = 0;
	private final int internalId;

	// Actions from DwPanel
	protected CopyAsTextAction dataToClipboard;
	protected SaveDataAsAction exportDataAction;
	protected CopyAsDbUnitXMLAction copyAsDBUnitXML;
	protected CopyAsSqlInsertAction copyAsSqlInsert;
	protected CopyAsSqlUpdateAction copyAsSqlUpdate;
	protected CopyAsSqlDeleteInsertAction copyAsSqlDeleteInsert;
	protected CopyAsSqlDeleteAction copyAsSqlDelete;
	protected CopyAsSqlMergeAction copyAsSqlMerge;
	protected CreateDeleteScriptAction createDeleteScript;
	protected ImportFileAction importFileAction;
	protected ImportClipboardAction importClipAction;
	protected PrintAction printDataAction;
	protected PrintPreviewAction printPreviewAction;
	protected UpdateDatabaseAction updateAction;
	protected InsertRowAction insertRow;
	protected CopyRowAction duplicateRow;
	protected DeleteRowAction deleteRow;
	protected DeleteDependentRowsAction deleteDependentRow;
	protected SelectKeyColumnsAction selectKeys;
	protected DisplayDataFormAction showFormAction;
	protected FilterDataAction filterAction;
	protected SelectionFilterAction selectionFilterAction;
	protected FilterPickerAction filterPicker;
	protected ResetFilterAction resetFilterAction;
	protected OptimizeAllColumnsAction optimizeAllCol;
	protected OptimizeRowHeightAction optimizeRowHeights;
	protected AppendResultsAction appendResultsAction;
	protected CloseResultTabAction closeResultAction;
	protected CloseAllResultsAction closeAllResultsAction;
	protected ClearCompletionCacheAction clearCompletionCache;
	protected AutoCompletionAction autoCompletion;
	protected SqlPanelReloadAction reloadAction;
	protected ShowObjectInfoAction showObjectInfoAction;
	protected JoinCompletionAction joinCompletion;
	protected CopyCurrentStatementAction copyStatementAction;
  protected IgnoreErrorsAction ignoreErrors;

	protected WbMenu copyAsSQLMenu;
	protected WbMenu copySelectedMenu;
	protected ToggleAutoCommitAction toggleAutoCommit;
	protected ToggleSelectionHighlightAction toggleSelectionHilite;
	protected CommitAction commitAction;
	protected RollbackAction rollbackAction;

	protected FormatSqlAction formatSql;
	protected SpoolDataAction spoolData;

	protected FileDiscardAction fileDiscardAction;
	protected FindDataAction findDataAction;
	protected FindDataAgainAction findDataAgainAction;
	protected ReplaceDataAction replaceDataAction;
	protected ResetHighlightAction resetHighlightAction;

  protected FindObjectAction findInDbTree;

	protected MainToolbar toolbar;
	protected WbConnection dbConnection;

	protected boolean importRunning;
	protected boolean updateRunning;
	protected String tabName;

	private final List<DbExecutionListener> execListener = Collections.synchronizedList(new ArrayList<>());
	protected Thread executionThread;
	protected Interruptable worker;

	private boolean appendResults;

	protected DwStatusBar statusBar;
	protected StatementRunner stmtRunner;
	protected GenericRowMonitor rowMonitor;
	protected IconHandler iconHandler;
	private boolean locked;
	private boolean ignoreStateChange;
	private long lastScriptExecTime;
	protected final List<ToolWindow> resultWindows = new ArrayList<>(1);
	private final int macroClientId;
  private final AutomaticRefreshMgr refreshMgr;
  private final Highlighter highlighter;
  private ResultTabDropHandler tabDropHandler;
	private boolean macroExecution = false;

  private final Object toolbarLock = new Object();

//</editor-fold>

	public SqlPanel(int clientId)
	{
		super(new BorderLayout());
		internalId = ++instanceCount;

    setDoubleBuffered(true);

    refreshMgr = new AutomaticRefreshMgr();
		macroClientId = clientId;
		setName("sqlpanel-" + internalId);
		setBorder(WbSwingUtilities.EMPTY_BORDER);

		editor = EditorPanel.createSqlEditor();
    highlighter = new Highlighter(editor);

		statusBar = new DwStatusBar(true, true);

		int defRows = GuiSettings.getDefaultMaxRows();
		if (defRows > 0)
		{
			statusBar.setMaxRows(defRows);
		}
		editor.setStatusBar(statusBar);
		editor.setBorder(new EtchedBorderTop());

    if (GuiSettings.getShowTextSelectionSummary())
    {
      statusBar.addTextSelectionDisplay(editor);
    }

		// The name of the component is used for the Jemmy GUI Tests
		editor.setName("sqleditor" + internalId);

		log = new LogArea(this);
		// The name of the component is used for the Jemmy GUI Tests
		log.setName("msg" + internalId);

		resultTab = new WbTabbedPane();
		resultTab.setTabPlacement(JTabbedPane.TOP);
		resultTab.setFocusable(false);
		resultTab.enableDragDropReordering(this);
		resultTab.hideDisabledButtons(true);

		// The name of the component is used for the Jemmy GUI Tests
		resultTab.setName("resultspane");

		JScrollPane scroll = new WbScrollPane(log);
		resultTab.addTab(ResourceMgr.getString("LblTabMessages"), scroll);

		contentPanel = new WbSplitPane(JSplitPane.VERTICAL_SPLIT, editor, resultTab);
		contentPanel.setOneTouchExpandable(true);

		appendResults = GuiSettings.getDefaultAppendResults();

		this.add(contentPanel, BorderLayout.CENTER);
		this.add(statusBar, BorderLayout.SOUTH);

		this.initStatementHistory();

		this.initActions();
		this.setupActionMap();

		Settings s = Settings.getInstance();
		s.addFontChangedListener(this);

		rowMonitor = new GenericRowMonitor(this.statusBar);

		// The listeners have to be added as late as possible to ensure
		// that everything is created properly in case an event is fired
		resultTab.addChangeListener(this);
		editor.addFilenameChangeListener(this);
		new ResultTabHandler(this.resultTab, this);
		iconHandler = new IconHandler(this);

		if (GuiSettings.getShowResultTabCloseButton())
		{
			resultTab.showCloseButton(this);
		}
		else
		{
			resultTab.showCloseButton(null);
		}

		tabName = ResourceMgr.getDefaultTabLabel();
		Settings.getInstance().addPropertyChangeListener(this,
      ToolbarBuilder.CONFIG_PROPERTY,
      GuiSettings.PROPERTY_RESULTTAB_CLOSE_BUTTON,
      GuiSettings.PROP_SHOW_TEXT_SELECTION_INFO);
		editor.setMacroExpansionEnabled(true, macroClientId);
		editor.setBracketCompletionEnabled(true);
		historyStatements = new StatementHistory(Settings.getInstance().getMaxHistorySize());

    stmtRunner = new StatementRunner();
    stmtRunner.setMacroClientId(macroClientId);
    stmtRunner.setRowMonitor(this.rowMonitor);
    stmtRunner.setMessagePrinter(this);
    stmtRunner.setResultLogger(this);
    stmtRunner.setRetryHandler(this);

    tabDropHandler = new ResultTabDropHandler(this, resultTab, log);
	}

	@Override
	public boolean supportsBookmarks()
	{
		return true;
	}

	@Override
	public List<NamedScriptLocation> getBookmarks()
	{
		if (editor == null) return null;
		String text = editor.getText();
		if (StringUtil.isEmptyString(text)) return Collections.emptyList();
		BookmarkAnnotation reader = new BookmarkAnnotation();
		ParserType type = ParserType.getTypeFromConnection(getConnection());
		List<NamedScriptLocation> bookmarks = reader.getBookmarks(text, getId(), type);
		for (NamedScriptLocation loc : bookmarks)
		{
			int line = editor.getLineOfOffset(loc.getOffset());
			loc.setLineNumber(line + 1);
		}
		return bookmarks;
	}

	@Override
	public boolean isModifiedAfter(long time)
	{
		return editor.isModifiedAfter(time);
	}

	@Override
	public void jumpToBookmark(NamedScriptLocation bookmark)
	{
		if (bookmark == null) return;
		int position = bookmark.getOffset();
		final int line = this.editor.getLineOfOffset(position);
		final int offset = editor.getLineStartOffset(line);
		EventQueue.invokeLater(() ->
    {
      editor.setCaretPosition(offset);
      editor.centerLine(line);
      editor.requestFocusInWindow();
    });
	}

  @Override
  public void registerObjectFinder(ObjectFinder finder)
  {
    findInDbTree.setFinder(finder);
    if (finder == null)
    {
      editor.removePopupMenuItem(findInDbTree);
    }
    else
    {
      editor.addPopupMenuItem(findInDbTree, false);
    }
  }

	public void setDividerLocation(int location)
	{
		if (contentPanel != null)
		{
			contentPanel.setDividerLocation(location);
		}
	}

	@Override
	public void setLocked(final boolean flag)
	{
		if (flag == locked) return;
		this.locked = flag;
		EventQueue.invokeLater(() ->
    {
      updateTabTitle();
      Component c = getParent();
      if (c instanceof WbTabbedPane)
      {
        ((WbTabbedPane)c).setCloseButtonEnabled(SqlPanel.this, !flag);
      }
    });
	}

	@Override
	public boolean isLocked()
	{
		return locked;
	}

	@Override
	public String getId()
	{
		return NumberStringCache.getNumberString(this.internalId);
	}

	@Override
	public void setConnectionClient(Connectable client)
	{
	}

	public boolean getAppendResults()
	{
		return appendResults;
	}

	public void setAppendResults(boolean flag)
	{
		this.appendResults = flag;
	}

	protected void updateAppendAction()
	{
		if (this.appendResultsAction != null) this.appendResultsAction.setSwitchedOn(appendResults);
	}


	public void initDivider(int height)
	{
		height -= (statusBar.getPreferredSize().getHeight() * 3);
		height -= editor.getHScrollBarHeight();
		height -= resultTab.getTabHeight();
		height -= contentPanel.getDividerSize();
		height -= 8;
		int loc = height / 2;
		if (loc <= 50) loc = -1;
		contentPanel.setDividerLocation(loc);
	}

	@Override
	public WbToolbar getToolbar(List<WbAction> globalActions, boolean createNew)
	{
    synchronized (toolbarLock)
    {
      if (this.toolbar == null || createNew)
      {
        if (toolbar != null)
        {
          toolbar.dispose();
        }
        ToolbarBuilder builder = new ToolbarBuilder(getAllActions(), globalActions);
        toolbar = builder.createToolbar();
        updateConnectionInfo();
      }
      return this.toolbar;
    }
	}

  public List<WbAction> getAllActions()
  {
    List<WbAction> result = new ArrayList<>(actions.size());
    for (Object obj : actions)
    {
      if (obj instanceof WbAction )
      {
        result.add((WbAction)obj);
      }
      else if (obj instanceof WbMenu)
      {
        WbMenu menu = (WbMenu)obj;
        result.addAll(menu.getAllActions());
      }
    }
    result.add(ignoreErrors);
    result.add(filterPicker);
    result.add(appendResultsAction);
    return result;
  }

	public boolean readFile(String aFilename, String encoding)
	{
		if (aFilename == null) return false;

		boolean result = false;

		File f = new File(aFilename);

		if (this.editor.readFile(f, encoding))
		{
			this.selectEditor();
			result = true;
		}
		else
		{
			iconHandler.removeIcon();
			result = false;
		}
		return result;
	}

	public boolean hasFileLoaded()
	{
		if (this.editor == null) return false;
		return this.editor.hasFileLoaded();
	}

	public boolean checkAndSaveFile()
	{
		if (this.editor == null) return true;
		int result = this.editor.checkAndSaveFile();
		return result != JOptionPane.CANCEL_OPTION;
	}

	/**
	 * For testing purposes, so the Unit test can access the messages
	 */
	public String getLogMessage()
	{
		return this.log.getText();
	}

	public EditorPanel getEditor()
	{
		return this.editor;
	}

	public void clearSqlHistory(boolean removeEditorText)
	{
		if (this.sqlHistory != null) this.sqlHistory.clear();
		this.editor.setText("");
	}

	public boolean closeFile(boolean emptyEditor)
	{
		return closeFile(emptyEditor, true);
	}

	public boolean closeFile(boolean emptyEditor, boolean checkUnsaved)
	{
		if (this.editor == null) return true;
		if (checkUnsaved)
		{
			boolean canClose = this.checkAndSaveFile();
			if (!canClose) return false;
		}
		if (this.editor.closeFile(emptyEditor))
    {
			this.selectEditorLater();
			this.clearSqlHistory(false);
			return true;
    }
		return false;
	}

	@Override
	public void fileNameChanged(Object sender, String newFilename)
	{
		if (sender == this.editor)
		{
			boolean hasFile = editor.hasFileLoaded();
			this.fileDiscardAction.setEnabled(hasFile);
			if (hasFile)
			{
				// clear a user-defined tab name if a file is loaded
				this.tabName = null;
			}
			fireFilenameChanged(newFilename);
		}
	}

	private void fireFilenameChanged(String aNewName)
	{
		updateTabTitle();
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

	@SuppressWarnings("unchecked")
	private void initActions()
	{
		WbAction a;
		this.executeAll = new ExecuteAllAction(this);
		this.executeSelected = new ExecuteSelAction(this);
		this.executeCurrent = new ExecuteCurrentAction(this);
		this.executeFromCurrent = new ExecuteFromCursorAction(this);
    this.executeToCursor = new ExecuteUpToCursorAction(this);

		MakeLowerCaseAction makeLower = new MakeLowerCaseAction(this.editor);
		MakeUpperCaseAction makeUpper = new MakeUpperCaseAction(this.editor);

		this.editor.showFindOnPopupMenu();
		this.editor.showFormatSql();

		this.editor.addPopupMenuItem(this.executeSelected, true);
		this.editor.addPopupMenuItem(this.executeAll, false);
		this.editor.addPopupMenuItem(this.executeCurrent, false);
		this.editor.addPopupMenuItem(this.executeFromCurrent, false);
		this.editor.addPopupMenuItem(this.executeToCursor, false);

		TextPopup pop = (TextPopup)this.editor.getRightClickPopup();

//		this.actions.add(editor.getFileOpenAction());
		this.actions.add(editor.getFileSaveAction());
		this.actions.add(editor.getFileSaveAsAction());

		this.actions.add(this.editor.getReloadAction());
		this.fileDiscardAction = new FileDiscardAction(this);
		this.actions.add(this.fileDiscardAction);

		this.actions.add(editor.getUndoAction());
		this.actions.add(editor.getRedoAction());

		a = pop.getCutAction();
		a.setCreateMenuSeparator(true);
		this.actions.add(a);
		this.actions.add(pop.getCopyAction());
		this.actions.add(pop.getPasteAction());

		a = pop.getClearAction();
		a.setCreateMenuSeparator(true);
		this.actions.add(a);
		this.actions.add(pop.getSelectAllAction());
		this.actions.add(editor.getColumnSelection());

		FindAction f = this.editor.getFindAction();
		f.setCreateMenuSeparator(true);
		this.actions.add(f);
		this.actions.add(this.editor.getFindNextAction());
		this.actions.add(this.editor.getFindPreviousAction());
		this.actions.add(this.editor.getReplaceAction());

		IndentSelection indent = new IndentSelection(editor);
		indent.setCreateMenuSeparator(true);
		actions.add(indent);
		actions.add(new UnIndentSelection(editor));

		this.actions.add(makeLower);
		this.actions.add(makeUpper);
		this.actions.add(this.editor.getToggleCommentAction());
		this.actions.add(this.editor.getCommentAction());
		this.actions.add(this.editor.getUnCommentAction());
		this.actions.add(this.editor.getMatchBracketAction());

		this.toggleSelectionHilite = new ToggleSelectionHighlightAction(this.editor);
		this.actions.add(this.toggleSelectionHilite);

		// The update actions are proxies for the real ones
		// Once a result tab (DwPanel) has been displayed
		// they are "dispatched" to the real ones
		this.updateAction = new UpdateDatabaseAction(null);
		this.updateAction.setEnabled(GuiSettings.getAlwaysEnableSaveButton());
		this.insertRow = new InsertRowAction(null);
		this.deleteRow = new DeleteRowAction(null);
		this.deleteDependentRow = new DeleteDependentRowsAction(null);
		this.duplicateRow = new CopyRowAction(null);
		this.selectKeys = new SelectKeyColumnsAction(null);
		this.showFormAction = new DisplayDataFormAction(null);
		reloadAction = new SqlPanelReloadAction(this);
		showObjectInfoAction = new ShowObjectInfoAction(this);
		editor.addPopupMenuItem(showObjectInfoAction, true);

		this.actions.add(this.showFormAction);
		this.actions.add(this.selectKeys);
		this.actions.add(this.updateAction);
		this.actions.add(this.insertRow);
		this.actions.add(this.duplicateRow);
		this.actions.add(this.reloadAction);
		deleteRow.setCreateMenuSeparator(true);
		this.actions.add(this.deleteRow);
		this.actions.add(this.deleteDependentRow);

		this.createDeleteScript = new CreateDeleteScriptAction(null);
		this.actions.add(this.createDeleteScript);

		this.exportDataAction = new SaveDataAsAction(null);
		this.exportDataAction.setCreateMenuSeparator(true);
		this.exportDataAction.setEnabled(false);

		SelectEditorAction sea = new SelectEditorAction(this);
		sea.setCreateMenuSeparator(true);
		this.actions.add(sea);
		this.actions.add(new SelectMaxRowsAction(this.statusBar));
		this.actions.add(new ClearMessagesAction(this));

		SelectResultAction selectResult = new SelectResultAction(this);
		selectResult.setCreateMenuSeparator(true);
		this.actions.add(selectResult);
		this.actions.add(new ViewMessageLogAction(this));

		SplitPaneExpander expander = contentPanel.getExpander();
		a = new ExpandEditorAction(expander);
		a.setCreateMenuSeparator(true);
		this.actions.add(a);
		this.actions.add(new ExpandResultAction(expander));
		this.actions.add(new UndoExpandAction(expander));

		this.optimizeAllCol = new OptimizeAllColumnsAction(null);
		this.optimizeAllCol.setCreateMenuSeparator(true);
		this.optimizeAllCol.setEnabled(false);
		this.optimizeAllCol.removeIcon();
		this.optimizeAllCol.setMenuItemName(ResourceMgr.MNU_TXT_VIEW);
		this.actions.add(this.optimizeAllCol);

		this.optimizeRowHeights = new OptimizeRowHeightAction();
		this.optimizeRowHeights.setCreateMenuSeparator(false);
		this.optimizeRowHeights.setEnabled(false);
		this.optimizeRowHeights.removeIcon();
		this.optimizeRowHeights.setMenuItemName(ResourceMgr.MNU_TXT_VIEW);
		this.actions.add(this.optimizeRowHeights);

		this.dataToClipboard = new CopyAsTextAction(null);
		this.dataToClipboard.setEnabled(false);
		this.actions.add(this.exportDataAction);
		this.actions.add(this.dataToClipboard);

		copyAsSQLMenu = WbTable.createCopyAsSQLMenu();
		copyAsSQLMenu.setEnabled(false);

		if (DbUnitHelper.isDbUnitAvailable())
		{
			copyAsDBUnitXML = new CopyAsDbUnitXMLAction(null);
			copyAsSQLMenu.add(this.copyAsDBUnitXML);
		}

		copyAsSqlInsert = new CopyAsSqlInsertAction(null);
		copyAsSQLMenu.add(this.copyAsSqlInsert);

		copyAsSqlUpdate = new CopyAsSqlUpdateAction(null);
		copyAsSQLMenu.add(this.copyAsSqlUpdate);

		copyAsSqlMerge = new CopyAsSqlMergeAction(null);
		copyAsSQLMenu.add(copyAsSqlMerge);

		copyAsSqlDeleteInsert = new CopyAsSqlDeleteInsertAction(null);
		copyAsSQLMenu.add(this.copyAsSqlDeleteInsert);

		copyAsSqlDelete = new CopyAsSqlDeleteAction(null);
		copyAsSQLMenu.add(copyAsSqlDelete);

		actions.add(copyAsSQLMenu);

		copySelectedMenu = WbTable.createCopySelectedMenu();
		copySelectedMenu.setEnabled(false);
		this.actions.add(copySelectedMenu);

		this.importFileAction = new ImportFileAction(this);
		this.importFileAction.setCreateMenuSeparator(true);
		this.actions.add(this.importFileAction);

		this.importClipAction = new ImportClipboardAction(this);
		this.actions.add(this.importClipAction);

		this.printDataAction = new PrintAction(null);
		this.printPreviewAction = new PrintPreviewAction(null);

		this.actions.add(this.executeAll);
		this.actions.add(this.executeSelected);
		this.actions.add(this.executeCurrent);
		this.actions.add(this.executeFromCurrent);
		this.actions.add(this.executeToCursor);

		this.spoolData = new SpoolDataAction(this, "MnuTxtSpoolSql");
		this.spoolData.setCreateMenuSeparator(true);
		this.spoolData.setEditor(this.editor);
		this.actions.add(this.spoolData);

		this.stopAction = new StopAction(this);
		this.stopAction.setEnabled(false);
		this.stopAction.setCreateMenuSeparator(false);
		this.actions.add(this.stopAction);

		this.commitAction = new CommitAction(this);
		this.commitAction.setCreateMenuSeparator(true);
		this.commitAction.setEnabled(false);
		this.actions.add(this.commitAction);
		this.rollbackAction = new RollbackAction(this);
		this.rollbackAction.setEnabled(false);
		this.actions.add(this.rollbackAction);
		this.toggleAutoCommit = new ToggleAutoCommitAction();
		this.actions.add(this.toggleAutoCommit);

		this.actions.add(this.sqlHistory.getShowFirstStatementAction());
		this.actions.add(this.sqlHistory.getShowPreviousStatementAction());
		this.actions.add(this.sqlHistory.getShowNextStatementAction());
		this.actions.add(this.sqlHistory.getShowLastStatementAction());
		this.actions.add(this.sqlHistory.getClearHistoryAction());

		actions.add(new JumpToStatement(this));
		actions.add(editor.getJumpToLineAction());

		this.filterAction = new FilterDataAction(null);
		this.selectionFilterAction = new SelectionFilterAction();
		this.filterPicker = new FilterPickerAction(null);

		filterAction.setCreateMenuSeparator(true);
		this.resetFilterAction = new ResetFilterAction(null);

    ignoreErrors = new IgnoreErrorsAction();
		this.appendResultsAction = new AppendResultsAction(this);
		this.appendResultsAction.setEnabled(false);
		this.findDataAction = new FindDataAction(null);
		this.findDataAction.setMenuTextByKey("MnuTxtFindData");
		this.findDataAction.setEnabled(false);
		this.findDataAction.setCreateMenuSeparator(true);
		this.findDataAgainAction = new FindDataAgainAction(null);
		this.findDataAgainAction.setMenuTextByKey("MnuTxtFindDataAgain");
		this.findDataAgainAction.setEnabled(false);
		this.replaceDataAction = new ReplaceDataAction(null);
		this.replaceDataAction.setEnabled(false);

		this.autoCompletion = new AutoCompletionAction(this.editor, this.statusBar);
		this.autoCompletion.setCreateMenuSeparator(true);
		this.actions.add(this.autoCompletion);

		this.joinCompletion = new JoinCompletionAction(this);
		this.actions.add(joinCompletion);

		ShowTipAction showTip = new ShowTipAction(editor, new InsertTipProvider(this));
		this.actions.add(showTip);

		this.clearCompletionCache = new ClearCompletionCacheAction();
		this.actions.add(clearCompletionCache);
		this.actions.add(showObjectInfoAction);

		this.formatSql = this.editor.getFormatSqlAction();
		this.formatSql.setCreateMenuSeparator(true);
		this.actions.add(this.formatSql);

		WbMenu config = new WbMenu(ResourceMgr.getString("MnuTxtSettings"));
		config.setParentMenuId(ResourceMgr.MNU_TXT_SQL);
		new AutoJumpNextStatement().addToMenu(config);
		appendResultsAction.addToMenu(config);
    config.addSeparator();
		new HighlightCurrentStatement().addToMenu(config);
		new HighlightErrorLineAction().addToMenu(config);
		ignoreErrors.addToMenu(config);
    new ConsolidateLogAction().addToMenu(config);

    config.addSeparator();
		new CheckPreparedStatementsAction().addToMenu(config);

		WbMenu codeTools = new WbMenu(ResourceMgr.getString("MnuTxtCodeTools"));
		codeTools.setParentMenuId(ResourceMgr.MNU_TXT_SQL);
		new CreateSnippetAction(this.editor).addToMenu(codeTools);
		new CleanJavaCodeAction(this.editor).addToMenu(codeTools);
		new MakeInListAction(this.editor).addToMenu(codeTools);
		new MakeNonCharInListAction(this.editor).addToMenu(codeTools);
		copyStatementAction = new CopyCurrentStatementAction(this.editor);
		copyStatementAction.addToMenu(codeTools);
		this.actions.add(codeTools);
		this.actions.add(config);

		this.findDataAction.setCreateMenuSeparator(true);
		this.resetHighlightAction = new ResetHighlightAction(null);

		this.actions.add(this.findDataAction);
		this.actions.add(this.findDataAgainAction);
		this.actions.add(this.replaceDataAction);
		this.actions.add(this.resetHighlightAction);
		this.actions.add(filterAction);
		this.actions.add(selectionFilterAction);
		this.actions.add(this.resetFilterAction );
		closeResultAction = new CloseResultTabAction(this);
		closeResultAction.setCreateMenuSeparator(true);
		this.actions.add(closeResultAction);

		closeAllResultsAction = new CloseAllResultsAction(this);
		closeAllResultsAction.setCreateMenuSeparator(false);
		this.actions.add(closeAllResultsAction);

		this.printDataAction.setCreateMenuSeparator(true);
		this.actions.add(this.printDataAction);
		this.actions.add(this.printPreviewAction);
		editor.addKeyBinding(showTip);

    findInDbTree = new FindObjectAction(editor);
	}

	@Override
	public void setVisible(boolean flag)
	{
		super.setVisible(flag);
		if (!flag)
		{
			this.autoCompletion.closePopup();
		}
	}

	protected boolean isResultLocked(int index)
	{
		DwPanel panel = (DwPanel)resultTab.getComponentAt(index);
		return panel.isLocked();
	}

	protected boolean isResultModified(int index)
	{
		DwPanel panel = (DwPanel)resultTab.getComponentAt(index);
		return panel.isModified();
	}

	protected int getFirstModified()
	{
		if (this.currentData == null) return -1;
		if (this.resultTab.getTabCount() == 1) return -1;

		for (int i=0; i < resultTab.getTabCount() - 1; i++)
		{
			DwPanel panel = (DwPanel)resultTab.getComponentAt(i);
			if (panel.isModified()) return i;
		}
		return -1;
	}

	protected boolean isDataModified()
	{
    return getFirstModified() != -1;
	}

	@Override
	public boolean isModified()
	{
		if (isDataModified()) return true;
		if (!editor.hasFileLoaded()) return false;
		return editor.isModified();
	}

	private void setupActionMap()
	{
		InputMap im = new ComponentInputMap(this);
		ActionMap am = new ActionMap();
		//this.setInputMap(WHEN_IN_FOCUSED_WINDOW, im);
		this.setInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, im);
		this.setActionMap(am);

		Iterator itr = this.actions.iterator();
		while (itr.hasNext())
		{
			Object entry = itr.next();
			if (entry instanceof WbAction)
			{
				WbAction wb = (WbAction)entry;
				wb.addToInputMap(im, am);
			}
		}
		editor.getInputMap().setParent(im);
		editor.getActionMap().setParent(am);
	}

	public void selectEditorLater()
	{
		EventQueue.invokeLater(this::_selectEditor);
	}

	public void selectEditor()
	{
		WbSwingUtilities.invoke(this::_selectEditor);
	}

	protected void _selectEditor()
	{
		// make sure the panel and its window are really visible
		// before putting the focus to the editor
		// otherwise requestFocusInWindow() would bring the window to
		// front, which we do not want
		Window w = SwingUtilities.getWindowAncestor(this);
		if (w == null) return;

		if (w.isActive() && w.isVisible() && w.isFocused() && this.isCurrentTab() && editor != null)
		{
			editor.requestFocusInWindow();
		}
	}

	public void requestEditorFocus()
	{
		if (editor != null)
		{
			editor.requestFocus();
			editor.requestFocusInWindow();
		}
	}

	@Override
	public String getText()
	{
		if (editor == null) return null;
		return editor.getText();
	}

	@Override
	public String getSelectedText()
	{
		if (editor == null) return null;
		return editor.getSelectedText();
	}

	@Override
	public JComponent getPanel()
	{
		return this;
	}

	@Override
	public String getStatementAtCursor()
	{
		ScriptParser parser = ScriptParser.createScriptParser(dbConnection);
		parser.setScript(getEditor().getText());
		int index = parser.getCommandIndexAtCursorPos(getEditor().getCaretPosition());
		String currentStatement = parser.getCommand(index);
		return currentStatement;
	}

	private boolean isCurrentTab()
	{
		JTabbedPane parentTab = null;
		Component p = this.getParent();
		if (p instanceof JTabbedPane)
		{
			parentTab = (JTabbedPane)p;
		}
		if (parentTab == null) return false;

		return (parentTab.getSelectedComponent() == this);
	}

	/**
	 * Selects the first result table and tries to put the focus to that.
	 */
	public void selectResult()
	{
		if (this.isVisible() && this.isCurrentTab())
		{
			showResultPanel();
			EventQueue.invokeLater(() ->
      {
        if (currentData != null)
        {
          currentData.getTable().requestFocusInWindow();
        }
      });
		}
	}

	/**
	 * Saves any change to the current result set to the database.
	 * The saving is done in a background thread by updateDb()
	 *
	 * @see #updateDb()
	 */
	@Override
	public void saveChangesToDatabase(boolean confirm)
	{
		if (this.currentData == null)
		{
			Exception e = new IllegalStateException("No data panel!");
			LogMgr.logError("SqlPanel.saveChangesToDatabase()", "Save called without a current DwPanel!", e);
			return;
		}

		if (!this.currentData.prepareDatabaseUpdate(confirm)) return;

		setBusy(true);
		setCancelState(true);
		setConnActionsState(false);

		Thread t = new WbThread("Workbench DB Update Thread")
		{
			@Override
			public void run()
			{
				updateDb();
			}
		};
		t.start();
	}


	/**
	 * Does the actually saving of the database changes.
	 *
	 * The method is public to allow a direct call for GUI tests.
	 * Normally {@link #saveChangesToDatabase() } should be called to
	 * initiate the background thread.
	 */
	public void updateDb()
	{
		try
		{
			fireDbExecStart();
			this.updateRunning = true;
			setLogText(ResourceMgr.getString("MsgUpdatingDatabase") + "\n");
			this.currentData.saveChanges(this.dbConnection, this);
		}
		catch (OutOfMemoryError mem)
		{
			setLogText(ExceptionUtil.getDisplay(mem));
			iconHandler.showBusyIcon(false);
			EventQueue.invokeLater(() ->
      {
        WbSwingUtilities.showErrorMessageKey(SqlPanel.this, "MsgOutOfMemoryError");
      });
		}
		catch (Exception e)
		{
			setLogText(ExceptionUtil.getDisplay(e));
			LogMgr.logError("SqlPanel.updatedb()", "Error during update", e);
		}
		finally
		{
			this.updateRunning = false;
			this.setCancelState(false);
			this.setBusy(false);
			fireDbExecEnd();
			WbSwingUtilities.showDefaultCursor(this);
		}
		this.checkResultSetActions();
	}

	@Override
	public void panelSelected()
	{
		selectEditorLater();
	}

	@Override
	public List getMenuItems()
	{
		return Collections.unmodifiableList(this.actions);
	}

	/**
	 *	Show a message in the log panel. This will also switch
	 *	the display to the log panel (away from the result panel)
	 */
	@Override
	public void showLogMessage(String aMsg)
	{
		this.showLogPanel();
		setLogText(aMsg);
	}

	/**
	 *	Clear the message log, but do not switch the panel display to it.
	 */
	@Override
	public void clearLog()
	{
		setLogText("");
	}

	protected void setLogText(final String msg)
	{
		WbSwingUtilities.invoke(() ->
    {
      log.setText(msg);
    });
	}

	/**
	 *	Show the panel with the log messages.
	 */
	@Override
	public void showLogPanel()
	{
		WbSwingUtilities.invoke(() ->
    {
      int index = resultTab.getTabCount() - 1;
      resultTab.setSelectedIndex(index);
    });
	}

	@Override
	public void showResultPanel()
	{
		showResultPanel(0);
	}

	/**
	 *	Show the panel with the result set
	 */
	public void showResultPanel(final int index)
	{
    if (index < 0 || index > resultTab.getTabCount() - 1) return;

		WbSwingUtilities.invoke(() ->
    {
      resultTab.setSelectedIndex(index);
      Component comp = resultTab.getComponentAt(index);
      WbSwingUtilities.requestFocus((JComponent)comp);
    });
	}

	public StatusBar getStatusBar()
	{
		return statusBar;
	}

	@Override
	public void setStatusMessage(String message)
	{
		statusBar.setStatusMessage(message);
	}

	@Override
	public void setStatusMessage(String message, int duration)
	{
		statusBar.setStatusMessage(message, duration);
	}

	@Override
	public void doRepaint()
	{
		statusBar.doRepaint();
	}

	/**
	 *	Clear the message in the status bar of the DwPanel
	 */
	@Override
	public void clearStatusMessage()
	{
		this.statusBar.clearStatusMessage();
	}

	public final void initStatementHistory()
	{
		int size = Settings.getInstance().getMaxHistorySize();
		this.sqlHistory = new SqlHistory(editor,size);
	}

	@Override
	public void readFromWorkspace(WbWorkspace w, int index)
		throws IOException
	{
		PanelWorkspaceHandler handler = new PanelWorkspaceHandler(this);
		handler.readFromWorkspace(w, index);
	}

	private boolean confirmDiscardChanges(int index, boolean showResultName)
	{
    if (!GuiSettings.getConfirmDiscardResultSetChanges()) return true;
		if (index >= resultTab.getTabCount() - 1) return false;

		boolean isModified = (index == -1 ? isDataModified() : isResultModified(index));
		if (!isModified) return true;

		String title = null;
    if (showResultName)
    {
      if (index == -1)
      {
        index = getFirstModified();
      }
      title = resultTab.getTitleAt(index);
    }
    else
    {
      title = getRealTabTitle();
    }

		return WbSwingUtilities.getProceedCancel(this, "MsgDiscardTabChanges", HtmlUtil.cleanHTML(title));
	}

	private boolean confirmDiscardTransaction()
	{
		WbConnection con = getConnection();
		if (con == null) return true;
		TransactionChecker checker = con.getTransactionChecker();
		if (checker.hasUncommittedChanges(con))
		{
			String tabTitle = null;
			if (con.getProfile().getUseSeparateConnectionPerTab())
			{
				tabTitle = getRealTabTitle();
			}
			else
			{
				tabTitle = con.getProfile().getName();
			}
			return WbSwingUtilities.getProceedCancel(this, "MsgDiscardOpenTrans", HtmlUtil.cleanHTML(tabTitle));
		}
		return true;
	}

	/**
	 *  Do any work which should be done during the process of saving the
	 *  current workspace, but before the workspace file is actually opened!
	 *  This is to prevent a corrupted workspace due to interrupting the saving
	 *  because of the check for unsaved changes in the current editor file
	 */
	@Override
	public boolean canClosePanel(boolean checkTransaction)
	{
		boolean fileOk = this.checkAndSaveFile() && confirmDiscardChanges(-1, false);
		if (checkTransaction)
		{
			fileOk = fileOk && confirmDiscardTransaction();
		}
		return fileOk;
	}

	@Override
	public void saveToWorkspace(WbWorkspace w, int index)
		throws IOException
	{
		PanelWorkspaceHandler handler = new PanelWorkspaceHandler(this);
		handler.saveToWorkspace(w, index);
	}

	public SqlHistory getHistory()
	{
		return sqlHistory;
	}

	public String getRealTabTitle()
	{
		if (getParent() instanceof JTabbedPane)
		{
			JTabbedPane p = (JTabbedPane)getParent();
			int index = p.indexOfComponent(this);
			if (index > -1)
			{
				String t = p.getTitleAt(index);
				return t;
			}
		}
		return getTabTitle();
	}

	/**
	 * Implementation of the ResultReceiver interface.
	 */
	@Override
	public String getTitle()
	{
		return getTabTitle();
	}

	/**
	 * Returns the title that is shown for the current tab.
	 * This is not necessarily the same as the tabName because
	 * e.g. a filename could be appended to the tabName
	 */
	@Override
	public String getTabTitle()
	{
		String defaultLabel = ResourceMgr.getDefaultTabLabel();

		String fname = this.getCurrentFileName();
		if (fname != null)
		{
			File f = new File(fname);
			String tName = getTabName();
			if (tName == null || tName.startsWith(defaultLabel))
			{
				fname = f.getName();
			}
			else
			{
				fname = "[" + tName + "]";
			}
		}
		else
		{
			fname = this.getTabName();
		}
		if (fname == null) fname = defaultLabel;
		return fname;
	}

  public boolean toggleLockedResult()
  {
    DwPanel result = getCurrentResult();
    if (result == null) return false;

    result.setLocked(!result.isLocked());
    int index = resultTab.getSelectedIndex();

    String title = resultTab.getTitleAt(index);
    if (result.isLocked())
    {
      resultTab.setTitleAt(index, "<html><i>" + title + "</i></html>");
    }
    else
    {
      resultTab.setTitleAt(index, HtmlUtil.cleanHTML(title));
    }
    return result.isLocked();
  }

	protected void updateTabTitle()
	{
		PanelTitleSetter.updateTitle(this);
	}

	@Override
	public void setTabTitle(final JTabbedPane tab, final int index)
	{
		String fname = null;
		String tooltip = null;

		// don't mess with the icon if a statement is running
		if (!isBusy())
		{
			fname = this.getCurrentFileName();
			if (fname != null)
			{
				File f = new File(fname);
				tooltip = f.getAbsolutePath();
				if (editor != null)
				{
					tooltip += " (" + editor.getCurrentFileEncoding() + ")";
				}
				iconHandler.showIconForTab(iconHandler.getFileIcon());
			}
			else
			{
				iconHandler.removeIcon();
			}
		}
		PanelTitleSetter.setTabTitle(tab, this, index, getTabTitle());
		tab.setToolTipTextAt(index, tooltip);
	}

	public String getTabName()
	{
		return this.tabName;
	}

	@Override
	public void setTabName(String aName)
	{
		if (StringUtil.isBlank(aName))
		{
			this.tabName = null;
		}
		else
		{
			this.tabName = aName;
		}
		this.fireFilenameChanged(aName);
	}

	public String getCurrentFileName()
	{
		if (this.editor == null) return null;
		return this.editor.getCurrentFileName();
	}

	public void appendStatementText(String text)
	{
		this.editor.appendLine("\n");
		int pos = this.editor.getText().length();
		this.editor.appendLine(text);
		this.editor.setCaretPosition(pos);
		this.editor.scrollToCaret();
	}

	public void setStatementText(String text)
	{
		this.storeStatementInHistory();
		if (this.editor.getCurrentFile() != null) this.editor.saveCurrentFile();
		this.editor.closeFile(true);
		this.editor.setText(text);
    this.editor.setCaretPosition(0);
    this.editor.scrollToCaret();
	}

	public void addStatement(String text)
	{
    this.editor.insertText("\n");
    this.editor.insertText(text);
    this.editor.scrollToCaret();
	}

	@Override
	public String toString()
	{
		return this.getTabTitle();
	}

	@Override
	public void disconnect()
	{
    if (this.dbConnection != null)
    {
      this.setConnection(null);
    }
    if (this.currentData != null)
    {
      currentData.endEdit();
    }
    clearResultTabs(false);
    for (ToolWindow window : resultWindows)
    {
      window.disconnect();
    }
    setLogText("");
	}

	@Override
	public WbConnection getConnection()
	{
		return this.dbConnection;
	}

	@Override
	public boolean isConnected()
	{
		// I'm only checking if the connection is defined, because
		// MainWindow will make sure a valid connection is set
		// for the panel. When using only one connection for all
		// panels, isClosed() will block the entire AWT thread!
		return (this.dbConnection != null);
	}

	@Override
	public void setConnection(final WbConnection aConnection)
	{
    if (this.dbConnection != null)
    {
      this.dbConnection.removeChangeListener(this);
    }

    this.dbConnection = aConnection;

		this.toggleAutoCommit.setConnection(this.dbConnection);

		if (this.clearCompletionCache != null) this.clearCompletionCache.setConnection(this.dbConnection);
		if (this.autoCompletion != null) this.autoCompletion.setConnection(this.dbConnection);
		if (showObjectInfoAction != null) showObjectInfoAction.checkEnabled();
    if (findInDbTree != null) findInDbTree.setEditorConnection(this.dbConnection);

		if (this.stmtRunner != null)
		{
			this.stmtRunner.setConnection(aConnection);
			this.stmtRunner.setResultLogger(this);
			this.stmtRunner.setHistoryProvider(this.historyStatements);
		}

		if (this.editor != null) this.editor.setDatabaseConnection(this.dbConnection);
		if (this.copyStatementAction != null) this.copyStatementAction.setConnection(this.dbConnection);

		checkResultSetActions();
		checkCommitAction();

		setConnActionsState(false);

		if (this.dbConnection != null)
		{
			dbConnection.addChangeListener(this);
		}
    updateConnectionInfo();
	}

  private void updateConnectionInfo()
  {
    // ConnectionInfo.setConnection() might access the database (to retrieve the current schema, database and user)
    // In order to not block the GUI this is done in a separate thread.
    WbThread info = new WbThread("Update connection info " + this.getId())
    {
      @Override
      public void run()
      {
        try
        {
          if (toolbar != null)
          {
            toolbar.setConnection(dbConnection);
          }

          // avoid the <IDLE> in transaction for Postgres that is caused by retrieving the current schema.
          // the second check for isBusy() is to prevent the situation where the user manages
          // to manually run a statement between the above setBusy(false) and this point)
          if (dbConnection != null && doRollbackOnSetConnection())
          {
            LogMgr.logDebug("SqlPanel.updateConnectionInfo()", "Sending a rollback to end the current transaction");
            dbConnection.rollbackSilently();
          }
        }
        finally
        {
          setConnActionsState(dbConnection != null);
        }
      }
    };
    info.start();
  }

	private boolean doRollbackOnSetConnection()
	{
		if (dbConnection == null) return false;
		DbSettings dbs = dbConnection.getDbSettings();
		if (dbs != null)
		{
			if (!dbs.endTransactionAfterConnect()) return false;
		}
		if (dbConnection.getAutoCommit()) return false;
		if (dbConnection.isBusy()) return false;
		if (dbConnection.getProfile() == null) return false;

		// if we are using a separate connection, we always need to do the rollback
		if (dbConnection.getProfile().getUseSeparateConnectionPerTab()) return true;

		// a single connection is used for all tabs. Only terminate the transaction for the current tab.
		return this.isCurrentTab();
	}

	/**
	 * Check the autoCommit property of the current connection
	 * and enable/disable the rollback and commit actions
	 * accordingly
	 */
	protected void checkCommitAction()
	{
		EventQueue.invokeLater(() ->
    {
      if (dbConnection != null)
      {
        // if autocommit is enabled, then rollback and commit will
        // be disabled
        boolean flag = dbConnection.getAutoCommit();
        commitAction.setEnabled(!flag);
        rollbackAction.setEnabled(!flag);
      }
      else
      {
        commitAction.setEnabled(false);
        rollbackAction.setEnabled(false);
      }
    });
	}

	@Override
	public boolean isRequestFocusEnabled()
	{
		return true;
	}

	public void storeStatementInHistory()
	{
		this.sqlHistory.addContent(editor);
	}

	public void cancelUpdate()
	{
		if (this.currentData == null) return;

		WbTable table = this.currentData.getTable();
		if (table != null)
		{
			DataStoreTableModel model = (DataStoreTableModel)table.getModel();
			if (model == null) return;
			DataStore ds = table.getDataStore();
			if (ds == null) return;
			ds.cancelUpdate();

			if (!this.dbConnection.getAutoCommit())
			{
				String msg = ResourceMgr.getString("MsgCommitPartialUpdate");
				WbSwingUtilities.TransactionEnd action = WbSwingUtilities.getCommitRollbackQuestion(this, msg);
				try
				{
					if (action == WbSwingUtilities.TransactionEnd.Rollback)
					{
						this.dbConnection.commit();
						ds.resetStatusForSentRows();
					}
					else
					{
						this.dbConnection.rollback();
						ds.resetDmlSentStatus();
					}

				}
				catch (SQLException e)
				{
					LogMgr.logError("SqlPanel.cancelExecution()", "Commit failed!", e);
					msg = e.getMessage();
					WbSwingUtilities.showErrorMessage(this, msg);
				}
				currentData.rowCountChanged();
				WbSwingUtilities.repaintLater(this);
			}
			else
			{
				ds.resetStatusForSentRows();
			}
		}
		this.setCancelState(false);
	}

	public void forceAbort()
	{
		if (!this.isBusy()) return;
		if (this.executionThread == null) return;
		try
		{
			String name = executionThread.getName();
			this.cancelExecution = true;
			this.executionThread.interrupt();
			this.executionThread = null;
			if (this.stmtRunner != null) this.stmtRunner.abort();
			LogMgr.logDebug("SqlPanel.forceAbort()", "'" + name + "' was interrupted.");
		}
		catch (Exception e)
		{
			LogMgr.logWarning("SqlPanel.forceAbort()", "Error when trying to kill background thread",e);
		}
		finally
		{
			this.setBusy(false);
		}
	}

	/**
	 * Try to abort the currently running statement by interrupting the execution thread.
	 *
	 * This method will wait join the execution thread and will wait until it's finished
	 * but only for a default of 5 seconds.
	 */
	public void abortExecution()
	{
		if (this.isCancelling())
		{
			// if we are already trying to cancel the statement, there is not much
			// hope that it will succeed (otherwise the user wouldn't have closed the windw)
			forceAbort();
			return;
		}

		if (!this.isBusy()) return;
		if (this.executionThread == null) return;
		long wait = Settings.getInstance().getIntProperty(this.getClass().getName() + ".abortwait", 5);
		try
		{
			LogMgr.logDebug("SqlPanel.abortExecution()", "Interrupting SQL Thread...");
			this.cancelExecution = true;
			this.executionThread.interrupt();
			this.executionThread.join(wait * 1000);
			if (this.isBusy())
			{
				// execution could not be interrupted --> force a stop of the command
				this.stmtRunner.abort();
				LogMgr.logDebug("SqlPanel.abortExecution()", "SQL Thread still running after " + wait +"s!");
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("SqlPanel.abortExecution()", "Error when interrupting SQL thread", e);
		}
	}

	@Override
	public boolean confirmCancel()
	{
		return true;
	}

	/**
	 *	Implementation of the Interruptable Interface.
	 */
	@Override
	public void cancelExecution()
	{
		if (!this.isBusy()) return;

		setStatusMessage(ResourceMgr.getString("MsgCancellingStmt") + "\n");
		iconHandler.showCancelIcon();
		try
		{
			if (worker != null)
			{
				worker.cancelExecution();
			}
			else if (updateRunning)
			{
				cancelUpdate();
			}
			else
			{
				WbThread t = new WbThread("SqlPanel " + this.getId() + " Cancel Thread")
				{
					@Override
					public void run()
					{
						cancelRetrieve();
					}
				};
				t.start();
			}
		}
		catch (Throwable th)
		{
			LogMgr.logError("SqlPanel.cancelExecution()", "Error cancelling execution", th);
		}
	}

	@Override
	public boolean isCancelling()
	{
		return cancelExecution;
	}

	protected void cancelRetrieve()
	{
		cancelExecution = true;
		setCancelState(false);
		stmtRunner.cancel();
		Thread.yield();
		if (this.executionThread != null)
		{
			// Wait for the execution thread to finish because of the cancel() call.
			// but wait at most 2.5 seconds (configurable) for the statement to respond
			// to the cancel() call. Depending on the sate of the statement, calling Statement.cancel()
			// might not have any effect.
			try
			{
				executionThread.join(Settings.getInstance().getIntProperty("workbench.sql.cancel.timeout", 5000));
			}
			catch (InterruptedException ex)
			{
				LogMgr.logDebug("SqlPanel.cancelRetrieve()", "Error when waiting for cancel to finish", ex);
			}
		}
		// Apparently cancelling the SQL statement did not work, so kill it the brutal way...
		if (this.executionThread != null && executionThread.isAlive())
		{
			executionThread.interrupt();
		}
	}

	public void setCancelState(final boolean aFlag)
	{
		setActionState(stopAction, aFlag);
	}

	/**
	 *	Modify the enabled state of the given action.
	 */
	public void setActionState(final Action anAction, final boolean aFlag)
	{
		setActionState(new Action[] { anAction }, aFlag);
	}

	public void setActionState(final Action[] anActionList, final boolean aFlag)
	{
		WbSwingUtilities.invoke(() ->
    {
      for (Action action : anActionList)
      {
        if (action != null) action.setEnabled(aFlag);
      }
    });
	}

	public void runCurrentStatement()
	{
		String sql = this.editor.getText();
		startExecution(sql, 0, GuiSettings.getHighlightErrorStatement(), this.appendResults, RunType.RunCurrent);
	}

	public void runFromCursor()
	{
		String sql = this.editor.getText();
		startExecution(sql, 0, GuiSettings.getHighlightErrorStatement(), this.appendResults, RunType.RunFromCursor);
	}

	public void runToCursor()
	{
		String sql = this.editor.getText();
		startExecution(sql, 0, GuiSettings.getHighlightErrorStatement(), this.appendResults, RunType.RunToCursor);
	}

	public void runSelectedStatement()
	{
		String sql = this.editor.getSelectedStatement();
		int offset = 0;
		boolean highlight = GuiSettings.getHighlightErrorStatement();
		if (this.editor.isTextSelected())
		{
			offset = this.editor.getSelectionStart();
			highlight = false;
		}
		this.startExecution(sql, offset, highlight, this.appendResults, RunType.RunAll);
	}

	@Override
	public void commit()
	{
		this.startExecution(TransactionEndCommand.COMMIT_VERB, 0, false, this.appendResults, RunType.RunAll);
	}

	@Override
	public void rollback()
	{
		this.startExecution(TransactionEndCommand.ROLLBACK_VERB, 0, false, this.appendResults, RunType.RunAll);
	}

	public void runAll()
	{
		String sql = this.editor.getText();
		this.startExecution(sql, 0, GuiSettings.getHighlightErrorStatement(), this.appendResults, RunType.RunAll);
	}

	private void startExecution(final String sql, final int offset, final boolean highlightError, final boolean appendResult, final RunType runType)
	{
		if (this.isConnectionBusy()) return;

    if (!appendResult && !confirmDiscardChanges(-1, true))
    {
      return;
    }

    if (this.executionThread != null || (dbConnection != null && this.dbConnection.isBusy()))
    {
      showLogMessage(ResourceMgr.getString("ErrConnectionBusy"));
      return;
    }

			this.executionThread = new WbThread(getThreadId())
    {
      @Override
      public void run()
      {
        runStatement(sql, offset, highlightError, appendResult, runType);
      }
    };

    this.executionThread.start();
	}

	private String getThreadId()
	{
		String id = "SQL Thread " + getRealTabTitle();
		if (this.dbConnection != null)
		{
			id += " (" + dbConnection.getId() + ")";
		}
		return id;
	}

  private void doAutoSaveFile()
  {
    AutoFileSaveType saveType = Settings.getInstance().getAutoSaveExternalFiles();
    if (saveType != AutoFileSaveType.never && editor.hasFileLoaded() && editor.isModified())
    {
      if (saveType == AutoFileSaveType.always)
      {
        editor.saveCurrentFile();
      }
      else
      {
        editor.checkAndSaveFile(false);
      }
    }
  }

	/**
	 * Execute the given SQL string. This is invoked from the the run() and other
	 * methods in order to execute the SQL command. It takes care of updating the
	 * actions and the menu.
	 * The actual execution and display of the result is handled by displayResult()
	 *
	 * This is only public to allow a direct call during
	 * GUI testing (to avoid multi-threading)
	 */
	protected void runStatement(String sql, int selectionOffset, boolean highlightOnError, boolean appendResult, RunType runType)
	{
		this.setStatusMessage(ResourceMgr.getString("MsgExecutingSql"));

		this.storeStatementInHistory();
		cancelExecution = false;

		setBusy(true);

		// the dbStart should be fired *after* updating the
		// history, as the history might be saved ("AutoSaveHistory") if the MainWindow
		// receives the execStart event
		fireDbExecStart();

    doAutoSaveFile();

		setCancelState(true);

		try
		{
			this.displayResult(sql, selectionOffset, highlightOnError, appendResult, runType);
		}
		finally
		{
			fireDbExecEnd();
			clearStatusMessage();
			setCancelState(false);
			updateResultInfos();
			iconHandler.showBusyIcon(false);
			// setBusy(false) should be called after dbExecEnd()
			// otherwise the panel would indicate it's not busy, but
			// the connection would still be marked as busy
			this.setBusy(false);
			this.selectEditorLater();
			this.executionThread = null;
			this.cancelExecution = false;
		}
	}

  public AutomaticRefreshMgr getRefreshMgr()
  {
    return refreshMgr;
  }

	/**
	 * Re-run the SQL of the current result in the background.
	 */
	public void reloadCurrent()
	{
		if (isConnectionBusy()) return;
		if (currentData == null) return;

    startReloadPanel(currentData);
  }

  @Override
  public void startReloadPanel(final DwPanel panel)
  {
    if (panel == null) return;

    int index = resultTab.indexOfComponent(panel);
    if (!confirmDiscardChanges(index, true)) return;

		this.executionThread = new WbThread(getThreadId())
		{
			@Override
			public void run()
			{
				runCurrentSql(panel);
			}
		};
		this.executionThread.start();
	}

	protected void runCurrentSql()
  {
    runCurrentSql(currentData);
  }

  public void checkAutoRefreshIndicator(DwPanel panel)
  {
    int index = resultTab.indexOfComponent(panel);
    if (index < 0) return;
    Icon tabIcon = resultTab.getIconAt(index);
    resultTab.setIconAt(index, refreshMgr.getTabIcon(tabIcon, panel));
    if (refreshMgr.isRegistered(panel))
    {
      // replace the standard tooltip with the refresh information
      int interval = refreshMgr.getRefreshPeriod(panel);
      DurationFormatter formatter = new DurationFormatter();
      String intDisplay = formatter.formatDuration(interval, DurationFormat.dynamic, false, false).trim();
      String msg = ResourceMgr.getFormattedString("MsgRefreshing", intDisplay, StringUtil.getCurrentTimestamp());
      resultTab.setToolTipTextAt(index, msg);
    }
  }

	private void runCurrentSql(DwPanel dataPanel)
	{
		if (isConnectionBusy()) return;
		if (dataPanel == null) return;

		cancelExecution = false;
		setBusy(true);

		fireDbExecStart();
		setCancelState(true);
 		setStatusMessage(ResourceMgr.getString("MsgExecutingSql"));

		try
		{
			dataPanel.runCurrentSql(true);
    	TableAnnotationProcessor processor = new TableAnnotationProcessor();
  		processor.handleAnnotations(this, dataPanel, null);
      checkAutoRefreshIndicator(dataPanel);
		}
		catch (Exception e)
		{
			this.showLogMessage(e.getMessage());
			LogMgr.logError("SqlPanel.runCurrentSql()", "Error reloading current result", e);
		}
		finally
		{
			clearStatusMessage();
			setCancelState(false);
			updateResultInfos();
			fireDbExecEnd();

			// setBusy(false) should be called after dbExecEnd()
			// otherwise the panel would indicate it's not busy, but
			// the connection would still be marked as busy
			setBusy(false);
			executionThread = null;
		}
	}

  public void showData(TableIdentifier table, List<ColumnIdentifier> toSelect)
  {
		if (isConnectionBusy()) return;
    if (table == null) return;

    String sql = null;

    if (CollectionUtil.isNonEmpty(toSelect))
    {
      TableSelectBuilder builder = new TableSelectBuilder(dbConnection, TableSelectBuilder.TABLEDATA_TEMPLATE_NAME);
      sql = builder.getSelectForColumns(table, toSelect, statusBar.getMaxRows());
    }
    else
    {
      if (DbTreeSettings.useColumnListForTableDataDisplay(dbConnection.getDbId()))
      {
        List<ColumnIdentifier> columns = dbConnection.getObjectCache().getColumns(table);
        TableSelectBuilder builder = new TableSelectBuilder(dbConnection, TableSelectBuilder.TABLEDATA_TEMPLATE_NAME);
        sql = builder.getSelectForColumns(table, columns, statusBar.getMaxRows());
      }
      else
      {
        sql = "select * from " + table.getTableExpression(dbConnection);
      }
    }
    this.startExecution(sql, 0, false, true, RunType.RunAll);
  }

	@Override
	public void executeMacroSql(final String sql, final boolean replaceText, boolean appendData)
	{
		if (isConnectionBusy()) return;
		if (StringUtil.isBlank(sql)) return;

		if (replaceText)
		{
			this.storeStatementInHistory();
			this.editor.setText(sql);
			this.macroExecution = false;
		}
		else
		{
			this.macroExecution = true;
		}
		this.startExecution(sql, 0, false, this.appendResults || appendData, RunType.RunAll);
	}

	@Override
	public void exportData()
	{
		final String sql = SqlUtil.makeCleanSql(this.editor.getSelectedStatement(),false);

		this.cancelExecution = false;

		ExportFileDialog dialog = new ExportFileDialog(SwingUtilities.getWindowAncestor(this));
		dialog.setQuerySql(sql, this.getConnection());
		dialog.setIncludeSqlInsert(true);

		boolean result = dialog.selectOutput();
		if (!result) return;

		final DataExporter exporter = new DataExporter(this.dbConnection);
		exporter.setRowMonitor(this.rowMonitor);
		WbFile f = new WbFile(dialog.getSelectedFilename());
		exporter.addQueryJob(sql, f, null);
		dialog.setExporterOptions(exporter);

		this.worker = exporter;

		String msg = ResourceMgr.getString("MsgQueryExportInit");
		msg = StringUtil.replace(msg, "%type%", exporter.getTypeDisplay());
		msg = StringUtil.replace(msg, "%sql%", StringUtil.getMaxSubstring(sql, 100));
		showLogMessage(msg);

		this.executionThread = new WbThread("ExportSQL")
		{
			@Override
			public void run()
			{
				setBusy(true);
				setCancelState(true);
				fireDbExecStart();
				statusBar.executionStart();
				long start = System.currentTimeMillis();
				try
				{
					boolean newLineAppended = false;
					StringBuilder messages = new StringBuilder();

					long rowCount = exporter.startExport();

					long execTime = (System.currentTimeMillis() - start);

					CharSequence errors = exporter.getErrors();
					if (errors.length() > 0)
					{
						messages.append('\n');
						newLineAppended = true;
						messages.append(errors);
						messages.append('\n');
					}

					CharSequence warnings = exporter.getWarnings();
					if (warnings.length() > 0)
					{
						if (!newLineAppended) messages.append('\n');
						messages.append(warnings);
						messages.append('\n');
					}

					if (exporter.isSuccess())
					{
						messages.append("\n");
						messages.append(ResourceMgr.getFormattedString("MsgSpoolOk", NumberStringCache.getNumberString(rowCount)));
						messages.append("\n");
						messages.append(ResourceMgr.getString("MsgSpoolTarget"));
						messages.append(' ');
						messages.append(exporter.getFullOutputFilename());
						messages.append("\n\n");
					}

					messages.append(ResourceMgr.getString("MsgExecTime"));
					messages.append(' ');
					messages.append(Double.toString( ((double)execTime) / 1000.0));
					messages.append("s\n");
					appendToLog(messages.toString());
					showLogPanel();
				}
				catch (Exception e)
				{
          appendToLog(ExceptionUtil.getDisplay(e));
					LogMgr.logError("SqlPanel.spoolData()", "Error exporting data", e);
				}
				finally
				{
					setBusy(false);
					fireDbExecEnd();
					statusBar.executionEnd();
					long execTime = (System.currentTimeMillis() - start);
					statusBar.setExecutionTime(execTime);
					clearStatusMessage();
					setCancelState(false);
					executionThread = null;
					worker = null;
				}
			}
		};
		this.executionThread.start();
	}

	@Override
	public void fatalError(String msg)
	{
		WbSwingUtilities.showErrorMessage(this, msg);
	}

	@Override
	public int getActionOnError(int errorRow, String errorColumn, String dataLine, String errorMessage)
	{
		if (this.importRunning)
		{
			return this.getImportErrorAction(errorRow, errorColumn, dataLine, errorMessage);
		}
		else if (this.updateRunning)
		{
			return this.getUpdateErrorAction(errorRow, errorColumn, dataLine, errorMessage);
		}
		return JobErrorHandler.JOB_ABORT;
	}

	/**
	 * 	We are implementing our own getUpdateErrorAction() (and not using the one from
	 *  DwPanel) because it's necessary to turn off the loading indicator before displaying a message box.
	 *
	 * 	DwPanel's getUpdateErrorAction is called from here after turning off the loading indicator.
	 */
	public int getUpdateErrorAction(int errorRow, String errorColumn, String dataLine, String errorMessage)
	{
		iconHandler.showBusyIcon(false);
		int choice = this.currentData.getActionOnError(errorRow, errorColumn, dataLine, errorMessage);
		iconHandler.showBusyIcon(true);
		return choice;
	}

	public int getImportErrorAction(int errorRow, String errorColumn, String dataLine, String errorMessage)
	{
		String msg = null;
		if (errorColumn != null)
		{
			msg = ResourceMgr.getString("ErrColumnImportError");
			msg = msg.replace("%row%", NumberStringCache.getNumberString(errorRow));
			msg = msg.replace("%column%", errorColumn);
			msg = msg.replace("%data%", dataLine);
		}
		else
		{
			msg = ResourceMgr.getString("ErrRowImportError");
			msg = msg.replace("%row%", NumberStringCache.getNumberString(errorRow));
			msg = msg.replace("%data%", dataLine == null ? "(null)" : dataLine.substring(0,40) + " ...");
		}

		iconHandler.showBusyIcon(false);
		int choice = WbSwingUtilities.getYesNoIgnoreAll(this, msg);
		int result = JobErrorHandler.JOB_ABORT;
		iconHandler.showBusyIcon(true);
		if (choice == JOptionPane.YES_OPTION)
		{
			result = JobErrorHandler.JOB_CONTINUE;
		}
		else if (choice == WbSwingUtilities.IGNORE_ALL)
		{
			result = JobErrorHandler.JOB_IGNORE_ALL;
		}
		return result;
	}

	public void importFile()
	{
		if (this.currentData == null) return;
		if (!this.currentData.startEdit()) return;
		ImportFileDialog dialog = new ImportFileDialog(this);
		dialog.allowImportModeSelection(false);
		boolean ok = dialog.selectInput(ResourceMgr.getString("TxtWindowTitleSelectImportFile"), "general");
		if (!ok) return;
		DataStoreImporter importer = new DataStoreImporter(currentData.getTable().getDataStore(), currentData.getRowMonitor(), this);
		File importFile = dialog.getSelectedFile();
		importer.setImportOptions(importFile,
			                        dialog.getImportType(),
			                        dialog.getGeneralOptions(),
			                        dialog.getTextOptions());

		Settings.getInstance().setLastImportDir(importFile.getParent());
		dialog.saveSettings();
		runImporter(importer);
	}

	public void importString(String content, boolean showOptions)
	{
		if (this.currentData == null) return;
		if (!this.currentData.startEdit()) return;

		DataStore ds = currentData.getTable().getDataStore();

		ImportStringVerifier v = new ImportStringVerifier(content, ds.getResultInfo());
		DataStoreImporter importer = new DataStoreImporter(ds, currentData.getRowMonitor(), this);
		boolean dataOK = v.checkData();
		if (showOptions || !dataOK)
		{
			boolean checked = false;
			while (!checked)
			{
				boolean ok = v.showOptionsDialog();
				if (!ok) return; // user cancelled dialog
				checked = v.checkData();
			}
			TextImportOptions textOptions = v.getTextImportOptions();
			ImportOptions options = v.getImportOptions();
			importer.importString(content, options, textOptions);
		}
		else
		{
			if (!v.columnNamesMatched())
			{
				// assume the clipboard does not contain a header
				TextImportOptions textOptions = new DefaultTextImportOptions("\t", "\"");
				textOptions.setContainsHeader(false);
				ImportOptions options = new DefaultImportOptions();
				importer.importString(content, options, textOptions);
			}
			else
			{
				importer.importString(content);
			}
		}
		if (!this.currentData.startEdit()) return;

		runImporter(importer);
	}

	public synchronized void runImporter(final DataStoreImporter importer)
	{
		this.setActionState(this.importFileAction, false);
		this.setBusy(true);
		this.setCancelState(true);
		this.worker = importer;
		WbThread importThread = new WbThread("DataImport")
		{
			@Override
			public void run()
			{
				try
				{
					importRunning = true;
					fireDbExecStart();
					importer.startImport();
				}
				catch (Throwable e)
				{
					LogMgr.logError("SqlPanel.importData() - worker thread", "Error when importing data", e);
				}
				finally
				{
					importRunning = false;
					setBusy(false);
					fireDbExecEnd();
					currentData.getTable().getDataStoreTableModel().fileImported();
					currentData.rowCountChanged();
					currentData.clearStatusMessage();
					MessageBuffer buff = importer.getMessage();
					if (buff != null && buff.getLength() > 0)
					{
						appendToLog(buff.getBuffer().toString());
					}
					setCancelState(false);
					checkResultSetActions();
				}
			}
		};
		importThread.start();
		this.selectEditor();
	}

	@Override
	public void printMessage(String trace)
	{
		appendMessage(trace,"\n");
	}

	@Override
	public void appendToLog(final String logMessage)
	{
		if (logMessage == null) return;
		appendMessage(logMessage);
	}

	private void appendMessage(final String logMessage, final String ... moreMessages)
	{
		WbSwingUtilities.invoke(() ->
    {
      log.append(logMessage);
      if (moreMessages != null)
      {
        for (String msg : moreMessages)
        {
          log.append(msg);
        }
      }
      log.setCaretPosition(log.getDocument().getLength());
    });
	}

	@Override
	public String getInput(String prompt)
	{
		String pwd = WbSwingUtilities.getUserInput(this, prompt, "");
		if (StringUtil.isEmptyString(pwd)) return null;
		return pwd;
	}

	@Override
	public String getPassword(String prompt)
	{
		String pwd = WbSwingUtilities.getUserInputHidden(this, ResourceMgr.getString("MsgInputPwdWindowTitle"), "");
		if (StringUtil.isEmptyString(pwd)) return null;
		return pwd;
	}

	@Override
	public boolean confirmExecution(String prompt, String yes, String no)
	{
		String title = "";
		Window w = SwingUtilities.getWindowAncestor(this);

		if (dbConnection != null)
		{
			WindowTitleBuilder builder = new WindowTitleBuilder();
			title = builder.getWindowTitle(dbConnection.getProfile(), null, null, null) + " - ";
		}
		title += getRealTabTitle();

		int result = WbSwingUtilities.getYesNo(w, title, prompt, yes, no);
		return result == JOptionPane.YES_OPTION;
	}

	/** Used for storing the result of the confirmExecution() callback */
	private boolean executeAllStatements = true;
	private boolean cancelAll = false;

	@Override
	public boolean confirmStatementExecution(String command)
	{
		if (executeAllStatements) return true;
		boolean result = false;
		iconHandler.showBusyIcon(false);
		try
		{
			String msg = ResourceMgr.getString("MsgConfirmExecution") + "\n" + StringUtil.getMaxSubstring(command, 60);
			int choice = WbSwingUtilities.getYesNoExecuteAll(this, msg);
			switch (choice)
			{
				case JOptionPane.YES_OPTION:
					result = true;
					break;
				case JOptionPane.NO_OPTION:
					result = false;
					break;
				case JOptionPane.CANCEL_OPTION:
					result = false;
					this.executeAllStatements = false;
					this.cancelAll = true;
					break;
				case WbSwingUtilities.EXECUTE_ALL:
					result = true;
					this.executeAllStatements = true;
					break;
				default:
					result = false;
			}
		}
		finally
		{
			iconHandler.showBusyIcon(true);
		}
		return result;
	}

	/**
	 * This is called when the user switches between the result tabs (if multiple
	 * results are available)
	 *
	 * If this happens, we need to update all actions in the menu that operate on the
	 * result to use the new datastore.
	 *
	 * @param evt
	 * @see #updateResultInfos()
	 */
	@Override
	public void stateChanged(ChangeEvent evt)
	{
		if (ignoreStateChange || isBusy()) return;

		updateResultInfos();
		if (currentData != null)
		{
			statusBar.setExecutionTime(currentData.getLastExecutionTime());
		}
		else
		{
			statusBar.setExecutionTime(getTotalResultExecutionTime());
		}
	}

	private long getTotalResultExecutionTime()
	{
		if (lastScriptExecTime > 0) return lastScriptExecTime;

		long time = 0;
		for (int i=0; i < resultTab.getTabCount() - 1; i++)
		{
			DwPanel data = (DwPanel)this.resultTab.getComponentAt(i);
			time += data.getLastExecutionTime();
		}
		return time;
	}

	private void updateResultInfos()
	{
		int newIndex = this.resultTab.getSelectedIndex();
		if (newIndex == -1)
		{
			return;
		}

		if (currentData != null)
		{
			this.currentData.removePropertyChangeListener(this);
			this.currentData.getTable().stopEditing();
		}

		if (newIndex == this.resultTab.getTabCount() - 1)
		{
			currentData = null;
			// this means the message panel is displayed, so remove the rowcount display from the status bar
			statusBar.clearRowcount();
		}
		else
		{
			currentData = getCurrentResult();
			if (currentData != null)
			{
				currentData.updateStatusBar();
				currentData.addPropertyChangeListener("updateTable", this);
			}
		}
		closeResultAction.setEnabled(currentData != null);
		closeAllResultsAction.setEnabled(this.getResultTabCount() > 0);
		updateProxiedActions();
		checkResultSetActions();
	}

	public String getCurrentResultTitle()
	{
		int index = resultTab.getSelectedIndex();
		return resultTab.getTitleAt(index);
	}

	public DwPanel getCurrentResult()
	{
		Component c = resultTab.getSelectedComponent();
		if (c instanceof DwPanel)
		{
			return (DwPanel)c;
		}
		return null;
	}

	public long getLoadedAt()
	{
		if (currentData == null) return 0;
		DataStore ds = currentData.getDataStore();
		if (ds == null) return 0;
		return ds.getLoadedAt();
	}

	public String getSourceQuery()
	{
		if (currentData == null) return null;
		DataStore ds = currentData.getDataStore();
		if (ds != null) return ds.getGeneratingSql();
		return null;
	}

	/**
	 * Closes the currently selected result tab.
	 *
	 * @see #closeResult(int)
	 */
	public void closeCurrentResult()
	{
		int index = resultTab.getSelectedIndex();
		closeResult(index);
	}

	public void removeCurrentResult()
	{
		int index = resultTab.getSelectedIndex();
		if (!confirmDiscardChanges(index, true)) return;
		discardResult(index, false);
	}

	/**
	 * Closes the result tab with the given index.
	 *
	 * If confirmation for discarding changes is enabled, the user will be asked to proceed in case the data has been edited.
	 *
	 * @see #closeCurrentResult()
	 */
	public void closeResult(int index)
	{
		if (!confirmDiscardChanges(index, true)) return;
		discardResult(index, true);
	}

	/**
	 * Closes the results identified by the filter.
	 *
	 * @param filter the filter to use.
	 */
	public void closeSelectedResults(final ResultCloseFilter filter)
	{
		try
		{
			ignoreStateChange = true;
			WbSwingUtilities.invoke(() ->
      {
        int index = 0;
        while (index < resultTab.getTabCount() - 1)
        {
          Component c = resultTab.getComponentAt(index);
          DwPanel panel = (DwPanel)c;
          if (filter.shouldClose(panel, index) && confirmDiscardChanges(index, true))
          {
            panel.removePropertyChangeListener(SqlPanel.this);
            panel.dispose();
            resultTab.removeTabAt(index);
          }
          else
          {
            index ++;
          }
        }
        resultTab.setSelectedIndex(0);
        currentData = getCurrentResult();
        updateProxiedActions();
        updateResultInfos();
      });
		}
		finally
		{
			ignoreStateChange = false;
		}
	}

	/**
	 * Closes the result tab with the given index without further questions
	 *
	 */
	private void discardResult(int index, boolean disposeData)
	{
		if (index == resultTab.getTabCount() - 1) return;

		try
		{
			DwPanel panel = (DwPanel)resultTab.getComponentAt(index);
      refreshMgr.removeRefresh(panel);
			panel.removePropertyChangeListener(SqlPanel.this);
			if (disposeData)
			{
				panel.dispose();
			}

			resultTab.removeTabAt(index);
			currentData = null;

			int newIndex = resultTab.getSelectedIndex();
			if (newIndex > 0 && newIndex == resultTab.getTabCount() - 1)
			{
				newIndex --;
				resultTab.setSelectedIndex(newIndex);
			}
			// if the index stayed the same (e.g. because the first tab is still selected)
			// no stateChange has been fired and we need to "fake" that because
			// several actions need to be informed that a different ResultTab is
			// now active
			if (index == resultTab.getSelectedIndex())
			{
				EventQueue.invokeLater(resultTab::fireStateChanged);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("SqlPanel.closeCurrentResult()", "Error closing current result tab", e);
		}
	}

	public void closeAllResults()
	{
		for (int i=0; i < resultTab.getTabCount() - 1; i ++)
		{
      if (!isResultLocked(i))
      {
        if (!confirmDiscardChanges(i, true)) return;
      }
		}
		clearResultTabs(true);
	}

	private int findResultToSelect()
	{
    int firstReused = -1;
    int lastLocked = -1;

		for (int i=0; i < resultTab.getTabCount() - 1; i ++)
		{
			Component c = resultTab.getComponentAt(i);
			if (c instanceof DwPanel)
			{
				DwPanel panel = (DwPanel)c;
				if (panel.wasReUsed() && firstReused == -1)
        {
          firstReused = i;
        }
        if (panel.isLocked())
        {
          lastLocked = i + 1;
        }
			}
		}
    return Math.max(firstReused, lastLocked);
	}

	private void resetReuse()
	{
		for (int i=0; i < resultTab.getTabCount() - 1; i ++)
		{
			Component c = resultTab.getComponentAt(i);
			if (c instanceof DwPanel)
			{
				DwPanel panel = (DwPanel)c;
				panel.setReUsed(false);
			}
		}
	}

	/**
	 * Close all result tabs without asking
	 */
	public void clearResultTabs(boolean keepLocked)
	{
		try
		{
			ignoreStateChange = true;
			WbSwingUtilities.invoke(() ->
      {
        for (int index=resultTab.getTabCount() - 2; index >= 0; index--)
        {
          Component c = resultTab.getComponentAt(index);
          if (c instanceof DwPanel)
          {
            DwPanel panel = (DwPanel)c;
            if (keepLocked && panel.isLocked()) continue;
            refreshMgr.removeRefresh(panel);
            panel.removePropertyChangeListener(SqlPanel.this);
            panel.dispose();
          }
          resultTab.removeTabAt(index);
        }
        resultTab.setSelectedIndex(0);
        boolean wasNull = currentData == null;
        currentData = null;
        if (!wasNull)
        {
          updateProxiedActions();
        }
        checkResultSetActions();
      });
		}
		finally
		{
			ignoreStateChange = false;
		}
	}

	/**
	 * Update those actions that depend on the current data result.
	 */
	protected void updateProxiedActions()
	{
		WbSwingUtilities.invoke(this::_updateProxiedActions);
	}

	private void _updateProxiedActions()
	{
		if (currentData == null)
		{
			updateAction.setOriginal(null);
			insertRow.setOriginal(null);
			deleteRow.setOriginal(null);
			deleteDependentRow.setOriginal(null);
			duplicateRow.setOriginal(null);
			selectKeys.setOriginal(null);
			createDeleteScript.setClient(null);
			exportDataAction.setOriginal(null);
			optimizeAllCol.setClient(null);
			optimizeRowHeights.setClient(null);
			dataToClipboard.setOriginal(null);
			copyAsSqlInsert.setOriginal(null);
			copyAsSqlUpdate.setOriginal(null);
			copyAsSqlDeleteInsert.setOriginal(null);
			copyAsSqlDelete.setOriginal(null);
			copyAsSqlMerge.setOriginal(null);
			findDataAction.setOriginal(null);
			findDataAgainAction.setOriginal(null);
			copyAsSQLMenu.setEnabled(false);
			copySelectedMenu.removeAll();
			copySelectedMenu.setEnabled(false);
			printDataAction.setOriginal(null);
			printPreviewAction.setOriginal(null);
			filterAction.setOriginal(null);
			filterPicker.setClient(null);
			resetFilterAction.setOriginal(null);
			selectionFilterAction.setClient(null);
			resetHighlightAction.setOriginal(null);
			showFormAction.setTable(null);
		}
		else
		{
			updateAction.setOriginal(currentData.getUpdateDatabaseAction());
			insertRow.setOriginal(currentData.getInsertRowAction());
			deleteRow.setOriginal(currentData.getDeleteRowAction());
			deleteDependentRow.setOriginal(currentData.getDeleteDependentRowsAction());
			duplicateRow.setOriginal(currentData.getCopyRowAction());
			selectKeys.setOriginal(currentData.getSelectKeysAction());
			createDeleteScript.setClient(currentData.getTable());
			exportDataAction.setOriginal(currentData.getTable().getExportAction());
			optimizeAllCol.setClient(currentData.getTable());
			optimizeRowHeights.setClient(currentData.getTable());
			dataToClipboard.setOriginal(currentData.getTable().getDataToClipboardAction());
			copyAsSqlInsert.setOriginal(currentData.getTable().getCopyAsInsertAction());
			copyAsSqlUpdate.setOriginal(currentData.getTable().getCopyAsUpdateAction());
			copyAsSqlDeleteInsert.setOriginal(currentData.getTable().getCopyAsDeleteInsertAction());
			copyAsSqlDelete.setOriginal(currentData.getTable().getCopyAsDeleteAction());
			copyAsSqlMerge.setOriginal(currentData.getTable().getCopyAsSqlMergeAction());
			findDataAction.setOriginal(currentData.getTable().getReplacer().getFindAction());
			findDataAgainAction.setOriginal(currentData.getTable().getReplacer().getFindAgainAction());
			replaceDataAction.setOriginal(currentData.getTable().getReplacer().getReplaceAction());
			copyAsSQLMenu.setEnabled(true);
			copySelectedMenu.removeAll();
			currentData.getTable().populateCopySelectedMenu(copySelectedMenu);
			copySelectedMenu.setEnabled(true);
			printDataAction.setOriginal(currentData.getTable().getPrintAction());
			printPreviewAction.setOriginal(currentData.getTable().getPrintPreviewAction());
			filterAction.setOriginal(currentData.getTable().getFilterAction());
			filterPicker.setClient(currentData.getTable());
			resetFilterAction.setOriginal(currentData.getTable().getResetFilterAction());
			selectionFilterAction.setClient(currentData.getTable());
			resetHighlightAction.setOriginal(currentData.getTable().getResetHighlightAction());
			showFormAction.setTable(currentData.getTable());
		}
	}

	private boolean checkPrepared;

	@Override
	public boolean processParameterPrompts(String sql)
	{
		boolean goOn = true;

		VariablePool varPool = VariablePool.getInstance();

		DataStore ds = varPool.getParametersToBePrompted(sql);

		if (ds != null && ds.getRowCount() > 0)
		{
			iconHandler.showBusyIcon(false);
			goOn = VariablesEditor.showVariablesDialog(ds);
			iconHandler.showBusyIcon(true);
		}

		if (goOn && this.checkPrepared)
		{
			PreparedStatementPool pool = this.dbConnection.getPreparedStatementPool();
			try
			{
				if (pool.isRegistered(sql) || pool.addPreparedStatement(sql))
				{
					StatementParameters parms = pool.getParameters(sql);
					iconHandler.showBusyIcon(false);
					goOn = ParameterEditor.showParameterDialog(parms, false);
					iconHandler.showBusyIcon(true);
				}
			}
			catch (SQLException e)
			{
				iconHandler.showBusyIcon(false);
				String msg = ResourceMgr.getString("ErrCheckPreparedStatement");
				msg = StringUtil.replace(msg, "%error%", ExceptionUtil.getDisplay(e));
				WbSwingUtilities.showErrorMessage(this, msg);
				iconHandler.showBusyIcon(true);

				// Ignore errors in prepared statements...
				goOn = true;

				// Disable checking as the current driver does not seem to support it
				Settings.getInstance().setCheckPreparedStatements(false);
			}
		}
		return goOn;
	}

  /**
   * Display the result of running a statement.
   *
   * The SQL is being passed to enable running statements other than the ones in the editor.
   *
   * For run types RunCurrent, RunFromCursor and RunToCursor the current cursor location of the editor is used.
   * So passing a SQL statement that is not in the editor together anything other RunType.RunAll is not supported
   *
   * @param script           the SQL to run
   * @param selectionOffset  the caret offset inside the script
   * @param highlightOnError if true, errors are highlighted (rather than just jumpeed to)
   * @param appendResult     if true and result is appended to the result tab
   * @param runType          how to run the SQL.
   */
	private void displayResult(String script, int selectionOffset, boolean highlightOnError, boolean appendResult, RunType runType)
	{
		if (script == null) return;

		boolean logWasCompressed = false;
    boolean jumpToNext = (runType == RunType.RunCurrent && Settings.getInstance().getAutoJumpNextStatement());
		boolean highlightCurrent = false;
		boolean restoreSelection = false;
		boolean shouldRestoreSelection = Settings.getInstance().getBoolProperty("workbench.gui.sql.restoreselection", true);
		boolean macroRun = false;
		String nl = Settings.getInstance().getInternalEditorLineEnding();
		Pattern fixNLPattern = null;
		// do we need to convert the \n used in the editor
		// before sending the SQL to the DBMS?
		if (!nl.equals("\n"))
		{
			fixNLPattern = Pattern.compile("\n");
		}

		checkPrepared = Settings.getInstance().getCheckPreparedStatements();
		executeAllStatements = false;
		cancelAll = false;

		ScriptParser scriptParser = ScriptParser.createScriptParser(dbConnection);

		int oldSelectionStart = -1;
		int oldSelectionEnd = -1;

		stmtRunner.setExecutionController(this);
		stmtRunner.setParameterPrompter(this);

		DurationFormatter df = new DurationFormatter();

		// If a file is loaded in the editor, make sure the StatementRunner
		// is using the file's directory as the base directory
		// Thanks to Christian d'Heureuse for this fix!
		if (editor.hasFileLoaded())
		{
			try
			{
				File f = new File(editor.getCurrentFileName());
				String dir = f.getCanonicalFile().getParent();
				this.stmtRunner.setBaseDir(dir);
			}
			catch (IOException e)
			{
				this.stmtRunner.setBaseDir(System.getProperty("user.dir"));
			}
		}
		else
		{
			this.stmtRunner.setBaseDir(System.getProperty("user.dir"));
		}

		int maxRows = this.statusBar.getMaxRows();
		int timeout = this.statusBar.getQueryTimeout();
		int firstResultIndex = 0;
		final long scriptStart = System.currentTimeMillis();

		StatementRunnerResult statementResult = null;

		int currentCursor = this.editor.getCaretPosition();
		int currentResultCount = this.resultTab.getTabCount() - 1;

    String macroMsg = null;
		if (this.macroExecution)
		{
			// executeMacro() will set "macroExecution" so that we can
			// log the macro statement here. Otherwise we wouldn't know at this point
			// that a macro is beeing executed
      // we don't need to log this here, as this can only occur when the user
      // execute the macro through the menu
			macroRun = true;
		}
		else
		{
			String macroKey = SqlUtil.trimSemicolon(script.trim());
			String macroText = MacroManager.getInstance().getMacroText(macroClientId, macroKey);
			if (macroText != null)
			{
        // log the fact that the current SQL text was taken as a macro statement
        // we can't append the message here, as the log output might be cleared later
				macroMsg = ResourceMgr.getString("MsgExecutingMacro") + ": " + macroKey + "\n";
				script = macroText;
				macroRun = true;
			}
		}

		try
		{
			scriptParser.setScript(script);

			int commandWithError = -1;
			int startIndex = 0;
			int count = scriptParser.getSize();
			int endIndex = count;
			int failuresIgnored = 0;

      boolean showScriptProgress = count > 1 && GuiSettings.showScriptProgress();

			if (count == 0)
			{
				this.appendToLog(ResourceMgr.getString("ErrNoCommand"));
				this.showLogPanel();
				return;
			}

      int cursorPos = editor.getCaretPosition();

      if (runType != RunType.RunAll)
			{
				// cursorPos > -1 means that the statement at (or from) the cursor position should be executed
				int realPos = cursorPos;
				if (GuiSettings.getUseStatementInCurrentLine())
				{
					realPos = editor.getLineStartOffset(editor.getCaretLine());
				}


        if (runType == RunType.RunFromCursor)
        {
          startIndex = scriptParser.getCommandIndexAtCursorPos(realPos);
          count = count - startIndex;
        }
        else if (runType == RunType.RunToCursor)
        {
          startIndex = 0;
          endIndex = scriptParser.getCommandIndexAtCursorPos(realPos) + 1;
          count = endIndex;
        }
        else if (runType == RunType.RunCurrent)
        {
          startIndex = scriptParser.getCommandIndexAtCursorPos(realPos);
          count = 1;
          endIndex = startIndex + 1;
        }

				if (startIndex == -1)
				{
					// no statement found at the cursor position
					// this usually means, the cursor is behind the last statement
					int numStatements = scriptParser.getSize();
					int endOfLastStatement = scriptParser.getEndPosForCommand(numStatements - 1);

					if (GuiSettings.getUseLastIfNoCurrentStmt() && cursorPos >= endOfLastStatement)
					{
						startIndex = numStatements - 1;
						endIndex = numStatements;
						LogMgr.logWarning("SqlPanel.displayResult()", "The cursor is not located inside a statement. Using the last statement of the editor instead!");
					}
					else
					{
						this.appendToLog(ResourceMgr.getString("ErrNoCurrentStatement"));
						this.showLogPanel();
						return;
					}
				}
			}

			if (endIndex == startIndex + 1)
			{
				AppendResultAnnotation append = new AppendResultAnnotation();
				String sql = scriptParser.getCommand(startIndex);
				if (append.containsAnnotation(sql))
				{
					appendResult = true;
				}
			}

			if (appendResult)
			{
				resetReuse();
				firstResultIndex = this.resultTab.getTabCount() - 1;
				appendToLog("\n");
			}
			else
			{
				setLogText("");
				clearResultTabs(true);
				firstResultIndex = 0;
			}

      if (macroMsg != null)
      {
        appendToLog(macroMsg);
      }

			if (count > 1)
			{
				logWasCompressed = !this.stmtRunner.getVerboseLogging();
			}

			String finishedMsg1 = ResourceMgr.getString("TxtScriptStatementFinished1") + " ";
			String finishedMsg2 = " " + ResourceMgr.getFormattedString("TxtScriptStatementFinished2", NumberStringCache.getNumberString(count));

			boolean onErrorAsk = !Settings.getInstance().getIgnoreErrors();

			highlightCurrent = ((count > 1 || cursorPos > -1) && (!macroRun) && Settings.getInstance().getHighlightCurrentStatement());

			if (highlightCurrent)
			{
				oldSelectionStart = this.editor.getSelectionStart();
				oldSelectionEnd = this.editor.getSelectionEnd();
				restoreSelection = shouldRestoreSelection;
			}

			statusBar.executionStart();
			long stmtTotal = 0;
			int executedCount = 0;
			int resultSets = 0;

			macroExecution = false;

			long totalRows = 0;
			lastScriptExecTime = 0;
			stmtRunner.setMaxRows(maxRows);

			ignoreStateChange = true;
			ErrorDescriptor errorDetails = null;
      boolean ignoreUpdateCounts = true;

      if (LogMgr.isTraceEnabled())
      {
        LogMgr.logTrace("SqlPanel.displayResults()",
          runType.toString() + ": " + count + " of " + scriptParser.getSize() + " statement(s): start=" + startIndex + ", end=" + (endIndex - 1) +
          " Cursor: line=" + (editor.getCaretLine() + 1) + ", column=" + (editor.getCaretPositionInLine(editor.getCaretLine()) + 1) + " (" + cursorPos + ")");
      }

			for (int i=startIndex; i < endIndex; i++)
			{
				String currentSql = scriptParser.getCommand(i);
        if (StringUtil.isEmptyString(currentSql)) continue;

        if (LogMgr.isTraceEnabled())
        {
          LogMgr.logTrace("SqlPanel.displayResults()", "Statement " + i + ": " + SqlUtil.makeCleanSql(StringUtil.getMaxSubstring(currentSql, 150), false));
        }

				historyStatements.add(currentSql);

				if (fixNLPattern != null)
				{
					currentSql = fixNLPattern.matcher(currentSql).replaceAll(nl);
				}

				String macro = MacroManager.getInstance().getMacroText(macroClientId, currentSql);
				if (macro != null)
				{
					appendToLog(ResourceMgr.getString("MsgExecutingMacro") + ": " + currentSql + "\n");
					macroRun = true;
					currentSql = macro;
				}

				// By calling yield() we make sure that
				// this thread can actually be interrupted!
				Thread.yield();
				if (cancelExecution) break;

				if (highlightCurrent && !editor.isModifiedAfter(scriptStart))
				{
					highlighter.highlightStatement(scriptParser, i, selectionOffset);
				}

				stmtRunner.setQueryTimeout(timeout);
				stmtRunner.runStatement(currentSql);
				statementResult = this.stmtRunner.getResult();

				if (statementResult == null) continue;
        ignoreUpdateCounts = ignoreUpdateCounts && statementResult.isIgnoreUpdateCount();

				if (statementResult.stopScript())
				{
					String cancelMsg = ResourceMgr.getString("MsgScriptCancelled");
					this.appendToLog(cancelMsg);
					this.appendToLog("\n");
					this.showLogPanel();
					break;
				}

				if (statementResult.promptingWasCancelled())
				{
					this.printMessage(ResourceMgr.getFormattedString("MsgSqlCancelledDuringPrompt", NumberStringCache.getNumberString(i+1)));
					this.showLogPanel();
					if (GuiSettings.cancellingVariablePromptStopsExecution())
					{
						break;
					}
					else
					{
						continue;
					}
				}

				resultSets += this.addResult(statementResult);
				stmtTotal += statementResult.getExecutionDuration();

				// the WbFeedback command might change the feedback level
				// so it needs to be checked each time.
				if (count > 1) logWasCompressed = logWasCompressed || !this.stmtRunner.getVerboseLogging();

        // Concatenating Strings is faster than using String.format() (or ResourceMgr.getFormattedString()) for each statement
				final String currentMsg = finishedMsg1 + NumberStringCache.getNumberString( (i + 1) - startIndex) + finishedMsg2;

        if (!logWasCompressed)
        {
          showResultMessage(statementResult);
          StringBuilder logmsg = new StringBuilder(50);

          if (showScriptProgress)
          {
            String timing = statementResult.getTimingMessage();
            if (timing != null)
            {
              logmsg.append('\n');
              logmsg.append(timing);
            }
            logmsg.append('\n');
            logmsg.append(currentMsg);
          }

          if (count > 1 && GuiSettings.showScriptStmtFinishTime())
          {
            logmsg.append(" (" + StringUtil.getCurrentTimestamp() + ")");
          }

          if (showScriptProgress)
          {
            logmsg.append('\n');
          }

          if (logmsg.length() > 0)
          {
            appendToLog(logmsg.toString());
            appendToLog("\n");
          }
        }
        else if (statementResult.hasWarning())
        {
          // Warnings should always be shown, even if the log output is "compressed"
          String verb = stmtRunner.getConnection().getParsingUtil().getSqlVerb(currentSql);
          String warn = StringUtil.replace(ResourceMgr.getString("MsgStmtCompletedWarn"), "%verb%", verb);
          this.appendToLog(warn + "\n");
        }

        if (count > 1)
        {
          this.statusBar.setStatusMessage(currentMsg);
        }

				this.stmtRunner.statementDone();

				// this will be set by confirmExecution() if "Cancel" was selected
        if (cancelAll) break;

        if (statementResult.isSuccess())
        {
          totalRows += statementResult.getTotalUpdateCount();
        }
				else
				{
					commandWithError = i;
					errorDetails = statementResult.getErrorDescriptor();

          // make sure the error messages are visible
          showLogPanel();

					// error messages should always be shown in the log panel, even if compressLog is enabled
          // if it is not enabled the messages have been appended to the log already
					if (logWasCompressed)
          {
            appendToLog(statementResult.getMessages().toString());
            appendToLog("\n");
          }

          // When cancelling a statement some JDBC drivers throw an exception as well.
          // In that case we also don't want to display the error dialog
					if (onErrorAsk && shouldAsk(i - startIndex, count) && !cancelExecution)
					{
            // we can't highlight the statement if this is a macro or if the user changed the editor content
            if (!macroRun && !editor.isModifiedAfter(scriptStart))
            {
              highlighter.markError(highlightOnError, scriptParser, commandWithError, selectionOffset, null);
            }

            int choice = handleScriptError(i, count, errorDetails, scriptParser, selectionOffset);

    				if (choice == JOptionPane.CANCEL_OPTION)
						{
              statementResult.setStopScript(true);
							break;
						}
						else if (choice == WbSwingUtilities.IGNORE_ALL)
						{
							onErrorAsk = false;
						}
					}
					failuresIgnored ++;
				}

				executedCount ++;

				if (this.cancelExecution) break;

        // this is for an automatic execution of the SELECT statement when doing an "Execute current" with a WbExport
				if (cursorPos > -1 && shouldRunNextStatement(scriptParser, startIndex))
				{
					endIndex ++;
				}
			} // end for loop over all statements

			lastScriptExecTime = stmtTotal;

			// this will also automatically stop the execution timer in the status bar
			statusBar.setExecutionTime(stmtTotal);
			statusBar.clearStatusMessage();

			boolean editorWasModified = editor.isModifiedAfter(scriptStart);
			highlightCurrent = highlightCurrent && !editorWasModified;
			highlightOnError = highlightOnError && !editorWasModified;

			if (commandWithError > -1 && !macroRun)
			{
				restoreSelection = false;
				highlighter.markError(highlightOnError, scriptParser, commandWithError, selectionOffset, errorDetails);
			}

			if (failuresIgnored > 0)
			{
				this.appendToLog(ResourceMgr.getFormattedString("MsgTotalStatementsFailed", failuresIgnored)+ "\n");
			}

			if (logWasCompressed)
			{
				this.appendToLog(ResourceMgr.getFormattedString("MsgTotalStatementsExecuted", executedCount) + "\n");
        // ignoreUpdateCounts will only be true if ALL statement results signaled that the update counts
        // should be ignored
        if (!ignoreUpdateCounts)
        {
          this.appendToLog(ResourceMgr.getFormattedString("MsgRowsAffected", totalRows) + "\n");
        }
        this.appendToLog("\n");
			}

			ignoreStateChange = false;
      // only show the result sets if no error occurred
			if (resultSets > 0 && commandWithError == -1)
			{
				if (resultTab.getTabCount() - 1 == currentResultCount)
				{
					// this means at least one result was re-used
					int index = findResultToSelect();
					if (index > -1) this.showResultPanel(index);
				}
				else if (firstResultIndex > 0)
				{
					this.showResultPanel(firstResultIndex);
				}
			}
			else
			{
				this.showLogPanel();
			}

      String duration = df.formatDuration(lastScriptExecTime, Settings.getInstance().getDurationFormat(), (lastScriptExecTime < DurationFormatter.ONE_MINUTE));

			if (count > 1)
			{
				String finish = ResourceMgr.getString("TxtScriptFinished");
				if (GuiSettings.showScriptFinishTime())
				{
					finish += " (" + StringUtil.getCurrentTimestamp() + ")";
				}
				this.appendToLog("\n" + finish);
				this.appendToLog("\n" + ResourceMgr.getString("MsgScriptExecTime") + " " + duration);
        if (!showScriptProgress)
        {
          this.appendToLog("\n" + ResourceMgr.getFormattedString("MsgTotalStatementsExecuted", executedCount));
        }
			}
			else
			{
        if (!showScriptProgress)
        {
          this.appendToLog("\n" + ResourceMgr.getString("MsgExecTime") + " " + duration + "\n");
        }
        if (GuiSettings.showScriptFinishTime())
        {
          this.appendToLog("(" + StringUtil.getCurrentTimestamp() + ")\n");
        }
			}

			restoreSelection = restoreSelection && !GuiSettings.getKeepCurrentSqlHighlight() && !editorWasModified;
			restoreSelection(highlightCurrent, jumpToNext, restoreSelection, currentCursor, oldSelectionStart, oldSelectionEnd, commandWithError, startIndex, endIndex, scriptParser);
		}
		catch (LowMemoryException mem)
		{
			WbManager.getInstance().showLowMemoryError();
		}
		catch (Throwable e)
		{
			if (e instanceof OutOfMemoryError)
			{
				if (statementResult != null)
				{
					try { stmtRunner.statementDone(); } catch (Throwable th) {}
					statementResult.clearResultData();
				}
				WbManager.getInstance().showOutOfMemoryError();
			}
			LogMgr.logError("SqlPanel.displayResult()", "Error executing statement", e);
			if (statementResult != null)
			{
				showLogMessage(statementResult.getMessages().toString());
				statementResult.clear();
			}
      else
      {
        showLogMessage(e.getLocalizedMessage());
      }
		}
		finally
		{
			stmtRunner.done();
			ignoreStateChange = false;
		}
	}

  private boolean shouldAsk(int cmdIndex, int count)
  {
    // The retry dialog should be shown for all statements that are executed
    if (GuiSettings.getErrorPromptType() == ErrorPromptType.PromptWithRetry)
    {
      if (count > 1) return true;
      return GuiSettings.retryForSingleStatement();
    }

    // only show the "Ignore/Cancel" prompt if the failed statement is not the last one in the script
    return (cmdIndex) < (count - 1);
  }

  private int handleScriptError(int cmdIndex, int totalStatements, ErrorDescriptor errorDetails, ScriptParser parser, int selectionOffset)
  {
    // the animated gif needs to be turned off when a
    // dialog is displayed, otherwise Swing uses too much CPU
    // this might be obsolete with Java 7 but it does not do any harm either
    iconHandler.showBusyIcon(false);

    int choice = -1;
    ErrorPromptType promptType = GuiSettings.getErrorPromptType();
    try
    {
      if (promptType == ErrorPromptType.PromptWithRetry)
      {
        choice = handleRetry(cmdIndex, errorDetails, parser, selectionOffset);
      }
      else
      {
        String msg = ResourceMgr.getFormattedString("MsgScriptStatementError", cmdIndex + 1, totalStatements);
        if (promptType == ErrorPromptType.PromptWithErroressage)
        {
          msg += "\n" + ResourceMgr.getString("MsgScriptErrorLabel");
        }
        choice = askContinue(errorDetails, msg);
      }
    }
    finally
    {
      iconHandler.showBusyIcon(true);
    }
    return choice;
  }

  @Override
  public int scriptErrorPrompt(int cmdIndex, ErrorDescriptor errorDetails, ScriptParser parser, int selectionOffset)
  {
    return handleScriptError(cmdIndex, -1, errorDetails, parser, selectionOffset);
  }

  private int handleRetry(final int cmdIndex, final ErrorDescriptor errorDetails, final ScriptParser parser, int selectionOffset)
  {
    final ErrorRetryPanel retry = new ErrorRetryPanel(getConnection());
    retry.setEnableReplace(parser != null);

    WbSwingUtilities.invoke(() ->
    {
      boolean busy = getConnection().isBusy();
      try
      {
        getConnection().setBusy(false);
        if (parser != null)
        {
          retry.setStatement(parser, cmdIndex, errorDetails);
        }
        retry.showDialog(WbSwingUtilities.getWindowAncestor(SqlPanel.this));
      }
      finally
      {
        getConnection().setBusy(busy);
      }
    });

    int result = retry.getChoice();

    try
    {
      if (retry.getChoice() == WbSwingUtilities.CONTINUE_OPTION)
      {
        if (parser != null && retry.shouldReplaceOriginalStatement())
        {
          int startOfStatement = parser.getStartPosForCommand(cmdIndex) + selectionOffset;
          int endOfStatement = parser.getEndPosForCommand(cmdIndex) + selectionOffset;
          editor.replaceText(startOfStatement, endOfStatement, retry.getStatement());
        }
        result = WbSwingUtilities.IGNORE_ONE;
      }
    }
    finally
    {
      retry.dispose();
    }
    return result;
  }

  private int askContinue(final ErrorDescriptor errorDetails, final String question)
  {
    ErrorContinueDialog invoker = new ErrorContinueDialog(errorDetails, question);
    int choice = invoker.askContinue(this);
    return choice;
  }

	private boolean shouldRunNextStatement(ScriptParser parser, int statementIndex)
	{
		if (!Settings.getInstance().getAutoRunExportStatement()) return false;
		if (this.stmtRunner == null) return false;
		if (this.stmtRunner.getConsumer() == null) return false;

		if (statementIndex + 1 < parser.getSize())
		{
			String verb = stmtRunner.getConnection().getParsingUtil().getSqlVerb(parser.getCommand(statementIndex + 1));
			Collection<String> autoRunVerbs = Settings.getInstance().getAutoRunVerbs();
			if (autoRunVerbs.contains(verb))
			{
				return true;
			}
		}
		return false;
	}

	private void restoreSelection(final boolean highlightCurrent, final boolean jumpToNext, final boolean restoreSelection, final int currentCursor, final int oldSelectionStart, final int oldSelectionEnd, final int commandWithError, final int startIndex, final int endIndex, final ScriptParser scriptParser)
	{
		// changing the selection and the caret should be done on the EDT
		WbSwingUtilities.invoke(() ->
    {
      _restoreSelection(highlightCurrent, jumpToNext, restoreSelection, currentCursor, oldSelectionStart, oldSelectionEnd, commandWithError, startIndex, endIndex, scriptParser);
    });
	}

	private void _restoreSelection(boolean highlightCurrent, boolean jumpToNext, boolean restoreSelection, int currentCursor, int oldSelectionStart, int oldSelectionEnd, int commandWithError, int startIndex, int endIndex, ScriptParser scriptParser)
	{
		if (!(highlightCurrent && GuiSettings.getKeepCurrentSqlHighlight()))
		{
			if (!jumpToNext && restoreSelection && oldSelectionStart > -1 && oldSelectionEnd > -1)
			{
				final int selstart = oldSelectionStart;
				final int selend = oldSelectionEnd;
				editor.select(selstart, selend);
			}

			if (highlightCurrent && !restoreSelection && commandWithError == -1)
			{
				int startPos = scriptParser.getStartPosForCommand(endIndex - 1);
				startPos = scriptParser.findNextLineStart(startPos);
				if (startPos > -1 && startPos < this.editor.getText().length())
				{
					this.editor.setCaretPosition(startPos);
				}
			}

			if (commandWithError == -1 && jumpToNext)
			{
				int nextCommand = startIndex + 1;
				int startPos = scriptParser.getStartPosForCommand(nextCommand);
				startPos = scriptParser.findNextLineStart(startPos);
				if (startPos > -1)
				{
					this.editor.setCaretPosition(startPos);
				}
				else if (oldSelectionStart > -1)
				{
					this.editor.setCaretPosition(oldSelectionStart);
				}
			}
		}
		else if (highlightCurrent && currentCursor > -1)
		{
			editor.setCaretPosition(currentCursor);
		}
	}

	private void showResultMessage(StatementRunnerResult result)
	{
		if (!result.hasMessages()) return;
		try
		{
			if (!MemoryWatcher.isMemoryLow(true))
			{
				result.appendMessages(this);
			}
			else
			{
				LogMgr.logError("SqlPanel.showResultMessage()", "Not enough memory to show all messages!", null);
			}
      appendToLog("\n");
		}
		catch (OutOfMemoryError oome)
		{
			result.clearMessageBuffer();
			clearLog();
			System.gc(); // as we have just freed some memory the gc() does make sense here.
			WbManager.getInstance().setOutOfMemoryOcurred();
			final boolean success = result.isSuccess();
			EventQueue.invokeLater(() ->
      {
        if (success)
        {
          log.append(ResourceMgr.getString("ErrLogNoMemSuccess"));
        }
        else
        {
          log.append(ResourceMgr.getString("ErrLogNoMemError"));
        }
        log.append("\n");
        log.append(ResourceMgr.getString("ErrLogNoMemCheckLog"));
        log.append("\n");
        log.setCaretPosition(log.getDocument().getLength());
      });
		}
		catch (Throwable th)
		{
			LogMgr.logError("SqlPanel.showResultMessage()", "Could not show message!", th);
		}
	}

	private DwPanel createDwPanel(boolean enableNavigation)
		throws SQLException
	{
		DwPanel data = new DwPanel(statusBar);
		data.getTable().setTransposeRowEnabled(true);
		data.setBorder(WbSwingUtilities.EMPTY_BORDER);
		data.setConnection(dbConnection);
		data.setUpdateHandler(this);
		data.setSqlInfoEnabled(true);

		if (enableNavigation)
		{
			MainWindow w = null;
			try
			{
				w = (MainWindow)SwingUtilities.getWindowAncestor(this);
			}
			catch (Exception e)
			{
				LogMgr.logError("SqlPanel.createDwPanel()", "Could not find MainWindow!", e);
				w = null;
			}

			if (w != null)
			{
				data.initTableNavigation(w);
			}
		}
		return data;
	}

	/**
	 * Implementation of the ResultReceiver interface.
	 * The given sql will be executed and the result will always
	 * be displayed in a new result tab
	 */
	@Override
	public void showResult(final String sql, String comment, ResultReceiver.ShowType how)
	{
		if (how == ResultReceiver.ShowType.logText)
		{
			appendToLog("\n" + ResourceMgr.getString("MsgLoadRelatedData") + "\n" + sql + "\n\n");
		}
		else if (how != ResultReceiver.ShowType.showNone)
		{
			int pos = this.editor.getDocumentLength();
			if (how == ResultReceiver.ShowType.replaceText)
			{
				this.editor.setText("");
				pos = 0;
			}
			else
			{
				if (StringUtil.isNonBlank(comment))
				{
					if (pos > 1) this.editor.appendLine("\n");
					this.editor.appendLine(comment + "\n");
				}
				else
				{
					this.editor.appendLine("\n\n");
				}
				pos = this.editor.getDocumentLength();
			}
			this.editor.appendLine(sql + ";\n");
			this.editor.setCaretPosition(pos);
			this.editor.scrollToCaret();
		}
		startExecution(comment + "\n" + sql, 0, false, true, RunType.RunAll);
	}

	public void showData(DataStore ds)
		throws SQLException
	{
		String gen = ds.getGeneratingSql();
		DwPanel p = createDwPanel(false);
		p.showData(ds, gen, 0);
		final int newIndex = addResultTab(p);
		if (newIndex > 0)
		{
			WbSwingUtilities.invokeLater(() ->
      {
        resultTab.setSelectedIndex(newIndex);
      });
		}
	}

	private int getNextResultNumber()
	{
		int count = this.resultTab.getTabCount();
		if (count <= 1) return 1;

		String defaultTitle = ResourceMgr.getString("LblTabResult");

		int maxNr = 0;
		for (int i=0; i < count - 1; i++)
		{
			String title = HtmlUtil.cleanHTML(this.resultTab.getTitleAt(i));
			if (title != null && title.startsWith(defaultTitle))
			{
				int nr = StringUtil.getIntValue(title.replaceAll("[^0-9]", ""), -1);
				if (nr > maxNr)
				{
					maxNr = nr;
				}
			}
		}
		return maxNr + 1;
	}

  private int addResultTab(DwPanel data)
  {
		int newIndex = this.resultTab.getTabCount() - 1;
    WbTable tbl = data.getTable();
    DataStore ds = (tbl != null ? tbl.getDataStore() : null);
    String resultName = (ds != null ? ds.getResultName() : null);
    if (StringUtil.isBlank(resultName))
    {
      resultName = ResourceMgr.getString("LblTabResult") + " " + NumberStringCache.getNumberString(getNextResultNumber());
    }
    else
    {
      tbl.setPrintHeader(resultName);
    }
    resultTab.insertTab(resultName, null, data, null, newIndex);
    data.showGeneratingSQLAsTooltip();
    data.setName("dwresult" + NumberStringCache.getNumberString(newIndex));
    if (this.resultTab.getTabCount() == 2)
    {
      this.resultTab.setSelectedIndex(0);
    }
    data.checkLimitReachedDisplay();

    TableAnnotationProcessor processor = new TableAnnotationProcessor();
    processor.handleAnnotations(this, data, this.getRefreshMgr());
    checkAutoRefreshIndicator(data);
    return newIndex;
  }

	public void setSelectedResultTab(int index)
	{
		resultTab.setSelectedIndex(index);
	}

	/**
	 * Returns the number of results tabs including the message tab
	 * (so the return value is always >= 1)
	 */
	public int getResultTabCount()
	{
		return resultTab.getTabCount();
	}

	private DwPanel findResultPanelByName(String toFind)
	{
		int tabCount = this.resultTab.getTabCount();
		for (int i = 0; i < tabCount; i++)
		{

			String name  = resultTab.getTitleAt(i);
			if (StringUtil.equalStringIgnoreCase(name, toFind))
			{
				DwPanel p = (DwPanel)resultTab.getComponentAt(i);
				return p;
			}
		}
		return null;
	}

	/**
	 * Display the data contained in the StatementRunnerResult.
	 * For each DataStore or ResultSet in the result, an additional
	 * result {@link workbench.gui.sql.DwPanel} will be added.
	 *
	 * @param result the result to be displayed (obtained from a {@link workbench.sql.StatementRunner})
	 * @see workbench.gui.sql.DwPanel
	 */
	public int addResult(final StatementRunnerResult result)
		throws SQLException
	{
		if (result == null) return 0;
		final String sql = result.getSourceCommand();
		final long time = result.getExecutionDuration();

		int count = 0;

		if (result.hasDataStores())
		{
			final List<DataStore> results = result.getDataStores();
			count += results.size();
			final List<DwPanel> panels = new ArrayList<>(results.size());
			WbSwingUtilities.invoke(() ->
      {
        try
        {
          UseTabAnnotation useTab = new UseTabAnnotation();
          for (DataStore ds : results)
          {
            String gen = StringUtil.isNonBlank(sql) ? sql : ds.getGeneratingSql();
            String tabName1 = useTab.getResultName(sql);
            DwPanel p = null;
            if (StringUtil.isNonEmpty(tabName1))
            {
              ds.setResultName(tabName1);
              p = findResultPanelByName(tabName1);
            }
            if (p != null)
            {
              p.showData(ds, gen, time);
              panels.add(p);
              p.setReUsed(true);
              p.showGeneratingSQLAsTooltip();
            }
            else
            {
              p = createDwPanel(true);
              p.showData(ds, gen, time);
              addResultTab(p);
              panels.add(p);
            }
          }
        }
        catch (Exception e)
        {
          LogMgr.logError("SqlPanel.addResult()", "Error when adding new DwPanel with DataStore", e);
        }
      });

			// The retrieval of column comments should not be done on the AWT Thread
			if (GuiSettings.getRetrieveQueryComments())
			{
				for (DwPanel p : panels)
				{
					p.readColumnComments();
				}
			}
		}

		if (result.hasResultSets())
		{
			final List<ResultSet> results = result.getResultSets();
			count += results.size();
			WbSwingUtilities.invoke(() ->
      {
        try
        {
          for (ResultSet rs : results)
          {
            DwPanel p = createDwPanel(true);
            p.showData(rs, sql, time);
            addResultTab(p);
          }
        }
        catch (Exception e)
        {
          LogMgr.logError("SqlPanel.addResult()", "Error when adding new DwPanel with ResultSet", e);
        }
      });
		}

		return count;
	}

	@Override
	public void registerToolWindow(ToolWindow window)
	{
		synchronized (resultWindows)
		{
			resultWindows.add(window);
		}
		// We must register the tool window with the WbManager as well
		// in order to close them properly when the application is closed
		WbManager.getInstance().registerToolWindow(window);
	}

	@Override
	public void unregisterToolWindow(ToolWindow window)
	{
		if (window == null) return;
		synchronized (resultWindows)
		{
			resultWindows.remove(window);
		}
		WbManager.getInstance().unregisterToolWindow(window);
	}

	protected void checkResultSetActions()
	{
		final boolean readOnly = (dbConnection == null ? false : dbConnection.isSessionReadOnly());
		final boolean hasResult = currentData != null ? currentData.hasResultSet() : false;
		final boolean hasRows = (hasResult && currentData.getTable().getRowCount() > 0);
		final boolean mayEdit = !readOnly && hasResult && currentData.hasUpdateableColumns();
		final boolean findNext = hasResult && (currentData.getTable().canSearchAgain());
		Action[] actionList = new Action[]
						{ dataToClipboard,
							exportDataAction,
							optimizeAllCol,
							optimizeRowHeights,
							printDataAction,
							printPreviewAction
						};
		setActionState(actionList, hasResult);

		WbSwingUtilities.invoke(() ->
    {
      importFileAction.setEnabled(mayEdit);
      importClipAction.setEnabled(mayEdit);
      findDataAgainAction.setEnabled(findNext);
      copySelectedMenu.setEnabled(hasResult);
      reloadAction.checkEnabled();
      showFormAction.setEnabled(hasRows);
    });
  }

	private void setExecActionsState(final boolean flag)
	{
		EventQueue.invokeLater(() ->
    {
      executeAll.setEnabled(flag);
      executeCurrent.setEnabled(flag);
      executeSelected.setEnabled(flag);
      executeFromCurrent.setEnabled(flag);
      executeToCursor.setEnabled(flag);
    });
	}

	private void setConnActionsState(final boolean flag)
	{
		EventQueue.invokeLater(() ->
    {
      if (flag)
      {
        checkCommitAction();
      }
      else
      {
        commitAction.setEnabled(flag);
        rollbackAction.setEnabled(flag);
      }
      spoolData.canExport(flag);
      appendResultsAction.setEnabled(flag);
      joinCompletion.setEnabled(flag);
    });
	}

	/**
	 * Returns true if the editor should be disabled when running a query.
	 *
	 * @see GuiSettings#getDisableEditorDuringExecution()
	 */
	private boolean disableEditor()
	{
		return GuiSettings.getDisableEditorDuringExecution();
	}

	public void setBusy(final boolean busy)
	{
    threadBusy = busy;
    if (iconHandler != null) iconHandler.showBusyIcon(busy);
    setConnActionsState(!busy);
    setExecActionsState(!busy);
    if (disableEditor())
    {
      if (editor != null) editor.setEditable(!busy);
    }
    if (sqlHistory != null) sqlHistory.setEnabled(!busy);
	}

  public boolean isConnectionBusy()
  {
    if (isBusy()) return true;
    if (this.dbConnection == null) return false;
    return this.dbConnection.isBusy();
  }

	@Override
	public boolean isBusy()
	{
		return threadBusy;
	}

	@Override
	public void fontChanged(String aFontId, Font newFont)
	{
		if (aFontId.equals(Settings.PROPERTY_MSGLOG_FONT))
		{
			this.log.setFont(newFont);
		}
	}

	@Override
	public void addDbExecutionListener(DbExecutionListener l)
	{
		this.execListener.add(l);
	}

	@Override
	public void removeDbExecutionListener(DbExecutionListener l)
	{
		if (this.execListener == null) return;
		this.execListener.remove(l);
	}

	public void fireDbExecStart()
	{
    if (this.execListener != null)
    {
      for (DbExecutionListener l : this.execListener)
      {
        if (l != null) l.executionStart(this.dbConnection, this);
      }
    }
    if (this.dbConnection != null)
    {
      this.dbConnection.setBusy(true);
    }
	}

	public void fireDbExecEnd()
	{
    // It is important to first tell the connection that we are finished
    // otherwise the connection still thinks it's "busy" although it is not
    if (this.dbConnection != null)
    {
      this.dbConnection.setBusy(false);
    }
    if (this.execListener != null)
    {
      for (DbExecutionListener l : this.execListener)
      {
        if (l != null) l.executionEnd(this.dbConnection, this);
      }
    }
	}

	@Override
	public void reset()
	{
		locked = false;
		if (editor != null) editor.reset();
		clearLog();
		clearResultTabs(false);

    // calling RefreshMgr.clear() is not really necessary
    // because clearResultTabs() should have unregistered any
    // auto-refreshing result tab...
    refreshMgr.clear();
		if (this.currentData != null)
		{
			this.currentData.dispose();
		}
		currentData = null;
		if (iconHandler != null) iconHandler.flush();
		if (sqlHistory != null) sqlHistory.clear();
    contentPanel.setDividerLocation(0.5d);
	}

	@Override
	public void dispose()
	{
		Settings.getInstance().removePropertyChangeListener(this);
		Settings.getInstance().removeFontChangedListener(this);

    disconnect();
		reset();
		if (iconHandler != null) iconHandler.dispose();
		if (stmtRunner != null) this.stmtRunner.dispose();
    if (tabDropHandler != null) tabDropHandler.dispose();

		execListener.clear();

    if (statusBar != null)
    {
      statusBar.removeTextSelectionDisplay(editor);
    }

		if (this.editor != null) this.editor.dispose();
		this.editor = null;

		if (this.actions != null)
		{
			for (Object o : actions)
			{
				if (o instanceof WbAction)
				{
					((WbAction)o).dispose();
				}
				else if (o instanceof WbMenu)
				{
					((WbMenu)o).dispose();
				}
			}
			this.actions.clear();
		}

		if (this.filenameChangeListeners != null)
		{
			this.filenameChangeListeners.clear();
		}

		if (this.toolbar != null)
		{
			this.toolbar.removeAll();
			this.toolbar = null;
		}
		this.forceAbort();
		this.executionThread = null;
		if (this.log != null)
		{
			log.dispose();
		}
		if (this.sqlHistory != null) sqlHistory.dispose();

		copyAsSQLMenu.dispose();
		copySelectedMenu.dispose();
	}

  @Override
  public void propertyChange(PropertyChangeEvent evt)
  {
    String prop = evt.getPropertyName();
    if (prop == null) return;

    if (ToolbarBuilder.CONFIG_PROPERTY.equals(prop))
    {
      // force re-creation of the toolbar
      toolbar = null;
    }
    if (evt.getSource() == this.dbConnection && WbConnection.PROP_AUTOCOMMIT.equals(prop))
    {
      this.checkCommitAction();
    }
    else if (evt.getSource() == this.currentData && prop.equals("updateTable"))
    {
      this.checkResultSetActions();
    }
    else if (GuiSettings.PROP_SHOW_TEXT_SELECTION_INFO.equals(prop))
    {
      if (GuiSettings.getShowTextSelectionSummary())
      {
        statusBar.addTextSelectionDisplay(editor);
      }
      else
      {
        statusBar.removeTextSelectionDisplay(editor);
      }
    }
    else if (GuiSettings.PROPERTY_RESULTTAB_CLOSE_BUTTON.equals(prop))
    {
      if (GuiSettings.getShowResultTabCloseButton())
      {
        resultTab.showCloseButton(this);
      }
      else
      {
        resultTab.showCloseButton(null);
      }
    }
  }

	private boolean hasMoved;

	@Override
	public boolean startMove(int index)
	{
		boolean canMove = (index != resultTab.getTabCount() - 1);
		ignoreStateChange = canMove;
		hasMoved = false;
		return canMove;
	}

  @Override
  public void endMove(int finalIndex)
  {
    ignoreStateChange = false;
    if (!hasMoved) return;
    if (resultTab.getSelectedIndex() != finalIndex)
    {
      resultTab.setSelectedIndex(finalIndex);
    }
    else
    {
      updateResultInfos();
    }
  }

	@Override
	public void moveCancelled()
	{
		ignoreStateChange = false;
	}

	@Override
	public boolean moveTab(int oldIndex, int newIndex)
	{
		Component c = resultTab.getComponentAt(oldIndex);
		if (newIndex == resultTab.getTabCount() - 1) return false;

		String title = resultTab.getTitleAt(oldIndex);
		resultTab.remove(c);
		resultTab.add(c, newIndex);
		resultTab.setTitleAt(newIndex, title);
		hasMoved = true;
		return true;
	}

  @Override
  public boolean canCloseTab(int index)
  {
    return (index != resultTab.getTabCount() - 1);
  }

  @Override
  public void tabCloseButtonClicked(int index)
  {
    if (!canCloseTab(index)) return;

    if (confirmDiscardChanges(index, true))
    {
      discardResult(index, true);
    }
  }

}
