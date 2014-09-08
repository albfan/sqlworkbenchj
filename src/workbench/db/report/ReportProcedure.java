/*
 * ReportProcedure.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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

import java.io.IOException;
import java.io.Writer;

import workbench.resource.Settings;

import workbench.db.NoConfigException;
import workbench.db.ProcedureDefinition;
import workbench.db.WbConnection;

import workbench.sql.DelimiterDefinition;

import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class ReportProcedure
{
	public static final String TAG_PROC_DEF = "proc-def";
	public static final String TAG_PROC_NAME = "proc-name";
	public static final String TAG_PROC_FULL_NAME = "proc-full-name";
	public static final String TAG_PROC_CATALOG = "proc-catalog";
	public static final String TAG_PROC_SCHEMA = "proc-schema";
	public static final String TAG_PROC_TYPE = "proc-type";
	public static final String TAG_PROC_SOURCE = "proc-source";
	public static final String TAG_PROC_COMMENT = "proc-comment";

	private ProcedureDefinition procDef;
	private WbConnection dbConn;
	private final TagWriter tagWriter = new TagWriter();
	private StringBuilder indent = new StringBuilder("  ");
	private StringBuilder indent2 = new StringBuilder("    ");
	private String schemaToUse;
	private String fullName;
	private DelimiterDefinition delim;

	public ReportProcedure(ProcedureDefinition def, WbConnection conn)
	{
		this.procDef = def;
		this.dbConn = conn;
		delim = Settings.getInstance().getAlternateDelimiter(dbConn);
	}

	public void setFullname(String name)
	{
		this.fullName = name;
	}

	public void setSchemaToUse(String targetSchema)
	{
		this.schemaToUse = targetSchema;
	}

	private String getSchema()
	{
		return schemaToUse == null ? procDef.getSchema() : schemaToUse;
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
		StringBuilder xml = getXml();
		out.append(xml);
	}

	public ProcedureDefinition getProcedure()
	{
		return procDef;
	}

	public String getProcedureName()
	{
		return procDef.getProcedureName();
	}

	public void setIndent(StringBuilder ind)
	{
		this.indent = ind == null ? new StringBuilder(0) : ind;
		this.indent2 = new StringBuilder(indent);
		this.indent2.append("  ");
	}

	public StringBuilder getXml()
	{
		return getXml(true);
	}

	public StringBuilder getXml(boolean includeSource)
	{
		StringBuilder result = new StringBuilder(500);
		String objectName = procDef.getProcedureName();

		tagWriter.appendOpenTag(result, indent, TAG_PROC_DEF);

		result.append('\n');
		if (!procDef.isOraclePackage() && procDef.getCatalog() != null)
		{
			tagWriter.appendTag(result, indent2, TAG_PROC_CATALOG, procDef.getCatalog());
		}
		tagWriter.appendTag(result, indent2, TAG_PROC_SCHEMA, getSchema());
		tagWriter.appendTag(result, indent2, TAG_PROC_NAME, objectName);
		if (StringUtil.isNonBlank(fullName))
		{
			tagWriter.appendTag(result, indent2, TAG_PROC_FULL_NAME, fullName);
		}

		if (StringUtil.isNonBlank(procDef.getComment()))
		{
			tagWriter.appendTag(result, indent2, TAG_PROC_COMMENT, procDef.getComment());
		}

		tagWriter.appendTag(result, indent2, TAG_PROC_TYPE, procDef.getObjectType(), "jdbcResultType", Integer.toString(procDef.getResultType()));
		if (includeSource)
		{
			String src = getSource().toString().trim();
			if (delim != null)
			{
				src = delim.removeFromEnd(src);
			}
			tagWriter.appendTag(result, indent2, TAG_PROC_SOURCE, src, true);
			//result.append('\n');
		}
		tagWriter.appendCloseTag(result, indent, TAG_PROC_DEF);
		return result;
	}
}
