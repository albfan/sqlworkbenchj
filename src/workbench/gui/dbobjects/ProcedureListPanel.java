/*
 * Created on 5. August 2002, 21:06
 */
package workbench.gui.dbobjects;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import workbench.WbManager;
import workbench.db.DbMetadata;
import workbench.db.WbConnection;
import workbench.gui.components.DividerBorder;
import workbench.gui.components.FindPanel;
import workbench.gui.components.WbScrollPane;
import workbench.gui.components.WbSplitPane;
import workbench.gui.components.WbTable;
import workbench.gui.components.WbTraversalPolicy;
import workbench.gui.editor.JEditTextArea;
import workbench.gui.sql.EditorPanel;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;


/**
 *
 * @author  sql.workbench@freenet.de
 *	
 */
public class ProcedureListPanel extends JPanel implements ListSelectionListener
{
	private WbConnection dbConnection;
	private JPanel listPanel;
	private FindPanel findPanel;
	private WbTable procList;
	private WbTable procColumns;
	private EditorPanel source;
	private JTabbedPane displayTab;
	private JSplitPane splitPane;
	private DbMetadata meta;
	private Object retrieveLock = new Object();
	private String currentSchema;
	private String currentCatalog;
	private boolean shouldRetrieve;
	
	public ProcedureListPanel() throws Exception
	{
		this.displayTab = new JTabbedPane();
		this.displayTab.setTabPlacement(JTabbedPane.BOTTOM);
		
		this.procColumns = new WbTable();
		this.procColumns.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		this.procColumns.setCellSelectionEnabled(false);
		this.procColumns.setColumnSelectionAllowed(false);
		this.procColumns.setRowSelectionAllowed(true);
		this.procColumns.getSelectionModel().addListSelectionListener(this);
		this.procColumns.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		this.procColumns.setAdjustToColumnLabel(false);
		JScrollPane scroll = new WbScrollPane(this.procColumns);

		
		this.source = new EditorPanel();
		this.source.setEditable(false);
		
		this.displayTab.add(ResourceMgr.getString("TxtDbExplorerSource"), this.source);
		this.displayTab.add(ResourceMgr.getString("TxtDbExplorerTableDefinition"), scroll);
		
		this.listPanel = new JPanel();
		this.procList = new WbTable();
		this.procList.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		this.procList.setCellSelectionEnabled(false);
		this.procList.setColumnSelectionAllowed(false);
		this.procList.setRowSelectionAllowed(true);
		this.procList.getSelectionModel().addListSelectionListener(this);
		this.procList.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		this.procList.setAdjustToColumnLabel(false);
		
		this.findPanel = new FindPanel(this.procList);
		this.findPanel.toolbar.setBorder(new DividerBorder(DividerBorder.RIGHT));
		this.listPanel.setLayout(new BorderLayout());
		this.listPanel.add(findPanel, BorderLayout.NORTH);
		
		this.splitPane = new WbSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		scroll = new WbScrollPane(this.procList);
		this.listPanel.add(scroll, BorderLayout.CENTER);
		
		this.splitPane.setLeftComponent(this.listPanel);
		this.splitPane.setRightComponent(displayTab);
		this.setLayout(new BorderLayout());
		this.add(splitPane, BorderLayout.CENTER);
		
		WbTraversalPolicy pol = new WbTraversalPolicy();
		pol.setDefaultComponent(findPanel);
		pol.addComponent(this.procList);
		pol.addComponent(this.procColumns);
		this.setFocusTraversalPolicy(pol);
		this.reset();
	}

	public void disconnect()
	{
		this.dbConnection = null;
		this.meta = null;
		this.reset();
	}
	
	public void reset()
	{
		this.procList.reset();
		this.procColumns.reset();
		this.source.setText("");
	}
	
	public void setConnection(WbConnection aConnection)
	{
		this.dbConnection = aConnection;
		this.meta = aConnection.getMetadata();
		this.reset();
	}
	
	public void setCatalogAndSchema(String aCatalog, String aSchema)
		throws Exception
	{
		this.reset();
		this.currentSchema = aSchema;
		this.currentCatalog = aCatalog;
		if (this.isVisible())
			this.retrieve();
		else
			this.shouldRetrieve = true;
	}

	public void retrieve()
	{
		final Container parent = this.getParent();
		
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				synchronized (retrieveLock)
				{
					try
					{
						procList.setVisible(false);
						parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
						procList.setModel(meta.getListOfProcedures(currentCatalog, currentSchema), true);
						procList.adjustColumns();
						parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
						procList.setVisible(true);
						shouldRetrieve = false;
					}
					catch (Exception e)
					{
						LogMgr.logError("ProcedureListPanel.retrieve() thread", "Could not retrieve procedure list", e);
					}
				}
			}
		});
	}
	
	public void setVisible(boolean aFlag)
	{
		super.setVisible(aFlag);
		if (this.shouldRetrieve)
			this.retrieve();
	}
	
	public void saveSettings()
	{
		WbManager.getSettings().setProperty(this.getClass().getName(), "divider", this.splitPane.getDividerLocation());
	}
		
	public void restoreSettings()
	{
		int loc = WbManager.getSettings().getIntProperty(this.getClass().getName(), "divider");
		if (loc == 0) loc = 200;
		this.splitPane.setDividerLocation(loc);
	}
	
	public void valueChanged(ListSelectionEvent e)
	{
		if (e.getValueIsAdjusting()) return;
		final Container parent = this.getParent();
		int row = this.procList.getSelectedRow();
		
		if (row < 0) return;
		
		final String proc = this.procList.getValueAsString(row, DbMetadata.COLUMN_IDX_PROC_LIST_NAME);
		final String schema = this.procList.getValueAsString(row, DbMetadata.COLUMN_IDX_PROC_LIST_SCHEMA);
		final String catalog = this.procList.getValueAsString(row, DbMetadata.COLUMN_IDX_PROC_LIST_CATALOG);

		EventQueue.invokeLater(new Runnable() 
		{
			public void run()
			{
				synchronized (retrieveLock)
				{
					parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					try
					{
						procColumns.setVisible(false);
						procColumns.setModel(meta.getProcedureColumns(catalog, schema, proc), true);
						procColumns.adjustColumns();
						procColumns.setVisible(true);
					}
					catch (Exception ex)
					{
						LogMgr.logError("ProcedureListPanel.valueChanged() thread", "Could not read procedure definition", ex);
						procColumns.reset();
						procColumns.setVisible(true);
					}

					try
					{
						String sql = meta.getProcedureSource(catalog, schema, proc);
						source.setText(sql);
					}
					catch (Exception ex)
					{
						LogMgr.logError("ProcedureListPanel.valueChanged() thread", "Could not read procedure source", ex);
						source.setText(ex.getMessage());
					}
					source.setCaretPosition(0);

					parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				}
			}
		});
	}
	
}
