/*
 * ReportProcedure.java
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

import java.io.IOException;
import java.io.Writer;
import workbench.db.WbConnection;
import workbench.util.StrBuffer;

/**
 *
 * @author support@sql-workbench.net
 */
public class ReportProcedure
{
	// <editor-fold defaultstate="collapsed" desc=" Variables ">
	public static final String TAG_PROC_DEF = "proc-def";
	public static final String TAG_PROC_NAME = "proc-name";
	public static final String TAG_PROC_CATALOG = "proc-catalog";
	public static final String TAG_PROC_SCHEMA = "proc-schema";
	public static final String TAG_PROC_SOURCE = "proc-source";
	
	private String procName;
	private String procSchema;
	private String procCatalog;
	private WbConnection dbConn;
	private int returnType;
	private TagWriter tagWriter = new TagWriter();
	private static StrBuffer indent = new StrBuffer("  ");
	private static StrBuffer indent2 = new StrBuffer("    ");
	// </editor-fold>
	
	public ReportProcedure(String cat, String schema, String name, int type, WbConnection conn)
	{
		this.procName = name;
		this.procSchema = schema;
		this.procCatalog = cat;
		this.returnType = type;
		this.dbConn = conn;
	}
	
	public void writeXml(Writer out)
		throws IOException
	{
		StrBuffer xml = getXml();
		xml.writeTo(out);
	}

	public String getProcedureName() { return procName; }
	
	public StrBuffer getXml()
	{
		StrBuffer result = new StrBuffer(500);
		String source = this.dbConn.getMetadata().getProcedureSource(this.procCatalog, this.procSchema, this.procName, this.returnType);
		tagWriter.appendOpenTag(result, indent, TAG_PROC_DEF);
		result.append('\n');
		tagWriter.appendTag(result, indent2, TAG_PROC_CATALOG, procCatalog);
		tagWriter.appendTag(result, indent2, TAG_PROC_SCHEMA, procSchema);
		tagWriter.appendTag(result, indent2, TAG_PROC_NAME, procName);
		tagWriter.appendTag(result, indent2, TAG_PROC_SOURCE, source, true);
		result.append('\n');
		tagWriter.appendCloseTag(result, indent, TAG_PROC_DEF);
		return result;
	}
}
