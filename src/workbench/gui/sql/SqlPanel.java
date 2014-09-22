/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.ComponentInputMap;
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
import workbench.interfaces.StatusBar;
import workbench.interfaces.ToolWindow;
import workbench.interfaces.ToolWindowManager;
import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.DbSettings;
import workbench.db.TransactionChecker;
import workbench.db.WbConnection;
import workbench.db.exporter.DataExporter;
import workbench.db.importer.DataStoreImporter;
import workbench.db.importer.DefaultImportOptions;
import workbench.db.importer.DefaultTextImportOptions;
import workbench.db.importer.ImportOptions;
import workbench.db.importer.TextImportOptions;

import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
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
import workbench.gui.actions.CopyAsDbUnitXMLAction;
import workbench.gui.actions.CopyAsSqlDeleteAction;
import workbench.gui.actions.CopyAsSqlDeleteInsertAction;
import workbench.gui.actions.CopyAsSqlInsertAction;
import workbench.gui.actions.CopyAsSqlMergeAction;
import workbench.gui.actions.CopyAsSqlUpdateAction;
import workbench.gui.actions.CopyAsTextAction;
import workbench.gui.actions.CopyRowAction;
import workbench.gui.actions.CreateDeleteScriptAction;
import workbench.gui.actions.CreateSnippetAction;
import workbench.gui.actions.DeleteDependentRowsAction;
import workbench.gui.actions.DeleteRowAction;
import workbench.gui.actions.DisplayDataFormAction;
import workbench.gui.actions.ExecuteAllAction;
import workbench.gui.actions.ExecuteCurrentAction;
import workbench.gui.actions.ExecuteSelAction;
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
import workbench.gui.components.ConnectionInfo;
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
import workbench.gui.components.WbToolbarSeparator;
import workbench.gui.dialogs.dataimport.ImportFileDialog;
import workbench.gui.dialogs.export.ExportFileDialog;
import workbench.gui.editor.InsertTipProvider;
import workbench.gui.editor.actions.IndentSelection;
import workbench.gui.editor.actions.ShowTipAction;
import workbench.gui.editor.actions.UnIndentSelection;
import workbench.gui.macros.MacroClient;
import workbench.gui.menu.TextPopup;
import workbench.gui.preparedstatement.ParameterEditor;

import workbench.storage.DataStore;

import workbench.sql.AppendResultAnnotation;
import workbench.sql.ErrorDescriptor;
import workbench.sql.OutputPrinter;
import workbench.sql.ParserType;
import workbench.sql.ScriptParser;
import workbench.sql.ScrollAnnotation;
import workbench.sql.StatementHistory;
import workbench.sql.StatementRunner;
import workbench.sql.StatementRunnerResult;
import workbench.sql.UseTabAnnotation;
import workbench.sql.VariablePool;
import workbench.sql.commands.SingleVerbCommand;
import workbench.sql.macros.MacroManager;
import workbench.sql.preparedstatement.PreparedStatementPool;
import workbench.sql.preparedstatement.StatementParameters;

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
		FilenameChangeListener, ResultReceiver, MacroClient, Moveable, TabCloser, StatusBar, ToolWindowManager, OutputPrinter
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

	private final List actions = new LinkedList();
	private final List<WbAction> toolbarActions = new LinkedList<>();

	private final List<FilenameChangeListener> filenameChangeListeners = new LinkedList<>();

	protected StopAction stopAction;
	protected ExecuteAllAction executeAll;
	protected ExecuteCurrentAction executeCurrent;
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
	protected CheckPreparedStatementsAction checkPreparedAction;
	protected ClearCompletionCacheAction clearCompletionCache;
	protected AutoCompletionAction autoCompletion;
	protected SqlPanelReloadAction reloadAction;
	protected ShowObjectInfoAction showObjectInfoAction;
	protected JoinCompletionAction joinCompletion;

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

	protected WbToolbar toolbar;
	protected ConnectionInfo connectionInfo;

	protected WbConnection dbConnection;

	private final Object connectionLock = new Object();

	protected boolean importRunning;
	protected boolean updateRunning;
	protected String tabName;

	private List<DbExecutionListener> execListener;
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

//</editor-fold>

	public SqlPanel(int clientId)
	{
		super(new BorderLayout());
		this.internalId = ++instanceCount;
		this.macroClientId = clientId;
		this.setName("sqlpanel-" + internalId);
		this.setBorder(WbSwingUtilities.EMPTY_BORDER);

		editor = EditorPanel.createSqlEditor();
		statusBar = new DwStatusBar(true, true);

		int defRows = GuiSettings.getDefaultMaxRows();
		if (defRows > 0)
		{
			statusBar.setMaxRows(defRows);
		}
		editor.setStatusBar(statusBar);
		editor.setBorder(new EtchedBorderTop());

		// The name of the component is used for the Jemmy GUI Tests
		editor.setName("sqleditor" + internalId);

		log = new LogArea();
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

		contentPanel = new WbSplitPane(JSplitPane.VERTICAL_SPLIT, true, this.editor, this.resultTab);
		contentPanel.setBorder(WbSwingUtilities.EMPTY_BORDER);
		contentPanel.setDividerSize(8);
		contentPanel.setOneTouchExpandable(true);
		contentPanel.setContinuousLayout(true);

		appendResults = GuiSettings.getDefaultAppendResults();

		this.add(this.contentPanel, BorderLayout.CENTER);
		this.add(statusBar, BorderLayout.SOUTH);

		this.initStatementHistory();

		this.initActions();
		this.setupActionMap();
		this.initToolbar();

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
		Settings.getInstance().addPropertyChangeListener(this, GuiSettings.PROPERTY_RESULTTAB_CLOSE_BUTTON);
		editor.setMacroExpansionEnabled(true, macroClientId);
		editor.setBracketCompletionEnabled(true);
		historyStatements = new StatementHistory(Settings.getInstance().getMaxHistorySize());
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
		List<NamedScriptLocation> bookmarks = reader.getBookmarks(text, getId());
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
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				editor.setCaretPosition(offset);
				editor.setFirstLine(line);
				editor.validate();
				editor.requestFocusInWindow();
			}
		});
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
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				updateTabTitle();
				Component c = getParent();
				if (c instanceof WbTabbedPane)
				{
					((WbTabbedPane)c).setCloseButtonEnabled(SqlPanel.this, !flag);
				}
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
	public WbToolbar getToolbar()
	{
		return this.toolbar;
	}

	private void initToolbar()
	{
		if (!SwingUtilities.isEventDispatchThread())
		{

			Exception e = new Exception("initToolbar() not called on EDT");
			e.printStackTrace();
		}

		toolbar = new WbToolbar();
		toolbar.addDefaultBorder();

		Iterator<WbAction> itr = toolbarActions.iterator();
		while (itr.hasNext())
		{
			WbAction a = itr.next();
			boolean toolbarSep = a.getCreateToolbarSeparator();
			if (toolbarSep)
			{
				toolbar.addSeparator();
			}
			a.addToToolbar(toolbar);
		}
		toolbar.addSeparator();
		connectionInfo = new ConnectionInfo(toolbar.getBackground());
		toolbar.add(connectionInfo);
	}

	@Override
	public void addToToolbar(WbAction anAction, boolean withSeperator)
	{
		if (!SwingUtilities.isEventDispatchThread())
		{
			Exception e = new Exception("addToToolbar() not called on EDT");
			e.printStackTrace();
		}
		this.toolbar.add(anAction.getToolbarButton(true), this.toolbar.getComponentCount() - 1);
		if (withSeperator) this.toolbar.add(new WbToolbarSeparator(), this.toolbar.getComponentCount() - 1);
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
				// reset a user-defined tab name if a file is loaded
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

		MakeLowerCaseAction makeLower = new MakeLowerCaseAction(this.editor);
		MakeUpperCaseAction makeUpper = new MakeUpperCaseAction(this.editor);

		this.editor.showFindOnPopupMenu();
		this.editor.showFormatSql();

		this.editor.addPopupMenuItem(this.executeSelected, true);
		this.editor.addPopupMenuItem(this.executeAll, false);
		this.editor.addPopupMenuItem(this.executeCurrent, false);

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

		SplitPaneExpander expander = new SplitPaneExpander(this.contentPanel);
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

		this.executeAll.setEnabled(false);
		this.executeSelected.setEnabled(false);

		this.toolbarActions.add(this.executeSelected);
		this.toolbarActions.add(this.executeCurrent);

		this.stopAction.setCreateToolbarSeparator(true);
		this.toolbarActions.add(this.stopAction);
		this.toolbarActions.add(this.sqlHistory.getShowFirstStatementAction());
		this.toolbarActions.add(this.sqlHistory.getShowPreviousStatementAction());
		this.toolbarActions.add(this.sqlHistory.getShowNextStatementAction());
		this.toolbarActions.add(this.sqlHistory.getShowLastStatementAction());

		this.toolbarActions.add(this.updateAction);
		this.toolbarActions.add(this.insertRow);
		this.toolbarActions.add(this.duplicateRow);
		this.toolbarActions.add(this.deleteRow);

		this.filterAction = new FilterDataAction(null);
		this.selectionFilterAction = new SelectionFilterAction();
		this.filterPicker = new FilterPickerAction(null);

		filterAction.setCreateToolbarSeparator(true);
		filterAction.setCreateMenuSeparator(true);
		this.selectionFilterAction.setCreateToolbarSeparator(true);
		this.toolbarActions.add(selectionFilterAction);
		this.toolbarActions.add(filterAction);
		this.toolbarActions.add(filterPicker);
		this.resetFilterAction = new ResetFilterAction(null);
		this.resetFilterAction.setCreateToolbarSeparator(true);
		this.toolbarActions.add(this.resetFilterAction);

		this.commitAction.setCreateToolbarSeparator(true);
		this.toolbarActions.add(this.commitAction);
		this.toolbarActions.add(this.rollbackAction);
		IgnoreErrorsAction ignore = new IgnoreErrorsAction();
		ignore.setCreateToolbarSeparator(true);
		this.toolbarActions.add(ignore);
		this.appendResultsAction = new AppendResultsAction(this);
		this.appendResultsAction.setEnabled(false);
		appendResultsAction.setCreateToolbarSeparator(true);
		this.toolbarActions.add(appendResultsAction);
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
		this.actions.add(this.clearCompletionCache);
		this.actions.add(showObjectInfoAction);

		this.formatSql = this.editor.getFormatSqlAction();
		this.formatSql.setCreateMenuSeparator(true);
		this.actions.add(this.formatSql);

		WbMenu config = new WbMenu(ResourceMgr.getString("MnuTxtSettings"));
		config.setParentMenuId(ResourceMgr.MNU_TXT_SQL);
		new AutoJumpNextStatement().addToMenu(config);
		appendResultsAction.addToMenu(config);
		new HighlightCurrentStatement().addToMenu(config);
		new HighlightErrorLineAction().addToMenu(config);
		this.checkPreparedAction = new CheckPreparedStatementsAction();
		this.checkPreparedAction.addToMenu(config);
		ignore.addToMenu(config);

		WbMenu codeTools = new WbMenu(ResourceMgr.getString("MnuTxtCodeTools"));
		codeTools.setParentMenuId(ResourceMgr.MNU_TXT_SQL);
		new CreateSnippetAction(this.editor).addToMenu(codeTools);
		new CleanJavaCodeAction(this.editor).addToMenu(codeTools);
		new MakeInListAction(this.editor).addToMenu(codeTools);
		new MakeNonCharInListAction(this.editor).addToMenu(codeTools);

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

	protected boolean isResultModified(int index)
	{
		DwPanel panel = (DwPanel)resultTab.getComponentAt(index);
		return panel.isModified();
	}

	protected boolean isDataModified()
	{
		if (this.currentData == null) return false;
		if (this.resultTab.getTabCount() == 1) return false; // only messages

		for (int i=0; i < resultTab.getTabCount() - 1; i++)
		{
			DwPanel panel = (DwPanel)resultTab.getComponentAt(i);
			if (panel.isModified()) return true;
		}
		return false;
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

	private final Runnable selector = new Runnable()
		{
			@Override
			public void run()
			{
				_selectEditor();
			}
		};

	public void selectEditorLater()
	{
		EventQueue.invokeLater(selector);
	}

	public void selectEditor()
	{
		WbSwingUtilities.invoke(selector);
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
		ScriptParser parser = createScriptParser();
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
			EventQueue.invokeLater(new Runnable()
			{
				@Override
				public void run()
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
	public void saveChangesToDatabase()
	{
		if (this.currentData == null)
		{
			Exception e = new IllegalStateException("No data panel!");
			LogMgr.logError("SqlPanel.saveChangesToDatabase()", "Save called without a current DwPanel!", e);
			return;
		}

		if (!this.currentData.prepareDatabaseUpdate()) return;

		this.setBusy(true);
		this.setCancelState(true);
		this.setExecuteActionStates(false);

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
			EventQueue.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					WbSwingUtilities.showErrorMessageKey(SqlPanel.this, "MsgOutOfMemoryError");
				}
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
		return this.actions;
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
		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				log.setText(msg);
			}
		});
	}

	/**
	 *	Show the panel with the log messages.
	 */
	@Override
	public void showLogPanel()
	{
		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				int index = resultTab.getTabCount() - 1;
				resultTab.setSelectedIndex(index);
			}
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
		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				resultTab.setSelectedIndex(index);
			}
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

	public boolean confirmDiscardChanges(int index)
	{
		if (index >= resultTab.getTabCount() - 1) return false;

		boolean isModified = (index == -1 ? isDataModified() : isResultModified(index));
		if (!isModified) return true;
		String title = getRealTabTitle();
		if (!GuiSettings.getConfirmDiscardResultSetChanges()) return true;
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
	public boolean canClosePanel(boolean checkTransAction)
	{
		boolean fileOk = this.checkAndSaveFile() && confirmDiscardChanges(-1);
		if (checkTransAction)
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

	SqlHistory getHistory()
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
	 * Implementation of the ResultReceiver interface
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
		this.editor.appendLine("\n\n");
		int pos = this.editor.getText().length();
		this.editor.appendLine(text);
		this.editor.setCaretPosition(pos);
		this.editor.scrollToCaret();
	}

	public void setStatementText(String aStatement)
	{
		this.storeStatementInHistory();
		if (this.editor.getCurrentFile() != null) this.editor.saveCurrentFile();
		this.editor.closeFile(true);
		this.editor.setText(aStatement);
	}

	@Override
	public String toString()
	{
		return this.getTabTitle();
	}

	@Override
	public void disconnect()
	{
		synchronized (this.connectionLock)
		{
			if (this.dbConnection != null)
			{
				this.setConnection(null);
			}
			if (this.currentData != null)
			{
				currentData.endEdit();
			}
			clearResultTabs();
			for (ToolWindow window : resultWindows)
			{
				window.disconnect();
			}
			setLogText("");
		}
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
		synchronized (this.connectionLock)
		{
			if (this.dbConnection != null)
			{
				this.dbConnection.removeChangeListener(this);
			}

			this.dbConnection = aConnection;
		}

		this.toggleAutoCommit.setConnection(this.dbConnection);

		if (this.clearCompletionCache != null) this.clearCompletionCache.setConnection(this.dbConnection);
		if (this.autoCompletion != null) this.autoCompletion.setConnection(this.dbConnection);
		if (showObjectInfoAction != null) showObjectInfoAction.checkEnabled();

		if (this.stmtRunner == null)
		{
			this.stmtRunner = new StatementRunner();
			this.stmtRunner.setRowMonitor(this.rowMonitor);
			this.stmtRunner.setMessagePrinter(this);
		}

		if (this.stmtRunner != null)
		{
			this.stmtRunner.setConnection(aConnection);
			this.stmtRunner.setResultLogger(this);
			this.stmtRunner.setHistoryProvider(this.historyStatements);
		}

		if (this.editor != null) this.editor.setDatabaseConnection(this.dbConnection);

		checkResultSetActions();
		checkCommitAction();

		setExecuteActionStates(false);

		if (this.dbConnection != null)
		{
			dbConnection.addChangeListener(this);

			// ConnectionInfo.setConnection() might access the database (to retrieve the current schema, database and user)
			// In order to not block the GUI this is done in a separate thread.
			WbThread info = new WbThread("Update connection info " + this.getId() )
			{
				@Override
				public void run()
				{
					try
					{
						connectionInfo.setConnection(dbConnection);

						// avoid the <IDLE> in transaction for Postgres that is caused by retrieving the current schema.
						// the second check for isBusy() is to prevent the situation where the user manages
						// to manually run a statement between the above setBusy(false) and this point)
						if (doRollbackOnSetConnection())
						{
							LogMgr.logDebug("SqlPanel.setConnection()", "Sending a rollback to end the current transaction");
							dbConnection.rollbackSilently();
						}
					}
					finally
					{
						setExecuteActionStates(true);
					}
				}
			};
			info.start();
		}
		else
		{
			connectionInfo.setConnection(null);
		}
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
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
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
				executionThread.join(Settings.getInstance().getIntProperty("workbench.sql.cancel.timeout", 2500));
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
		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				for (Action action : anActionList)
				{
					if (action != null) action.setEnabled(aFlag);
				}
			}
		});
	}

	public void runCurrentStatement()
	{
		String sql = this.editor.getText();
		int caret = this.editor.getCaretPosition();
		startExecution(sql, 0, caret, GuiSettings.getHighlightErrorStatement(), this.appendResults);
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
		this.startExecution(sql, offset, -1, highlight, this.appendResults);
	}

	@Override
	public void commit()
	{
		this.startExecution(SingleVerbCommand.COMMIT_VERB, 0, -1, false, this.appendResults);
	}

	@Override
	public void rollback()
	{
		this.startExecution(SingleVerbCommand.ROLLBACK_VERB, 0, -1, false, this.appendResults);
	}

	public void runAll()
	{
		String sql = this.editor.getText();
		this.startExecution(sql, 0, -1, GuiSettings.getHighlightErrorStatement(), this.appendResults);
	}

	private void startExecution(final String sql, final int offset, final int commandAtIndex, final boolean highlightError, final boolean appendResult)
	{
		if (this.isBusy()) return;

		if (GuiSettings.getConfirmDiscardResultSetChanges() && !appendResult && isDataModified())
		{
			if (!WbSwingUtilities.getProceedCancel(this, "MsgDiscardDataChanges")) return;
		}

		synchronized (this.connectionLock)
		{
			if (!this.isConnected())
			{
				LogMgr.logError("SqlPanel.startExecution()", "startExecution() was called but no connection available!", null);
				return;
			}

			if (this.dbConnection.isBusy())
			{
				showLogMessage(ResourceMgr.getString("ErrConnectionBusy"));
				return;
			}

			this.executionThread = new WbThread("SQL Execution Thread " + getThreadId())
			{
				@Override
				public void run()
				{
					runStatement(sql, offset, commandAtIndex, highlightError, appendResult);
				}
			};
			this.executionThread.start();
		}
	}

	private String getThreadId()
	{
		String id = getId();
		if (this.dbConnection != null)
		{
			id += "(" + dbConnection.getId() + ")";
		}
		return id;
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
	protected void runStatement(String sql, int selectionOffset, int commandAtIndex, boolean highlightOnError, boolean appendResult)
	{
		this.setStatusMessage(ResourceMgr.getString("MsgExecutingSql"));

		this.storeStatementInHistory();
		cancelExecution = false;

		setBusy(true);

		// the dbStart should be fired *after* updating the
		// history, as the history might be saved ("AutoSaveHistory") if the MainWindow
		// receives the execStart event
		fireDbExecStart();

		if (Settings.getInstance().getAutoSaveExternalFiles() && editor.hasFileLoaded() && editor.isModified())
		{
			editor.saveCurrentFile();
		}

		setCancelState(true);

		try
		{
			this.displayResult(sql, selectionOffset, commandAtIndex, highlightOnError, appendResult);
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

	/**
	 * Re-run the SQL of the current result in the background.
	 */
	public void reloadCurrent()
	{
		if (isBusy()) return;
		if (currentData == null) return;

		if (GuiSettings.getConfirmDiscardResultSetChanges() && currentData != null && currentData.isModified())
		{
			if (!WbSwingUtilities.getProceedCancel(this, "MsgDiscardDataChanges")) return;
		}

		this.executionThread = new WbThread("SQL Execution Thread " + getThreadId())
		{
			@Override
			public void run()
			{
				runCurrentSql();
			}
		};
		this.executionThread.start();
	}

	protected void runCurrentSql()
	{
		if (isBusy()) return;
		if (currentData == null) return;

		cancelExecution = false;
		setBusy(true);

		// the dbStart should be fired *after* updating the
		// history, as the history might be saved ("AutoSaveHistory") if the MainWindow
		// receives the execStart event
		fireDbExecStart();
		setCancelState(true);
		try
		{
			currentData.runCurrentSql(true);
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
			this.fireDbExecEnd();

			// setBusy(false) should be called after dbExecEnd()
			// otherwise the panel would indicate it's not busy, but
			// the connection would still be marked as busy
			this.setBusy(false);
			this.executionThread = null;
		}
	}

	private boolean macroExecution = false;

	@Override
	public void executeMacroSql(final String sql, final boolean replaceText, boolean appendData)
	{
		if (isBusy()) return;
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
		this.startExecution(sql, 0, -1, false, this.appendResults || appendData);
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
		appendMessage(logMessage);
	}

	private void appendMessage(final String logMessage, final String ... moreMessages)
	{
		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
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
			}
		});
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
		String title = null;
		if (dbConnection != null)
		{
			title = dbConnection.getProfile().getName();
		}
		Window w = SwingUtilities.getWindowAncestor(this);
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
		if (!confirmDiscardChanges(index)) return;
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
		if (!confirmDiscardChanges(index)) return;
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
			WbSwingUtilities.invoke(new Runnable()
			{
				@Override
				public void run()
				{
					int index = 0;
					while (index < resultTab.getTabCount() - 1)
					{
						Component c = resultTab.getComponentAt(index);
						DwPanel panel = (DwPanel)c;
						if (filter.shouldClose(panel, index) && confirmDiscardChanges(index))
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
				}
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
				EventQueue.invokeLater(new Runnable()
				{
					@Override
					public void run()
					{
						resultTab.fireStateChanged();
					}
				});
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
			if (!confirmDiscardChanges(i)) return;
		}
		clearResultTabs();
	}

	private int findFirstReused()
	{
		for (int i=0; i < resultTab.getTabCount() - 1; i ++)
		{
			Component c = resultTab.getComponentAt(i);
			if (c instanceof DwPanel)
			{
				DwPanel panel = (DwPanel)c;
				if (panel.wasReUsed()) return i;
			}
		}
		return -1;
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
	public void clearResultTabs()
	{
		try
		{
			ignoreStateChange = true;
			WbSwingUtilities.invoke(new Runnable()
			{
				@Override
				public void run()
				{
					while (resultTab.getTabCount() > 1)
					{
						Component c = resultTab.getComponentAt(0);
						if (c instanceof DwPanel)
						{
							DwPanel panel = (DwPanel)c;
							panel.removePropertyChangeListener(SqlPanel.this);
							panel.dispose();
						}
						resultTab.removeTabAt(0);
					}
					resultTab.setSelectedIndex(0);
					boolean wasNull = currentData == null;
					currentData = null;
					if (!wasNull)
					{
						updateProxiedActions();
					}
					checkResultSetActions();
				}
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
		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				_updateProxiedActions();
			}
		});
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

	public ScriptParser createScriptParser()
	{
		ScriptParser scriptParser = new ScriptParser(ParserType.getTypeFromConnection(this.dbConnection));
		scriptParser.setAlternateDelimiter(dbConnection.getAlternateDelimiter());
		scriptParser.setCheckEscapedQuotes(Settings.getInstance().getCheckEscapedQuotes());
		scriptParser.setEmptyLineIsDelimiter(Settings.getInstance().getEmptyLineIsDelimiter());

		if (this.dbConnection != null)
		{
			DbSettings db = this.dbConnection.getDbSettings();
			if (db == null)
			{
				LogMgr.logError("SqlPanel.createScriptParser()", "No db settings available!", null);
				return scriptParser;
			}
			scriptParser.setSupportOracleInclude(db.supportShortInclude());
			scriptParser.setCheckForSingleLineCommands(db.supportSingleLineCommands());
			scriptParser.setAlternateLineComment(db.getLineComment());
			scriptParser.setSupportIdioticQuotes(db.getUseIdioticQuotes());
		}
		else
		{
			LogMgr.logError("SqlPanel.createScriptParser", "Created a script parser without a connection!", null);
		}
		return scriptParser;
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

	private void displayResult(String script, int selectionOffset, int cursorPos, boolean highlightOnError, boolean appendResult)
	{
		if (script == null) return;

		boolean logWasCompressed = false;
		boolean jumpToNext = (cursorPos > -1 && Settings.getInstance().getAutoJumpNextStatement());
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

		ScriptParser scriptParser = createScriptParser();

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

		if (this.macroExecution)
		{
			// executeMacro() will set "macroExecution" so that we can
			// log the macro statement here. Otherwise we wouldn't know at this point
			// that a macro is beeing executed
			macroRun = true;
			appendToLog(ResourceMgr.getString("MsgExecutingMacro") + ":\n" + script + "\n");
		}
		else
		{
			String cleanSql = SqlUtil.trimSemicolon(script.trim());
			String macro = MacroManager.getInstance().getMacroText(macroClientId, cleanSql);
			if (macro != null)
			{
				appendToLog(ResourceMgr.getString("MsgExecutingMacro") + ":\n" + cleanSql + "\n");
				script = macro;
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

			if (count == 0)
			{
				this.appendToLog(ResourceMgr.getString("ErrNoCommand"));
				this.showLogPanel();
				return;
			}

			if (cursorPos > -1)
			{
				// cursorPos > -1 means that the statement at the cursor position should be executed
				count = 1;
				int realPos = cursorPos;
				if (GuiSettings.getUseStatementInCurrentLine())
				{
					realPos = editor.getLineStartOffset(editor.getCaretLine());
				}
				startIndex = scriptParser.getCommandIndexAtCursorPos(realPos);
				endIndex = startIndex + 1;
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
				clearResultTabs();
				firstResultIndex = 0;
			}



			if (count > 1)
			{
				logWasCompressed = !this.stmtRunner.getVerboseLogging();
			}

			StringBuilder finishedMsg1 = new StringBuilder(ResourceMgr.getString("TxtScriptStatementFinished1"));
			finishedMsg1.append(' ');
			StringBuilder finishedMsg2 = new StringBuilder(20);
			finishedMsg2.append(' ');
			String msg = ResourceMgr.getString("TxtScriptStatementFinished2");
			msg = StringUtil.replace(msg, "%total%", NumberStringCache.getNumberString(count));
			finishedMsg2.append(msg);

			final int finishedSize = finishedMsg1.length() + finishedMsg2.length() + 5;

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
			String currentSql = null;

			int resultSets = 0;

			macroExecution = false;

			long totalRows = 0;
			lastScriptExecTime = 0;
			stmtRunner.setMaxRows(maxRows);

			ignoreStateChange = true;
			ErrorDescriptor errorDetails = null;

			for (int i=startIndex; i < endIndex; i++)
			{
				currentSql = scriptParser.getCommand(i);
				historyStatements.add(currentSql);

				if (fixNLPattern != null)
				{
					currentSql = fixNLPattern.matcher(currentSql).replaceAll(nl);
				}
				if (currentSql.length() == 0) continue;

				String macro = MacroManager.getInstance().getMacroText(macroClientId, currentSql);
				if (macro != null)
				{
					appendToLog(ResourceMgr.getString("MsgExecutingMacro") + ":\n" + currentSql + "\n");
					macroRun = true;
					currentSql = macro;
				}

				// By calling yield() we make sure that
				// this thread can actually be interrupted!
				Thread.yield();
				if (cancelExecution) break;

				if (highlightCurrent && !editor.isModifiedAfter(scriptStart))
				{
					highlightStatement(scriptParser, i, selectionOffset);
				}

				stmtRunner.setQueryTimeout(timeout);
				stmtRunner.runStatement(currentSql);
				statementResult = this.stmtRunner.getResult();

				if (statementResult == null) continue;

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
					String cancelMsg = ResourceMgr.getString("MsgSqlCancelledDuringPrompt");
					cancelMsg = cancelMsg.replace("%nr%", NumberStringCache.getNumberString(i+1));
					this.appendToLog(cancelMsg);
					this.showLogPanel();
					continue;
				}

				resultSets += this.addResult(statementResult);
				stmtTotal += statementResult.getExecutionDuration();

				// the WbFeedback command might change the feedback level
				// so it needs to be checked each time.
				if (count > 1) logWasCompressed = logWasCompressed || !this.stmtRunner.getVerboseLogging();

				StringBuilder finishedMsg = new StringBuilder(finishedSize);
				finishedMsg.append(finishedMsg1);
				finishedMsg.append(NumberStringCache.getNumberString(i + 1));
				finishedMsg.append(finishedMsg2);
				String currentMsg = finishedMsg.toString();

				if (!logWasCompressed)
				{
					showResultMessage(statementResult);
					StringBuilder logmsg = new StringBuilder(100);
					String timing = statementResult.getTimingMessage();
					if (timing != null)
					{
						logmsg.append('\n');
						logmsg.append(timing);
					}

					logmsg.append('\n');
					if (count > 1)
					{
						logmsg.append('(');
						logmsg.append(currentMsg);
						logmsg.append(")\n\n");
					}
					this.appendToLog(logmsg.toString());
				}
				else if (statementResult.hasWarning())
				{
					// Warnings should always be shown, even if the log output is "compressed"
					String verb = SqlUtil.getSqlVerb(currentSql);
					String warn = StringUtil.replace(ResourceMgr.getString("MsgStmtCompletedWarn"), "%verb%", verb);
					this.appendToLog(warn + "\n");
				}

				if (count > 1)
				{
					this.statusBar.setStatusMessage(currentMsg);
				}

				// this will be set by confirmExecution() if
				// Cancel was selected
				if (cancelAll) break;

				if (statementResult.isSuccess())
				{
					totalRows += statementResult.getTotalUpdateCount();
				}
				else
				{
					commandWithError = i;
					errorDetails = statementResult.getErrorDescriptor();

					// error messages should always be shown in the log
					// panel, even if compressLog is enabled (if it is not enabled
					// the messages have been appended to the log already)
					if (logWasCompressed) this.appendToLog(statementResult.getMessageBuffer().toString());

					if (count > 1 && onErrorAsk && (i < (count - 1)))
					{
						// the animated gif needs to be turned off when a
						// dialog is displayed, otherwise Swing uses too much CPU
						iconHandler.showBusyIcon(false);

						if (!macroRun && !editor.isModifiedAfter(scriptStart))
						{
							highlightError(true, scriptParser, commandWithError, selectionOffset, null);
						}

						// force a refresh in order to display the selection
						WbSwingUtilities.repaintLater(this);
						Thread.yield();

						String question = ResourceMgr.getFormattedString("MsgScriptStatementError",
							NumberStringCache.getNumberString(i+1),
							NumberStringCache.getNumberString(count));
						int choice = WbSwingUtilities.getYesNoIgnoreAll(this, question);
						iconHandler.showBusyIcon(true);

						if (choice == JOptionPane.NO_OPTION)
						{
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
				this.stmtRunner.statementDone();
				if (this.cancelExecution) break;

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
				highlightError(highlightOnError, scriptParser, commandWithError, selectionOffset, errorDetails);
			}

			if (failuresIgnored > 0)
			{
				this.appendToLog("\n" + ResourceMgr.getFormattedString("MsgTotalStatementsFailed", failuresIgnored)+ "\n");
			}

			if (logWasCompressed)
			{
				msg = ResourceMgr.getFormattedString("MsgTotalStatementsExecuted", executedCount) + "\n";
				this.appendToLog(msg);
				msg = ResourceMgr.getFormattedString("MsgRowsAffected", totalRows) + "\n\n";
				this.appendToLog(msg);
			}

			ignoreStateChange = false;
			if (resultSets > 0)
			{
				if (resultTab.getTabCount() - 1 == currentResultCount)
				{
					// this means at least one result was re-used
					int index = findFirstReused();
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

			if (count > 1)
			{
				String finish = ResourceMgr.getString("TxtScriptFinished");
				if (GuiSettings.showScriptFinishTime())
				{
					finish += " (" + StringUtil.getCurrentTimestamp() + ")";
				}
				this.appendToLog(finish + "\n");
				String duration = df.formatDuration(lastScriptExecTime, (lastScriptExecTime < DurationFormatter.ONE_MINUTE));
				String s = ResourceMgr.getString("MsgScriptExecTime") + " " + duration + "\n";
				this.appendToLog(s);
			}

			restoreSelection = restoreSelection && !GuiSettings.getKeepCurrentSqlHighlight() && !editorWasModified;
			restoreSelection(highlightCurrent, jumpToNext, restoreSelection, currentCursor, oldSelectionStart, oldSelectionEnd, commandWithError, startIndex, endIndex, scriptParser);
		}
		catch (SQLException e)
		{
			if (statementResult != null) this.showLogMessage(statementResult.getMessageBuffer().toString());
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
				this.showLogMessage(statementResult.getMessageBuffer().toString());
				statementResult.clear();
			}
		}
		finally
		{
			stmtRunner.done();
			ignoreStateChange = false;
		}
	}

	private boolean shouldRunNextStatement(ScriptParser parser, int statementIndex)
	{
		if (!Settings.getInstance().getAutoRunExportStatement()) return false;
		if (this.stmtRunner == null) return false;
		if (this.stmtRunner.getConsumer() == null) return false;

		if (statementIndex + 1 < parser.getSize())
		{
			String verb = SqlUtil.getSqlVerb(parser.getCommand(statementIndex + 1));
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
		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				_restoreSelection(highlightCurrent, jumpToNext, restoreSelection, currentCursor, oldSelectionStart, oldSelectionEnd, commandWithError, startIndex, endIndex, scriptParser);
			}
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
				this.appendToLog("\n");
			}
			else
			{
				LogMgr.logError("SqlPanel.showResultMessage()", "Not enough memory to show all messages!", null);
			}
		}
		catch (OutOfMemoryError oome)
		{
			result.clearMessageBuffer();
			clearLog();
			System.gc(); // as we have just freed some memory the gc() does make sense here.
			WbManager.getInstance().setOutOfMemoryOcurred();
			final boolean success = result.isSuccess();
			EventQueue.invokeLater(new Runnable()
			{
				@Override
				public void run()
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
				}
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
		startExecution(comment + "\n" + sql, 0, -1, false, true);
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
			WbSwingUtilities.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					resultTab.setSelectedIndex(newIndex);
				}
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
			String title = this.resultTab.getTitleAt(i);
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
		this.resultTab.insertTab(resultName, null, data, null, newIndex);
		data.showGeneratingSQLAsTooltip();
		data.setName("dwresult" + NumberStringCache.getNumberString(newIndex));
		if (this.resultTab.getTabCount() == 2)
		{
			this.resultTab.setSelectedIndex(0);
		}
		data.checkLimitReachedDisplay();
		String sql = (ds != null ? ds.getGeneratingSql() : "");
		ScrollAnnotation scroll = new ScrollAnnotation();
		boolean scrollToEnd = scroll.scrollToEnd(sql);
		int line = scroll.scrollToLine(sql);
		if (scrollToEnd && tbl != null)
		{
			tbl.scrollToRow(tbl.getRowCount() - 1);
		}
		else if (line > 0)
		{
			tbl.scrollToRow(line - 1);
		}
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
		if (!result.isSuccess()) return 0;
		final String sql = result.getSourceCommand();
		final long time = result.getExecutionDuration();

		int count = 0;

		if (result.hasDataStores())
		{
			final List<DataStore> results = result.getDataStores();
			count += results.size();
			final List<DwPanel> panels = new ArrayList<>(results.size());
			WbSwingUtilities.invoke(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						UseTabAnnotation useTab = new UseTabAnnotation();
						for (DataStore ds : results)
						{
							String gen = StringUtil.isNonBlank(sql) ? sql : ds.getGeneratingSql();
							String tabName = useTab.getResultName(sql);

							DwPanel p = null;
							if (StringUtil.isNonEmpty(tabName))
							{
								ds.setResultName(tabName);
								p = findResultPanelByName(tabName);
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
			WbSwingUtilities.invoke(new Runnable()
			{
				@Override
				public void run()
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
				}
			});
		}

		return count;
	}

	private void highlightStatement(ScriptParser scriptParser, int command, int startOffset)
	{
		if (this.editor == null) return;
		final int startPos = scriptParser.getStartPosForCommand(command) + startOffset;
		final int endPos = scriptParser.getEndPosForCommand(command) + startOffset;
		final int line = this.editor.getLineOfOffset(startPos);

		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				editor.scrollTo(line, 0);
				editor.selectStatementTemporary(startPos, endPos);
			}
		});
	}

	protected void highlightError(final boolean doHighlight, ScriptParser scriptParser, int commandWithError, int startOffset, ErrorDescriptor error)
	{
		if (this.editor == null) return;
		if (!doHighlight && !GuiSettings.jumpToError()) return;
		if (scriptParser == null) return;

		final int startPos;
		final int endPos;
		final int line;
		final int newCaret;

		boolean jumpToError = false;

		if (error != null && error.getErrorPosition() > -1)
		{
			int startOfCommand = scriptParser.getStartPosForCommand(commandWithError) + startOffset;
			line = this.editor.getLineOfOffset(startOfCommand + error.getErrorPosition());
			startPos = editor.getLineStartOffset(line);
			endPos = editor.getLineEndOffset(line) - 1;
			jumpToError = true;
			if (error.getErrorColumn() > -1)
			{
				newCaret = startPos + error.getErrorColumn();
			}
			else
			{
				newCaret = startOfCommand + error.getErrorPosition();
			}
		}
		else
		{
			startPos = scriptParser.getStartPosForCommand(commandWithError) + startOffset;
			endPos = scriptParser.getEndPosForCommand(commandWithError) + startOffset;
			line = this.editor.getLineOfOffset(startPos);
			newCaret = -1;
		}

		if (!doHighlight && !jumpToError) return;

		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				if (!editor.isLineVisible(line))
				{
					editor.scrollTo(line, 0);
				}
				if (doHighlight)
				{
					editor.selectError(startPos, endPos);
				}
				else
				{
					editor.setCaretPosition(newCaret);
				}
			}
		});
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

		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				importFileAction.setEnabled(mayEdit);
				importClipAction.setEnabled(mayEdit);

				findDataAgainAction.setEnabled(findNext);
				copySelectedMenu.setEnabled(hasResult);
				reloadAction.checkEnabled();
				showFormAction.setEnabled(hasRows);
			}
		});
	}

	private void setExecuteActionStates(final boolean flag)
	{
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				executeAll.setEnabled(flag);
				executeSelected.setEnabled(flag);
				executeCurrent.setEnabled(flag);

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
			}
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
		synchronized (this.connectionLock)
		{
			threadBusy = busy;
			if (iconHandler != null) iconHandler.showBusyIcon(busy);
			setExecuteActionStates(!busy);
			if (disableEditor())
			{
				if (editor != null) editor.setEditable(!busy);
			}
			if (sqlHistory != null) sqlHistory.setEnabled(!busy);
		}
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
		if (this.execListener == null) this.execListener = Collections.synchronizedList(new LinkedList<DbExecutionListener>());
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
		synchronized (this.connectionLock)
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
	}

	public void fireDbExecEnd()
	{
		synchronized (this.connectionLock)
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
	}

	@Override
	public void reset()
	{
		locked = false;
		if (editor != null) editor.reset();
		clearLog();
		clearResultTabs();
		if (this.currentData != null)
		{
			this.currentData.dispose();
		}
		currentData = null;
		if (iconHandler != null) iconHandler.flush();
		if (sqlHistory != null) sqlHistory.clear();
	}

	@Override
	public void dispose()
	{
		Settings.getInstance().removePropertyChangeListener(this);
		Settings.getInstance().removeFontChangedListener(this);

		reset();
		if (iconHandler != null) iconHandler.dispose();
		if (this.stmtRunner != null) this.stmtRunner.dispose();

		if (this.execListener != null) execListener.clear();
		this.execListener = null;

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

		if (this.toolbarActions != null)
		{
			for (WbAction action : toolbarActions)
			{
				if (action != null)
				{
					action.dispose();
				}
			}
			this.toolbarActions.clear();
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
		this.connectionInfo = null;
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

		if (evt.getSource() == this.dbConnection && WbConnection.PROP_AUTOCOMMIT.equals(prop))
		{
			this.checkCommitAction();
		}
		else if (evt.getSource() == this.currentData && prop.equals("updateTable"))
		{
			this.checkResultSetActions();
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

		if (confirmDiscardChanges(index))
		{
			discardResult(index, true);
		}
	}

}
