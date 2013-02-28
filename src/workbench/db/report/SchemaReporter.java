/*
 * SchemaReporter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.report;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import workbench.interfaces.Interruptable;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.db.DbMetadata;
import workbench.db.DbObject;
import workbench.db.DbObjectComparator;
import workbench.db.DbSettings;
import workbench.db.ProcedureDefinition;
import workbench.db.SequenceDefinition;
import workbench.db.SequenceReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.storage.RowActionMonitor;

import workbench.util.CollectionUtil;
import workbench.util.FileUtil;
import workbench.util.StrBuffer;
import workbench.util.StringUtil;


/**
 * Generate an report from a selection of database tables.
 *
 * @author Thomas Kellerer
 */
public class SchemaReporter
	implements Interruptable
{
	private WbConnection dbConn;
	private List<TableIdentifier> tables = new ArrayList<TableIdentifier>();
	private List<ReportProcedure> procedures = new ArrayList<ReportProcedure>();
	private List<ReportSequence> sequences = new ArrayList<ReportSequence>();
	private Set<String> types;
	private List<String> schemas;

	private TagWriter tagWriter = new TagWriter();
	private RowActionMonitor monitor;
	private String outputfile;
	protected boolean cancel;
	private boolean includeProcedures;
	private boolean includeGrants;
	private boolean includeTriggers;
	private boolean includeExtendedOptions;
	private String schemaNameToUse = null;
	private String reportTitle = null;

	/**
	 * Creates a new SchemaReporter for the supplied connection
	 * @param conn The connection that the schema report should use
	 */
	public SchemaReporter(WbConnection conn)
	{
		this.dbConn = conn;
		types = CollectionUtil.caseInsensitiveSet();
		types.addAll(conn.getMetadata().getTableTypes());
		types.addAll(this.dbConn.getDbSettings().getViewTypes());
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
		DbMetadata meta = dbConn.getMetadata();

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
			if (meta.isSequenceType(dbo.getObjectType()))
			{
				SequenceDefinition seq = meta.getSequenceReader().getSequenceDefinition(dbo.getCatalog(), dbo.getSchema(), dbo.getObjectName());
				this.sequences.add(new ReportSequence(seq));
			}
		}
	}

	/**
	 * Define the list of tables to run the report on.
	 * The tables are added to the existing list of tables
	 *
	 */
	public void setTableList(List<TableIdentifier> tableList)
	{
		DbMetadata meta = dbConn.getMetadata();
		SequenceReader reader = meta.getSequenceReader();

		for (TableIdentifier tbl : tableList)
		{
			if (meta.isSequenceType(tbl.getType()) && reader != null)
			{
				SequenceDefinition seq = reader.getSequenceDefinition(tbl.getRawCatalog(), tbl.getRawSchema(), tbl.getRawTableName());
				this.sequences.add(new ReportSequence(seq));
			}
			else
			{
				this.tables.add(tbl);
			}
		}
	}

	/**
	 * Return the count of "table like" objects.
	 * Stored procedures are not taken into account.
	 */
	public int getObjectCount()
	{
		return tables.size() + sequences.size();
	}

	public void clearObjects()
	{
		tables.clear();
		sequences.clear();
		procedures.clear();
	}

	public void setSchemas(List<String> list)
	{
		if (list == null || list.isEmpty()) return;
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

	public void setIncludeExtendedOptions(boolean flag)
	{
		this.includeExtendedOptions = flag;
	}

	public void setIncludeProcedures(boolean flag)
	{
		this.includeProcedures = flag;
	}

	public void setIncludeGrants(boolean flag)
	{
		this.includeGrants = flag;
	}

	public void setIncludeTriggers(boolean flag)
	{
		includeTriggers = flag;
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
		StringWriter out = new StringWriter(5000);
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
			FileUtil.closeQuietely(bw);
		}
	}

	private void sortTableObjects()
	{
		Collections.sort(tables, new DbObjectComparator());
	}
	/**
	 *	Write the XML into the supplied output
	 */
	public void writeXml(Writer out)
		throws IOException, SQLException
	{
		this.cancel = false;

		sortTableObjects();
		if (this.includeProcedures && this.procedures.isEmpty()) this.retrieveProcedures();
		if (this.cancel) return;

		out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		out.write("<");
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

				DbObject dbo = null;
				if (this.dbConn.getMetadata().isExtendedObject(table))
				{
					 dbo = dbConn.getMetadata().getObjectDefinition(table);
				}

				if (dbo != null)
				{
					GenericReportObject genObject = new GenericReportObject(dbConn, dbo);
					genObject.writeXml(out);
				}
				else if (dbs.isViewType(type))
				{
					ReportView rview = new ReportView(table, this.dbConn, true, includeGrants);
					rview.setSchemaNameToUse(this.schemaNameToUse);
					rview.writeXml(out);
				}
				else
				{
					ReportTable rtable = new ReportTable(table, this.dbConn, true, true, true, true, includeGrants, includeTriggers, includeExtendedOptions);
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
		info.append(this.dbConn.getDatabaseInfoAsXml(indent));
		info.writeTo(out);
	}


	/**
	 * Cancel the current reporting process (this might leave a corrupted XML file)
	 */
	@Override
	public void cancelExecution()
	{
		this.cancel = true;
	}

	@Override
	public boolean confirmCancel()
	{
		return true;
	}

	private void retrieveProcedures()
	{
		if (this.monitor != null)
		{
			this.monitor.setCurrentObject(ResourceMgr.getString("MsgRetrievingProcedures"), -1, -1);
		}
		if (CollectionUtil.isEmpty(schemas))
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
			List<ProcedureDefinition> procs = null;
			if (this.dbConn.getDbSettings().supportsSchemas())
			{
				procs = this.dbConn.getMetadata().getProcedureReader().getProcedureList(null, schema, null);
			}
			else
			{
				procs = this.dbConn.getMetadata().getProcedureReader().getProcedureList(schema, null, null);
			}
			Set<String> oraPackages = new HashSet<String>();

			for (ProcedureDefinition def : procs)
			{
				// Object types are reported with the "normal" objects
				if (def.isOracleObjectType()) continue;

				if (def.isOraclePackage())
				{
					// Make sure Oracle packages are only reported once
					// getProcedureList() will return a procedure definition for each procedure/function of a package
					if (oraPackages.contains(def.getPackageName())) continue;
					oraPackages.add(def.getPackageName());
				}
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
	private void retrieveObjects(String targetSchema)
	{
		try
		{
			if (this.cancel) return;
			String schema = this.dbConn.getMetadata().adjustSchemaNameCase(targetSchema);
			String[] typesToUse = new String[types.size()];
			types.toArray(typesToUse);
			if (this.dbConn.getDbSettings().supportsSchemas())
			{
				this.setTableList(dbConn.getMetadata().getObjectList(schema, typesToUse));
			}
			else
			{
				// Assume the schema names are really catalogs
				this.setTableList(dbConn.getMetadata().getObjectList(null, schema, null, typesToUse));
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("SchemaReporter.retrieveTables()", "Error retrieving tables", e);
		}
	}

}
