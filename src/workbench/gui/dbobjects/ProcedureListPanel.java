/*
 * Created on 5. August 2002, 21:06
 */
package workbench.gui.dbobjects;

import java.awt.BorderLayout;
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
	private TableModel emptyModel = new DefaultTableModel();
	
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
		this.procList.setModel(new DefaultTableModel(), false);
		this.procColumns.setModel(new DefaultTableModel(), false);
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
		this.procList.setModel(this.meta.getListOfProcedures(aCatalog, aSchema), true);
		this.procList.adjustColumns();
		this.procColumns.setModel(new DefaultTableModel(), false);
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
		
		try
		{
			int row = this.procList.getSelectedRow();
			if (row >= 0)
			{
				int col = this.procList.getColumn("TYPE").getModelIndex();
				final String type = this.procList.getValueAsString(row, 3);
				
				final String proc = this.procList.getValueAsString(row, 2);
				final String schema = this.procList.getValueAsString(row, 1);
				final String catalog = this.procList.getValueAsString(row, 0);
				
				EventQueue.invokeLater(new Runnable() 
				{
					public void run()
					{
						synchronized (retrieveLock)
						{
							splitPane.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
							try
							{
								procColumns.setModel(meta.getProcedureColumns(catalog, schema, proc), true);
								procColumns.adjustColumns();
							}
							catch (Exception ex)
							{
								LogMgr.logError(this, "Could not read procedure definition", ex);
								procColumns.setModel(new DefaultTableModel());
							}
							
							try
							{
								String sql = meta.getProcedureSource(catalog, schema, proc);
								source.setText(sql);
							}
							catch (Exception ex)
							{
								LogMgr.logError(this, "Could not read procedure source", ex);
								source.setText(ex.getMessage());
							}
							source.setCaretPosition(0);

							splitPane.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
						}
					}
				});
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
}
