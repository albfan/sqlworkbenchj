/*
 * ReportProcedure.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.report;

import java.io.IOException;
import java.io.Writer;
import workbench.db.NoConfigException;
import workbench.db.ProcedureDefinition;
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
	public static final String TAG_PROC_TYPE = "proc-type";
	public static final String TAG_PROC_SOURCE = "proc-source";
	
	private ProcedureDefinition procDef;
	private WbConnection dbConn;
	private TagWriter tagWriter = new TagWriter();
	private StrBuffer indent = new StrBuffer("  ");
	private StrBuffer indent2 = new StrBuffer("    ");

	// </editor-fold>
	
	public ReportProcedure(ProcedureDefinition def, WbConnection conn)
	{
		this.procDef = def;
		this.dbConn = conn;
	}
	
	public CharSequence getSource()
	{
		if (this.procDef == null) return null;
		if (this.procDef.getSource() == null)
		{
			try
			{
				this.dbConn.getMetadata().getProcedureReader().readProcedureSource(this.procDef);
			}
			catch (NoConfigException e)
			{
				procDef.setSource("n/a");
			}
		}
		return this.procDef.getSource();
	}
	
	public void writeXml(Writer out)
		throws IOException
	{
		StrBuffer xml = getXml();
		xml.writeTo(out);
	}

	public String getProcedureName() { return procDef.getProcedureName(); }
	
	public void setIndent(StrBuffer ind)
	{
		this.indent = ind;
		this.indent2 = new StrBuffer(indent);
		this.indent2.append("  ");
	}
	
	public StrBuffer getXml()
	{
		return getXml(true);
	}
	
	public StrBuffer getXml(boolean includeSource)
	{
		StrBuffer result = new StrBuffer(500);
		tagWriter.appendOpenTag(result, indent, TAG_PROC_DEF);
		result.append('\n');
		tagWriter.appendTag(result, indent2, TAG_PROC_CATALOG, procDef.getCatalog());
		tagWriter.appendTag(result, indent2, TAG_PROC_SCHEMA, procDef.getSchema());
		tagWriter.appendTag(result, indent2, TAG_PROC_NAME, procDef.getProcedureName());
		tagWriter.appendTag(result, indent2, TAG_PROC_TYPE, procDef.getObjectType(), "jdbcResultType", Integer.toString(procDef.getResultType()));
		if (includeSource)
		{
			tagWriter.appendTag(result, indent2, TAG_PROC_SOURCE, getSource(), true);
			//result.append('\n');
		}
		tagWriter.appendCloseTag(result, indent, TAG_PROC_DEF);
		return result;
	}
}
