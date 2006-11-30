/*
 * SqlPanel.java
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.ComponentInputMap;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
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
import workbench.db.WbConnection;
import workbench.db.exporter.DataExporter;
import workbench.db.importer.DataStoreImporter;
import workbench.gui.actions.FilterDataAction;
import workbench.gui.actions.FilterPickerAction;
import workbench.gui.actions.ImportClipboardAction;
import workbench.gui.actions.ResetFilterAction;
import workbench.gui.actions.SelectionFilterAction;
import workbench.gui.actions.ViewMessageLogAction;
import workbench.gui.components.GenericRowMonitor;
import workbench.gui.components.WbTabbedPane;
import workbench.gui.dialogs.dataimport.ImportFileDialog;
import workbench.interfaces.DbExecutionNotifier;
import workbench.interfaces.ParameterPrompter;
import workbench.interfaces.StatementRunner;
import workbench.sql.StatementRunnerResult;
import workbench.util.ExceptionUtil;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.AutoCompletionAction;
import workbench.gui.actions.AutoJumpNextStatement;
import workbench.gui.actions.CheckPreparedStatementsAction;
import workbench.gui.actions.CleanJavaCodeAction;
import workbench.gui.actions.ClearCompletionCacheAction;
import workbench.gui.actions.CommitAction;
import workbench.gui.actions.CopyAsSqlDeleteInsertAction;
import workbench.gui.actions.CopyAsSqlInsertAction;
import workbench.gui.actions.CopyAsSqlUpdateAction;
import workbench.gui.actions.CopyRowAction;
import workbench.gui.actions.CreateDeleteScriptAction;
import workbench.gui.actions.CreateSnippetAction;
import workbench.gui.actions.CopyAsTextAction;
import workbench.gui.actions.DeleteRowAction;
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
import workbench.gui.actions.PrintAction;
import workbench.gui.actions.PrintPreviewAction;
import workbench.gui.actions.RedoAction;
import workbench.gui.actions.RollbackAction;
import workbench.gui.actions.SaveDataAsAction;
import workbench.gui.actions.SelectEditorAction;
import workbench.gui.actions.SelectKeyColumnsAction;
import workbench.gui.actions.SelectMaxRowsAction;
import workbench.gui.actions.SelectResultAction;
import workbench.gui.actions.SpoolDataAction;
import workbench.gui.actions.StopAction;
import workbench.gui.actions.ToggleAutoCommitAction;
import workbench.gui.actions.UndoAction;
import workbench.gui.actions.UndoExpandAction;
import workbench.gui.actions.UpdateDatabaseAction;
import workbench.gui.actions.WbAction;
import workbench.gui.components.ConnectionInfo;
import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.EtchedBorderTop;
import workbench.gui.components.TextComponentMouseListener;
import workbench.gui.components.WbMenu;
import workbench.gui.components.WbScrollPane;
import workbench.gui.components.WbSplitPane;
import workbench.gui.components.WbTable;
import workbench.gui.components.WbToolbar;
import workbench.gui.components.WbToolbarSeparator;
import workbench.gui.dialogs.dataimport.ImportOptions;
import workbench.gui.dialogs.dataimport.TextImportOptions;
import workbench.gui.menu.TextPopup;
import workbench.gui.preparedstatement.ParameterEditor;
import workbench.interfaces.Commitable;
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
import workbench.interfaces.TextChangeListener;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.sql.DelimiterDefinition;
import workbench.sql.MacroManager;
import workbench.sql.ScriptParser;
import workbench.sql.commands.SingleVerbCommand;
import workbench.sql.preparedstatement.PreparedStatementPool;
import workbench.sql.preparedstatement.StatementParameters;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
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
	implements FontChangedListener, ActionListener, TextChangeListener,
		PropertyChangeListener, ChangeListener, 
		MainPanel, Exporter, DbUpdater, Interruptable, FormattableSql, Commitable,
		JobErrorHandler, ExecutionController, ResultLogger, ParameterPrompter, DbExecutionNotifier,
		FilenameChangeListener
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
	private List toolbarActions = new LinkedList();

	private List filenameChangeListeners;

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
	protected SelectKeyColumnsAction selectKeys;
	protected FilterDataAction filterAction;
	protected SelectionFilterAction selectionFilterAction;
	protected FilterPickerAction filterPicker;
	protected ResetFilterAction resetFilterAction;
	protected OptimizeAllColumnsAction optimizeAllCol;
	
	protected CheckPreparedStatementsAction checkPreparedAction;
	protected ClearCompletionCacheAction clearCompletionCache;
	protected AutoCompletionAction autoCompletion;

	protected WbMenu copySelectedMenu;
	protected ToggleAutoCommitAction toggleAutoCommit;
	protected CommitAction commitAction;
	protected RollbackAction rollbackAction;

	protected FormatSqlAction formatSql;
	protected SpoolDataAction spoolData;

	protected FileDiscardAction fileDiscardAction;
	protected FindDataAction findDataAction;
	protected FindDataAgainAction findDataAgainAction;
	protected WbToolbar toolbar;
	protected ConnectionInfo connectionInfo;

	protected WbConnection dbConnection;
	protected boolean importRunning;
	protected boolean updateRunning;
	protected boolean textModified;
	protected String tabName;

	private List execListener;
	protected Thread executionThread;
	protected Interruptable worker;

	private static final Border statusBarBorder = new CompoundBorder(new EmptyBorder(2, 1, 0, 1), new EtchedBorder());
	private static final Border logBorder = new EmptyBorder(0,2,0,0);

	protected DwStatusBar statusBar;
	protected StatementRunner stmtRunner;
	protected GenericRowMonitor rowMonitor;
//</editor-fold>
	
	public SqlPanel(int anId)
	{
		this.setId(anId);
		this.setDoubleBuffered(true);
		this.setBorder(null);
		this.setLayout(new BorderLayout());

		editor = EditorPanel.createSqlEditor();
		statusBar = new DwStatusBar(true, true);
		statusBar.setBorder(statusBarBorder);
		editor.setStatusBar(statusBar);
		editor.setBorder(new EtchedBorderTop());
		
		log = new JTextArea();
		log.putClientProperty("JTextArea.infoBackground", Boolean.TRUE);
		log.setBorder(logBorder);
		log.setFont(Settings.getInstance().getMsgLogFont());
		log.setEditable(false);
		log.setLineWrap(true);
		log.setWrapStyleWord(true);
		log.addMouseListener(new TextComponentMouseListener());
		
		this.resultTab = new WbTabbedPane();
		this.resultTab.setTabPlacement(JTabbedPane.TOP);
		this.resultTab.setDoubleBuffered(true);
		this.resultTab.setFocusable(false);

		this.resultTab.addChangeListener(this);
		JScrollPane scroll = new WbScrollPane(log);
		this.resultTab.addTab(ResourceMgr.getString("LblTabMessages"), scroll);

		this.editor.addFilenameChangeListener(this);
		this.contentPanel = new WbSplitPane(JSplitPane.VERTICAL_SPLIT, true, this.editor, this.resultTab);
		this.contentPanel.setOneTouchExpandable(true);
		this.contentPanel.setContinuousLayout(true);

		this.add(this.contentPanel, BorderLayout.CENTER);
		this.add(statusBar, BorderLayout.SOUTH);

		this.initStatementHistory();
		
		this.initActions();
		this.initToolbar();
		this.setupActionMap();

		Settings s = Settings.getInstance();
		s.addFontChangedListener(this);
		s.addPropertyChangeListener(this);
		
		this.makeReadOnly();
		this.checkResultSetActions();

		this.editor.addTextChangeListener(this);
		this.rowMonitor = new GenericRowMonitor(this.statusBar);
	}

	public String getId()
	{
		return Integer.toString(this.internalId);
	}

	public void setId(int anId)
	{
		this.internalId = anId;
	}

	public void initDivider()
	{
		int myHeight = (int)this.getPreferredSize().getHeight();
		initDivider(myHeight);
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
		this.toolbar = new WbToolbar();
		this.toolbar.addDefaultBorder();
		Iterator itr = this.toolbarActions.iterator();
		while (itr.hasNext())
		{
			WbAction a = (WbAction)itr.next();
			boolean toolbarSep = a.getCreateToolbarSeparator();
			if (toolbarSep)
			{
				toolbar.addSeparator();
			}
			a.addToToolbar(toolbar);
		}
		toolbar.addSeparator();
		this.connectionInfo = new ConnectionInfo(this.toolbar.getBackground());
		toolbar.add(this.connectionInfo);
	}

	public void addToToolbar(WbAction anAction, boolean withSeperator)
	{
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
			this.removeIconFromTab();
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
				this.showFileIcon();
			}
			else
			{
				this.removeIconFromTab();
			}
			updateTabTitle();
		}
	}
	
	private void fireFilenameChanged(String aNewName)
	{
		updateTabTitle();
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
		
		this.fileDiscardAction = new FileDiscardAction(this);
		this.actions.add(this.fileDiscardAction);
		this.actions.add(this.editor.getReloadAction());

		this.actions.add(new UndoAction(this.editor));
		this.actions.add(new RedoAction(this.editor));

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

		makeLower.setCreateMenuSeparator(true);
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
		this.duplicateRow = new CopyRowAction(null);
		this.selectKeys = new SelectKeyColumnsAction(null);

		this.actions.add(this.selectKeys);
		this.actions.add(this.updateAction);
		this.actions.add(this.insertRow);
		this.actions.add(this.duplicateRow);
		this.actions.add(this.deleteRow);
		
		this.createDeleteScript = new CreateDeleteScriptAction(null);
		this.actions.add(this.createDeleteScript);

		this.exportDataAction = new SaveDataAsAction(null);
		this.exportDataAction.setCreateMenuSeparator(true);
		this.exportDataAction.setEnabled(false);

		SelectEditorAction sea = new SelectEditorAction(this);
		sea.setCreateMenuSeparator(true);
		this.actions.add(sea);
		this.actions.add(new SelectResultAction(this));
		this.actions.add(new SelectMaxRowsAction(this.statusBar));
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
		//this.toolbarActions.add(this.startEdit);
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

		this.findDataAction = new FindDataAction(null); 
		this.findDataAction.setMenuTextByKey("MnuTxtFindData");
		this.findDataAction.setEnabled(false);
		this.findDataAction.setCreateMenuSeparator(true);
		this.findDataAgainAction = new FindDataAgainAction(null);
		this.findDataAgainAction.setMenuTextByKey("MnuTxtFindDataAgain");
		this.findDataAgainAction.setEnabled(false);

		this.autoCompletion = new AutoCompletionAction(this.editor, this.statusBar);
		this.autoCompletion.setCreateMenuSeparator(true);
		this.actions.add(this.autoCompletion);
		this.clearCompletionCache = new ClearCompletionCacheAction();
		this.actions.add(this.clearCompletionCache);
		
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
		this.actions.add(this.findDataAction);
		this.actions.add(this.findDataAgainAction);
		this.actions.add(filterAction);
		this.actions.add(selectionFilterAction);
		this.actions.add(this.resetFilterAction );
		
		this.printDataAction.setCreateMenuSeparator(true);
		this.actions.add(this.printDataAction);
		this.actions.add(this.printPreviewAction);

		this.setExecuteActionStates(false);
	}

	public void setVisible(boolean flag)
	{
		super.setVisible(flag);
		if (!flag)
		{
			this.autoCompletion.closePopup();
		}
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
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				selectEditor();
			}
		});
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

	public void selectEditor()
	{
		// make sure the panel and its window are really visible
		// before putting the focus to the editor
		Window w = SwingUtilities.getWindowAncestor(this);
		if (w == null) return;

		if (w.isActive() && w.isVisible() && w.isFocused() && this.isVisible() && this.isCurrentTab() && editor != null)
		{
			editor.requestFocusInWindow();
		}
	}

	public void selectResult()
	{
		if (this.isVisible() && this.isCurrentTab() && currentData != null)
		{
			showResultPanel();
			currentData.getTable().requestFocusInWindow();
		}
	}

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

	protected void updateDb()
	{
		try
		{
			fireDbExecStart();
			this.updateRunning = true;
			this.log.setText(ResourceMgr.getString("MsgUpdatingDatabase"));
			this.log.append("\n");
			this.currentData.saveChanges(this.dbConnection, this);
		}
		catch (OutOfMemoryError mem)
		{
			this.log.setText(ExceptionUtil.getDisplay(mem));
			showBusyIcon(false);
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
		this.log.append(this.currentData.getLastMessage());
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
		this.log.setText(aMsg);
	}

	/**
	 *	Clear the message log, but do not switch the panel display to it.
	 */
	public void clearLog()
	{
		this.log.setText("");
	}

	/**
	 *	Show the panel with the log messages.
	 */
	public void showLogPanel()
	{
		int index = this.resultTab.getTabCount() - 1;
		this.resultTab.setSelectedIndex(index);
	}

	/**
	 *	Show the panel with the result set
	 */
	public void showResultPanel()
	{
		this.resultTab.setSelectedIndex(0);
	}

	/**
	 *	Display a message in the status bar of the DwPanel.
	 */
	public void showStatusMessage(String aMsg)
	{
		this.statusBar.setStatusMessage(aMsg);
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
		if (this.hasFileLoaded())
		{
			this.closeFile(true, false);
		}
		else
		{
			this.editor.setText("");
		}
		

		try
		{
			w.readHistoryData(index, this.sqlHistory);
		}
		catch (Exception e)
		{
			LogMgr.logWarning("SqlPanel.readFromWorkspace()", "Could not read history data for index " + (this.internalId - 1));
			this.clearSqlHistory(false);
		}

		String filename = w.getExternalFileName(index);
		this.tabName = w.getTabTitle(index);
		if (this.tabName != null && this.tabName.length() == 0)
		{
			this.tabName = null;
		}

		int v = w.getMaxRows(index);
		this.statusBar.setMaxRows(v);
		v = w.getQueryTimeout(index);
		this.statusBar.setQueryTimeout(v);

		boolean fileLoaded = false;
		if (filename != null)
		{
			String encoding = w.getExternalFileEncoding(index);
			fileLoaded = this.readFile(filename, encoding);
		}

		if (!fileLoaded)
		{
			try
			{
				this.sqlHistory.showCurrent();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			int cursorPos = w.getExternalFileCursorPos(index);
			if (cursorPos > -1 && cursorPos < this.editor.getText().length()) this.editor.setCaretPosition(cursorPos);
		}

		Properties props = w.getSettings();
		try
		{
			int loc = Integer.parseInt(props.getProperty("tab" + (index) + ".divider.location", "0"));
			if (loc <= 0) loc = 200;
			this.contentPanel.setDividerLocation(loc);
			loc = Integer.parseInt(props.getProperty("tab" + (index) + ".divider.lastlocation", "0"));
			if (loc > 0) this.contentPanel.setLastDividerLocation(loc);
		}
		catch (Exception e)
		{
			LogMgr.logWarning("SqlPanel.restoreSettings()", "Error when restore settings", e);
		}
		this.updateTabTitle();
		this.editor.clearUndoBuffer();
		this.editor.resetModified();
	}

	/** Do any work which should be done during the process of saving the
	 *  current workspace, but before the workspace file is actually opened!
	 *  This is to ensure a corrupted workspace due to interrupting the saving
	 *  because of the check for unsaved changes in the current editor file
	 */
	public boolean prepareWorkspaceSaving()
	{
		return this.checkAndSaveFile();
	}

	public void saveToWorkspace(WbWorkspace w, int index)
		throws IOException
	{
		this.saveHistory(w);
		Properties props = w.getSettings();
		
		int location = this.contentPanel.getDividerLocation();
		int last = this.contentPanel.getLastDividerLocation();
		props.setProperty("tab" + (index) + ".divider.location", Integer.toString(location));
		props.setProperty("tab" + (index) + ".divider.lastlocation", Integer.toString(last));
		
		w.setMaxRows(index, this.statusBar.getMaxRows());
		w.setQueryTimeout(index, this.statusBar.getQueryTimeout());
		if (this.hasFileLoaded())
		{
			w.setExternalFileName(index, this.getCurrentFileName());
			w.setExternalFileCursorPos(index, this.editor.getCaretPosition());
			w.setExternalFileEncoding(index, this.editor.getCurrentFileEncoding());
		}
		if (this.tabName != null)
		{
			w.setTabTitle(index, this.tabName);
		}
	}

	public void saveHistory(WbWorkspace w)
		throws IOException
	{
		this.storeStatementInHistory();
		w.addHistoryEntry("WbStatements" + this.internalId + ".txt", this.sqlHistory);
	}

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

	
	private void updateTabTitle()
	{
		Container parent = this.getParent();
		if (parent instanceof JTabbedPane)
		{
			JTabbedPane tab = (JTabbedPane)parent;
			int index = tab.indexOfComponent(this);
			setTabTitle(tab, index);
		}
	}
	
	public void setTabTitle(JTabbedPane tab, int index)
	{
		String fname = null;
		String tooltip = null;
		this.setId(index + 1);

		fname = this.getCurrentFileName();
		if (fname != null)
		{
			File f = new File(fname);
			tooltip = f.getAbsolutePath();
			this.showIconForTab(getFileIcon());
		}
		else
		{
			this.removeIconFromTab();
		}
		
		String tabTitle = getTabTitle();
		String title = tabTitle + " " + Integer.toString(index+1);
		tab.setTitleAt(index, title);
		if (index < 9)
		{
			char c = Integer.toString(index+1).charAt(0);
			int pos = tabTitle.length() + 1;
			tab.setMnemonicAt(index, c);
			// The Mnemonic index has to be set explicitely otherwise
			// the display would be wrong if the tab title contains
			// the mnemonic character
			tab.setDisplayedMnemonicIndexAt(index, pos);
		}
		tab.setToolTipTextAt(index, tooltip);
	}

	public String getTabName()
	{
		return this.tabName;
	}

	public void setTabName(String aName)
	{
		if (StringUtil.isEmptyString(aName))
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

	public void setStatementText(String aStatement)
	{
		this.storeStatementInHistory();
		if (this.editor.getCurrentFile() != null) this.editor.saveCurrentFile();
		this.editor.closeFile(true);
		this.editor.setText(aStatement);
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
			this.log.setText("");
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
		synchronized (this)
		{
			if (this.dbConnection != null)
			{
				this.dbConnection.removeChangeListener(this);
				this.removeDbExecutionListener(this.dbConnection);
			}

			this.dbConnection = aConnection;
			this.toggleAutoCommit.setConnection(this.dbConnection);

			if (this.clearCompletionCache != null) this.clearCompletionCache.setConnection(this.dbConnection);
			if (this.autoCompletion != null) this.autoCompletion.setConnection(this.dbConnection);

			if (this.stmtRunner == null && aConnection != null)
			{
				try
				{
					// Use reflection to create instance to avoid class loading upon startup
					this.stmtRunner = (StatementRunner)Class.forName("workbench.sql.DefaultStatementRunner").newInstance();
				}
				catch (Exception e)
				{
					LogMgr.logError("SqlPanel.setConnection()", "Error creating batch runner", e);
				}
				this.stmtRunner.setRowMonitor(this.rowMonitor);
			}
			if (this.stmtRunner != null)
			{
				this.stmtRunner.setConnection(aConnection);
				this.stmtRunner.setResultLogger(this);
			}

			boolean enable = (aConnection != null);
			if (this.connectionInfo != null) this.connectionInfo.setConnection(aConnection);
			this.setExecuteActionStates(enable);

			if (aConnection != null)
			{
				if (this.editor != null) this.editor.setDatabaseConnection(aConnection);
			}

			if (this.dbConnection != null)
			{
				this.dbConnection.addChangeListener(this);
				this.addDbExecutionListener(this.dbConnection);
			}
		}
		
		this.checkResultSetActions();
		this.checkAutocommit();
	}

	/**
	 * Check the autoCommit property of the current connection
	 * and enable/disable the rollback and commit actions
	 * accordingly
	 */
	protected void checkAutocommit()
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
				int commit = WbSwingUtilities.getCommitRollbackQuestion(this, msg);
				{
					try
					{
						if (commit == WbSwingUtilities.DO_COMMIT)
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
				}
				this.currentData.rowCountChanged();
				this.repaint();
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
		int wait = Settings.getInstance().getIntProperty(this.getClass().getName() + ".abortwait", 1);
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
				WbThread t = new WbThread("Cancel Thread")
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
		this.showCancelIcon();
		this.cancelExecution = true;
		this.setCancelState(false);
		this.stmtRunner.cancel();
	}

	public void setCancelState(final boolean aFlag)
	{
		this.stopAction.setEnabled(aFlag);
	}

	/**
	 *	Modify the enabled state of the given action.
	 */
	public void setActionState(final Action anAction, final boolean aFlag)
	{
		anAction.setEnabled(aFlag);
	}

	public void setActionState(final Action[] anActionList, final boolean aFlag)
	{
		for (int i=0; i < anActionList.length; i++)
		{
			anActionList[i].setEnabled(aFlag);
		}
	}

	private String getStatementAtCursor()
	{
		ScriptParser parser = createScriptParser();
		parser.setScript(this.editor.getText());
		int index = parser.getCommandIndexAtCursorPos(this.editor.getCaretPosition());
		String currentStatement = parser.getCommand(index);
		return currentStatement;
	}
	
	public void runCurrentStatement()
	{
		String sql = this.editor.getText();
		int caret = this.editor.getCaretPosition();
		startExecution(sql, 0, caret, true);
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
		this.startExecution(sql, offset, -1, highlight);
	}

	public void commit()
	{
		this.startExecution(SingleVerbCommand.COMMIT.getVerb(), 0, -1, false);
	}

	public void rollback()
	{
		this.startExecution(SingleVerbCommand.ROLLBACK.getVerb(), 0, -1, false);
	}

	public void runAll()
	{
		String sql = this.editor.getText();
		this.startExecution(sql, 0, -1, true);
	}

	private void startExecution(final String sql, final int offset, final int commandAtIndex, final boolean highlight)
	{
		if (this.isBusy()) return;
		if (!this.isConnected()) return;
		if (this.dbConnection.isBusy())
		{
			showLogMessage(ResourceMgr.getString("ErrConnectionBusy"));
			return;
		}
		this.executionThread = new WbThread("SQL Execution Thread " + this.getId())
		{
			public void run()
			{
				runStatement(sql, offset, commandAtIndex, highlight);
			}
		};
		this.executionThread.setPriority(Thread.NORM_PRIORITY+2);
		this.executionThread.start();
	}

	/*
	 * 	Execute the given SQL string. This is invoked from the the run() and other
	 *  methods in order to execute the SQL command. It takes care of updating the
	 *  actions and the menu.
	 *  The actual execution and display of the result is handled by displayResult()
	 */
	private void runStatement(String sql, int selectionOffset, int commandAtIndex, boolean highlightOnError)
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
		makeReadOnly();
		
		try
		{
			this.displayResult(sql, selectionOffset, commandAtIndex, highlightOnError);
		}
		finally
		{
			this.setBusy(false);
			clearStatusMessage();
			setCancelState(false);
			updateResultInfos();
			this.fireDbExecEnd();
			this.selectEditorLater();
			this.executionThread = null;
		}
	}

	private boolean macroExecution = false;
	
	public void executeMacro(final String macroName, final boolean replaceText)
	{
		if (isBusy()) return;

		MacroManager mgr = MacroManager.getInstance();
		String sql = mgr.getMacroText(macroName);
		if (sql == null || sql.trim().length() == 0) return;

		if (mgr.hasSelectedKey(sql))
		{
			String selected = this.editor.getSelectedText();
			if (selected == null)
			{
				WbSwingUtilities.showErrorMessage(this, ResourceMgr.getString("ErrNoSelection4Macro"));
				return;
			}
			sql = mgr.replaceSelected(sql, selected);
		}
		
		if (mgr.hasCurrentKey(sql))
		{
			String current = getStatementAtCursor();
			if (current == null)
			{
				WbSwingUtilities.showErrorMessage(this, ResourceMgr.getString("ErrNoCurrent4Macro"));
				return;
			}
			sql = mgr.replaceCurrent(sql, current);
		}
		
		if (mgr.hasTextKey(sql))
		{
			sql = mgr.replaceEditorText(sql, editor.getText());
		}
		
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
		this.startExecution(sql, 0, -1, false);
	}

	public void exportData()
	{
		final String sql = SqlUtil.makeCleanSql(this.editor.getSelectedStatement(),false);

		this.cancelExecution = false;

		final DataExporter exporter = new DataExporter(this.dbConnection);
		exporter.setRowMonitor(this.rowMonitor);
		exporter.setSql(sql);
		this.worker = exporter;
		
		boolean selected = exporter.selectOutput(getParentWindow());
		if (selected)
		{
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
						StringBuffer messages = new StringBuffer();
						long start = System.currentTimeMillis();
						long rowCount = exporter.startExport();
						long execTime = (System.currentTimeMillis() - start);
						String[] spoolMsg = exporter.getErrors();
						if (spoolMsg.length > 0)
						{
							messages.append('\n');
							newLineAppended = true;
							for (int i=0; i < spoolMsg.length; i++)
							{
								messages.append(spoolMsg[i]);
								messages.append('\n');
							}
						}

						//String warn = ResourceMgr.getString("TxtWarning");
						spoolMsg = exporter.getWarnings();
						if (spoolMsg.length > 0)
						{
							if (!newLineAppended) messages.append('\n');
							for (int i=0; i < spoolMsg.length; i++)
							{
								messages.append(spoolMsg[i]);
								messages.append('\n');
							}
						}
						if (exporter.isSuccess())
						{
							String msg2 = ResourceMgr.getString("MsgSpoolOk").replaceAll("%rows%", Long.toString(rowCount));
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
						fireDbExecEnd();
						setBusy(false);
						clearStatusMessage();
						setCancelState(false);
						executionThread = null;
						worker = null;
					}
				}
			};
			this.executionThread.start();
		}
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
		this.showBusyIcon(false);
		int choice = this.currentData.getActionOnError(errorRow, errorColumn, dataLine, errorMessage);
		this.showBusyIcon(true);
		return choice;
	}

	public int getImportErrorAction(int errorRow, String errorColumn, String dataLine, String errorMessage)
	{
		String msg = null;
		if (errorColumn != null)
		{
			msg = ResourceMgr.getString("ErrColumnImportError");
			msg = msg.replaceAll("%row%", Integer.toString(errorRow));
			msg = msg.replaceAll("%column%", errorColumn);
			msg = msg.replaceAll("%data%", dataLine);
		}
		else
		{
			msg = ResourceMgr.getString("ErrRowImportError");
			msg = msg.replaceAll("%row%", Integer.toString(errorRow));
			msg = msg.replaceAll("%data%", dataLine == null ? "(null)" : dataLine.substring(0,40) + " ...");
		}

		this.showBusyIcon(false);
		int choice = WbSwingUtilities.getYesNoIgnoreAll(this, msg);
		int result = JobErrorHandler.JOB_ABORT;
		this.showBusyIcon(true);
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
	
	protected synchronized void runImporter(final DataStoreImporter importer)
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
					String msg = importer.getMessage();
					if (!StringUtil.isEmptyString(msg)) appendToLog(msg);
					setCancelState(false);
					checkResultSetActions();
				}
			}
		};
		importThread.start();
		this.selectEditor();
	}

	public void appendToLog(final String aString)
	{
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				log.append(aString);
				log.setCaretPosition(log.getDocument().getLength());
			}
		});
	}

	/** Used for storing the result of the confirmExecution() callback */
	private boolean executeAllStatements = true;
	private boolean cancelAll = false;
	
	public boolean confirmExecution(String command)
	{
		if (executeAllStatements) return true;
		boolean result = false;
		this.showBusyIcon(false);
		try
		{
			String msg = ResourceMgr.getString("MsgConfirmExecution") + "\n" + StringUtil.leftString(command, 60, true);
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
			this.showBusyIcon(true);
		}
		return result;
	}

	private boolean ignoreStateChange = false;
	
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
		// If the log panel is selected, do nothing
		if (newIndex == this.resultTab.getTabCount() - 1) return;
		this.currentData = (DwPanel)this.resultTab.getSelectedComponent();
		this.currentData.updateStatusBar();
		this.currentData.addPropertyChangeListener("updateTable", this);
		updateProxiedActions();
		checkResultSetActions();
		
	}
	private void clearResultTabs()
	{
		try
		{
			ignoreStateChange = true;
			while (resultTab.getTabCount() > 1)
			{
				Component c = resultTab.getComponentAt(0);
				if (c instanceof DwPanel)
				{
					DwPanel panel = (DwPanel)c;
					panel.removePropertyChangeListener(SqlPanel.this);
					panel.clearContent();
				}
				resultTab.remove(0);
			}
			resultTab.setSelectedIndex(0);
			currentData = null; 
			updateProxiedActions();
			checkResultSetActions();
		}
		finally
		{
			ignoreStateChange = false;
		}
	}
	
	private void updateProxiedActions()
	{
		if (this.currentData == null) 
		{
			this.updateAction.setOriginal(null);
			this.insertRow.setOriginal(null);
			this.deleteRow.setOriginal(null);
			//this.startEdit.setOriginal(null);
			this.duplicateRow.setOriginal(null);
			this.selectKeys.setOriginal(null);
			this.createDeleteScript.setClient(null);
			this.exportDataAction.setOriginal(null);
			this.optimizeAllCol.setClient(null);
			this.dataToClipboard.setOriginal(null);
			this.copyAsSqlInsert.setOriginal(null);	
			this.copyAsSqlUpdate.setOriginal(null);
			this.copyAsSqlDeleteInsert.setOriginal(null);
			this.findDataAction.setOriginal(null);
			this.findDataAgainAction.setOriginal(null);
			copySelectedMenu.removeAll();
			copySelectedMenu.setEnabled(false);
			this.printDataAction.setOriginal(null);
			this.printPreviewAction.setOriginal(null);
			this.filterAction.setOriginal(null);
			this.filterPicker.setClient(null);
			this.resetFilterAction.setOriginal(null);
			this.selectionFilterAction.setClient(null);
		}
		else
		{
			this.updateAction.setOriginal(this.currentData.getUpdateDatabaseAction());
			this.insertRow.setOriginal(this.currentData.getInsertRowAction());
			this.deleteRow.setOriginal(this.currentData.getDeleteRowAction());
			//this.startEdit.setOriginal(this.currentData.getStartEditAction());
			this.duplicateRow.setOriginal(this.currentData.getCopyRowAction());
			this.selectKeys.setOriginal(this.currentData.getSelectKeysAction());
			this.createDeleteScript.setClient(this.currentData.getTable());
			this.exportDataAction.setOriginal(this.currentData.getTable().getExportAction());
			this.optimizeAllCol.setClient(this.currentData.getTable());
			this.dataToClipboard.setOriginal(this.currentData.getTable().getDataToClipboardAction());
			this.copyAsSqlInsert.setOriginal(this.currentData.getTable().getCopyAsInsertAction());	
			this.copyAsSqlUpdate.setOriginal(this.currentData.getTable().getCopyAsUpdateAction());
			this.copyAsSqlDeleteInsert.setOriginal(this.currentData.getTable().getCopyAsDeleteInsertAction());
			this.findDataAction.setOriginal(this.currentData.getTable().getFindAction());
			this.findDataAgainAction.setOriginal(this.currentData.getTable().getFindAgainAction());
			copySelectedMenu.removeAll();
			this.currentData.getTable().populateCopySelectedMenu(copySelectedMenu);
			copySelectedMenu.setEnabled(true);
			this.printDataAction.setOriginal(this.currentData.getTable().getPrintAction());
			this.printPreviewAction.setOriginal(this.currentData.getTable().getPrintPreviewAction());
			this.filterAction.setOriginal(this.currentData.getTable().getFilterAction());
			this.filterPicker.setClient(this.currentData.getTable());
			this.resetFilterAction.setOriginal(this.currentData.getTable().getResetFilterAction());
			this.selectionFilterAction.setClient(this.currentData.getTable());
		}
	}

	private void addResultTab(DwPanel data, String sql)
	{
		int newIndex = this.resultTab.getTabCount() - 1;
		this.resultTab.insertTab(ResourceMgr.getString("LblTabResult"), null, data, sql, newIndex);
		if (this.resultTab.getTabCount() == 2)
		{
			this.resultTab.setSelectedIndex(0);
		}
	}

	private ScriptParser createScriptParser()
	{
		ScriptParser scriptParser = new ScriptParser();
		DelimiterDefinition altDelim = null;
		if (this.dbConnection.getProfile() != null)
		{
			altDelim = this.dbConnection.getProfile().getAlternateDelimiter();
		}
		if (altDelim == null) altDelim = Settings.getInstance().getAlternateDelimiter();
		scriptParser.setAlternateDelimiter(altDelim);
		scriptParser.setCheckEscapedQuotes(Settings.getInstance().getCheckEscapedQuotes());
		scriptParser.setSupportOracleInclude(this.dbConnection.getMetadata().supportShortInclude());
		scriptParser.setCheckForSingleLineCommands(this.dbConnection.getMetadata().supportSingleLineCommands());
		scriptParser.setCheckHashComments(this.dbConnection.getMetadata().isMySql());
		return scriptParser;
	}

	private VariablePrompter prompter;
	private boolean checkPrepared;
	
	public boolean processParameterPrompts(String sql)
	{
		boolean goOn = true;
		
		if (prompter == null) prompter = new VariablePrompter();
		prompter.setSql(sql);
		if (prompter.needsInput())
		{
			// the animated gif needs to be turned off when a
			// dialog is displayed, otherwise Swing uses too much CPU
			this.showBusyIcon(false);
			goOn = prompter.getPromptValues();
			this.showBusyIcon(true);
		}

		if (goOn && this.checkPrepared)
		{
			PreparedStatementPool pool = this.dbConnection.getPreparedStatementPool();
			try
			{
				if (pool.isRegistered(sql) || pool.addPreparedStatement(sql))
				{
					StatementParameters parms = pool.getParameters(sql);
					this.showBusyIcon(false);
					goOn = ParameterEditor.showParameterDialog(parms);
					this.showBusyIcon(true);
				}
			}
			catch (SQLException e)
			{
					this.showBusyIcon(false);
					String msg = ResourceMgr.getString("ErrCheckPreparedStatement");
					msg = StringUtil.replace(msg, "%error%", ExceptionUtil.getDisplay(e));
					WbSwingUtilities.showErrorMessage(this, msg);
					this.showBusyIcon(true);
					
					// Ignore errors in prepared statements...
					goOn = true;
					
					// Disable checking as the current driver does not seem to support it
					Settings.getInstance().setCheckPreparedStatements(false);
			}
		}
		return goOn;
		
	}
	
	private void displayResult(String script, int selectionOffset, int commandAtIndex, boolean highlightOnError)
	{
		if (script == null) return;

		boolean logWasCompressed = false;
		boolean jumpToNext = (commandAtIndex > -1 && Settings.getInstance().getAutoJumpNextStatement());
		boolean highlightCurrent = false;
		boolean restoreSelection = false;
		boolean shouldRestoreSelection = Settings.getInstance().getBoolProperty("workbench.gui.sql.restoreselection", true);
		boolean macroRun = false;
		this.checkPrepared = Settings.getInstance().getCheckPreparedStatements();
		this.executeAllStatements = false;
		this.cancelAll = false;
		
		ScriptParser scriptParser = createScriptParser();
		
		int oldSelectionStart = -1;
		int oldSelectionEnd = -1;

		if (this.dbConnection.getProfile().isConfirmUpdates())
		{
			this.stmtRunner.setExecutionController(this);
		}
		else
		{
			this.stmtRunner.setExecutionController(null);
		}
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
		
		StatementRunnerResult statementResult = null;
		
		try
		{
			this.log.setText("");
			
			this.clearResultTabs();

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
				// executeMacro will set this variable for logging purposes only
				// the same SQL is actually passed into this method
				macroRun = true;
				appendToLog(ResourceMgr.getString("MsgExecutingMacro") + ":\n" + script + "\n");
				macroExecution = false;
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
			
			StringBuffer finishedMsg1 = new StringBuffer(ResourceMgr.getString("TxtScriptStatementFinished1"));
			finishedMsg1.append(' ');
			StringBuffer finishedMsg2 = new StringBuffer(20);
			finishedMsg2.append(' ');
			String msg = ResourceMgr.getString("TxtScriptStatementFinished2");
			msg = StringUtil.replace(msg, "%total%", Integer.toString(count));
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
			
			for (int i=startIndex; i < endIndex; i++)
			{
				currentSql = scriptParser.getCommand(i);

				// By calling yield() we make sure that
				// this thread can actually be interrupted!
				Thread.yield();
				if (cancelExecution) break;

				if (highlightCurrent)
				{
					highlightStatement(scriptParser, i, selectionOffset);
					//editor.validate();
					Thread.yield();
				}
				
				this.stmtRunner.runStatement(currentSql, maxRows, timeout);
				statementResult = this.stmtRunner.getResult();	
				if (statementResult.promptingWasCancelled())
				{
					String cancelMsg = ResourceMgr.getString("MsgSqlCancelledDuringPrompt");
					cancelMsg = cancelMsg.replaceAll("%nr%", Integer.toString(i+1));
					this.appendToLog(cancelMsg);
					this.showLogPanel();
					continue;
				}

				resultSets += this.showResult(statementResult);
				stmtTotal += statementResult.getExecutionTime();

				// the SET FEEDBACK command might change the feedback level
				// so it needs to be checked each time.
				if (count > 1) logWasCompressed = logWasCompressed || !this.stmtRunner.getVerboseLogging();
				
				StringBuffer finishedMsg = new StringBuffer(finishedSize);
				finishedMsg.append(finishedMsg1);
				finishedMsg.append(i + 1);
				finishedMsg.append(finishedMsg2);
				String currentMsg = finishedMsg.toString();

				if (!logWasCompressed)
				{
					showResultMessage(statementResult);
					StringBuffer logmsg = new StringBuffer(100);
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

				if (count > 1)
				{
					this.showStatusMessage(currentMsg);
				}

				// this will be set by confirmExecution() if
				// Cancel was selected
				if (this.cancelAll) break;
				
				if (!statementResult.isSuccess())
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
						this.showBusyIcon(false);

						if (!macroRun) this.highlightError(scriptParser, commandWithError, selectionOffset);

						// force a refresh in order to display the selection
						EventQueue.invokeLater(new Runnable()
						{
							public void run()
							{
								validate();
								repaint();
							}
						});
						Thread.yield();

						String question = ResourceMgr.getString("MsgScriptStatementError");
						question = StringUtil.replace(question, "%nr%", Integer.toString(i+1));
						question = StringUtil.replace(question, "%count%", Integer.toString(count));
						int choice = WbSwingUtilities.getYesNoIgnoreAll(this, question);
						this.showBusyIcon(true);

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

			} // end for loop

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
				long rows = statementResult.getTotalUpdateCount();
				msg = rows + " " + ResourceMgr.getString("MsgTotalRowsAffected") + "\n";
				this.appendToLog(msg);
			}

			ignoreStateChange = false;
			if (resultSets > 0)
			{
				this.showResultPanel();
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
		StringBuffer msg = result.getMessageBuffer();
		if (msg == null) return;
		try
		{
			this.appendToLog(msg.toString() + "\n");
		}
		catch (OutOfMemoryError oome)
		{
			result.clearMessageBuffer();
			System.gc();
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
		
		return data;
	}
	/**
	 * Display the data contained in the StatementRunnerResult.
	 * For each DataStore or ResultSet in the result, an additional
	 * result {@link workbench.gui.sql.DwPanel} will be added.
	 * @param result the result to be displayed (obtained from a {@link workbench.sql.StatementRunner}
	 * @see workbench.gui.sql.DwPanel
	 */
	private int showResult(StatementRunnerResult result)
		throws SQLException
	{
		if (!result.isSuccess()) return 0;
		String sql = result.getSourceCommand();
		
		int count = 0;
		if (result.hasDataStores())
		{
			DataStore[] results = result.getDataStores();
			for (int i = 0; i < results.length; i++)
			{
				count ++;
				DwPanel p = createDwPanel();
				p.showData(results[i], sql);
				this.addResultTab(p, sql);
			}
		}

		if (result.hasResultSets())
		{
			ResultSet[] results = result.getResultSets();
			for (int i = 0; i < results.length; i++)
			{
				count ++;
				DwPanel p = createDwPanel();
				p.showData(results[i], sql);
				this.addResultTab(p, sql);
			}
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
				editor.scrollTo(line, 0);;
				editor.selectStatementTemporary(startPos, endPos);
			}
		});
		
	}

	protected void highlightError(ScriptParser scriptParser, int commandWithError, int startOffset)
	{
		if (this.editor == null) return;
		int startPos = scriptParser.getStartPosForCommand(commandWithError) + startOffset;
		int endPos = scriptParser.getEndPosForCommand(commandWithError) + startOffset;
		int line = this.editor.getLineOfOffset(startPos);
		this.editor.select(0, 0);
		this.editor.scrollTo(line, 0);
		this.editor.selectError(startPos, endPos);
	}

	protected void checkResultSetActions()
	{
		boolean hasResult = false;
		boolean mayEdit = false;
		boolean findNext = false;
		if (this.currentData != null)
		{
			hasResult = this.currentData.hasResultSet();
			mayEdit = hasResult && this.currentData.hasUpdateableColumns();
			findNext = hasResult && (this.currentData.getTable().canSearchAgain());
		}
		
		Action[] actions = new Action[] 
						{ this.findDataAction, 
							this.dataToClipboard, 
							this.exportDataAction, 
							this.optimizeAllCol, 
							this.printDataAction, 
							this.printPreviewAction
						};
		this.setActionState(actions, hasResult);
		
		this.importFileAction.setEnabled(mayEdit);
		this.importClipAction.setEnabled(mayEdit);
		
		this.findDataAgainAction.setEnabled(findNext);
		this.copySelectedMenu.setEnabled(hasResult);
	}

	private void setExecuteActionStates(final boolean aFlag)
	{
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				executeAll.setEnabled(aFlag);
				executeSelected.setEnabled(aFlag);
				executeCurrent.setEnabled(aFlag);
				if (aFlag)
				{
					checkAutocommit();
				}
				else
				{
					commitAction.setEnabled(aFlag);
					rollbackAction.setEnabled(aFlag);
				}
				spoolData.setEnabled(aFlag);
			}
		});
	}

	private ImageIcon fileIcon = null;
	private ImageIcon fileModifiedIcon = null;
	private ImageIcon cancelIcon = null;
	private ImageIcon loadingIcon;

	private void showCancelIcon()
	{
		this.showIconForTab(this.getCancelIndicator());
		if (this.loadingIcon != null) this.loadingIcon.getImage().flush();
	}

	private ImageIcon getLoadingIndicator()
	{
		if (this.loadingIcon == null)
		{
			if (Settings.getInstance().getUseAnimatedIcon())
			{
				String name = Settings.getInstance().getProperty("workbench.gui.animatedicon.name", "loading");
				this.loadingIcon = ResourceMgr.getPicture(name);
			}
			else
			{
				this.loadingIcon = ResourceMgr.getPicture("loading-static");
			}
		}
		return this.loadingIcon;
	}

	
	private ImageIcon getCancelIndicator()
	{
		if (this.cancelIcon == null)
		{
			if (Settings.getInstance().getUseAnimatedIcon())
			{
				this.cancelIcon = ResourceMgr.getPicture("cancelling");
			}
			else
			{
				this.cancelIcon = ResourceMgr.getPicture("cancelling-static");
			}
		}
		return this.cancelIcon;
	}
	
	private ImageIcon getFileIcon()
	{
		ImageIcon icon = null;
		if (this.textModified)
		{
			if (this.fileModifiedIcon == null)
			{
				this.fileModifiedIcon = ResourceMgr.getPicture("file-modified-icon");
			}
			icon = this.fileModifiedIcon;
		}
		else
		{
			if (this.fileIcon == null)
			{
				this.fileIcon = ResourceMgr.getPicture("file-icon");
			}
			icon = this.fileIcon;
		}

		return icon;
	}

	private void removeIconFromTab()
	{
		if (this.isBusy()) return;
		this.showIconForTab(null);
	}

	private void showFileIcon()
	{
		if (this.isBusy()) return;
		this.showIconForTab(this.getFileIcon());
	}

	private void showIconForTab(ImageIcon icon)
	{
		Container parent = this.getParent();
		if (parent instanceof JTabbedPane)
		{
			JTabbedPane tab = (JTabbedPane)parent;
			int index = tab.indexOfComponent(this);
			Icon oldIcon = tab.getIconAt(index);
			if (icon == null && oldIcon == null) return;
			if (icon != oldIcon)
			{
				tab.setIconAt(index, icon);
			}
		}
	}

	private Runnable hideBusyRunnable = new Runnable()
	{
		public void run()
		{
			_showBusyIcon(false);
		}
	};
	
	private Runnable showBusyRunnable = new Runnable()
	{
		public void run()
		{
			_showBusyIcon(true);
		}
	};
	
	private void showBusyIcon(boolean show)
	{
		if (show)
		{
			WbSwingUtilities.invoke(showBusyRunnable);
		}
		else
		{
			WbSwingUtilities.invoke(hideBusyRunnable);
		}
	}
	
	private void _showBusyIcon(boolean show)
	{
		Container parent = this.getParent();
		if (parent instanceof JTabbedPane)
		{
			final JTabbedPane tab = (JTabbedPane)parent;
			int index = tab.indexOfComponent(this);
			if (index >= 0 && index < tab.getTabCount())
			{
				try
				{
					if (show)
					{
						tab.setIconAt(index, getLoadingIndicator());
					}
					else
					{
						if (this.hasFileLoaded())
						{
							tab.setIconAt(index, getFileIcon());
						}
						else
						{
							tab.setIconAt(index, null);
						}
						if (Settings.getInstance().getUseAnimatedIcon())
						{
							// flushing the animated icons also stops the thread that
							// is used for the animation. If this is not done it will still
							// "animate" in the background (at least on older JDKs) and thus
							// degrade performance
							// For a static icon this is not necessary, actually not flushing
							// the static icon improves performance when it's re-displayed
							if (this.loadingIcon != null) this.loadingIcon.getImage().flush();
							if (this.cancelIcon != null) this.cancelIcon.getImage().flush();
						}
					}
				}
				catch (Throwable th)
				{
					LogMgr.logWarning("SqlPanel.setBusy()", "Error when setting busy icon!", th);
				}
				tab.invalidate();
				tab.repaint();
			}
		}
	}

	protected void setBusy(final boolean busy)
	{
		synchronized (this)
		{
			this.threadBusy = busy;
			this.showBusyIcon(busy);
			this.setExecuteActionStates(!busy);
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

	public Window getParentWindow()
	{
		return SwingUtilities.getWindowAncestor(this);
	}

	/** Invoked when an action occurs.
	 *
	 */
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == this.optimizeAllCol)
		{
			this.currentData.getTable().optimizeAllColWidth();
		}
	}

	public void textStatusChanged(boolean modified)
	{
		this.textModified = modified;
		// Make sure the icon for the file is updated to reflect
		// the modidified status
		if (this.hasFileLoaded()) this.showFileIcon();
	}

	public void addDbExecutionListener(DbExecutionListener l)
	{
		if (this.execListener == null) this.execListener = Collections.synchronizedList(new ArrayList());
		this.execListener.add(l);
	}

	public void removeDbExecutionListener(DbExecutionListener l)
	{
		if (this.execListener == null) return;
		this.execListener.remove(l);
	}

	protected void fireDbExecStart()
	{
		synchronized (this)
		{
			if (this.execListener == null) return;
			int count = this.execListener.size();
			for (int i=0; i < count; i++)
			{
				DbExecutionListener l = (DbExecutionListener)this.execListener.get(i);
				if (l != null) l.executionStart(this.dbConnection, this);
			}
		}
	}
	
	protected void fireDbExecEnd()
	{
		synchronized (this)
		{
			if (this.execListener == null) return;
			int count = this.execListener.size();
			for (int i=0; i < count; i++)
			{
				DbExecutionListener l = (DbExecutionListener)this.execListener.get(i);
				if (l != null) l.executionEnd(this.dbConnection, this);
			}
		}
	}

	public void dispose()
	{
		Settings.getInstance().removePropertyChangeLister(this);
		if (this.currentData != null) this.currentData.clearContent();
		this.currentData = null;
		if (this.execListener != null) execListener.clear();
		if (this.sqlHistory != null) this.sqlHistory.clear();
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
		if (cancelIcon != null) cancelIcon.getImage().flush();
		if (loadingIcon != null) loadingIcon.getImage().flush();
		if (fileIcon != null) fileIcon.getImage().flush();
		if (fileModifiedIcon != null) fileModifiedIcon.getImage().flush();
	}

	public void propertyChange(PropertyChangeEvent evt)
	{
		String prop = evt.getPropertyName();
		if (prop == null) return;
		if (prop.equals(Settings.PROPERTY_ANIMATED_ICONS))
		{
			if (this.cancelIcon != null)
			{
				this.cancelIcon.getImage().flush();
				this.cancelIcon = null;
			}
			if (this.loadingIcon != null)
			{
				this.loadingIcon.getImage().flush();
				this.loadingIcon = null;
			}
		}
		else if (evt.getSource() == this.dbConnection && WbConnection.PROP_AUTOCOMMIT.equals(prop))
		{
			this.checkAutocommit();
		}
		else if (evt.getSource() == this.currentData && prop.equals("updateTable"))
		{
			this.checkResultSetActions();
		}
	}

}
