/*
 * SchemaReporter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.report;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JDialog;

import javax.swing.JFrame;

import workbench.db.DbMetadata;
import workbench.db.ProcedureReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.gui.WbSwingUtilities;
import workbench.gui.dbobjects.ProgressPanel;
import workbench.interfaces.Interruptable;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.storage.DataStore;
import workbench.storage.RowActionMonitor;
import workbench.util.StrBuffer;
import workbench.util.StrWriter;
import workbench.util.WbThread;


/**
 * Generate an report from a selection of database tables
 * @author  support@sql-workbench.net
 *
 */
public class SchemaReporter
	implements Interruptable
{
	private WbConnection dbConn;
	private List tables = new ArrayList();
	private List procedures = new ArrayList();
	private String xmlNamespace;
	private String[] types;
	private List schemas;

	private TagWriter tagWriter = new TagWriter();
	private RowActionMonitor monitor;
	private String outputfile;
	private boolean cancel = false;
	private boolean includeTables = true;
	private boolean includeProcedures = false;
	private ProgressPanel progressPanel;
	private JDialog progressWindow;
	private boolean showProgress = false;
	private boolean outputDbDesigner = false;
	private String schemaNameToUse = null;
	private JFrame parentWindow;

	/**
	 * Creates a new SchemaReporter for the supplied connection
	 * @param conn The connection that the schema report should use
	 */
	public SchemaReporter(WbConnection conn)
	{
		this.dbConn = conn;
		types = new String[2];
		types[0] = conn.getMetadata().getTableTypeName();
		types[1] = DbMetadata.TABLE_TYPE_VIEW;
	}

	public void setProgressMonitor(RowActionMonitor mon)
	{
		this.monitor = mon;
	}

	/**
	 *	Define the list of tables to run the report on
	 */
	public void setTableList(TableIdentifier[] tableList)
	{
		this.tables = new ArrayList();
		for (int i=0; i < tableList.length; i++)
		{
			if (tableList[i].getTableName().indexOf('%') > -1)
			{
				List tlist = retrieveWildcardTables(tableList[i].getTableName());
				if (tlist != null)
				{
					this.tables.addAll(tlist);
				}
			}
			else
			{
				this.tables.add(tableList[i]);
			}
		}
	}

	public void setSchemas(List list)
	{
		if (list == null || list.size() == 0) return;
		this.schemas = list;
		this.schemaNameToUse = null;
	}

	public void setSchemaNameToUse(String name)
	{
		if (this.schemas == null || this.schemas.size() == 1)
		{
			this.schemaNameToUse = name;
		}
	}

	public void setIncludeTables(boolean flag)
	{
		this.includeTables = flag;
	}
	
	public void setIncludeProcedures(boolean flag)
	{
		this.includeProcedures = flag;
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

	public void setShowProgress(boolean flag, JFrame parent)
	{
		this.showProgress = flag;
		this.parentWindow = parent;
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
			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.outputfile), "UTF-8"), 16*1024);
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

		if (this.includeTables && this.tables.size() == 0) this.retrieveTables();
		if (this.cancel) return;
		
		if (this.includeProcedures && this.procedures.size() == 0) this.retrieveProcedures();
		if (this.cancel) return;

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

		int count = this.tables.size();
		int totalCount = count + this.procedures.size();
		int totalCurrent = 1;
		
		TableIdentifier table  = null;
		for (int i=0; i < count; i++)
		{
			try
			{
				if (this.cancel) break;

				table = (TableIdentifier)this.tables.get(i);
				String tableName = table.getTableExpression();
				if (this.monitor != null)
				{
					this.monitor.setCurrentObject(tableName, totalCurrent, totalCount);
				}
				if (this.progressPanel != null)
				{
					this.progressPanel.setInfoText(tableName);
				}
				
				String type = table.getType();
				if (type == null)
				{
					type = this.dbConn.getMetadata().getObjectType(table);
				}
				if ("VIEW".equals(type))
				{
					ReportView rview = new ReportView(table, this.dbConn, this.xmlNamespace);
					rview.setSchemaNameToUse(this.schemaNameToUse);
					rview.writeXml(out);
				}
				else
				{
					ReportTable rtable = new ReportTable(table, this.dbConn, this.xmlNamespace, true, true, true, true);
					rtable.setSchemaNameToUse(this.schemaNameToUse);
					rtable.writeXml(out);
					rtable.done();
				}
				out.flush();
				totalCurrent ++;
			}
			catch (Exception e)
			{
				LogMgr.logError("SchemaReporter.writeXml()", "Error writing table: " + table, e);
			}
		}
		count = this.procedures.size();
		if (count > 0) out.write("\n");
		for (int i = 0; i < count; i++)
		{
			ReportProcedure proc = (ReportProcedure)procedures.get(i);
			if (this.monitor != null)
			{
				this.monitor.setCurrentObject(proc.getProcedureName(), totalCurrent, totalCount);
			}
			if (this.progressPanel != null)
			{
				this.progressPanel.setInfoText(proc.getProcedureName());
			}
			proc.writeXml(out);
			out.write('\n');
			totalCurrent ++;
		}
		
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
		StrBuffer info = new StrBuffer();
		StrBuffer indent = new StrBuffer("  ");
		info.append(this.dbConn.getDatabaseInfoAsXml(indent, this.xmlNamespace));
		info.writeTo(out);
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

		this.progressWindow = new JDialog(this.parentWindow, true);
		this.progressWindow.getContentPane().add(progressPanel);
		this.progressWindow.pack();
		this.progressWindow.setSize(300,120);
		WbSwingUtilities.center(progressWindow, parentWindow);
		this.progressWindow.setTitle(ResourceMgr.getString("MsgReportWindowTitle"));
		this.progressWindow.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				cancel = true;
			}
		});

		WbSwingUtilities.center(this.progressWindow, null);
		WbThread t = new WbThread("Reporter Progress")
		{
			public void run()
			{
				progressWindow.setVisible(true);
			}
		};
		t.start();
	}

	private void retrieveTables()
	{
		if (this.monitor != null)
		{
			this.monitor.setCurrentObject(ResourceMgr.getString("MsgSchemaReporterRetrievingTables"), -1, -1);
		}
		if (this.schemas == null || this.schemas.size() == 0)
		{
			this.retrieveTables(null);
		}
		else
		{
			int count = this.schemas.size();
			for (int i=0; i < count; i++)
			{
				this.retrieveTables((String)schemas.get(i));
			}
		}
		if (this.monitor != null)
		{
			this.monitor.setCurrentObject(null, -1, -1);
		}
	}

	private List retrieveWildcardTables(String name)
	{
		try
		{
			return this.dbConn.getMetadata().getTableList(null, name, this.types);
		}
		catch (SQLException e)
		{
			LogMgr.logError("SchemaReporter.retrieveWildcardTables()", "Error retrieving tables", e);
			return null;
		}
	}

	private void retrieveProcedures()
	{
		if (this.monitor != null)
		{
			this.monitor.setCurrentObject(ResourceMgr.getString("MsgSchemaReporterRetrievingProcedures"), -1, -1);
		}
		if (this.schemas == null || this.schemas.size() == 0)
		{
			this.retrieveProcedures(null);
		}
		else
		{
			int count = this.schemas.size();
			for (int i=0; i < count; i++)
			{
				this.retrieveProcedures((String)schemas.get(i));
			}
		}
		if (this.monitor != null)
		{
			this.monitor.setCurrentObject(null, -1, -1);
		}
	}

	private void retrieveProcedures(String targetSchema)
	{
		try
		{
			DataStore procs = this.dbConn.getMetadata().getProcedures(null, targetSchema);
			procs.sortByColumn(ProcedureReader.COLUMN_IDX_PROC_LIST_NAME, true);
			int count = procs.getRowCount();
			for (int i = 0; i < count; i++)
			{
				String name = procs.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_LIST_NAME);
				String schema = procs.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_LIST_SCHEMA);
				String cat = procs.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_LIST_CATALOG);
				int type = procs.getValueAsInt(i, ProcedureReader.COLUMN_IDX_PROC_LIST_TYPE, DatabaseMetaData.procedureResultUnknown);
				ReportProcedure proc = new ReportProcedure(cat, schema, name, type, this.dbConn);
				this.procedures.add(proc);
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("SchemaReporter.retrieveProcedures()", "Error retrieving procedures", e);
		}
	}
	/**
	 *	Retrieve all tables for the current user.
	 *	The "type" of table can be defined by #setTableTypes(String)
	 */
	private void retrieveTables(String targetSchema)
	{
		try
		{
			if (this.cancel) return;
			this.tables = this.dbConn.getMetadata().getTableList(targetSchema, this.types);
		}
		catch (SQLException e)
		{
			LogMgr.logError("SchemaReporter.retrieveTables()", "Error retrieving tables", e);
		}
	}

}
