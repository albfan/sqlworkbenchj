/*
 * SqlPanel.java
 *
 * Created on November 25, 2001, 2:17 PM
 */

package workbench.gui.sql;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import workbench.WbManager;
import workbench.db.WbConnection;

import workbench.gui.actions.*;
import workbench.gui.actions.CreateSnippetAction;
import workbench.gui.components.WbToolbar;
import workbench.gui.menu.TextPopup;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.util.StringUtil;
import workbench.util.WbPersistence;




/**
 *	A panel with an editor (EditorPanel), a log panel and
 *	a display panel.
 *
 * @author  thomas
 * @version 1.0
 */
public class SqlPanel extends JPanel implements Runnable, TableModelListener
{
	private boolean selected = false;
	private EditorPanel editor;
	private SqlResultDisplay result;
	private JSplitPane contentPanel;
	private boolean threadBusy = false;
	private boolean suspended = true;
	private Thread background;
	//private JMenu[] menus = null;
	//private WindowStatusBar winStatusBar = null;
	private ArrayList statementHistory;
	private int currentHistoryEntry = -1;
	private int maxHistorySize = 10;

	private List actions = new ArrayList();
	private List toolbarActions = new ArrayList();
	
	private NextStatementAction nextStmtAction;
	private PrevStatementAction prevStmtAction;
	private StopAction stopAction = null;
	private ExecuteAllAction executeAll;
	private UpdateDatabaseAction updateAction;
	private ExecuteSelAction executeSelected;
	private int internalId;
	private String historyFilename;

	private FindAction findAction = null;
	private FindAgainAction findAgainAction = null;
	private static final int DIVIDER_SIZE = 5;
	private String lastSearchCriteria;
	private WbToolbar toolbar;
	
	/** Creates new SqlPanel */
	public SqlPanel(int anId)
	{
		this.internalId = anId;
		this.setDoubleBuffered(true);
		this.setBorder(null);
		this.historyFilename = "WbStatements" + Integer.toString(this.internalId) + ".xml";
		this.setLayout(new BorderLayout());
		this.result = new SqlResultDisplay();
		this.editor = new EditorPanel();
		this.contentPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, this.editor, this.result);
		this.contentPanel.setDividerSize(DIVIDER_SIZE);
		this.contentPanel.setBorder(null);
		this.result.setBorder(null);
		this.add(this.contentPanel, BorderLayout.CENTER);
		
		int loc = WbManager.getSettings().getSqlDividerLocation();
		if (loc <= 0) loc = 200;
		this.contentPanel.setDividerLocation(loc);
		
		this.initActions();
		this.initToolbar();
		this.setupActionMap();
		this.initListener();
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
			Action a = (Action)toolbarActions.get(i);
			boolean toolbarSep = "true".equals((String)a.getValue(WbAction.TBAR_SEPARATOR));
			{
				if (toolbarSep)
				{
					toolbar.addSeparator();
				}
				toolbar.add(a);
			}
		}
		toolbar.addSeparator();
	}

	public void updateUI()
	{
		super.updateUI();
		if (this.toolbar != null)
		{
			this.toolbar.updateUI();
		}
	}
	
	private void initActions()
	{
		ExecuteSql e = new ExecuteSql();
		this.executeAll = new ExecuteAllAction(e);

		ExecuteSelectedSql se = new ExecuteSelectedSql();
		this.executeSelected = new ExecuteSelAction(se);
		
		this.editor.addPopupMenuItem(this.executeSelected, true);
		this.editor.addPopupMenuItem(this.executeAll, false);

		TextPopup pop = (TextPopup)this.editor.getRightClickPopup();
		
		this.actions.add(pop.getCutAction());
		this.actions.add(pop.getCopyAction());
		this.actions.add(pop.getPasteAction());
		
		WbAction a = pop.getClearAction();
		a.setCreateMenuSeparator(true);
		this.actions.add(a);
		this.actions.add(pop.getSelectAllAction());

		this.updateAction = new UpdateDatabaseAction(this);
		this.actions.add(this.updateAction);
	
		SaveDataAsAction save = new SaveDataAsAction(this.result);
		save.setCreateMenuSeparator(true);

		DataToClipboardAction clp = new DataToClipboardAction(this.result);
		this.actions.add(save);
		this.actions.add(clp);
		
		this.actions.add(this.executeAll);
		this.actions.add(this.executeSelected);
		
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
		
		this.findAction = new FindAction(this);
		this.findAction.setEnabled(false);
		this.findAction.setCreateMenuSeparator(true);
		this.findAgainAction = new FindAgainAction(this);
		this.findAgainAction.setEnabled(false);

		WbAction action = new CreateSnippetAction(this.editor);
		action.setCreateMenuSeparator(true);
		this.actions.add(action);
		
		this.toolbarActions.add(this.findAction);
		this.toolbarActions.add(this.findAgainAction);
		this.findAction.setCreateMenuSeparator(true);
		this.actions.add(this.findAction);
		this.actions.add(this.findAgainAction);
	}
	
	private void setupActionMap()
	{
		InputMap im = new ComponentInputMap(this);
		ActionMap am = new ActionMap();
		this.setInputMap(WHEN_IN_FOCUSED_WINDOW, im);
		this.setActionMap(am);

		for (int i=0; i < this.actions.size(); i++)
		{
			this.addToActionMap((WbAction)this.actions.get(i));
		}
	}
	
	public void addToActionMap(WbAction anAction)
	{
		InputMap in = this.getInputMap();
		ActionMap am = this.getActionMap();
		KeyStroke key = anAction.getAccelerator();
		if (key != null)
		{
			in.put(key, anAction.getActionName());
			am.put(anAction.getActionName(), anAction);
		}
	}
	
	private void initListener()
	{
		this.result.addTableModelListener(this);
	}
	
	public void saveChangesToDatabase()
	{
		this.result.saveChangesToDatabase();
	}
	
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

	public void findData()
	{
		String criteria;
		criteria = JOptionPane.showInputDialog(this, ResourceMgr.getString("EnterSearchCriteria"), this.lastSearchCriteria);
		int row = this.result.search(criteria);
		this.findAgainAction.setEnabled(row >= 0);
		this.lastSearchCriteria = criteria;
	}
	
	public void findNext()
	{
		this.result.searchNext();
	}
	
	public List getToolbarActions()
	{
		return this.toolbarActions;
	}
	
	public List getActions()
	{
		return this.actions;
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
		if (this.statementHistory != null) return;
		
		this.maxHistorySize = WbManager.getSettings().getMaxHistorySize();
		try
		{	
			Object data = WbPersistence.readObject(this.historyFilename);
			if (data != null && data instanceof ArrayList)
			{
				this.statementHistory = (ArrayList)data;
				this.currentHistoryEntry = this.statementHistory.size() - 1;
				if (this.currentHistoryEntry > -1)
				{
					this.editor.setText(this.statementHistory.get(currentHistoryEntry).toString());
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
	
	public synchronized void showNextStatement()
	{
		this.showStatementFromHistory(this.currentHistoryEntry + 1);
	}
	
	public synchronized void showPrevStatement()
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
		this.checkStatementActions();
	}
	

	/*
	public void setStatusbar(WindowStatusBar aStatusBar)
	{
		this.winStatusBar = aStatusBar;
	}
	*/
	public void storeSettings()
	{
		// Divider location will only be saved for the first panel...
		if (this.internalId == 0)
		{
			WbManager.getSettings().setSqlDividerLocation(this.contentPanel.getDividerLocation());
		}
		
		try
		{
			WbPersistence.writeObject(this.statementHistory, this.historyFilename);
		}
		catch (Exception e)
		{
			LogMgr.logWarning(this, "Error storing statement history", e);
		}
	}
	
	public void setConnection(WbConnection aConnection)
	{
		this.result.setConnection(aConnection);
		this.initStatementHistory();
		this.executeAll.setEnabled(true);
		this.executeSelected.setEnabled(true);
		this.updateAction.setEnabled(false);
		this.findAgainAction.setEnabled(false);
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
		this.showStatusMessage(ResourceMgr.getString("CancellingStmt"));
		this.result.cancelExecution();
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
	
	public void runSql()
	{
		this.setCancelState(true);
		this.setBusy(true);
		//this.editor.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		
		this.showStatusMessage(ResourceMgr.getString(ResourceMgr.MSG_EXEC_SQL));

		String sql;
		
		if (selected) 
		{
			sql = this.editor.getSelectedStatement();
			if (sql == null) 
			{
				sql = this.editor.getStatement();
			}
		}
		else
		{
			sql = this.editor.getStatement();
		}
		
		this.storeInHistory(sql);
		this.result.displayResult(sql);
		this.clearStatusMessage();
		this.setBusy(false);
		this.setCancelState(false);
		this.setActionState(this.findAction, this.result.getRowCount() > 0);
		this.setActionState(this.updateAction, this.result.isUpdateable());
		//this.editor.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
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

	private synchronized void setBusy(boolean value)
	{
		this.threadBusy = value;
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


	/**
	 *	Display a message in the message area of the result display
	 */
	public void showLogMessage(String aMsg)
	{
		this.result.showLogMessage(aMsg);
	}

	public synchronized void clearStatusMessage()
	{
		this.result.clearStatusMessage();
	}		
	/**
	 *	Display a message in the status bar
	 */
	public synchronized void showStatusMessage(String aMsg)
	{
		this.result.setStatusMessage(aMsg);
	}
	
	public Component getFocusComponent()
	{
		return this.editor;
	}
	
	/** This fine grain notification tells listeners the exact range
	 * of cells, rows, or columns that changed.
	 *
	 */
	public void tableChanged(TableModelEvent e)
	{
		this.updateAction.setEnabled(true);
	}
	
}
