/*
 * SchemaReporter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JDialog;

import javax.swing.JFrame;
import workbench.db.DbMetadata;
import workbench.db.DbObject;
import workbench.db.DbSettings;

import workbench.db.ProcedureDefinition;
import workbench.db.SequenceDefinition;
import workbench.db.SequenceReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.gui.WbSwingUtilities;
import workbench.gui.dbobjects.ProgressPanel;
import workbench.interfaces.Interruptable;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.storage.RowActionMonitor;
import workbench.util.FileUtil;
import workbench.util.StrBuffer;
import workbench.util.StrWriter;
import workbench.util.StringUtil;
import workbench.util.WbFile;
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
	private List<TableIdentifier> tables = new ArrayList<TableIdentifier>();
	private List<ReportProcedure> procedures = new ArrayList<ReportProcedure>();
	private List<ReportSequence> sequences = new ArrayList<ReportSequence>();
	private String xmlNamespace;
	private String[] types;
	private List<String> schemas;

	private TagWriter tagWriter = new TagWriter();
	private RowActionMonitor monitor;
	private String outputfile;
	protected boolean cancel = false;
	private boolean includeTables = true;
	private boolean includeProcedures = false;
	private boolean includeGrants = false;
	private boolean includeSequences = false;
	private ProgressPanel progressPanel;
	protected JDialog progressWindow;
	private boolean showProgress = false;
	private String schemaNameToUse = null;
	private String reportTitle = null;
	private JFrame parentWindow;
	private boolean dbDesignerFormat = false;

	/**
	 * Creates a new SchemaReporter for the supplied connection
	 * @param conn The connection that the schema report should use
	 */
	public SchemaReporter(WbConnection conn)
	{
		this.dbConn = conn;
		// Initialize the types to retrieve
		setIncludeViews(true);
	}

	public void setProgressMonitor(RowActionMonitor mon)
	{
		this.monitor = mon;
	}

	public void setReportTitle(String title)
	{
		this.reportTitle = title;
	}
	
	public void setObjectList(List<? extends DbObject> objectList)
	{
		if (this.tables == null) this.tables = new ArrayList<TableIdentifier>();
		
		for (DbObject dbo : objectList)
		{
			if (dbo instanceof TableIdentifier)
			{
				for (String type : this.types)
				{
					if (dbo.getObjectType().equalsIgnoreCase(type))
					{
						this.tables.add((TableIdentifier)dbo);
						break;
					}
				}
			}
		}
	}

	/**
	 *	Define the list of tables to run the report on
	 */
	public void setTableList(TableIdentifier[] tableList)
	{
		if (this.tables == null) this.tables = new ArrayList<TableIdentifier>();
		for (int i=0; i < tableList.length; i++)
		{
			if (tableList[i].getTableName().indexOf('%') > -1)
			{
				List<TableIdentifier> tlist = retrieveWildcardTables(tableList[i].getTableName());
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

	public void setSchemas(List<String> list)
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

	public void setIncludeViews(boolean flag) 
	{ 
		if (flag)
		{
			types = new String[] { 
				this.dbConn.getMetadata().getTableTypeName(), 
				this.dbConn.getMetadata().getViewTypeName(),
				DbMetadata.MVIEW_NAME};
		}
		else
		{
			types = new String[] { this.dbConn.getMetadata().getTableTypeName() };
		}
	}
	
	public void setIncludeSequences(boolean flag) { this.includeSequences = flag; }
	public void setIncludeTables(boolean flag) { this.includeTables = flag; }
	public void setIncludeProcedures(boolean flag) { this.includeProcedures = flag; }
	public void setIncludeGrants(boolean flag) { this.includeGrants = flag; }
	
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
		catch (Exception e)
		{
			// Cannot happen with StrWriter
		}
		return out.toString();
	}

	public void setDbDesigner(boolean flag)
	{
		this.dbDesignerFormat = flag;
	}
	
	public void setShowProgress(boolean flag, JFrame parent)
	{
		this.showProgress = flag;
		this.parentWindow = parent;
	}

	public void writeXml()
		throws IOException, SQLException
	{

		if (this.showProgress)
		{
			// This is the only way I can figure out to show
			// the progress as as modal window, but let the 
			// calling thread proceed with the work.
			Thread t = new WbThread("ShowProgress")
			{
				public void run()
				{
					openProgressMonitor();
				}
			};
			t.start();
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
			FileUtil.closeQuitely(bw);
			closeProgress();
		}
	}

	/**
	 *	Write the XML into the supplied output
	 */
	public void writeXml(Writer out)
		throws IOException, SQLException
	{
		this.cancel = false;

		if (this.includeTables && this.tables.size() == 0) this.retrieveTables();
		if (this.cancel) return;

		if (this.dbDesignerFormat)
		{
			WbFile f = new WbFile(this.outputfile);
			DbDesignerWriter writer = new DbDesignerWriter(this.dbConn, this.tables, f.getFileName());
			writer.setMonitor(monitor);
			writer.setProgressPanel(progressPanel);
			writer.writeXml(out);
			return;
		}
		
		if (this.includeProcedures && this.procedures.size() == 0) this.retrieveProcedures();
		if (this.cancel) return;

		if (this.includeSequences && this.sequences.size() == 0) this.retrieveSequences();
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
		int totalCount = count + this.procedures.size() + this.sequences.size();
		int totalCurrent = 1;
		
		DbSettings dbs = dbConn.getMetadata().getDbSettings();
		
		for (TableIdentifier table : tables)
		{
			try
			{
				if (this.cancel) break;

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
					table.setType(type);
				}
				
				if (dbs.isViewType(type))
				{
					ReportView rview = new ReportView(table, this.dbConn, true, this.xmlNamespace);
					rview.setSchemaNameToUse(this.schemaNameToUse);
					rview.writeXml(out);
				}
				else
				{
					ReportTable rtable = new ReportTable(table, this.dbConn, this.xmlNamespace, true, true, true, true, this.includeGrants);
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
		for (ReportProcedure proc : procedures)
		{
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
			if (this.cancel) break;
		}
		
		count = this.sequences.size();
		if (count > 0) out.write("\n");
		for (ReportSequence seq : sequences)
		{
			String name = seq.getSequence().getSequenceName();
			if (this.monitor != null)
			{
				this.monitor.setCurrentObject(name, totalCurrent, totalCount);
			}
			if (this.progressPanel != null)
			{
				this.progressPanel.setInfoText(name);
			}
			seq.writeXml(out);
			out.write('\n');
			totalCurrent ++;
			if (this.cancel) break;
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
		if (!StringUtil.isEmptyString(this.reportTitle))
		{
			this.tagWriter.appendTag(info, indent, "report-title", this.reportTitle);
		}
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
		this.progressWindow.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		this.progressWindow.getContentPane().add(progressPanel);
		this.progressWindow.setTitle(ResourceMgr.getString("MsgReportWindowTitle"));
		
		this.progressWindow.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				cancel = true;
				if (progressWindow != null)
				{
					progressWindow.setVisible(false);
					progressWindow.dispose();
				}
			}
		});
		
		WbSwingUtilities.invoke(new Runnable()
		{
			public void run()
			{
				progressWindow.pack();
				progressWindow.setSize(300,120);
				WbSwingUtilities.center(progressWindow, parentWindow);
				progressWindow.setVisible(true);
			}
		});
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
				this.retrieveTables(schemas.get(i));
			}
		}
		if (this.monitor != null)
		{
			this.monitor.setCurrentObject(null, -1, -1);
		}
	}

	private List<TableIdentifier> retrieveWildcardTables(String name)
	{
		try
		{
			String schema = this.dbConn.getMetadata().adjustSchemaNameCase(name);
			return this.dbConn.getMetadata().getTableList(null, schema, this.types);
		}
		catch (SQLException e)
		{
			LogMgr.logError("SchemaReporter.retrieveWildcardTables()", "Error retrieving tables", e);
			return null;
		}
	}

	private void retrieveSequences()
	{
		if (this.monitor != null)
		{
			this.monitor.setCurrentObject(ResourceMgr.getString("MsgSchemaReporterRetrievingProcedures"), -1, -1);
		}
		if (this.schemas == null || this.schemas.size() == 0)
		{
			this.retrieveSequences(null);
		}
		else
		{
			for (String schema : schemas)
			{
				this.retrieveSequences(schema);
			}
		}
		if (this.monitor != null)
		{
			this.monitor.setCurrentObject(null, -1, -1);
		}
	}
	
	private void retrieveSequences(String targetSchema)
	{
		try
		{
			String schema = this.dbConn.getMetadata().adjustSchemaNameCase(targetSchema);
			if (schema == null) 
			{
				schema = this.dbConn.getMetadata().getSchemaToUse();
			}
			SequenceReader reader = this.dbConn.getMetadata().getSequenceReader();
			if (reader == null) return;
			
			List<SequenceDefinition> seqs = reader.getSequences(schema);
			
			for (SequenceDefinition seq : seqs)
			{
				ReportSequence rseq = new ReportSequence(seq, this.xmlNamespace);
				this.sequences.add(rseq);
				if (this.cancel) return;
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("SchemaReporter.retrieveSequences()", "Error retrieving sequences", e);
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
			for (String schema : schemas)
			{
				this.retrieveProcedures(schema);
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
			String schema = this.dbConn.getMetadata().adjustSchemaNameCase(targetSchema);
			List<ProcedureDefinition> procs = this.dbConn.getMetadata().getProcedureList(null, schema);
			
			for (ProcedureDefinition def : procs)
			{
				ReportProcedure proc = new ReportProcedure(def, this.dbConn);
				this.procedures.add(proc);
				if (this.cancel) return;
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
			String schema = this.dbConn.getMetadata().adjustSchemaNameCase(targetSchema);	
			this.tables = this.dbConn.getMetadata().getTableList(schema, this.types);
		}
		catch (SQLException e)
		{
			LogMgr.logError("SchemaReporter.retrieveTables()", "Error retrieving tables", e);
		}
	}

}
