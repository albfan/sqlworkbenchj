/*
 * RepositoryReporter.java
 *
 * Created on 12. August 2004, 23:09
 */

package workbench.db.report;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.util.Date;
import javax.swing.JFrame;
import workbench.db.DbMetadata;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.gui.WbSwingUtilities;
import workbench.gui.dbobjects.ProgressPanel;
import workbench.interfaces.Interruptable;
import workbench.interfaces.ScriptGenerationMonitor;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.storage.DataStore;
import workbench.util.StrBuffer;
import workbench.util.StrWriter;
import workbench.util.StringUtil;

/**
 * Generate an report from a selection of database tables
 * @author  workbench@kellerer.org
 * 
 */
public class SchemaReporter
	implements Interruptable
{
	private WbConnection dbConn;
	private TableIdentifier[] tables;
	private String xmlNamespace;
	private String[] types = DbMetadata.TABLE_TYPE_TABLE;
	private TagWriter tagWriter = new TagWriter();
	private ScriptGenerationMonitor monitor;
	private String outputfile;
	private boolean cancel = false;
	private ProgressPanel progressPanel;
	private JFrame progressWindow;
	private boolean showProgress = false;
	
	/**
	 * Creates a new SchemaReporter for the supplied connection
	 * @param conn The connection that the schema report should use
	 */
	public SchemaReporter(WbConnection conn)
	{
		this.dbConn = conn;
	}

	public void setProgressMonitor(ScriptGenerationMonitor mon)
	{
		this.monitor = mon;
	}
	
	/**
	 *	Define the list of tables to run the report on
	 */
	public void setTableList(TableIdentifier[] tableList)
	{
		this.tables = tableList;
	}
	
	/**
	 *	Define the table types to be retrieved if no tables
	 *  are specified.
	 */
	public void setTypes(String[] selectedTypes)
	{
		this.types = selectedTypes;
	}

	public void setOutputFilename(String filename)
	{
		this.outputfile = filename;
	}
	
	/**
	 * Return the XML for the (selected) tables
	 * @return The XML for all tables
	 */
	public String getXml()
	{
		this.cancel = false;
		StrWriter out = new StrWriter(5000);
		try
		{
			this.writeXml(out);
		}
		catch (IOException e)
		{
			// Cannot happen with StrWriter
		}
		return out.toString();
	}
	
	public void setShowProgress(boolean flag)
	{
		this.showProgress = flag;
	}
	
	public void writeXml()
		throws IOException
	{
		
		if (this.showProgress)
		{
			this.openProgressMonitor();
		}
		
		BufferedWriter bw = null;
			
		try
		{
			bw = new BufferedWriter(new FileWriter(this.outputfile));
			this.writeXml(bw);
		}
		catch (IOException io)
		{
			LogMgr.logError("WbSchemaReport.execute()", "Error writing report", io);
			throw io;
		}
		finally
		{
			try { bw.close(); } catch (Throwable th) {}
			closeProgress();
		}
	}
	/**
	 *	Write the XML into the supplied output 
	 */
	public void writeXml(Writer out)
		throws IOException
	{
		this.cancel = false;

		if (this.tables == null) this.retrieveTables();
		if (this.cancel) return;
		if (this.tables == null) return;
		
		int count = this.tables.length;
		out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		out.write("<");
		if (this.xmlNamespace != null) 
		{
			out.write(this.xmlNamespace);
			out.write(':');
		}
		out.write("schema-report>\n\n");
		writeReportInfo(out);
		out.write("\n");
		for (int i=0; i < count; i++)
		{
			try
			{
				if (this.cancel) return;
				
				String tableName = tables[i].getTableExpression();
				if (this.monitor != null)
				{
					this.monitor.setCurrentObject(tableName);
				}
				if (this.progressPanel != null)
				{
					this.progressPanel.setInfoText(tableName);
				}
				ReportTable table = new ReportTable(tables[i], this.dbConn, this.xmlNamespace);
				table.writeXml(out);
			}
			catch (Exception e)
			{
				LogMgr.logError("SchemaReporter.writeXml()", "Error writing table: " + tables[i], e);
			}
		}
		out.write("\n");
		out.write("</");
		if (this.xmlNamespace != null) 
		{
			out.write(this.xmlNamespace);
			out.write(':');
		}
		out.write("schema-report>");
		out.flush();
	}
	
	/**
	 *	Writes basic information about the report
	 */
	private void writeReportInfo(Writer out)
		throws IOException
	{
		Date now = new Date(System.currentTimeMillis());
		StrBuffer info = new StrBuffer();
		StrBuffer indent = new StrBuffer("  ");
		info.append(this.dbConn.getDatabaseInfoAsXml(indent, this.xmlNamespace));
		info.writeTo(out);
	}
	
	/**
	 * Return the XML definition for the supplied table
	 * @param tbl The table to get the definition for
	 * @return The XML definition for the table
	 * @throws java.sql.SQLException When retrieving of the table definition fails
	 */
	public String getXmlForTable(TableIdentifier tbl)
		throws SQLException
	{
		ReportTable table = new ReportTable(tbl, this.dbConn, this.xmlNamespace);
		return table.getXml();
	}
	
	/**
	 * Define the namespace for the XML tags
	 * @param name The namespace to be used for the XML tags
	 */
	public void setNamespace(String name) 
	{ 
		this.xmlNamespace = name; 
		this.tagWriter.setNamespace(name);
	}
	
	/**
	 * Get the currently defined namespace
	 * @return The namespace that is used
	 */
	public String getNamespace() 
	{ 
		return this.xmlNamespace; 
	}

	/**
	 * Cancel the current reporting process (this might leave a corrupted XML file)
	 */
	public void cancelExecution()
	{
		this.cancel = true;
		closeProgress();
	}

	public boolean confirmCancel()
	{
		return true;
	}
	
	private void closeProgress()
	{
		if (this.progressWindow != null)
		{
			this.progressWindow.setVisible(false);
			this.progressWindow.dispose();
		}
	}
	private void openProgressMonitor()
	{
		progressPanel = new ProgressPanel(this);
		this.progressPanel.setInfoSize(15);
		this.progressPanel.setRowSize(0);
		this.progressPanel.setFilename(this.outputfile);
		this.progressPanel.setInfoText(ResourceMgr.getString("MsgProcessTable"));

		this.progressWindow = new JFrame();
		this.progressWindow.getContentPane().add(progressPanel);
		this.progressWindow.pack();
		this.progressWindow.setSize(300,120);
		this.progressWindow.setTitle(ResourceMgr.getString("MsgReportWindowTitle"));
		this.progressWindow.setIconImage(ResourceMgr.getPicture("xml16").getImage());
		this.progressWindow.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				cancel = true;
			}
		});

		WbSwingUtilities.center(this.progressWindow, null);
		this.progressWindow.show();
	}
	
	/**
	 *	Retrieve all tables for the current user.
	 *	The "type" of table can be defined by #setTableTypes(String)
	 */
	private void retrieveTables()
	{
		if (this.monitor != null)
		{
			this.monitor.setCurrentObject(ResourceMgr.getString("MsgSchemaReporterRetrievingTables"));
		}
		try
		{
			if (this.cancel) return;
			DataStore ds = this.dbConn.getMetadata().getTables(this.types);
			int count = ds.getRowCount();
			this.tables = new TableIdentifier[count];

			for (int i=0; i < count; i++)
			{
				if (this.cancel) return;
				
				String type = ds.getValueAsString(i, DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE);
				if (type.indexOf("TABLE") > -1)
				{
					String table = ds.getValueAsString(i, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME);
					String catalog = ds.getValueAsString(i, DbMetadata.COLUMN_IDX_TABLE_LIST_CATALOG);
					String schema = ds.getValueAsString(i, DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA);
					this.tables[i] = new TableIdentifier(catalog, schema, table);
				}
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("SchemaReporter.retrieveTables()", "Error retrieving tables", e);
		}
	}
}
