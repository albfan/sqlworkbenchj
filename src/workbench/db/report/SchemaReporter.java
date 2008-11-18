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

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import workbench.db.DbMetadata;
import workbench.db.DbObject;
import workbench.db.DbSettings;

import workbench.db.ProcedureDefinition;
import workbench.db.SequenceDefinition;
import workbench.db.SequenceReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.interfaces.Interruptable;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.storage.RowActionMonitor;
import workbench.util.FileUtil;
import workbench.util.StrBuffer;
import workbench.util.StrWriter;
import workbench.util.StringUtil;


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
	private String schemaNameToUse = null;
	private String reportTitle = null;

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

	/**
	 *	Define the list of tables to run the report on
	 */
	public void setTableList(List<TableIdentifier> tableList)
	{
		if (this.tables == null) this.tables = new ArrayList<TableIdentifier>();
		this.tables.addAll(tableList);
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

	public void writeXml()
		throws IOException, SQLException
	{
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
	}

	public boolean confirmCancel()
	{
		return true;
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
			List<ProcedureDefinition> procs = this.dbConn.getMetadata().getProcedureReader().getProcedureList(null, schema, null);
			
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
			this.setTableList(dbConn.getMetadata().getTableList(schema, this.types));
		}
		catch (SQLException e)
		{
			LogMgr.logError("SchemaReporter.retrieveTables()", "Error retrieving tables", e);
		}
	}

}
