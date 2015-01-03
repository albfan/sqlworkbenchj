/*
 * FirebirdSequenceReader.java
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
package workbench.db.firebird;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.SequenceDefinition;
import workbench.db.SequenceReader;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * SequenceReader for Firebird
 *
 * @author  Thomas Kellerer
 */
public class FirebirdSequenceReader
	implements SequenceReader
{
	private WbConnection dbConnection;

	public FirebirdSequenceReader(WbConnection conn)
	{
		this.dbConnection = conn;
	}

	/**
	 *	Return the source SQL for a H2 sequence definition.
	 *
	 *	@return The SQL to recreate the given sequence
	 */
	@Override
	public CharSequence getSequenceSource(String catalog, String owner, String aSequence)
	{
		SequenceDefinition def = getSequenceDefinition(catalog, owner, aSequence);
		if (def == null) return "";
		return def.getSource();
	}

	@Override
	public List<SequenceDefinition> getSequences(String catalog, String owner, String namePattern)
	{
		DataStore ds = getRawSequenceDefinition(catalog, owner, namePattern);
		if (ds == null) return Collections.emptyList();
		List<SequenceDefinition> result = new ArrayList<>();

		for (int row=0; row < ds.getRowCount(); row++)
		{
			result.add(createSequenceDefinition(ds, row));
		}
		return result;
	}

	@Override
	public SequenceDefinition getSequenceDefinition(String catalog, String owner, String sequence)
	{
    DataStore ds = getRawSequenceDefinition(catalog, owner, sequence);
    if (ds == null || ds.getRowCount() == 0) return null;

		return createSequenceDefinition(ds, 0);
	}

	private SequenceDefinition createSequenceDefinition(DataStore ds, int row)
	{
		SequenceDefinition result = null;

    if (ds == null || ds.getRowCount() == 0) return null;

		String name = ds.getValueAsString(row, 0);
		result = new SequenceDefinition(null, name);

		String comment = ds.getValueAsString(row, 1);
		result.setComment(comment);
		readSequenceSource(result);

		return result;
	}

	@Override
	public void readSequenceSource(SequenceDefinition def)
	{
		if (def == null) return;

		StringBuilder result = new StringBuilder(100);

    result.append("CREATE SEQUENCE ");
    result.append(def.getSequenceName());

		result.append(";\n");

		String comments = def.getComment();
		if (StringUtil.isNonBlank(comments))
		{
			result.append("\nCOMMENT ON SEQUENCE ");
			result.append(def.getSequenceName());
			result.append(" IS '");
			result.append(comments.replace("'", "''"));
			result.append("';");
		}
		def.setSource(result);
	}

	@Override
	public DataStore getRawSequenceDefinition(String catalog, String owner, String sequence)
	{
		Statement stmt = null;
		ResultSet rs = null;
		DataStore ds = null;
		try
		{
			StringBuilder sql = new StringBuilder(100);
			sql.append(
				"SELECT trim(rdb$generator_name) AS SEQUENCE_NAME, \n" +
				"       trim(rdb$description) AS REMARKS \n" +
				"FROM rdb$generators \n" +
				"WHERE (rdb$system_flag = 0 OR rdb$system_flag IS NULL) \n");

			SqlUtil.appendAndCondition(sql, "rdb$generator_name", sequence, dbConnection);
			sql.append("\n ORDER BY 1");
			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logInfo("FirebirdSequenceReader.getRawSequenceDefinition()", "Using query=\n" + sql);
			}
			stmt = this.dbConnection.createStatement();
			rs = stmt.executeQuery(sql.toString());
			ds = new DataStore(rs, true);
		}
		catch (Exception e)
		{
			LogMgr.logError("FirebirdSequenceReader.getRawSequenceDefinition()", "Error reading sequence definition", e);
			ds = null;
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return ds;
	}

	@Override
	public String getSequenceTypeName()
	{
		return SequenceReader.DEFAULT_TYPE_NAME;
	}
}
