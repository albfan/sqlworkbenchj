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
import workbench.WbManager;
import workbench.db.DbMetadata;
import workbench.db.WbConnection;
import workbench.gui.components.WbTable;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;


/**
 *
 * @author  sql.workbench@freenet.de
 *	
 */
public class TableListPanel extends JPanel implements ListSelectionListener
{
	private WbConnection dbConnection;
	private WbTable tableList;
	private WbTable tableDefinition;
	private WbTable indexes;
	private JTabbedPane displayTab;
	private JSplitPane splitPane;
	private DbMetadata meta;
	private Object retrieveLock = new Object();
	
	public TableListPanel(WbConnection aConnection)
		throws Exception
	{
		this.displayTab = new JTabbedPane();
		this.displayTab.setTabPlacement(JTabbedPane.BOTTOM);
		this.tableDefinition = new WbTable();
		this.tableDefinition.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		this.tableDefinition.setAdjustToColumnLabel(false);
		
		JScrollPane scroll = new JScrollPane(this.tableDefinition);
		this.displayTab.add(ResourceMgr.getString("TxtDbExplorerTableDefinition"), scroll);
		
		this.indexes = new WbTable();
		scroll = new JScrollPane(this.indexes);
		this.displayTab.add(ResourceMgr.getString("TxtDbExplorerIndexes"), scroll);
		
		this.tableList = new WbTable();
		this.tableList.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		this.tableList.setCellSelectionEnabled(false);
		this.tableList.setColumnSelectionAllowed(false);
		this.tableList.setRowSelectionAllowed(true);
		this.tableList.getSelectionModel().addListSelectionListener(this);
		this.tableList.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		this.tableList.setAdjustToColumnLabel(false);
		this.splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		scroll = new JScrollPane(this.tableList);
		this.splitPane.setLeftComponent(scroll);
		this.splitPane.setRightComponent(displayTab);
		this.setLayout(new BorderLayout());
		//splitPane.setDividerLocation(200);
		splitPane.setDividerSize(5);
		this.add(splitPane, BorderLayout.CENTER);
		this.setConnection(aConnection);
	}

	public void setConnection(WbConnection aConnection)
		throws Exception
	{
		this.dbConnection = aConnection;
		this.meta = aConnection.getMetadata();
		this.tableList.setModel(meta.getListOfTables(), true);
		this.tableList.adjustColumns();
		this.tableDefinition.setModel(new DefaultTableModel(), false);
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
		this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		try
		{
			int row = this.tableList.getSelectedRow();
			if (row >= 0)
			{
				int col = this.tableList.getColumn("TABLE_NAME").getModelIndex();
				final String table = this.tableList.getValueAsString(row, col);
				col = this.tableList.getColumn("TABLE_SCHEM").getModelIndex();
				final String schema = this.tableList.getValueAsString(row, col);
				col = this.tableList.getColumn("TABLE_CAT").getModelIndex();
				final String catalog = this.tableList.getValueAsString(row, col);
				
				EventQueue.invokeLater(new Runnable() 
				{
					public void run()
					{
						synchronized (retrieveLock)
						{
							splitPane.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
							try
							{
								tableDefinition.setModel(meta.getTableDefinitionModel(catalog, schema, table), true);
								tableDefinition.adjustColumns();
							}
							catch (Exception ex)
							{
								LogMgr.logError(this, "Could not read table definition", ex);
							}
							try
							{
								indexes.setModel(meta.getTableIndexes(catalog, schema, table));
								tableDefinition.adjustColumns();
							}
							catch (Exception ex)
							{
								// ignore errors for index retrieval
								// they might be due to privileges missing
							}
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
		this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
	
	
	public static void main(String args[])
	{
		try
		{
			//JFrame f = new JFrame("Test");
			Class.forName("com.inet.tds.TdsDriver");
			Class.forName("oracle.jdbc.OracleDriver");
			//final Connection con = DriverManager.getConnection("jdbc:inetdae:demsqlvisa02:1433?database=visa_cpl_test", "visa", "savivisa");
			final Connection con = DriverManager.getConnection("jdbc:oracle:thin:@DEMRDB34:1521:SBL1", "sadmin", "sadmin");
			/*
			WbConnection wb = new WbConnection(con);
			TableListPanel p = new TableListPanel(wb);
			f.addWindowListener(new WindowAdapter()
			{
				public void windowClosing(WindowEvent e)
				{
					try { con.close(); } catch (Throwable th) {}
					System.exit(0);
				}
			});
			
			f.getContentPane().add(p);
			f.setSize(600,400);
			f.show();
			*/
			DatabaseMetaData meta = con.getMetaData();
			String schema = meta.getSchemaTerm();
			ResultSet schemas = meta.getSchemas();
			
			while (schemas.next())
			{
				System.out.println(schema + "=" + schemas.getString(1));
				//System.out.println("TABLE_CATALOG=" + schemas.getString(2));
			}			
			schemas.close();
			String catName = meta.getCatalogTerm();
			System.out.println("Current " + catName + "=" + con.getCatalog());
			ResultSet catalogs = meta.getCatalogs();
			while (catalogs.next())
			{
				System.out.println(catName + "=" + catalogs.getString(1));
			}
			catalogs.close();
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
}
