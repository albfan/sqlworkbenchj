/*
 * ReportPackage.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.report;

import workbench.db.ProcedureDefinition;
import workbench.db.ReaderFactory;
import workbench.db.WbConnection;
import workbench.db.oracle.OracleProcedureReader;
import workbench.util.StrBuffer;

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
	private StrBuffer indent = new StrBuffer("  ");
	private StrBuffer indent2 = new StrBuffer("    ");

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
		if (conn.getMetadata().isOracle())
		{
			OracleProcedureReader reader = (OracleProcedureReader)ReaderFactory.getProcedureReader(conn.getMetadata());
			source = reader.getPackageSource(owner, name);
		}
	}

	public void setIndent(StrBuffer ind)
	{
		this.indent = ind;
		this.indent2 = new StrBuffer(indent);
		this.indent2.append("  ");
	}

	public StrBuffer getXml(boolean includeSource)
	{
		StrBuffer result = new StrBuffer(500);
		TagWriter tagWriter = new TagWriter();

		tagWriter.appendOpenTag(result, indent, TAG_PKG_DEF, new String[]{"schema", "packageName"},new String[]{owner, name});

		result.append('\n');
		tagWriter.appendTag(result, indent2, TAG_PKG_SCHEMA, owner);
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
