/*
 * SqlPanel.java
 *
 * Created on November 25, 2001, 2:17 PM
 */

package workbench.gui.sql;

import java.awt.*;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.ImageProducer;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import workbench.WbManager;
import workbench.db.DataSpooler;
import workbench.db.DeleteScriptGenerator;
import workbench.db.WbConnection;
import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.*;
import workbench.gui.components.*;
import workbench.gui.editor.AnsiSQLTokenMarker;
import workbench.gui.menu.TextPopup;
import workbench.interfaces.*;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.util.LineTokenizer;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbPersistence;


/**
 *	A panel with an editor (EditorPanel), a log panel and
 *	a display panel.
 *
 * @author  workbench@kellerer.org
 * @version 1.0
 */
public class SqlPanel
	extends JPanel
	implements Runnable, TableModelListener, FontChangedListener, ActionListener,
						MainPanel, Spooler, TextFileContainer
{
	private boolean selected;
	EditorPanel editor;
	private DwPanel data;
	private JTextArea log;
	private JTabbedPane resultTab;
	private JSplitPane contentPanel;
	private boolean threadBusy;
	private boolean suspended = true;
	private Thread background;
	private ArrayList statementHistory;
	private int currentHistoryEntry = -1;
	private int maxHistorySize = 10;

	private List actions = new ArrayList();
	private List toolbarActions = new ArrayList();

	private List filenameChangeListeners;

	private NextStatementAction nextStmtAction;
	private PrevStatementAction prevStmtAction;
	private StopAction stopAction;
	private ExecuteAllAction executeAll;
	private UpdateDatabaseAction updateAction;
	private ExecuteSelAction executeSelected;
	private StartEditAction startEditAction;
	private InsertRowAction insertRowAction;
	private DeleteRowAction deleteRowAction;
	private DataToClipboardAction dataToClipboard;
	private SaveDataAsAction exportDataAction;
	private CopyAsSqlInsertAction copyAsSqlInsert;
	private CreateDeleteScriptAction createDeleteScript;
	private ImportFileAction importFileAction;

	private OptimizeAllColumnsAction optimizeAllCol;

	private SpoolDataAction spoolData;
	private UndoAction undo;
	private RedoAction redo;

	private int internalId;
	private String historyFilename;

	private FileDiscardAction fileDiscardAction;
	private FindAction findAction;
	private FindAgainAction findAgainAction;
	private String lastSearchCriteria;
	private WbToolbar toolbar;
	private ConnectionInfo connectionInfo;

	private WbConnection dbConnection;
	private boolean updating;
	private String alternateDelimiter;

	private ImageIcon loadingIcon;
	private Icon dummyIcon;
	private boolean dummyIconFetched = false;
	private int lastDividerLocation = -1;

	/** Creates new SqlPanel */
	public SqlPanel(int anId)
	{
		this.setId(anId);
		this.setDoubleBuffered(true);
		this.setBorder(WbSwingUtilities.EMPTY_BORDER);
		this.setLayout(new BorderLayout());
		this.data = new DwPanel();
		this.data.setBorder(WbSwingUtilities.EMPTY_BORDER);
		this.log = new JTextArea();
		this.log.setDoubleBuffered(true);
		this.log.setBorder(new EmptyBorder(0,2,0,0));
		this.log.setFont(WbManager.getSettings().getMsgLogFont());
		this.log.setEditable(false);
		this.log.setLineWrap(true);
		this.log.setWrapStyleWord(true);
		this.log.addMouseListener(new TextComponentMouseListener());

		this.resultTab = new JTabbedPane();
		this.resultTab.setTabPlacement(JTabbedPane.TOP);
		this.resultTab.setUI(TabbedPaneUIFactory.getBorderLessUI());
		this.resultTab.setDoubleBuffered(true);
		this.resultTab.setBorder(WbSwingUtilities.EMPTY_BORDER);

		this.resultTab.addTab(ResourceMgr.getString(ResourceMgr.TAB_LABEL_RESULT), this.data);
		JScrollPane scroll = new WbScrollPane(log);
		this.resultTab.addTab(ResourceMgr.getString(ResourceMgr.TAB_LABEL_MSG), scroll);

		WbTraversalPolicy pol = new WbTraversalPolicy();
		pol.setDefaultComponent(data.getTable());
		pol.addComponent(data.getTable());
		this.resultTab.setFocusTraversalPolicy(pol);
		this.alternateDelimiter = WbManager.getSettings().getAlternateDelimiter();

		this.editor = new EditorPanel();
		this.contentPanel = new WbSplitPane(JSplitPane.VERTICAL_SPLIT, true, this.editor, this.resultTab);
		this.contentPanel.setOneTouchExpandable(true);
		this.contentPanel.setContinuousLayout(true);
		this.contentPanel.setBorder(WbSwingUtilities.EMPTY_BORDER);

		this.add(this.contentPanel, BorderLayout.CENTER);

		this.initActions();
		this.initToolbar();
		this.setupActionMap();

		this.data.getTable().addTableModelListener(this);
		this.data.getTable().setMaxColWidth(WbManager.getSettings().getMaxColumnWidth());
		this.data.getTable().setMinColWidth(WbManager.getSettings().getMinColumnWidth());
		this.makeReadOnly();
		this.checkResultSetActions();
		WbManager.getSettings().addFontChangedListener(this);
	}

	public void setId(int anId)
	{
		this.internalId = anId;
		this.historyFilename = WbManager.getSettings().getConfigDir() + "WbStatements" + Integer.toString(this.internalId) + ".xml";
	}

	public void initDefaults()
	{
		int loc = this.getHeight() / 2;
		this.contentPanel.setDividerLocation(loc);
		this.initStatementHistory(false);
	}

	public void restoreSettings()
	{
		int loc = WbManager.getSettings().getSqlDividerLocation(this.internalId);
		if (loc <= 0) loc = 200;
		this.contentPanel.setDividerLocation(loc);
		loc = WbManager.getSettings().getLastSqlDividerLocation(this.internalId);
		if (loc > 0) this.contentPanel.setLastDividerLocation(loc);
		this.initStatementHistory(true);
		this.restoreLastEditorFile();
	}

	public WbToolbar getToolbar()
	{
		return this.toolbar;
	}

	private void initToolbar()
	{
		this.toolbar = new WbToolbar();
		Border b = new CompoundBorder(new EmptyBorder(1,0,1,0), new EtchedBorder());
		toolbar.setBorder(b);
		toolbar.setBorderPainted(true);
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

	public void updateUI()
	{
		super.updateUI();
		if (this.toolbar != null)
		{
			this.toolbar.updateUI();
			this.toolbar.repaint();
		}
	}

	public boolean openFile()
	{
		String oldFile = this.editor.getCurrentFileName();
		if (this.editor.openFile())
		{
			String newFile = this.editor.getCurrentFileName();
			if (newFile != null && !newFile.equals(oldFile))
			{
				this.fireFilenameChanged();
				this.selectEditor();
			}
		}
		return true;
	}

	public boolean hasFileLoaded()
	{
		String file = this.editor.getCurrentFileName();
		return (file != null) && (file.length() > 0);
	}

	public boolean saveCurrentFile()
	{
		String oldFile = this.editor.getCurrentFileName();
		if (this.editor.saveCurrentFile())
		{
			String newFile = this.editor.getCurrentFileName();
			if (newFile != null && !newFile.equals(oldFile))
			{
				this.fireFilenameChanged();
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
				this.fireFilenameChanged();
				return true;
			}
		}
		return false;
	}

	public boolean closeFile(boolean emptyEditor)
	{
		if (this.editor.closeFile(emptyEditor))
    {
      this.fireFilenameChanged();
			this.selectEditor();
			return true;
    }
		return false;
	}

	public void fireFilenameChanged()
	{
		this.fileDiscardAction.setEnabled(this.hasFileLoaded());
		if (this.filenameChangeListeners == null) return;
		for (int i=0; i < this.filenameChangeListeners.size(); i++)
		{
			FilenameChangeListener l = (FilenameChangeListener)this.filenameChangeListeners.get(i);
			l.fileNameChanged(this, this.editor.getCurrentFileName());
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
		ExecuteSql e = new ExecuteSql();
		this.executeAll = new ExecuteAllAction(e);

		ExecuteSelectedSql se = new ExecuteSelectedSql();
		this.executeSelected = new ExecuteSelAction(se);

		MakeLowerCaseAction makeLower = new MakeLowerCaseAction(this.editor);
		MakeUpperCaseAction makeUpper = new MakeUpperCaseAction(this.editor);

		this.editor.addPopupMenuItem(makeLower, true);
		this.editor.addPopupMenuItem(makeUpper, false);

		this.editor.addPopupMenuItem(this.executeSelected, true);
		this.editor.addPopupMenuItem(this.executeAll, false);

		TextPopup pop = (TextPopup)this.editor.getRightClickPopup();

		a = new FileOpenAction(this);
		a.setCreateMenuSeparator(true);
		this.actions.add(a);
		this.actions.add(new FileSaveAction(this));
		this.actions.add(new FileSaveAsAction(this));
		this.fileDiscardAction = new FileDiscardAction(this);
		this.actions.add(this.fileDiscardAction);

		this.undo = new UndoAction(this.editor);
		this.actions.add(undo);
		a = pop.getCutAction();
		a.setCreateMenuSeparator(true);
		this.actions.add(a);
		this.actions.add(pop.getCopyAction());
		this.actions.add(pop.getPasteAction());

		a = pop.getClearAction();
		a.setCreateMenuSeparator(true);
		this.actions.add(a);
		this.actions.add(pop.getSelectAllAction());

		makeLower.setCreateMenuSeparator(true);
		this.actions.add(makeLower);
		this.actions.add(makeUpper);
		this.actions.add(new MakeInListAction(this.editor));
		this.actions.add(new MakeNonCharInListAction(this.editor));

		this.startEditAction = new StartEditAction(this);
		this.actions.add(this.startEditAction);
		this.updateAction = new UpdateDatabaseAction(this);
		this.actions.add(this.updateAction);
		this.insertRowAction = new InsertRowAction(this);
		this.deleteRowAction = new DeleteRowAction(this);
		this.actions.add(this.insertRowAction);
		this.actions.add(this.deleteRowAction);

		this.createDeleteScript = new CreateDeleteScriptAction(this);
		this.actions.add(this.createDeleteScript);

		this.exportDataAction = this.data.getTable().getExportAction();
		this.exportDataAction.setCreateMenuSeparator(true);
		this.exportDataAction.setEnabled(false);

		SelectEditorAction sea = new SelectEditorAction(this);
		sea.setCreateMenuSeparator(true);
		this.actions.add(sea);
		SelectResultAction r = new SelectResultAction(this);
		this.actions.add(r);
		a = new ExpandEditorAction(this);
		a.setCreateMenuSeparator(true);
		this.actions.add(a);
		this.actions.add(new ExpandResultAction(this));
		this.actions.add(new UndoExpandAction(this));

		this.optimizeAllCol = new OptimizeAllColumnsAction(this);
		this.optimizeAllCol.setCreateMenuSeparator(true);
		this.optimizeAllCol.setEnabled(false);
		this.optimizeAllCol.enableShortCut();
		this.optimizeAllCol.putValue(Action.SMALL_ICON, null);
		this.optimizeAllCol.putValue(WbAction.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_VIEW);
		this.actions.add(this.optimizeAllCol);

		this.dataToClipboard = this.data.getTable().getDataToClipboardAction();
		this.dataToClipboard.setEnabled(false);
		this.actions.add(this.exportDataAction);
		this.actions.add(this.dataToClipboard);

		this.copyAsSqlInsert = new CopyAsSqlInsertAction(this.data.getTable());
		this.actions.add(this.copyAsSqlInsert);

		this.importFileAction = new ImportFileAction(this);
		this.actions.add(this.importFileAction);

		this.actions.add(this.executeAll);
		this.actions.add(this.executeSelected);
		this.spoolData = new SpoolDataAction(this);
		this.actions.add(this.spoolData);

		this.stopAction = new StopAction(this);
		this.stopAction.setEnabled(false);
		this.actions.add(this.stopAction);

		this.nextStmtAction = new NextStatementAction(this);
		this.nextStmtAction.setEnabled(false);
		this.actions.add(this.nextStmtAction);

		this.prevStmtAction = new PrevStatementAction(this);
		this.prevStmtAction.setEnabled(false);
		this.actions.add(this.prevStmtAction);

		this.executeAll.setEnabled(false);
		this.executeSelected.setEnabled(false);
		this.initBackgroundThread();

		this.toolbarActions.add(this.executeSelected);
		this.toolbarActions.add(this.stopAction);
		this.toolbarActions.add(this.nextStmtAction);
		this.toolbarActions.add(this.prevStmtAction);

		this.toolbarActions.add(this.updateAction);
		this.toolbarActions.add(this.startEditAction);
		this.toolbarActions.add(this.insertRowAction);
		this.toolbarActions.add(this.deleteRowAction);

		this.findAction = this.data.getTable().getFindAction();
		this.findAction.setEnabled(false);
		this.findAction.setCreateMenuSeparator(true);
		this.findAgainAction = this.data.getTable().getFindAgainAction();
		this.findAgainAction.setEnabled(false);

		WbAction action = new CreateSnippetAction(this.editor);
		action.setCreateMenuSeparator(true);
		this.actions.add(action);
		action = new CleanJavaCodeAction(this.editor);
		this.actions.add(action);

		action = new SaveSqlHistoryAction(this);
		action.setCreateMenuSeparator(true);
		this.actions.add(action);

		this.toolbarActions.add(this.findAction);
		this.toolbarActions.add(this.findAgainAction);
		this.findAction.setCreateMenuSeparator(true);
		this.actions.add(this.findAction);
		this.actions.add(this.findAgainAction);

		WbTable table = this.data.getTable();
		table.addPopupAction(this.startEditAction, true);
		table.addPopupAction(this.insertRowAction, false);
		table.addPopupAction(this.deleteRowAction, false);
	}

	private void setupActionMap()
	{
		InputMap im = new ComponentInputMap(this);
		ActionMap am = new ActionMap();
		this.setInputMap(WHEN_IN_FOCUSED_WINDOW, im);
		this.setActionMap(am);

		for (int i=0; i < this.actions.size(); i++)
		{
			WbAction wb = (WbAction)this.actions.get(i);
			wb.addToInputMap(im, am);
		}
		editor.getInputMap().setParent(im);
		editor.getActionMap().setParent(am);
	}

	public void addToActionMap(WbAction anAction)
	{
		InputMap im = this.getInputMap(WHEN_IN_FOCUSED_WINDOW);
		ActionMap am = this.getActionMap();
		anAction.addToInputMap(im, am);
	}

	public void selectEditor()
	{
		editor.requestFocusInWindow();
	}

	public void selectResult()
	{
		showResultPanel();
		data.getTable().requestFocusInWindow();
	}

	public void saveChangesToDatabase()
	{
		this.showStatusMessage(ResourceMgr.getString("MsgUpdatingDatabase"));
		try
		{
			this.log.setText(ResourceMgr.getString("MsgUpdatingDatabase"));
			this.log.append("\n");
			int rows = this.data.saveChanges(this.dbConnection);
			this.log.append(this.data.getLastMessage());
		}
		catch (Exception e)
		{
			this.showLogMessage(this.data.getLastMessage());
		}
		this.clearStatusMessage();
	}

	/**
	 *	When the SqlPanel becomse visible (i.e. the tab is
	 *	selected in the main window) we set the focus to
	 *	the editor component.
	 */
	public void setVisible(boolean aFlag)
	{
		super.setVisible(aFlag);
		if (aFlag)
		{
			EventQueue.invokeLater(new Runnable()
				{
					public void run()
					{
						editor.grabFocus();
					}
				}
			);
		}
	}

	public List getToolbarActions()
	{
		return this.toolbarActions;
	}

	public List getActions()
	{
		return this.actions;
	}

	public void insertRow()
	{
		this.data.addRow();
	}

	public void deleteRow()
	{
		this.data.deleteRow();
	}

	public void makeReadOnly()
	{
		if (this.data.getTable() != null) this.data.getTable().setShowStatusColumn(false);
		this.setActionState(new Action[] {this.updateAction, this.insertRowAction, this.deleteRowAction}, false);
	}

	public synchronized void showLogMessage(String aMsg)
	{
		this.showLogPanel();
		this.log.setText(aMsg);
	}

	public void clearLog()
	{
		this.log.setText("");
	}

	public void showLogPanel()
	{
		this.resultTab.setSelectedIndex(1);
	}

	public void showResultPanel()
	{
		this.resultTab.setSelectedIndex(0);
	}

	public void showStatusMessage(String aMsg)
	{
		this.data.setStatusMessage(aMsg);
	}

	public void clearStatusMessage()
	{
		this.data.clearStatusMessage();
	}

	public void endEdit()
	{
		this.makeReadOnly();
		this.data.restoreOriginalValues();
	}

	public boolean startEdit()
	{
		// if the result is not yet updateable (automagically)
		// then try to find the table. If the table cannot be
		// determined, then ask the user
		if (!this.data.isUpdateable())
		{
			if (!this.data.checkUpdateTable())
			{
				String sql = this.data.getCurrentSql();
				List tables = SqlUtil.getTables(sql);
				String table = null;

				if (tables.size() > 1)
				{
					table = (String)JOptionPane.showInputDialog(this,
							null, ResourceMgr.getString("MsgEnterUpdateTable"),
							JOptionPane.QUESTION_MESSAGE,
							null,tables.toArray(),null);
				}

				if (table != null)
				{
					this.data.setVisible(false);
					this.data.setUpdateTable(table);
					this.data.setVisible(true);
				}
			}
		}
		boolean update = this.data.isUpdateable();
		if (update)
		{
			this.data.getTable().setShowStatusColumn(true);
			if (this.data.getTable().getDataStore().isModified())
			{
				this.setActionState(this.updateAction, true);

				// Check if the editing mode was started automatically
				if (!this.startEditAction.isSwitchedOn())
					this.startEditAction.setSwitchedOn(true);
			}
		}
		else
		{
			this.startEditAction.setSwitchedOn(false);
		}
		this.setActionState(new Action[] {this.insertRowAction, this.deleteRowAction}, update);
		return update;
	}

	private void initBackgroundThread()
	{
		this.suspendThread();
		this.background = new Thread(this);
		this.background.setDaemon(true);
		this.background.setName("SQL execution thread");
		this.background.start();
	}


	private void initStatementHistory()
	{
		this.initStatementHistory(false);
	}

	private void initStatementHistory(boolean readFromFile)
	{
		if (this.statementHistory != null) return;

		this.maxHistorySize = WbManager.getSettings().getMaxHistorySize();
		try
		{
			Object data = null;
			if (readFromFile)
				data = WbPersistence.readObject(this.historyFilename);
			if (data != null && data instanceof ArrayList)
			{
				this.statementHistory = (ArrayList)data;
				this.currentHistoryEntry = this.statementHistory.size() - 1;
				if (this.currentHistoryEntry > -1)
				{
					this.editor.setText(this.statementHistory.get(currentHistoryEntry).toString());
					this.editor.setCaretPosition(0);
					this.editor.clearUndoBuffer();
				}
			}
			else
			{
				this.statementHistory = new ArrayList();
				this.currentHistoryEntry = -1;
			}
		}
		catch (Exception e)
		{
			LogMgr.logWarning(this, "Error reading the statement history (" + e.getMessage() + ")");
			this.statementHistory = new ArrayList();
			this.currentHistoryEntry = -1;
		}
		this.checkStatementActions();
	}

	private void checkStatementActions()
	{
		int count;
		if (this.statementHistory == null)
		{
			count = 0;
		}
		else
		{
			count = this.statementHistory.size();
		}
		if (count == 0)
		{
			this.nextStmtAction.setEnabled(false);
			this.prevStmtAction.setEnabled(false);
		}
		else
		{
			this.nextStmtAction.setEnabled(this.currentHistoryEntry < (count - 1));
			this.prevStmtAction.setEnabled(this.currentHistoryEntry > 0);
		}
	}

	public void showNextStatement()
	{
		this.showStatementFromHistory(this.currentHistoryEntry + 1);
	}

	public void showPrevStatement()
	{
		this.showStatementFromHistory(this.currentHistoryEntry - 1);
	}

	private synchronized void showStatementFromHistory(int anIndex)
	{
		if (anIndex < 0) return;
		if (anIndex >= this.statementHistory.size()) return;
		this.currentHistoryEntry = anIndex;
		String stmt = this.statementHistory.get(this.currentHistoryEntry).toString();
		this.editor.setText(stmt);
		this.editor.setCaretPosition(0);
		this.checkStatementActions();
	}

	public void saveSettings()
	{
		int location = this.contentPanel.getDividerLocation();
		int last = this.contentPanel.getLastDividerLocation();
		WbManager.getSettings().setSqlDividerLocation(this.internalId, location);
		WbManager.getSettings().setLastSqlDividerLocation(this.internalId, last);
		String fname = this.editor.getCurrentFileName();
		WbManager.getSettings().setEditorFile(this.internalId, fname);
		this.storeInHistory(this.editor.getText());
		this.saveSqlStatementHistory();
	}

	public void saveSqlStatementHistory()
	{
		try
		{
			WbPersistence.writeObject(this.statementHistory, this.historyFilename);
		}
		catch (Exception e)
		{
			LogMgr.logWarning(this, "Error storing statement history", e);
		}
	}

	private void restoreLastEditorFile()
	{
		String filename = WbManager.getSettings().getEditorFile(this.internalId);
		if (filename != null && filename.length() > 0)
		{
			File f = new File(filename);
			if (f.exists())
			{
				if (this.editor.readFile(f)) this.fireFilenameChanged();
			}
		}
	}

	public void setStatementText(String aStatement)
	{
		this.storeInHistory(this.editor.getText());
		if (this.editor.getCurrentFile() != null) this.editor.saveCurrentFile();
		this.editor.closeFile(true);
		this.editor.setText(aStatement);
	}

	public void disconnect()
	{
		this.setConnection(null);
		this.makeReadOnly();
		this.log.setText("");
	}

	public void setConnection(WbConnection aConnection)
	{
		this.dbConnection = aConnection;
		try
		{
			this.data.setConnection(aConnection);
		}
		catch (Exception e)
		{
		}
		boolean enable = aConnection != null;
		this.connectionInfo.setConnection(aConnection);
		this.setActionState( new Action[] {this.executeAll, this.executeSelected, this.spoolData}, enable);

		if (aConnection != null)
		{
			AnsiSQLTokenMarker token = this.editor.getSqlTokenMarker();
			token.initDatabaseKeywords(aConnection.getSqlConnection());
		}
		this.checkResultSetActions();
	}

	public boolean isRequestFocusEnabled() { return true; }
	public boolean isFocusTraversable() { return true; }

	public void suspendThread()
	{
		this.suspended = true;
	}

	public synchronized void resumeThread()
	{
		this.suspended = false;
		notify();
	}

	public synchronized void storeInHistory(String aStatement)
	{
		if (this.statementHistory == null) return;
		if (aStatement == null) return;

		// if this statement is alread in the history
		// delete the old one, and put it at first position
		int index = this.statementHistory.indexOf(aStatement);
		if (index >= 0)
		{
			this.statementHistory.remove(index);
		}

		if (this.statementHistory.size() == this.maxHistorySize)
		{
			this.statementHistory.remove(0);
		}
		this.statementHistory.add(aStatement);
	}

	public void cancelExecution()
	{
		this.showStatusMessage(ResourceMgr.getString("MsgCancellingStmt"));
		this.data.cancelExecution();
		this.setCancelState(false);
		this.clearStatusMessage();
	}

	/**
	 *	This has to be executed in a separate thread
	 *	because otherwise the GUI locks up.
	 */
	public void setCancelState(final boolean aFlag)
	{
		this.setActionState(this.stopAction, aFlag);
	}

	/**
	 *	Modify the enabled state of the given action.
	 */
	public void setActionState(final Action anAction, final boolean aFlag)
	{
		EventQueue.invokeLater(
			new Runnable()
			{
				public void run()
				{
					anAction.setEnabled(aFlag);
				}
			});
	}

	public void setActionState(final Action[] anActionList, final boolean aFlag)
	{
		//EventQueue.invokeLater(
		//	new Runnable()
		//	{
		//		public void run()
		//		{
					for (int i=0; i < anActionList.length; i++)
					{
						anActionList[i].setEnabled(aFlag);
					}
		//		}
		//	});
	}


	public void runSql()
	{
		this.setCancelState(true);
		this.setBusy(true);
		this.makeReadOnly();

		this.showStatusMessage(ResourceMgr.getString(ResourceMgr.MSG_EXEC_SQL));
		this.startEditAction.setSwitchedOn(false);
		String sql;

		if (selected)
		{
			sql = this.editor.getSelectedStatement();
		}
		else
		{
			sql = this.editor.getStatement();
		}

		this.storeInHistory(sql);
		this.displayResult(sql);
		this.setBusy(false);

		this.clearStatusMessage();
		this.setCancelState(false);
		this.checkResultSetActions();
	}

	public void spoolData()
	{
		String sql = this.editor.getSelectedStatement();
		DataSpooler spooler = new DataSpooler();
		spooler.executeStatement(this.getParentWindow(), this.dbConnection, sql);
	}

	public void importFile()
	{
		if (!this.startEdit()) return;

		WbTable table = this.data.getTable();
		if (table == null) return;
		DataStoreTableModel model = (DataStoreTableModel)table.getModel();
		DataStore ds = table.getDataStore();
		if (ds == null) return;
		String lastDir = WbManager.getSettings().getLastImportDir();
		JFileChooser fc = new JFileChooser(lastDir);
		ImportFileOptionsPanel optionPanel = new ImportFileOptionsPanel();
		optionPanel.restoreSettings();
		fc.setAccessory(optionPanel);
		fc.addChoosableFileFilter(ExtensionFileFilter.getTextFileFilter());
		int answer = fc.showOpenDialog(SwingUtilities.getWindowAncestor(this));
		if (answer == JFileChooser.APPROVE_OPTION)
		{
			//result = this.readFile(fc.getSelectedFile());
			String filename = fc.getSelectedFile().getAbsolutePath();
			try
			{
				model.importFile(filename, optionPanel.getContainsHeader(), optionPanel.getColumnDelimiter());
				lastDir = fc.getCurrentDirectory().getAbsolutePath();
				WbManager.getSettings().setLastImportDir(lastDir);
			}
			catch (Exception e)
			{
				LogMgr.logError("SqlPanel.importFile()", "Error importing " + filename, e);
			}
			optionPanel.saveSettings();
		}
    this.data.dataChanged();
	}

	private void displayResult(String aSqlScript)
	{
		try
		{
			this.log.setText(ResourceMgr.getString(ResourceMgr.MSG_EXEC_SQL));
			String delimit = ";";

			if (aSqlScript.trim().endsWith(this.alternateDelimiter))
			{
				delimit = this.alternateDelimiter;
			}

			List sqls = SqlUtil.getCommands(aSqlScript, delimit);
			String msg = ResourceMgr.getString("TxtScriptStatementFinished");
			int count = sqls.size();
			msg = StringUtil.replace(msg, "%total%", Integer.toString(count));
			this.log.setText("");
			for (int i=0; i < count; i++)
			{
				String sql = (String)sqls.get(i);
				this.data.runStatement(sql);
				this.log.append(this.data.getLastMessage());
				if (count > 1)
				{
					this.log.append("\n");
					this.log.append(StringUtil.replace(msg, "%nr%", Integer.toString(i + 1)));
					this.log.append("\n\n");
					this.log.setCaretPosition(this.log.getText().length());
				}
				if (i == 0 && !this.data.hasResultSet())
				{
					this.showLogPanel();
				}
			}
			if (this.data.hasResultSet())
			{
				this.showResultPanel();
				this.data.checkUpdateTable();
			}
			else
			{
				this.showLogPanel();
			}
			if (count > 1)
			{
				this.log.append("\n");
				this.log.append(ResourceMgr.getString("TxtScriptFinished"));
				this.log.setCaretPosition(this.log.getText().length());
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
	}

	public void generateDeleteScript()
	{
		WbTable table = this.data.getTable();
		if (table == null) return;

		DataStore ds = table.getDataStore();
		if (ds == null) return;

		int row = table.getSelectedRow();
		if (row < 0)
		{
			WbManager.getInstance().showErrorMessage(this, ResourceMgr.getString("MsgSelectRow"));
			return;
		}

		String updatetable = ds.getUpdateTable();
		String schema = ds.getUpdateTableSchema();

		Map pkvalues = ds.getPkValues(row);
		try
		{
			DeleteScriptGenerator gen = new DeleteScriptGenerator(this.dbConnection);
			gen.setTable(null, schema, updatetable);
			gen.setValues(pkvalues);
			String script = gen.createScript();
			MainWindow parent = (MainWindow)SwingUtilities.getWindowAncestor(this);
			String title = ResourceMgr.getString("TxtDeleteScriptWindowTitle") + " " + schema + "." + ds.getRealUpdateTable();
			final String id = this.getClass().getName() + ".ScriptDialog";
			JFrame f = new JFrame(title);
			f.setIconImage(ResourceMgr.getPicture("workbench16").getImage());
			final JFrame fd = f;
			f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			f.addWindowListener(new WindowAdapter()
			{
				public void windowClosing(WindowEvent e)
				{
					WbManager.getSettings().storeWindowSize(fd, id);
				}
			});
			EditorPanel editor = new EditorPanel();
			editor.setText(script);
			editor.setCaretPosition(0);
			//editor.addPopupMenuItem(new FileSaveAsAction(editor), true);

			f.getContentPane().add(editor);
			if (!WbManager.getSettings().restoreWindowSize(f, id))
			{
				f.setSize(400,400);
			}
			WbSwingUtilities.center(f, parent);
			f.show();
		}
		catch (Exception e)
		{
			LogMgr.logError("SqlPanel.generateDeleteScript", "Error generating delete script", e);
		}
	}

	private void checkResultSetActions()
	{
		boolean hasResult = this.data.hasResultSet();
		this.setActionState(new Action[] {this.findAction, this.dataToClipboard, this.exportDataAction, this.optimizeAllCol}, hasResult);

		boolean mayEdit = hasResult && this.data.hasUpdateableColumns();
		this.setActionState(this.startEditAction, mayEdit);
		this.setActionState(this.createDeleteScript, mayEdit);

		boolean findNext = hasResult && (this.data.getTable() != null && this.data.getTable().canSearchAgain());
		this.setActionState(this.findAgainAction, findNext);

		boolean canUpdate = this.data.isUpdateable();
		this.setActionState(this.copyAsSqlInsert, canUpdate);
		this.setActionState(this.importFileAction, canUpdate);
	}

	public void run()
	{
		while (true)
		{
			try
			{
				if (suspended)
				{
					synchronized(this)
					{
						while(suspended)
							wait();
					}
				}
			}
			catch(InterruptedException e)
			{
				System.err.println("Interrupted");
			}
			runSql();
			suspendThread();
		}
	}

  private Image loadingImage = null; 
  
	private ImageIcon getLoadingIndicator()
	{
		if (this.loadingIcon == null)
		{
      loadingImage = ResourceMgr.getPicture("loading_smiley").getImage();
			this.loadingIcon = new ImageIcon(loadingImage);
		}
		return this.loadingIcon;
	}

	private synchronized void setBusy(boolean busy)
	{
		Container parent = this.getParent();
		if (parent instanceof JTabbedPane)
		{
			JTabbedPane tab = (JTabbedPane)parent;
			int index = tab.indexOfComponent(this);
			if (index >= 0)
			{
				/*
				if (!this.dummyIconFetched)
				{
					this.dummyIcon = tab.getIconAt(index);
					this.dummyIconFetched = true;
				}
				*/
				if (busy)
				{
					tab.setIconAt(index, getLoadingIndicator());
				}
				else
				{
          this.loadingImage.flush();
          //this.loadingImage = null;
          //this.loadingIcon = null;
					tab.setIconAt(index, null);
				}
			}
		}
		this.threadBusy = busy;
	}

	private synchronized boolean isBusy() { return this.threadBusy; }

	public class ExecuteSql implements ActionListener
	{
		public void actionPerformed(ActionEvent actionEvent)
		{
			if (!isBusy())
			{
				selected = false;
				resumeThread();
			}
			else
			{
				Toolkit.getDefaultToolkit().beep();
			}
		}

	}
	public class ExecuteSelectedSql implements ActionListener
	{
		public void actionPerformed(ActionEvent actionEvent)
		{
			if (!isBusy())
			{
				selected = true;
				resumeThread();
			}
			else
			{
				Toolkit.getDefaultToolkit().beep();
			}
		}

	}

	public void tableChanged(TableModelEvent e)
	{
		if (this.isBusy()) return;
		if (e.getFirstRow() != TableModelEvent.ALL_COLUMNS && this.data.isModified())
		{
			if (!this.startEditAction.isSwitchedOn())
			{
				this.startEdit();
			}
			else
			{
				this.setActionState(this.updateAction, true);
			}
		}
	}

	public void fontChanged(String aFontId, Font newFont)
	{
		if (aFontId.equals(Settings.MSGLOG_FONT_KEY))
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

}
