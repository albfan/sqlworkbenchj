/*
 * SqlResultDisplay.java
 *
 * Created on November 30, 2001, 11:02 PM
 */

package workbench.gui.sql;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.event.TableModelListener;
import workbench.WbManager;
import workbench.db.WbConnection;
import workbench.interfaces.Exporter;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;


/**
 *
 * @author  thomas.kellerer@web.de
 * @version
 */
public class SqlResultDisplay extends JPanel implements Exporter
{
	private DwPanel data;
	private JTextArea log;
	private JTabbedPane tab;
	private WbConnection dbConnection;
	
	public SqlResultDisplay()
	{
		this.tab = new JTabbedPane();
		this.tab.setTabPlacement(JTabbedPane.TOP);
		this.tab.setDoubleBuffered(true);
		this.log = new JTextArea();
		this.log.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		this.log.setFont(WbManager.getSettings().getMsgLogFont());
		this.log.setEditable(false);
		this.data = new DwPanel();
		this.tab.addTab(ResourceMgr.getString(ResourceMgr.TAB_LABEL_RESULT), data);
		this.tab.addTab(ResourceMgr.getString(ResourceMgr.TAB_LABEL_MSG), log);
		this.setLayout(new BorderLayout());
		this.add(tab, BorderLayout.CENTER);
		this.setPreferredSize(null);
		this.tab.setBorder(null);
		this.setBorder(null);
		this.showResultPanel();
	}
	
	public void setConnection(WbConnection aConnection)
	{
		this.dbConnection = aConnection;
		try
		{
			this.data.setConnection(this.dbConnection);
		}
		catch (Exception e)
		{
			LogMgr.logError(this, "Error when setting connection for SqlResultDisplay", e);
		}
	}
	
	public WbConnection getConnection()
	{
		return this.dbConnection;
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
		this.tab.setSelectedIndex(1);
	}

	public void showResultPanel()
	{
		this.tab.setSelectedIndex(0);
	}

	public void cancelExecution()
	{
		this.data.cancelExecution();
	}
	
	public void setStatusMessage(String aMsg)
	{
		this.data.setStatusMessage(aMsg);
	}
	
	public void clearStatusMessage()
	{
		this.data.clearStatusMessage();
	}	
	
	public void saveChangesToDatabase()
	{
		try
		{
			this.log.setText(ResourceMgr.getString("UpdatingDatabase"));
			this.data.saveChangesToDatabase();
			this.log.append("\r\n" + ResourceMgr.getString("SuccessfullUpdate"));
		}
		catch (SQLException e)
		{
			this.showLogMessage(this.data.getLastMessage());
		}
	}
	
	public void displayResult(String aSqlStatement)
	{
		try
		{
			this.log.setText(ResourceMgr.getString("ExecutingSql"));
			this.data.setSqlStatement(aSqlStatement);
			this.data.runStatement();
			this.log.setText(this.data.getLastMessage());
			if (this.data.hasResultSet()) 
			{
				this.showResultPanel();
			}
			else
			{
				this.showLogPanel();
			}
		}
		catch (SQLException e)
		{
			this.showLogMessage(this.data.getLastMessage());
		}
		catch (Exception e)
		{
			this.showLogMessage(this.data.getLastMessage());
			LogMgr.logError(this, "Error executing statement", e);
		}
	}
	
	public void saveAsAscii()
	{
		try
		{
			String lastDir = WbManager.getSettings().getLastExportDir();
			JFileChooser fc = new JFileChooser(lastDir);
			int answer = fc.showSaveDialog(SwingUtilities.getWindowAncestor(this));
			if (answer == JFileChooser.APPROVE_OPTION)
			{
				File fl = fc.getSelectedFile();
				this.saveAsAscii(fl.getAbsolutePath());
				lastDir = fc.getCurrentDirectory().getAbsolutePath();
				WbManager.getSettings().setLastExportDir(lastDir);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	public void addTableModelListener(TableModelListener aListener)
	{
		this.data.getTable().addTableModelListener(aListener);
	}

	public void removeTableModelListener(TableModelListener aListener)
	{
		this.data.getTable().removeTableModelListener(aListener);
	}
	

	public boolean isUpdateable() 
	{
		return this.data.isUpdateable();
	}
	
	public int getRowCount()
	{
		return this.data.getTable().getRowCount();
	}
	
	public int search(String aCriteria)
	{
		return this.data.getTable().search(aCriteria, false);
	}
	
	public int searchNext()
	{
		return this.data.getTable().searchNext();
	}
	
	public void saveAsAscii(String aFilename)
		throws IOException
	{
		this.data.getTable().saveAsAscii(aFilename);
	}
	
	public void copyDataToClipboard()
	{
		try
		{
			String data = this.data.getTable().getDataString("\r");
			Clipboard clp = Toolkit.getDefaultToolkit().getSystemClipboard();
			StringSelection sel = new StringSelection(data);
			clp.setContents(sel, sel);
		}
		catch (Exception e)
		{
			LogMgr.logError(this, "Could not access clipboard", e);
		}
	}
	
}
