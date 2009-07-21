/*
 * SqlPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
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
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import workbench.WbManager;
import workbench.db.DbSettings;
import workbench.db.WbConnection;
import workbench.db.exporter.DataExporter;
import workbench.db.importer.DataStoreImporter;
import workbench.gui.MainWindow;
import workbench.gui.actions.AppendResultsAction;
import workbench.gui.actions.CloseResultTabAction;
import workbench.gui.actions.FilterDataAction;
import workbench.gui.actions.FilterPickerAction;
import workbench.gui.actions.ImportClipboardAction;
import workbench.gui.actions.ReplaceDataAction;
import workbench.gui.actions.ResetFilterAction;
import workbench.gui.actions.SelectionFilterAction;
import workbench.gui.actions.ViewMessageLogAction;
import workbench.gui.components.GenericRowMonitor;
import workbench.gui.components.WbTabbedPane;
import workbench.gui.dialogs.dataimport.ImportFileDialog;
import workbench.interfaces.DbExecutionNotifier;
import workbench.interfaces.ParameterPrompter;
import workbench.interfaces.ResultReceiver;
import workbench.sql.StatementRunnerResult;
import workbench.util.ExceptionUtil;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.AutoCompletionAction;
import workbench.gui.actions.AutoJumpNextStatement;
import workbench.gui.actions.CheckPreparedStatementsAction;
import workbench.gui.actions.CleanJavaCodeAction;
import workbench.gui.actions.ClearCompletionCacheAction;
import workbench.gui.actions.ClearMessagesAction;
import workbench.gui.actions.CommitAction;
import workbench.gui.actions.CopyAsSqlDeleteInsertAction;
import workbench.gui.actions.CopyAsSqlInsertAction;
import workbench.gui.actions.CopyAsSqlUpdateAction;
import workbench.gui.actions.CopyRowAction;
import workbench.gui.actions.CreateDeleteScriptAction;
import workbench.gui.actions.CreateSnippetAction;
import workbench.gui.actions.CopyAsTextAction;
import workbench.gui.actions.DeleteDependentRowsAction;
import workbench.gui.actions.DeleteRowAction;
import workbench.gui.actions.DisplayDataFormAction;
import workbench.gui.actions.SqlPanelReloadAction;
import workbench.gui.actions.ExecuteAllAction;
import workbench.gui.actions.ExecuteCurrentAction;
import workbench.gui.actions.ExecuteSelAction;
import workbench.gui.actions.ExpandEditorAction;
import workbench.gui.actions.ExpandResultAction;
import workbench.gui.actions.FileDiscardAction;
import workbench.gui.actions.FindAction;
import workbench.gui.actions.FindDataAction;
import workbench.gui.actions.FindDataAgainAction;
import workbench.gui.actions.FormatSqlAction;
import workbench.gui.actions.HighlightCurrentStatement;
import workbench.gui.actions.IgnoreErrorsAction;
import workbench.gui.actions.ImportFileAction;
import workbench.gui.actions.InsertRowAction;
import workbench.gui.actions.MakeInListAction;
import workbench.gui.actions.MakeLowerCaseAction;
import workbench.gui.actions.MakeNonCharInListAction;
import workbench.gui.actions.MakeUpperCaseAction;
import workbench.gui.actions.OptimizeAllColumnsAction;
import workbench.gui.actions.OptimizeRowHeightAction;
import workbench.gui.actions.PrintAction;
import workbench.gui.actions.PrintPreviewAction;
import workbench.gui.actions.ResetHighlightAction;
import workbench.gui.actions.RollbackAction;
import workbench.gui.actions.SaveDataAsAction;
import workbench.gui.actions.SelectEditorAction;
import workbench.gui.actions.SelectKeyColumnsAction;
import workbench.gui.actions.SelectMaxRowsAction;
import workbench.gui.actions.SelectResultAction;
import workbench.gui.actions.ShowObjectInfoAction;
import workbench.gui.actions.SpoolDataAction;
import workbench.gui.actions.StopAction;
import workbench.gui.actions.ToggleAutoCommitAction;
import workbench.gui.actions.UndoExpandAction;
import workbench.gui.actions.UpdateDatabaseAction;
import workbench.gui.actions.WbAction;
import workbench.gui.components.ConnectionInfo;
import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.EtchedBorderTop;
import workbench.gui.components.WbMenu;
import workbench.gui.components.WbScrollPane;
import workbench.gui.components.WbSplitPane;
import workbench.gui.components.WbTable;
import workbench.gui.components.WbToolbar;
import workbench.gui.components.WbToolbarSeparator;
import workbench.gui.dialogs.dataimport.ImportOptions;
import workbench.gui.dialogs.dataimport.TextImportOptions;
import workbench.gui.dialogs.export.ExportFileDialog;
import workbench.gui.editor.actions.IndentSelection;
import workbench.gui.editor.actions.UnIndentSelection;
import workbench.gui.macros.MacroClient;
import workbench.gui.menu.TextPopup;
import workbench.gui.preparedstatement.ParameterEditor;
import workbench.interfaces.Commitable;
import workbench.interfaces.Connectable;
import workbench.interfaces.DbExecutionListener;
import workbench.interfaces.DbUpdater;
import workbench.interfaces.ExecutionController;
import workbench.interfaces.FilenameChangeListener;
import workbench.interfaces.FontChangedListener;
import workbench.interfaces.FormattableSql;
import workbench.interfaces.Interruptable;
import workbench.interfaces.JobErrorHandler;
import workbench.interfaces.MainPanel;
import workbench.interfaces.ResultLogger;
import workbench.interfaces.Exporter;
import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.sql.DelimiterDefinition;
import workbench.sql.macros.MacroManager;
import workbench.sql.ScriptParser;
import workbench.sql.StatementRunner;
import workbench.sql.VariablePool;
import workbench.sql.commands.SingleVerbCommand;
import workbench.sql.preparedstatement.PreparedStatementPool;
import workbench.sql.preparedstatement.StatementParameters;
import workbench.storage.DataStore;
import workbench.util.LowMemoryException;
import workbench.util.MemoryWatcher;
import workbench.util.MessageBuffer;
import workbench.util.SqlUtil;
import workbench.util.NumberStringCache;
import workbench.util.StringUtil;
import workbench.util.WbFile;
import workbench.util.WbThread;
import workbench.util.WbWorkspace;


/**
 *	A panel with an SQL editor (EditorPanel), a log panel and
 *	a panel for displaying SQL results (DwPanel)
 *
 * @author  support@sql-workbench.net
 */
public class SqlPanel
	extends JPanel
	implements FontChangedListener,
		PropertyChangeListener, ChangeListener,
		MainPanel, Exporter, DbUpdater, Interruptable, FormattableSql, Commitable,
		JobErrorHandler, ExecutionController, ResultLogger, ParameterPrompter, DbExecutionNotifier,
		FilenameChangeListener, ResultReceiver, MacroClient
{
	//<editor-fold defaultstate="collapsed" desc=" Variables ">
	protected EditorPanel editor;
	protected DwPanel currentData;
	protected SqlHistory sqlHistory;

	protected JTextArea log;
	protected WbTabbedPane resultTab;
	protected JSplitPane contentPanel;
	protected boolean threadBusy;
	protected boolean cancelExecution;

	private List actions = new LinkedList();
	private List<WbAction> toolbarActions = new LinkedList<WbAction>();

	private List<FilenameChangeListener> filenameChangeListeners;

	protected StopAction stopAction;
	protected ExecuteAllAction executeAll;
	protected ExecuteCurrentAction executeCurrent;
	protected ExecuteSelAction executeSelected;

	private int internalId;

	// Actions from DwPanel
	protected CopyAsTextAction dataToClipboard;
	protected SaveDataAsAction exportDataAction;
	protected CopyAsSqlInsertAction copyAsSqlInsert;
	protected CopyAsSqlUpdateAction copyAsSqlUpdate;
	protected CopyAsSqlDeleteInsertAction copyAsSqlDeleteInsert;
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
	protected CheckPreparedStatementsAction checkPreparedAction;
	protected ClearCompletionCacheAction clearCompletionCache;
	protected AutoCompletionAction autoCompletion;
	protected SqlPanelReloadAction reloadAction;
	protected ShowObjectInfoAction showObjectInfoAction;

	protected WbMenu copySelectedMenu;
	protected ToggleAutoCommitAction toggleAutoCommit;
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

	private static final Border statusBarBorder = new CompoundBorder(new EmptyBorder(2, 1, 0, 1), new EtchedBorder());

	private boolean appendResults = false;

	protected DwStatusBar statusBar;
	protected StatementRunner stmtRunner;
	protected GenericRowMonitor rowMonitor;
	protected IconHandler iconHandler;
	private boolean locked = false;

//</editor-fold>

	public SqlPanel(int anId)
	{
		super(new BorderLayout());
		this.setId(anId);
		this.setBorder(WbSwingUtilities.EMPTY_BORDER);

		editor = EditorPanel.createSqlEditor();
		statusBar = new DwStatusBar(true, true);
		statusBar.setBorder(statusBarBorder);
		editor.setStatusBar(statusBar);
		editor.setBorder(new EtchedBorderTop());
		// The name of the component is used for the Jemmy GUI Tests
		editor.setName("sqleditor" + anId);

		log = new LogArea();
		// The name of the component is used for the Jemmy GUI Tests
		log.setName("msg" + anId);

		this.resultTab = new WbTabbedPane();
		this.resultTab.setTabPlacement(JTabbedPane.TOP);
		this.resultTab.setFocusable(false);
		// The name of the component is used for the Jemmy GUI Tests
		this.resultTab.setName("resultspane");

		JScrollPane scroll = new WbScrollPane(log);
		this.resultTab.addTab(ResourceMgr.getString("LblTabMessages"), scroll);

		this.contentPanel = new WbSplitPane(JSplitPane.VERTICAL_SPLIT, true, this.editor, this.resultTab);
		this.contentPanel.setBorder(WbSwingUtilities.EMPTY_BORDER);
		this.contentPanel.setOneTouchExpandable(true);
		this.contentPanel.setContinuousLayout(true);

		this.add(this.contentPanel, BorderLayout.CENTER);
		this.add(statusBar, BorderLayout.SOUTH);

		this.initStatementHistory();

		this.initActions();
		this.setupActionMap();
		this.initToolbar();

		Settings s = Settings.getInstance();
		s.addFontChangedListener(this);

		this.rowMonitor = new GenericRowMonitor(this.statusBar);

		// The listeners have to be added as late as possible to ensure
		// that everything is created properly in case an event is fired
		this.resultTab.addChangeListener(this);
		this.editor.addFilenameChangeListener(this);
		new ResultTabHandler(this.resultTab, this);
		iconHandler = new IconHandler(this);
	}

	public void setLocked(boolean flag)
	{
		this.locked = flag;
		updateTabTitle();
	}

	public boolean isLocked()
	{
		return locked;
	}

	public String getId()
	{
		return NumberStringCache.getNumberString(this.internalId);
	}

	public void setId(int anId)
	{
		this.setName("sqlpanel" + anId);
		this.internalId = anId;
	}

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
		height -= (this.statusBar.getPreferredSize().getHeight() * 3);
		height -= this.editor.getHScrollBarHeight();
		height -= this.resultTab.getTabHeight();
		height -= this.contentPanel.getDividerSize();
		height -= 8;
		int loc = height / 2;
		if (loc <= 50) loc = -1;
		this.contentPanel.setDividerLocation(loc);
	}

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
		return this.editor.hasFileLoaded();
	}

	public boolean checkAndSaveFile()
	{
		if (this.editor == null) return true;
		int result = this.editor.checkAndSaveFile();
		if (result == JOptionPane.CANCEL_OPTION) return false;
		return true;
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
			updateTabTitle();
		}
	}

	private void fireFilenameChanged(String aNewName)
	{
		updateTabTitle();
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

		this.actions.add(editor.getFileOpenAction());
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
		this.actions.add(this.editor.getFindAgainAction());
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


		// The update actions are proxies for the real ones
		// Once a result tab (DwPanel) has been displayed
		// they are "dispatched" to the real ones
		this.updateAction = new UpdateDatabaseAction(null);
		this.insertRow = new InsertRowAction(null);
		this.deleteRow = new DeleteRowAction(null);
		this.deleteDependentRow = new DeleteDependentRowsAction(null);
		this.duplicateRow = new CopyRowAction(null);
		this.selectKeys = new SelectKeyColumnsAction(null);
		this.showFormAction = new DisplayDataFormAction(null);
		reloadAction = new SqlPanelReloadAction(this);
		showObjectInfoAction = new ShowObjectInfoAction(this);

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

		this.copyAsSqlInsert = new CopyAsSqlInsertAction(null);
		this.actions.add(this.copyAsSqlInsert);

		this.copyAsSqlUpdate = new CopyAsSqlUpdateAction(null);
		this.actions.add(this.copyAsSqlUpdate);

		this.copyAsSqlDeleteInsert = new CopyAsSqlDeleteInsertAction(null);
		this.actions.add(this.copyAsSqlDeleteInsert);

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

		this.actions.add(new AutoJumpNextStatement());
		this.appendResultsAction = new AppendResultsAction(this);
		this.appendResultsAction.setEnabled(false);
		this.actions.add(appendResultsAction);
		a = new HighlightCurrentStatement();
		a.setCreateMenuSeparator(false);
		this.actions.add(a);
		this.checkPreparedAction = new CheckPreparedStatementsAction();
		this.actions.add(this.checkPreparedAction);
		IgnoreErrorsAction ignore = new IgnoreErrorsAction();
		this.actions.add(ignore);
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
		ignore.setCreateToolbarSeparator(true);
		this.toolbarActions.add(ignore);
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
		this.clearCompletionCache = new ClearCompletionCacheAction();
		this.actions.add(this.clearCompletionCache);
		this.actions.add(showObjectInfoAction);

		this.formatSql = this.editor.getFormatSqlAction();
		this.formatSql.setCreateMenuSeparator(true);
		this.actions.add(this.formatSql);

		a = new CreateSnippetAction(this.editor);
		this.actions.add(a);
		a = new CleanJavaCodeAction(this.editor);
		this.actions.add(a);

		a = new MakeInListAction(this.editor);
		a.setCreateMenuSeparator(true);
		this.actions.add(a);
		this.actions.add(new MakeNonCharInListAction(this.editor));

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

		this.printDataAction.setCreateMenuSeparator(true);
		this.actions.add(this.printDataAction);
		this.actions.add(this.printPreviewAction);
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

	private Runnable selector = new Runnable()
		{
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

	public String getText()
	{
		if (editor == null) return null;
		return editor.getText();
	}

	public String getSelectedText()
	{
		if (editor == null) return null;
		return editor.getSelectedText();
	}

	public JComponent getPanel()
	{
		return this;
	}

	public String getStatementAtCursor()
	{
		ScriptParser parser = createScriptParser();
		parser.setScript(getEditor().getText());
		int index = parser.getCommandIndexAtCursorPos(getEditor().getCaretPosition());
		String currentStatement = parser.getCommand(index);
		return currentStatement;
	}

	public void reformatSql()
	{
		this.storeStatementInHistory();
		this.editor.reformatSql();
		this.selectEditorLater();
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
				public void run()
				{
					WbSwingUtilities.showErrorMessage(SqlPanel.this, ResourceMgr.getString("MsgOutOfMemoryError"));
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

	public void panelSelected()
	{
		selectEditorLater();
	}

	public List getActions()
	{
		return this.actions;
	}

	public void makeReadOnly()
	{
		if (this.currentData == null) return;
		this.currentData.endEdit();
	}

	/**
	 *	Show a message in the log panel. This will also switch
	 *	the display to the log panel (away from the result panel)
	 */
	public void showLogMessage(String aMsg)
	{
		this.showLogPanel();
		setLogText(aMsg);
	}

	/**
	 *	Clear the message log, but do not switch the panel display to it.
	 */
	public void clearLog()
	{
		setLogText("");
	}

	protected void setLogText(final String msg)
	{
		WbSwingUtilities.invoke(new Runnable()
		{
			public void run()
			{
				log.setText(msg);
			}
		});
	}

	/**
	 *	Show the panel with the log messages.
	 */
	public void showLogPanel()
	{
		WbSwingUtilities.invoke(new Runnable()
		{
			public void run()
			{
				int index = resultTab.getTabCount() - 1;
				resultTab.setSelectedIndex(index);
			}
		});
	}

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
			public void run()
			{
				resultTab.setSelectedIndex(index);
			}
		});
	}

	/**
	 *	Display a message in the status bar of the DwPanel.
	 */
	public void showStatusMessage(String aMsg)
	{
		this.statusBar.setStatusMessage(aMsg);
		this.statusBar.forcePaint();
	}

	/**
	 *	Clear the message in the status bar of the DwPanel
	 */
	public void clearStatusMessage()
	{
		this.statusBar.clearStatusMessage();
	}

	public void initStatementHistory()
	{
		int size = Settings.getInstance().getMaxHistorySize();
		this.sqlHistory = new SqlHistory(editor,size);
	}

	public void readFromWorkspace(WbWorkspace w, int index)
		throws IOException
	{
		PanelWorkspaceHandler handler = new PanelWorkspaceHandler(this);
		handler.readFromWorkspace(w, index);
	}

	/**
	 *  Do any work which should be done during the process of saving the
	 *  current workspace, but before the workspace file is actually opened!
	 *  This is to prevent a corrupted workspace due to interrupting the saving
	 *  because of the check for unsaved changes in the current editor file
	 */
	public boolean canCloseTab()
	{
		boolean fileOk = this.checkAndSaveFile();
		if (fileOk && GuiSettings.getConfirmDiscardResultSetChanges())
		{
			if (!isModified()) return true;
			return WbSwingUtilities.getProceedCancel(this, "MsgDiscardTabChanges", getRealTabTitle());
		}
		return fileOk;
	}

	public void saveToWorkspace(WbWorkspace w, int index)
		throws IOException
	{
		PanelWorkspaceHandler handler = new PanelWorkspaceHandler(this);
		handler.saveToWorkspace(w, index);
	}

	public void saveHistory(WbWorkspace w)
		throws IOException
	{
		this.storeStatementInHistory();
		w.addHistoryEntry(this.internalId, this.sqlHistory);
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
	public String getTitle()
	{
		return getTabTitle();
	}

	/**
	 * Returns the title that is shown for the current tab.
	 * This is not necessarily the same as the tabName because
	 * e.g. a filename could be appended to the tabName
	 */
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

	public void setTabTitle(final JTabbedPane tab, final int index)
	{
		String fname = null;
		String tooltip = null;
		this.setId(index + 1);

		fname = this.getCurrentFileName();
		if (fname != null)
		{
			File f = new File(fname);
			tooltip = f.getAbsolutePath();
			iconHandler.showIconForTab(iconHandler.getFileIcon());
		}
		else
		{
			iconHandler.removeIcon();
		}

		PanelTitleSetter.setTabTitle(tab, this, index, getTabTitle());

		tab.setToolTipTextAt(index, tooltip);
	}

	public String getTabName()
	{
		return this.tabName;
	}

	public void setTabName(String aName)
	{
		if (StringUtil.isBlank(aName))
			this.tabName = null;
		else
			this.tabName = aName;
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

	public String toString()
	{
		return this.getTabTitle();
	}

	public void disconnect()
	{
		synchronized (this)
		{
			if (this.dbConnection != null)
			{
				this.setConnection(null);
			}
			this.clearResultTabs();
			this.makeReadOnly();
			setLogText("");
		}
	}

	public WbConnection getConnection()
	{
		return this.dbConnection;
	}

	public boolean isConnected()
	{
		// I'm only checking if the connection is defined, because
		// MainWindow will make sure a valid connection is set
		// for the panel. When using only one connection for all
		// panels, isClosed() will block the entire AWT thread!
		return (this.dbConnection != null);
	}

	public void setConnection(WbConnection aConnection)
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
		}

		if (this.stmtRunner != null)
		{
			this.stmtRunner.setConnection(aConnection);
			this.stmtRunner.setResultLogger(this);
		}

		if (this.connectionInfo != null) this.connectionInfo.setConnection(aConnection);
		this.setExecuteActionStates(aConnection != null);

		if (this.editor != null) this.editor.setDatabaseConnection(this.dbConnection);

		if (this.dbConnection != null)
		{
			this.dbConnection.addChangeListener(this);
		}

		this.checkResultSetActions();
		this.checkCommitAction();
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

	public boolean isRequestFocusEnabled() { return true; }

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
				this.currentData.rowCountChanged();
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
			this.cancelExecution = true;
			this.executionThread.interrupt();
			this.executionThread = null;
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

	public boolean abortExecution()
	{
		if (!this.isBusy()) return true;
		if (this.executionThread == null) return true;

		boolean success = false;
		long wait = Settings.getInstance().getIntProperty(this.getClass().getName() + ".abortwait", 1);
		try
		{
			LogMgr.logDebug("SqlPanel.abortExecution()", "Interrupting SQL Thread...");
			this.executionThread.interrupt();
			this.executionThread.join(wait * 1000);
			if (this.isBusy())
			{
				LogMgr.logDebug("SqlPanel.abortExecution()", "SQL Thread still running after " + wait +"s!");
			}
			else
			{
				success = true;
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("SqlPanel.abortExecution()", "Error when interrupting SQL thread", e);
		}
		return success;
	}

	public boolean confirmCancel()
	{
		return true;
	}

	/**
	 *	Implementation of the Interruptable Interface.
	 */
	public void cancelExecution()
	{
		if (!this.isBusy()) return;

		this.showStatusMessage(ResourceMgr.getString("MsgCancellingStmt") + "\n");
		try
		{
			if (this.worker != null)
			{
				this.worker.cancelExecution();
			}
			else if (this.updateRunning)
			{
				this.cancelUpdate();
			}
			else
			{
				WbThread t = new WbThread("SqlPanel " + this.getId() + " Cancel Thread")
				{
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

	protected void cancelRetrieve()
	{
		try
		{
			iconHandler.showCancelIcon();
			this.cancelExecution = true;
			this.setCancelState(false);
			this.stmtRunner.cancel();
		}
		finally
		{
			iconHandler.showBusyIcon(false);
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
			public void run()
			{
				for (int i=0; i < anActionList.length; i++)
				{
					anActionList[i].setEnabled(aFlag);
				}
			}
		});
	}

	public void runCurrentStatement()
	{
		String sql = this.editor.getText();
		int caret = this.editor.getCaretPosition();
		startExecution(sql, 0, caret, true, this.appendResults);
	}

	public void runSelectedStatement()
	{
		String sql = this.editor.getSelectedStatement();
		int offset = 0;
		boolean highlight = true;
		if (this.editor.isTextSelected())
		{
			offset = this.editor.getSelectionStart();
			highlight = false;
		}
		this.startExecution(sql, offset, -1, highlight, this.appendResults);
	}

	public void commit()
	{
		this.startExecution(SingleVerbCommand.COMMIT.getVerb(), 0, -1, false, this.appendResults);
	}

	public void rollback()
	{
		this.startExecution(SingleVerbCommand.ROLLBACK.getVerb(), 0, -1, false, this.appendResults);
	}

	public void runAll()
	{
		String sql = this.editor.getText();
		this.startExecution(sql, 0, -1, true, this.appendResults);
	}

	private void startExecution(final String sql, final int offset, final int commandAtIndex, final boolean highlight, final boolean appendResult)
	{
		if (this.isBusy()) return;

		if (GuiSettings.getConfirmDiscardResultSetChanges()  && !appendResult && isDataModified())
		{
			if (!WbSwingUtilities.getProceedCancel(this, "MsgDiscardDataChanges")) return;
		}

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

		this.executionThread = new WbThread("SQL Execution Thread " + this.getId())
		{
			public void run()
			{
				runStatement(sql, offset, commandAtIndex, highlight, appendResult);
			}
		};
		this.executionThread.start();
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
		this.showStatusMessage(ResourceMgr.getString("MsgExecutingSql"));

		this.storeStatementInHistory();
		cancelExecution = false;
		setBusy(true);

		// the dbStart should be fired *after* updating the
		// history, as the history might be saved ("AutoSaveHistory") if the MainWindow
		// receives the execStart event
		fireDbExecStart();

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

			// setBusy(false) should be called after dbExecEnd()
			// otherwise the panel would indicate it's not busy, but
			// the connection would still be marked as busy
			this.setBusy(false);
			this.selectEditorLater();
			this.executionThread = null;
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

		this.executionThread = new WbThread("SQL Execution Thread " + this.getId())
		{
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

	public void executeMacroSql(final String sql, final boolean replaceText)
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
		this.startExecution(sql, 0, -1, false, this.appendResults);
	}

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
		exporter.addQueryJob(sql, f);
		dialog.setExporterOptions(exporter);

		this.worker = exporter;

		String msg = ResourceMgr.getString("MsgQueryExportInit");
		msg = StringUtil.replace(msg, "%type%", exporter.getTypeDisplay());
		msg = StringUtil.replace(msg, "%sql%", StringUtil.getMaxSubstring(sql, 100));
		showLogMessage(msg);

		this.executionThread = new WbThread("ExportSQL")
		{
			public void run()
			{
				setBusy(true);
				setCancelState(true);
				fireDbExecStart();
				try
				{
					boolean newLineAppended = false;
					StringBuilder messages = new StringBuilder();
					long start = System.currentTimeMillis();
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
						String msg2 = ResourceMgr.getString("MsgSpoolOk").replace("%rows%", Long.toString(rowCount));
						messages.append("\n");
						messages.append(msg2);
						messages.append("\n");
						msg2 = ResourceMgr.getString("MsgSpoolTarget") + " " + exporter.getFullOutputFilename();
						messages.append(msg2);
						messages.append("\n\n");
					}
					messages.append(ResourceMgr.getString("MsgExecTime") + " " + (((double)execTime) / 1000.0) + "s\n");
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
					clearStatusMessage();
					setCancelState(false);
					executionThread = null;
					worker = null;
				}
			}
		};
		this.executionThread.start();
	}

	public void fatalError(String msg)
	{
		WbSwingUtilities.showErrorMessage(this, msg);
	}

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
		boolean ok = dialog.selectInput(ResourceMgr.getString("TxtWindowTitleSelectImportFile"));
		if (!ok) return;
		DataStoreImporter importer = new DataStoreImporter(currentData.getTable().getDataStore(), currentData.getRowMonitor(), this);
		File importFile = dialog.getSelectedFile();
		importer.setImportOptions(importFile,
			                        dialog.getImportType(),
			                        dialog.getGeneralOptions(),
			                        dialog.getTextOptions(),
			                        dialog.getXmlOptions());

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
		if (showOptions || !v.checkData())
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
			importer.importString(content);
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

	public void appendToLog(final String logMessage)
	{
		WbSwingUtilities.invoke(new Runnable()
		{
			public void run()
			{
				log.append(logMessage);
				log.setCaretPosition(log.getDocument().getLength());
			}
		});
	}

	public String getPassword(String prompt)
	{
		String pwd = WbSwingUtilities.getUserInput(this, ResourceMgr.getString("MsgInputPwdWindowTitle"), "", true);
		if (StringUtil.isEmptyString(pwd)) return null;
		return pwd;
	}

	public boolean confirmExecution(String prompt)
	{
		Window w = SwingUtilities.getWindowAncestor(this);
		return WbSwingUtilities.getYesNo(w, prompt);
	}

	/** Used for storing the result of the confirmExecution() callback */
	private boolean executeAllStatements = true;
	private boolean cancelAll = false;

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

	private boolean ignoreStateChange = false;

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
	public void stateChanged(ChangeEvent evt)
	{
		if (this.ignoreStateChange) return;
		updateResultInfos();
	}

	private void updateResultInfos()
	{
		if (currentData != null)
		{
			this.currentData.removePropertyChangeListener(this);
			this.currentData.getTable().stopEditing();
		}

		int newIndex = this.resultTab.getSelectedIndex();

		if (newIndex == this.resultTab.getTabCount() - 1)
		{
			this.currentData = null;
		}
		else
		{
			this.currentData = (DwPanel)this.resultTab.getSelectedComponent();
			this.currentData.updateStatusBar();
			this.currentData.addPropertyChangeListener("updateTable", this);
		}
		this.closeResultAction.setEnabled(currentData != null);
		updateProxiedActions();
		checkResultSetActions();
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

	public String getSourceQuery()
	{
		if (currentData == null) return null;
		DataStore ds = currentData.getDataStore();
		if (ds != null) return ds.getGeneratingSql();
		return null;
	}

	public void closeCurrentResult()
	{
		int index = resultTab.getSelectedIndex();
		if (index == resultTab.getTabCount() - 1) return;

		try
		{
			DwPanel panel = (DwPanel)resultTab.getComponentAt(index);
			panel.removePropertyChangeListener(SqlPanel.this);
			panel.clearContent();

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

	private void clearResultTabs()
	{
		try
		{
			ignoreStateChange = true;
			WbSwingUtilities.invoke(new Runnable()
			{
				public void run()
				{
					while (resultTab.getTabCount() > 1)
					{
						Component c = resultTab.getComponentAt(0);
						if (c instanceof DwPanel)
						{
							DwPanel panel = (DwPanel)c;
							panel.removePropertyChangeListener(SqlPanel.this);
							panel.clearContent();
						}
						resultTab.removeTabAt(0);
					}
					resultTab.setSelectedIndex(0);
					currentData = null;
					updateProxiedActions();
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
			public void run()
			{
				_updateProxiedActions();
			}
		});
	}

	protected void _updateProxiedActions()
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
			findDataAction.setOriginal(null);
			findDataAgainAction.setOriginal(null);
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
			findDataAction.setOriginal(currentData.getTable().getReplacer().getFindAction());
			findDataAgainAction.setOriginal(currentData.getTable().getReplacer().getFindAgainAction());
			replaceDataAction.setOriginal(currentData.getTable().getReplacer().getReplaceAction());
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
		ScriptParser scriptParser = new ScriptParser();
		DelimiterDefinition altDelim = Settings.getInstance().getAlternateDelimiter(this.dbConnection);

		scriptParser.setAlternateDelimiter(altDelim);
		scriptParser.setCheckEscapedQuotes(Settings.getInstance().getCheckEscapedQuotes());

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
		}
		else
		{
			LogMgr.logError("SqlPanel.createScriptParser", "Created a script parser without a connection!", null);
		}
		return scriptParser;
	}

	private boolean checkPrepared;

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
					goOn = ParameterEditor.showParameterDialog(parms);
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

	private void displayResult(String script, int selectionOffset, int commandAtIndex, boolean highlightOnError, boolean appendResult)
	{
		if (script == null) return;

		boolean logWasCompressed = false;
		boolean jumpToNext = (commandAtIndex > -1 && Settings.getInstance().getAutoJumpNextStatement());
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

		this.checkPrepared = Settings.getInstance().getCheckPreparedStatements();
		this.executeAllStatements = false;
		this.cancelAll = false;

		ScriptParser scriptParser = createScriptParser();

		int oldSelectionStart = -1;
		int oldSelectionEnd = -1;

		this.stmtRunner.setExecutionController(this);
		this.stmtRunner.setParameterPrompter(this);

		// If a file is loaded in the editor, make sure the StatementRunner
		// is using the file's directory as the base directory
		// Thanks to Christian d'Heureuse for this fix!
		if (this.editor.hasFileLoaded())
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

		StatementRunnerResult statementResult = null;

		try
		{
			if (appendResult)
			{
				firstResultIndex = this.resultTab.getTabCount() - 1;
				appendToLog("\n");
			}
			else
			{
				setLogText("");
				this.clearResultTabs();
				firstResultIndex = 0;
			}

			String cleanSql = SqlUtil.makeCleanSql(script, false);
			String macro = MacroManager.getInstance().getMacroText(cleanSql);
			if (macro != null)
			{
				appendToLog(ResourceMgr.getString("MsgExecutingMacro") + ":\n" + cleanSql + "\n");
				script = macro;
				macroRun = true;
			}

			if (this.macroExecution)
			{
				// executeMacro() will set this variable so that we can
				// log the macro statement here. Otherwise we wouldn know at this point
				// that a macro is beeing executed
				this.macroExecution = false;

				macroRun = true;
				appendToLog(ResourceMgr.getString("MsgExecutingMacro") + ":\n" + script + "\n");
			}

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

			if (commandAtIndex > -1)
			{
				count = 1;
				startIndex = scriptParser.getCommandIndexAtCursorPos(commandAtIndex);
				endIndex = startIndex + 1;
				if (startIndex == -1)
				{
					this.appendToLog(ResourceMgr.getString("ErrNoCurrentStatement"));
					this.showLogPanel();
					return;
				}
			}

			if (count > 1) logWasCompressed = !this.stmtRunner.getVerboseLogging();

			StringBuilder finishedMsg1 = new StringBuilder(ResourceMgr.getString("TxtScriptStatementFinished1"));
			finishedMsg1.append(' ');
			StringBuilder finishedMsg2 = new StringBuilder(20);
			finishedMsg2.append(' ');
			String msg = ResourceMgr.getString("TxtScriptStatementFinished2");
			msg = StringUtil.replace(msg, "%total%", NumberStringCache.getNumberString(count));
			finishedMsg2.append(msg);

			final int finishedSize = finishedMsg1.length() + finishedMsg2.length() + 5;

			boolean onErrorAsk = !Settings.getInstance().getIgnoreErrors();

			// Displays the first "result" tab. As no result is available
			// at this point, it merely shows the message log
			this.showResultPanel();

			highlightCurrent = ((count > 1 || commandAtIndex > -1) && (!macroRun) && Settings.getInstance().getHighlightCurrentStatement());

			if (highlightCurrent)
			{
				oldSelectionStart = this.editor.getSelectionStart();
				oldSelectionEnd = this.editor.getSelectionEnd();
				restoreSelection = shouldRestoreSelection;
			}

			long startTime = System.currentTimeMillis();
			statusBar.executionStart();
			long stmtTotal = 0;
			int executedCount = 0;
			String currentSql = null;

			int resultSets = 0;
			this.ignoreStateChange = false;
			this.macroExecution = false;

			long totalRows = 0;

			for (int i=startIndex; i < endIndex; i++)
			{
				currentSql = scriptParser.getCommand(i);
				if (fixNLPattern != null)
				{
					currentSql = fixNLPattern.matcher(currentSql).replaceAll(nl);
				}
				if (currentSql.length() == 0) continue;

				// By calling yield() we make sure that
				// this thread can actually be interrupted!
				Thread.yield();
				if (cancelExecution) break;

				if (highlightCurrent)
				{
					highlightStatement(scriptParser, i, selectionOffset);
				}

				this.stmtRunner.setMaxRows(maxRows);
				this.stmtRunner.setQueryTimeout(timeout);
				this.stmtRunner.runStatement(currentSql);
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
				stmtTotal += statementResult.getExecutionTime();

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
				if (this.cancelAll) break;

				if (statementResult.isSuccess())
				{
					totalRows += statementResult.getTotalUpdateCount();
				}
				else
				{
					commandWithError = i;

					// error messages should always be shown in the log
					// panel, even if compressLog is enabled (if it is not enabled
					// the messages have been appended to the log already)
					if (logWasCompressed) this.appendToLog(statementResult.getMessageBuffer().toString());

					if (count > 1 && onErrorAsk && (i < (count - 1)))
					{
						// the animated gif needs to be turned off when a
						// dialog is displayed, otherwise Swing uses too much CPU
						iconHandler.showBusyIcon(false);

						if (!macroRun) this.highlightError(scriptParser, commandWithError, selectionOffset);

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

			} // end for loop over all statements

			long execTime = (System.currentTimeMillis() - startTime);

			// this will automatically stop the execution timer in the status bar
			statusBar.setExecutionTime(stmtTotal);
			statusBar.clearStatusMessage();

			if (commandWithError > -1 && highlightOnError && !macroRun)
			{
				restoreSelection = false;
				final ScriptParser p = scriptParser;
				final int command = commandWithError;
				final int offset = selectionOffset;

				EventQueue.invokeLater(new Runnable()
				{
					public void run()
					{
						highlightError(p, command, offset);
					}
				});
			}

			if (failuresIgnored > 0)
			{
				this.appendToLog("\n" + failuresIgnored + " " + ResourceMgr.getString("MsgTotalStatementsFailed")+ "\n");
			}

			if (logWasCompressed)
			{
				msg = executedCount + " " + ResourceMgr.getString("MsgTotalStatementsExecuted") + "\n";
				this.appendToLog(msg);
				msg = totalRows + " " + ResourceMgr.getString("MsgTotalRowsAffected") + "\n\n";
				this.appendToLog(msg);
			}

			ignoreStateChange = false;
			if (resultSets > 0)
			{
				this.showResultPanel(firstResultIndex);
			}
			else
			{
				this.showLogPanel();
			}

			if (count > 1)
			{
				this.appendToLog(ResourceMgr.getString("TxtScriptFinished")+ "\n");
				String s = ResourceMgr.getString("MsgScriptExecTime") + " " + (((double)execTime) / 1000.0) + "s\n";
				this.appendToLog(s);
			}

			if (!jumpToNext && restoreSelection && oldSelectionStart > -1 && oldSelectionEnd > -1)
			{
				final int selstart = oldSelectionStart;
				final int selend = oldSelectionEnd;
				EventQueue.invokeLater(new Runnable()
				{
					public void run()
					{
						editor.select(selstart, selend);
					}
				});
			}

			if (highlightCurrent && !restoreSelection)
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
			this.stmtRunner.done();
			ignoreStateChange = false;
		}
	}

	private void showResultMessage(StatementRunnerResult result)
	{
		if (!result.hasMessages()) return;
		try
		{
			if (!MemoryWatcher.isMemoryLow())
			{
				result.appendMessages(this);
				this.appendToLog("\n");
			}
		}
		catch (OutOfMemoryError oome)
		{
			result.clearMessageBuffer();
			clearLog();
			System.gc();
			WbManager.getInstance().setOutOfMemoryOcurred();
			final boolean success = result.isSuccess();
			EventQueue.invokeLater(new Runnable()
			{
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
	}

	private DwPanel createDwPanel()
		throws SQLException
	{
		DwPanel data = new DwPanel(statusBar);
		data.setBorder(WbSwingUtilities.EMPTY_BORDER);
		data.setConnection(this.dbConnection);
		data.setUpdateHandler(this);
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
		return data;
	}

	/**
	 * Implementation of the ResultReceiver interface.
	 * The given sql will be executed and the result will always
	 * be displayed in a new result tab
	 */
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

	private int addResultTab(DwPanel data, String sql)
	{
		int newIndex = this.resultTab.getTabCount() - 1;
		WbTable tbl = data.getTable();
		DataStore ds = (tbl != null ? tbl.getDataStore() : null);
		String resultName = (ds != null ? ds.getResultName() : null);
		if (StringUtil.isBlank(resultName))
		{
			resultName = ResourceMgr.getString("LblTabResult");
		}
		else
		{
			tbl.setPrintHeader(resultName);
		}
		this.resultTab.insertTab(resultName, null, data, sql, newIndex);
		data.setName("dwresult" + newIndex);
		if (this.resultTab.getTabCount() == 2)
		{
			this.resultTab.setSelectedIndex(0);
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
	
	/**
	 * Display the data contained in the StatementRunnerResult.
	 * For each DataStore or ResultSet in the result, an additional
	 * result {@link workbench.gui.sql.DwPanel} will be added.
	 *
	 * @param result the result to be displayed (obtained from a {@link workbench.sql.StatementRunner})
	 * @see workbench.gui.sql.DwPanel
	 */
	public int addResult(StatementRunnerResult result)
		throws SQLException
	{
		if (result == null) return 0;
		if (!result.isSuccess()) return 0;
		final String sql = result.getSourceCommand();

		int count = 0;

		if (result.hasDataStores())
		{
			final List<DataStore> results = result.getDataStores();
			count += results.size();
			WbSwingUtilities.invoke(new Runnable()
			{
				public void run()
				{
					try
					{
						for (DataStore ds : results)
						{
							DwPanel p = createDwPanel();
							p.showData(ds, sql);
							addResultTab(p, sql);
						}
					}
					catch (Exception e)
					{
						LogMgr.logError("SqlPanel.addResult()", "Error when adding new DwPanel with DataStore", e);
					}
				}
			});
		}

		if (result.hasResultSets())
		{
			final List<ResultSet> results = result.getResultSets();
			count += results.size();
			WbSwingUtilities.invoke(new Runnable()
			{
				public void run()
				{
					try
					{
						for (ResultSet rs : results)
						{
							DwPanel p = createDwPanel();
							p.showData(rs, sql);
							addResultTab(p, sql);
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
			public void run()
			{
				editor.scrollTo(line, 0);
				editor.selectStatementTemporary(startPos, endPos);
			}
		});
	}

	protected void highlightError(ScriptParser scriptParser, int commandWithError, int startOffset)
	{
		if (this.editor == null) return;

		final int startPos = scriptParser.getStartPosForCommand(commandWithError) + startOffset;
		final int endPos = scriptParser.getEndPosForCommand(commandWithError) + startOffset;
		final int line = this.editor.getLineOfOffset(startPos);

		WbSwingUtilities.invoke(new Runnable()
		{
			public void run()
			{
				editor.select(0, 0);
				editor.scrollTo(line, 0);
				editor.selectError(startPos, endPos);
			}
		});
	}

	protected void checkResultSetActions()
	{
		final boolean readOnly = (dbConnection == null ? false : dbConnection.getProfile().isReadOnly());
		final boolean hasResult = currentData != null ? currentData.hasResultSet() : false;
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
			public void run()
			{
				importFileAction.setEnabled(mayEdit);
				importClipAction.setEnabled(mayEdit);

				findDataAgainAction.setEnabled(findNext);
				copySelectedMenu.setEnabled(hasResult);
				reloadAction.checkEnabled();
			}
		});
	}

	private void setExecuteActionStates(final boolean flag)
	{
		EventQueue.invokeLater(new Runnable()
		{
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
			}
		});
	}

	public void setBusy(final boolean busy)
	{
		synchronized (this)
		{
			this.threadBusy = busy;
			iconHandler.showBusyIcon(busy);
			this.setExecuteActionStates(!busy);
			this.editor.setEditable(!busy);
		}
	}

	public boolean isBusy() { return this.threadBusy; }


	public void fontChanged(String aFontId, Font newFont)
	{
		if (aFontId.equals(Settings.PROPERTY_MSGLOG_FONT))
		{
			this.log.setFont(newFont);
		}
	}

	public void addDbExecutionListener(DbExecutionListener l)
	{
		if (this.execListener == null) this.execListener = Collections.synchronizedList(new LinkedList<DbExecutionListener>());
		this.execListener.add(l);
	}

	public void removeDbExecutionListener(DbExecutionListener l)
	{
		if (this.execListener == null) return;
		this.execListener.remove(l);
	}

	public void fireDbExecStart()
	{
		synchronized (this)
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
		synchronized (this)
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

	public void reset()
	{
		this.editor.reset();
		this.setLocked(false);
		this.clearLog();
		this.clearResultTabs();
		if (this.currentData != null) this.currentData.clearContent();
		this.currentData = null;
		iconHandler.flush();
		if (this.sqlHistory != null) this.sqlHistory.clear();
	}

	public void dispose()
	{
		Settings.getInstance().removePropertyChangeListener(this);
		Settings.getInstance().removeFontChangedListener(this);

		reset();
		iconHandler.dispose();
		if (this.stmtRunner != null) this.stmtRunner.dispose();
		if (this.execListener != null) execListener.clear();
		if (this.editor != null) this.editor.dispose();
		this.editor = null;
		if (this.actions != null) this.actions.clear();
		if (this.toolbarActions != null) this.toolbarActions.clear();
		if (this.filenameChangeListeners != null) this.filenameChangeListeners.clear();
		if (this.execListener != null) this.execListener.clear();
		if (this.toolbar != null) this.toolbar.removeAll();
		this.toolbar = null;
		this.abortExecution();
		this.executionThread = null;
		this.connectionInfo = null;
	}

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
	}

}
