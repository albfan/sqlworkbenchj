/*
 * Created on 5. August 2002, 21:06
 */
package workbench.gui.dbobjects;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import workbench.WbManager;

import workbench.db.WbConnection;
import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.ReloadAction;
import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.WbTable;
import workbench.interfaces.Reloadable;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;


/**
 *
 * @author  workbench@kellerer.org
 *
 */
public class TableDataPanel
  extends JPanel
	implements Reloadable
{
	private WbConnection dbConnection;
	private WbTable dataTable;

	private Object retrieveLock = new Object();

	private JTextField maxRowField;
	private JLabel rowCountLabel;
	private JLabel maxRowsLabel;
	private JCheckBox autoRetrieve;

	private String catalog;
	private String schema;
	private String tableName;

	public TableDataPanel() throws Exception
	{
		this.setBorder(WbSwingUtilities.EMPTY_BORDER);
		this.setLayout(new BorderLayout());
    JPanel topPanel = new JPanel();
		topPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

		ReloadAction a = new ReloadAction(this);
		JButton b = a.getToolbarButton();
		b.setBorder(new EtchedBorder());
		b.setToolTipText(ResourceMgr.getDescription("TxtLoadTableData"));
		topPanel.add(b);

		autoRetrieve = new JCheckBox(ResourceMgr.getString("LabelAutoRetrieveTableData"));
		topPanel.add(autoRetrieve);

		rowCountLabel = new JLabel(ResourceMgr.getString("LabelTableDataRowCount"));
		Font std = WbManager.getSettings().getStandardFont();
		Font bold = std.deriveFont(Font.BOLD);
		rowCountLabel.setFont(bold);
		topPanel.add(rowCountLabel);


		JLabel dummy = new JLabel(" ");
		dummy.setMinimumSize(new Dimension(50,0));
		topPanel.add(dummy);

		maxRowsLabel = new JLabel(ResourceMgr.getString("LabelTableDataMaxRows"));
		topPanel.add(maxRowsLabel);

		this.maxRowField = new JTextField(10);
		topPanel.add(this.maxRowField);

		this.dataTable = new WbTable();
		this.add(topPanel, BorderLayout.NORTH);
		JScrollPane scroll = new JScrollPane(this.dataTable);
		this.add(scroll, BorderLayout.CENTER);
	}

	public void disconnect()
	{
		this.dbConnection = null;
		this.reset();
	}

	private int getMaxRows()
	{
		int rows;
		try
		{
			rows = Integer.parseInt(this.maxRowField.getText());
		}
		catch (Exception e)
		{
			rows = 0;
		}
		finally
		{
		}
		return rows;
	}

	public void reset()
	{
		if (this.dataTable != null) this.dataTable.reset();
	}

	public void setConnection(WbConnection aConnection)
	{
		this.dbConnection = aConnection;
	}

	public void showRowCount()
	{
		if (this.dbConnection == null) return;
		String sql = this.buildSqlForTable(true);
		if (sql == null) return;

		Statement stmt = null;
		ResultSet rs = null;
		long rowCount = 0;
		try
		{
			stmt = this.dbConnection.createStatement();
			rs = stmt.executeQuery(sql);
			if (rs.next())
			{
				rowCount = rs.getLong(1);
			}
			this.rowCountLabel.setText(ResourceMgr.getString("LabelTableDataRowCount") + " " + rowCount);
		}
		catch (Exception e)
		{
			LogMgr.logError("TableDataPanel.showRowCount()", "Error retrieving rowcount for " + this.tableName, e);
		}
		finally
		{
			try { rs.close(); } catch (Throwable th) {}
			try { stmt.close(); } catch (Throwable th) {}
		}
	}

	public void setTable(String aCatalog, String aSchema, String aTable)
	{
		this.reset();
		this.schema = aSchema;
		this.catalog = aCatalog;
    this.tableName = aTable;
	}

	private String buildSqlForTable(boolean forRowCount)
	{
		if (this.tableName == null || this.tableName.length() == 0) return null;
		String table = SqlUtil.quoteObjectname(this.tableName);

		StringBuffer sql = new StringBuffer(100);
		if (forRowCount)
			sql.append("SELECT COUNT(*) FROM ");
		else
			sql.append("SELECT * FROM ");
		if (this.schema != null && this.schema.trim().length() > 0)
		{
			sql.append(SqlUtil.quoteObjectname(this.schema));
			sql.append(".");
		}
		sql.append(table);

		return sql.toString();
	}

	public void retrieve()
	{
    final String sql = this.buildSqlForTable(false);
    if (sql == null) return;

		final Container parent = this.getParent();
		final int maxRows = this.getMaxRows();

		new Thread(new Runnable()
		{
			public void run()
			{
				try
				{
					synchronized (retrieveLock)
					{
            Statement stmt = dbConnection.createStatement();
						ResultSet rs = null;
						try
						{
							WbSwingUtilities.showWaitCursor(parent);
							reset();
							stmt.setMaxRows(maxRows);
							rs = stmt.executeQuery(sql);
							DataStore ds = new DataStore(rs, true);
							DataStoreTableModel model = new DataStoreTableModel(ds);
							dataTable.setModel(model, true);
						}
						catch (Exception e)
						{
							LogMgr.logError("TableDataPanel.retrieve()", "Error retrieving data for " + tableName, e);
						}
						finally
						{
							WbSwingUtilities.showDefaultCursor(parent);
							try { rs.close(); } catch (Throwable th) {}
							try { stmt.close(); } catch (Throwable th) {}
						}
					}
				}
				catch (Throwable e)
				{
					LogMgr.logError("TableListPanel.retrieve()", "Error retrieving table list", e);
				}
			}
		}).start();
	}

	public void saveSettings()
	{
		WbManager.getSettings().setProperty(TableDataPanel.class.getName(), "maxrows", this.maxRowField.getText());
		String auto = Boolean.toString(this.autoRetrieve.isSelected());
		WbManager.getSettings().setProperty(TableDataPanel.class.getName(), "autoretrieve", auto);
	}

	public void restoreSettings()
	{
		this.maxRowField.setText(WbManager.getSettings().getProperty(TableDataPanel.class.getName(), "maxrows", "0"));
		boolean auto = "true".equals(WbManager.getSettings().getProperty(TableDataPanel.class.getName(), "autoretrieve", "false"));
		this.autoRetrieve.setSelected(auto);
	}

	public void showData()
	{
		this.showRowCount();
		if (this.autoRetrieve.isSelected())
		{
			this.reload();
		}
	}

	public void reload()
	{
		this.reset();
		this.retrieve();
	}

	public Window getParentWindow()
	{
		return SwingUtilities.getWindowAncestor(this);
	}

}