/*
 * Created on 5. August 2002, 21:06
 */
package workbench.gui.dbobjects;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;

import workbench.WbManager;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.ReloadAction;
import workbench.gui.actions.StopAction;
import workbench.gui.components.WbButton;
import workbench.gui.components.WbToolbar;
import workbench.gui.sql.DwPanel;
import workbench.interfaces.Interruptable;
import workbench.interfaces.Reloadable;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.util.SqlUtil;



/**
 *
 * @author  workbench@kellerer.org
 *
 */
public class TableDataPanel
  extends JPanel
	implements Reloadable, ActionListener, Interruptable
{
	private WbConnection dbConnection;
	private DwPanel dataDisplay;
	//private WbTable dataTable;

	private Object retrieveLock = new Object();
	private ReloadAction reloadAction;

	private JButton config;
	//private JTextField maxRowField;
	private JLabel rowCountLabel;
	//private JLabel maxRowsLabel;
	private JCheckBox autoRetrieve;

	private long warningThreshold = -1;

	private boolean shiftDown = false;

	private String catalog;
	private String schema;
	private String tableName;
	private ImageIcon loadingIcon;
	private Image loadingImage;

	private StopAction cancelRetrieve;

	public TableDataPanel() throws Exception
	{
		this.setBorder(WbSwingUtilities.EMPTY_BORDER);
		this.setLayout(new BorderLayout());

		this.dataDisplay = new DwPanel();
		this.dataDisplay.setManageActions(true);
		this.dataDisplay.setShowLoadProcess(true);
		this.dataDisplay.setDefaultStatusMessage("");
		this.dataDisplay.setShowErrorMessages(true);
		this.dataDisplay.getTable().setMaxColWidth(WbManager.getSettings().getMaxColumnWidth());
		this.dataDisplay.getTable().setMinColWidth(WbManager.getSettings().getMinColumnWidth());
		this.dataDisplay.setSaveChangesInBackground(true);

    JPanel topPanel = new JPanel();
		topPanel.setMaximumSize(new Dimension(32768, 32768));
		//topPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
		BoxLayout box = new BoxLayout(topPanel, BoxLayout.X_AXIS);
		topPanel.setLayout(box);

		this.reloadAction = new ReloadAction(this);
		this.reloadAction.setTooltip(ResourceMgr.getDescription("TxtLoadTableData"));

		WbToolbar toolbar = new WbToolbar();
		toolbar.addDefaultBorder();
		topPanel.add(toolbar);
		toolbar.add(this.reloadAction);
		toolbar.addSeparator();

		this.cancelRetrieve = new StopAction(this);
		this.cancelRetrieve.setEnabled(false);
		toolbar.add(this.cancelRetrieve);
		toolbar.addSeparator();

		//JButton b = a.getToolbarButton();
		//b.setBorder(new EtchedBorder());
		//b.setToolTipText(ResourceMgr.getDescription("TxtLoadTableData"));
		//topPanel.add(b);

		topPanel.add(Box.createHorizontalStrut(15));

		autoRetrieve = new JCheckBox(ResourceMgr.getString("LabelAutoRetrieveTableData"));
		autoRetrieve.setToolTipText(ResourceMgr.getDescription("LabelAutoRetrieveTableData"));
		autoRetrieve.setHorizontalTextPosition(SwingConstants.LEFT);
		topPanel.add(autoRetrieve);

		topPanel.add(Box.createHorizontalStrut(10));

		rowCountLabel = new JLabel(ResourceMgr.getString("LabelTableDataRowCount"));
		rowCountLabel.setToolTipText(ResourceMgr.getDescription("LabelTableDataRowCount"));
		Font std = WbManager.getSettings().getStandardFont();
		Font bold = std.deriveFont(Font.BOLD);
		rowCountLabel.setFont(bold);
		rowCountLabel.setHorizontalTextPosition(SwingConstants.LEFT);
		topPanel.add(rowCountLabel);

		topPanel.add(Box.createHorizontalStrut(10));

		topPanel.add(Box.createHorizontalGlue());
		this.config = new WbButton(ResourceMgr.getString("LabelConfigureWarningThreshold"));
		this.config.addActionListener(this);
		Border border = new CompoundBorder(new EtchedBorder(), new EmptyBorder(1,6,1,6));
		this.config.setBorder(border);
		topPanel.add(this.config);

		//maxRowsLabel = new JLabel(ResourceMgr.getString("LabelTableDataMaxRows"));
		//topPanel.add(maxRowsLabel);

		//this.maxRowField = new JTextField(4);
		//topPanel.add(this.maxRowField);

		this.add(topPanel, BorderLayout.NORTH);

		toolbar.add(this.dataDisplay.getUpdateDatabaseAction());
		toolbar.addSeparator();
		toolbar.add(this.dataDisplay.getInsertRowAction());
		toolbar.add(this.dataDisplay.getCopyRowAction());
		toolbar.add(this.dataDisplay.getDeleteRowAction());

		//this.dataTable = this.dataDisplay.getTable();
		//JScrollPane scroll = new JScrollPane(this.dataTable);
		this.add(dataDisplay, BorderLayout.CENTER);
	}

	private ImageIcon getLoadingIndicator()
	{
		if (this.loadingIcon == null)
		{
			this.loadingImage = ResourceMgr.getPicture("wait").getImage();
			this.loadingIcon = new ImageIcon(this.loadingImage);
		}
		return this.loadingIcon;
	}

	public void disconnect()
	{
		this.dbConnection = null;
		this.reset();
	}

	private int getMaxRows()
	{
		return this.dataDisplay.getMaxRows();
	}

	public void reset()
	{
		this.dataDisplay.clearContent();
	}

	public void setConnection(WbConnection aConnection)
	{
		this.dbConnection = aConnection;
		try
		{
			this.dataDisplay.setConnection(aConnection);
		}
		catch (Throwable th)
		{
			LogMgr.logError("TableDataPanel.setConnection()", "Error when setting connection", th);
		}
	}

	public long showRowCount()
	{
		if (this.dbConnection == null) return -1;

		this.rowCountLabel.setText(ResourceMgr.getString("LabelTableDataRowCount"));
		this.rowCountLabel.setIcon(this.getLoadingIndicator());

		this.reloadAction.setEnabled(false);
		this.dataDisplay.setStatusMessage(ResourceMgr.getString("MsgCalculatingRowCount"));
		this.repaint();
		this.rowCountLabel.repaint();
		String sql = this.buildSqlForTable(true);
		if (sql == null) return -1;

		long rowCount = 0;
		Statement stmt = null;
		ResultSet rs = null;

		try
		{
			WbSwingUtilities.showWaitCursor(this);

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
			this.rowCountLabel.setText(ResourceMgr.getString("LabelTableDataRowCount") + " " + ResourceMgr.getString("TxtError"));
			LogMgr.logError("TableDataPanel.showRowCount()", "Error retrieving rowcount for " + this.tableName, e);
		}
		finally
		{
			this.dataDisplay.setStatusMessage("");
			if (this.loadingImage != null) this.loadingImage.flush();
			this.rowCountLabel.setIcon(null);
			try { rs.close(); } catch (Throwable th) {}
			try { stmt.close(); } catch (Throwable th) {}
			this.reloadAction.setEnabled(true);
			WbSwingUtilities.showDefaultCursor(this);
		}
		return rowCount;
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
		//if (this.currentTable == null) return null;

		if (this.tableName == null || this.tableName.length() == 0) return null;
		String table = this.dbConnection.getMetadata().quoteObjectname(this.tableName);

		StringBuffer sql = new StringBuffer(100);
		if (forRowCount)
			sql.append("SELECT COUNT(*) FROM ");
		else
			sql.append("SELECT * FROM ");

		if (this.schema != null && this.schema.trim().length() > 0)
		{
			if (!this.dbConnection.getMetadata().isOracle() || (this.dbConnection.getMetadata().isOracle() && !"PUBLIC".equalsIgnoreCase(this.schema)))
			{
				sql.append(this.dbConnection.getMetadata().quoteObjectname(this.schema));
				sql.append(".");
			}
		}
		sql.append(table);

		//sql.append(this.currentTable.getTableExpression());
		//LogMgr.logDebug("TableDataPanel.buildSql()", "Using query=" + sql);
		return sql.toString();
	}

	public boolean confirmCancel() { return true; }
	public void cancelExecution()
	{
		Thread t = new Thread()
		{
			public void run()
			{
				try
				{
					dataDisplay.cancelExecution();
				}
				finally
				{
					cancelRetrieve.setEnabled(false);
					WbSwingUtilities.showDefaultCursor(dataDisplay);
				}
			}
		};
		t.setName("TableDataPanel cancel thread");
		t.setDaemon(true);
		t.start();
	}


	public synchronized void retrieve()
	{
    final String sql = this.buildSqlForTable(false);
    if (sql == null) return;

		final int maxRows = this.getMaxRows();

		this.cancelRetrieve.setEnabled(true);
		this.reloadAction.setEnabled(false);
		Thread t = new Thread()
		{
			public void run()
			{
				try
				{
					WbSwingUtilities.showWaitCursor(dataDisplay);
					dataDisplay.setShowErrorMessages(true);
					dataDisplay.scriptStarting();
					dataDisplay.setMaxRows(maxRows);
					dataDisplay.runStatement(sql);
					String header = ResourceMgr.getString("TxtTableDataPrintHeader") + " " + tableName;
					dataDisplay.setPrintHeader(header);
					dataDisplay.setStatusMessage("");
				}
				catch (OutOfMemoryError mem)
				{
					WbManager.getInstance().showErrorMessage(TableDataPanel.this, ResourceMgr.getString("MsgOutOfMemoryError"));
				}
				catch (Throwable e)
				{
					LogMgr.logError("TableDataPanel.retrieve()", "Error retrieving table data", e);
				}
				finally
				{
					WbSwingUtilities.showDefaultCursor(dataDisplay);
					dataDisplay.scriptFinished();
					cancelRetrieve.setEnabled(false);
					reloadAction.setEnabled(true);
				}
			}
		};
		t.setName("TableDataPanel retrieve thread");
		t.setDaemon(true);
		t.start();
	}

	public void saveSettings()
	{
		WbManager.getSettings().setProperty(TableDataPanel.class.getName(), "maxrows", this.getMaxRows());
		String auto = Boolean.toString(this.autoRetrieve.isSelected());
		WbManager.getSettings().setProperty(TableDataPanel.class.getName(), "autoretrieve", auto);
		WbManager.getSettings().setProperty(TableDataPanel.class.getName(), "warningthreshold", Long.toString(this.warningThreshold));
	}

	public void restoreSettings()
	{
		int max = WbManager.getSettings().getIntProperty(TableDataPanel.class.getName(), "maxrows", 500);
		this.dataDisplay.setMaxRows(max);
		boolean auto = "true".equals(WbManager.getSettings().getProperty(TableDataPanel.class.getName(), "autoretrieve", "false"));
		this.autoRetrieve.setSelected(auto);

		try
		{
			String v = WbManager.getSettings().getProperty(TableDataPanel.class.getName(), "warningthreshold", "1500");
			this.warningThreshold = Long.parseLong(v);
		}
		catch (Exception e)
		{
			this.warningThreshold = -1;
		}
	}

	public void showData()
	{
		this.showData(true);
	}

	public void showData(boolean includeData)
	{
		this.reset();
		long rows = this.showRowCount();
		if (this.autoRetrieve.isSelected() && includeData)
		{
			int max = this.getMaxRows();
			if ( this.warningThreshold > 0 &&
			     rows > this.warningThreshold &&
			     (max > this.warningThreshold || max == 0)
				 )
			{
				String msg = ResourceMgr.getString("MsgDataDisplayWarningThreshold");
				msg = msg.replaceAll("%rows%", Long.toString(rows));
				int choice = JOptionPane.showConfirmDialog(this, msg, ResourceMgr.TXT_PRODUCT_NAME, JOptionPane.YES_NO_OPTION);
				if (choice == JOptionPane.NO_OPTION) return;
			}
			this.retrieve();
		}
	}

	public void reload()
	{
		this.reset();
		this.showRowCount();
		this.retrieve();
	}

	public Window getParentWindow()
	{
		return SwingUtilities.getWindowAncestor(this);
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == this.config)
		{
			ConfigureWarningThreshold p = new ConfigureWarningThreshold();
			p.setThresholdValue(this.warningThreshold);
			Window parent = SwingUtilities.getWindowAncestor(this);
			int choice = JOptionPane.showConfirmDialog(parent, p, ResourceMgr.getString("LabelConfigureWarningThresholdTitle"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
			if (choice == JOptionPane.OK_OPTION)
			{
				this.warningThreshold = p.getThresholdValue();
			}
		}
	}
	public void setReadOnly(boolean aFlag)
	{
		this.dataDisplay.setReadOnly(aFlag);
	}

}