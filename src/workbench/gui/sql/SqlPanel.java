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
import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.ComponentInputMap;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JFileChooser;
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

import workbench.db.DeleteScriptGenerator;
import workbench.db.WbConnection;
import workbench.db.exporter.DataExporter;
import workbench.db.importer.DataStoreImporter;
import workbench.gui.components.WbTabbedPane;
import workbench.gui.dialogs.dataimport.ImportFileDialog;
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
import workbench.gui.actions.CreateDeleteScriptAction;
import workbench.gui.actions.CreateSnippetAction;
import workbench.gui.actions.DataToClipboardAction;
import workbench.gui.actions.ExecuteAllAction;
import workbench.gui.actions.ExecuteCurrentAction;
import workbench.gui.actions.ExecuteSelAction;
import workbench.gui.actions.ExpandEditorAction;
import workbench.gui.actions.ExpandResultAction;
import workbench.gui.actions.FileDiscardAction;
import workbench.gui.actions.FileOpenAction;
import workbench.gui.actions.FileReloadAction;
import workbench.gui.actions.FileSaveAction;
import workbench.gui.actions.FileSaveAsAction;
import workbench.gui.actions.FindAction;
import workbench.gui.actions.FindDataAction;
import workbench.gui.actions.FindDataAgainAction;
import workbench.gui.actions.FirstStatementAction;
import workbench.gui.actions.FormatSqlAction;
import workbench.gui.actions.HighlightCurrentStatement;
import workbench.gui.actions.IgnoreErrorsAction;
import workbench.gui.actions.ImportFileAction;
import workbench.gui.actions.LastStatementAction;
import workbench.gui.actions.MakeInListAction;
import workbench.gui.actions.MakeLowerCaseAction;
import workbench.gui.actions.MakeNonCharInListAction;
import workbench.gui.actions.MakeUpperCaseAction;
import workbench.gui.actions.NextStatementAction;
import workbench.gui.actions.OptimizeAllColumnsAction;
import workbench.gui.actions.PrevStatementAction;
import workbench.gui.actions.PrintAction;
import workbench.gui.actions.PrintPreviewAction;
import workbench.gui.actions.RedoAction;
import workbench.gui.actions.RollbackAction;
import workbench.gui.actions.SaveDataAsAction;
import workbench.gui.actions.SelectEditorAction;
import workbench.gui.actions.SelectMaxRowsAction;
import workbench.gui.actions.SelectResultAction;
import workbench.gui.actions.SpoolDataAction;
import workbench.gui.actions.StopAction;
import workbench.gui.actions.ToggleAutoCommitAction;
import workbench.gui.actions.UndoAction;
import workbench.gui.actions.UndoExpandAction;
import workbench.gui.actions.WbAction;
import workbench.gui.components.ConnectionInfo;
import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.ExtensionFileFilter;
import workbench.gui.components.TabbedPaneUIFactory;
import workbench.gui.components.TextComponentMouseListener;
import workbench.gui.components.WbMenu;
import workbench.gui.components.WbScrollPane;
import workbench.gui.components.WbSplitPane;
import workbench.gui.components.WbTable;
import workbench.gui.components.WbToolbar;
import workbench.gui.components.WbToolbarSeparator;
import workbench.gui.components.WbTraversalPolicy;
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
import workbench.interfaces.TextFileContainer;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.sql.MacroManager;
import workbench.sql.ScriptParser;
import workbench.sql.VariablePool;
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
				    PropertyChangeListener,
						MainPanel, Exporter, TextFileContainer, DbUpdater, Interruptable, FormattableSql, Commitable,
						JobErrorHandler, FilenameChangeListener, ExecutionController, ResultLogger
{
	private EditorPanel editor;
	private DwPanel data;
	private SqlHistory sqlHistory;

	private JTextArea log;
	private JTabbedPane resultTab;
	private JSplitPane contentPanel;
	private boolean threadBusy;
	private boolean cancelExecution = false;

	private List actions = new ArrayList();
	private List toolbarActions = new ArrayList();

	private List filenameChangeListeners;

	private StopAction stopAction;
	private ExecuteAllAction executeAll;
	private ExecuteCurrentAction executeCurrent;
	private ExecuteSelAction executeSelected;

	private DataToClipboardAction dataToClipboard;
	private SaveDataAsAction exportDataAction;
	private CopyAsSqlInsertAction copyAsSqlInsert;
	private CopyAsSqlUpdateAction copyAsSqlUpdate;
	private CopyAsSqlDeleteInsertAction copyAsSqlDeleteInsert;
	private CreateDeleteScriptAction createDeleteScript;
	private ImportFileAction importFileAction;
	private PrintAction printDataAction;
	private PrintPreviewAction printPreviewAction;
	private CheckPreparedStatementsAction checkPreparedAction;
	private ClearCompletionCacheAction clearCompletionCache;
	private AutoCompletionAction autoCompletion;
	
	private WbMenu copySelectedMenu;
	private ToggleAutoCommitAction toggleAutoCommit;
	private CommitAction commitAction;
	private RollbackAction rollbackAction;

	private OptimizeAllColumnsAction optimizeAllCol;
	private FormatSqlAction formatSql;

	private SpoolDataAction spoolData;
	private UndoAction undo;
	private RedoAction redo;

	private int internalId;

	private FileDiscardAction fileDiscardAction;
	private FileReloadAction fileReloadAction;
	private FindDataAction findDataAction;
	private FindDataAgainAction findDataAgainAction;
	private WbToolbar toolbar;
	private ConnectionInfo connectionInfo;

	private WbConnection dbConnection;
	private boolean importRunning = false;
	private boolean updateRunning = false;
	private boolean textModified = false;
	private String tabName = null;

	private List execListener = null;
	private Thread executionThread = null;
	private Interruptable worker = null;

	private static final Border statusBarBorder = new CompoundBorder(new EmptyBorder(2, 1, 0, 1), new EtchedBorder());
	private static final Border logBorder = new EmptyBorder(0,2,0,0);

	public SqlPanel(int anId)
	{
		this.setId(anId);
		this.setDoubleBuffered(true);
		this.setBorder(WbSwingUtilities.EMPTY_BORDER);
		this.setLayout(new BorderLayout());
		this.setOpaque(false);
		
		DwStatusBar statusBar = new DwStatusBar(true);
		statusBar.setBorder(statusBarBorder);
		this.data = new DwPanel(statusBar);
		this.data.setBorder(WbSwingUtilities.EMPTY_BORDER);

		this.log = new JTextArea();
		this.log.setDoubleBuffered(true);
		this.log.setBorder(logBorder);
		this.log.setFont(Settings.getInstance().getMsgLogFont());
		this.log.setEditable(false);
		this.log.setLineWrap(true);
		this.log.setWrapStyleWord(true);
		this.log.addMouseListener(new TextComponentMouseListener());

		this.resultTab = new WbTabbedPane();
		this.resultTab.setTabPlacement(JTabbedPane.TOP);
		this.resultTab.setUI(TabbedPaneUIFactory.getBorderLessUI());
		this.resultTab.setDoubleBuffered(true);
		this.resultTab.setBorder(WbSwingUtilities.EMPTY_BORDER);
		this.resultTab.setFocusable(false);

		this.resultTab.addTab(ResourceMgr.getString(ResourceMgr.TAB_LABEL_RESULT), this.data);
		JScrollPane scroll = new WbScrollPane(log);
		this.resultTab.addTab(ResourceMgr.getString(ResourceMgr.TAB_LABEL_MSG), scroll);

		WbTraversalPolicy pol = new WbTraversalPolicy();
		pol.setDefaultComponent(data.getTable());
		pol.addComponent(data.getTable());
		
		this.resultTab.setFocusTraversalPolicy(pol);
		this.editor = EditorPanel.createSqlEditor();
		this.editor.addFilenameChangeListener(this);
		this.contentPanel = new WbSplitPane(JSplitPane.VERTICAL_SPLIT, true, this.editor, this.resultTab);
		this.contentPanel.setOneTouchExpandable(true);
		this.contentPanel.setContinuousLayout(true);
		this.contentPanel.setBorder(WbSwingUtilities.EMPTY_BORDER);

		this.add(this.contentPanel, BorderLayout.CENTER);
		this.add(statusBar, BorderLayout.SOUTH);

		this.initStatementHistory();
		
		this.initActions();
		this.initToolbar();
		this.setupActionMap();

		this.data.getTable().setMaxColWidth(Settings.getInstance().getMaxColumnWidth());
		this.data.getTable().setMinColWidth(Settings.getInstance().getMinColumnWidth());
		this.makeReadOnly();
		this.checkResultSetActions();

		Settings s = Settings.getInstance();
		s.addFontChangedListener(this);

		this.editor.addTextChangeListener(this);
		this.data.setUpdateDelegate(this);
		this.data.addPropertyChangeListener("updateTable", this);

		Settings.getInstance().addPropertyChangeListener(this);
	}

	public String getId()
	{
		return Integer.toString(this.internalId);
	}

	public void setId(int anId)
	{
		this.internalId = anId;
	}

	public void initDefaults()
	{
		int loc = this.getHeight() / 2;
		if (loc <= 5) loc = 200;
		this.contentPanel.setDividerLocation(loc);
	}

	public void saveSettings()
	{
		// no global settings to store
	}
	
	public WbToolbar getToolbar()
	{
		return this.toolbar;
	}

	private void initToolbar()
	{
		this.toolbar = new WbToolbar();
		this.toolbar.addDefaultBorder();
		for (int i=0; i < this.toolbarActions.size(); i++)
		{
			WbAction a = (WbAction)toolbarActions.get(i);
			boolean toolbarSep = "true".equals((String)a.getValue(WbAction.TBAR_SEPARATOR));
			{
				if (toolbarSep)
				{
					toolbar.addSeparator();
				}
				a.addToToolbar(toolbar);
			}
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
		if (!f.exists()) return false;

		if (this.editor.readFile(f, encoding))
		{
			this.fileDiscardAction.setEnabled(true);
			this.fileReloadAction.setEnabled(true);
      this.fireFilenameChanged(this.editor.getCurrentFileName());
			this.selectEditor();
			result = true;
			this.showFileIcon();
		}
		else
		{
			this.removeIconFromTab();
		}
		return result;
	}

	public boolean openFile()
	{
		String oldFile = this.editor.getCurrentFileName();
		if (!this.canCloseFile())
		{
			this.selectEditorLater();
			return false;
		}
		if (this.editor.openFile())
		{
			String newFile = this.editor.getCurrentFileName();
			if (newFile != null && !newFile.equals(oldFile))
			{
				this.fileDiscardAction.setEnabled(true);
				this.fileReloadAction.setEnabled(true);
	      this.fireFilenameChanged(this.editor.getCurrentFileName());
				this.selectEditorLater();
			}
			this.showFileIcon();
			return true;
		}
		return false;
	}

	public boolean hasFileLoaded()
	{
		return this.editor.hasFileLoaded();
	}

	public boolean reloadFile()
	{
		if (this.editor.reloadFile())
		{
			this.showFileIcon();
			return true;
		}
		return false;
	}

	public boolean checkAndSaveFile()
	{
		if (this.editor == null) return true;
		int result = this.editor.checkAndSaveFile();
		return (result != JOptionPane.CANCEL_OPTION);
	}

	public EditorPanel getEditor()
	{
		return this.editor;
	}
	/**
	 *	Check if the current file has modifications.
	 *	@return true Modifications saved or user doesn't care
	 *          false do not close the current
	 */
	public boolean canCloseFile()
	{
		return this.editor.canCloseFile();
	}

	public boolean saveCurrentFile()
	{
		String oldFile = this.editor.getCurrentFileName();
		if (this.editor.saveCurrentFile())
		{
			String newFile = this.editor.getCurrentFileName();
			if (newFile != null && !newFile.equals(oldFile))
			{
	      this.fireFilenameChanged(this.editor.getCurrentFileName());
				return true;
			}
		}
		return false;
	}

	public boolean saveFile()
	{
		String oldFile = this.editor.getCurrentFileName();
		if (this.editor.saveFile())
		{
			String newFile = this.editor.getCurrentFileName();
			if (newFile != null && !newFile.equals(oldFile))
			{
				return true;
			}
		}
		return false;
	}

	public void clearSqlStatements()
	{
		if (this.sqlHistory != null) this.sqlHistory.clear();
		this.editor.setText("");
	}

	public boolean closeFile(boolean emptyEditor)
	{
		if (this.editor == null) return true;
		this.checkAndSaveFile();
		if (this.editor.closeFile(emptyEditor))
    {
			this.fileDiscardAction.setEnabled(false);
			this.fileReloadAction.setEnabled(false);
      this.fireFilenameChanged(this.tabName);
			this.removeIconFromTab();
			this.selectEditorLater();
			return true;
    }
		return false;
	}

	public void fileNameChanged(Object sender, String aNewName)
	{
		this.fireFilenameChanged(aNewName);
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

	public void undoExpand()
	{
		int newLoc = (int)(this.getHeight() / 2);
		this.contentPanel.setDividerLocation(newLoc);
	}

	public void expandEditor()
	{
		this.contentPanel.setDividerLocation(this.getHeight());
	}

	public void expandResultTable()
	{
		this.contentPanel.setDividerLocation(0);
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

		FileOpenAction open = new FileOpenAction(this);
		open.setCreateMenuSeparator(true);
		this.actions.add(open);
		this.actions.add(new FileSaveAction(this));
		this.actions.add(new FileSaveAsAction(this));
		this.fileDiscardAction = new FileDiscardAction(this);
		this.actions.add(this.fileDiscardAction);
		this.fileReloadAction = new FileReloadAction(this);
		this.actions.add(this.fileReloadAction);

		this.undo = new UndoAction(this.editor);
		this.actions.add(undo);
		this.redo = new RedoAction(this.editor);
		this.actions.add(redo);

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

		this.actions.add(this.data.getStartEditAction());
		this.actions.add(this.data.getSelectKeysAction());
		this.actions.add(this.data.getUpdateDatabaseAction());
		this.actions.add(this.data.getInsertRowAction());
		this.actions.add(this.data.getCopyRowAction());
		this.actions.add(this.data.getDeleteRowAction());

		this.createDeleteScript = new CreateDeleteScriptAction(this.data.getTable());
		this.actions.add(this.createDeleteScript);

		this.exportDataAction = this.data.getTable().getExportAction();
		this.exportDataAction.setCreateMenuSeparator(true);
		this.exportDataAction.setEnabled(false);

		SelectEditorAction sea = new SelectEditorAction(this);
		sea.setCreateMenuSeparator(true);
		this.actions.add(sea);
		SelectResultAction r = new SelectResultAction(this);
		this.actions.add(r);
		this.actions.add(new SelectMaxRowsAction(this.data));

		a = new ExpandEditorAction(this);
		a.setCreateMenuSeparator(true);
		this.actions.add(a);
		this.actions.add(new ExpandResultAction(this));
		this.actions.add(new UndoExpandAction(this));

		this.optimizeAllCol = new OptimizeAllColumnsAction(this.data.getTable());
		this.optimizeAllCol.setCreateMenuSeparator(true);
		this.optimizeAllCol.setEnabled(false);
		this.optimizeAllCol.putValue(Action.SMALL_ICON, null);
		this.optimizeAllCol.putValue(WbAction.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_VIEW);
		this.actions.add(this.optimizeAllCol);

		this.dataToClipboard = this.data.getTable().getDataToClipboardAction();
		this.dataToClipboard.setEnabled(false);
		this.actions.add(this.exportDataAction);
		this.actions.add(this.dataToClipboard);

		this.copyAsSqlInsert = this.data.getTable().getCopyAsInsertAction();
		this.actions.add(this.copyAsSqlInsert);

		this.copyAsSqlUpdate = this.data.getTable().getCopyAsUpdateAction();
		this.actions.add(this.copyAsSqlUpdate);

		this.copyAsSqlDeleteInsert = this.data.getTable().getCopyAsDeleteInsertAction();
		this.actions.add(this.copyAsSqlDeleteInsert);

		copySelectedMenu = this.data.getTable().getCopySelectedMenu();
		this.actions.add(copySelectedMenu);

		this.importFileAction = new ImportFileAction(this);
		this.importFileAction.setCreateMenuSeparator(true);
		this.actions.add(this.importFileAction);

		this.printDataAction = this.data.getTable().getPrintAction();
		this.printPreviewAction = this.data.getTable().getPrintPreviewAction();

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
		
		this.toolbarActions.add(this.data.getUpdateDatabaseAction());
		this.toolbarActions.add(this.data.getStartEditAction());
		this.toolbarActions.add(this.data.getInsertRowAction());
		this.toolbarActions.add(this.data.getCopyRowAction());
		this.toolbarActions.add(this.data.getDeleteRowAction());

		WbAction filterAction = this.data.getTable().getFilterAction();
		filterAction.setCreateToolbarSeparator(true);
		filterAction.setCreateMenuSeparator(true);
		this.toolbarActions.add(filterAction);
		this.toolbarActions.add(this.data.getTable().getResetFilterAction());
		
		this.commitAction.setCreateToolbarSeparator(true);
		this.toolbarActions.add(this.commitAction);
		this.toolbarActions.add(this.rollbackAction);
		ignore.setCreateToolbarSeparator(true);
		this.toolbarActions.add(ignore);

		this.findDataAction = this.data.getTable().getFindAction();
		this.findDataAction.setMenuTextByKey("MnuTxtFindData");
		this.findDataAction.setEnabled(false);
		this.findDataAction.setCreateMenuSeparator(true);
		this.findDataAgainAction = this.data.getTable().getFindAgainAction();
		this.findDataAgainAction.setMenuTextByKey("MnuTxtFindDataAgain");
		this.findDataAgainAction.setEnabled(false);

		this.autoCompletion = new AutoCompletionAction(this.editor, this.data.getStatusBar());
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
		this.actions.add(this.data.getTable().getResetFilterAction());
		
		this.printDataAction.setCreateMenuSeparator(true);
		this.actions.add(this.printDataAction);
		this.actions.add(this.printPreviewAction);

		this.setExecuteActionStates(false);
	}

	private void setupActionMap()
	{
		InputMap im = new ComponentInputMap(this);
		ActionMap am = new ActionMap();
		this.setInputMap(WHEN_IN_FOCUSED_WINDOW, im);
		this.setActionMap(am);

		for (int i=0; i < this.actions.size(); i++)
		{
			Object entry = this.actions.get(i);
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
		if (this.isVisible() && this.isCurrentTab())
		{
			showResultPanel();
			data.getTable().requestFocusInWindow();
		}
	}

	public void saveChangesToDatabase()
	{
		// Make sure we have real PK columns.
		boolean hasPk = this.data.getTable().checkPkColumns(true);
		if (!hasPk) return;

		// check if we really want to save the data
		// it fhe SQL Preview is not enabled this will
		// always return true, otherwise it depends on the user's
		// selection after the SQL preview has been displayed
		if (!this.data.shouldSaveChanges(this.dbConnection)) return;

		this.setBusy(true);

		this.updateRunning = true;
		this.showStatusMessage(ResourceMgr.getString("MsgUpdatingDatabase"));
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

	private void updateDb()
	{
		String errorMessage = null;
		boolean success = false;
		boolean wasUpdate = this.updateRunning;

		try
		{
			this.log.setText(ResourceMgr.getString("MsgUpdatingDatabase"));
			this.log.append("\n");
			this.data.saveChanges(this.dbConnection, this);
			this.log.append(this.data.getLastMessage());
			success = true;
		}
		catch (OutOfMemoryError mem)
		{
			// do not show the error message right away
			// the message dialog should only be shown if the
			// animated icon is not running! Otherwise
			// the system might lock
			this.log.setText(ExceptionUtil.getDisplay(mem));
			errorMessage = ResourceMgr.getString("MsgOutOfMemoryError");
			success = false;
		}
		catch (Exception e)
		{
			errorMessage = this.data.getLastMessage();
			this.log.setText(errorMessage);
			success = false;
		}
		finally
		{
			this.updateRunning = false;
			this.setCancelState(false);
			this.setBusy(false);
			WbSwingUtilities.showDefaultCursor(this);
		}

		this.setExecuteActionStates(true);

		if (success)
		{
			this.clearStatusMessage();
			this.checkResultSetActions();
		}
		else if (!wasUpdate)
		{
			final String msg = errorMessage;
			// Make sure the error dialog is displayed on the AWT
			EventQueue.invokeLater(new Runnable()
			{
				public void run()
				{
					WbSwingUtilities.showErrorMessage(getParentWindow(), msg);
				}
			});
		}
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
		if (this.data == null) return;
		this.data.endEdit();
	}

	/**
	 *	Show a message in the log panel. This will also switch
	 *	the display to the log panel (away from the result panel
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
		this.resultTab.setSelectedIndex(1);
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
		if (this.data == null) return;
		this.data.setStatusMessage(aMsg);
	}

	/**
	 *	Clear the message in the status bar of the DwPanel
	 */
	public void clearStatusMessage()
	{
		this.data.clearStatusMessage();
	}

	public void initStatementHistory()
	{
		int size = Settings.getInstance().getMaxHistorySize();
		this.sqlHistory = new SqlHistory(editor,size);
	}

	public void readFromWorkspace(WbWorkspace w, int index)
		throws IOException
	{
		this.editor.setText("");

		try
		{
			w.readHistoryData(index, this.sqlHistory);
		}
		catch (Exception e)
		{
			LogMgr.logWarning("SqlPanel.readFromWorkspace()", "Could not read history data for index " + (this.internalId - 1));
			this.clearSqlStatements();
		}

		String filename = w.getExternalFileName(index);
		this.tabName = w.getTabTitle(index);
		if (this.tabName != null && this.tabName.length() == 0)
		{
			this.tabName = null;
		}

		int v = w.getMaxRows(index);
		this.data.setMaxRows(v);
		v = w.getQueryTimeout(index);
		this.data.setQueryTimeout(v);

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
		this.editor.clearUndoBuffer();
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
		
		w.setMaxRows(index, this.data.getMaxRows());
		w.setQueryTimeout(index, this.data.getQueryTimeout());
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

	public String getName()
	{
		return this.getTabTitle();
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

	public synchronized void disconnect()
	{
		if (this.dbConnection != null) 
		{
			this.setConnection(null);
		}
		this.makeReadOnly();
		this.log.setText("");
	}

	public synchronized WbConnection getConnection()
	{
		return this.dbConnection;
	}

	public synchronized boolean isConnected()
	{
		// I'm only checking if the connection is defined, because
		// MainWindow will make sure a valid connection is set
		// for the panel. When using only one connection for all
		// panels, isClosed() will block the entire AWT thread!

		return (this.dbConnection != null);
	}

	public synchronized void setConnection(WbConnection aConnection)
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
		
		try
		{
			this.data.setConnection(aConnection);
			this.data.setResultLogger(this);
		}
		catch (Exception e)
		{
		}
		boolean enable = (aConnection != null);
		if (this.connectionInfo != null) this.connectionInfo.setConnection(aConnection);
		this.setExecuteActionStates(enable);

		if (aConnection != null)
		{
			if (this.editor != null) this.editor.setDatabaseConnection(aConnection);
			if (this.data != null) this.data.setAutomaticUpdateTableCheck(!aConnection.getProfile().getDisableUpdateTableCheck());
		}

		if (this.dbConnection != null)
		{
			this.dbConnection.addChangeListener(this);
			this.addDbExecutionListener(this.dbConnection);
		}

		this.checkResultSetActions();
		this.checkAutocommit();
	}

	/**
	 * Check the autoCommit property of the current connection
	 * and enable/disable the rollback and commit actions
	 * accordingly
	 */
	private void checkAutocommit()
	{
		SwingUtilities.invokeLater(new Runnable()
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

	public synchronized void storeStatementInHistory()
	{
		this.sqlHistory.addContent(editor);
	}

	public void cancelUpdate()
	{
		WbTable table = this.data.getTable();
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
				this.data.rowCountChanged();
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

	private void cancelRetrieve()
	{
		this.showCancelIcon();
		this.cancelExecution = true;
		this.setCancelState(false);
		this.data.cancelExecution();
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
			showLogMessage(ResourceMgr.getString("ErrorConnectionBusy"));
			return;
		}
		this.executionThread = new WbThread(new Runnable()
		{
			public void run()
			{
				runStatement(sql, offset, commandAtIndex, highlight);
			}
		},"SQL Execution Thread " + this.getId());
		this.executionThread.setPriority(Thread.NORM_PRIORITY + 2);
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
		this.showStatusMessage(ResourceMgr.getString(ResourceMgr.MSG_EXEC_SQL));
		
		this.storeStatementInHistory();
		cancelExecution = false;
		setBusy(true);
		fireDbExecStart();
		data.setBatchUpdate(true);

		// the dbStart should be fired *after* updating the
		// history, as the history might be saved ("AutoSaveHistory") if the MainWindow
		// receives the execStart event
		WbSwingUtilities.invoke(new Runnable()
		{
			public void run()
			{
				setCancelState(true);
				makeReadOnly();
			}
		});
		
		try
		{
			this.displayResult(sql, selectionOffset, commandAtIndex, highlightOnError);
		}
		finally
		{
			this.data.setBatchUpdate(false);
			this.setBusy(false);

			WbSwingUtilities.invoke(new Runnable()
			{
				public void run()
				{
					clearStatusMessage();
					setCancelState(false);
					checkResultSetActions();
				}
			});

			this.fireDbExecEnd();
			this.selectEditorLater();
			this.executionThread = null;
		}
	}

	public void executeMacro(final String macroName, final boolean replaceText)
	{
		if (isBusy()) return;

		final String sql = MacroManager.getInstance().getMacroText(macroName);
		if (sql == null || sql.trim().length() == 0) return;

		if (replaceText)
		{
			this.storeStatementInHistory();
			this.editor.setText(sql);
		}
		this.startExecution(sql, 0, -1, false);
	}

	public void exportData()
	{
		final String sql = SqlUtil.makeCleanSql(this.editor.getSelectedStatement(),false);

		this.cancelExecution = false;

		final DataExporter exporter = new DataExporter(this.dbConnection);
		exporter.setRowMonitor(this.data.getRowMonitor());
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
	 * 	We are implementing our own getUpdateErrorAction() because it's necessary to
	 *  turn off the loading indicator before displaying a message box.
	 * 	DwPanel's getUpdateErrorAction is called from here after turning off the loading indicator.
	 */
	public int getUpdateErrorAction(int errorRow, String errorColumn, String dataLine, String errorMessage)
	{
		this.showBusyIcon(false);
		int choice = this.data.getActionOnError(errorRow, errorColumn, dataLine, errorMessage);
		this.showBusyIcon(true);
		return choice;
	}

	public int getImportErrorAction(int errorRow, String errorColumn, String dataLine, String errorMessage)
	{
		String msg = null;
		if (errorColumn != null)
		{
			msg = ResourceMgr.getString("ErrorColumnImportError");
			msg = msg.replaceAll("%row%", Integer.toString(errorRow));
			msg = msg.replaceAll("%column%", errorColumn);
			msg = msg.replaceAll("%data%", dataLine);
		}
		else
		{
			msg = ResourceMgr.getString("ErrorRowImportError");
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

	public synchronized void importFile()
	{
		if (!this.data.startEdit()) return;
		ImportFileDialog dialog = new ImportFileDialog(this);
		dialog.allowImportModeSelection(false);
		boolean ok = dialog.selectInput(ResourceMgr.getString("TxtWindowTitleSelectImportFile"));
		if (!ok) return; 
		
		this.setActionState(this.importFileAction, false);
		
		final DataStoreImporter importer = new DataStoreImporter(data.getTable().getDataStore(), data.getRowMonitor(), this);
		final String filename = dialog.getSelectedFilename();
		
		importer.setImportOptions(filename, 
			                        dialog.getImportType(), 
			                        dialog.getGeneralOptions(), 
			                        dialog.getTextOptions(), 
			                        dialog.getXmlOptions());
		
		File f = new File(filename);
		Settings.getInstance().setLastImportDir(f.getParent());
		dialog.saveSettings();

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
				catch (Exception e)
				{
					LogMgr.logError("SqlPanel.importFile() - worker thread", "Error when importing " + filename, e);
				}
				finally
				{
					importRunning = false;
					setBusy(false);
					fireDbExecEnd();
					data.getTable().getDataStoreTableModel().fileImported();
					data.rowCountChanged();
					data.clearStatusMessage();
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
			}
		}
		finally
		{
			this.showBusyIcon(true);
		}
		return result;
	}

	private void displayResult(String script, int selectionOffset, int commandAtIndex, boolean highlightOnError)
	{
		if (script == null) return;

		boolean logWasCompressed = false;
		boolean jumpToNext = (commandAtIndex > -1 && Settings.getInstance().getAutoJumpNextStatement());
		boolean highlightCurrent = false;
		boolean restoreSelection = false;
		boolean checkPreparedStatement = Settings.getInstance().getCheckPreparedStatements();
		boolean shouldRestoreSelection = Settings.getInstance().getBoolProperty("workbench.gui.sql.restoreselection", true);
		this.executeAllStatements = false;
		this.cancelAll = false;
		
		ExecutionController control = null;
		if (this.dbConnection.getProfile().isConfirmUpdates())
		{
			control = this;
		}

		ScriptParser scriptParser = new ScriptParser();
		scriptParser.setAlternateDelimiter(Settings.getInstance().getAlternateDelimiter());
		scriptParser.setCheckEscapedQuotes(Settings.getInstance().getCheckEscapedQuotes());
		scriptParser.setSupportOracleInclude(this.dbConnection.getMetadata().supportShortInclude());

		int oldSelectionStart = -1;
		int oldSelectionEnd = -1;

		try
		{
			this.log.setText("");

			String cleanSql = SqlUtil.makeCleanSql(script, false);
			String macro = MacroManager.getInstance().getMacroText(cleanSql);
			if (macro != null)
			{
				appendToLog(ResourceMgr.getString("MsgExecutingMacro") + ": " + cleanSql + "\n");
				script = macro;
			}
			scriptParser.setScript(script);
			List sqls = scriptParser.getCommands();

			int commandWithError = -1;
			int startIndex = 0;
			int count = sqls.size();
			int endIndex = count;
			int failuresIgnored = 0;

			if (count == 0)
			{
				this.appendToLog(ResourceMgr.getString("ErrorNoCommand"));
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
					this.appendToLog(ResourceMgr.getString("ErrorNoCurrentStatement"));
					this.showLogPanel();
					return;
				}
			}

			if (count > 1) logWasCompressed = !this.data.getVerboseLogging();
			
			StringBuffer finishedMsg1 = new StringBuffer(ResourceMgr.getString("TxtScriptStatementFinished1"));
			finishedMsg1.append(' ');
			StringBuffer finishedMsg2 = new StringBuffer(20);
			finishedMsg2.append(' ');
			String msg = ResourceMgr.getString("TxtScriptStatementFinished2");
			msg = StringUtil.replace(msg, "%total%", Integer.toString(count));
			finishedMsg2.append(msg);
			
			StringBuffer finishedMsg = new StringBuffer(finishedMsg1.length() + finishedMsg2.length() + 5);
			String currentMsg = null;

			boolean onErrorAsk = !Settings.getInstance().getIgnoreErrors();

			this.data.scriptStarting();
			this.showResultPanel();

			highlightCurrent = ( (count > 1 || commandAtIndex > -1) && Settings.getInstance().getHighlightCurrentStatement());

			if (highlightCurrent)
			{
				oldSelectionStart = this.editor.getSelectionStart();
				oldSelectionEnd = this.editor.getSelectionEnd();
				restoreSelection = shouldRestoreSelection;
			}
			long startTime = System.currentTimeMillis();
			long stmtTotal = 0;
			int executedCount = 0;
			String currentSql = null;
			VariablePrompter prompter = new VariablePrompter();
			
			StringBuffer logmsg = new StringBuffer(100);
			for (int i=startIndex; i < endIndex; i++)
			{
				logmsg.delete(0,logmsg.length());
				currentSql = scriptParser.getCommand(i);

				// By calling yield() we make sure that
				// this thread can actually be interrupted!
				Thread.yield();
				if (cancelExecution) break;

				boolean goOn = true;
				
				prompter.setSql(currentSql);
				if (prompter.needsInput())
				{
					// the animated gif needs to be turned off when a
					// dialog is displayed, otherwise Swing uses too much CPU
					this.showBusyIcon(false);
					goOn = prompter.getPromptValues();
					this.showBusyIcon(true);
				}

				if (goOn && checkPreparedStatement)
				{
					PreparedStatementPool pool = this.dbConnection.getPreparedStatementPool();
					try
					{
						if (pool.isRegistered(currentSql) || pool.addPreparedStatement(currentSql))
						{
							StatementParameters parms = pool.getParameters(currentSql);
							this.showBusyIcon(false);
							goOn = ParameterEditor.showParameterDialog(parms);
							this.showBusyIcon(true);
						}
					}
					catch (SQLException e)
					{
							this.showBusyIcon(false);
							msg = ResourceMgr.getString("ErrorCheckPreparedStatement").replaceAll("%error%", ExceptionUtil.getDisplay(e));
							WbSwingUtilities.showErrorMessage(this, msg);
							this.showBusyIcon(true);
					}
				}
				if (!goOn)
				{
					String cancelMsg = ResourceMgr.getString("MsgSqlCancelledDuringPrompt");
					cancelMsg = cancelMsg.replaceAll("%nr%", Integer.toString(i+1));
					this.appendToLog(cancelMsg);
					this.showLogPanel();
					continue;
				}

				if (highlightCurrent)
				{
					highlightStatement(scriptParser, i, selectionOffset);
					editor.validate();
					Thread.yield();
				}
				
				this.data.runStatement(currentSql, control, true);
				
				stmtTotal += data.getLastExecutionTime();

				// the SET FEEDBACK command might change the feedback level
				// so it needs to be checked each time.
				if (count > 1) logWasCompressed = logWasCompressed || !this.data.getVerboseLogging();
				
				finishedMsg.delete(0, finishedMsg.length());
				finishedMsg.append(finishedMsg1);
				finishedMsg.append(i + 1);
				finishedMsg.append(finishedMsg2);
				currentMsg = finishedMsg.toString();

				if (!logWasCompressed)
				{
					logmsg.append(this.data.getLastMessage());
					logmsg.append('\n');
					if (count > 1)
					{
						logmsg.append("(");
						logmsg.append(currentMsg);
						logmsg.append(")\n\n");
					}
					this.appendToLog(logmsg.toString());
				}

				if (count > 1)
				{
					this.showStatusMessage(currentMsg);
				}

				if (i == 0 && !this.data.hasResultSet())
				{
					this.showLogPanel();
				}

				// this will be set by confirmExecution() if
				// Cancel was selected
				if (this.cancelAll) break;
				
				if (!this.data.wasSuccessful())
				{
					commandWithError = i;

					// error messages should always be shown in the log
					// panel, even if compressLog is enabled (if it is not enabled
					// the messages have been appended to the log already)
					if (logWasCompressed) this.appendToLog(this.data.getLastMessage());

					if (count > 1 && onErrorAsk && (i < (count - 1)))
					{
						// the animated gif needs to be turned off when a
						// dialog is displayed, otherwise Swing uses too much CPU
						this.showBusyIcon(false);

						this.highlightError(scriptParser, commandWithError, selectionOffset);

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

			} // end for loop

			final long end = System.currentTimeMillis();
			long execTime = (end - startTime);
			data.getStatusBar().setExecutionTime(stmtTotal);

			this.data.clearStatusMessage();

			if (commandWithError > -1 && highlightOnError)
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
				long rows = this.data.getRowsAffectedByScript();
				msg = rows + " " + ResourceMgr.getString("MsgTotalRowsAffected") + "\n";
				this.appendToLog(msg);
			}

			if (this.data.hasResultSet() && !this.data.hasWarning())
			{
				this.showResultPanel();
				StringBuffer header = new StringBuffer(80);
				header.append(ResourceMgr.getString("TxtPrintHeaderResultFrom"));
				header.append(currentSql);
				this.data.setPrintHeader(header.toString());
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
				else
				{
					this.editor.setCaretPosition(oldSelectionStart);
				}
			}
		}
		catch (SQLException e)
		{
			this.showLogMessage(this.data.getLastMessage());
		}
		catch (Exception e)
		{
			this.showLogMessage(this.data.getLastMessage());
			LogMgr.logError("SqlPanel.displayResult()", "Error executing statement", e);
		}
		finally
		{
			this.data.scriptFinished();
		}
	}

	private void highlightStatement(ScriptParser scriptParser, int command, int startOffset)
	{
		if (this.editor == null) return;
		int startPos = scriptParser.getStartPosForCommand(command) + startOffset;
		int endPos = scriptParser.getEndPosForCommand(command) + startOffset;
		int line = this.editor.getLineOfOffset(startPos);
		this.editor.scrollTo(line, 0);
		this.editor.selectStatementTemporary(startPos, endPos);
	}

	private void highlightError(ScriptParser scriptParser, int commandWithError, int startOffset)
	{
		if (this.editor == null) return;
		int startPos = scriptParser.getStartPosForCommand(commandWithError) + startOffset;
		int endPos = scriptParser.getEndPosForCommand(commandWithError) + startOffset;
		int line = this.editor.getLineOfOffset(startPos);
		this.editor.select(0, 0);
		this.editor.scrollTo(line, 0);
		this.editor.selectError(startPos, endPos);
	}

	private void checkResultSetActions()
	{
		if (this.data == null) return;

		boolean hasResult = this.data.hasResultSet();
		this.setActionState(new Action[] {this.findDataAction, this.dataToClipboard, this.exportDataAction, this.optimizeAllCol}, hasResult);

		boolean mayEdit = hasResult && this.data.hasUpdateableColumns();
		this.data.getStartEditAction().setEnabled(mayEdit);
		int rows = this.data.getTable().getSelectedRowCount();
		this.createDeleteScript.setEnabled(mayEdit && rows == 1);

		this.data.getCopyRowAction().setEnabled(mayEdit && (rows == 1));
		this.data.getInsertRowAction().setEnabled(this.data.isUpdateable());

		boolean findNext = hasResult && (this.data.getTable() != null && this.data.getTable().canSearchAgain());
		this.findDataAgainAction.setEnabled(findNext);

		this.data.getTable().checkKeyActions();
		this.importFileAction.setEnabled(mayEdit);
	}

	private void setExecuteActionStates(boolean aFlag)
	{
		this.executeAll.setEnabled(aFlag);
		this.executeSelected.setEnabled(aFlag);
		this.executeCurrent.setEnabled(aFlag);
		this.importFileAction.setEnabled(aFlag);
		if (aFlag)
		{
			this.checkAutocommit();
		}
		else
		{
			this.commitAction.setEnabled(aFlag);
			this.rollbackAction.setEnabled(aFlag);
		}
		this.spoolData.setEnabled(aFlag);
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
				this.loadingIcon = ResourceMgr.getPicture("loading");
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
				//tab.validate();
				tab.repaint();
			}
		}
	}

	private synchronized void setBusy(final boolean busy)
	{
		if (busy == this.threadBusy) return;
		this.threadBusy = busy;
		this.setExecuteActionStates(!busy);
		this.showBusyIcon(busy);
	}


	public synchronized boolean isBusy() { return this.threadBusy; }


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
			this.data.getTable().optimizeAllColWidth();
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

	private void fireDbExecStart()
	{
		if (this.execListener == null) return;
		int count = this.execListener.size();
		for (int i=0; i < count; i++)
		{
			DbExecutionListener l = (DbExecutionListener)this.execListener.get(i);
			if (l != null) l.executionStart(this.dbConnection, this);
		}
	}
	
	private void fireDbExecEnd()
	{
		if (this.execListener == null) return;
		int count = this.execListener.size();
		for (int i=0; i < count; i++)
		{
			DbExecutionListener l = (DbExecutionListener)this.execListener.get(i);
			if (l != null) l.executionEnd(this.dbConnection, this);
		}
	}

	public void dispose()
	{
		Settings.getInstance().removePropertyChangeLister(this);
		this.data.clearContent();
		this.data = null;
		this.sqlHistory.clear();
		this.editor.dispose();
		this.editor = null;
		this.actions.clear();
		this.toolbarActions.clear();
		this.filenameChangeListeners.clear();
		this.execListener.clear();
		this.toolbar.removeAll();
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
		else if (evt.getSource() == this.data && prop.equals("updateTable"))
		{
			this.checkResultSetActions();
		}
	}

}
