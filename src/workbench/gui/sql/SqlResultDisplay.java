/*
 * SqlResultDisplay.java
 *
 * Created on November 30, 2001, 11:02 PM
 */

package workbench.gui.sql;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelListener;
import workbench.WbManager;
import workbench.db.WbConnection;
import workbench.gui.WbSwingUtilities;
import workbench.interfaces.Exporter;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.util.SqlUtil;


/**
 *
 * @author  sql.workbench@freenet.de
 * @version
 */
public class SqlResultDisplay extends JPanel implements Exporter
{
	DwPanel data;
	private JTextArea log;
	private JTabbedPane tab;
	private WbConnection dbConnection;

	public SqlResultDisplay()
	{
		this.tab = new JTabbedPane();
		this.tab.setTabPlacement(JTabbedPane.TOP);
		this.tab.setDoubleBuffered(true);
		this.tab.setBorder(WbSwingUtilities.EMPTY_BORDER);
		
		this.log = new JTextArea();
		//this.log.setBorder(WbSwingUtilities.EMPTY_BORDER);
		this.log.setBorder(new EmptyBorder(0,2,0,0));
		this.log.setFont(WbManager.getSettings().getMsgLogFont());
		this.log.setEditable(false);
		this.log.setLineWrap(true);
		this.log.setWrapStyleWord(true);
		//this.log.setMargin(new Insets(0, 10, 0, 0));
		
		this.data = new DwPanel();
		this.data.setBorder(WbSwingUtilities.EMPTY_BORDER);
		this.tab.addTab(ResourceMgr.getString(ResourceMgr.TAB_LABEL_RESULT), data);
		JScrollPane scroll = new JScrollPane(log);
		this.tab.addTab(ResourceMgr.getString(ResourceMgr.TAB_LABEL_MSG), scroll);
		this.tab.setBorder(WbSwingUtilities.EMPTY_BORDER);
		
		this.setBorder(WbSwingUtilities.EMPTY_BORDER);
		this.setLayout(new BorderLayout());
		this.add(tab, BorderLayout.CENTER);
		this.setPreferredSize(null);
		this.showResultPanel();
	}

	public void setConnection(WbConnection aConnection)
	{
		this.dbConnection = aConnection;
		this.clearLog();
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
			this.log.setText(ResourceMgr.getString("MsgUpdatingDatabase"));
			long start, end;
			int rows = this.data.saveChanges(this.dbConnection);
			this.log.append(this.data.getLastMessage());
		}
		catch (Exception e)
		{
			this.showLogMessage(this.data.getLastMessage());
		}
	}

	public void displayResult(String aSqlScript)
	{
		try
		{
			this.log.setText(ResourceMgr.getString(ResourceMgr.MSG_EXEC_SQL));
			List sqls = SqlUtil.getCommands(aSqlScript, ";");
			int count = sqls.size();
			this.log.setText("");
			for (int i=0; i < count; i++)
			{
				String sql = (String)sqls.get(i);
				this.data.runStatement(sql);
				this.log.append(this.data.getLastMessage());
				if (i < count - 1)
				{
					this.log.append("\r\n");
				}
				if (i == 0 && !this.data.hasResultSet())
				{
					this.showLogPanel();
				}
			}
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


	public boolean checkUpdateTable()
	{
		return this.data.checkUpdateTable();
	}
	public void setUpdateTable(String aTable)
	{
		this.data.setUpdateTable(aTable);
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
		Window parent = SwingUtilities.getWindowAncestor(this);
		try
		{
			parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			String data = this.data.getTable().getDataString("\r");
			Clipboard clp = Toolkit.getDefaultToolkit().getSystemClipboard();
			StringSelection sel = new StringSelection(data);
			clp.setContents(sel, sel);
			parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
		catch (Exception e)
		{
			LogMgr.logError(this, "Could not access clipboard", e);
		}
	}

}
