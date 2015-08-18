/*
 * ReportPackage.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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

import workbench.db.ProcedureDefinition;
import workbench.db.ProcedureReader;
import workbench.db.ReaderFactory;
import workbench.db.WbConnection;

/**
 * A class to represent an Oracle package for SchemaDiff and SchemaReport
 * @author Thomas Kellerer
 */
public class ReportPackage
{
	public static final String TAG_PKG_DEF = "package-def";
	public static final String TAG_PKG_NAME = "package-name";
	public static final String TAG_PKG_SCHEMA = "package-schema";
	public static final String TAG_PKG_SOURCE = "package-source";

	private String owner;
	private String name;
	private CharSequence source;
	private StringBuilder indent = new StringBuilder("  ");
	private StringBuilder indent2 = new StringBuilder("    ");
	private String schemaToUse;

	public ReportPackage(ProcedureDefinition proc)
	{
		if (proc.isOraclePackage())
		{
			this.owner = proc.getSchema();
			this.name = proc.getPackageName();
		}
		else
		{
			throw new IllegalArgumentException(proc.getProcedureName() + " is not an Oracle package!");
		}
	}

	public void setSchemaToUse(String targetSchema)
	{
		this.schemaToUse = targetSchema;
	}

	private String getSchema()
	{
		return schemaToUse == null ? owner : schemaToUse;
	}

	public ReportPackage(String owner, String name)
	{
		this.owner = owner;
		this.name = name;
	}

	public String getPackageName()
	{
		return name;
	}
	public CharSequence getSource()
	{
		return source;
	}

	public void setSource(CharSequence sql)
	{
		source = sql;
	}

	public void readSource(WbConnection conn)
	{
    ProcedureReader reader = ReaderFactory.getProcedureReader(conn.getMetadata());
    source = reader.getPackageSource(null, owner, name);
	}

	public void setIndent(StringBuilder ind)
	{
		this.indent = ind == null ? new StringBuilder(0) : ind;
		this.indent2 = new StringBuilder(indent);
		this.indent2.append("  ");
	}

	public StringBuilder getXml(boolean includeSource)
	{
		StringBuilder result = new StringBuilder(500);
		TagWriter tagWriter = new TagWriter();

		String schema = getSchema();
		tagWriter.appendOpenTag(result, indent, TAG_PKG_DEF, new String[]{"schema", "packageName"}, new String[] {schema, name});

		result.append('\n');
		tagWriter.appendTag(result, indent2, TAG_PKG_SCHEMA, schema);
		tagWriter.appendTag(result, indent2, TAG_PKG_NAME, name);

		if (includeSource)
		{
			tagWriter.appendTag(result, indent2, TAG_PKG_SOURCE, source, true);
		}
		tagWriter.appendCloseTag(result, indent, TAG_PKG_DEF);
		return result;
	}

	@Override
	public int hashCode()
	{
		int hash = 7;
		hash = 61 * hash + (this.owner != null ? this.owner.hashCode() : 0);
		hash = 61 * hash + (this.name != null ? this.name.hashCode() : 0);
		return hash;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == null)
		{
			return false;
		}
		if (getClass() != obj.getClass())
		{
			return false;
		}
		final ReportPackage other = (ReportPackage) obj;
		if ((this.owner == null) ? (other.owner != null) : !this.owner.equals(other.owner))
		{
			return false;
		}
		if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name))
		{
			return false;
		}
		return true;
	}

}
