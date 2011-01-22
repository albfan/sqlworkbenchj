/*
 * ReportProcedure.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
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
 * @author Thomas Kellerer
 */
public class ReportProcedure
{
	public static final String TAG_PROC_DEF = "proc-def";
	public static final String TAG_PROC_NAME = "proc-name";
	public static final String TAG_PROC_CATALOG = "proc-catalog";
	public static final String TAG_PROC_SCHEMA = "proc-schema";
	public static final String TAG_PROC_TYPE = "proc-type";
	public static final String TAG_PROC_SOURCE = "proc-source";

	public static final String TAG_PKG_DEF = "package-def";
	public static final String TAG_PKG_NAME = "package-name";
	public static final String TAG_PKG_SCHEMA = "package-schema";
	public static final String TAG_PKG_SOURCE = "package-source";

	private ProcedureDefinition procDef;
	private WbConnection dbConn;
	private TagWriter tagWriter = new TagWriter();
	private StrBuffer indent = new StrBuffer("  ");
	private StrBuffer indent2 = new StrBuffer("    ");

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

	public String getProcedureName()
	{
		return procDef.getProcedureName();
	}

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
		String mainTag = TAG_PROC_DEF;
		String srcTag = TAG_PROC_SOURCE;
		String schemaTag = TAG_PROC_SCHEMA;
		String nameTag = TAG_PROC_NAME;
		String objectName = procDef.getProcedureName();

		if (procDef.isOraclePackage())
		{
			mainTag = TAG_PKG_DEF;
			srcTag = TAG_PKG_SOURCE;
			schemaTag = TAG_PKG_SCHEMA;
			nameTag = TAG_PKG_NAME;
			objectName = procDef.getPackageName();
		}
		tagWriter.appendOpenTag(result, indent, mainTag);

		result.append('\n');
		if (!procDef.isOraclePackage() && procDef.getCatalog() != null)
		{
			tagWriter.appendTag(result, indent2, TAG_PROC_CATALOG, procDef.getCatalog());
		}
		tagWriter.appendTag(result, indent2, schemaTag, procDef.getSchema());
		tagWriter.appendTag(result, indent2, nameTag, objectName);

			// For an Oracle Package writing the type does not make sense
		if (procDef.getPackageName() == null)
		{
			tagWriter.appendTag(result, indent2, TAG_PROC_TYPE, procDef.getObjectType(), "jdbcResultType", Integer.toString(procDef.getResultType()));
		}

		if (includeSource)
		{
			tagWriter.appendTag(result, indent2, srcTag, getSource(), true);
			//result.append('\n');
		}
		tagWriter.appendCloseTag(result, indent, mainTag);
		return result;
	}
}
