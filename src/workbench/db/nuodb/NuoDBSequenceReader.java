/*
 * HsqlSequenceReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.nuodb;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import workbench.db.SequenceDefinition;
import workbench.db.SequenceReader;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;


/**
 * SequenceReader for <a href="http://www.nuodb">NuoDB</a>
 *
 * @author  Thomas Kellerer
 */
public class NuoDBSequenceReader
	implements SequenceReader
{
	private WbConnection dbConn;
	private String baseQuery;

	public NuoDBSequenceReader(WbConnection conn)
	{
		this.dbConn = conn;
		baseQuery =
			"SELECT schema, \n" +
			"       sequencename\n " +
			"FROM system.sequences";
	}

	@Override
	public void readSequenceSource(SequenceDefinition def)
	{
		if (def == null) return;
		CharSequence s = getSequenceSource(def.getCatalog(), def.getSequenceOwner(), def.getSequenceName());
		def.setSource(s);
	}

	@Override
	public DataStore getRawSequenceDefinition(String catalog, String schema, String namePattern)
	{
		StringBuilder query = new StringBuilder(baseQuery.length() + 20);
		query.append(baseQuery);
		boolean whereAdded = false;

		if (StringUtil.isNonBlank(namePattern))
		{
			whereAdded = true;
			query.append(" WHERE ");
			SqlUtil.appendExpression(query, "SEQUENCENAME", StringUtil.trimQuotes(namePattern), dbConn);
		}

		if (StringUtil.isNonBlank(schema))
		{
			if (!whereAdded)
			{
				query.append(" WHERE ");
			}
			else
			{
				query.append(" AND ");
			}
			SqlUtil.appendExpression(query, "SCHEMA", StringUtil.trimQuotes(schema), null);
		}

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("NuoDBSequenceReader.getRawSequenceDefinition()", "Using query=" + query);
		}

		PreparedStatement stmt = null;
		ResultSet rs = null;
		DataStore result = null;
		try
		{
			stmt = this.dbConn.getSqlConnection().prepareStatement(query.toString());
			rs = stmt.executeQuery();
			result = new DataStore(rs, true);
		}
		catch (Throwable e)
		{
			LogMgr.logError("NuoDbSequenceReader.getSequenceDefinition()", "Error when retrieving sequence definition", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}

		return result;
	}


	@Override
	public List<SequenceDefinition> getSequences(String catalog, String owner, String namePattern)
	{
		DataStore ds = getRawSequenceDefinition(catalog, owner, namePattern);
		if (ds == null) return Collections.emptyList();

		List<SequenceDefinition> result = new ArrayList<SequenceDefinition>();

		for (int row = 0; row < ds.getRowCount(); row++)
		{
			result.add(createSequenceDefinition(ds, row));
		}
		return result;
	}

	private SequenceDefinition createSequenceDefinition(DataStore ds, int row)
	{
		SequenceDefinition result = null;

    if (ds == null || ds.getRowCount() == 0) return null;

		String name = ds.getValueAsString(row, "SEQUENCENAME");
		String schema = ds.getValueAsString(row, "SCHEMA");
		result = new SequenceDefinition(schema, name);
		result.setSource(buildSource(result));
		return result;
	}

	@Override
	public SequenceDefinition getSequenceDefinition(String catalog, String owner, String sequence)
	{
		DataStore ds = getRawSequenceDefinition(catalog, owner, sequence);
		if (ds == null) return null;
		return createSequenceDefinition(ds, 0);
	}

	@Override
	public CharSequence getSequenceSource(String catalog, String owner, String sequence)
	{
		SequenceDefinition def = getSequenceDefinition(catalog, owner, sequence);
		return buildSource(def);
	}

	protected CharSequence buildSource(SequenceDefinition def)
	{
		if (def == null) return StringUtil.EMPTY_STRING;

		StringBuilder result = new StringBuilder(100);
		result.append("CREATE SEQUENCE ");
		result.append(SqlUtil.buildExpression(dbConn, null, def.getSchema(), def.getSequenceName()));
		result.append(';');
		return result;
	}

	@Override
	public String getSequenceTypeName()
	{
		return SequenceReader.DEFAULT_TYPE_NAME;
	}
}
