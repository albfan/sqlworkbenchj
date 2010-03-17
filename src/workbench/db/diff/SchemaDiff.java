/*
 * SchemaDiff.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.diff;

import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import workbench.db.DbMetadata;
import workbench.db.DbSettings;
import workbench.db.ProcedureDefinition;
import workbench.db.SequenceDefinition;
import workbench.db.SequenceReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.report.ReportProcedure;
import workbench.db.report.ReportSequence;
import workbench.db.report.ReportTable;
import workbench.db.report.ReportView;
import workbench.db.report.TagWriter;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.storage.RowActionMonitor;
import workbench.util.CaseInsensitiveComparator;
import workbench.util.StrBuffer;
import workbench.util.StrWriter;
import workbench.util.StringUtil;

/**
 * Compare two Schemas for differences in the definition of the tables
 *
 * @author Thomas Kellerer
 */
public class SchemaDiff
{
	public static final String TAG_ADD_TABLE = "add-table";
	public static final String TAG_ADD_VIEW = "add-view";
	public static final String TAG_DROP_TABLE = "drop-table";
	public static final String TAG_DROP_VIEW = "drop-view";
	public static final String TAG_DROP_PROC = "drop-procedure";
	public static final String TAG_ADD_PROC = "add-procedure";
	public static final String TAG_DROP_SEQUENCE = "drop-sequence";

	public static final String TAG_REF_CONN = "reference-connection";
	public static final String TAG_TARGET_CONN = "target-connection";
	public static final String TAG_COMPARE_INFO = "compare-settings";
	public static final String TAG_VIEW_PAIR = "view-info";
	public static final String TAG_TABLE_PAIR = "table-info";
	public static final String TAG_PROC_PAIR = "procedure-info";
	public static final String TAG_SEQ_PAIR = "sequence-info";
	public static final String TAG_INDEX_INFO = "include-index";
	public static final String TAG_FK_INFO = "include-foreign-key";
	public static final String TAG_PK_INFO = "include-primary-key";
	public static final String TAG_GRANT_INFO = "include-tablegrants";
	public static final String TAG_CONSTRAINT_INFO = "include-constraints";
	public static final String TAG_VIEW_INFO = "include-views";
	public static final String TAG_PROC_INFO = "include-procs";
	public static final String TAG_SEQUENCE_INFO = "include-sequences";
	public static final String TAG_VIEWS_AS_TABLE = "views-as-tables";

	private WbConnection referenceDb;
	private WbConnection targetDb;
	private List<Object> objectsToCompare;
	private List<TableIdentifier> tablesToDelete;
	private List<ProcedureDefinition> procsToDelete;
	private List<TableIdentifier> viewsToDelete;
	private List<SequenceDefinition> sequencesToDelete;

	private String encoding = "UTF-8";
	private boolean compareJdbcTypes = false;
	private boolean diffIndex = true;
	private boolean diffForeignKeys = true;
	private boolean diffPrimaryKeys = true;
	private boolean diffConstraints = false;
	private boolean diffGrants = false;
	private boolean diffViews = true;
	private boolean diffProcs = true;
	private boolean diffTriggers = true;
	private boolean diffSequences = true;
	private boolean treatViewAsTable = false;
	private boolean compareConstraintsByName;

//	private boolean diffComments;
	private RowActionMonitor monitor;
	private boolean cancel = false;
	private String referenceSchema;
	private String targetSchema;
	private Set<String> tablesToIgnore = new TreeSet<String>(new CaseInsensitiveComparator());

	public SchemaDiff()
	{
	}

	/**
	 * Create a new SchemaDiff for the given connections with the given
	 * namespace to be used when writing the XML.
	 *
	 * @param reference The connection to the reference schema
	 * @param target the connection to the target schema
	 */
	public SchemaDiff(WbConnection reference, WbConnection target)
	{
		referenceDb = reference;
		targetDb = target;
	}

	public void setCompareConstraintsByName(boolean flag)
	{
		this.compareConstraintsByName = flag;
	}

	public void setIncludeTriggers(boolean flag)
	{
		this.diffTriggers = flag;
	}

	public void setIncludeSequences(boolean flag) { this.diffSequences = flag; }

	/**
	 * Control whether foreign keys should be compared as well.
	 * The default is to compare foreign keys.
	 */
	public void setIncludeForeignKeys(boolean flag) { this.diffForeignKeys = flag; }
	public boolean getIncludeForeignKeys() { return this.diffForeignKeys; }

	/**
	 *	Control whether index definitions should be compared as well.
	 *  The default is to compare index definitions
	 */
	public void setIncludeIndex(boolean flag) { this.diffIndex = flag; }
	public boolean getIncludeIndex() { return this.diffIndex; }

	/**
	 * Control whether primary keys should be compared as well.
	 * The default is to compare primary keys.
	 */
	public void setIncludePrimaryKeys(boolean flag) { this.diffPrimaryKeys = flag; }

	/**
	 * Control whether table constraints should be compared as well.
	 * The default is to not compare primary keys.
	 */
	public void setIncludeTableConstraints(boolean flag) { this.diffConstraints = flag; }

	public void setCompareJdbcTypes(boolean flag) { this.compareJdbcTypes = flag; }
	public boolean getCompareJdbcTypes() { return this.compareJdbcTypes; }

	public void setIncludeViews(boolean flag) { this.diffViews = flag; }

	public void setIncludeProcedures(boolean flag) { this.diffProcs = flag; }

	public void setIncludeTableGrants(boolean flag) { this.diffGrants = flag; }
	public boolean getIncludeTableGrants() { return this.diffGrants; }

	public void setTreatViewAsTable(boolean flag) { this.treatViewAsTable = flag; }

//	public void setIncludeComments(boolean flag) { this.diffComments = flag; }

	/**
	 *	Set the {@link workbench.storage.RowActionMonitor} for reporting progress
	 */
	public void setMonitor(RowActionMonitor mon)
	{
		this.monitor = mon;
	}

	/**
	 *	Cancel the creation of the XML file
	 *  @see #isCancelled()
	 */
	public void cancel()
	{
		this.cancel = true;
	}

	/**
	 *	Return if the XML generation has been cancelled
	 * @return true if #cancel() has been called
	 */
	public boolean isCancelled()
	{
		return this.cancel;
	}

	/**
	 * Define table names to be compared. The table names in the passed
	 * lists will be converted to TableIdentifiers and then passed
	 * on to setTables(List<TableIdentifier>, List<TableIdentifier>)
	 *
	 * No name matching will take place. Thus it's possible to compare
	 * tables that might have different names but are supposed to be identical
	 * otherwise.
	 *
	 * @see #setTables(List, List)
	 * @see #compareAll()
	 */
	public void setTableNames(List<String> referenceList, List<String> targetList)
		throws SQLException
	{
		ArrayList<TableIdentifier> reference = new ArrayList<TableIdentifier>(referenceList.size());
		ArrayList<TableIdentifier> target = new ArrayList<TableIdentifier>(targetList.size());

		if (referenceList.size() != targetList.size()) throw new IllegalArgumentException("Size of lists does not match");

		for (int i=0; i < referenceList.size(); i++)
		{
			String rname = referenceList.get(i);
			TableIdentifier rtbl = referenceDb.getMetadata().findTable(new TableIdentifier(rname));
			String tname = targetList.get(i);
			TableIdentifier ttbl = targetDb.getMetadata().findTable(new TableIdentifier(tname));
			if (rtbl != null && ttbl != null)
			{
				reference.add(rtbl);
				target.add(ttbl);
			}
			else
			{
				LogMgr.logWarning("SchemaDiff.setTableNames()", "Table combination not found: "
					+ rname + " [" + rtbl + "] to " + tname + " [" + ttbl + "]");
			}
		}
		setTables(reference, target);
	}

	/**
	 * Define the tables to be compared. They will be compared based
	 * on the position in the arrays (i.e. reference at index 0 will be
	 * compared to target at index 0...)
	 *
	 * No name matching will take place. Thus it's possible to compare
	 * tables that might have different names but are supposed to be identical
	 * otherwise.
	 *
	 * @see #setTables(List)
	 * @see #compareAll()
	 */
	public void setTables(List<TableIdentifier> referenceList, List<TableIdentifier> targetList)
		throws SQLException
	{
		if (referenceList == null) throw new NullPointerException("Source tables may not be null");
		if (targetList == null) throw new NullPointerException("Target tables may not be null");
		if (referenceList.size() != targetList.size()) throw new IllegalArgumentException("Number of source and target tables have to match");
		int count = referenceList.size();
		this.objectsToCompare = new ArrayList<Object>(count);

		if (this.monitor != null)
		{
			this.monitor.setMonitorType(RowActionMonitor.MONITOR_PROCESS_TABLE);
			this.monitor.setCurrentObject(ResourceMgr.getString("MsgDiffRetrieveDbInfo"), -1, -1);
		}

		for (int i=0; i < count; i++)
		{
			if (this.cancel)
			{
				this.objectsToCompare = null;
				break;
			}
			TableIdentifier reference = referenceList.get(i);

			if (reference == null) continue;

			TableIdentifier target = targetList.get(i);
			DiffEntry entry = new DiffEntry(reference, target);
			this.objectsToCompare.add(entry);
		}
	}

	private ReportView createReportViewInstance(TableIdentifier tbl, WbConnection con)
		throws SQLException
	{
		tbl.adjustCase(con);
		ReportView view = new ReportView(tbl, con, diffIndex);
		return view;
	}

	private ReportTable createReportTableInstance(TableIdentifier tbl, WbConnection con)
		throws SQLException
	{
		tbl.adjustCase(con);
		return new ReportTable(tbl, con, diffIndex, diffForeignKeys, diffPrimaryKeys, diffConstraints, diffGrants, diffTriggers);
	}

	/**
	 * Define a list of table names that should not be compared.
	 * Tables in the reference/source database that match one of the
	 * names in this list will be skipped.
	 */
	public void setExcludeTables(List<String> tables)
	{
		if (tables == null || tables.size() == 0)
		{
			this.tablesToIgnore.clear();
			return;
		}
		for (String tname : tables)
		{
			if (tname.indexOf('%') > -1 || tname.indexOf('*') > -1)
			{
				try
				{
					String toSearch = this.referenceDb.getMetadata().adjustObjectnameCase(tname.trim());
					List<TableIdentifier> tlist = this.referenceDb.getMetadata().getTableList(toSearch, (String)null);
					for (TableIdentifier t : tlist)
					{
						tablesToIgnore.add(t.getTableName());
					}
					toSearch = this.targetDb.getMetadata().adjustObjectnameCase(tname.trim());
					tlist = this.targetDb.getMetadata().getTableList(toSearch, (String)null);
					for (TableIdentifier t : tlist)
					{
						tablesToIgnore.add(t.getTableName());
					}
				}
				catch (SQLException e)
				{
					LogMgr.logError("SchemaDiff.setExcludeTables()","Could not retrieve excluded tables", e);
				}

			}
			else
			{
				this.tablesToIgnore.add(tname);
			}
		}
	}

	/**
	 *	Setup this SchemaDiff object to compare all tables that the user
	 *  can access in the reference connection with all matching (=same name)
	 *  tables in the target connection.
	 *  This will retrieve all user tables from the reference (=source)
	 *  connection and will match them to the tables in the target connection.
	 *
	 *  When using compareAll() drop statements will be created for tables
	 *  present in the target connection but not existing in the reference
	 *  connection.
	 *
	 * @see #setTables(List, List)
	 * @see #setTables(List)
	 */
	public void compareAll()
		throws SQLException
	{
		setSchemas(null, null);
	}

	/**
	 *	Setup this SchemaDiff object to compare all tables that the user
	 *  can access in the reference connection with all matching (=same name)
	 *  tables in the target connection.
	 *  This will retrieve all user tables from the reference (=source)
	 *  connection and will match them to the tables in the target connection.
	 *
	 *  When using compareAll() drop statements will be created for tables
	 *  present in the target connection but not existing in the reference
	 *  connection.
	 *
	 * @see #setTables(List, List)
	 * @see #setTables(List)
	 */
	public void setSchemas(String rSchema, String tSchema)
		throws SQLException
	{
		if (this.monitor != null)
		{
			this.monitor.setMonitorType(RowActionMonitor.MONITOR_PLAIN);
			this.monitor.setCurrentObject(ResourceMgr.getString("MsgDiffRetrieveDbInfo"), -1, -1);
		}

		this.referenceSchema = (rSchema == null ? this.referenceDb.getMetadata().getSchemaToUse() : this.referenceDb.getMetadata().adjustSchemaNameCase(rSchema));
		this.targetSchema = (tSchema == null ? this.targetDb.getMetadata().getSchemaToUse() : this.referenceDb.getMetadata().adjustSchemaNameCase(tSchema));

		String[] types;
		if (diffViews || treatViewAsTable)
		{
			types = new String[] { this.referenceDb.getMetadata().getTableTypeName(), this.referenceDb.getMetadata().getViewTypeName() };
		}
		else
		{
			types = new String[] { this.referenceDb.getMetadata().getTableTypeName() };
		}

		List<TableIdentifier> refTables = referenceDb.getMetadata().getObjectList(this.referenceSchema, types);
		List<TableIdentifier> target = targetDb.getMetadata().getObjectList(this.targetSchema, types);

		if (treatViewAsTable)
		{
			String viewType = referenceDb.getMetadata().getViewTypeName();
			String tblType = referenceDb.getMetadata().getTableTypeName();
			for (TableIdentifier table : refTables)
			{
				if (table.getType().equals(viewType))
				{
					table.setType(tblType);
				}
			}
		}

		processTableList(refTables, target);

		if (diffProcs)
		{
			List<ProcedureDefinition> refProcs = referenceDb.getMetadata().getProcedureReader().getProcedureList(null, this.referenceSchema, null);
			List<ProcedureDefinition> targetProcs = targetDb.getMetadata().getProcedureReader().getProcedureList(null, this.targetSchema, null);
			processProcedureList(refProcs, targetProcs);
		}

		if (diffSequences)
		{
			List<SequenceDefinition> refSeqs = Collections.emptyList();
			List<SequenceDefinition> tarSeqs = Collections.emptyList();
			SequenceReader refReader = referenceDb.getMetadata().getSequenceReader();
			SequenceReader tarReader = targetDb.getMetadata().getSequenceReader();
			if (refReader != null)
			{
				refSeqs = refReader.getSequences(this.referenceSchema, null);
			}
			if (tarReader != null)
			{
				tarSeqs = tarReader.getSequences(this.referenceSchema, null);
			}
			processSequenceList(refSeqs, tarSeqs);
		}
	}

	private void processTableList(List<TableIdentifier> refTables, List<TableIdentifier> targetTables)
		throws SQLException
	{
		int count = refTables.size();
		HashSet<String> refTableNames = new HashSet<String>();

		this.objectsToCompare = new ArrayList<Object>(count);

		if (this.monitor != null)
		{
			this.monitor.setMonitorType(RowActionMonitor.MONITOR_PLAIN);
		}

		for (int i=0; i < count; i++)
		{
			if (this.cancel)
			{
				this.objectsToCompare = null;
				break;
			}

			TableIdentifier rid = refTables.get(i);

			String tname = StringUtil.trimQuotes(rid.getTableName());

			if (this.tablesToIgnore.contains(tname)) continue;

			if (this.monitor != null)
			{
				this.monitor.setCurrentObject(ResourceMgr.getFormattedString("MsgLoadTableInfo", tname), -1, -1);
			}

			TableIdentifier tid = rid.createCopy();
			tid.setSchema(targetSchema);
			tid.setCatalog(null);
			tid.setNeverAdjustCase(false);
			tid.adjustCase(targetDb);

			DiffEntry entry = null;
			if (targetDb.getMetadata().objectExists(tid, rid.getType()))
			{
				tid.setType(rid.getType());
				entry = new DiffEntry(rid, tid);
			}
			else
			{
				entry = new DiffEntry(rid, null);
			}
			objectsToCompare.add(entry);
			refTableNames.add(tname);
		}

		if (cancel) return;

		this.tablesToDelete = new ArrayList<TableIdentifier>();
		this.viewsToDelete = new ArrayList<TableIdentifier>();

		if (targetTables != null)
		{
			String tableType = targetDb.getMetadata().getTableTypeName();
			count = targetTables.size();
			for (int i=0; i < count; i++)
			{
				TableIdentifier t = targetTables.get(i);
				String tbl = StringUtil.trimQuotes(t.getTableName());
				if (this.tablesToIgnore.contains(tbl)) continue;

				if (targetDb.getMetadata().isDefaultCase(tbl))
				{
					tbl = referenceDb.getMetadata().adjustObjectnameCase(tbl);
				}
				if (!refTableNames.contains(tbl))
				{
					if (tableType.equals(t.getType()))
					{
						this.tablesToDelete.add(t);
					}
					else
					{
						this.viewsToDelete.add(t);
					}
				}
			}
		}
	}

	private void processSequenceList(List<SequenceDefinition> refSeqs, List<SequenceDefinition> targetSeqs)
	{
		HashSet<String> refSeqNames = new HashSet<String>();
		this.sequencesToDelete= new ArrayList<SequenceDefinition>();

		if (this.monitor != null)
		{
			this.monitor.setMonitorType(RowActionMonitor.MONITOR_PLAIN);
		}

		SequenceReader targetReader = this.targetDb.getMetadata().getSequenceReader();

		for (SequenceDefinition refSeq : refSeqs)
		{
			if (this.cancel)
			{
				this.objectsToCompare = null;
				break;
			}

			if (this.monitor != null)
			{
				this.monitor.setCurrentObject(ResourceMgr.getFormattedString("MsgLoadSeqInfo", refSeq.getSequenceName()), -1, -1);
			}

			SequenceDiffEntry entry = null;
			if (targetReader != null)
			{
				SequenceDefinition def = targetReader.getSequenceDefinition(this.targetSchema, refSeq.getSequenceName());
				entry = new SequenceDiffEntry(refSeq, def);
				objectsToCompare.add(entry);
				refSeqNames.add(refSeq.getSequenceName());
			}
		}

		if (cancel) return;

		for (SequenceDefinition tSeq : targetSeqs)
		{
			String seqname = tSeq.getSequenceName();
			if (!refSeqNames.contains(seqname))
			{
				this.sequencesToDelete.add(tSeq);
			}
		}
	}

	private void processProcedureList(List<ProcedureDefinition> refProcs, List<ProcedureDefinition> targetProcs)
	{
		HashSet<String> refProcNames = new HashSet<String>();
		this.procsToDelete = new ArrayList<ProcedureDefinition>();

		DbMetadata targetMeta = this.targetDb.getMetadata();

		if (this.monitor != null)
		{
			this.monitor.setMonitorType(RowActionMonitor.MONITOR_PLAIN);
		}

		if (refProcs == null) refProcs = Collections.emptyList();
		if (targetProcs == null) targetProcs = Collections.emptyList();

		for (ProcedureDefinition refProc : refProcs)
		{
			if (this.cancel)
			{
				this.objectsToCompare = null;
				break;
			}

			if (this.monitor != null)
			{
				this.monitor.setCurrentObject(ResourceMgr.getFormattedString("MsgLoadProcInfo", refProc.getProcedureName()), -1, -1);
			}

			ProcDiffEntry entry = null;
			ProcedureDefinition tp = new ProcedureDefinition(null, this.targetSchema, refProc.getProcedureName(), refProc.getResultType());
			if (targetMeta.getProcedureReader().procedureExists(tp))
			{
				entry = new ProcDiffEntry(refProc,tp);
			}
			else
			{
				entry = new ProcDiffEntry(refProc, null);
			}
			objectsToCompare.add(entry);
			refProcNames.add(refProc.getProcedureName());
		}

		if (cancel) return;

		for (ProcedureDefinition tProc : targetProcs)
		{
			String procname = tProc.getProcedureName();
			if (!refProcNames.contains(procname))
			{
				this.procsToDelete.add(tProc);
			}
		}
	}

	/**
	 * Define the reference tables to be compared with the matching
	 * tables (based on the name) in the target connection. The list
	 * has to contain objects of type {@link workbench.db.TableIdentifier}
	 *
	 * @see #setTables(List, List)
	 * @see #compareAll()
	 */
	public void setTables(List<TableIdentifier> reference)
		throws SQLException
	{
		this.processTableList(reference, null);
	}

	/**
	 *	Return the XML that describes how the target schema needs to be
	 *  modified in order to get the same structure as the reference schema.
	 *
	 *	For this, each defined table in the reference schema will be compared
	 *  to the corresponding table in the target schema.
	 *
	 *  @see TableDiff#getMigrateTargetXml()
	 */
	public String getMigrateTargetXml()
	{
		StrWriter writer = new StrWriter(5000);
		try
		{
			this.writeXml(writer);
		}
		catch (Exception e)
		{
			LogMgr.logError("SchemaDiff.getMigrateTargetXml()", "Error getting XML", e);
		}
		return writer.toString();
	}

	/**
	 *	Return the encoding that is used in the encoding attribute of the XML tag
	 */
	public String getEncoding()
	{
		return encoding;
	}

	/**
	 *	Set the encoding that is used for writing the XML. This will
	 *  be put into the <?xml tag at the beginning of the generated XML
	 */
	public void setEncoding(String enc)
	{
		this.encoding = enc;
	}

	/**
	 *	Write the XML of the schema differences to the supplied writer.
	 *  This writes some meta information about the compare, and then
	 *  creates a {@link TableDiff} object for each pair of tables that
	 *  needs to be compared. The output of {@link TableDiff#getMigrateTargetXml()}
	 *  will then be written into the writer.
	 */
	public void writeXml(Writer out)
		throws IOException
	{
		if (objectsToCompare == null) throw new NullPointerException("Source tables may not be null");

		StrBuffer indent = new StrBuffer("  ");
		StrBuffer tblIndent = new StrBuffer("    ");
		TagWriter tw = new TagWriter();
		out.write("<?xml version=\"1.0\" encoding=\"");
		out.write(this.encoding);
		out.write("\"?>\n");

		if (this.monitor != null)
		{
			this.monitor.setMonitorType(RowActionMonitor.MONITOR_PROCESS_TABLE);
		}

		writeTag(out, null, "schema-diff", true);
		writeDiffInfo(out);
		int count = this.objectsToCompare.size();
		List<ViewDiff> viewDiffs = new ArrayList<ViewDiff>();
		String tableType = referenceDb.getMetadata().getTableTypeName();
		// First we have to process the tables
		for (int i=0; i < count; i++)
		{
			Object o = objectsToCompare.get(i);
			if (o instanceof ProcDiffEntry) continue;
			if (o instanceof SequenceDiffEntry) continue;

			DiffEntry entry = (DiffEntry)o;
			if (this.cancel) break;

			if (this.monitor != null)
			{
				this.monitor.setCurrentObject(entry.reference.getTableExpression(), i+1, count);
			}

			try
			{
				if (tableType.equalsIgnoreCase(entry.reference.getType()))
				{
					ReportTable source = createReportTableInstance(entry.reference, this.referenceDb);
					if (entry.target == null)
					{
						out.write("\n");
						writeTag(out, indent, TAG_ADD_TABLE, true, "name", entry.reference.getTableName());
						StrBuffer s = source.getXml(tblIndent);
						s.writeTo(out);
						writeTag(out, indent, TAG_ADD_TABLE, false);
					}
					else
					{
						ReportTable target = createReportTableInstance(entry.target, this.targetDb);
						TableDiff d = new TableDiff(source, target, this);
						//d.setCompareComments(this.diffComments);
						d.setIndent(indent);
						d.setTagWriter(tw);
						d.setExactConstraintMatch(compareConstraintsByName);
						StrBuffer s = d.getMigrateTargetXml();
						if (s.length() > 0)
						{
							out.write("\n");
							s.writeTo(out);
						}
					}
				}
				else
				{
					// We cannot write out the diff for the views immediately
					// because they should be listed after the table diffs
					ReportView source = createReportViewInstance(entry.reference, referenceDb);
					ReportView target = null;
					if (entry.target != null)
					{
						target = createReportViewInstance(entry.target, targetDb);
					}
					ViewDiff d = new ViewDiff(source, target);
					d.setIndent(indent);
					d.setTagWriter(tw);
					viewDiffs.add(d);
				}
			}
			catch (SQLException sql)
			{
				LogMgr.logError("SchemaDiff.writeXml()", "Error comparing " + entry.toString(), sql);
			}
		}

		if (this.cancel) return;

		this.appendDropTables(out, indent);
		out.write("\n");

		if (this.diffViews)
		{
			this.appendViewDiff(viewDiffs, out);
			//out.write("\n");
			this.appendDropViews(out, indent);
		}
		if (this.diffSequences)
		{
			this.appendSequenceDiff(out, indent, tw);
			out.write("\n");
		}

		if (this.diffProcs)
		{
			this.appendProcDiff(out, indent, tw);
			out.write("\n");
		}

		if (this.cancel) return;

		if (this.diffProcs)
		{
			this.appendProcDiff(out, indent, tw);
			out.write("\n");
		}

		writeTag(out, null, "schema-diff", false);
	}

	private void appendSequenceDiff(Writer out, StrBuffer indent, TagWriter tw)
		throws IOException
	{
		int count = this.objectsToCompare.size();
		for (int i=0; i < count; i++)
		{
			Object o = objectsToCompare.get(i);
			if (o instanceof SequenceDiffEntry)
			{
				SequenceDiffEntry entry = (SequenceDiffEntry)o;
				ReportSequence rp = new ReportSequence(entry.reference);
				ReportSequence tp = (entry.target == null ? null : new ReportSequence(entry.target));
				SequenceDiff diff = new SequenceDiff(rp, tp);
				diff.setIndent(indent);
				diff.setTagWriter(tw);
				StrBuffer xml = diff.getMigrateTargetXml();
				if (xml.length() > 0)
				{
					out.write("\n");
					xml.writeTo(out);
				}
			}
		}

		if (this.sequencesToDelete == null || sequencesToDelete.size() == 0) return;

		out.write('\n');
		writeTag(out, indent, TAG_DROP_SEQUENCE, true);
		StrBuffer myindent = new StrBuffer(indent);
		myindent.append("  ");
		for (SequenceDefinition def : sequencesToDelete)
		{
			writeTagValue(out, myindent, ReportSequence.TAG_SEQ_NAME, def.getSequenceName());
		}
		writeTag(out, indent, TAG_DROP_SEQUENCE, false);
	}

	private void appendProcDiff(Writer out, StrBuffer indent, TagWriter tw)
		throws IOException
	{
		int count = this.objectsToCompare.size();
		for (int i=0; i < count; i++)
		{
			Object o = objectsToCompare.get(i);
			if (o instanceof ProcDiffEntry)
			{
				ProcDiffEntry entry = (ProcDiffEntry)o;
				ReportProcedure rp = new ReportProcedure(entry.reference, this.referenceDb);
				ReportProcedure tp = new ReportProcedure(entry.target, this.targetDb);
				ProcDiff diff = new ProcDiff(rp, tp);
				diff.setIndent(indent);
				diff.setTagWriter(tw);
				StrBuffer xml = diff.getMigrateTargetXml();
				if (xml.length() > 0)
				{
					out.write("\n");
					xml.writeTo(out);
				}
			}
		}

		if (this.procsToDelete == null || procsToDelete.size() == 0) return;

		out.write('\n');
		writeTag(out, indent, TAG_DROP_PROC, true);
		StrBuffer myindent = new StrBuffer(indent);
		myindent.append("  ");
		Iterator itr = this.procsToDelete.iterator();
		while (itr.hasNext())
		{
			ProcedureDefinition def = (ProcedureDefinition)itr.next();
			ReportProcedure rp = new ReportProcedure(def, targetDb);
			rp.setIndent(myindent);
			StrBuffer xml = rp.getXml(false);
			xml.writeTo(out);
		}
		writeTag(out, indent, TAG_DROP_PROC, false);
	}

	private void appendViewDiff(List<ViewDiff> diffs, Writer out)
		throws IOException
	{
		for (ViewDiff d : diffs)
		{
			StrBuffer source = d.getMigrateTargetXml();
			if (source.length() > 0)
			{
				out.write("\n");
				source.writeTo(out);
			}
		}
	}

	private void appendDropViews(Writer out, StrBuffer indent)
		throws IOException
	{
		if (this.viewsToDelete == null || this.viewsToDelete.size() == 0) return;
		out.write("\n");
		writeTag(out, indent, TAG_DROP_VIEW, true);
		Iterator itr = this.viewsToDelete.iterator();
		StrBuffer myindent = new StrBuffer(indent);
		myindent.append("  ");
		while (itr.hasNext())
		{
			TableIdentifier t = (TableIdentifier)itr.next();
			writeTagValue(out, myindent, ReportView.TAG_VIEW_NAME, t.getTableName());
		}
		writeTag(out, indent, TAG_DROP_VIEW, false);
	}

	private void appendDropTables(Writer out, StrBuffer indent)
		throws IOException
	{
		if (this.tablesToDelete == null || this.tablesToDelete.size() == 0) return;
		out.write("\n");
		writeTag(out, indent, TAG_DROP_TABLE, true);
		Iterator itr = this.tablesToDelete.iterator();
		StrBuffer myindent = new StrBuffer(indent);
		myindent.append("  ");
		while (itr.hasNext())
		{
			TableIdentifier t = (TableIdentifier)itr.next();
			writeTagValue(out, myindent, ReportTable.TAG_TABLE_NAME, t.getTableName());
		}
		writeTag(out, indent, TAG_DROP_TABLE, false);
	}

	private void writeDiffInfo(Writer out)
		throws IOException
	{
		StrBuffer indent = new StrBuffer("  ");
		StrBuffer indent2 = new StrBuffer("    ");
		writeTag(out, indent, TAG_REF_CONN, true);
		StrBuffer info = this.referenceDb.getDatabaseInfoAsXml(indent2);
		info.writeTo(out);
		writeTag(out, indent, TAG_REF_CONN, false);
		out.write("\n");
		out.write("  <!-- If the target connection is modified according to the  -->\n");
		out.write("  <!-- defintions in this file, then its structure will be    -->\n");
		out.write("  <!-- the same as the reference connection -->\n");
		writeTag(out, indent, TAG_TARGET_CONN, true);
		info = this.targetDb.getDatabaseInfoAsXml(indent2);
		info.writeTo(out);
		writeTag(out, indent, TAG_TARGET_CONN, false);
		out.write("\n");

		info = new StrBuffer();
		TagWriter tw = new TagWriter();

		tw.appendOpenTag(info, indent, TAG_COMPARE_INFO);
		info.append('\n');
		tw.appendTag(info, indent2, TAG_INDEX_INFO, this.diffIndex);
		tw.appendTag(info, indent2, TAG_FK_INFO, this.diffForeignKeys);
		tw.appendTag(info, indent2, TAG_PK_INFO, this.diffPrimaryKeys);
		tw.appendTag(info, indent2, TAG_CONSTRAINT_INFO, Boolean.toString(this.diffConstraints), "compare-names", Boolean.toString(compareConstraintsByName));
		tw.appendTag(info, indent2, TAG_GRANT_INFO, this.diffGrants);
		tw.appendTag(info, indent2, TAG_VIEW_INFO, this.diffViews);
		tw.appendTag(info, indent2, TAG_VIEWS_AS_TABLE, this.treatViewAsTable);


		if (this.referenceSchema != null && this.targetSchema != null)
		{
			tw.appendTag(info, indent2, "reference-schema", this.referenceSchema);
			tw.appendTag(info, indent2, "target-schema", this.targetSchema);
		}
		int count = this.objectsToCompare.size();
		String[] tattr = new String[] { "type", "reference", "compareTo"};
		String[] pattr = new String[] { "referenceProcedure", "compareTo" };
		String[] sattr = new String[] { "referenceSequence", "compareTo" };
		String[] tbls = new String[3];
		DbSettings dbs = this.referenceDb.getMetadata().getDbSettings();
		for (int i=0; i < count; i++)
		{
			// check for ignored tables
			//if (this.referenceTables[i] == null) continue;
			Object o = objectsToCompare.get(i);
			if (o instanceof DiffEntry)
			{
				DiffEntry de = (DiffEntry)o;
				tbls[0] = de.reference.getType();
				tbls[1] = (de.target == null ? "" : StringUtil.trimQuotes(de.target.getTableName()));
				tbls[2] = StringUtil.trimQuotes(de.reference.getTableName());
				if (dbs.isViewType(tbls[0]))
				{
					tw.appendOpenTag(info, indent2, TAG_VIEW_PAIR, tattr, tbls, false);
				}
				else
				{
					tw.appendOpenTag(info, indent2, TAG_TABLE_PAIR, tattr, tbls, false);
				}
			}
			else if (o instanceof ProcDiffEntry)
			{
				ProcDiffEntry pe = (ProcDiffEntry)o;
				tbls[0] = pe.reference.getProcedureName();
				tbls[1] = (pe.target == null ? "" : pe.target.getProcedureName());

				tw.appendOpenTag(info, indent2, TAG_PROC_PAIR, pattr, tbls, false);
			}
			else if (o instanceof SequenceDiffEntry)
			{
				SequenceDiffEntry pe = (SequenceDiffEntry)o;
				tbls[0] = pe.reference.getSequenceName();
				tbls[1] = (pe.target == null ? "" : pe.target.getSequenceName());
				tw.appendOpenTag(info, indent2, TAG_SEQ_PAIR, sattr, tbls, false);
			}
			info.append("/>\n");

		}
		tw.appendCloseTag(info, indent, TAG_COMPARE_INFO);

		info.writeTo(out);
	}

	private void writeTag(Writer out, StrBuffer indent, String tag, boolean isOpeningTag)
		throws IOException
	{
		writeTag(out, indent, tag, isOpeningTag, null, null);
	}
	private void writeTag(Writer out, StrBuffer indent, String tag, boolean isOpeningTag, String attr, String attrValue)
		throws IOException
	{
		if (indent != null) indent.writeTo(out);
		if (isOpeningTag)
		{
			out.write("<");
		}
		else
		{
			out.write("</");
		}
		out.write(tag);
		if (isOpeningTag && attr != null)
		{
			out.write(' ');
			out.write(attr);
			out.write("=\"");
			out.write(attrValue);
			out.write('"');
		}
		out.write(">\n");
	}

	private void writeTagValue(Writer out, StrBuffer indent, String tag, String value)
		throws IOException
	{
		if (indent != null) indent.writeTo(out);
		out.write("<");
		out.write(tag);
		out.write(">");
		out.write(value);
		out.write("</");
		out.write(tag);
		out.write(">\n");
	}
}

class SequenceDiffEntry
{
	SequenceDefinition reference;
	SequenceDefinition target;

	public SequenceDiffEntry(SequenceDefinition ref, SequenceDefinition tar)
	{
		reference = ref;
		target = tar;
	}
}

class ProcDiffEntry
{
	ProcedureDefinition reference;
	ProcedureDefinition target;
	public ProcDiffEntry(ProcedureDefinition ref, ProcedureDefinition tar)
	{
		reference = ref;
		target = tar;
	}
}

class DiffEntry
{
	TableIdentifier reference;
	TableIdentifier target;
	public DiffEntry(TableIdentifier ref, TableIdentifier tar)
	{
		reference = ref;
		target = tar;
	}

	public String toString()
	{
		if (target == null)
			return reference.getType() + ": " + reference.getTableExpression();
		else
			return reference.getType() + ": " + reference.getTableExpression() + " to " + target.getType() + ": " + target.getTableExpression();
	}
}
